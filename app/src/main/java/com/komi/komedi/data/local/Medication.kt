package com.komi.komedi.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String = "",
    val dosageAmount: String,        // e.g., "20mg", "500ml"
    val dosageUnit: String = "pill", // pill, ml, tablet, etc.
    val frequency: Int = 1,          // times per day
    val scheduleTimes: String = "",  // comma-separated times: "08:00,14:00,20:00"
    val instructions: String = "",   // e.g., "After breakfast", "With water"
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val notes: String = "",
    val iconType: String = "pill",   // pill, capsule, liquid, injection
    val isActive: Boolean = true
)

// Helper extension to parse schedule times
fun Medication.getScheduleTimesList(): List<String> {
    return if (scheduleTimes.isBlank()) emptyList()
    else scheduleTimes.split(",").map { it.trim() }
}

fun Medication.setScheduleTimesList(times: List<String>): Medication {
    return this.copy(scheduleTimes = times.joinToString(","))
}
