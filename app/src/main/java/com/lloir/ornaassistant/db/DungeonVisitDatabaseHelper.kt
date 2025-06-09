package com.lloir.ornaassistant.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.lloir.ornaassistant.DungeonMode
import com.lloir.ornaassistant.DungeonVisit
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class DungeonVisitDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, VERSION) {

    companion object {
        private const val TAG = "DungeonVisitDB"
        const val DATABASE_NAME = "dungeonVisits.db"
        const val TABLE_NAME = "dungeon"
        const val COL_1 = "ID"
        const val COL_2 = "started"
        const val COL_3 = "duration"
        const val COL_4 = "session"
        const val COL_5 = "name"
        const val COL_6 = "hard"
        const val COL_7 = "type"
        const val COL_8 = "orns"
        const val COL_9 = "gold"
        const val COL_10 = "experience"
        const val COL_11 = "floor"
        const val COL_12 = "godforges"
        const val COL_13 = "completed"
        const val VERSION = 3
    }

    override fun onCreate(db: SQLiteDatabase) {
        try {
            db.execSQL(
                "CREATE TABLE $TABLE_NAME (" +
                        "$COL_1 INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "$COL_2 INTEGER," +
                        "$COL_3 INTEGER," +
                        "$COL_4 INTEGER," +
                        "$COL_5 TEXT," +
                        "$COL_6 INTEGER," +
                        "$COL_7 TEXT," +
                        "$COL_8 INTEGER," +
                        "$COL_9 INTEGER," +
                        "$COL_10 INTEGER," +
                        "$COL_11 INTEGER," +
                        "$COL_12 INTEGER," +
                        "$COL_13 INTEGER" +
                        ")"
            )
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error creating database", e)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            if (oldVersion == 2 && newVersion == 3) {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COL_13 INTEGER DEFAULT 0")
            }
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error upgrading database", e)
        }
    }

    fun insertData(entry: DungeonVisit): Boolean {
        return try {
            val db = this.writableDatabase
            val contentValues = ContentValues().apply {
                put(COL_2, entry.mStarted.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000)
                put(COL_3, entry.mDurationSeconds)
                put(COL_4, entry.sessionID ?: -1)
                put(COL_5, entry.name)
                put(COL_6, if (entry.mode.mbHard) 1 else 0)
                put(COL_7, entry.mode.mMode.toString())
                put(COL_8, entry.orns)
                put(COL_9, entry.gold)
                put(COL_10, entry.experience)
                put(COL_11, entry.floor)
                put(COL_12, entry.godforges)
                put(COL_13, if (entry.completed) 1 else 0)
            }

            val result = db.insert(TABLE_NAME, null, contentValues)
            result != -1L
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error inserting dungeon visit", e)
            false
        }
    }

    fun updateData(id: String, entry: DungeonVisit): Boolean {
        return try {
            val db = this.writableDatabase
            val contentValues = ContentValues().apply {
                put(COL_2, entry.mStarted.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000)
                put(COL_3, entry.mDurationSeconds)
                put(COL_4, entry.sessionID ?: -1)
                put(COL_5, entry.name)
                put(COL_6, if (entry.mode.mbHard) 1 else 0)
                put(COL_7, entry.mode.mMode.toString())
                put(COL_8, entry.orns)
                put(COL_9, entry.gold)
                put(COL_10, entry.experience)
                put(COL_11, entry.floor)
                put(COL_12, entry.godforges)
                put(COL_13, if (entry.completed) 1 else 0)
            }

            val rowsUpdated = db.update(TABLE_NAME, contentValues, "$COL_1 = ?", arrayOf(id))
            rowsUpdated > 0
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error updating dungeon visit", e)
            false
        }
    }

    fun deleteData(id: String): Int {
        return try {
            val db = this.writableDatabase
            db.delete(TABLE_NAME, "$COL_1 = ?", arrayOf(id))
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error deleting dungeon visit", e)
            0
        }
    }

    fun deleteAllData(): Boolean {
        return try {
            val db = this.writableDatabase
            db.delete(TABLE_NAME, null, null)
            true
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error deleting all data", e)
            false
        }
    }

    val allData: ArrayList<DungeonVisit>
        get() {
            return try {
                val db = this.readableDatabase
                val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)
                toEntries(cursor)
            } catch (e: SQLiteException) {
                Log.e(TAG, "Error getting all data", e)
                arrayListOf()
            }
        }

    private fun toEntries(cursor: Cursor): ArrayList<DungeonVisit> {
        val list = ArrayList<DungeonVisit>()
        try {
            cursor.use { cur ->
                while (cur.moveToNext()) {
                    try {
                        var col = 1
                        val started = cur.getLong(col++)
                        val duration = cur.getLong(col++)
                        val sessionId = cur.getLong(col++)
                        val name = cur.getString(col++)
                        val hard = cur.getInt(col++) == 1
                        val type = cur.getString(col++)
                        val orns = cur.getLong(col++)
                        val gold = cur.getLong(col++)
                        val experience = cur.getLong(col++)
                        val floor = cur.getLong(col++)
                        val godforges = cur.getLong(col++)
                        val completed = cur.getInt(col++) == 1

                        // Create DungeonMode
                        val mode = DungeonMode(DungeonMode.Modes.valueOf(type)).apply {
                            mbHard = hard
                        }

                        // Create start time from database
                        val startTime = LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(started),
                            ZoneId.systemDefault()
                        )

                        // Create DungeonVisit with reconstructed data
                        val visit = createDungeonVisitFromDatabase(
                            sessionId = if (sessionId == -1L) null else sessionId,
                            name = name,
                            mode = mode,
                            startTime = startTime,
                            duration = duration,
                            orns = orns,
                            gold = gold,
                            experience = experience,
                            floor = floor,
                            godforges = godforges,
                            completed = completed
                        )

                        list.add(visit)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing database row", e)
                        // Continue with next row
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting cursor to entries", e)
        }

        return list
    }

    /**
     * Creates a DungeonVisit instance from database data using the secondary constructor.
     */
    private fun createDungeonVisitFromDatabase(
        sessionId: Long?,
        name: String,
        mode: DungeonMode,
        startTime: LocalDateTime,
        duration: Long,
        orns: Long,
        gold: Long,
        experience: Long,
        floor: Long,
        godforges: Long,
        completed: Boolean
    ): DungeonVisit {
        // Use the secondary constructor that accepts all parameters
        return DungeonVisit(
            sessionID = sessionId,
            name = name,
            mode = mode,
            startTime = startTime,
            duration = duration,
            orns = orns,
            gold = gold,
            experience = experience,
            floor = floor,
            godforges = godforges,
            completed = completed
        )
    }

    fun getEntriesBetween(start: LocalDateTime, end: LocalDateTime): ArrayList<DungeonVisit> {
        return try {
            val startUnix = start.toEpochSecond(ZoneOffset.UTC)
            val endUnix = end.toEpochSecond(ZoneOffset.UTC)
            val db = this.readableDatabase

            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_NAME WHERE $COL_2 > ? AND $COL_2 < ?",
                arrayOf(startUnix.toString(), endUnix.toString())
            )

            toEntries(cursor)
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error getting entries between dates", e)
            arrayListOf()
        }
    }

    fun getVisitsForSession(session: Long): ArrayList<DungeonVisit> {
        return try {
            val db = this.readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_NAME WHERE $COL_4 = ?",
                arrayOf(session.toString())
            )

            toEntries(cursor)
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error getting visits for session", e)
            arrayListOf()
        }
    }

    fun getVisitById(id: Long): DungeonVisit? {
        return try {
            val db = this.readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_NAME WHERE $COL_1 = ?",
                arrayOf(id.toString())
            )

            val visits = toEntries(cursor)
            visits.firstOrNull()
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error getting visit by ID", e)
            null
        }
    }

    fun getRecentVisits(limit: Int = 50): ArrayList<DungeonVisit> {
        return try {
            val db = this.readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_NAME ORDER BY $COL_2 DESC LIMIT ?",
                arrayOf(limit.toString())
            )

            toEntries(cursor)
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error getting recent visits", e)
            arrayListOf()
        }
    }

    fun getVisitCount(): Int {
        return try {
            val db = this.readableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_NAME", null)
            cursor.use {
                if (it.moveToFirst()) {
                    it.getInt(0)
                } else {
                    0
                }
            }
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error getting visit count", e)
            0
        }
    }
}