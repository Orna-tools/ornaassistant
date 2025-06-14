package com.lloir.ornaassistant.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.os.Build
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Type Converters
class Converters {
    private val gson = Gson()
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val TAG = "DatabaseConverters"

    @TypeConverter
    fun fromLocalDateTime(dateTime: Any?): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (dateTime as? LocalDateTime)?.format(formatter)
        } else {
            // For legacy devices, store as ISO string
            dateTime?.toString()
        }
    }

    @TypeConverter
    fun toLocalDateTime(dateTimeString: String?): Any? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dateTimeString?.let { LocalDateTime.parse(it, formatter) }
        } else {
            // For legacy devices, just return the string
            // You'll need to handle this in your repository layer
            dateTimeString
        }
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

@Entity(tableName = "wayvessel_sessions")
@TypeConverters(Converters::class)
data class WayvesselSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startTime: LocalDateTime,
    val durationSeconds: Long = 0,
    val orns: Long = 0,
    val gold: Long = 0,
    val experience: Long = 0,
    val dungeonsVisited: Int = 0
)

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