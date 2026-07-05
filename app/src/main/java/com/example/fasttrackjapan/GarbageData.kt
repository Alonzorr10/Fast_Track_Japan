package com.example.fasttrackjapan

import kotlinx.serialization.Serializable

@Serializable
data class GarbageWard(
    val code: String,
    val nameJa: String,
    val nameEn: String
)

@Serializable
data class GarbageArea(
    val id: String,
    val wardCode: String,
    val nameJa: String,
    val nameEn: String? = null,
    val postalCode: String? = null
)

@Serializable
data class GarbageCategory(
    val code: String,
    val nameJa: String,
    val nameEn: String,
    val isScheduled: Boolean = true,
    val sort: Int = 0
)

@Serializable
data class GarbageSchedule(
    val id: String = java.util.UUID.randomUUID().toString(),
    val areaId: String,
    val categoryCode: String,
    // ISO-8601 day of week: Monday = 1 ... Sunday = 7
    val weekday: Int,
    // Which occurrences within the month (1..5). null = every week.
    val weeksOfMonth: List<Int>? = null
)

@Serializable
data class GarbageUserSettings(
    val userId: String,
    val areaId: String,
    val reminderEnabled: Boolean = true,
    // Local time to fire the evening-before reminder, "HH:mm"
    val reminderTime: String = "19:00",
    val updatedAt: String? = null
)

/** Everything the app needs offline for one area: cached locally as JSON. */
@Serializable
data class GarbageScheduleSnapshot(
    val area: GarbageArea,
    val categories: List<GarbageCategory>,
    val schedules: List<GarbageSchedule>
)

/** UI/computed model: the categories collected on a given date. */
data class UpcomingCollection(
    val date: java.time.LocalDate,
    val categoryCodes: List<String>
)
