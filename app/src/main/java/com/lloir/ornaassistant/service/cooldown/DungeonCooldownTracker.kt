package com.lloir.ornaassistant.service.cooldown

import com.lloir.ornaassistant.domain.model.DungeonCooldown
import com.lloir.ornaassistant.domain.repository.DungeonRepository
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DungeonCooldownTracker @Inject constructor(
    private val dungeonRepository: DungeonRepository
) {
    private val _cooldowns = MutableStateFlow<List<DungeonCooldown>>(emptyList())
    val cooldowns: StateFlow<List<DungeonCooldown>> = _cooldowns.asStateFlow()

    private val _readyDungeons = MutableStateFlow<List<String>>(emptyList())
    val readyDungeons: StateFlow<List<String>> = _readyDungeons.asStateFlow()

    suspend fun updateCooldowns() {
        val recentVisits = dungeonRepository.getRecentVisits(days = 1)
        
        val latestVisits = recentVisits
            .groupBy { it.name }
            .mapValues { (_, visits) -> visits.maxBy { it.startTime } }

        val now = LocalDateTime.now()
        val cooldownList = mutableListOf<DungeonCooldown>()
        val readyList = mutableListOf<String>()

        latestVisits.forEach { (dungeonName, visit) ->
            val cooldownHours = visit.cooldownHours().toInt()
            val cooldownEnd = visit.cooldownEndTime()
            val isReady = now.isAfter(cooldownEnd)

            cooldownList.add(
                DungeonCooldown(
                    dungeonName = dungeonName,
                    lastVisitTime = visit.startTime,
                    cooldownHours = cooldownHours,
                    cooldownEndTime = cooldownEnd,
                    isReady = isReady
                )
            )

            if (isReady) {
                readyList.add(dungeonName)
            }
        }

        _cooldowns.value = cooldownList.sortedWith(
            compareBy(
                { !it.isReady },
                { it.cooldownEndTime }
            )
        )
        
        _readyDungeons.value = readyList
    }

    fun getTimeRemaining(cooldown: DungeonCooldown): String {
        if (cooldown.isReady) return "Ready"
        
        val now = LocalDateTime.now()
        val duration = Duration.between(now, cooldown.cooldownEndTime)
        
        return when {
            duration.toHours() > 0 -> {
                "${duration.toHours()}h ${duration.toMinutes() % 60}m"
            }
            duration.toMinutes() > 0 -> {
                "${duration.toMinutes()}m ${duration.seconds % 60}s"
            }
            else -> {
                "${duration.seconds}s"
            }
        }
    }
}
