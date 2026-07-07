package com.example.fasttrackjapan

import kotlinx.serialization.Serializable

@Serializable
data class ExpirationDocument(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: String,
    val expirationDate: String,
    val notificationLeadTime: Int,
    val userId: String? = null
)
