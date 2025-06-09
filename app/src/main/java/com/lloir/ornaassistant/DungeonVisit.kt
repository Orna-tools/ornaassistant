package com.lloir.ornaassistant.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.lloir.ornaassistant.WayvesselSession
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class WayvesselSessionDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, VERSION) {

    companion object {
        private const val TAG = "WayvesselSessionDB"
        const val DATABASE_NAME = "wvSession.db"
        const val TABLE_NAME = "wayvessel_session"
        const val COL_1 = "ID"
        const val COL_2 = "started"
        const val COL_3 = "duration"
        const val COL_4 = "name"
        const val COL_5 = "orns"
        const val COL_6 = "gold"
        const val COL_7 = "experience"
        const val COL_8 = "dungeons_visited"
        const val VERSION = 2
    }

    override fun onCreate(db: SQLiteDatabase) {
        try {
            db.execSQL(
                "CREATE TABLE $TABLE_NAME (" +
                        "$COL_1 INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "$COL_2 INTEGER," +
                        "$COL_3 INTEGER," +
                        "$COL_4 TEXT," +
                        "$COL_5 INTEGER," +
                        "$COL_6 INTEGER," +
                        "$COL_7 INTEGER," +
                        "$COL_8 INTEGER DEFAULT 0" +
                        ")"
            )
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error creating database", e)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            if (oldVersion == 1 && newVersion == 2) {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COL_8 INTEGER DEFAULT 0")
            } else {
                db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
                onCreate(db)
            }
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error upgrading database", e)
        }
    }

    fun insertData(entry: WayvesselSession): Long {
        return try {
            val db = this.writableDatabase
            val started = entry.mStarted.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000

            val contentValues = ContentValues().apply {
                put(COL_2, started)
                put(COL_3, entry.mDurationSeconds)
                put(COL_4, entry.name)
                put(COL_5, entry.orns)
                put(COL_6, entry.gold)
                put(COL_7, entry.experience)
                put(COL_8, entry.mDungeonsVisited)
            }

            val result = db.insert(TABLE_NAME, null, contentValues)
            if (result != -1L) {
                Log.d(TAG, "Inserted wayvessel session: ${entry.name}")
                result
            } else {
                Log.e(TAG, "Failed to insert wayvessel session")
                -1L
            }
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error inserting wayvessel session", e)
            -1L
        }
    }

    fun updateData(id: String, entry: WayvesselSession): Boolean {
        return try {
            val db = this.writableDatabase
            val contentValues = ContentValues().apply {
                put(COL_2, entry.mStarted.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000)
                put(COL_3, entry.mDurationSeconds)
                put(COL_4, entry.name)
                put(COL_5, entry.orns)
                put(COL_6, entry.gold)
                put(COL_7, entry.experience)
                put(COL_8, entry.mDungeonsVisited)
            }

            val rowsUpdated = db.update(TABLE_NAME, contentValues, "$COL_1 = ?", arrayOf(id))
            rowsUpdated > 0
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error updating wayvessel session", e)
            false
        }
    }

    fun deleteData(id: String): Int {
        return try {
            val db = this.writableDatabase
            db.delete(TABLE_NAME, "$COL_1 = ?", arrayOf(id))
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error deleting wayvessel session", e)
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

    val allData: ArrayList<WayvesselSession>
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

    private fun toEntries(cursor: Cursor): ArrayList<WayvesselSession> {
        val list = ArrayList<WayvesselSession>()
        try {
            cursor.use { cur ->
                while (cur.moveToNext()) {
                    try {
                        var col = 0
                        val id = cur.getLong(col++)
                        val started = cur.getLong(col++)
                        val duration = cur.getLong(col++)
                        val name = cur.getString(col++)
                        val orns = cur.getLong(col++)
                        val gold = cur.getLong(col++)
                        val experience = cur.getLong(col++)

                        val dungeonsVisited = if (cur.columnCount > col) {
                            cur.getInt(col)
                        } else {
                            0
                        }

                        val startTime = LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(started),
                            ZoneId.systemDefault()
                        )

                        val session = WayvesselSession(
                            name = name,
                            id = id,
                            startTime = startTime,
                            duration = duration,
                            orns = orns,
                            gold = gold,
                            experience = experience,
                            dungeonsVisited = dungeonsVisited
                        )

                        list.add(session)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing database row", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting cursor to entries", e)
        }

        return list
    }

    fun getEntriesBetween(start: LocalDateTime, end: LocalDateTime): ArrayList<WayvesselSession> {
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

    fun getLastNSessionsFor(name: String, n: Int): ArrayList<WayvesselSession> {
        return try {
            val db = this.readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_NAME WHERE $COL_4 = ? ORDER BY $COL_1 DESC LIMIT ?",
                arrayOf(name, n.toString())
            )

            toEntries(cursor)
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error getting last N sessions for name", e)
            arrayListOf()
        }
    }

    fun getLastNSessions(n: Int): ArrayList<WayvesselSession> {
        return try {
            val db = this.readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_NAME ORDER BY $COL_1 DESC LIMIT ?",
                arrayOf(n.toString())
            )

            toEntries(cursor)
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error getting last N sessions", e)
            arrayListOf()
        }
    }

    fun getSessionById(id: Long): WayvesselSession? {
        return try {
            val db = this.readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_NAME WHERE $COL_1 = ?",
                arrayOf(id.toString())
            )

            val sessions = toEntries(cursor)
            sessions.firstOrNull()
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error getting session by ID", e)
            null
        }
    }

    fun getRecentSessions(limit: Int = 50): ArrayList<WayvesselSession> {
        return try {
            val db = this.readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_NAME ORDER BY $COL_2 DESC LIMIT ?",
                arrayOf(limit.toString())
            )

            toEntries(cursor)
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error getting recent sessions", e)
            arrayListOf()
        }
    }

    fun getSessionCount(): Int {
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
            Log.e(TAG, "Error getting session count", e)
            0
        }
    }
}