package com.lloir.ornaassistant.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import android.util.Log
import com.lloir.ornaassistant.data.database.dao.*
import com.lloir.ornaassistant.data.database.entities.*

@Database(
    entities = [
        DungeonVisitEntity::class,
        WayvesselSessionEntity::class,
        KingdomMemberEntity::class,
        ItemAssessmentEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class OrnaDatabase : RoomDatabase() {

    abstract fun dungeonVisitDao(): DungeonVisitDao
    abstract fun wayvesselSessionDao(): WayvesselSessionDao
    abstract fun kingdomMemberDao(): KingdomMemberDao
    abstract fun itemAssessmentDao(): ItemAssessmentDao

    companion object {
        const val DATABASE_NAME = "orna_assistant_database"

        // Migration from version 1 to 2 - add battle reward tracking
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to dungeon_visits table
                Log.d("OrnaDatabase", "Running migration 1->2: Adding battle/floor reward tracking columns")
                database.execSQL("ALTER TABLE dungeon_visits ADD COLUMN battleOrns INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE dungeon_visits ADD COLUMN battleGold INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE dungeon_visits ADD COLUMN battleExperience INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE dungeon_visits ADD COLUMN floorOrns INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE dungeon_visits ADD COLUMN floorGold INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE dungeon_visits ADD COLUMN floorExperience INTEGER NOT NULL DEFAULT 0")

                // Migrate existing data - set floor values to current totals
                database.execSQL("UPDATE dungeon_visits SET floorOrns = orns WHERE floorOrns = 0")
                database.execSQL("UPDATE dungeon_visits SET floorGold = gold WHERE floorGold = 0")
                database.execSQL("UPDATE dungeon_visits SET floorExperience = experience WHERE floorExperience = 0")
                Log.d("OrnaDatabase", "Migration 1->2 completed successfully")
            }
        }

        // Migration from version 2 to 3 - add floor rewards tracking
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d("OrnaDatabase", "Running migration 2->3: Adding floor rewards tracking")
                database.execSQL("ALTER TABLE dungeon_visits ADD COLUMN floorRewards TEXT NOT NULL DEFAULT '[]'")
                Log.d("OrnaDatabase", "Migration 2->3 completed successfully")
            }
        }

        // Migration from legacy database (if needed)
        val MIGRATION_LEGACY_TO_1 = object : Migration(0, 1) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new tables - Room will handle this automatically
                // This migration is for users upgrading from the old app

                // We'll create a separate migration script if needed to import old data
                // For now, start fresh since it's a complete rewrite
            }
        }

        fun create(context: Context): OrnaDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                OrnaDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_LEGACY_TO_1, MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration() // For development - remove in production
                .build()
        }
    }
}