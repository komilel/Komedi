package com.komi.komedi

import android.app.Application
import com.komi.komedi.di.AppContainer
import com.komi.komedi.notification.MedicationNotificationWorker
import com.komi.komedi.notification.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KomediApplication : Application() {

    lateinit var appContainer: AppContainer

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)

        // Schedule medication reminder alarms immediately
        applicationScope.launch(Dispatchers.IO) {
            NotificationScheduler.scheduleAllAlarms(this@KomediApplication)
        }

        // Also schedule periodic worker to maintain alarms
        MedicationNotificationWorker.schedule(this)
    }
}
