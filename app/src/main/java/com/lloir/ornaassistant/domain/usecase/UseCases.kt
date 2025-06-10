package com.lloir.ornaassistant.domain.usecase

import com.lloir.ornaassistant.domain.model.*
import com.lloir.ornaassistant.domain.repository.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetDungeonStatisticsUseCase @Inject constructor(
    private val dungeonRepository: DungeonRepository
) {
    suspend operator fun invoke(days: Int = 7): DungeonStatistics {
        val startDate = LocalDateTime.now().minusDays(days.toLong())
        return dungeonRepository.getStatistics(startDate)
    }
}

@Singleton
class GetWeeklyStatisticsUseCase @Inject constructor(
    private val dungeonRepository: DungeonRepository
) {
    suspend operator fun invoke(): WeeklyStatistics {
        val now = LocalDateTime.now()
        val startOfWeek = now.minusDays(6) // Last 7 days

        val visits = dungeonRepository.getVisitsBetween(startOfWeek, now)

        val visitsByDay = Array(7) { 0 }
        val ornsByDay = Array(7) { 0L }

        visits.forEach { visit ->
            val dayIndex = java.time.temporal.ChronoUnit.DAYS.between(startOfWeek.toLocalDate(), visit.startTime.toLocalDate()).toInt()
            if (dayIndex in 0..6) {
                visitsByDay[dayIndex]++
                ornsByDay[dayIndex] += visit.orns
            }
        }

        return WeeklyStatistics(
            mondayVisits = visitsByDay[0],
            tuesdayVisits = visitsByDay[1],
            wednesdayVisits = visitsByDay[2],
            thursdayVisits = visitsByDay[3],
            fridayVisits = visitsByDay[4],
            saturdayVisits = visitsByDay[5],
            sundayVisits = visitsByDay[6],
            mondayOrns = ornsByDay[0],
            tuesdayOrns = ornsByDay[1],
            wednesdayOrns = ornsByDay[2],
            thursdayOrns = ornsByDay[3],
            fridayOrns = ornsByDay[4],
            saturdayOrns = ornsByDay[5],
            sundayOrns = ornsByDay[6]
        )
    }
}

@Singleton
class TrackDungeonVisitUseCase @Inject constructor(
    private val dungeonRepository: DungeonRepository,
    private val wayvesselRepository: WayvesselRepository
) {
    suspend operator fun invoke(
        dungeonName: String,
        mode: DungeonMode,
        sessionId: Long? = null
    ): DungeonVisit {
        val visit = DungeonVisit(
            sessionId = sessionId,
            name = dungeonName,
            mode = mode,
            startTime = LocalDateTime.now()
        )

        val id = dungeonRepository.insertVisit(visit)
        return visit.copy(id = id)
    }
}

@Singleton
class UpdateDungeonVisitUseCase @Inject constructor(
    private val dungeonRepository: DungeonRepository
) {
    suspend operator fun invoke(
        visit: DungeonVisit,
        orns: Long? = null,
        gold: Long? = null,
        experience: Long? = null,
        floor: Long? = null,
        godforges: Long? = null,
        completed: Boolean? = null,
        duration: Long? = null
    ) {
        val updatedVisit = visit.copy(
            orns = orns ?: visit.orns,
            gold = gold ?: visit.gold,
            experience = experience ?: visit.experience,
            floor = floor ?: visit.floor,
            godforges = godforges ?: visit.godforges,
            completed = completed ?: visit.completed,
            durationSeconds = duration ?: visit.durationSeconds
        )

        dungeonRepository.updateVisit(updatedVisit)
    }
}

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

@Singleton
class AssessItemUseCase @Inject constructor(
    private val itemAssessmentRepository: ItemAssessmentRepository
) {
    suspend operator fun invoke(
        itemName: String,
        level: Int,
        attributes: Map<String, Int>
    ): AssessmentResult {
        val result = itemAssessmentRepository.assessItem(itemName, level, attributes)

        // Save assessment to database
        val assessment = ItemAssessment(
            itemName = itemName,
            level = level,
            attributes = attributes,
            assessmentResult = result,
            timestamp = LocalDateTime.now(),
            quality = result.quality
        )

        itemAssessmentRepository.insertAssessment(assessment)

        return result
    }
}

@Singleton
class GetPartyInvitesUseCase @Inject constructor(
    private val wayvesselRepository: WayvesselRepository,
    private val dungeonRepository: DungeonRepository
) {
    suspend operator fun invoke(inviterNames: List<String>): List<PartyInviteInfo> {
        return inviterNames.map { inviterName ->
            val lastSession = wayvesselRepository.getLastSessionsFor(inviterName, 1).firstOrNull()

            val dungeonCounts = if (lastSession != null) {
                val visits = dungeonRepository.getVisitsForSession(lastSession.id)
                    .let { flow ->
                        // Convert Flow to List for this use case
                        // In real implementation, you might want to collect this differently
                        emptyList<DungeonVisit>()
                    }

                DungeonCounts(
                    normal = visits.count { it.name.lowercase().endsWith("dungeon") && it.mode.type == DungeonMode.Type.NORMAL },
                    vog = visits.count { it.name.lowercase().contains("valley") },
                    bg = visits.count { it.name.lowercase().contains("battle") },
                    dragon = visits.count { it.name.lowercase().contains("dragon") },
                    underworld = visits.count { it.name.lowercase().contains("underworld") },
                    chaos = visits.count { it.name.lowercase().contains("chaos") }
                )
            } else {
                DungeonCounts()
            }

            val cooldownStatus = if (lastSession != null) {
                calculateCooldownStatus(lastSession.startTime)
            } else {
                "Ready"
            }

            PartyInviteInfo(
                inviterName = inviterName,
                dungeonCounts = dungeonCounts,
                cooldownStatus = cooldownStatus,
                isOnCooldown = cooldownStatus != "Ready"
            )
        }
    }

    private fun calculateCooldownStatus(lastSessionTime: LocalDateTime): String {
        val now = LocalDateTime.now()
        val duration = java.time.Duration.between(lastSessionTime, now)

        return when {
            duration.toHours() >= 1 -> "Ready"
            duration.toMinutes() > 0 -> "${duration.toMinutes()} min"
            else -> "${duration.seconds} sec"
        }
    }
}

// Supporting data classes
data class PartyInviteInfo(
    val inviterName: String,
    val dungeonCounts: DungeonCounts,
    val cooldownStatus: String,
    val isOnCooldown: Boolean
)

data class DungeonCounts(
    val normal: Int = 0,
    val vog: Int = 0,
    val bg: Int = 0,
    val dragon: Int = 0,
    val underworld: Int = 0,
    val chaos: Int = 0
)