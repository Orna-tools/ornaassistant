
package com.lloir.ornaassistant.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateTimeUtils {

    @RequiresApi(Build.VERSION_CODES.O)
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
    @RequiresApi(Build.VERSION_CODES.O)
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd")
    @RequiresApi(Build.VERSION_CODES.O)
    private val FULL_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")

    @RequiresApi(Build.VERSION_CODES.O)
    fun formatTime(dateTime: LocalDateTime): String {
        return dateTime.format(TIME_FORMATTER)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun formatDate(dateTime: LocalDateTime): String {
        return dateTime.format(DATE_FORMATTER)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun formatFullDateTime(dateTime: LocalDateTime): String {
        return dateTime.format(FULL_FORMATTER)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun formatDuration(startTime: LocalDateTime, endTime: LocalDateTime): String {
        val duration = ChronoUnit.SECONDS.between(startTime, endTime)
        val hours = duration / 3600
        val minutes = (duration % 3600) / 60
        val seconds = duration % 60

        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            minutes > 0 -> String.format("%d:%02d", minutes, seconds)
            else -> "${seconds}s"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun formatCooldownTime(endTime: LocalDateTime): String {
        val now = LocalDateTime.now()
        val duration = ChronoUnit.SECONDS.between(now, endTime)

        return when {
            duration <= 0 -> "Ready"
            duration < 60 -> "${duration}s"
            duration < 3600 -> "${duration / 60}m"
            duration < 86400 -> "${duration / 3600}h ${(duration % 3600) / 60}m"
            else -> "${duration / 86400}d"
        }
    }
}