package com.lloir.ornaassistant.utils

import android.os.Build
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.*

object DateCompat {
    private val legacyFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val modernFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun now(): Any {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.now()
        } else {
            Date()
        }
    }

    fun format(dateTime: Any): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (dateTime as LocalDateTime).format(modernFormatter)
        } else {
            legacyFormatter.format(dateTime as Date)
        }
    }

    fun parse(dateString: String): Any {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.parse(dateString, modernFormatter)
        } else {
            legacyFormatter.parse(dateString) ?: Date()
        }
    }
}