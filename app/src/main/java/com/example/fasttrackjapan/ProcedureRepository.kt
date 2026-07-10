package com.example.fasttrackjapan

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class ProcedureRepository {

    suspend fun fetchProcedures(): List<Procedure> =
        Supabase.client.postgrest["procedures"]
            .select { order("sort", Order.ASCENDING) }
            .decodeList()

    suspend fun fetchSteps(procedureCode: String): List<ProcedureStep> =
        Supabase.client.postgrest["procedure_steps"]
            .select {
                filter { eq("procedureCode", procedureCode) }
                order("sort", Order.ASCENDING)
            }
            .decodeList()

    suspend fun getUserProcedure(procedureCode: String): UserProcedure? {
        val user = Supabase.client.auth.currentUserOrNull() ?: return null
        return Supabase.client.postgrest["user_procedures"]
            .select {
                filter {
                    eq("userId", user.id)
                    eq("procedureCode", procedureCode)
                }
            }
            .decodeSingleOrNull()
    }

    suspend fun startProcedure(procedureCode: String, startDate: String): UserProcedure? {
        val user = Supabase.client.auth.currentUserOrNull() ?: return null
        val row = UserProcedure(
            userId = user.id,
            procedureCode = procedureCode,
            startDate = startDate,
            createdAt = java.time.Instant.now().toString()
        )
        Supabase.client.postgrest["user_procedures"].upsert(row) { onConflict = "userId,procedureCode" }
        // Re-read so callers get the DB-authoritative id (in case the row already existed).
        return getUserProcedure(procedureCode)
    }

    suspend fun fetchUserSteps(userProcedureId: String): List<UserProcedureStep> =
        Supabase.client.postgrest["user_procedure_steps"]
            .select { filter { eq("userProcedureId", userProcedureId) } }
            .decodeList()

    suspend fun setStepCompleted(userProcedureId: String, stepId: String, completedAt: String?) {
        val row = UserProcedureStep(userProcedureId = userProcedureId, stepId = stepId, completedAt = completedAt)
        try {
            Supabase.client.postgrest["user_procedure_steps"].upsert(row)
        } catch (e: Exception) {
            // Rethrow so the ViewModel can roll back its optimistic toggle.
            Log.e("ProcedureRepository", "setStepCompleted failed: ${e.message}")
            throw e
        }
    }
}
