package com.komi.komedi.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.komi.komedi.data.local.AppDatabase
import com.komi.komedi.data.local.Medication
import com.komi.komedi.data.local.getScheduleTimesList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object NotificationScheduler {
    private const val TAG = "NotificationScheduler"

    /**
     * Schedule all medication alarms for today and tomorrow.
     * Should be called on app start and when medications are added/updated.
     */
    suspend fun scheduleAllAlarms(context: Context) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Scheduling all medication alarms")

        val prefs = context.getSharedPreferences("komedi_settings", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        val minutesBefore = prefs.getInt("notification_time", 15)

        if (!notificationsEnabled) {
            Log.d(TAG, "Notifications disabled, cancelling all alarms")
            cancelAllAlarms(context)
            return@withContext
        }

        val database = AppDatabase.getDatabase(context)
        val medications = database.medicationDao().getAllActiveMedications().first()

        Log.d(TAG, "Found ${medications.size} active medications")

        for (medication in medications) {
            scheduleMedicationAlarms(context, medication, minutesBefore)
        }
    }

    /**
     * Schedule alarms for a single medication.
     * Uses "K minutes or less" logic: if notification time has passed but medication
     * time hasn't, schedule an immediate notification.
     */
    fun scheduleMedicationAlarms(context: Context, medication: Medication, minutesBefore: Int) {
        val times = medication.getScheduleTimesList()
        Log.d(TAG, "Scheduling alarms for ${medication.name}: $times, minutesBefore: $minutesBefore")

        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        for (timeStr in times) {
            try {
                val scheduledTime = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))

                // Calculate notification time (X minutes before scheduled time)
                val notifyTime = scheduledTime.minusMinutes(minutesBefore.toLong())

                val todayMedDateTime = LocalDateTime.of(today, scheduledTime)
                val todayNotifyDateTime = LocalDateTime.of(today, notifyTime)

                // Schedule for today based on "K minutes or less" logic
                when {
                    // Case 1: Notification time is still in the future - schedule normally
                    todayNotifyDateTime.isAfter(now) -> {
                        scheduleAlarm(
                            context,
                            medication,
                            timeStr,
                            todayNotifyDateTime
                        )
                        Log.d(TAG, "Scheduled normal alarm for ${medication.name} at $todayNotifyDateTime")
                    }
                    // Case 2: Notification time passed, but medication time is still in future
                    // Schedule immediate notification (in 5 seconds to avoid race conditions)
                    todayMedDateTime.isAfter(now) -> {
                        val immediateTime = now.plusSeconds(5)
                        scheduleAlarm(
                            context,
                            medication,
                            timeStr,
                            immediateTime
                        )
                        Log.d(TAG, "Scheduled IMMEDIATE alarm for ${medication.name} (med time $timeStr still in future)")
                    }
                    // Case 3: Both notification and medication time have passed - skip today
                    else -> {
                        Log.d(TAG, "Skipping today's alarm for ${medication.name} at $timeStr (already passed)")
                    }
                }

                // Also schedule for tomorrow to ensure continuity
                val tomorrowNotifyDateTime = LocalDateTime.of(tomorrow, notifyTime)
                scheduleAlarm(
                    context,
                    medication,
                    timeStr,
                    tomorrowNotifyDateTime
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling alarm for time: $timeStr", e)
            }
        }
    }

    private fun scheduleAlarm(
        context: Context,
        medication: Medication,
        scheduledTime: String,
        notifyDateTime: LocalDateTime
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra(MedicationAlarmReceiver.EXTRA_MEDICATION_ID, medication.id)
            putExtra(MedicationAlarmReceiver.EXTRA_MEDICATION_NAME, medication.name)
            putExtra(MedicationAlarmReceiver.EXTRA_DOSAGE, medication.dosageAmount)
            putExtra(MedicationAlarmReceiver.EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(MedicationAlarmReceiver.EXTRA_INSTRUCTIONS, medication.instructions.ifEmpty { "Time to take your medication" })
        }

        // Unique request code per medication + time + date
        val requestCode = generateRequestCode(medication.id, scheduledTime, notifyDateTime.toLocalDate())

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTimeMillis = notifyDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled exact alarm for ${medication.name} at $notifyDateTime (notify before $scheduledTime)")
                } else {
                    // Fall back to inexact alarm if exact alarms not allowed
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled inexact alarm for ${medication.name} at $notifyDateTime (exact alarms not allowed)")
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled exact alarm for ${medication.name} at $notifyDateTime")
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled alarm for ${medication.name} at $notifyDateTime")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling alarm - exact alarms may not be permitted", e)
            // Fall back to inexact
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        }
    }

    /**
     * Schedule alarm for the next day (called after an alarm fires).
     */
    fun scheduleNextDayAlarm(
        context: Context,
        medicationId: Int,
        medicationName: String,
        dosage: String,
        scheduledTime: String,
        instructions: String
    ) {
        val prefs = context.getSharedPreferences("komedi_settings", Context.MODE_PRIVATE)
        val minutesBefore = prefs.getInt("notification_time", 15)

        try {
            val time = LocalTime.parse(scheduledTime, DateTimeFormatter.ofPattern("HH:mm"))
            val notifyTime = time.minusMinutes(minutesBefore.toLong())
            val tomorrow = LocalDate.now().plusDays(1)
            val notifyDateTime = LocalDateTime.of(tomorrow, notifyTime)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
                putExtra(MedicationAlarmReceiver.EXTRA_MEDICATION_ID, medicationId)
                putExtra(MedicationAlarmReceiver.EXTRA_MEDICATION_NAME, medicationName)
                putExtra(MedicationAlarmReceiver.EXTRA_DOSAGE, dosage)
                putExtra(MedicationAlarmReceiver.EXTRA_SCHEDULED_TIME, scheduledTime)
                putExtra(MedicationAlarmReceiver.EXTRA_INSTRUCTIONS, instructions)
            }

            val requestCode = generateRequestCode(medicationId, scheduledTime, tomorrow)

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTimeMillis = notifyDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }

            Log.d(TAG, "Scheduled next day alarm for $medicationName at $notifyDateTime")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling next day alarm", e)
        }
    }

    /**
     * Cancel all medication alarms.
     */
    fun cancelAllAlarms(context: Context) {
        Log.d(TAG, "Cancelling all alarms")
        // Note: We can't easily cancel all alarms without tracking request codes
        // For now, this is a placeholder - individual cancellation happens when meds are deleted
    }

    /**
     * Cancel alarms for a specific medication.
     */
    fun cancelMedicationAlarms(context: Context, medicationId: Int, times: List<String>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        for (timeStr in times) {
            // Cancel for today and tomorrow
            for (date in listOf(today, tomorrow)) {
                val requestCode = generateRequestCode(medicationId, timeStr, date)

                val intent = Intent(context, MedicationAlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )

                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                    Log.d(TAG, "Cancelled alarm for medication $medicationId, time $timeStr, date $date")
                }
            }
        }
    }

    private fun generateRequestCode(medicationId: Int, time: String, date: LocalDate): Int {
        return (medicationId.toString() + time.replace(":", "") + date.toString().replace("-", "")).hashCode()
    }
}
