package com.lloir.ornaassistant.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime

// Domain Models (Business Logic Layer)

/**
 * Represents the current state of a dungeon in the game.
 * This class tracks the player's progress through a dungeon, including
 * which floor they're on, whether they've completed it, and any rewards received.
 */
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

/**
 * Represents rewards received from completing a specific floor in a dungeon.
 * Tracks the currency and experience gained from that floor.
 */
@Parcelize
data class FloorReward(
    val floor: Int,
    val orns: Long = 0,
    val gold: Long = 0,
    val experience: Long = 0
) : Parcelable

/**
 * Represents a player's visit to a dungeon in the game.
 * 
 * This class tracks all aspects of a dungeon run, including:
 * - Basic information (name, mode, duration)
 * - Progress (current floor, completion status)
 * - Rewards (orns, gold, experience) from different sources
 * - Special drops (godforges)
 * - Detailed floor-by-floor rewards
 * 
 * It also provides methods to calculate cooldown times for dungeons,
 * which is important for wayvessel session tracking.
 */
@Parcelize
data class DungeonVisit(
    val id: Long = 0,                      // Database ID
    val sessionId: Long? = null,           // Optional wayvessel session ID
    val name: String,                      // Dungeon name
    val mode: DungeonMode,                 // Normal/Hard/Boss/Endless
    val startTime: LocalDateTime,          // When the dungeon was entered
    val durationSeconds: Long = 0,         // How long the run took
    val battleOrns: Long = 0,              // From individual battles
    val battleGold: Long = 0,              // From individual battles  
    val battleExperience: Long = 0,        // From individual battles
    val floorOrns: Long = 0,               // From floor completions
    val floorGold: Long = 0,               // From floor completions
    val floorExperience: Long = 0,         // From floor completions
    val orns: Long = 0,                    // Total of all sources
    val gold: Long = 0,                    // Total of all sources
    val experience: Long = 0,              // Total of all sources
    val floor: Long = 0,                   // Current/highest floor reached
    val godforges: Long = 0,               // Count of godforge drops
    val completed: Boolean = false,        // Whether dungeon was completed
    val floorRewards: List<FloorReward> = emptyList() // Track rewards per floor
) : Parcelable {

    /**
     * Calculates the cooldown period in hours for this dungeon.
     * 
     * Cooldown periods vary based on dungeon type and difficulty:
     * - Normal dungeons: 6 hours (11 hours for hard mode)
     * - Boss dungeons: 11 hours (22 hours for hard mode)
     * - Endless dungeons: 22 hours
     * 
     * @return The cooldown period in hours, or 0 if not applicable
     */
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

    /**
     * Calculates when this dungeon will be available again after cooldown.
     * 
     * @return The LocalDateTime when the dungeon will be available again
     */
    fun cooldownEndTime(): LocalDateTime {
        return startTime.plusHours(cooldownHours())
    }

    /**
     * Checks if this dungeon is currently on cooldown.
     * 
     * @return true if the dungeon is still on cooldown, false otherwise
     */
    fun isOnCooldown(): Boolean {
        return LocalDateTime.now().isBefore(cooldownEndTime())
    }

    override fun toString(): String {
        return "DungeonVisit(name='$name', mode=$mode, floor=$floor, orns=$orns, gold=$gold, exp=$experience, completed=$completed)"
    }
}

/**
 * Represents the mode and difficulty of a dungeon.
 * 
 * Dungeons in Orna can be of different types (normal, boss, endless)
 * and difficulties (normal or hard). This affects rewards, cooldowns,
 * and gameplay experience.
 */
@Parcelize
data class DungeonMode(
    val type: Type = Type.NORMAL,  // The type of dungeon
    val isHard: Boolean = false    // Whether it's in hard mode
) : Parcelable {

    /**
     * Enum representing the different types of dungeons available in the game.
     */
    enum class Type {
        NORMAL,  // Standard multi-floor dungeon
        BOSS,    // Single boss encounter
        ENDLESS  // Endless mode with increasing difficulty
    }

    /**
     * Returns a string representation of the dungeon mode.
     * 
     * @return String in format "HARD TYPE" or just "TYPE"
     */
    override fun toString(): String {
        return if (isHard) "HARD $type" else type.toString()
    }
}

/**
 * Represents a member of a kingdom in the game.
 * 
 * This class tracks information about kingdom members, particularly
 * in the context of wayvessel sessions and party invites. It includes
 * data about their character, session timing, and dungeon floors.
 */
@Parcelize
data class KingdomMember(
    val characterName: String,                      // In-game character name
    val discordName: String = "",                   // Associated Discord username
    val immunity: Boolean = false,                  // Whether they have immunity status
    val endTime: LocalDateTime,                     // When their session ends
    val endTimeLeftSeconds: Long = 0,               // Cached time remaining
    val seenCount: Int = 0,                         // How many times they've been seen
    val timezone: Int = 1000,                       // Player's timezone offset
    val floors: Map<String, GauntletFloor> = emptyMap() // Their current dungeon floors
) : Parcelable {

    /**
     * Calculates the number of active (uncleared) floors this member has.
     * 
     * @return Count of floors that haven't been won or lost yet
     */
    val numFloors: Int
        get() = floors.values.count { !it.loss && !it.win }

    /**
     * Checks if the member has any berserk floors available.
     * Berserk floors provide better rewards but are more difficult.
     * 
     * @return true if they have at least one berserk floor, false otherwise
     */
    val hasBerserkFloor: Boolean
        get() = floors.values.any {
            !it.loss && !it.win && it.mobName.lowercase().contains("(berserk)")
        }

    /**
     * Calculates the time remaining in seconds for this member's session.
     * 
     * @return Seconds remaining in the session, or 0 if the session has ended
     */
    fun timeLeftSeconds(): Long {
        val now = LocalDateTime.now()
        return if (endTime.isAfter(now)) {
            java.time.Duration.between(now, endTime).seconds
        } else {
            0L
        }
    }
}

/**
 * Represents a single floor in a gauntlet/dungeon.
 * 
 * This class tracks information about a specific floor in a dungeon,
 * including its number, the monster encountered, and whether the
 * player has won or lost on this floor.
 */
@Parcelize
data class GauntletFloor(
    val number: Int,                // Floor number
    val mobName: String,            // Name of the monster on this floor
    val loss: Boolean = false,      // Whether the player has lost on this floor
    val win: Boolean = false        // Whether the player has won on this floor
) : Parcelable

/**
 * Represents an assessment of an in-game item.
 * 
 * This class stores information about an item that has been assessed,
 * including its name, level, attributes, and the assessment result.
 * It's used to track item quality and stats for inventory management.
 */
@Parcelize
data class ItemAssessment(
    val id: Long = 0,                          // Database ID
    val itemName: String,                      // Name of the item
    val level: Int,                            // Item level
    val attributes: Map<String, Int>,          // Item attributes (e.g., attack, defense)
    val assessmentResult: AssessmentResult,    // Detailed assessment result
    val timestamp: LocalDateTime,              // When the assessment was performed
    val quality: Double = 0.0                  // Overall quality score
) : Parcelable

/**
 * Represents the detailed result of an item assessment.
 * 
 * This class contains the quality score, stat comparisons, and material
 * information for an assessed item. It's used to determine how good an
 * item is compared to its potential maximum stats.
 */
@Parcelize
data class AssessmentResult(
    val quality: Double,                      // Quality score (higher is better)
    val stats: Map<String, List<String>>,     // Stat name to [10★, MF, DF, GF] values
    val materials: List<Int>                  // [135, MF mats, DF mats, 0]
) : Parcelable

// Screen parsing models

/**
 * Represents a single text element extracted from the game screen.
 * 
 * This class stores information about text content found on the screen,
 * including its position, when it was detected, and its depth in the
 * accessibility tree.
 */
data class ScreenData(
    val text: String,                    // The text content
    val bounds: android.graphics.Rect,   // Screen position of the text
    val timestamp: Long,                 // When the text was detected
    val depth: Int                       // Depth in accessibility tree
)

/**
 * Represents a complete parsed game screen.
 * 
 * This class combines the detected screen type with all the text elements
 * found on that screen. It's the main data structure used for analyzing
 * the current game state.
 */
data class ParsedScreen(
    val screenType: ScreenType,          // The type of screen detected
    val data: List<ScreenData>,          // All text elements on the screen
    val timestamp: LocalDateTime         // When the screen was parsed
)

/**
 * Enum representing the different types of screens in the game.
 * 
 * Each screen type corresponds to a different context in the game,
 * and different parsing rules may apply to each type.
 */
enum class ScreenType {
    INVENTORY,      // Player's inventory screen
    ITEM_DETAIL,    // Detailed view of a specific item
    NOTIFICATIONS,  // Game notifications screen
    DUNGEON_ENTRY,  // Dungeon selection/entry screen
    BATTLE,         // Combat screen
    UNKNOWN         // Screen type couldn't be determined
}

// Quest models

/**
 * Represents a quest in the game.
 * 
 * This class stores information about a quest, including its name,
 * description, objectives, rewards, and completion status. It's used
 * to track player progress through quests and provide guidance.
 */
@Parcelize
data class Quest(
    val id: Long = 0,                          // Database ID
    val name: String,                          // Quest name
    val description: String,                   // Quest description
    val objectives: List<QuestObjective>,      // List of objectives to complete
    val rewards: QuestRewards,                 // Rewards for completing the quest
    val isCompleted: Boolean = false,          // Whether the quest is completed
    val isTracked: Boolean = false,            // Whether the quest is being tracked
    val questType: QuestType,                  // Type of quest (main, side, daily, etc.)
    val requiredLevel: Int = 1,                // Minimum player level required
    val unlockRequirements: String = "",       // Additional requirements to unlock
    val startTime: LocalDateTime? = null,      // When the quest was started
    val completionTime: LocalDateTime? = null, // When the quest was completed
    val expiryTime: LocalDateTime? = null,     // When the quest expires (for timed quests)
    val locationHint: String = "",             // Hint about where to go for the quest
    val questGiver: String = ""                // NPC who gave the quest
) : Parcelable {
    /**
     * Calculates the overall progress percentage for this quest.
     * 
     * @return Progress as a percentage (0-100)
     */
    fun progressPercentage(): Int {
        if (objectives.isEmpty()) return if (isCompleted) 100 else 0

        val completedObjectives = objectives.count { it.isCompleted }
        return (completedObjectives * 100) / objectives.size
    }

    /**
     * Checks if the quest is currently active (started but not completed).
     * 
     * @return true if the quest is active, false otherwise
     */
    fun isActive(): Boolean {
        return startTime != null && !isCompleted
    }

    /**
     * Checks if the quest has expired.
     * 
     * @return true if the quest has expired, false otherwise
     */
    fun isExpired(): Boolean {
        return expiryTime != null && LocalDateTime.now().isAfter(expiryTime)
    }

    /**
     * Gets the next incomplete objective for this quest.
     * 
     * @return The next objective to complete, or null if all are completed
     */
    fun nextObjective(): QuestObjective? {
        return objectives.firstOrNull { !it.isCompleted }
    }
}

/**
 * Represents a single objective within a quest.
 * 
 * This class tracks a specific task that needs to be completed
 * as part of a quest, such as defeating monsters, collecting items,
 * or visiting locations.
 */
@Parcelize
data class QuestObjective(
    val id: Long = 0,                      // Database ID
    val description: String,               // Description of the objective
    val type: ObjectiveType,               // Type of objective
    val targetAmount: Int = 1,             // How many items/monsters/etc. needed
    val currentAmount: Int = 0,            // Current progress
    val isCompleted: Boolean = false,      // Whether this objective is completed
    val targetName: String = "",           // Name of the target (monster, item, etc.)
    val locationHint: String = ""          // Hint about where to complete this objective
) : Parcelable {
    /**
     * Calculates the progress percentage for this objective.
     * 
     * @return Progress as a percentage (0-100)
     */
    fun progressPercentage(): Int {
        return if (targetAmount <= 0) {
            if (isCompleted) 100 else 0
        } else {
            (currentAmount * 100) / targetAmount
        }
    }
}

/**
 * Enum representing the different types of quest objectives.
 */
enum class ObjectiveType {
    KILL_MONSTERS,    // Defeat specific monsters
    COLLECT_ITEMS,    // Collect specific items
    VISIT_LOCATION,   // Visit a specific location
    COMPLETE_DUNGEON, // Complete a dungeon
    DEFEAT_BOSS,      // Defeat a specific boss
    REACH_LEVEL,      // Reach a specific player level
    CRAFT_ITEM,       // Craft a specific item
    TALK_TO_NPC,      // Talk to a specific NPC
    USE_SKILL,        // Use a specific skill
    OTHER             // Other objective type
}

/**
 * Represents the rewards for completing a quest.
 * 
 * This class tracks all the different types of rewards that
 * can be earned from completing a quest, including currency,
 * experience, items, and special rewards.
 */
@Parcelize
data class QuestRewards(
    val orns: Long = 0,                    // Orns (premium currency)
    val gold: Long = 0,                    // Gold (basic currency)
    val experience: Long = 0,              // Experience points
    val items: List<String> = emptyList(), // Reward items
    val skills: List<String> = emptyList(), // Reward skills
    val specialReward: String = ""         // Any special reward
) : Parcelable

/**
 * Enum representing the different types of quests.
 */
enum class QuestType {
    MAIN,       // Main storyline quest
    SIDE,       // Optional side quest
    DAILY,      // Daily repeatable quest
    WEEKLY,     // Weekly repeatable quest
    EVENT,      // Limited-time event quest
    KINGDOM,    // Kingdom-related quest
    ACHIEVEMENT // Achievement-based quest
}

// Settings models

/**
 * Represents user-configurable application settings.
 * 
 * This class stores all user preferences for the application,
 * including overlay visibility, notification settings, and
 * debug options.
 */
data class AppSettings(
    val showSessionOverlay: Boolean = true,    // Show dungeon session overlay
    val showAssessOverlay: Boolean = true,     // Show item assessment overlay
    val showQuestOverlay: Boolean = true,      // Show quest tracker overlay
    val notificationSounds: Boolean = true,    // Play notification sounds
    val overlayTransparency: Float = 0.8f,     // Overlay transparency (0-1)
    val autoHideOverlays: Boolean = false,     // Auto-hide overlays when not needed
    val debugMode: Boolean = false,            // Enable debug logging and features
    val useMlKit: Boolean = false,             // Use ML Kit for screen reading (experimental)
    val enableQuestFeature: Boolean = false,   // Enable quest tracking feature
    val enableDungeonTracker: Boolean = true,  // Enable dungeon tracking feature
    val enableMaterialTracking: Boolean = false // Enable material tracking feature
)
