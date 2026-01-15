package com.komi.komedi.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.komi.komedi.MainActivity
import java.time.LocalDate

class MedicationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm received!")

        val medicationId = intent.getIntExtra(EXTRA_MEDICATION_ID, -1)
        val medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: "Medication"
        val dosage = intent.getStringExtra(EXTRA_DOSAGE) ?: ""
        val scheduledTime = intent.getStringExtra(EXTRA_SCHEDULED_TIME) ?: ""
        val instructions = intent.getStringExtra(EXTRA_INSTRUCTIONS) ?: "Time to take your medication"

        Log.d(TAG, "Medication: $medicationName, ID: $medicationId, Time: $scheduledTime")

        if (medicationId == -1) {
            Log.e(TAG, "Invalid medication ID")
            return
        }

        // Check if notifications are enabled
        val prefs = context.getSharedPreferences("komedi_settings", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)

        if (!notificationsEnabled) {
            Log.d(TAG, "Notifications disabled, skipping")
            return
        }

        // Check for duplicate notification (prevent sending same notification twice)
        val notificationTracker = context.getSharedPreferences("notification_tracker", Context.MODE_PRIVATE)
        val todayStr = LocalDate.now().toString()
        val notificationKey = "$todayStr-$medicationId-$scheduledTime"

        if (notificationTracker.getBoolean(notificationKey, false)) {
            Log.d(TAG, "Notification already sent for $notificationKey, skipping duplicate")
            return
        }

        // Clean up old entries (keep only today's)
        val editor = notificationTracker.edit()
        notificationTracker.all.keys
            .filter { !it.startsWith(todayStr) }
            .forEach { editor.remove(it) }

        // Mark this notification as sent
        editor.putBoolean(notificationKey, true)
        editor.apply()

        createNotificationChannel(context)
        showNotification(context, medicationId, medicationName, dosage, scheduledTime, instructions)

        // Reschedule for next day
        NotificationScheduler.scheduleNextDayAlarm(context, medicationId, medicationName, dosage, scheduledTime, instructions)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Medication Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for medication reminders"
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(
        context: Context,
        medicationId: Int,
        medicationName: String,
        dosage: String,
        scheduledTime: String,
        instructions: String
    ) {
        val notificationId = medicationId * 100 + scheduledTime.hashCode()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationText = if (dosage.isNotBlank()) "$dosage at $scheduledTime" else "Scheduled at $scheduledTime"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("$medicationName")
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$notificationText\n$instructions"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        NotificationManagerCompat.from(context).notify(notificationId, notification)
        Log.d(TAG, "Notification shown with ID: $notificationId")
    }

    companion object {
        private const val TAG = "MedAlarmReceiver"
        const val CHANNEL_ID = "medication_reminders"
        const val EXTRA_MEDICATION_ID = "medication_id"
        const val EXTRA_MEDICATION_NAME = "medication_name"
        const val EXTRA_DOSAGE = "dosage"
        const val EXTRA_SCHEDULED_TIME = "scheduled_time"
        const val EXTRA_INSTRUCTIONS = "instructions"
    }
}
