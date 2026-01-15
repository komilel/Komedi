package com.komi.komedi.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.komi.komedi.data.local.Medication
import com.komi.komedi.data.local.setScheduleTimesList
import com.komi.komedi.data.local.getScheduleTimesList
import com.komi.komedi.data.remote.DrugApiService
import com.komi.komedi.data.remote.DrugInfo
import com.komi.komedi.data.repository.MedicationRepository
import com.komi.komedi.notification.NotificationScheduler
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AddEditUiState(
    val isEditing: Boolean = false,
    val medicationId: Int? = null,
    val name: String = "",
    val description: String = "",
    val dosageAmount: String = "",
    val dosageUnit: String = "pill",
    val frequency: Int = 1,
    val scheduleTimes: List<String> = listOf("08:00"),
    val instructions: String = "",
    val notes: String = "",
    val iconType: String = "pill",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val errorMessage: String? = null,
    // Drug search
    val isSearching: Boolean = false,
    val searchResults: List<DrugInfo> = emptyList(),
    val showSearchResults: Boolean = false,
    val searchQuery: String = ""  // Separate from name, for the search field
)

class AddEditMedicationViewModel(
    private val medicationRepository: MedicationRepository,
    private val drugApiService: DrugApiService,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun loadMedication(medicationId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val medication = medicationRepository.getMedicationByIdOnce(medicationId)
            if (medication != null) {
                _uiState.update {
                    it.copy(
                        isEditing = true,
                        medicationId = medication.id,
                        name = medication.name,
                        searchQuery = medication.name,
                        description = medication.description,
                        dosageAmount = medication.dosageAmount,
                        dosageUnit = medication.dosageUnit,
                        frequency = medication.frequency,
                        scheduleTimes = medication.getScheduleTimesList().ifEmpty { listOf("08:00") },
                        instructions = medication.instructions,
                        notes = medication.notes,
                        iconType = medication.iconType,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Medication not found") }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        // Cancel previous search job
        searchJob?.cancel()

        if (query.length < 2) {
            _uiState.update { it.copy(showSearchResults = false, searchResults = emptyList(), isSearching = false) }
            return
        }

        // Debounce: wait 500ms before searching
        searchJob = viewModelScope.launch {
            delay(500)
            performSearch(query)
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isSearching = true) }
        val result = drugApiService.searchDrug(query)
        result.onSuccess { drugs ->
            _uiState.update {
                it.copy(
                    isSearching = false,
                    searchResults = drugs,
                    showSearchResults = drugs.isNotEmpty()
                )
            }
        }.onFailure { e ->
            _uiState.update {
                it.copy(
                    isSearching = false,
                    showSearchResults = false,
                    searchResults = emptyList()
                )
            }
        }
    }

    fun selectDrugFromSearch(drug: DrugInfo) {
        _uiState.update {
            it.copy(
                name = drug.brandName,
                searchQuery = drug.brandName,
                description = drug.genericName ?: drug.purpose ?: "",
                showSearchResults = false,
                searchResults = emptyList()
            )
        }
    }

    fun useCustomName() {
        // User wants to use their typed name without selecting from API
        _uiState.update {
            it.copy(
                name = it.searchQuery,
                showSearchResults = false,
                searchResults = emptyList()
            )
        }
    }

    fun dismissSearchResults() {
        _uiState.update { it.copy(showSearchResults = false) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateDosageAmount(amount: String) {
        _uiState.update { it.copy(dosageAmount = amount) }
    }

    fun updateDosageUnit(unit: String) {
        _uiState.update { it.copy(dosageUnit = unit) }
    }

    fun updateFrequency(frequency: Int) {
        val times = _uiState.value.scheduleTimes.toMutableList()
        while (times.size < frequency) {
            times.add("08:00")
        }
        while (times.size > frequency) {
            times.removeLast()
        }
        _uiState.update { it.copy(frequency = frequency, scheduleTimes = times) }
    }

    fun updateScheduleTime(index: Int, time: String) {
        val times = _uiState.value.scheduleTimes.toMutableList()
        if (index in times.indices) {
            times[index] = time
            _uiState.update { it.copy(scheduleTimes = times) }
        }
    }

    fun updateInstructions(instructions: String) {
        _uiState.update { it.copy(instructions = instructions) }
    }

    fun updateNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun updateIconType(iconType: String) {
        _uiState.update { it.copy(iconType = iconType) }
    }

    fun saveMedication() {
        val state = _uiState.value

        // Use searchQuery as name if name is empty
        val medicationName = state.name.ifBlank { state.searchQuery }

        if (medicationName.isBlank() || state.dosageAmount.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please fill in name and dosage") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val medication = Medication(
                id = state.medicationId ?: 0,
                name = medicationName.trim(),
                description = state.description.trim(),
                dosageAmount = state.dosageAmount.trim(),
                dosageUnit = state.dosageUnit,
                frequency = state.frequency,
                scheduleTimes = state.scheduleTimes.joinToString(","),
                instructions = state.instructions.trim(),
                notes = state.notes.trim(),
                iconType = state.iconType
            )

            try {
                if (state.isEditing) {
                    // Cancel old alarms before updating (times may have changed)
                    val oldMedication = medicationRepository.getMedicationByIdOnce(state.medicationId!!)
                    oldMedication?.let {
                        val oldTimes = it.getScheduleTimesList()
                        NotificationScheduler.cancelMedicationAlarms(context, it.id, oldTimes)
                    }
                    medicationRepository.update(medication)
                } else {
                    medicationRepository.insert(medication)
                }

                // Schedule notifications for the new/updated times
                val prefs = context.getSharedPreferences("komedi_settings", Context.MODE_PRIVATE)
                val minutesBefore = prefs.getInt("notification_time", 15)
                NotificationScheduler.scheduleMedicationAlarms(context, medication, minutesBefore)

                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    class Factory(
        private val medicationRepository: MedicationRepository,
        private val drugApiService: DrugApiService,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AddEditMedicationViewModel::class.java)) {
                return AddEditMedicationViewModel(medicationRepository, drugApiService, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
