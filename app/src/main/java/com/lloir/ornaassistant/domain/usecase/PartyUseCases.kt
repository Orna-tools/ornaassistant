package com.lloir.ornaassistant.domain.usecase

import com.lloir.ornaassistant.domain.model.*
import com.lloir.ornaassistant.domain.repository.*
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetPartyInvitesUseCase @Inject constructor(
    private val wayvesselRepository: WayvesselRepository,
    private val dungeonRepository: DungeonRepository
) {
    suspend operator fun invoke(inviterNames: List<String>): List<PartyInviteInfo> {
        return inviterNames.map { inviterName ->
            val lastSession = wayvesselRepository.getLastSessionsFor(inviterName, 1).firstOrNull()

            val dungeonCounts = if (lastSession != null) {
                // Get visits for this session
                val visits = dungeonRepository.getVisitsForSession(lastSession.id).first()

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