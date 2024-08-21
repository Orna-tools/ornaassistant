package com.rockethat.ornaassistant.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class KingdomGauntletDatabaseHelper(context: Context) :SQLiteOpenHelper(context, DATABASE_NAME, null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_NAME (" +
                    "time INTEGER," +
                    "name TEXT" +
                    ")"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertData(dt: LocalDateTime, name:String) {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COL_1, dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000)
        contentValues.put(COL_2, name)
        db.insert(TABLE_NAME, null, contentValues)
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

    private fun toEntries(cur: Cursor): List<KingdomMemberDatabaseItem> {
        val list = mutableListOf<KingdomMemberDatabaseItem>()

        while (cur.moveToNext()) {
            var col = 0
            val started = cur.getLong(col++)
            val name = cur.getString(col++)

            val startedDt = LocalDateTime.ofInstant(Instant.ofEpochSecond(started), ZoneId.systemDefault())

            list.add(KingdomMemberDatabaseItem(startedDt, name))
        }
        cur.close()

        return list
    }

    fun getLastNEntries(n: Int): List<KingdomMemberDatabaseItem> {
        val db = this.writableDatabase
        return toEntries(
            db.rawQuery(
                "SELECT * FROM $TABLE_NAME " +
                        "ORDER BY time DESC LIMIT $n ",
                null
            )
        )
    }

    fun getEntriesBetween(start: LocalDateTime, end: LocalDateTime, name: String): List<KingdomMemberDatabaseItem> {
        val startUnix = start.toEpochSecond(ZoneOffset.UTC)
        val endUnix = end.toEpochSecond(ZoneOffset.UTC)
        val db = this.writableDatabase
        return toEntries(
            db.rawQuery(
                "SELECT * FROM $TABLE_NAME " +
                        "WHERE time > $startUnix " +
                        "AND time < $endUnix " +
                        "AND name = '${name.replace("'", "''")}'",
                null
            )
        )
    }

    fun getEntriesBetween(start: LocalDateTime, end: LocalDateTime): List<KingdomMemberDatabaseItem> {
        val startUnix = start.toEpochSecond(ZoneOffset.UTC)
        val endUnix = end.toEpochSecond(ZoneOffset.UTC)
        val db = this.writableDatabase
        return toEntries(
            db.rawQuery(
                "SELECT * FROM $TABLE_NAME " +
                        "WHERE time > $startUnix " +
                        "AND time < $endUnix ",
                null
            )
        )
    }

    companion object {
        val DATABASE_NAME = "kingdomGauntlet.db"
        val TABLE_NAME = "kg"
        val COL_1 = "time"
        val COL_2 = "name"
    }
}