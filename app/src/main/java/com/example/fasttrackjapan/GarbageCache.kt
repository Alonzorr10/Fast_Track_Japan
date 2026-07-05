package com.example.fasttrackjapan

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Serialization for the on-disk schedule cache. File I/O lives in GarbageRepository. */
object GarbageCache {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(snapshot: GarbageScheduleSnapshot): String = json.encodeToString(snapshot)

    fun decode(text: String): GarbageScheduleSnapshot? = try {
        json.decodeFromString<GarbageScheduleSnapshot>(text)
    } catch (e: Exception) {
        null
    }
}
