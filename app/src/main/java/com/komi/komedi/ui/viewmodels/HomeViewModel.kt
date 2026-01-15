package com.komi.komedi.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.komi.komedi.data.local.Medication
import com.komi.komedi.data.local.MedicationLog
import com.komi.komedi.data.local.getScheduleTimesList
import com.komi.komedi.data.repository.MedicationLogRepository
import com.komi.komedi.data.repository.MedicationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class MedicationScheduleItem(
    val medication: Medication,
    val scheduledTime: String,
    val isTaken: Boolean,
    val log: MedicationLog?
)

data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val medications: List<Medication> = emptyList(),
    val scheduleItems: List<MedicationScheduleItem> = emptyList(),
    val isLoading: Boolean = true,
    val userName: String = "User"
)

class HomeViewModel(
    private val medicationRepository: MedicationRepository,
    private val logRepository: MedicationLogRepository,
    private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("komedi_settings", Context.MODE_PRIVATE)
    private val _selectedDate = MutableStateFlow(LocalDate.now())

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserName()

        viewModelScope.launch {
            combine(
                medicationRepository.activeMedications,
                _selectedDate
            ) { medications, date ->
                Pair(medications, date)
            }.collect { (medications, date) ->
                updateScheduleItems(medications, date)
            }
        }
    }

    private fun loadUserName() {
        val userName = prefs.getString("user_name", "User") ?: "User"
        _uiState.update { it.copy(userName = userName) }
    }

    fun refreshUserName() {
        loadUserName()
    }

    private suspend fun updateScheduleItems(medications: List<Medication>, date: LocalDate) {
        val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val scheduleItems = mutableListOf<MedicationScheduleItem>()

        for (medication in medications) {
            val times = medication.getScheduleTimesList()
            for (time in times) {
                val log = logRepository.getLogForScheduledTime(medication.id, dateMillis, time)
                scheduleItems.add(
                    MedicationScheduleItem(
                        medication = medication,
                        scheduledTime = time,
                        isTaken = log?.taken == true,
                        log = log
                    )
                )
            }
        }

        // Sort by time
        val sortedItems = scheduleItems.sortedBy {
            try {
                LocalTime.parse(it.scheduledTime, DateTimeFormatter.ofPattern("HH:mm"))
            } catch (e: Exception) {
                LocalTime.MIDNIGHT
            }
        }

        _uiState.update {
            it.copy(
                selectedDate = date,
                medications = medications,
                scheduleItems = sortedItems,
                isLoading = false
            )
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun toggleMedicationTaken(item: MedicationScheduleItem) {
        viewModelScope.launch {
            val dateMillis = _selectedDate.value
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            if (item.log != null) {
                // Update existing log
                logRepository.updateLog(item.log.copy(taken = !item.isTaken))
            } else {
                // Create new log
                logRepository.logMedication(
                    MedicationLog(
                        medicationId = item.medication.id,
                        scheduledTime = item.scheduledTime,
                        takenAt = System.currentTimeMillis(),
                        date = dateMillis,
                        taken = true
                    )
                )
            }

            // Refresh the schedule
            updateScheduleItems(_uiState.value.medications, _selectedDate.value)
        }
    }

    class Factory(
        private val medicationRepository: MedicationRepository,
        private val logRepository: MedicationLogRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(medicationRepository, logRepository, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
