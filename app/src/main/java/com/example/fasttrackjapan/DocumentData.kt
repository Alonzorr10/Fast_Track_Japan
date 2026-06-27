package com.example.fasttrackjapan

import kotlinx.serialization.Serializable

@Serializable
data class ExpirationDocument(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: String, // e.g., "Residence Card", "My Number Card"
    val expirationDate: String, // YYYY-MM-DD
    val notificationLeadTime: Int, // e.g., 30 for 30 days before
    val userId: String? = null
)
