package com.rockethat.ornaassistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import androidx.work.R
import java.util.concurrent.TimeUnit

class NotificationManager(private val context: Context) {

    private val wayvesselNotificationChannelName = "ornaassistant_channel_wayvessel"

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                wayvesselNotificationChannelName,
                wayvesselNotificationChannelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, name: String, importance: Int) {
        val channel = NotificationChannel(channelId, name, importance).apply {
            enableVibration(true)
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun scheduleWayvesselNotification(delayMinutes: Long) {
        val notificationContent =
            NotificationContent(wayvesselNotificationChannelName, "Wayvessel", "Wayvessel is open!")
        val work = OneTimeWorkRequestBuilder<OneTimeScheduleWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .addTag("WayvesselNotification")
            .setInputData(
                workDataOf(
                    "channelId" to notificationContent.channelId,
                    "title" to notificationContent.title,
                    "description" to notificationContent.description
                )
            )
            .build()

        WorkManager.getInstance(context).enqueue(work)
    }

    data class NotificationContent(
        val channelId: String,
        val title: String,
        val description: String
    )

    class OneTimeScheduleWorker(
        private val context: Context,
        workerParams: WorkerParameters
    ) : Worker(context, workerParams) {

        override fun doWork(): Result {
            val notificationContent = NotificationContent(
                inputData.getString("channelId") ?: "",
                inputData.getString("title") ?: "",
                inputData.getString("description") ?: ""
            )

            // Check if notification permission is granted
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                val builder = NotificationCompat.Builder(context, notificationContent.channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(notificationContent.title)
                    .setContentText(notificationContent.description)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                with(NotificationManagerCompat.from(context)) {
                    val notificationId = 0 // Consider using a unique ID
                    notify(notificationId, builder.build())
                }
            } else {
                // Handle the case where notification permission is not granted
                // You might want to log a message or show a notification in the app
            }

            return Result.success()
        }
    }
}