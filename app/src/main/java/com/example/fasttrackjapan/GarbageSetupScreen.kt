package com.example.fasttrackjapan

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarbageSetupScreen(
    viewModel: GarbageViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { viewModel.loadWards() }

    var selectedWard by remember { mutableStateOf<GarbageWard?>(null) }
    var selectedArea by remember { mutableStateOf<GarbageArea?>(null) }
    var reminderEnabled by remember { mutableStateOf(true) }
    var reminderTime by remember { mutableStateOf("19:00") }
    var wardExpanded by remember { mutableStateOf(false) }
    var areaExpanded by remember { mutableStateOf(false) }
    var timeExpanded by remember { mutableStateOf(false) }
    val times = listOf("18:00", "19:00", "20:00", "21:00")

    val notifPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set up garbage schedule") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Select your ward and district", style = MaterialTheme.typography.titleMedium)

            // Ward dropdown
            ExposedDropdownMenuBox(expanded = wardExpanded, onExpandedChange = { wardExpanded = !wardExpanded }) {
                OutlinedTextField(
                    value = selectedWard?.let { "${it.nameJa} (${it.nameEn})" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Ward") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = wardExpanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = wardExpanded, onDismissRequest = { wardExpanded = false }) {
                    viewModel.wards.forEach { ward ->
                        DropdownMenuItem(
                            text = { Text("${ward.nameJa} (${ward.nameEn})") },
                            onClick = {
                                selectedWard = ward
                                selectedArea = null
                                viewModel.selectWard(ward.code)
                                wardExpanded = false
                            }
                        )
                    }
                }
            }

            // Area (chōme) dropdown
            ExposedDropdownMenuBox(expanded = areaExpanded, onExpandedChange = { if (selectedWard != null) areaExpanded = !areaExpanded }) {
                OutlinedTextField(
                    value = selectedArea?.nameJa ?: "",
                    onValueChange = {},
                    readOnly = true,
                    enabled = selectedWard != null,
                    label = { Text("District (丁目)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = areaExpanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = areaExpanded, onDismissRequest = { areaExpanded = false }) {
                    viewModel.areas.forEach { area ->
                        DropdownMenuItem(
                            text = { Text(area.nameJa) },
                            onClick = {
                                selectedArea = area
                                areaExpanded = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable reminders", modifier = Modifier.weight(1f))
                Switch(checked = reminderEnabled, onCheckedChange = {
                    reminderEnabled = it
                    if (it && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                })
            }

            // Reminder time dropdown
            ExposedDropdownMenuBox(expanded = timeExpanded, onExpandedChange = { if (reminderEnabled) timeExpanded = !timeExpanded }) {
                OutlinedTextField(
                    value = reminderTime,
                    onValueChange = {},
                    readOnly = true,
                    enabled = reminderEnabled,
                    label = { Text("Remind me the evening before, at") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeExpanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = timeExpanded, onDismissRequest = { timeExpanded = false }) {
                    times.forEach { t ->
                        DropdownMenuItem(text = { Text(t) }, onClick = { reminderTime = t; timeExpanded = false })
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    selectedArea?.let { area ->
                        viewModel.saveSetup(area.id, reminderEnabled, reminderTime, onDone = onSaved)
                    }
                },
                enabled = selectedArea != null && !viewModel.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
