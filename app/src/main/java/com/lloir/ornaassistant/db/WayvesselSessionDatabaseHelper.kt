package com.lloir.ornaassistant

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class DungeonVisit {

    companion object {
        private const val TAG = "DungeonVisit"
    }

    // Core immutable properties
    var sessionID: Long? = null
    val name: String
    var mode: DungeonMode

    // Use var for start time to allow database reconstruction
    var mStarted: LocalDateTime
        internal set // Allow access from same module (database helpers)

    // Use var for mutable properties that need to be updated during dungeon run
    var orns: Long = 0
        set(value) {
            field = maxOf(0, value) // Ensure non-negative
        }

    var gold: Long = 0
        set(value) {
            field = maxOf(0, value) // Ensure non-negative
        }

    var experience: Long = 0
        set(value) {
            field = maxOf(0, value) // Ensure non-negative
        }

    var floor: Long = 0
        set(value) {
            field = maxOf(0, value) // Ensure non-negative
        }

    var godforges: Long = 0
        set(value) {
            field = maxOf(0, value) // Ensure non-negative
        }

    var completed = false
    var mDurationSeconds: Long = 0 // var because duration is calculated at end

    // Primary constructor for new dungeon visits
    constructor(sessionID: Long?, name: String, mode: DungeonMode) {
        require(name.isNotBlank()) { "Dungeon name cannot be blank" }
        require(name.length <= 100) { "Dungeon name too long: ${name.length}" }

        this.sessionID = sessionID
        this.name = name
        this.mode = mode
        this.mStarted = LocalDateTime.now()
    }

    // Secondary constructor for database reconstruction
    constructor(
        sessionID: Long?,
        name: String,
        mode: DungeonMode,
        startTime: LocalDateTime,
        duration: Long = 0,
        orns: Long = 0,
        gold: Long = 0,
        experience: Long = 0,
        floor: Long = 0,
        godforges: Long = 0,
        completed: Boolean = false
    ) {
        require(name.isNotBlank()) { "Dungeon name cannot be blank" }
        require(name.length <= 100) { "Dungeon name too long: ${name.length}" }

        this.sessionID = sessionID
        this.name = name
        this.mode = mode
        this.mStarted = startTime
        this.mDurationSeconds = duration
        this.orns = orns
        this.gold = gold
        this.experience = experience
        this.floor = floor
        this.godforges = godforges
        this.completed = completed
    }

    fun finish() {
        try {
            mDurationSeconds = ChronoUnit.SECONDS.between(mStarted, LocalDateTime.now())
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error calculating duration", e)
            mDurationSeconds = 0
        }
    }

    override fun toString(): String {
        return "Dungeon visit $mStarted, sessionID $sessionID, name $name, mode $mode, " +
                "duration $mDurationSeconds s, gold $gold, experience $experience, orns $orns, floor $floor"
    }

    fun coolDownHours(): Long {
        return try {
            val split = name.split(' ')
            if (split.size > 1 && split.last() == "Dungeon") {
                when (mode.mMode) {
                    DungeonMode.Modes.NORMAL -> if (mode.mbHard) 11 else 6
                    DungeonMode.Modes.BOSS -> if (mode.mbHard) 22 else 11
                    DungeonMode.Modes.ENDLESS -> 22 // Always 22 for endless
                }
            } else {
                0
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error calculating cooldown hours", e)
            0
        }
    }

    fun coolDownEnds(): LocalDateTime {
        return try {
            mStarted.plusHours(coolDownHours())
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error calculating cooldown end time", e)
            mStarted
        }
    }
}