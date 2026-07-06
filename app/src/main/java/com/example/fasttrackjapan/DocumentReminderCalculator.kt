package com.example.fasttrackjapan

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Pure logic for deciding which tracked documents should trigger an expiration
 * reminder. No Android dependencies, so it is fully unit-testable.
 */
object DocumentReminderCalculator {

    /**
     * Documents whose expiration falls within their notification lead-time window
     * and has not yet passed — i.e. 0 <= daysUntilExpiry <= notificationLeadTime.
     */
    fun documentsToRemind(documents: List<ExpirationDocument>, today: LocalDate): List<ExpirationDocument> =
        documents.filter { doc ->
            val days = daysUntil(doc.expirationDate, today) ?: return@filter false
            days in 0..doc.notificationLeadTime.toLong()
        }

    /** Whole days from [today] to the document's expiration date, or null if unparseable. */
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
