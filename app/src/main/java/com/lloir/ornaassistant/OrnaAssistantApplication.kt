package com.lloir.ornaassistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class OrnaAssistantApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Implement the required property for Configuration.Provider
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Wayvessel notification channel
            val wayvesselChannel = NotificationChannel(
                WAYVESSEL_CHANNEL_ID,
                "Wayvessel Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for wayvessel cooldown alerts"
                enableVibration(true)
            }

            // Service notification channel
            val serviceChannel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Service Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent notification for accessibility service"
            }

            notificationManager.createNotificationChannels(
                listOf(wayvesselChannel, serviceChannel)
            )
        }
    }

    companion object {
        const val WAYVESSEL_CHANNEL_ID = "wayvessel_notifications"
        const val SERVICE_CHANNEL_ID = "service_notifications"
    }
}