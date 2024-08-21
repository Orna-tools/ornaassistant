package com.lloir.ornaassistant.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.lloir.ornaassistant.DungeonMode
import com.lloir.ornaassistant.DungeonVisit
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class DungeonVisitDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_NAME (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "started INTEGER," +
                    "duration INTEGER," +
                    "session INTEGER," +
                    "name TEXT," +
                    "hard INTEGER," +
                    "type TEXT," +
                    "orns INTEGER," +
                    "gold INTEGER," +
                    "experience INTEGER," +
                    "floor INTEGER," +
                    "godforges INTEGER," +
                    "completed INTEGER" +
                    ")"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 2 && newVersion == 3) {
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COL_13 INTEGER DEFAULT 0")
        }
    }

    fun insertData(entry: DungeonVisit) {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COL_2, entry.mStarted.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000)
        contentValues.put(COL_3, entry.mDurationSeconds)
        contentValues.put(COL_4, if (entry.sessionID != null) entry.sessionID else -1)
        contentValues.put(COL_5, entry.name)
        contentValues.put(COL_6, entry.mode.mbHard)
        contentValues.put(COL_7, entry.mode.mMode.toString())
        contentValues.put(COL_8, entry.orns)
        contentValues.put(COL_9, entry.gold)
        contentValues.put(COL_10, entry.experience)
        contentValues.put(COL_11, entry.floor)
        contentValues.put(COL_12, entry.godforges)
        contentValues.put(COL_13, if (entry.completed) 1 else 0)
        db.insert(TABLE_NAME, null, contentValues)
    }

    fun updateData(id: String, entry: DungeonVisit): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COL_1, id)
        contentValues.put(COL_2, entry.mStarted.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000)
        contentValues.put(COL_3, entry.mDurationSeconds)
        contentValues.put(COL_4, if (entry.sessionID != null) entry.sessionID else -1)
        contentValues.put(COL_5, entry.name)
        contentValues.put(COL_6, entry.mode.mbHard)
        contentValues.put(COL_7, entry.mode.mMode.toString())
        contentValues.put(COL_8, entry.orns)
        contentValues.put(COL_9, entry.gold)
        contentValues.put(COL_10, entry.experience)
        contentValues.put(COL_11, entry.floor)
        contentValues.put(COL_12, entry.godforges)
        contentValues.put(COL_13, if (entry.completed) 1 else 0)
        db.update(TABLE_NAME, contentValues, "ID = ?", arrayOf(id))
        return true
    }

    fun deleteData(id: String): Int {
        val db = this.writableDatabase
        return db.delete(TABLE_NAME, "ID = ?", arrayOf(id))
    }

    fun deleteAllData() {
        val db = this.writableDatabase
        return db.execSQL("delete from $TABLE_NAME")}

    val allData: Cursor
        get() {
            val db = this.writableDatabase
            val res = db.rawQuery("SELECT * FROM $TABLE_NAME", null)
            return res
        }

    private fun toEntries(cur: Cursor): ArrayList<DungeonVisit> {
        val list = ArrayList<DungeonVisit>()
        while (cur.moveToNext()) {
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

            val mode = DungeonMode(DungeonMode.Modes.valueOf(type))
            mode.mbHard = hard
            val visit = DungeonVisit(sessionId, name, mode)
            visit.mStarted = LocalDateTime.ofInstant(Instant.ofEpochSecond(started), ZoneId.systemDefault())
            visit.mDurationSeconds = duration
            visit.orns = orns
            visit.gold = gold
            visit.experience = experience
            visit.floor = floor
            visit.godforges = godforges
            visit.completed = completed

            list.add(visit)
        }

        cur.close()

        return list
    }

    fun getEntriesBetween(start: LocalDateTime, end: LocalDateTime): ArrayList<DungeonVisit> {
        val startUnix = start.toEpochSecond(ZoneOffset.UTC)
        val endUnix = end.toEpochSecond(ZoneOffset.UTC)
        val db = this.writableDatabase
        return toEntries(
            db.rawQuery(
                "SELECT * FROM $TABLE_NAME " +
                        "WHERE started > $startUnix " +
                        "AND started < $endUnix ",
                null
            )
        )
    }

    fun getVisitsForSession(session: Long):ArrayList<DungeonVisit> {
        val db = this.writableDatabase
        return toEntries(
            db.rawQuery(
                "SELECT * FROM $TABLE_NAME " +
                        "WHERE session='$session' ",
                null
            )
        )
    }

    companion object {
        val DATABASE_NAME = "dungeonVisits.db"
        val TABLE_NAME = "dungeon"
        val COL_1 = "ID"
        val COL_2 = "started"
        val COL_3 = "duration"
        val COL_4 = "session"
        val COL_5 = "name"
        val COL_6 = "hard"
        val COL_7 = "type"
        val COL_8 = "orns"
        val COL_9 = "gold"
        val COL_10 = "experience"
        val COL_11 = "floor"
        val COL_12 = "godforges"
        val COL_13 = "completed"
        val VERSION = 3
    }
}