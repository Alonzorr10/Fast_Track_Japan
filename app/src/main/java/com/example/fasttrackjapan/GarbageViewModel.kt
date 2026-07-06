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

    /** Non-empty once the user has completed setup. */
    val hasArea: Boolean get() = settings?.areaId != null

    /** Upcoming collections for the next 14 days, from the cached/loaded snapshot. */
    val upcoming: List<UpcomingCollection>
        get() = snapshot?.let {
            GarbageScheduleCalculator.upcoming(LocalDate.now(), 14, it.schedules)
        } ?: emptyList()

    fun categoryLabel(code: String): String =
        snapshot?.categories?.firstOrNull { it.code == code }?.let { "${it.nameJa} (${it.nameEn})" } ?: code

    /** Called when the screen opens: load settings + cached snapshot, refresh from network. */
    fun load() {
        // Show cache immediately for offline/instant render.
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

    /** Persist setup, cache the schedule, and (re)schedule reminders. onDone runs on success. */
    fun saveSetup(areaId: String, reminderEnabled: Boolean, reminderTime: String, onDone: () -> Unit) {
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
                repo.saveUserSettings(newSettings)
                settings = newSettings

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
