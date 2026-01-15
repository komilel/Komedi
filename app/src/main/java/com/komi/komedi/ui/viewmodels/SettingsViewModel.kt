package com.komi.komedi.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.komi.komedi.notification.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val userName: String = "User",
    val notificationsEnabled: Boolean = true,
    val notificationTime: Int = 15, // minutes before scheduled time
    val darkMode: Boolean = false
)

class SettingsViewModel(
    private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("komedi_settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(loadSettings())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private fun loadSettings(): SettingsUiState {
        return SettingsUiState(
            userName = prefs.getString("user_name", "User") ?: "User",
            notificationsEnabled = prefs.getBoolean("notifications_enabled", true),
            notificationTime = prefs.getInt("notification_time", 15),
            darkMode = prefs.getBoolean("dark_mode", false)
        )
    }

    fun updateUserName(name: String) {
        _uiState.update { it.copy(userName = name) }
        prefs.edit().putString("user_name", name).apply()
    }

    fun toggleNotifications(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        // Reschedule alarms based on new setting
        rescheduleAlarms()
    }

    fun updateNotificationTime(minutes: Int) {
        _uiState.update { it.copy(notificationTime = minutes) }
        prefs.edit().putInt("notification_time", minutes).apply()
        // Reschedule alarms with new timing
        rescheduleAlarms()
    }

    private fun rescheduleAlarms() {
        viewModelScope.launch(Dispatchers.IO) {
            NotificationScheduler.scheduleAllAlarms(context)
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        _uiState.update { it.copy(darkMode = enabled) }
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    class Factory(
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
