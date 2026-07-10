package com.example.fasttrackjapan

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Pure status logic for procedure steps. No Android dependencies so it is fully
 * unit-testable. All date arithmetic uses whole days at local midnight.
 */
object ProcedureStatusCalculator {

    /**
     * The full UI view for a step, from the reference step + the user's context.
     * daysDelta convention: positive = days until due; negative = days overdue;
     * null when there is no due date (no deadline, or procedure not started).
     */
    fun viewOf(
        step: ProcedureStep,
        startDate: LocalDate?,
        completedAt: String?,
        today: LocalDate
    ): ProcedureStepView {
        if (completedAt != null) {
            return ProcedureStepView(step, dueDate = null, status = ProcedureStatus.DONE, daysDelta = null, completedAt = completedAt)
        }
        val deadline = step.deadlineDaysFromStart
        if (startDate == null || deadline == null) {
            return ProcedureStepView(step, dueDate = null, status = ProcedureStatus.UPCOMING, daysDelta = null, completedAt = null)
        }
        val dueDate = startDate.plusDays(deadline.toLong())
        val daysUntil = ChronoUnit.DAYS.between(today, dueDate)
        val status = when {
            daysUntil < 0L -> ProcedureStatus.OVERDUE
            daysUntil <= step.reminderLeadDays.toLong() -> ProcedureStatus.DUE_SOON
            else -> ProcedureStatus.UPCOMING
        }
        return ProcedureStepView(step, dueDate = dueDate, status = status, daysDelta = daysUntil, completedAt = null)
    }

    /**
     * Steps that should trigger a reminder today for one user procedure.
     *
     * Rule:
     *   - include if today is inside the pre-deadline lead window
     *     (dueDate - reminderLeadDays <= today <= dueDate); OR
     *   - include if today is exactly one day past the due date (first day overdue).
     * Never include completed steps, steps without a deadline, or steps that have
     * been overdue for more than one day (avoids spam for a retroactive startDate).
     *
     * The caller passes `startDatesByStepId` — usually every step gets the same
     * `user_procedure.startDate`, but the map keeps the pure function decoupled
     * from that assumption.
     */
    fun stepsToRemind(
        steps: List<ProcedureStep>,
        userSteps: List<UserProcedureStep>,
        userProcedureId: String,
        startDatesByStepId: Map<String, LocalDate>,
        today: LocalDate
    ): List<ProcedureStep> {
        val completedIds = userSteps
            .filter { it.userProcedureId == userProcedureId && it.completedAt != null }
            .map { it.stepId }
            .toSet()
        return steps.filter { step ->
            if (step.id in completedIds) return@filter false
            val deadline = step.deadlineDaysFromStart ?: return@filter false
            val start = startDatesByStepId[step.id] ?: return@filter false
            val dueDate = start.plusDays(deadline.toLong())
            val delta = ChronoUnit.DAYS.between(today, dueDate) // + upcoming, - overdue
            val inLeadWindow = delta in 0L..step.reminderLeadDays.toLong()
            val freshlyOverdue = delta == -1L
            inLeadWindow || freshlyOverdue
        }
    }
}
