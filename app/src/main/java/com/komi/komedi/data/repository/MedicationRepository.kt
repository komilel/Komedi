package com.komi.komedi.data.repository

import com.komi.komedi.data.local.Medication
import com.komi.komedi.data.local.MedicationDao
import com.komi.komedi.data.local.MedicationLog
import com.komi.komedi.data.local.MedicationLogDao
import kotlinx.coroutines.flow.Flow

class MedicationRepository(private val medicationDao: MedicationDao) {
    val allMedications: Flow<List<Medication>> = medicationDao.getAllMedications()
    val activeMedications: Flow<List<Medication>> = medicationDao.getAllActiveMedications()

    fun getMedicationById(id: Int): Flow<Medication?> {
        return medicationDao.getMedicationById(id)
    }

    suspend fun getMedicationByIdOnce(id: Int): Medication? {
        return medicationDao.getMedicationByIdOnce(id)
    }

    fun searchMedications(query: String): Flow<List<Medication>> {
        return medicationDao.searchMedications(query)
    }

    suspend fun insert(medication: Medication): Long {
        return medicationDao.insertMedication(medication)
    }

    suspend fun update(medication: Medication) {
        medicationDao.updateMedication(medication)
    }

    suspend fun delete(medication: Medication) {
        medicationDao.deleteMedication(medication)
    }

    suspend fun deleteById(id: Int) {
        medicationDao.deleteMedicationById(id)
    }
}

class MedicationLogRepository(private val logDao: MedicationLogDao) {
    fun getLogsForMedication(medicationId: Int): Flow<List<MedicationLog>> {
        return logDao.getLogsForMedication(medicationId)
    }

    fun getLogsForDate(date: Long): Flow<List<MedicationLog>> {
        return logDao.getLogsForDate(date)
    }

    fun getLogsForMedicationOnDate(medicationId: Int, date: Long): Flow<List<MedicationLog>> {
        return logDao.getLogsForMedicationOnDate(medicationId, date)
    }

    suspend fun getLogForScheduledTime(medicationId: Int, date: Long, time: String): MedicationLog? {
        return logDao.getLogForScheduledTime(medicationId, date, time)
    }

    suspend fun logMedication(log: MedicationLog): Long {
        return logDao.insertLog(log)
    }

    suspend fun updateLog(log: MedicationLog) {
        logDao.updateLog(log)
    }

    suspend fun deleteLog(log: MedicationLog) {
        logDao.deleteLog(log)
    }
}
