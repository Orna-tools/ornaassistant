package com.lloir.ornaassistant.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lloir.ornaassistant.domain.model.ObjectiveType
import com.lloir.ornaassistant.domain.model.QuestObjective
import com.lloir.ornaassistant.domain.model.QuestRewards
import com.lloir.ornaassistant.domain.model.QuestType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Type converters for Room database.
 * 
 * This class provides conversion methods between complex Kotlin types and
 * primitive types that can be stored in SQLite. It handles:
 * - LocalDateTime <-> String conversion
 * - DungeonMode <-> String conversion
 * - Map<String, String> <-> String conversion
 * - List<FloorReward> <-> String conversion
 * 
 * Complex types are serialized to JSON for storage and deserialized when retrieved.
 * This allows storing rich data structures in the database while maintaining
 * compatibility with SQLite's limited type system.
 */
class Converters {
    private val gson = Gson()
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val TAG = "DatabaseConverters"

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(formatter)
    }

    @TypeConverter
    fun toLocalDateTime(dateTimeString: String?): LocalDateTime? {
        return dateTimeString?.let { LocalDateTime.parse(it, formatter) }
    }

    @TypeConverter
    fun fromDungeonMode(mode: DungeonMode): String {
        return gson.toJson(mode)
    }

    @TypeConverter
    fun toDungeonMode(modeJson: String): DungeonMode {
        return gson.fromJson(modeJson, DungeonMode::class.java)
    }

    @TypeConverter
    fun fromStringMap(map: Map<String, String>): String {
        return gson.toJson(map)
    }

    @TypeConverter
    fun toStringMap(mapJson: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(mapJson, type)
    }

    @TypeConverter
    fun fromFloorRewardsList(rewards: List<FloorReward>): String {
        val json = gson.toJson(rewards)
        Log.d(TAG, "Converting floor rewards to JSON: $rewards -> $json")
        return json
    }

    @TypeConverter
    fun toFloorRewardsList(rewardsJson: String): List<FloorReward> {
        return try {
            Log.d(TAG, "Converting JSON to floor rewards: $rewardsJson")
            val type = object : TypeToken<List<FloorReward>>() {}.type
            val rewards = gson.fromJson<List<FloorReward>>(rewardsJson, type) ?: emptyList()
            Log.d(TAG, "Converted to: $rewards")
            rewards
        } catch (e: Exception) {
            Log.e(TAG, "Error converting floor rewards from JSON", e)
            emptyList()
        }
    }

    // Quest-related converters

    @TypeConverter
    fun fromQuestType(questType: QuestType): String {
        return questType.name
    }

    @TypeConverter
    fun toQuestType(questTypeName: String): QuestType {
        return try {
            QuestType.valueOf(questTypeName)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting quest type from string: $questTypeName", e)
            QuestType.SIDE // Default to SIDE if conversion fails
        }
    }

    @TypeConverter
    fun fromObjectiveType(objectiveType: ObjectiveType): String {
        return objectiveType.name
    }

    @TypeConverter
    fun toObjectiveType(objectiveTypeName: String): ObjectiveType {
        return try {
            ObjectiveType.valueOf(objectiveTypeName)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting objective type from string: $objectiveTypeName", e)
            ObjectiveType.OTHER // Default to OTHER if conversion fails
        }
    }

    @TypeConverter
    fun fromQuestRewards(rewards: QuestRewards): String {
        return gson.toJson(rewards)
    }

    @TypeConverter
    fun toQuestRewards(rewardsJson: String): QuestRewards {
        return try {
            gson.fromJson(rewardsJson, QuestRewards::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting quest rewards from JSON", e)
            QuestRewards() // Return empty rewards if conversion fails
        }
    }

    @TypeConverter
    fun fromQuestObjectivesList(objectives: List<QuestObjective>): String {
        return gson.toJson(objectives)
    }

    @TypeConverter
    fun toQuestObjectivesList(objectivesJson: String): List<QuestObjective> {
        return try {
            val type = object : TypeToken<List<QuestObjective>>() {}.type
            gson.fromJson(objectivesJson, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error converting quest objectives from JSON", e)
            emptyList()
        }
    }
}

// Domain Models
data class DungeonMode(
    val type: Type = Type.NORMAL,
    val isHard: Boolean = false
) {
    enum class Type {
        NORMAL, BOSS, ENDLESS
    }

    override fun toString(): String {
        return if (isHard) "HARD $type" else type.toString()
    }
}

data class FloorReward(
    val floor: Int,
    val orns: Long = 0,
    val gold: Long = 0,
    val experience: Long = 0
)

// Database Entities
/**
 * Entity representing a dungeon visit in the database.
 * 
 * This entity stores all information about a player's visit to a dungeon, including:
 * - Basic information (name, mode, duration)
 * - Rewards from battles and floor completions
 * - Total rewards (orns, gold, experience)
 * - Floor progression and completion status
 * - Detailed floor-by-floor reward tracking
 * 
 * It also provides methods for calculating cooldown times for dungeons,
 * which is important for wayvessel session tracking.
 */
@Entity(tableName = "dungeon_visits")
@TypeConverters(Converters::class)
data class DungeonVisitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long? = null,
    val name: String,
    val mode: DungeonMode,
    val startTime: LocalDateTime,
    val durationSeconds: Long = 0,
    val battleOrns: Long = 0,
    val battleGold: Long = 0,
    val battleExperience: Long = 0,
    val floorOrns: Long = 0,
    val floorGold: Long = 0,
    val floorExperience: Long = 0,
    val orns: Long = 0,
    val gold: Long = 0,
    val experience: Long = 0,
    val floor: Long = 0,
    val godforges: Long = 0,
    val completed: Boolean = false,
    val floorRewards: List<FloorReward> = emptyList()
) {
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
}

/**
 * Entity representing a kingdom member in the database.
 * 
 * This entity stores information about a player's kingdom members, including:
 * - Character and Discord identification
 * - Wayvessel session timing information
 * - Floor information for party invites
 * - Tracking data for member activity
 * 
 * It's used for features like wayvessel session tracking and
 * party invite management, helping players coordinate dungeon runs.
 */
@Entity(tableName = "kingdom_members")
@TypeConverters(Converters::class)
data class KingdomMemberEntity(
    @PrimaryKey
    val characterName: String,
    val discordName: String = "",
    val immunity: Boolean = false,
    val endTime: LocalDateTime,
    val endTimeLeftSeconds: Long = 0,
    val seenCount: Int = 0,
    val timezone: Int = 1000,
    val floors: Map<String, String> = emptyMap() // Simplified floor storage
)

/**
 * Entity representing an item assessment in the database.
 * 
 * This entity stores information about assessed items, including:
 * - Basic item information (name, level)
 * - Item attributes and stats
 * - Assessment results and quality score
 * - Timestamp for tracking assessment history
 * 
 * It's used to store the history of item assessments performed by the
 * application, allowing players to review past assessments and compare
 * item quality over time.
 */
@Entity(tableName = "item_assessments")
@TypeConverters(Converters::class)
data class ItemAssessmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemName: String,
    val level: Int,
    val attributes: Map<String, String>,
    val assessmentResult: String, // JSON string of assessment
    val timestamp: LocalDateTime,
    val quality: Double = 0.0
)

/**
 * Entity representing a quest in the database.
 * 
 * This entity stores all information about a quest, including:
 * - Basic information (name, description, type)
 * - Objectives and rewards
 * - Progress tracking (completion status, timestamps)
 * - Additional metadata (level requirements, location hints)
 * 
 * It's used to persist quest data across app sessions and track
 * the player's progress through various quests in the game.
 */
@Entity(tableName = "quests")
@TypeConverters(Converters::class)
data class QuestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val objectives: List<QuestObjective>,
    val rewards: QuestRewards,
    val isCompleted: Boolean = false,
    val isTracked: Boolean = false,
    val questType: QuestType,
    val requiredLevel: Int = 1,
    val unlockRequirements: String = "",
    val startTime: LocalDateTime? = null,
    val completionTime: LocalDateTime? = null,
    val expiryTime: LocalDateTime? = null,
    val locationHint: String = "",
    val questGiver: String = ""
)
