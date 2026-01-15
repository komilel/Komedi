package com.komi.komedi.ui.screens.medication_details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.komi.komedi.data.local.getScheduleTimesList
import com.komi.komedi.navigation.Screen
import com.komi.komedi.ui.viewmodels.MedicationDetailsViewModel
import com.komi.komedi.ui.viewmodels.ScheduleTimeStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationDetailsScreen(
    navController: NavController,
    viewModel: MedicationDetailsViewModel,
    medicationId: Int
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(medicationId) {
        viewModel.loadMedication(medicationId)
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            navController.popBackStack()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Medication") },
            text = { Text("Are you sure you want to delete this medication? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMedication()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            navController.navigate(Screen.EditMedication.createRoute(medicationId))
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
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
        } else if (uiState.medication == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Medication not found")
            }
        } else {
            val medication = uiState.medication!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Medication Icon
                item {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                when (medication.iconType) {
                                    "pill" -> Color(0xFFE3F2FD)
                                    "capsule" -> Color(0xFFFCE4EC)
                                    "liquid" -> Color(0xFFE8F5E9)
                                    else -> MaterialTheme.colorScheme.primaryContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (medication.iconType) {
                                "pill" -> "💊"
                                "capsule" -> "💉"
                                "liquid" -> "🧴"
                                else -> "💊"
                            },
                            fontSize = 48.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Medication Name and Description
                item {
                    Text(
                        text = medication.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (medication.description.isNotEmpty()) {
                        Text(
                            text = medication.description,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Info Chips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        InfoChip(
                            title = medication.dosageAmount,
                            subtitle = "Dosage"
                        )
                        InfoChip(
                            title = "${medication.frequency}x ${medication.dosageUnit}",
                            subtitle = "Per Day"
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Instructions
                if (medication.instructions.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Instructions",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = medication.instructions)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Schedule Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Schedule",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.scheduleStatuses.count { it.isTaken }}/${uiState.scheduleStatuses.size}",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Schedule Items
                items(uiState.scheduleStatuses) { status ->
                    ScheduleItem(
                        status = status,
                        onToggle = { viewModel.toggleScheduleTime(status) }
                    )
                }

                // Notes
                if (medication.notes.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Notes",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = medication.notes)
                            }
                        }
                    }
                }

                // Action Buttons
                item {
                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { viewModel.logMedication() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Log All as Taken", fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun InfoChip(title: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ScheduleItem(
    status: ScheduleTimeStatus,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = if (status.isTaken)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = status.time,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (status.isTaken) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = if (status.isTaken) "Taken" else "Not taken",
                    tint = if (status.isTaken) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
