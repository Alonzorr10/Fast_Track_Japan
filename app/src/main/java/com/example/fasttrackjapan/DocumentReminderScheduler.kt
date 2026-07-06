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

/**
 * Schedules a once-daily check that fires reminders for documents nearing expiry.
 * Reuses WorkManager (already a project dependency); reschedules itself each run.
 */
object DocumentReminderScheduler {
    const val CHANNEL_ID = "document_reminders"
    const val WORK_NAME = "document_reminder_work"
    private const val REMINDER_HOUR = 9

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Document expiry reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Reminds you before a tracked document expires" }
        manager.createNotificationChannel(channel)
    }

    /** (Re)schedule the daily check for ~09:00 local time. */
    fun schedule(context: Context) {
        ensureChannel(context)
        val request = OneTimeWorkRequestBuilder<DocumentReminderWorker>()
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
