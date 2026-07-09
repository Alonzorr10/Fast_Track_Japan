package com.example.fasttrackjapan

import kotlinx.serialization.json.Json

object GarbageCache {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(snapshot: GarbageScheduleSnapshot): String = json.encodeToString(snapshot)

    fun decode(text: String): GarbageScheduleSnapshot? = try {
        json.decodeFromString<GarbageScheduleSnapshot>(text)
    } catch (e: Exception) {
        null
    }
}
