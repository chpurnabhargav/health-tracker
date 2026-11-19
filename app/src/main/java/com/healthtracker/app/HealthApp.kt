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
    val settings = SettingsRepository(app)
    val repo = HealthRepository(db.dao(), settings)
    val health = HealthConnectManager(app)
}

class HealthApp : Application() {
    lateinit var container: AppContainer
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        appScope.launch {
            ReminderScheduler.scheduleAll(this@HealthApp, container.settings.current())
        }
    }
}

val android.content.Context.container: AppContainer
    get() = (applicationContext as HealthApp).container
