package com.lloir.ornaassistant

import android.content.Context
import com.lloir.ornaassistant.db.WayvesselSessionDatabaseHelper
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class WayvesselSession {

    companion object {
        private const val TAG = "WayvesselSession"
    }

    // Core properties
    val name: String
    private val context: Context?

    // Use var for start time to allow database reconstruction
    var mStarted: LocalDateTime
        private set // Only allow internal modification

    // Use var for mutable properties that accumulate during session
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

    var mID: Long = 0 // Set during database insertion
    var mDurationSeconds: Long = 0 // var because calculated at end
    var mDungeonsVisited = 0 // var because increments during session

    // Primary constructor for new sessions
    constructor(name: String, context: Context?) {
        require(name.isNotBlank()) { "Session name cannot be blank" }
        require(name.length <= 100) { "Session name too long: ${name.length}" }

        this.name = name
        this.context = context
        this.mStarted = LocalDateTime.now()

        // Don't automatically insert into database here to avoid circular dependency
        // The database insertion will be handled by the calling code
    }

    // Method to save session to database
    fun saveToDatabase(): Long {
        return if (context != null) {
            try {
                val db = WayvesselSessionDatabaseHelper(context)
                mID = db.insertData(this)
                db.close()
                android.util.Log.d(TAG, "Created new wayvessel session with ID: $mID")
                mID
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to create session in database", e)
                -1L
            }
        } else {
            -1L
        }
    }

    // Secondary constructor for database reconstruction
    constructor(
        name: String,
        id: Long,
        startTime: LocalDateTime,
        duration: Long = 0,
        orns: Long = 0,
        gold: Long = 0,
        experience: Long = 0,
        dungeonsVisited: Int = 0
    ) {
        require(name.isNotBlank()) { "Session name cannot be blank" }
        require(name.length <= 100) { "Session name too long: ${name.length}" }

        this.name = name
        this.context = null // No context for database-loaded sessions
        this.mID = id
        this.mStarted = startTime
        this.mDurationSeconds = duration
        this.orns = orns
        this.gold = gold
        this.experience = experience
        this.mDungeonsVisited = dungeonsVisited
    }

    // Constructor for loading existing session from database (backward compatibility)
    constructor(name: String, id: Long) {
        require(name.isNotBlank()) { "Session name cannot be blank" }
        require(name.length <= 100) { "Session name too long: ${name.length}" }

        this.name = name
        this.context = null
        this.mID = id
        this.mStarted = LocalDateTime.now() // Default for backward compatibility
    }

    fun finish() {
        try {
            mDurationSeconds = ChronoUnit.SECONDS.between(mStarted, LocalDateTime.now())
            if (context != null) {
                val db = WayvesselSessionDatabaseHelper(context)
                db.updateData(mID.toString(), this)
                db.close()
                android.util.Log.d(TAG, "Finished and updated session: $this")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error finishing session", e)
        }
    }

    override fun toString(): String {
        return "Wayvessel session with ID $mID started at $mStarted in $name, " +
                "duration $mDurationSeconds s, dungeons $mDungeonsVisited, " +
                "gold $gold, experience $experience, orns $orns"
    }
}