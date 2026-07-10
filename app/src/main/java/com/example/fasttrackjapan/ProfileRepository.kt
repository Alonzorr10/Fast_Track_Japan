package com.example.fasttrackjapan

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Owns Supabase reads/writes and Storage upload for the profile screen. */
class ProfileRepository {

    /** Load the current user's profile row, or null if not signed in / no row yet. */
    suspend fun fetchProfile(): UserProfile? {
        val user = Supabase.client.auth.currentUserOrNull() ?: return null
        return Supabase.client.postgrest["profiles"]
            .select { filter { eq("id", user.id) } }
            .decodeSingleOrNull()
    }

    /** Optional avatar upload — returns the public URL when a bytes upload succeeded. */
    suspend fun uploadAvatar(context: Context, uri: Uri): String? {
        val user = Supabase.client.auth.currentUserOrNull() ?: return null
        val fileName = "${user.id}/avatar.jpg"
        val bytes = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } ?: return null
        Supabase.client.storage["profiles"].upload(fileName, bytes) { upsert = true }
        return Supabase.client.storage["profiles"].publicUrl(fileName)
    }

    /** Upsert the profile row. Returns the row on success. */
    suspend fun upsertProfile(profile: UserProfile): UserProfile {
        Supabase.client.postgrest["profiles"].upsert(profile)
        return profile
    }
}
