package com.komi.komedi.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.komi.komedi.navigation.Screen
import com.komi.komedi.ui.viewmodels.HomeViewModel
import com.komi.komedi.ui.viewmodels.MedicationScheduleItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    // Refresh username when screen becomes visible (e.g., returning from settings)
    LaunchedEffect(Unit) {
        viewModel.refreshUserName()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddMedication.route) }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Medication")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Top bar with greeting and settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hey, ${uiState.userName}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current date display with proper label
            val today = LocalDate.now()
            val dateLabel = when {
                uiState.selectedDate == today -> "Today"
                uiState.selectedDate == today.minusDays(1) -> "Yesterday"
                uiState.selectedDate == today.plusDays(1) -> "Tomorrow"
                else -> uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
            }
            Text(
                text = "$dateLabel, ${uiState.selectedDate.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date picker row
            DatePickerRow(
                selectedDate = uiState.selectedDate,
                onDateSelected = { viewModel.selectDate(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // "To take" section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "To take", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                if (uiState.scheduleItems.isNotEmpty()) {
                    Text(
                        text = "${uiState.scheduleItems.count { it.isTaken }}/${uiState.scheduleItems.size} taken",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.scheduleItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No medications scheduled",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to add a medication",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.scheduleItems) { item ->
                        MedicationItem(
                            item = item,
                            onTakeClick = { viewModel.toggleMedicationTaken(item) },
                            onClick = {
                                navController.navigate(
                                    Screen.MedicationDetails.createRoute(item.medication.id)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DatePickerRow(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val dates = (-3..3).map { today.plusDays(it.toLong()) }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(dates) { date ->
            DateCard(
                date = date,
                isSelected = date == selectedDate,
                isToday = date == today,
                onClick = { onDateSelected(date) }
            )
        }
    }
}

@Composable
fun DateCard(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val dayOfWeek = date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
    val dayOfMonth = date.dayOfMonth

    Card(
        modifier = Modifier
            .size(60.dp, 80.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isToday -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayOfWeek,
                fontSize = 12.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dayOfMonth.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MedicationItem(
    item: MedicationScheduleItem,
    onTakeClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isTaken)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (item.medication.iconType) {
                            "pill" -> Color(0xFFE3F2FD)
                            "capsule" -> Color(0xFFFCE4EC)
                            "liquid" -> Color(0xFFE8F5E9)
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (item.medication.iconType) {
                        "pill" -> "💊"
                        "capsule" -> "💉"
                        "liquid" -> "🧴"
                        else -> "💊"
                    },
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Medication info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.medication.name}, ${item.medication.dosageAmount}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (item.isTaken) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${item.medication.frequency}x ${item.medication.dosageUnit}, ${item.medication.instructions.ifEmpty { "As directed" }}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Time
            Text(
                text = item.scheduledTime,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Take button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.isTaken) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable(onClick = onTakeClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = if (item.isTaken) "Taken" else "Mark as taken",
                    tint = if (item.isTaken) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
