package com.example.fasttrackjapan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpirationTrackerScreen(
    documents: List<ExpirationDocument>,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (ExpirationDocument) -> Unit,
    onDeleteClick: (ExpirationDocument) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val notifPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { }
    )
    LaunchedEffect(Unit) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.POST_NOTIFICATIONS
        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expiration Tracker") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Document")
            }
        }
    ) { padding ->
        if (documents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No documents tracked yet.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(documents) { doc ->
                    DocumentCard(doc, onEditClick, onDeleteClick)
                }
            }
        }
    }
}

@Composable
fun DocumentCard(
    doc: ExpirationDocument, 
    onEdit: (ExpirationDocument) -> Unit,
    onDelete: (ExpirationDocument) -> Unit
) {
    val icon = when (doc.type) {
        "Residence Card" -> Icons.Default.Badge
        "My Number Card" -> Icons.Default.CreditCard
        "Driver's License" -> Icons.Default.DirectionsCar
        else -> Icons.Default.Badge
    }

    val isExpired = remember(doc.expirationDate) {
        try {
            LocalDate.parse(doc.expirationDate).isBefore(LocalDate.now())
        } catch (e: Exception) {
            false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { onEdit(doc) }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(doc.type, style = MaterialTheme.typography.titleMedium)
                Text("Expires: ${doc.expirationDate}", style = MaterialTheme.typography.bodyMedium)
                if (isExpired) {
                    Text("EXPIRED", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Alert: ${doc.notificationLeadTime} days before", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            IconButton(onClick = { onDelete(doc) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDocumentScreen(
    existingDoc: ExpirationDocument? = null,
    onSave: (String, String, Int) -> Unit,
    onBack: () -> Unit
) {
    var selectedType by remember { mutableStateOf(existingDoc?.type ?: "Residence Card") }
    var expirationDate by remember { mutableStateOf(existingDoc?.expirationDate ?: LocalDate.now().toString()) }
    var leadTime by remember { mutableStateOf(existingDoc?.notificationLeadTime?.toString() ?: "30") }
    val docTypes = listOf("Residence Card", "My Number Card", "Driver's License", "Health Insurance Card", "Passport", "Pension Book")
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingDoc == null) "Add Document" else "Edit Document") },
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
            Text("Select Document Type", style = MaterialTheme.typography.labelLarge)
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedType,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    docTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                selectedType = type
                                expanded = false
                            }
                        )
                    }
                }
            }

            DateField(
                value = expirationDate,
                onValueChange = { expirationDate = it },
                label = "Expiration Date",
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = leadTime,
                onValueChange = { leadTime = it },
                label = { Text("Notify me (days before)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { 
                    val days = leadTime.toIntOrNull() ?: 30
                    onSave(selectedType, expirationDate, days) 
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (existingDoc == null) "Save Document" else "Update Document")
            }
        }
    }
}
