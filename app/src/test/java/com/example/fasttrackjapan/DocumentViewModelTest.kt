package com.example.fasttrackjapan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun doc(id: String, type: String, expiration: String = "2027-01-01") =
        ExpirationDocument(id = id, type = type, expirationDate = expiration, notificationLeadTime = 30, userId = "u")

    @Test
    fun fetchDocuments_populates_from_repo() = runTest(dispatcher) {
        val fake = FakeDocumentRepo(initial = listOf(doc("1", "Residence Card"), doc("2", "Passport")))
        val vm = DocumentViewModel.newForTest(fake)
        vm.fetchDocuments()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, vm.documents.size)
        assertEquals(setOf("Residence Card", "Passport"), vm.documents.map { it.type }.toSet())
    }

    @Test
    fun addDocument_appends_when_repo_returns_row() = runTest(dispatcher) {
        val fake = FakeDocumentRepo(initial = emptyList())
        val vm = DocumentViewModel.newForTest(fake)
        vm.addDocument("Visa", "2027-06-01", notificationLeadTime = 60)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, vm.documents.size)
        assertEquals("Visa", vm.documents.first().type)
        // repo saw the call
        assertEquals(1, fake.addCalls.size)
        assertEquals(Triple("Visa", "2027-06-01", 60), fake.addCalls.first())
    }

    @Test
    fun addDocument_when_repo_returns_null_does_not_append() = runTest(dispatcher) {
        val fake = FakeDocumentRepo(initial = emptyList()).apply { returnNullOnAdd = true }
        val vm = DocumentViewModel.newForTest(fake)
        vm.addDocument("Visa", "2027-06-01", notificationLeadTime = 60)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.documents.isEmpty())
    }

    @Test
    fun deleteDocument_removes_locally_and_calls_repo() = runTest(dispatcher) {
        val initial = listOf(doc("1", "Residence Card"), doc("2", "Passport"))
        val fake = FakeDocumentRepo(initial = initial)
        val vm = DocumentViewModel.newForTest(fake)
        vm.fetchDocuments()
        dispatcher.scheduler.advanceUntilIdle()
        vm.deleteDocument(initial.first())
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf("Passport"), vm.documents.map { it.type })
        assertEquals(listOf("1"), fake.deleteCalls)
    }

    @Test
    fun updateDocument_replaces_matching_row_locally() = runTest(dispatcher) {
        val initial = listOf(doc("1", "Residence Card", expiration = "2026-01-01"))
        val fake = FakeDocumentRepo(initial = initial)
        val vm = DocumentViewModel.newForTest(fake)
        vm.fetchDocuments()
        dispatcher.scheduler.advanceUntilIdle()
        val updated = initial.first().copy(expirationDate = "2028-12-31")
        vm.updateDocument(updated)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("2028-12-31", vm.documents.first().expirationDate)
        assertEquals(1, fake.updateCalls.size)
    }

    @Test
    fun fetchDocuments_swallows_repo_error_without_crashing() = runTest(dispatcher) {
        val fake = FakeDocumentRepo(initial = emptyList()).apply { throwOnFetch = true }
        val vm = DocumentViewModel.newForTest(fake)
        vm.fetchDocuments()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.documents.isEmpty())
    }
}

/** Test double for DocumentViewModel.Repo. Records calls; no real Supabase involvement. */
class FakeDocumentRepo(initial: List<ExpirationDocument>) : DocumentViewModel.Repo {
    private val store = initial.toMutableList()
    val addCalls = mutableListOf<Triple<String, String, Int>>()
    val deleteCalls = mutableListOf<String>()
    val updateCalls = mutableListOf<ExpirationDocument>()
    var throwOnFetch: Boolean = false
    var returnNullOnAdd: Boolean = false

    override suspend fun fetchDocuments(): List<ExpirationDocument> {
        if (throwOnFetch) throw RuntimeException("boom")
        return store.toList()
    }

    override suspend fun addDocument(type: String, expirationDate: String, notificationLeadTime: Int): ExpirationDocument? {
        addCalls += Triple(type, expirationDate, notificationLeadTime)
        if (returnNullOnAdd) return null
        val row = ExpirationDocument(
            type = type,
            expirationDate = expirationDate,
            notificationLeadTime = notificationLeadTime,
            userId = "u"
        )
        store += row
        return row
    }

    override suspend fun deleteDocument(id: String) {
        deleteCalls += id
        store.removeAll { it.id == id }
    }

    override suspend fun updateDocument(doc: ExpirationDocument) {
        updateCalls += doc
        val i = store.indexOfFirst { it.id == doc.id }
        if (i >= 0) store[i] = doc
    }
}
