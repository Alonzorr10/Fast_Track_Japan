package com.example.fasttrackjapan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DocumentReminderCalculatorTest {

    private val today = LocalDate.of(2026, 7, 6)

    private fun doc(type: String, expiration: String, lead: Int) =
        ExpirationDocument(type = type, expirationDate = expiration, notificationLeadTime = lead, userId = "u")

    @Test
    fun includes_document_within_lead_window() {
        // expires in 10 days, lead 30 -> included
        val docs = listOf(doc("Residence Card", "2026-07-16", 30))
        assertEquals(1, DocumentReminderCalculator.documentsToRemind(docs, today).size)
    }

    @Test
    fun includes_document_expiring_today() {
        val docs = listOf(doc("Passport", "2026-07-06", 30))
        assertEquals(1, DocumentReminderCalculator.documentsToRemind(docs, today).size)
    }

    @Test
    fun includes_document_exactly_at_lead_boundary() {
        // expires in exactly 5 days, lead 5 -> included (inclusive)
        val docs = listOf(doc("Visa", "2026-07-11", 5))
        assertEquals(1, DocumentReminderCalculator.documentsToRemind(docs, today).size)
    }

    @Test
    fun excludes_document_beyond_lead_window() {
        // expires in 10 days but lead only 5 -> excluded
        val docs = listOf(doc("Visa", "2026-07-16", 5))
        assertTrue(DocumentReminderCalculator.documentsToRemind(docs, today).isEmpty())
    }

    @Test
    fun excludes_already_expired_document() {
        // expired yesterday -> excluded
        val docs = listOf(doc("Residence Card", "2026-07-05", 30))
        assertTrue(DocumentReminderCalculator.documentsToRemind(docs, today).isEmpty())
    }

    @Test
    fun excludes_document_with_unparseable_date() {
        val docs = listOf(doc("Broken", "not-a-date", 30))
        assertTrue(DocumentReminderCalculator.documentsToRemind(docs, today).isEmpty())
    }

    @Test
    fun filters_mixed_list_to_only_due_documents() {
        val docs = listOf(
            doc("A", "2026-07-10", 30), // in 4 days -> yes
            doc("B", "2026-09-01", 30), // in ~57 days -> no
            doc("C", "2026-07-06", 0),  // today, lead 0 -> yes
            doc("D", "2026-06-30", 30)  // expired -> no
        )
        val result = DocumentReminderCalculator.documentsToRemind(docs, today).map { it.type }
        assertEquals(listOf("A", "C"), result)
    }

    @Test
    fun daysUntil_returns_null_for_bad_date() {
        assertNull(DocumentReminderCalculator.daysUntil("nope", today))
    }
}
