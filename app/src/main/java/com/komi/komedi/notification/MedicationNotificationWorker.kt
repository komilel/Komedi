package com.komi.komedi.notification

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Worker that periodically reschedules medication alarms.
 * This ensures alarms are maintained even if they get cleared by the system.
 * The actual notifications are handled by AlarmManager + MedicationAlarmReceiver.
 */
class MedicationNotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "MedicationNotificationWorker started - rescheduling alarms")
            NotificationScheduler.scheduleAllAlarms(context)
            Log.d(TAG, "MedicationNotificationWorker completed")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "MedicationNotificationWorker failed", e)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "MedNotificationWorker"
        const val WORK_NAME = "medication_notification_work"

        /**
         * Schedule periodic work to maintain alarms.
         * Runs every 6 hours to ensure alarms are rescheduled.
         */
        fun schedule(context: Context) {
            Log.d(TAG, "Scheduling periodic alarm maintenance worker")

            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<MedicationNotificationWorker>(
                6, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInitialDelay(6, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )

            Log.d(TAG, "Periodic worker scheduled")
        }

        /**
         * Run once to immediately reschedule all alarms.
         * Called from Settings test button.
         */
        fun runOnce(context: Context) {
            Log.d(TAG, "Running one-time alarm reschedule")

            val workRequest = OneTimeWorkRequestBuilder<MedicationNotificationWorker>()
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Periodic worker cancelled")
        }
    }
}
