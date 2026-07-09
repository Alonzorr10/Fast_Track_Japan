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


    private val fallbackWards = listOf(GarbageWard("131041", "新宿区", "Shinjuku City"))
    private val fallbackArea = GarbageArea("shinjuku-1", "131041", "西新宿1丁目", "Nishi-Shinjuku 1-chome")

    suspend fun fetchWards(): List<GarbageWard> = try {
        val list = Supabase.client.postgrest["garbage_wards"]
            .select { order("nameEn", Order.ASCENDING) }
            .decodeList<GarbageWard>()
        list.ifEmpty { fallbackWards }
    } catch (e: Exception) {
        Log.e("GarbageRepository", "Fetch wards failed: ${e.message}")
        fallbackWards
    }

    suspend fun fetchAreas(wardCode: String): List<GarbageArea> = try {
        Supabase.client.postgrest["garbage_areas"]
            .select {
                filter { eq("wardCode", wardCode) }
                order("nameJa", Order.ASCENDING)
            }
            .decodeList<GarbageArea>()
    } catch (e: Exception) {
        Log.e("GarbageRepository", "Fetch areas failed: ${e.message}")
        emptyList()
    }

    suspend fun fetchSnapshot(areaId: String): GarbageScheduleSnapshot = try {
        val area = Supabase.client.postgrest["garbage_areas"]
            .select { filter { eq("id", areaId) } }
            .decodeSingle<GarbageArea>()

        val categories = try {
            Supabase.client.postgrest["garbage_categories"]
                .select { order("sort", Order.ASCENDING) }
                .decodeList<GarbageCategory>()
        } catch (e: Exception) {
            Log.w("GarbageRepository", "Categories fetch failed, using defaults")
            defaultCategories()
        }

        val schedules = Supabase.client.postgrest["garbage_schedules"]
            .select { filter { eq("areaId", areaId) } }
            .decodeList<GarbageSchedule>()

        GarbageScheduleSnapshot(area, categories, schedules)
    } catch (e: Exception) {
        Log.e("GarbageRepository", "fetchSnapshot failed: ${e.message}")
        GarbageScheduleSnapshot(fallbackArea, defaultCategories(), emptyList())
    }

    private fun defaultCategories() = listOf(
        GarbageCategory("burnable", "可燃ごみ", "Burnable", true, 1),
        GarbageCategory("unburnable", "不燃ごみ", "Unburnable", true, 2),
        GarbageCategory("plastic", "プラスチック", "Plastic", true, 3),
        GarbageCategory("recyclable", "資源ごみ", "Recyclables", true, 4)
    )

    suspend fun getUserSettings(): GarbageUserSettings? {
        val user = Supabase.client.auth.currentUserOrNull() ?: return null
        return try {
            Supabase.client.postgrest["garbage_user_settings"]
                .select { filter { eq("userId", user.id) } }
                .decodeSingleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveUserSettings(settings: GarbageUserSettings) {
        try {
            Supabase.client.postgrest["garbage_user_settings"].upsert(settings)
        } catch (e: Exception) {
            Log.e("GarbageRepository", "Save settings failed: ${e.message}")
        }
    }

    fun writeCache(snapshot: GarbageScheduleSnapshot) {
        try {
            cacheFile.writeText(GarbageCache.encode(snapshot))
        } catch (e: Exception) {
            Log.e("GarbageRepository", "Cache write failed: ${e.message}")
        }
    }

    fun readCache(): GarbageScheduleSnapshot? = try {
        if (cacheFile.exists()) GarbageCache.decode(cacheFile.readText()) else null
    } catch (e: Exception) {
        null
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        if (cacheFile.exists()) cacheFile.delete()
    }
}