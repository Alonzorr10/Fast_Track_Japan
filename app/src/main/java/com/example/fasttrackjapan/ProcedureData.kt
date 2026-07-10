package com.example.fasttrackjapan

import kotlinx.serialization.Serializable

@Serializable
data class Procedure(
    val code: String,
    val nameEn: String,
    val nameJa: String,
    val description: String,
    val sort: Int = 0
)

@Serializable
data class ProcedureStep(
    val id: String,
    val procedureCode: String,
    val sort: Int,
    val titleEn: String,
    val titleJa: String,
    val description: String,
    val linkUrl: String? = null,
    val deadlineDaysFromStart: Int? = null,
    val reminderLeadDays: Int = 3
)

@Serializable
data class UserProcedure(
    val id: String = java.util.UUID.randomUUID().toString(),
    val userId: String,
    val procedureCode: String,
    val startDate: String, // YYYY-MM-DD
    val createdAt: String? = null
)

@Serializable
data class UserProcedureStep(
    val userProcedureId: String,
    val stepId: String,
    val completedAt: String? = null
)

enum class ProcedureStatus { UPCOMING, DUE_SOON, OVERDUE, DONE }

/** UI view of a step: the reference step plus computed status/date. */
data class ProcedureStepView(
    val step: ProcedureStep,
    val dueDate: java.time.LocalDate?,
    val status: ProcedureStatus,
    /**
     * Positive = days until due; negative = days overdue (i.e. `today - dueDate`);
     * null when there is no due date (no deadline or procedure not started).
     */
    val daysDelta: Long?,
    val completedAt: String?
)
