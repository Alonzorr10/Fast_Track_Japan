package com.example.fasttrackjapan

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

/**
 * Owns all Supabase access for tracked expiration documents.
 * `DocumentViewModel` uses this via a small `Repo` interface so tests can supply a fake.
 */
class DocumentRepository {

    /** Documents belonging to the currently-authenticated user, or empty if not signed in. */
    suspend fun fetchDocuments(): List<ExpirationDocument> {
        val user = Supabase.client.auth.currentUserOrNull() ?: return emptyList()
        return Supabase.client.postgrest["documents"]
            .select { filter { eq("userId", user.id) } }
            .decodeList()
    }

    /** Insert a new document scoped to the current user. Returns the persisted row, or null if not signed in. */
    suspend fun addDocument(type: String, expirationDate: String, notificationLeadTime: Int): ExpirationDocument? {
        val user = Supabase.client.auth.currentUserOrNull() ?: return null
        val newDoc = ExpirationDocument(
            type = type,
            expirationDate = expirationDate,
            notificationLeadTime = notificationLeadTime,
            userId = user.id
        )
        Supabase.client.postgrest["documents"].insert(newDoc)
        return newDoc
    }

    suspend fun deleteDocument(id: String) {
        Supabase.client.postgrest["documents"].delete { filter { eq("id", id) } }
    }

    suspend fun updateDocument(doc: ExpirationDocument) {
        Supabase.client.postgrest["documents"].update(doc) { filter { eq("id", doc.id) } }
    }
}
