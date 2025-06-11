package com.lloir.ornaassistant.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime

// Domain Models (Business Logic Layer)

// Dungeon tracking state
data class DungeonState(
    val dungeonName: String = "",
    val hasEntered: Boolean = false,
    val isEnteringNewDungeon: Boolean = true,
    val isDone: Boolean = false,
    val mode: DungeonMode = DungeonMode(),
    val floorNumber: Int = 1,
    val victoryScreenHandledForFloor: Boolean = false,
    val currentVisit: DungeonVisit? = null,
    val onHoldVisits: Map<String, DungeonVisit> = emptyMap()
) {
    fun finish(): DungeonVisit? {
        return currentVisit?.copy(durationSeconds = java.time.temporal.ChronoUnit.SECONDS.between(currentVisit.startTime, LocalDateTime.now()))
    }
}

@Parcelize
data class DungeonVisit(
    val id: Long = 0,
    val sessionId: Long? = null,
    val name: String,
    val mode: DungeonMode,
    val startTime: LocalDateTime,
    val durationSeconds: Long = 0,
    val orns: Long = 0,
    val gold: Long = 0,
    val experience: Long = 0,
    val floor: Long = 0,
    val godforges: Long = 0,
    val completed: Boolean = false
) : Parcelable {

    fun cooldownHours(): Long {
        val dungeonName = name.split(' ')
        if (dungeonName.size > 1 && dungeonName.last() == "Dungeon") {
            return when (mode.type) {
                DungeonMode.Type.NORMAL -> if (mode.isHard) 11 else 6
                DungeonMode.Type.BOSS -> if (mode.isHard) 22 else 11
                DungeonMode.Type.ENDLESS -> 22
            }
        }
        return 0
    }

    fun cooldownEndTime(): LocalDateTime {
        return startTime.plusHours(cooldownHours())
    }

    fun isOnCooldown(): Boolean {
        return LocalDateTime.now().isBefore(cooldownEndTime())
    }
    
    override fun toString(): String {
        return "DungeonVisit(name='$name', mode=$mode, floor=$floor, orns=$orns, gold=$gold, exp=$experience, completed=$completed)"
    }
}

@Parcelize
data class DungeonMode(
    val type: Type = Type.NORMAL,
    val isHard: Boolean = false
) : Parcelable {

    enum class Type {
        NORMAL, BOSS, ENDLESS
    }

    override fun toString(): String {
        return if (isHard) "HARD $type" else type.toString()
    }
}

@Parcelize
data class WayvesselSession(
    val id: Long = 0,
    val name: String,
    val startTime: LocalDateTime,
    val durationSeconds: Long = 0,
    val orns: Long = 0,
    val gold: Long = 0,
    val experience: Long = 0,
    val dungeonsVisited: Int = 0
) : Parcelable {

    fun isActive(): Boolean {
        return durationSeconds == 0L
    }

    fun endTime(): LocalDateTime {
        return startTime.plusSeconds(durationSeconds)
    }
}

@Parcelize
data class KingdomMember(
    val characterName: String,
    val discordName: String = "",
    val immunity: Boolean = false,
    val endTime: LocalDateTime,
    val endTimeLeftSeconds: Long = 0,
    val seenCount: Int = 0,
    val timezone: Int = 1000,
    val floors: Map<String, GauntletFloor> = emptyMap()
) : Parcelable {

    val numFloors: Int
        get() = floors.values.count { !it.loss && !it.win }

    val hasBerserkFloor: Boolean
        get() = floors.values.any {
            !it.loss && !it.win && it.mobName.lowercase().contains("(berserk)")
        }

    fun timeLeftSeconds(): Long {
        val now = LocalDateTime.now()
        return if (endTime.isAfter(now)) {
            java.time.Duration.between(now, endTime).seconds
        } else {
            0L
        }
    }
}

@Parcelize
data class GauntletFloor(
    val number: Int,
    val mobName: String,
    val loss: Boolean = false,
    val win: Boolean = false
) : Parcelable

@Parcelize
data class ItemAssessment(
    val id: Long = 0,
    val itemName: String,
    val level: Int,
    val attributes: Map<String, Int>,
    val assessmentResult: AssessmentResult,
    val timestamp: LocalDateTime,
    val quality: Double = 0.0
) : Parcelable

@Parcelize
data class AssessmentResult(
    val quality: Double,
    val stats: Map<String, List<String>>, // Stat name to [10★, MF, DF, GF] values
    val materials: List<Int> // [135, MF mats, DF mats, 0]
) : Parcelable

// Screen parsing models
data class ScreenData(
    val text: String,
    val bounds: android.graphics.Rect,
    val timestamp: Long,
    val depth: Int
)

data class ParsedScreen(
    val screenType: ScreenType,
    val data: List<ScreenData>,
    val timestamp: LocalDateTime
)

enum class ScreenType {
    INVENTORY,
    ITEM_DETAIL,
    WAYVESSEL,
    NOTIFICATIONS,
    DUNGEON_ENTRY,
    BATTLE,
    UNKNOWN
}

// Notification models
data class PartyInvite(
    val inviterName: String,
    val bounds: android.graphics.Rect,
    val timestamp: LocalDateTime
)

data class WayvesselNotification(
    val wayvesselName: String,
    val cooldownEndTime: LocalDateTime,
    val isReady: Boolean
)

// Settings models
data class AppSettings(
    // Existing overlay settings
    val showSessionOverlay: Boolean = true,
    val showInvitesOverlay: Boolean = true,
    val showAssessOverlay: Boolean = true,
    
    // New overlay settings
    val showEfficiencyOverlay: Boolean = true,
    val showCombatLogOverlay: Boolean = false,
    val showDungeonCooldownOverlay: Boolean = true,
    val showGoalsOverlay: Boolean = false,
    
    // Feature toggles
    val enableVoiceAnnouncements: Boolean = true,
    val enableAutoScreenshot: Boolean = true,
    val enablePartyAutoAccept: Boolean = false,
    val enableDropAnalytics: Boolean = true,
    
    // Voice announcement settings
    val announceOrnates: Boolean = true,
    val announceGodforges: Boolean = true,
    val announceLevelUp: Boolean = true,
    val announceDungeonComplete: Boolean = false,
    val announceEfficiencyMilestones: Boolean = true,
    
    // Auto screenshot settings
    val screenshotOrnates: Boolean = true,
    val screenshotGodforges: Boolean = true,
    val screenshotQualityThreshold: Float = 1.8f,
    
    // Party settings
    val trustedPlayers: Set<String> = emptySet(),
    
    // Existing settings
    val wayvesselNotifications: Boolean = true,
    val notificationSounds: Boolean = true,
    val overlayTransparency: Float = 0.8f,
    val autoHideOverlays: Boolean = false
