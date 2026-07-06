package com.example.fasttrackjapan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDate

class GarbageReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reminderTime = inputData.getString(GarbageReminderScheduler.KEY_REMINDER_TIME) ?: "19:00"
        try {
            val snapshot = GarbageRepository(context).readCache()
            if (snapshot != null) {
                val tomorrow = LocalDate.now().plusDays(1)
                val codes = GarbageScheduleCalculator.collectionsOn(tomorrow, snapshot.schedules)
                if (codes.isNotEmpty()) {
                    notify(labelsFor(codes, snapshot.categories))
                }
            }
        } finally {
            // Always reschedule the next day's check.
            GarbageReminderScheduler.schedule(context, reminderTime)
        }
        return Result.success()
    }

    private fun labelsFor(codes: List<String>, categories: List<GarbageCategory>): String {
        val byCode = categories.associateBy { it.code }
        return codes.joinToString("、") { byCode[it]?.nameJa ?: it }
    }

    private fun notify(categoryLabels: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val notification = NotificationCompat.Builder(context, GarbageReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("明日のゴミ / Tomorrow's garbage")
            .setContentText("明日は $categoryLabels の日です。今夜出しましょう。")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(4001, notification)
    }
}
