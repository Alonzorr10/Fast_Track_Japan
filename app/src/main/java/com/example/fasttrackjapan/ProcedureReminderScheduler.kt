package com.example.fasttrackjapan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Daily WorkManager check that fires reminders for procedure steps in the lead window. */
object ProcedureReminderScheduler {
    const val CHANNEL_ID = "procedure_reminders"
    const val WORK_NAME = "procedure_reminder_work"
    private const val REMINDER_HOUR = 9

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Procedure step reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Reminds you before a step in a guided procedure is due" }
        manager.createNotificationChannel(channel)
    }

    fun schedule(context: Context) {
        ensureChannel(context)
        val request = OneTimeWorkRequestBuilder<ProcedureReminderWorker>()
            .setInitialDelay(millisUntilNextRun(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private fun millisUntilNextRun(): Long {
        val now = LocalDateTime.now()
        var next = LocalDateTime.of(LocalDate.now(), LocalTime.of(REMINDER_HOUR, 0))
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis()
    }
}
