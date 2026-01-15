package com.komi.komedi.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications ORDER BY name ASC")
    fun getAllMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE id = :id")
    fun getMedicationById(id: Int): Flow<Medication?>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getMedicationByIdOnce(id: Int): Medication?

    @Query("SELECT * FROM medications WHERE name LIKE '%' || :query || '%'")
    fun searchMedications(query: String): Flow<List<Medication>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication): Long

    @Update
    suspend fun updateMedication(medication: Medication)

    @Delete
    suspend fun deleteMedication(medication: Medication)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteMedicationById(id: Int)
}

@Dao
interface MedicationLogDao {
    @Query("SELECT * FROM medication_logs WHERE medicationId = :medicationId ORDER BY takenAt DESC")
    fun getLogsForMedication(medicationId: Int): Flow<List<MedicationLog>>

    @Query("SELECT * FROM medication_logs WHERE date = :date ORDER BY scheduledTime ASC")
    fun getLogsForDate(date: Long): Flow<List<MedicationLog>>

    @Query("SELECT * FROM medication_logs WHERE medicationId = :medicationId AND date = :date")
    fun getLogsForMedicationOnDate(medicationId: Int, date: Long): Flow<List<MedicationLog>>

    @Query("SELECT * FROM medication_logs WHERE medicationId = :medicationId AND date = :date AND scheduledTime = :time LIMIT 1")
    suspend fun getLogForScheduledTime(medicationId: Int, date: Long, time: String): MedicationLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MedicationLog): Long

    @Update
    suspend fun updateLog(log: MedicationLog)

    @Delete
    suspend fun deleteLog(log: MedicationLog)

    @Query("DELETE FROM medication_logs WHERE medicationId = :medicationId")
    suspend fun deleteLogsForMedication(medicationId: Int)
}
