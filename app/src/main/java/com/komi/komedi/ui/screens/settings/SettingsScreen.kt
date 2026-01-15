package com.komi.komedi.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.komi.komedi.notification.MedicationNotificationWorker
import com.komi.komedi.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showTestNotificationSent by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Section
            item {
                Text(
                    text = "Profile",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.userName,
                    onValueChange = { viewModel.updateUserName(it) },
                    label = { Text("Your Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Notifications Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notifications",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Enable Notifications") },
                            supportingContent = { Text("Get reminded to take your medications") },
                            trailingContent = {
                                Switch(
                                    checked = uiState.notificationsEnabled,
                                    onCheckedChange = { viewModel.toggleNotifications(it) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        )

                        if (uiState.notificationsEnabled) {
                            HorizontalDivider()
                            ListItem(
                                headlineContent = { Text("Reminder Time") },
                                supportingContent = {
                                    Text("Notify ${uiState.notificationTime} minutes before scheduled time")
                                },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(5, 10, 15, 30).forEach { minutes ->
                                    FilterChip(
                                        selected = uiState.notificationTime == minutes,
                                        onClick = { viewModel.updateNotificationTime(minutes) },
                                        label = { Text("${minutes}m") }
                                    )
                                }
                            }

                            HorizontalDivider()
                            ListItem(
                                headlineContent = { Text("Test Notifications") },
                                supportingContent = {
                                    Text(
                                        if (showTestNotificationSent) "Check triggered! Look at logcat for details."
                                        else "Run a notification check now"
                                    )
                                },
                                trailingContent = {
                                    Button(
                                        onClick = {
                                            MedicationNotificationWorker.runOnce(context)
                                            showTestNotificationSent = true
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Notifications,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Test")
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                            )
                        }
                    }
                }
            }

            // Appearance Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Appearance",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    ListItem(
                        headlineContent = { Text("Dark Mode") },
                        supportingContent = { Text("Use dark theme") },
                        trailingContent = {
                            Switch(
                                checked = uiState.darkMode,
                                onCheckedChange = { viewModel.toggleDarkMode(it) }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    )
                }
            }

            // About Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "About",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Komedi") },
                            supportingContent = { Text("Medication Reminder App") },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text("Version") },
                            supportingContent = { Text("1.0.0") },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
