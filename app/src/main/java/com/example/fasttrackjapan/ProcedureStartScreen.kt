package com.example.fasttrackjapan

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcedureStartScreen(
    procedureCode: String,
    viewModel: ProcedureViewModel,
    onStarted: () -> Unit,
    onBack: () -> Unit
) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Start procedure") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "When did you move in? Past dates are OK — we'll flag anything overdue.",
                style = MaterialTheme.typography.bodyMedium
            )
            DateField(
                value = date,
                onValueChange = { date = it },
                label = "Move-in date",
                modifier = Modifier.fillMaxWidth()
            )
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { viewModel.startProcedure(procedureCode, date, onDone = onStarted) },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Start") }
        }
    }
}
