package com.example.fasttrackjapan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProcedureViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun toggleStep_optimistically_marks_completed_then_persists() = runTest(dispatcher) {
        val fake = FakeProcedureRepo(shouldFail = false)
        val vm = ProcedureViewModel.newForTest(fake)
        vm.seedForTest(
            userProcedure = UserProcedure(id = "up1", userId = "u", procedureCode = "MOVING_IN", startDate = "2026-07-01"),
            steps = listOf(ProcedureStep(id = "s1", procedureCode = "MOVING_IN", sort = 1, titleEn = "S1", titleJa = "S1", description = "", deadlineDaysFromStart = 14, reminderLeadDays = 3)),
            userSteps = emptyList()
        )
        vm.toggleStep("s1")
        // Optimistic: view marks completedAt immediately.
        assertNotNull(vm.steps.value.first { it.step.id == "s1" }.completedAt)
        dispatcher.scheduler.advanceUntilIdle()
        // Persisted call happened.
        assertEquals(listOf("s1" to true), fake.calls)
        assertNull(vm.errorMessage.value)
    }

    @Test
    fun toggleStep_rolls_back_on_persist_failure() = runTest(dispatcher) {
        val fake = FakeProcedureRepo(shouldFail = true)
        val vm = ProcedureViewModel.newForTest(fake)
        vm.seedForTest(
            userProcedure = UserProcedure(id = "up1", userId = "u", procedureCode = "MOVING_IN", startDate = "2026-07-01"),
            steps = listOf(ProcedureStep(id = "s1", procedureCode = "MOVING_IN", sort = 1, titleEn = "S1", titleJa = "S1", description = "", deadlineDaysFromStart = 14, reminderLeadDays = 3)),
            userSteps = emptyList()
        )
        vm.toggleStep("s1")
        dispatcher.scheduler.advanceUntilIdle()
        // Rolled back: not completed, and errorMessage populated.
        assertNull(vm.steps.value.first { it.step.id == "s1" }.completedAt)
        assertNotNull(vm.errorMessage.value)
    }
}

/** Test double for ProcedureRepository — used only by ProcedureViewModelTest. */
class FakeProcedureRepo(var shouldFail: Boolean) : ProcedureViewModel.Repo {
    val calls = mutableListOf<Pair<String, Boolean>>() // (stepId, isCompleted)
    override suspend fun fetchProcedures(): List<Procedure> = emptyList()
    override suspend fun fetchSteps(code: String): List<ProcedureStep> = emptyList()
    override suspend fun getUserProcedure(code: String): UserProcedure? = null
    override suspend fun startProcedure(code: String, startDate: String): UserProcedure? = null
    override suspend fun fetchUserSteps(userProcedureId: String): List<UserProcedureStep> = emptyList()
    override suspend fun setStepCompleted(userProcedureId: String, stepId: String, completedAt: String?) {
        if (shouldFail) throw RuntimeException("boom")
        calls += stepId to (completedAt != null)
    }
}
