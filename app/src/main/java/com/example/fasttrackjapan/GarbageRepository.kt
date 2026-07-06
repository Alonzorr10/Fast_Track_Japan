package com.example.fasttrackjapan

import android.content.Context
import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class GarbageRepository(private val context: Context) {

    private val cacheFile: File get() = File(context.filesDir, "garbage_cache.json")

    suspend fun fetchWards(): List<GarbageWard> =
        Supabase.client.postgrest["garbage_wards"]
            .select { order("nameEn", Order.ASCENDING) }
            .decodeList()

    suspend fun fetchAreas(wardCode: String): List<GarbageArea> =
        Supabase.client.postgrest["garbage_areas"]
            .select {
                filter { eq("wardCode", wardCode) }
                order("nameJa", Order.ASCENDING)
            }
            .decodeList()

    suspend fun fetchSnapshot(areaId: String): GarbageScheduleSnapshot {
        val area = Supabase.client.postgrest["garbage_areas"]
            .select { filter { eq("id", areaId) } }
            .decodeSingle<GarbageArea>()
        val categories = Supabase.client.postgrest["garbage_categories"]
            .select { order("sort", Order.ASCENDING) }
            .decodeList<GarbageCategory>()
        val schedules = Supabase.client.postgrest["garbage_schedules"]
            .select { filter { eq("areaId", areaId) } }
            .decodeList<GarbageSchedule>()
        return GarbageScheduleSnapshot(area, categories, schedules)
    }

    suspend fun getUserSettings(): GarbageUserSettings? {
        val user = Supabase.client.auth.currentUserOrNull() ?: return null
        return Supabase.client.postgrest["garbage_user_settings"]
            .select { filter { eq("userId", user.id) } }
            .decodeSingleOrNull()
    }

    suspend fun saveUserSettings(settings: GarbageUserSettings) {
        Supabase.client.postgrest["garbage_user_settings"].upsert(settings)
    }

    fun writeCache(snapshot: GarbageScheduleSnapshot) {
        try {
            cacheFile.writeText(GarbageCache.encode(snapshot))
        } catch (e: Exception) {
            Log.e("GarbageRepository", "Failed to write cache: ${e.message}")
        }
    }

    fun readCache(): GarbageScheduleSnapshot? = try {
        if (cacheFile.exists()) GarbageCache.decode(cacheFile.readText()) else null
    } catch (e: Exception) {
        Log.e("GarbageRepository", "Failed to read cache: ${e.message}")
        null
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        if (cacheFile.exists()) cacheFile.delete()
    }
}
