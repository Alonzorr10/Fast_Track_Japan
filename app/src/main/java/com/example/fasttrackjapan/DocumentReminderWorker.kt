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
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import java.time.LocalDate

class DocumentReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val user = Supabase.client.auth.currentUserOrNull()
            if (user != null) {
                val docs = Supabase.client.postgrest["documents"]
                    .select { filter { eq("userId", user.id) } }
                    .decodeList<ExpirationDocument>()
                val today = LocalDate.now()
                DocumentReminderCalculator.documentsToRemind(docs, today).forEach { doc ->
                    notifyFor(doc, DocumentReminderCalculator.daysUntil(doc.expirationDate, today))
                }
            }
        } catch (e: Exception) {
            Log.e("DocumentReminderWorker", "check failed: ${e.message}")
        } finally {
            // Always reschedule the next day's check.
            DocumentReminderScheduler.schedule(context)
        }
        return Result.success()
    }

    private fun notifyFor(doc: ExpirationDocument, daysUntil: Long?) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val whenText = when {
            daysUntil == null -> "soon"
            daysUntil == 0L -> "today"
            daysUntil == 1L -> "tomorrow"
            else -> "in $daysUntil days"
        }

        val notification = NotificationCompat.Builder(context, DocumentReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("書類の有効期限 / Document expiring")
            .setContentText("${doc.type} expires $whenText (${doc.expirationDate}).")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        // Stable per-document id so re-runs update rather than stack duplicates.
        NotificationManagerCompat.from(context).notify(doc.id.hashCode(), notification)
    }
}
