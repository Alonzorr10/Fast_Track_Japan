package com.example.fasttrackjapan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fasttrackjapan.ui.theme.FastTrackJapanTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.LaunchedEffect
import io.github.jan.supabase.auth.auth
import android.util.Log
import kotlinx.coroutines.launch


import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FastTrackJapanTheme {
                val navController = rememberNavController()
                val viewModel: BillViewModel = viewModel()

                // Session persistence check
                LaunchedEffect(Unit) {
                    try {
                        val session = Supabase.client.auth.currentSessionOrNull()
                        if (session != null) {
                            Log.d("SupabaseTest", "Session found, navigating to main_menu")
                            viewModel.fetchBills()
                            navController.navigate("main_menu") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SupabaseTest", "Session check failed: ${e.message}")
                    }
                }

                NavHost(navController = navController, startDestination = "welcome") {
                    composable("welcome") {
                        WelcomeScreen(
                            onLogin = { navController.navigate("login") },
                            onSignUp = { navController.navigate("signup") },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = { 
                                viewModel.fetchBills() // Refresh bills after login
                                navController.navigate("main_menu") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("signup") {
                        SignUpScreen(
                            onSignUpSuccess = { 
                                viewModel.fetchBills() // Refresh bills after signup
                                navController.navigate("main_menu") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("main_menu") {
                        val scope = androidx.compose.runtime.rememberCoroutineScope()
                        MainMenuScreen(
                            onBillTrackerClick = { navController.navigate("bills_menu") },
                            onSignOutClick = {
                                scope.launch {
                                    try {
                                        Supabase.client.auth.signOut()
                                        viewModel.clearBills()
                                        navController.navigate("welcome") {
                                            popUpTo("main_menu") { inclusive = true }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Sign out failed", e)
                                    }
                                }
                            }
                        )
                    }
                    composable("bills_menu") {
                        BillsMenuScreen(
                            onScreenshotClick = { navController.navigate("camera") },
                            onViewLibraryClick = { navController.navigate("library") },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("camera") {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        CameraCaptureFlow(
                            onBillCaptured = { label, date, uri ->
                                viewModel.addBill(context, label, date, uri)
                                navController.popBackStack("bills_menu", false)
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("library") {
                        BillLibraryScreen(
                            bills = viewModel.bills,
                            onBack = { navController.popBackStack() },
                            onEditBill = { /* Handle edit */ },
                            onDeleteBill = { viewModel.deleteBill(it) },
                            onSortByLabel = { viewModel.sortBillsByLabel() },
                            onSortByDate = { viewModel.sortBillsByDate() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(
    onLogin: () -> Unit,
    onSignUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Fast Track Japan",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Log In", fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onSignUp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Sign Up", fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun WelcomeScreenPreview() {
    FastTrackJapanTheme {
        WelcomeScreen(onLogin = {}, onSignUp = {})
    }
}
