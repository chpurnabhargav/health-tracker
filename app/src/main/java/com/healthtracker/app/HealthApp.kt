package com.healthtracker.app

import android.app.Application
import com.healthtracker.app.data.AppDatabase
import com.healthtracker.app.data.HealthRepository
import com.healthtracker.app.data.SettingsRepository
import com.healthtracker.app.health.HealthConnectManager
import com.healthtracker.app.reminders.Notifications
import com.healthtracker.app.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(app: Application) {
    val db: AppDatabase = AppDatabase.create(app)
    val settings = SettingsRepository(app) { com.healthtracker.app.widget.HealthWidget.refresh(app) }
    val repo = HealthRepository(db.dao(), settings) { com.healthtracker.app.widget.HealthWidget.refresh(app) }
    val health = HealthConnectManager(app)
}

class HealthApp : Application() {
    lateinit var container: AppContainer
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // The motivation photo feature was removed; clean up a photo saved by an older version.
        java.io.File(filesDir, "motivation.jpg").delete()
        Notifications.createChannels(this)
        appScope.launch {
            ReminderScheduler.scheduleAll(this@HealthApp, container.settings.current())
        }
    }
}

val android.content.Context.container: AppContainer
    get() = (applicationContext as HealthApp).container
