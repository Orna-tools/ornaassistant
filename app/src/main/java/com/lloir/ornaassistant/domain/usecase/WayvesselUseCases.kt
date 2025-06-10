package com.lloir.ornaassistant.domain.usecase

import com.lloir.ornaassistant.domain.model.*
import com.lloir.ornaassistant.domain.repository.*
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartWayvesselSessionUseCase @Inject constructor(
    private val wayvesselRepository: WayvesselRepository,
    private val notificationRepository: NotificationRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(wayvesselName: String): WayvesselSession {
        val session = WayvesselSession(
            name = wayvesselName,
            startTime = LocalDateTime.now()
        )

        val id = wayvesselRepository.insertSession(session)
        val sessionWithId = session.copy(id = id)

        // Schedule wayvessel notification if enabled
        val settings = settingsRepository.getSettings()
        if (settings.wayvesselNotifications) {
            notificationRepository.scheduleWayvesselNotification(wayvesselName, 60)
        }

        return sessionWithId
    }
}

@Singleton
class EndWayvesselSessionUseCase @Inject constructor(
    private val wayvesselRepository: WayvesselRepository
) {
    suspend operator fun invoke(session: WayvesselSession) {
        val endTime = LocalDateTime.now()
        val duration = java.time.Duration.between(session.startTime, endTime).seconds

        val completedSession = session.copy(durationSeconds = duration)
        wayvesselRepository.updateSession(completedSession)
    }
}