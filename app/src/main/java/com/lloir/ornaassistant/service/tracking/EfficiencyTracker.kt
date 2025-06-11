package com.lloir.ornaassistant.service.tracking

import com.lloir.ornaassistant.domain.model.*
import com.lloir.ornaassistant.domain.repository.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime
import java.time.Duration

@Singleton
class EfficiencyTracker @Inject constructor(
    private val dungeonRepository: DungeonRepository,
    private val wayvesselRepository: WayvesselRepository,
    private val itemAssessmentRepository: ItemAssessmentRepository
) {
    private val _currentMetrics = MutableStateFlow<EfficiencyMetrics?>(null)
    val currentMetrics: StateFlow<EfficiencyMetrics?> = _currentMetrics.asStateFlow()

    private val _recentMilestones = MutableStateFlow<List<EfficiencyMilestone>>(emptyList())
    val recentMilestones = _recentMilestones.asStateFlow()

    suspend fun updateMetrics(session: WayvesselSession?) {
        if (session == null || session.durationSeconds == 0L) {
            _currentMetrics.value = null
            return
        }

        val hoursElapsed = session.durationSeconds / 3600.0
        if (hoursElapsed < 0.01) return // Less than 36 seconds

        val ornsPerHour = (session.orns / hoursElapsed).toLong()
        val expPerHour = (session.experience / hoursElapsed).toLong()
        val goldPerHour = (session.gold / hoursElapsed).toLong()
        val dungeonsPerHour = session.dungeonsVisited / hoursElapsed.toFloat()

        // Count items found in this session
        val sessionStart = session.startTime
        val itemsFound = itemAssessmentRepository.getAllAssessments()
            .first()
            .count { it.timestamp.isAfter(sessionStart) }
        val itemsPerHour = itemsFound / hoursElapsed.toFloat()

        val metrics = EfficiencyMetrics(
            sessionId = session.id,
            timestamp = LocalDateTime.now(),
            ornsPerHour = ornsPerHour,
            expPerHour = expPerHour,
            goldPerHour = goldPerHour,
            dungeonsPerHour = dungeonsPerHour,
            itemsPerHour = itemsPerHour
        )

        _currentMetrics.value = metrics
        checkMilestones(metrics)
    }

    private fun checkMilestones(metrics: EfficiencyMetrics) {
        val milestones = mutableListOf<EfficiencyMilestone>()

        // Orns milestones
        when {
            metrics.ornsPerHour >= 10_000_000 -> milestones.add(
                EfficiencyMilestone(
                    MilestoneType.ORNS_PER_HOUR,
                    10_000_000,
                    "10m orns/hr",
                    "Legendary grind speed!"
                )
            )
            metrics.ornsPerHour >= 5_000_000 -> milestones.add(
                EfficiencyMilestone(
                    MilestoneType.ORNS_PER_HOUR,
                    5_000_000,
                    "5m orns/hr",
                    "Elite efficiency!"
                )
            )
            metrics.ornsPerHour >= 1_000_000 -> milestones.add(
                EfficiencyMilestone(
                    MilestoneType.ORNS_PER_HOUR,
                    1_000_000,
                    "1m orns/hr",
                    "Great pace!"
                )
            )
        }

        // Dungeon speed milestones
        when {
            metrics.dungeonsPerHour >= 10 -> milestones.add(
                EfficiencyMilestone(
                    MilestoneType.DUNGEONS_PER_HOUR,
                    10,
                    "10 dungeons/hr",
                    "Speed demon!"
                )
            )
        }

        _recentMilestones.value = milestones
    }

    fun formatLargeNumber(number: Long): String {
        return when {
            number >= 1_000_000_000 -> "%.1fb".format(number / 1_000_000_000.0)
            number >= 1_000_000 -> "%.1fm".format(number / 1_000_000.0)
            number >= 1_000 -> "%.1fk".format(number / 1_000.0)
            else -> number.toString()
        }
    }
}
