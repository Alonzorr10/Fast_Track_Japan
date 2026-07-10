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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private val sampleProfile = UserProfile(
        id = "user-1",
        email = "u@example.com",
        fullName = "Test User",
        age = 30,
        address = "1-1 Somewhere",
        ward = "Setagaya",
        profilePictureUrl = null,
        updatedAt = null
    )

    @Test
    fun fetchProfile_populates_state_from_repo() = runTest(dispatcher) {
        val fake = FakeProfileRepo(profile = sampleProfile)
        val vm = ProfileViewModel.newForTest(fake)
        vm.fetchProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(sampleProfile, vm.profile)
        assertFalse(vm.isLoading)
    }

    @Test
    fun fetchProfile_when_repo_returns_null_leaves_profile_null() = runTest(dispatcher) {
        val fake = FakeProfileRepo(profile = null)
        val vm = ProfileViewModel.newForTest(fake)
        vm.fetchProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.profile)
    }

    @Test
    fun fetchProfile_swallows_repo_error_without_crashing() = runTest(dispatcher) {
        val fake = FakeProfileRepo(profile = null).apply { throwOnFetch = true }
        val vm = ProfileViewModel.newForTest(fake)
        vm.fetchProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.profile)
        assertFalse(vm.isLoading)
    }

    @Test
    fun clear_resets_profile_and_loading() = runTest(dispatcher) {
        val fake = FakeProfileRepo(profile = sampleProfile)
        val vm = ProfileViewModel.newForTest(fake)
        vm.fetchProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(sampleProfile, vm.profile)
        vm.clear()
        assertNull(vm.profile)
        assertFalse(vm.isLoading)
    }
}

/** Test double for ProfileViewModel.Repo. No real Supabase or Storage involvement. */
class FakeProfileRepo(private val profile: UserProfile?) : ProfileViewModel.Repo {
    var throwOnFetch: Boolean = false
    val upsertCalls = mutableListOf<UserProfile>()

    override suspend fun fetchProfile(): UserProfile? {
        if (throwOnFetch) throw RuntimeException("boom")
        return profile
    }

    // Not exercised by these tests — saveProfile involves Toast which is unfriendly to JVM tests.
    override suspend fun uploadAvatar(context: Context, uri: Uri): String? = null

    override suspend fun upsertProfile(profile: UserProfile): UserProfile {
        upsertCalls += profile
        return profile
    }
}
