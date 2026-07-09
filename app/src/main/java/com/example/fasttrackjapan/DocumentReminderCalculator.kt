package com.example.fasttrackjapan

import java.time.LocalDate
import java.time.temporal.ChronoUnit


object DocumentReminderCalculator {


    fun documentsToRemind(documents: List<ExpirationDocument>, today: LocalDate): List<ExpirationDocument> =
        documents.filter { doc ->
            val days = daysUntil(doc.expirationDate, today) ?: return@filter false
            days in 0..doc.notificationLeadTime.toLong()
        }

    fun daysUntil(expirationDate: String, today: LocalDate): Long? {
        val expiry = parseDateOrNull(expirationDate) ?: return null
        return ChronoUnit.DAYS.between(today, expiry)
    }

    private fun parseDateOrNull(value: String): LocalDate? = try {
        LocalDate.parse(value)
    } catch (e: Exception) {
        null
    }
}
