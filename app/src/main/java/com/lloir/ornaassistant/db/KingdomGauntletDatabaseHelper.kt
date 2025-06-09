package com.lloir.ornaassistant.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class KingdomGauntletDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, 2) {

    companion object {
        private const val TAG = "KingdomGauntletDB"
        const val DATABASE_NAME = "kingdomGauntlet.db"
        const val TABLE_NAME = "kg"
        const val COL_1 = "time"
        const val COL_2 = "name"
    }

    override fun onCreate(db: SQLiteDatabase) {
        try {
            db.execSQL(
                "CREATE TABLE $TABLE_NAME (" +
                        "$COL_1 INTEGER," +
                        "$COL_2 TEXT" +
                        ")"
            )
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error creating database", e)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
            onCreate(db)
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error upgrading database", e)
        }
    }

    fun insertData(dt: LocalDateTime, name: String): Boolean {
        return try {
            val db = this.writableDatabase
            val contentValues = ContentValues().apply {
                put(COL_1, dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000)
                put(COL_2, name)
            }
            val result = db.insert(TABLE_NAME, null, contentValues)
            result != -1L
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error inserting data", e)
            false
        }
    }

    fun deleteData(timeStamp: Long): Int {
        return try {
            val db = this.writableDatabase
            db.delete(TABLE_NAME, "$COL_1 = ?", arrayOf(timeStamp.toString()))
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error deleting data", e)
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

    val allData: Cursor?
        get() {
            return try {
                val db = this.readableDatabase
                db.rawQuery("SELECT * FROM $TABLE_NAME", null)
            } catch (e: SQLiteException) {
                Log.e(TAG, "Error getting all data", e)
                null
            }
        }

    private fun toEntries(cursor: Cursor): List<KingdomMemberDatabaseItem> {
        val list = mutableListOf<KingdomMemberDatabaseItem>()
        try {
            cursor.use { c ->
                while (c.moveToNext()) {
                    val timeIndex = c.getColumnIndexOrThrow(COL_1)
                    val nameIndex = c.getColumnIndexOrThrow(COL_2)

                    val started = c.getLong(timeIndex)
                    val name = c.getString(nameIndex)
                    val startedDt = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(started),
                        ZoneId.systemDefault()
                    )
                    list.add(KingdomMemberDatabaseItem(startedDt, name))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting cursor to entries", e)
        }
        return list
    }

    fun getLastNEntries(n: Int): List<KingdomMemberDatabaseItem> {
        return try {
            val db = this.readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_NAME ORDER BY $COL_1 DESC LIMIT ?",
                arrayOf(n.toString())
            )
            toEntries(cursor)
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error getting last N entries", e)
            emptyList()
        }
    }

    fun getEntriesBetween(start: LocalDateTime, end: LocalDateTime, name: String): List<KingdomMemberDatabaseItem> {
        return try {
            val startUnix = start.toEpochSecond(ZoneOffset.UTC)
            val endUnix = end.toEpochSecond(ZoneOffset.UTC)
            val db = this.readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_NAME WHERE $COL_1 > ? AND $COL_1 < ? AND $COL_2 = ?",
                arrayOf(startUnix.toString(), endUnix.toString(), name)
            )
            toEntries(cursor)
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error getting entries between dates", e)
            emptyList()
        }
    }

    fun getEntriesBetween(start: LocalDateTime, end: LocalDateTime): List<KingdomMemberDatabaseItem> {
        return try {
            val startUnix = start.toEpochSecond(ZoneOffset.UTC)
            val endUnix = end.toEpochSecond(ZoneOffset.UTC)
            val db = this.readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_NAME WHERE $COL_1 > ? AND $COL_1 < ?",
                arrayOf(startUnix.toString(), endUnix.toString())
            )
            toEntries(cursor)
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error getting entries between dates", e)
            emptyList()
        }
    }
}