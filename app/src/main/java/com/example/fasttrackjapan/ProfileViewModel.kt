package com.example.fasttrackjapan

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel : ViewModel() {
    var profile by mutableStateOf<UserProfile?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    fun fetchProfile() {
        Log.d("ProfileViewModel", "Fetching profile...")
        viewModelScope.launch {
            isLoading = true
            try {
                val user = Supabase.client.auth.currentUserOrNull()
                if (user != null) {
                    Log.d("ProfileViewModel", "Fetching profile")
                    val result = Supabase.client.postgrest["profiles"]
                        .select {
                            filter {
                                eq("id", user.id)
                            }
                        }
                        .decodeSingleOrNull<UserProfile>()
                    
                    Log.d("ProfileViewModel", "Fetch complete (found=${result != null})")
                    profile = result
                } else {
                    Log.e("ProfileViewModel", "Cannot fetch profile: No user logged in")
                }
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

                // Upload image if provided
                if (profilePictureUri != null) {
                    Log.d("ProfileViewModel", "Uploading new profile picture...")
                    val fileName = "${user.id}/avatar.jpg"
                    val bytes = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(profilePictureUri)?.use { it.readBytes() }
                    }
                    if (bytes != null) {
                        Supabase.client.storage["profiles"].upload(fileName, bytes) {
                            upsert = true
                        }
                        imageUrl = Supabase.client.storage["profiles"].publicUrl(fileName)
                        Log.d("ProfileViewModel", "Picture uploaded")
                    }
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
                Supabase.client.postgrest["profiles"].upsert(newProfile)
                Log.d("ProfileViewModel", "Profile saved successfully")
                profile = newProfile
                
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
}
