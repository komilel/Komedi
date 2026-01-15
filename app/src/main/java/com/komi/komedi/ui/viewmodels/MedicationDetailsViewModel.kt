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
import com.komi.komedi.notification.NotificationScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class ScheduleTimeStatus(
    val time: String,
    val isTaken: Boolean,
    val log: MedicationLog?
)

data class MedicationDetailsUiState(
    val medication: Medication? = null,
    val scheduleStatuses: List<ScheduleTimeStatus> = emptyList(),
    val recentLogs: List<MedicationLog> = emptyList(),
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null
)

class MedicationDetailsViewModel(
    private val medicationRepository: MedicationRepository,
    private val logRepository: MedicationLogRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicationDetailsUiState())
    val uiState: StateFlow<MedicationDetailsUiState> = _uiState.asStateFlow()

    private var currentMedicationId: Int = -1

    fun loadMedication(medicationId: Int) {
        currentMedicationId = medicationId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            medicationRepository.getMedicationById(medicationId).collect { medication ->
                if (medication != null) {
                    loadScheduleStatuses(medication)
                    loadRecentLogs(medicationId)
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Medication not found")
                    }
                }
            }
        }
    }

    private suspend fun loadScheduleStatuses(medication: Medication) {
        val today = LocalDate.now()
        val dateMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val times = medication.getScheduleTimesList()
        val statuses = times.map { time ->
            val log = logRepository.getLogForScheduledTime(medication.id, dateMillis, time)
            ScheduleTimeStatus(
                time = time,
                isTaken = log?.taken == true,
                log = log
            )
        }

        _uiState.update {
            it.copy(
                medication = medication,
                scheduleStatuses = statuses,
                isLoading = false
            )
        }
    }

    private fun loadRecentLogs(medicationId: Int) {
        viewModelScope.launch {
            logRepository.getLogsForMedication(medicationId).collect { logs ->
                _uiState.update { it.copy(recentLogs = logs.take(10)) }
            }
        }
    }

    fun toggleScheduleTime(status: ScheduleTimeStatus) {
        viewModelScope.launch {
            val medication = _uiState.value.medication ?: return@launch
            val today = LocalDate.now()
            val dateMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (status.log != null) {
                logRepository.updateLog(status.log.copy(taken = !status.isTaken))
            } else {
                logRepository.logMedication(
                    MedicationLog(
                        medicationId = medication.id,
                        scheduledTime = status.time,
                        takenAt = System.currentTimeMillis(),
                        date = dateMillis,
                        taken = true
                    )
                )
            }

            // Reload statuses
            loadScheduleStatuses(medication)
        }
    }

    fun logMedication() {
        viewModelScope.launch {
            val medication = _uiState.value.medication ?: return@launch
            val today = LocalDate.now()
            val dateMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            // Log all unlogged times as taken
            val statuses = _uiState.value.scheduleStatuses
            for (status in statuses) {
                if (!status.isTaken) {
                    logRepository.logMedication(
                        MedicationLog(
                            medicationId = medication.id,
                            scheduledTime = status.time,
                            takenAt = System.currentTimeMillis(),
                            date = dateMillis,
                            taken = true
                        )
                    )
                }
            }

            loadScheduleStatuses(medication)
        }
    }

    fun deleteMedication() {
        viewModelScope.launch {
            val medication = _uiState.value.medication ?: return@launch
            try {
                // Cancel alarms for this medication before deleting
                val times = medication.getScheduleTimesList()
                NotificationScheduler.cancelMedicationAlarms(context, medication.id, times)

                medicationRepository.delete(medication)
                _uiState.update { it.copy(isDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    class Factory(
        private val medicationRepository: MedicationRepository,
        private val logRepository: MedicationLogRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MedicationDetailsViewModel::class.java)) {
                return MedicationDetailsViewModel(medicationRepository, logRepository, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
