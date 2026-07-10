package com.example.fasttrackjapan

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    /** Minimal repo surface for testability. `ProfileRepository` implements it in production. */
    interface Repo {
        suspend fun fetchProfile(): UserProfile?
        suspend fun uploadAvatar(context: Context, uri: Uri): String?
        suspend fun upsertProfile(profile: UserProfile): UserProfile
    }

    private val repo: Repo = DefaultRepo(ProfileRepository())
    private var testRepo: Repo? = null
    protected fun setRepoForTest(r: Repo) { testRepo = r }
    private val effectiveRepo: Repo get() = testRepo ?: repo

    var profile by mutableStateOf<UserProfile?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set

    /** Reset in-memory state on sign-out so the previous user's profile does not leak into the next session. */
    fun clear() {
        profile = null
        isLoading = false
    }

    fun fetchProfile() {
        Log.d("ProfileViewModel", "Fetching profile")
        viewModelScope.launch {
            isLoading = true
            try {
                val result = effectiveRepo.fetchProfile()
                Log.d("ProfileViewModel", "Fetch complete (found=${result != null})")
                profile = result
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error fetching profile: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun saveProfile(
        fullName: String,
        age: Int?,
        address: String,
        ward: String,
        profilePictureUri: Uri?,
        context: Context,
        onSuccess: () -> Unit
    ) {
        Log.d("ProfileViewModel", "Saving profile")
        viewModelScope.launch {
            isLoading = true
            try {
                val user = Supabase.client.auth.currentUserOrNull() ?: return@launch
                var imageUrl = profile?.profilePictureUrl
                if (profilePictureUri != null) {
                    Log.d("ProfileViewModel", "Uploading new profile picture")
                    val uploaded = effectiveRepo.uploadAvatar(context, profilePictureUri)
                    if (uploaded != null) imageUrl = uploaded
                }
                val newProfile = UserProfile(
                    id = user.id,
                    email = user.email ?: "",
                    fullName = fullName,
                    age = age,
                    address = address,
                    ward = ward,
                    profilePictureUrl = imageUrl,
                    updatedAt = java.time.Instant.now().toString()
                )
                Log.d("ProfileViewModel", "Upserting profile")
                profile = effectiveRepo.upsertProfile(newProfile)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Profile updated!", android.widget.Toast.LENGTH_SHORT).show()
                }
                onSuccess()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error saving profile: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    val userFriendlyError = when {
                        e.message?.contains("policy", ignoreCase = true) == true -> "Permission denied. Please check your account settings."
                        e.message?.contains("network", ignoreCase = true) == true -> "Connection error. Please check your internet."
                        else -> "Could not save profile. Please try again."
                    }
                    android.widget.Toast.makeText(context, userFriendlyError, android.widget.Toast.LENGTH_LONG).show()
                }
            } finally {
                isLoading = false
            }
        }
    }

    /** Adapts the real repository to the ViewModel's Repo surface. */
    private class DefaultRepo(private val real: ProfileRepository) : Repo {
        override suspend fun fetchProfile() = real.fetchProfile()
        override suspend fun uploadAvatar(context: Context, uri: Uri) = real.uploadAvatar(context, uri)
        override suspend fun upsertProfile(profile: UserProfile) = real.upsertProfile(profile)
    }

    companion object {
        /** Testing seam: build a VM against a fake Repo. */
        fun newForTest(fake: Repo): ProfileViewModel {
            return object : ProfileViewModel(Application()) {
                init { this.setRepoForTest(fake) }
            }
        }
    }
}
