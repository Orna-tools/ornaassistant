package com.lloir.ornaassistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.lloir.ornaassistant.domain.assessment.EnhancedItemDatabase
import com.lloir.ornaassistant.domain.model.AppSettings
import com.lloir.ornaassistant.domain.repository.ItemDatabase
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import com.lloir.ornaassistant.utils.AccessibilityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class OrnaAssistantApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var itemDatabase: ItemDatabase

    @Inject
    lateinit var settingsRepository: SettingsRepository

    // Application-level coroutine scope
    private val appScope = CoroutineScope(Dispatchers.Main)

    // Implement the required property for Configuration.Provider
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        initializeItemDatabase()
        initializeAccessibilityFeatures()
    }

    private fun initializeAccessibilityFeatures() {
        // TextToSpeech initialization has been removed
        appScope.launch {
            try {
                // Still fetch settings for potential future accessibility features
                val settings = settingsRepository.getSettingsFlow().first()
                // TextToSpeech initialization code removed
            } catch (e: Exception) {
                Log.e("OrnaAssistantApp", "Failed to initialize accessibility features", e)
            }
        }
    }

    override fun onTerminate() {
        // TextToSpeech cleanup has been removed
        super.onTerminate()
    }

    private fun initializeItemDatabase() {
        try {
            // Set the ItemDatabase instance in the compatibility layer
            EnhancedItemDatabase.setItemDatabase(itemDatabase)

            // Initialize in a coroutine since it's a suspend function
            CoroutineScope(Dispatchers.IO).launch {
                itemDatabase.initialize(this@OrnaAssistantApplication)
            }
        } catch (e: Exception) {
            Log.e("OrnaAssistantApp", "Failed to initialize item database", e)
        }
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
