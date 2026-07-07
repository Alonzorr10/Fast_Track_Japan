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
    val weekday: Int,
    val weeksOfMonth: List<Int>? = null
)

@Serializable
data class GarbageUserSettings(
    val userId: String,
    val areaId: String,
    val reminderEnabled: Boolean = true,
    val reminderTime: String = "19:00",
    val updatedAt: String? = null
)

@Serializable
data class GarbageScheduleSnapshot(
    val area: GarbageArea,
    val categories: List<GarbageCategory>,
    val schedules: List<GarbageSchedule>
)

data class UpcomingCollection(
    val date: java.time.LocalDate,
    val categoryCodes: List<String>
)
