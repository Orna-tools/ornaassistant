package com.lloir.ornaassistant.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.lloir.ornaassistant.KingdomMember

class KingdomMemberDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, VERSION) {

    companion object {
        private const val TAG = "KingdomMemberDB"
        const val DATABASE_NAME = "kingdom.db"
        const val TABLE_NAME = "kingdom_member"
        const val COL_1 = "ign"
        const val COL_2 = "discord"
        const val COL_3 = "tz"
        const val VERSION = 2
    }

    override fun onCreate(db: SQLiteDatabase) {
        try {
            db.execSQL(
                "CREATE TABLE $TABLE_NAME (" +
                        "$COL_1 TEXT PRIMARY KEY," +
                        "$COL_2 TEXT," +
                        "$COL_3 INTEGER" +
                        ")"
            )
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error creating database", e)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            if (oldVersion == 1 && newVersion == 2) {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COL_3 INTEGER DEFAULT 1000")
            }
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error upgrading database", e)
        }
    }

    fun insertData(entry: KingdomMember): Boolean {
        return try {
            val db = this.writableDatabase
            val contentValues = ContentValues().apply {
                put(COL_1, entry.character)
                put(COL_2, entry.discordName)
                put(COL_3, entry.timezone)
            }

            val existing = getEntry(entry.character)
            if (existing != null) {
                updateData(entry)
            } else {
                val result = db.insert(TABLE_NAME, null, contentValues)
                result != -1L
            }
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error inserting data", e)
            false
        }
    }

    fun updateData(entry: KingdomMember): Boolean {
        return try {
            val db = this.writableDatabase
            val existing = getEntry(entry.character)

            if (existing != null &&
                existing.discordName == entry.discordName &&
                existing.timezone == entry.timezone) {
                return false
            }

            val contentValues = ContentValues().apply {
                put(COL_1, entry.character)
                put(COL_2, entry.discordName)
                put(COL_3, entry.timezone)
            }

            val rowsUpdated = db.update(
                TABLE_NAME,
                contentValues,
                "$COL_1 = ?",
                arrayOf(entry.character)
            )
            rowsUpdated > 0
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error updating data", e)
            false
        }
    }

    fun deleteData(ign: String): Int {
        return try {
            val db = this.writableDatabase
            db.delete(TABLE_NAME, "$COL_1 = ?", arrayOf(ign))
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

    val allData: ArrayList<KingdomMember>
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

    fun getEntry(ign: String): KingdomMember? {
        return try {
            val db = this.readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_NAME WHERE $COL_1 = ?",
                arrayOf(ign)
            )
            val entries = toEntries(cursor)
            entries.firstOrNull()
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error getting entry", e)
            null
        }
    }

    private fun toEntries(cursor: Cursor): ArrayList<KingdomMember> {
        val list = ArrayList<KingdomMember>()
        try {
            cursor.use { c ->
                while (c.moveToNext()) {
                    val ignIndex = c.getColumnIndexOrThrow(COL_1)
                    val discordIndex = c.getColumnIndexOrThrow(COL_2)
                    val tzIndex = c.getColumnIndexOrThrow(COL_3)

                    val ign = c.getString(ignIndex)
                    val discord = c.getString(discordIndex)
                    val tz = c.getInt(tzIndex)

                    val member = KingdomMember(ign, mutableMapOf()).apply {
                        discordName = discord
                        timezone = tz
                    }
                    list.add(member)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting cursor to entries", e)
        }
        return list
    }
}
