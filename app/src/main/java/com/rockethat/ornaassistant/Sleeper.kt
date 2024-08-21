package com.rockethat.ornaassistant

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

data class Sleeper(val discordName: String, val immunity: Boolean, val endTime: LocalDateTime) {
    fun endTimeLeft(): Long {
        val nowUTC = LocalDateTime.now(ZoneOffset.UTC)
        return -ChronoUnit.SECONDS.between(endTime, nowUTC)
    }
}