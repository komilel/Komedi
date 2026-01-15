package com.komi.komedi.di

import android.content.Context
import com.komi.komedi.data.local.AppDatabase
import com.komi.komedi.data.remote.DrugApiService
import com.komi.komedi.data.repository.MedicationLogRepository
import com.komi.komedi.data.repository.MedicationRepository

class AppContainer(private val context: Context) {

    private val database by lazy { AppDatabase.getDatabase(context) }

    val medicationRepository by lazy {
        MedicationRepository(database.medicationDao())
    }

    val medicationLogRepository by lazy {
        MedicationLogRepository(database.medicationLogDao())
    }

    val drugApiService by lazy {
        DrugApiService()
    }
}
