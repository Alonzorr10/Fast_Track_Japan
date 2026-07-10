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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontStyle
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
                val docViewModel: DocumentViewModel = viewModel()
                val profileViewModel: ProfileViewModel = viewModel()
                val garbageViewModel: GarbageViewModel = viewModel()
                val procedureViewModel: ProcedureViewModel = viewModel()
                val appContext = androidx.compose.ui.platform.LocalContext.current

                NavHost(navController = navController, startDestination = "splash") {
                    composable("splash") {
                        // Auto-login: wait for the persisted session to load, then route.
                        LaunchedEffect(Unit) {
                            Supabase.client.auth.awaitInitialization()
                            if (Supabase.client.auth.currentUserOrNull() != null) {
                                viewModel.fetchBills()
                                docViewModel.fetchDocuments()
                                profileViewModel.fetchProfile()
                                garbageViewModel.load()
                                procedureViewModel.load()
                                DocumentReminderScheduler.schedule(appContext)
                                ProcedureReminderScheduler.schedule(appContext)
                                navController.navigate("main_menu") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            } else {
                                navController.navigate("welcome") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        }
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) { CircularProgressIndicator() }
                    }
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
                                docViewModel.fetchDocuments()
                                profileViewModel.fetchProfile()
                                garbageViewModel.load()
                                procedureViewModel.load()
                                DocumentReminderScheduler.schedule(appContext)
                                ProcedureReminderScheduler.schedule(appContext)
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
                                docViewModel.fetchDocuments()
                                profileViewModel.fetchProfile()
                                garbageViewModel.load()
                                procedureViewModel.load()
                                DocumentReminderScheduler.schedule(appContext)
                                ProcedureReminderScheduler.schedule(appContext)
                                // Capture garbage-collection location as a one-time step right after registration.
                                navController.navigate("location_onboarding") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("location_onboarding") {
                        GarbageSetupScreen(
                            viewModel = garbageViewModel,
                            onSaved = {
                                navController.navigate("main_menu") {
                                    popUpTo("location_onboarding") { inclusive = true }
                                }
                            },
                            // Allow skipping; they can set it later from the Garbage card.
                            onBack = {
                                navController.navigate("main_menu") {
                                    popUpTo("location_onboarding") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("main_menu") {
                        val scope = androidx.compose.runtime.rememberCoroutineScope()
                        MainMenuScreen(
                            onBillTrackerClick = { navController.navigate("bills_menu") },
                            onExpirationTrackerClick = { navController.navigate("expiration_list") },
                            onResourceCenterClick = { navController.navigate("resource_center") },
                            onGarbageScheduleClick = { navController.navigate("garbage") },
                            onProceduresClick = { navController.navigate("procedures") },
                            onProfileClick = { navController.navigate("profile") },
                            onSignOutClick = {
                                scope.launch {
                                    try {
                                        Supabase.client.auth.signOut()
                                        viewModel.clearBills()
                                        docViewModel.clearDocuments()
                                        profileViewModel.clear()
                                        garbageViewModel.clear()
                                        procedureViewModel.clear()
                                        DocumentReminderScheduler.cancel(appContext)
                                        ProcedureReminderScheduler.cancel(appContext)
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
                    composable("profile") {
                        ProfileScreen(
                            viewModel = profileViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("expiration_list") {
                        ExpirationTrackerScreen(
                            documents = docViewModel.documents,
                            onBack = { navController.popBackStack() },
                            onAddClick = { navController.navigate("add_document") },
                            onEditClick = { doc ->
                                navController.navigate("edit_document/${doc.id}")
                            },
                            onDeleteClick = { docViewModel.deleteDocument(it) }
                        )
                    }
                    composable("add_document") {
                        AddEditDocumentScreen(
                            onSave = { type, date, lead ->
                                docViewModel.addDocument(type, date, lead)
                                DocumentReminderScheduler.schedule(appContext)
                                navController.popBackStack()
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("edit_document/{docId}") { backStackEntry ->
                        val docId = backStackEntry.arguments?.getString("docId")
                        val doc = docViewModel.documents.find { it.id == docId }
                        if (doc != null) {
                            AddEditDocumentScreen(
                                existingDoc = doc,
                                onSave = { type, date, lead ->
                                    docViewModel.updateDocument(doc.copy(
                                        type = type,
                                        expirationDate = date,
                                        notificationLeadTime = lead
                                    ))
                                    navController.popBackStack()
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
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

                    composable(route = "edit_bill/{billId}") { backStackEntry ->
                        val billId = backStackEntry.arguments?.getString("billId")
                        val bill = viewModel.bills.find { it.id == billId }
                        if (bill != null) {
                            EditBillScreen(
                                bill = bill,
                                onSave = { label, date ->
                                    viewModel.updateBill(bill.copy(label = label, date = date))
                                    navController.popBackStack()
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                    composable("resource_center") {
                        ResourceCenterScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("garbage") {
                        if (!garbageViewModel.initialLoadDone) {
                            LaunchedEffect(Unit) { garbageViewModel.load() }
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) { CircularProgressIndicator() }
                        } else if (garbageViewModel.hasArea) {
                            GarbageScheduleScreen(
                                viewModel = garbageViewModel,
                                onBack = { navController.popBackStack() },
                                onChangeArea = { navController.navigate("garbage_setup") }
                            )
                        } else {
                            GarbageSetupScreen(
                                viewModel = garbageViewModel,
                                onSaved = {
                                    navController.navigate("garbage") {
                                        popUpTo("garbage") { inclusive = true }
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                    composable("garbage_setup") {
                        GarbageSetupScreen(
                            viewModel = garbageViewModel,
                            onSaved = { navController.popBackStack() },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("procedures") {
                        ProcedureListScreen(
                            viewModel = procedureViewModel,
                            onBack = { navController.popBackStack() },
                            onProcedureClick = { code ->
                                val active = procedureViewModel.activeProcedure.value
                                if (active == null || active.procedureCode != code) {
                                    navController.navigate("procedure_start/$code")
                                } else {
                                    navController.navigate("procedure/$code")
                                }
                            }
                        )
                    }
                    composable("procedure_start/{code}") { backStackEntry ->
                        val code = backStackEntry.arguments?.getString("code") ?: return@composable
                        ProcedureStartScreen(
                            procedureCode = code,
                            viewModel = procedureViewModel,
                            onStarted = {
                                navController.navigate("procedure/$code") {
                                    popUpTo("procedures")
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("procedure/{code}") { _ ->
                        ProcedureDetailScreen(
                            viewModel = procedureViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("library") {
                        BillLibraryScreen(
                            bills = viewModel.bills,
                            onBack = { navController.popBackStack() },
                            onEditBill = { bill ->
                                navController.navigate("edit_bill/${bill.id}")
                            },
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
        Text(
            text = "Convenience at your 指先",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Italic
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

