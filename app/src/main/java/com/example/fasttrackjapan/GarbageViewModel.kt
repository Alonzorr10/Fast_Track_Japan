package com.example.fasttrackjapan

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.time.LocalDate

class GarbageViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = GarbageRepository(app.applicationContext)

    var settings by mutableStateOf<GarbageUserSettings?>(null)
        private set
    var snapshot by mutableStateOf<GarbageScheduleSnapshot?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var initialLoadDone by mutableStateOf(false)
        private set

    val wards = mutableStateListOf<GarbageWard>()
    val areas = mutableStateListOf<GarbageArea>()

    val hasArea: Boolean get() = settings?.areaId != null

    val upcoming: List<UpcomingCollection>
        get() = snapshot?.let {
            GarbageScheduleCalculator.upcoming(LocalDate.now(), 14, it.schedules)
        } ?: emptyList()

    fun categoryLabel(code: String): String =
        snapshot?.categories?.firstOrNull { it.code == code }?.let { "${it.nameJa} (${it.nameEn})" } ?: code

    fun load() {
        snapshot = repo.readCache()
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val s = repo.getUserSettings()
                settings = s
                if (s != null) {
                    val fresh = repo.fetchSnapshot(s.areaId)
                    snapshot = fresh
                    repo.writeCache(fresh)
                }
            } catch (e: Exception) {
                Log.e("GarbageViewModel", "load failed: ${e.message}")
                errorMessage = "Couldn't load your schedule. Check your connection."
            } finally {
                isLoading = false
                initialLoadDone = true
            }
        }
    }

    fun loadWards() {
        viewModelScope.launch {
            try {
                val list = repo.fetchWards()
                wards.clear(); wards.addAll(list)
            } catch (e: Exception) {
                Log.e("GarbageViewModel", "loadWards failed: ${e.message}")
            }
        }
    }

    fun selectWard(wardCode: String) {
        viewModelScope.launch {
            try {
                val list = repo.fetchAreas(wardCode)
                areas.clear(); areas.addAll(list)
            } catch (e: Exception) {
                Log.e("GarbageViewModel", "selectWard failed: ${e.message}")
            }
        }
    }

    fun saveSetup(areaId: String, wardName: String, reminderEnabled: Boolean, reminderTime: String, onDone: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val user = Supabase.client.auth.currentUserOrNull()
                if (user == null) {
                    Log.e("GarbageViewModel", "saveSetup: no user")
                    return@launch
                }
                val newSettings = GarbageUserSettings(
                    userId = user.id,
                    areaId = areaId,
                    reminderEnabled = reminderEnabled,
                    reminderTime = reminderTime,
                    updatedAt = java.time.Instant.now().toString()
                )
                
                // 1. Save specific garbage settings
                repo.saveUserSettings(newSettings)
                settings = newSettings

                // 2. Sync ward name to user profile for consistency
                try {
                    Supabase.client.postgrest["profiles"].update(
                        mapOf("ward" to wardName)
                    ) {
                        filter {
                            eq("id", user.id)
                        }
                    }
                    Log.d("GarbageViewModel", "Updated user profile ward to: $wardName")
                } catch (e: Exception) {
                    Log.e("GarbageViewModel", "Failed to sync ward to profile: ${e.message}")
                }

                val fresh = repo.fetchSnapshot(areaId)
                snapshot = fresh
                repo.writeCache(fresh)

                val ctx = getApplication<Application>().applicationContext
                if (reminderEnabled) {
                    GarbageReminderScheduler.schedule(ctx, reminderTime)
                } else {
                    GarbageReminderScheduler.cancel(ctx)
                }
                onDone()
            } catch (e: Exception) {
                Log.e("GarbageViewModel", "saveSetup failed: ${e.message}")
                errorMessage = "Couldn't save. Check your connection and try again."
            } finally {
                isLoading = false
            }
        }
    }

    /** Reset all garbage state on sign-out so nothing leaks to the next user. */
    fun clear() {
        settings = null
        snapshot = null
        wards.clear()
        areas.clear()
        errorMessage = null
        initialLoadDone = false
        val ctx = getApplication<Application>().applicationContext
        GarbageReminderScheduler.cancel(ctx)
        viewModelScope.launch { repo.clearCache() }
    }
}
