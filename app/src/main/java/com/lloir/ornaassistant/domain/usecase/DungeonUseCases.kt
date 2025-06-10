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
