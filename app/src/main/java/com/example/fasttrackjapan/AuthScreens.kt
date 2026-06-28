package com.example.fasttrackjapan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import android.util.Log

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Log In", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Text("The Email or Password you inputted was incorrect. Please fill in the correct details", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    try {
                        Supabase.client.auth.signInWith(Email) {
                            this.email = email
                            this.password = password
                        }
                        onLoginSuccess()
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Login failed"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Login")
            }
        }

        TextButton(onClick = onBack) {
            Text("Back")
        }
    }
}

@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var fullName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var ward by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Sign Up", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        TextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = age,
            onValueChange = { age = it },
            label = { Text("Age") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = ward,
            onValueChange = { ward = it },
            label = { Text("Ward (e.g. Minato)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val ageInt = age.toIntOrNull()
                
                // 1. Client-side Validation
                if (fullName.isBlank() || ward.isBlank() || address.isBlank()) {
                    errorMessage = "Please fill in all fields."
                    return@Button
                }
                
                if (password.length < 6) {
                    errorMessage = "Password must be at least 6 characters long."
                    return@Button
                }
                
                if (ageInt == null || ageInt < 20) {
                    errorMessage = "You must be at least 20 years old to register."
                    return@Button
                }

                scope.launch {
                    isLoading = true
                    errorMessage = null
                    try {
                        Supabase.client.auth.signUpWith(Email) {
                            this.email = email
                            this.password = password
                        }
                        
                        val user = Supabase.client.auth.currentUserOrNull()
                        
                        if (user != null) {
                            val profile = UserProfile(
                                id = user.id,
                                email = email,
                                fullName = fullName,
                                age = ageInt,
                                ward = ward,
                                address = address,
                                updatedAt = java.time.Instant.now().toString()
                            )
                            Log.d("SignUp", "Creating profile for user ${user.id}: $profile")
                            Supabase.client.postgrest["profiles"].insert(profile)
                            onSignUpSuccess()
                        } else {
                            errorMessage = "This email has already been registered. Please log in instead or sign up with another email."
                        }
                    } catch (e: Exception) {
                        Log.e("SignUp", "Error: ${e.message}")
                        errorMessage = when {
                            e.message?.contains("already registered", ignoreCase = true) == true -> 
                                "This email is already in use."
                            e.message?.contains("valid email", ignoreCase = true) == true -> 
                                "Please enter a valid email address."
                            else -> "Sign up failed. Please check your connection and try again."
                        }
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Create Account")
            }
        }

        TextButton(onClick = onBack) {
            Text("Back")
        }
    }
}
