package com.lloir.ornaassistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class NotificationManager(private val context: Context) {

    private val wayvesselNotificationChannelName = "ornaassistant_channel_wayvessel"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            wayvesselNotificationChannelName,
            wayvesselNotificationChannelName,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            enableVibration(true)
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun scheduleWayvesselNotification(delayMinutes: Long) {
        val notificationContent = // Corrected line
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

            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                val builder = NotificationCompat.Builder(context, notificationContent.channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(notificationContent.title)
                    .setContentText(notificationContent.description)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                with(NotificationManagerCompat.from(context)) {
                    notify(0, builder.build())
                }
            } else {
                // Handle the case where notification permission is not granted
            }

            return Result.success()
        }
    }
}