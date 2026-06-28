package com.example.fasttrackjapan

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String, // Matches Supabase Auth UID
    val email: String,
    val fullName: String = "",
    val age: Int? = null,
    val address: String = "",
    val ward: String = "",
    val profilePictureUrl: String? = null,
    val updatedAt: String? = null
)
