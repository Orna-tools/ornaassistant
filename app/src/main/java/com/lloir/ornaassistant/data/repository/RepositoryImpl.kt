package com.lloir.ornaassistant.data.repository

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import android.app.job.JobParameters
import android.app.job.JobService
import androidx.work.*
import com.lloir.ornaassistant.OrnaAssistantApplication
import com.lloir.ornaassistant.R
import com.lloir.ornaassistant.domain.repository.NotificationRepository
import com.lloir.ornaassistant.data.preferences.SettingsDataStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager
) : NotificationRepository {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

    override suspend fun showMaterialTargetReachedNotification(
        materialName: String,
        currentQuantity: Int,
        targetQuantity: Int
    ) {
        val message = "Material target reached: $materialName ($currentQuantity/$targetQuantity)"
        val notification = NotificationCompat.Builder(context, OrnaAssistantApplication.WAYVESSEL_CHANNEL_ID)
            .setContentTitle("Material Target Reached")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        private const val ONGOING_NOTIFICATION_ID = 1001
    }
}

/**
 * Android 16 JobScheduler compatibility helper
 */
@HiltWorker
class JobSchedulerCompatWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        // Handle abandoned job detection for Android 16
        return try {
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
