package com.lloir.ornaassistant.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.lloir.ornaassistant.WayvesselSession
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class WayvesselSessionDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_NAME (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "started INTEGER," +
                    "duration INTEGER," +
                    "name TEXT," +
                    "orns INTEGER," +
                    "gold INTEGER," +
                    "experience INTEGER" +
                    ")"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertData(entry: WayvesselSession): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        val started = entry.mStarted.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000
        contentValues.put(COL_2, started)
        contentValues.put(COL_3, entry.mDurationSeconds)
        contentValues.put(COL_4, entry.name)
        contentValues.put(COL_5, entry.orns)
        contentValues.put(COL_6, entry.gold)
        contentValues.put(COL_7, entry.experience)

        db.insert(TABLE_NAME, null, contentValues)
        val entries = toEntries(db.rawQuery(
            "SELECT * FROM $TABLE_NAME " +
                    "WHERE started = $started " +
                    "AND duration = ${entry.mDurationSeconds} " +
                    "AND name= '${entry.name}' " +
                    "AND orns = ${entry.orns} " +
                    "AND gold = ${entry.gold} " +
                    "AND experience = ${entry.experience} ",
            null))

        return if (entries.size == 1) {
            entries.first().mID
        } else {
            -1
        }
    }

    fun updateData(id: String, entry: WayvesselSession): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COL_1, id)
        contentValues.put(COL_2, entry.mStarted.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000)
        contentValues.put(COL_3, entry.mDurationSeconds)
        contentValues.put(COL_4, entry.name)
        contentValues.put(COL_5, entry.orns)
        contentValues.put(COL_6, entry.gold)
        contentValues.put(COL_7, entry.experience)
        db.update(TABLE_NAME, contentValues, "ID = ?", arrayOf(id))
        return true
    }

    fun deleteData(id: String): Int {
        val db = this.writableDatabase
        return db.delete(TABLE_NAME, "ID = ?", arrayOf(id))
    }

    fun deleteAllData() {
        val db = this.writableDatabase
        return db.execSQL("delete from $TABLE_NAME")
    }

    val allData: Cursor
        get() {
            val db = this.writableDatabase
            val res = db.rawQuery("SELECT * FROM $TABLE_NAME", null)
            return res
        }

    private fun toEntries(cur: Cursor): ArrayList<WayvesselSession> {
        val list = ArrayList<WayvesselSession>()
        while (cur.moveToNext()) {
            var col = 0
            val id = cur.getLong(col++)
            val started = cur.getLong(col++)
            val duration = cur.getLong(col++)
            val name = cur.getString(col++)
            val orns = cur.getLong(col++)
            val gold = cur.getLong(col++)
            val experience = cur.getLong(col++)

            val session = WayvesselSession(name, id)
            session.mStarted = LocalDateTime.ofInstant(Instant.ofEpochSecond(started), ZoneId.systemDefault())
            session.mDurationSeconds = duration
            session.orns = orns
            session.gold = gold
            session.experience = experience

            list.add(session)
        }

        cur.close()
        return list
    }

    fun getEntriesBetween(start: LocalDateTime, end: LocalDateTime): ArrayList<WayvesselSession> {
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

    fun getLastNSessionsFor(name: String, n: Int): ArrayList<WayvesselSession> {
        val db = this.writableDatabase
        return toEntries(
            db.rawQuery(
                "SELECT * FROM $TABLE_NAME " +
                        "WHERE name='${name.replace("'", "''")}' " +
                        "ORDER BY ID DESC LIMIT $n ",
                null
            )
        )
    }

    fun getLastNSessions(n: Int): ArrayList<WayvesselSession> {
        val db = this.writableDatabase
        return toEntries(
            db.rawQuery(
                "SELECT * FROM $TABLE_NAME " +
                        "ORDER BY ID DESC LIMIT $n ",
                null
            )
        )
    }

    companion object {
        val DATABASE_NAME = "wvSession.db"
        val TABLE_NAME = "wayvessel_session"
        val COL_1 = "ID"
        val COL_2 = "started"
        val COL_3 = "duration"
        val COL_4 = "name"
        val COL_5 = "orns"
        val COL_6 = "gold"
        val COL_7 = "experience"
    }
}