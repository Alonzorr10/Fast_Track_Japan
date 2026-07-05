package com.example.fasttrackjapan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarbageScheduleScreen(
    viewModel: GarbageViewModel,
    onBack: () -> Unit,
    onChangeArea: () -> Unit
) {
    LaunchedEffect(Unit) { viewModel.load() }

    val snapshot = viewModel.snapshot
    val upcoming = viewModel.upcoming
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Garbage Schedule") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onChangeArea) {
                        Icon(Icons.Default.Edit, contentDescription = "Change area")
                    }
                }
            )
        }
    ) { padding ->
        if (snapshot == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                if (viewModel.isLoading) CircularProgressIndicator() else Text("No schedule loaded.")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "${snapshot.area.nameJa}",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            item { Text("Upcoming", style = MaterialTheme.typography.titleMedium) }

            if (upcoming.isEmpty()) {
                item { Text("No collections found for your area yet.") }
            } else {
                items(upcoming) { day ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(day.date.format(dateFmt), fontWeight = FontWeight.Bold)
                            Text(day.categoryCodes.joinToString(", ") { viewModel.categoryLabel(it) })
                        }
                    }
                }
            }

            // Oversized garbage info card (on-demand, not scheduled)
            snapshot.categories.firstOrNull { it.code == "OVERSIZED" }?.let { oversized ->
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("${oversized.nameJa} (${oversized.nameEn})", fontWeight = FontWeight.Bold)
                            Text(
                                "Oversized garbage is by advance booking with your ward — it is not on a fixed schedule.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
