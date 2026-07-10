package com.example.fasttrackjapan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcedureDetailScreen(
    viewModel: ProcedureViewModel,
    onBack: () -> Unit
) {
    val steps by viewModel.steps.collectAsState()
    val active by viewModel.activeProcedure.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val uriHandler = LocalUriHandler.current

    val done = steps.count { it.status == ProcedureStatus.DONE }
    val overdue = steps.count { it.status == ProcedureStatus.OVERDUE }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Moving In") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            active?.let {
                Text("Started ${it.startDate} • $done of ${steps.size} done" + if (overdue > 0) " • $overdue overdue" else "",
                    style = MaterialTheme.typography.bodyMedium)
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(steps) { view ->
                    StepRow(view = view, onToggle = { viewModel.toggleStep(view.step.id) }, onOpenLink = { url -> uriHandler.openUri(url) })
                }
            }
        }
    }
}

@Composable
private fun StepRow(view: ProcedureStepView, onToggle: () -> Unit, onOpenLink: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Checkbox(
                checked = view.status == ProcedureStatus.DONE,
                onCheckedChange = { onToggle() }
            )
            Column(Modifier.weight(1f)) {
                Text(view.step.titleEn + "  (${view.step.titleJa})", style = MaterialTheme.typography.titleSmall)
                if (view.step.description.isNotBlank()) {
                    Text(view.step.description, style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(view)
                    view.step.linkUrl?.let { url ->
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { onOpenLink(url) }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Open form")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(view: ProcedureStepView) {
    val (label, color) = when (view.status) {
        ProcedureStatus.DONE -> "Done" to MaterialTheme.colorScheme.primary
        ProcedureStatus.UPCOMING -> {
            val d = view.daysDelta
            (if (d != null) "In $d days" else "No deadline") to MaterialTheme.colorScheme.secondary
        }
        ProcedureStatus.DUE_SOON -> {
            val d = view.daysDelta ?: 0L
            (if (d == 0L) "Due today" else "In $d days") to MaterialTheme.colorScheme.tertiary
        }
        ProcedureStatus.OVERDUE -> {
            val overdue = -(view.daysDelta ?: 0L)
            "$overdue day${if (overdue == 1L) "" else "s"} overdue" to MaterialTheme.colorScheme.error
        }
    }
    Surface(color = color.copy(alpha = 0.15f), contentColor = color, shape = MaterialTheme.shapes.small) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
    // Suppress unused-variable warning on `Color` import above; keeps signatures stable if theme changes.
    @Suppress("UNUSED_EXPRESSION") Color.Transparent
}
