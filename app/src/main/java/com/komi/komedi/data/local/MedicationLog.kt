package com.komi.komedi.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medication_logs",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medicationId")]
)
data class MedicationLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val medicationId: Int,
    val scheduledTime: String,     // The time it was scheduled for (e.g., "08:00")
    val takenAt: Long,             // Actual timestamp when taken
    val date: Long,                // Date (start of day) for grouping
    val taken: Boolean = true,
    val skipped: Boolean = false,
    val notes: String = ""
)
