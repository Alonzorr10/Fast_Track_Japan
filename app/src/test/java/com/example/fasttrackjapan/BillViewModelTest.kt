package com.example.fasttrackjapan

import android.content.Context
import android.net.Uri
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
class BillViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun bill(id: String, label: String, date: String) =
        Bill(id = id, label = label, date = date, imageUrl = "https://example.com/$id.jpg", userId = "u")

    @Test
    fun fetchBills_populates_from_repo() = runTest(dispatcher) {
        val fake = FakeBillRepo(initial = listOf(bill("1", "Wifi", "2026-06-01"), bill("2", "Gas", "2026-06-15")))
        val vm = BillViewModel.newForTest(fake)
        vm.fetchBills()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(setOf("Wifi", "Gas"), vm.bills.map { it.label }.toSet())
    }

    @Test
    fun clearBills_empties_the_list() = runTest(dispatcher) {
        val fake = FakeBillRepo(initial = listOf(bill("1", "Wifi", "2026-06-01")))
        val vm = BillViewModel.newForTest(fake)
        vm.fetchBills()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, vm.bills.size)
        vm.clearBills()
        assertTrue(vm.bills.isEmpty())
    }

    @Test
    fun deleteBill_removes_locally_and_calls_repo() = runTest(dispatcher) {
        val initial = listOf(bill("1", "Wifi", "2026-06-01"), bill("2", "Gas", "2026-06-15"))
        val fake = FakeBillRepo(initial = initial)
        val vm = BillViewModel.newForTest(fake)
        vm.fetchBills()
        dispatcher.scheduler.advanceUntilIdle()
        vm.deleteBill(initial.first())
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf("Gas"), vm.bills.map { it.label })
        assertEquals(listOf("1"), fake.deleteCalls.map { it.id })
    }

    @Test
    fun updateBill_replaces_matching_row_locally() = runTest(dispatcher) {
        val initial = listOf(bill("1", "Wifi", "2026-06-01"))
        val fake = FakeBillRepo(initial = initial)
        val vm = BillViewModel.newForTest(fake)
        vm.fetchBills()
        dispatcher.scheduler.advanceUntilIdle()
        val updated = initial.first().copy(label = "Wifi (renamed)", date = "2026-06-30")
        vm.updateBill(updated)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Wifi (renamed)", vm.bills.first().label)
        assertEquals("2026-06-30", vm.bills.first().date)
    }

    @Test
    fun sortBillsByLabel_is_case_insensitive() = runTest(dispatcher) {
        val initial = listOf(
            bill("1", "zebra", "2026-06-01"),
            bill("2", "Apple", "2026-06-02"),
            bill("3", "banana", "2026-06-03")
        )
        val fake = FakeBillRepo(initial = initial)
        val vm = BillViewModel.newForTest(fake)
        vm.fetchBills()
        dispatcher.scheduler.advanceUntilIdle()
        vm.sortBillsByLabel()
        assertEquals(listOf("Apple", "banana", "zebra"), vm.bills.map { it.label })
    }

    @Test
    fun sortBillsByDate_puts_unparseable_dates_last_and_orders_chronologically() = runTest(dispatcher) {
        val initial = listOf(
            bill("1", "A", "2026-06-15"),
            bill("2", "B", "not-a-date"),
            bill("3", "C", "2026-06-01"),
            bill("4", "D", "2026-06-08")
        )
        val fake = FakeBillRepo(initial = initial)
        val vm = BillViewModel.newForTest(fake)
        vm.fetchBills()
        dispatcher.scheduler.advanceUntilIdle()
        vm.sortBillsByDate()
        // C (06-01), D (06-08), A (06-15), then B (bad date) last
        assertEquals(listOf("C", "D", "A", "B"), vm.bills.map { it.label })
    }

    @Test
    fun fetchBills_swallows_repo_error_without_crashing() = runTest(dispatcher) {
        val fake = FakeBillRepo(initial = emptyList()).apply { throwOnFetch = true }
        val vm = BillViewModel.newForTest(fake)
        vm.fetchBills()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.bills.isEmpty())
    }
}

/** Test double for BillViewModel.Repo. Records calls; no real Supabase/Storage involvement. */
class FakeBillRepo(initial: List<Bill>) : BillViewModel.Repo {
    private val store = initial.toMutableList()
    val deleteCalls = mutableListOf<Bill>()
    val updateCalls = mutableListOf<Bill>()
    var throwOnFetch: Boolean = false

    override suspend fun fetchBills(): List<Bill> {
        if (throwOnFetch) throw RuntimeException("boom")
        return store.toList()
    }

    override suspend fun addBill(context: Context, label: String, date: String, imageUri: Uri): AddBillResult {
        // addBill uses Toasts inside the ViewModel and is not exercised by these tests.
        val row = Bill(label = label, date = date, imageUrl = "test://$label", userId = "u")
        return AddBillResult.Success(row)
    }

    override suspend fun updateBill(bill: Bill) {
        updateCalls += bill
        val i = store.indexOfFirst { it.id == bill.id }
        if (i >= 0) store[i] = bill
    }

    override suspend fun deleteBill(bill: Bill) {
        deleteCalls += bill
        store.removeAll { it.id == bill.id }
    }
}
