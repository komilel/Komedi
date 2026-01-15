package com.komi.komedi.ui.screens.add_edit_medication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.komi.komedi.ui.viewmodels.AddEditMedicationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicationScreen(
    navController: NavController,
    viewModel: AddEditMedicationViewModel,
    medicationId: Int?
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(medicationId) {
        if (medicationId != null) {
            viewModel.loadMedication(medicationId)
        }
    }

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isEditing) "Edit Medication" else "Add Medication")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Medication Name with autocomplete search
                item {
                    Column {
                        Text(
                            text = "Medication Name",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Start typing to search...") },
                            trailingIcon = {
                                if (uiState.isSearching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else if (uiState.name.isNotBlank()) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            singleLine = true,
                            supportingText = {
                                if (uiState.name.isNotBlank() && uiState.name != uiState.searchQuery) {
                                    Text(
                                        "Selected: ${uiState.name}",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else if (uiState.searchQuery.length >= 2 && !uiState.isSearching && uiState.searchResults.isEmpty() && !uiState.showSearchResults) {
                                    Text("Type to search or use custom name")
                                }
                            }
                        )

                        // Search results dropdown
                        if (uiState.showSearchResults && uiState.searchResults.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Found ${uiState.searchResults.size} result(s)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                    HorizontalDivider()

                                    uiState.searchResults.forEach { drug ->
                                        ListItem(
                                            headlineContent = {
                                                Text(
                                                    drug.brandName,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            },
                                            supportingContent = {
                                                Column {
                                                    if (drug.genericName != null) {
                                                        Text(
                                                            "Generic: ${drug.genericName}",
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                    if (drug.purpose != null) {
                                                        Text(
                                                            drug.purpose,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 2
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier.clickable {
                                                viewModel.selectDrugFromSearch(drug)
                                            }
                                        )
                                        HorizontalDivider()
                                    }

                                    // Option to use custom name
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                "Use \"${uiState.searchQuery}\" as custom name",
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        modifier = Modifier.clickable {
                                            viewModel.useCustomName()
                                        }
                                    )
                                }
                            }
                        } else if (uiState.searchQuery.length >= 2 && !uiState.isSearching && uiState.searchResults.isEmpty()) {
                            // No results found, show option to use custom name
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "No medications found in database",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = { viewModel.useCustomName() }
                                    ) {
                                        Text("Use \"${uiState.searchQuery}\" as custom name")
                                    }
                                }
                            }
                        }
                    }
                }

                // Description
                item {
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = { viewModel.updateDescription(it) },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        supportingText = {
                            Text("e.g., Generic name, purpose, or notes about this medication")
                        }
                    )
                }

                // Dosage
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.dosageAmount,
                            onValueChange = { viewModel.updateDosageAmount(it) },
                            label = { Text("Dosage") },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("e.g., 20mg") },
                            singleLine = true
                        )

                        // Dosage unit dropdown
                        var unitExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = unitExpanded,
                            onExpandedChange = { unitExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = uiState.dosageUnit,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Unit") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = unitExpanded,
                                onDismissRequest = { unitExpanded = false }
                            ) {
                                listOf("pill", "tablet", "capsule", "ml", "mg", "drops").forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unit) },
                                        onClick = {
                                            viewModel.updateDosageUnit(unit)
                                            unitExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Frequency
                item {
                    Column {
                        Text(
                            text = "Frequency (times per day)",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            (1..4).forEach { freq ->
                                FilterChip(
                                    selected = uiState.frequency == freq,
                                    onClick = { viewModel.updateFrequency(freq) },
                                    label = { Text("${freq}x") }
                                )
                            }
                        }
                    }
                }

                // Schedule Times
                item {
                    Column {
                        Text(
                            text = "Schedule Times",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        uiState.scheduleTimes.forEachIndexed { index, time ->
                            var showTimePicker by remember { mutableStateOf(false) }

                            // Parse current time
                            val timeParts = time.split(":")
                            val initialHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 8
                            val initialMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

                            OutlinedCard(
                                onClick = { showTimePicker = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Time ${index + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = time,
                                            style = MaterialTheme.typography.headlineMedium
                                        )
                                    }
                                    Icon(
                                        Icons.Outlined.Schedule,
                                        contentDescription = "Select time",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (showTimePicker) {
                                TimePickerDialog(
                                    initialHour = initialHour,
                                    initialMinute = initialMinute,
                                    onDismiss = { showTimePicker = false },
                                    onConfirm = { hour, minute ->
                                        val formattedTime = String.format("%02d:%02d", hour, minute)
                                        viewModel.updateScheduleTime(index, formattedTime)
                                        showTimePicker = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Instructions
                item {
                    OutlinedTextField(
                        value = uiState.instructions,
                        onValueChange = { viewModel.updateInstructions(it) },
                        label = { Text("Instructions") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g., After breakfast, With water") },
                        singleLine = true
                    )
                }

                // Icon Type Selection
                item {
                    Column {
                        Text(
                            text = "Medication Type",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                listOf(
                                    "pill" to "💊",
                                    "capsule" to "💉",
                                    "liquid" to "🧴",
                                    "tablet" to "💎"
                                )
                            ) { (type, emoji) ->
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (uiState.iconType == type)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { viewModel.updateIconType(type) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 28.sp)
                                }
                            }
                        }
                    }
                }

                // Notes
                item {
                    OutlinedTextField(
                        value = uiState.notes,
                        onValueChange = { viewModel.updateNotes(it) },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }

                // Error message
                if (uiState.errorMessage != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = uiState.errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                // Save Button
                item {
                    Button(
                        onClick = { viewModel.saveMedication() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !uiState.isSaving && (uiState.name.isNotBlank() || uiState.searchQuery.isNotBlank())
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                text = if (uiState.isEditing) "Update Medication" else "Add Medication",
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                // Extra padding at bottom
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select time",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                )

                TimePicker(state = timePickerState)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            onConfirm(timePickerState.hour, timePickerState.minute)
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}
