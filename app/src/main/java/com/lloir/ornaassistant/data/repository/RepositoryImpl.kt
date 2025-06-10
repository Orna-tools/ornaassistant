package com.lloir.ornaassistant.data.repository

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.lloir.ornaassistant.OrnaAssistantApplication
import com.lloir.ornaassistant.R
import com.lloir.ornaassistant.domain.repository.NotificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager
) : NotificationRepository {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun scheduleWayvesselNotification(wayvesselName: String, delayMinutes: Long) {
        val data = Data.Builder()
            .putString("wayvessel_name", wayvesselName)
            .build()

        val work = OneTimeWorkRequestBuilder<WayvesselNotificationWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(data)
            .addTag("wayvessel_notification_$wayvesselName")
            .build()

        workManager.enqueue(work)
    }

    override suspend fun cancelWayvesselNotification(wayvesselName: String) {
        workManager.cancelAllWorkByTag("wayvessel_notification_$wayvesselName")
    }

    override suspend fun showServiceNotification() {
        val notification = NotificationCompat.Builder(context, OrnaAssistantApplication.SERVICE_CHANNEL_ID)
            .setContentTitle("Orna Assistant")
            .setContentText("Accessibility service is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(ONGOING_NOTIFICATION_ID, notification)
    }

    override suspend fun hideServiceNotification() {
        notificationManager.cancel(ONGOING_NOTIFICATION_ID)
    }

    override suspend fun showOverlayNotification(message: String) {
        val notification = NotificationCompat.Builder(context, OrnaAssistantApplication.WAYVESSEL_CHANNEL_ID)
            .setContentTitle("Orna Assistant")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        private const val ONGOING_NOTIFICATION_ID = 1001
    }
}

// Worker for scheduled notifications
class WayvesselNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val wayvesselName = inputData.getString("wayvessel_name") ?: return Result.failure()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(applicationContext, OrnaAssistantApplication.WAYVESSEL_CHANNEL_ID)
            .setContentTitle("Wayvessel Ready")
            .setContentText("$wayvesselName's wayvessel is now available!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)

        return Result.success()
    }
}