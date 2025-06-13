package com.lloir.ornaassistant.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Type Converters
class Converters {
    private val gson = Gson()
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

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
    val completed: Boolean = false
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