package com.example.fasttrackjapan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object GarbageReminderScheduler {
    const val CHANNEL_ID = "garbage_reminders"
    const val WORK_NAME = "garbage_reminder_work"
    const val KEY_REMINDER_TIME = "reminderTime"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Garbage reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Reminds you the evening before a collection day" }
        manager.createNotificationChannel(channel)
    }

    /** (Re)schedule the daily reminder check to fire at [reminderTime] ("HH:mm"). */
    fun schedule(context: Context, reminderTime: String) {
        ensureChannel(context)
        val delayMillis = millisUntilNext(reminderTime)
        val request = OneTimeWorkRequestBuilder<GarbageReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putString(KEY_REMINDER_TIME, reminderTime).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private fun millisUntilNext(reminderTime: String): Long {
        val time = try {
            LocalTime.parse(reminderTime)
        } catch (e: Exception) {
            LocalTime.of(19, 0)
        }
        val now = LocalDateTime.now()
        var next = LocalDateTime.of(LocalDate.now(), time)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis()
    }
}
