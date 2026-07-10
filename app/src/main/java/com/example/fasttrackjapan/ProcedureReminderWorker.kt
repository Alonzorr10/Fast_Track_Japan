package com.example.fasttrackjapan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDate

class ProcedureReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val repo = ProcedureRepository()
            // v1 has one procedure; if we add more, iterate over all user_procedures for this user.
            val active = repo.getUserProcedure("MOVING_IN")
            if (active != null) {
                val steps = repo.fetchSteps("MOVING_IN")
                val userSteps = repo.fetchUserSteps(active.id)
                val start = try { LocalDate.parse(active.startDate) } catch (e: Exception) { null }
                if (start != null) {
                    val today = LocalDate.now()
                    val toRemind = ProcedureStatusCalculator.stepsToRemind(
                        steps = steps,
                        userSteps = userSteps,
                        userProcedureId = active.id,
                        startDatesByStepId = steps.associate { it.id to start },
                        today = today
                    )
                    toRemind.forEach { notifyFor(it, start, today) }
                }
            }
        } catch (e: Exception) {
            Log.e("ProcedureReminderWorker", "check failed: ${e.message}")
        } finally {
            ProcedureReminderScheduler.schedule(context)
        }
        return Result.success()
    }

    private fun notifyFor(step: ProcedureStep, start: LocalDate, today: LocalDate) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val deadline = step.deadlineDaysFromStart ?: return
        val dueDate = start.plusDays(deadline.toLong())
        val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, dueDate)

        val (title, body) = when {
            daysUntil == -1L ->
                "書類の期限 / Procedure step overdue" to "${step.titleEn} — 1 day overdue"
            daysUntil == 0L ->
                "書類の期限 / Procedure step due today" to "${step.titleEn} — due today"
            daysUntil > 0L ->
                "書類の期限 / Procedure step due soon" to "${step.titleEn} — in $daysUntil days"
            else -> return
        }

        val notification = NotificationCompat.Builder(context, ProcedureReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(step.id.hashCode(), notification)
    }
}
