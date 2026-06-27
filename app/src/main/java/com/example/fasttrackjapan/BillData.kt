package com.example.fasttrackjapan

import kotlinx.serialization.Serializable

@Serializable
data class Bill(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String,
    val date: String,
    val imageUrl: String, // Public URL or Storage path
    val userId: String? = null
)
