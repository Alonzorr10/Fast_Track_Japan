package com.example.fasttrackjapan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ProcedureStatusCalculatorTest {

    private val today = LocalDate.of(2026, 7, 10)

    private fun step(sort: Int = 1, deadline: Int? = 14, lead: Int = 3) = ProcedureStep(
        id = "s$sort",
        procedureCode = "MOVING_IN",
        sort = sort,
        titleEn = "Step $sort",
        titleJa = "手順 $sort",
        description = "",
        deadlineDaysFromStart = deadline,
        reminderLeadDays = lead
    )

    @Test
    fun no_startDate_is_upcoming_with_no_due_date() {
        val v = ProcedureStatusCalculator.viewOf(step(), startDate = null, completedAt = null, today = today)
        assertEquals(ProcedureStatus.UPCOMING, v.status)
        assertNull(v.dueDate)
        assertNull(v.daysDelta)
    }

    @Test
    fun no_deadline_is_upcoming_with_no_due_date() {
        val v = ProcedureStatusCalculator.viewOf(
            step(deadline = null),
            startDate = LocalDate.of(2026, 7, 1), completedAt = null, today = today
        )
        assertEquals(ProcedureStatus.UPCOMING, v.status)
        assertNull(v.dueDate)
        assertNull(v.daysDelta)
    }

    @Test
    fun completed_is_done_regardless_of_date() {
        val v = ProcedureStatusCalculator.viewOf(
            step(deadline = 14),
            startDate = LocalDate.of(2026, 6, 1), // dueDate = 2026-06-15 (overdue)
            completedAt = "2026-06-10T10:00:00Z",
            today = today
        )
        assertEquals(ProcedureStatus.DONE, v.status)
    }

    @Test
    fun upcoming_when_outside_lead_window() {
        // start 2026-07-01 + deadline 14 = due 2026-07-15; today 2026-07-10; lead 3 -> DUE_SOON window is [07-12, 07-15]
        // Move today back to 2026-07-11 -> UPCOMING
        val v = ProcedureStatusCalculator.viewOf(
            step(deadline = 14, lead = 3),
            startDate = LocalDate.of(2026, 7, 1),
            completedAt = null,
            today = LocalDate.of(2026, 7, 11)
        )
        assertEquals(ProcedureStatus.UPCOMING, v.status)
        assertEquals(4L, v.daysDelta)
        assertEquals(LocalDate.of(2026, 7, 15), v.dueDate)
    }

    @Test
    fun due_soon_at_lower_boundary() {
        // dueDate 07-15, lead 3 -> window starts 07-12. today = 07-12 -> DUE_SOON
        val v = ProcedureStatusCalculator.viewOf(
            step(deadline = 14, lead = 3),
            startDate = LocalDate.of(2026, 7, 1),
            completedAt = null,
            today = LocalDate.of(2026, 7, 12)
        )
        assertEquals(ProcedureStatus.DUE_SOON, v.status)
        assertEquals(3L, v.daysDelta)
    }

    @Test
    fun due_soon_on_due_date() {
        val v = ProcedureStatusCalculator.viewOf(
            step(deadline = 14, lead = 3),
            startDate = LocalDate.of(2026, 7, 1),
            completedAt = null,
            today = LocalDate.of(2026, 7, 15)
        )
        assertEquals(ProcedureStatus.DUE_SOON, v.status)
        assertEquals(0L, v.daysDelta)
    }

    @Test
    fun overdue_one_day_after_due_date() {
        val v = ProcedureStatusCalculator.viewOf(
            step(deadline = 14, lead = 3),
            startDate = LocalDate.of(2026, 7, 1),
            completedAt = null,
            today = LocalDate.of(2026, 7, 16)
        )
        assertEquals(ProcedureStatus.OVERDUE, v.status)
        assertEquals(-1L, v.daysDelta)
    }

    @Test
    fun overdue_many_days_after_due_date() {
        val v = ProcedureStatusCalculator.viewOf(
            step(deadline = 14, lead = 3),
            startDate = LocalDate.of(2026, 6, 1),   // dueDate = 06-15
            completedAt = null,
            today = LocalDate.of(2026, 7, 10)
        )
        assertEquals(ProcedureStatus.OVERDUE, v.status)
        assertEquals(-25L, v.daysDelta)
    }

    @Test
    fun retroactive_startDate_yields_overdue_immediately() {
        // A user setting up "moved in 20 days ago" should see the 14-day step already overdue.
        val v = ProcedureStatusCalculator.viewOf(
            step(deadline = 14, lead = 3),
            startDate = today.minusDays(20),
            completedAt = null,
            today = today
        )
        assertEquals(ProcedureStatus.OVERDUE, v.status)
        assertEquals(-6L, v.daysDelta)
    }

    @Test
    fun stepsToRemind_includes_due_soon_and_freshly_overdue_only() {
        // 3 steps: one DUE_SOON (in window), one just-overdue (1 day past), one 2 days past.
        val s1 = step(sort = 1, deadline = 5, lead = 3)     // due today+? — use startDate to control
        val s2 = step(sort = 2, deadline = 5, lead = 3)
        val s3 = step(sort = 3, deadline = 5, lead = 3)
        // start dates chosen so today=07-10:
        //  s1 dueDate = 07-13 (DUE_SOON at 07-10, lead 3 -> window 07-10..07-13)
        //  s2 dueDate = 07-09 (today-1) -> 1 day overdue (include)
        //  s3 dueDate = 07-08 (today-2) -> 2 days overdue (exclude)
        val steps = listOf(s1, s2, s3)
        val userSteps = emptyList<UserProcedureStep>() // none checked
        val remind = ProcedureStatusCalculator.stepsToRemind(
            steps = steps,
            userSteps = userSteps,
            userProcedureId = "up1",
            startDatesByStepId = mapOf(
                "s1" to LocalDate.of(2026, 7, 8),
                "s2" to LocalDate.of(2026, 7, 4),
                "s3" to LocalDate.of(2026, 7, 3),
            ),
            today = LocalDate.of(2026, 7, 10)
        ).map { it.id }.toSet()
        assertEquals(setOf("s1", "s2"), remind)
    }

    @Test
    fun stepsToRemind_skips_completed() {
        val s1 = step(sort = 1, deadline = 5, lead = 3)
        val userSteps = listOf(UserProcedureStep("up1", "s1", completedAt = "2026-07-05T00:00:00Z"))
        val remind = ProcedureStatusCalculator.stepsToRemind(
            steps = listOf(s1),
            userSteps = userSteps,
            userProcedureId = "up1",
            startDatesByStepId = mapOf("s1" to LocalDate.of(2026, 7, 8)),
            today = LocalDate.of(2026, 7, 10)
        )
        assertEquals(emptyList<ProcedureStep>(), remind)
    }
}
