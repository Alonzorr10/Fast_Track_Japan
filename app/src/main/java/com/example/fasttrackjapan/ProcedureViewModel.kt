package com.example.fasttrackjapan

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

open class ProcedureViewModel(app: Application) : AndroidViewModel(app) {

    /**
     * Minimal Repo surface used by the ViewModel. `ProcedureRepository` implements
     * this, and tests supply a fake — no Supabase needed to unit-test the VM.
     */
    interface Repo {
        suspend fun fetchProcedures(): List<Procedure>
        suspend fun fetchSteps(code: String): List<ProcedureStep>
        suspend fun getUserProcedure(code: String): UserProcedure?
        suspend fun startProcedure(code: String, startDate: String): UserProcedure?
        suspend fun fetchUserSteps(userProcedureId: String): List<UserProcedureStep>
        suspend fun setStepCompleted(userProcedureId: String, stepId: String, completedAt: String?)
    }

    private val repo: Repo = DefaultRepo(ProcedureRepository())

    private val _procedures = MutableStateFlow<List<Procedure>>(emptyList())
    val procedures: StateFlow<List<Procedure>> = _procedures.asStateFlow()

    private val _activeProcedure = MutableStateFlow<UserProcedure?>(null)
    val activeProcedure: StateFlow<UserProcedure?> = _activeProcedure.asStateFlow()

    private val _steps = MutableStateFlow<List<ProcedureStepView>>(emptyList())
    val steps: StateFlow<List<ProcedureStepView>> = _steps.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _initialLoadDone = MutableStateFlow(false)
    val initialLoadDone: StateFlow<Boolean> = _initialLoadDone.asStateFlow()

    private var loadJob: Job? = null

    /** Fetch catalog and, for each procedure the user has already started, its steps + progress. */
    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _procedures.value = effectiveRepo.fetchProcedures()
                // Load Moving In progress if present.
                val active = effectiveRepo.getUserProcedure("MOVING_IN")
                _activeProcedure.value = active
                if (active != null) {
                    val steps = effectiveRepo.fetchSteps("MOVING_IN")
                    val userSteps = effectiveRepo.fetchUserSteps(active.id)
                    _steps.value = viewsFor(steps, userSteps, active)
                } else {
                    _steps.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("ProcedureViewModel", "load failed: ${e.message}")
                _errorMessage.value = "Couldn't load your procedures. Check your connection."
                _activeProcedure.value = null
                _steps.value = emptyList()
            } finally {
                _isLoading.value = false
                _initialLoadDone.value = true
            }
        }
    }

    /** Persist the start date for a procedure and reload its steps. */
    fun startProcedure(code: String, startDate: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val active = effectiveRepo.startProcedure(code, startDate)
                _activeProcedure.value = active
                if (active != null) {
                    val steps = effectiveRepo.fetchSteps(code)
                    val userSteps = effectiveRepo.fetchUserSteps(active.id)
                    _steps.value = viewsFor(steps, userSteps, active)
                    onDone()
                }
            } catch (e: Exception) {
                Log.e("ProcedureViewModel", "startProcedure failed: ${e.message}")
                _errorMessage.value = "Couldn't save. Please try again."
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Optimistically flip a step's completed state, then persist. Rolls back on failure. */
    fun toggleStep(stepId: String) {
        val active = _activeProcedure.value ?: return
        val prior = _steps.value
        val idx = prior.indexOfFirst { it.step.id == stepId }
        if (idx < 0) return
        val current = prior[idx]
        val nextCompletedAt = if (current.completedAt == null) java.time.Instant.now().toString() else null

        val optimistic = prior.toMutableList().also {
            val newView = ProcedureStatusCalculator.viewOf(
                step = current.step,
                startDate = runCatching { LocalDate.parse(active.startDate) }.getOrNull(),
                completedAt = nextCompletedAt,
                today = LocalDate.now()
            )
            it[idx] = newView
        }
        _steps.value = optimistic

        viewModelScope.launch {
            try {
                effectiveRepo.setStepCompleted(active.id, stepId, nextCompletedAt)
            } catch (e: Exception) {
                Log.e("ProcedureViewModel", "toggle persist failed: ${e.message}")
                _steps.value = prior // rollback
                _errorMessage.value = "Couldn't save. Please try again."
            }
        }
    }

    /** Wipe all state and cancel in-flight work. Called on sign-out. */
    fun clear() {
        loadJob?.cancel()
        _procedures.value = emptyList()
        _activeProcedure.value = null
        _steps.value = emptyList()
        _isLoading.value = false
        _errorMessage.value = null
        _initialLoadDone.value = false
    }

    private fun viewsFor(
        steps: List<ProcedureStep>,
        userSteps: List<UserProcedureStep>,
        active: UserProcedure
    ): List<ProcedureStepView> {
        val start = runCatching { LocalDate.parse(active.startDate) }.getOrNull()
        val doneById = userSteps.associateBy { it.stepId }
        val today = LocalDate.now()
        return steps.sortedBy { it.sort }.map { s ->
            ProcedureStatusCalculator.viewOf(
                step = s,
                startDate = start,
                completedAt = doneById[s.id]?.completedAt,
                today = today
            )
        }
    }

    /** Adapts the real repository (which has richer types) to the ViewModel's Repo surface. */
    private class DefaultRepo(private val real: ProcedureRepository) : Repo {
        override suspend fun fetchProcedures() = real.fetchProcedures()
        override suspend fun fetchSteps(code: String) = real.fetchSteps(code)
        override suspend fun getUserProcedure(code: String) = real.getUserProcedure(code)
        override suspend fun startProcedure(code: String, startDate: String) = real.startProcedure(code, startDate)
        override suspend fun fetchUserSteps(userProcedureId: String) = real.fetchUserSteps(userProcedureId)
        override suspend fun setStepCompleted(userProcedureId: String, stepId: String, completedAt: String?) =
            real.setStepCompleted(userProcedureId, stepId, completedAt)
    }

    companion object {
        /** Testing seam: build a VM against a fake Repo without going through AndroidViewModel construction. */
        fun newForTest(fake: Repo): ProcedureViewModel {
            // A minimal Application is fine — we don't touch context in these paths.
            return object : ProcedureViewModel(android.app.Application()) {
                init { this.setRepoForTest(fake) }
            }
        }
    }

    // Small hooks used only from tests. Kept private to production callers.
    private var testRepo: Repo? = null
    protected fun setRepoForTest(r: Repo) { testRepo = r }
    internal fun seedForTest(
        userProcedure: UserProcedure,
        steps: List<ProcedureStep>,
        userSteps: List<UserProcedureStep>
    ) {
        _activeProcedure.value = userProcedure
        _steps.value = viewsFor(steps, userSteps, userProcedure)
    }

    // Route real calls to the test repo when set. All call sites in this class use `effectiveRepo`
    // (load(), startProcedure(), toggleStep()) so tests can inject a fake without touching Supabase.
    private val effectiveRepo: Repo get() = testRepo ?: repo
}
