package com.lloir.ornaassistant.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.lloir.ornaassistant.data.database.dao.*
import com.lloir.ornaassistant.data.database.entities.*

@Database(
    entities = [
        DungeonVisitEntity::class,
        WayvesselSessionEntity::class,
        KingdomMemberEntity::class,
        ItemAssessmentEntity::class
    ],
    version = 2,
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

        // Migration from legacy database (if needed)
        val MIGRATION_LEGACY_TO_1 = object : Migration(0, 1) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new tables - Room will handle this automatically
                // This migration is for users upgrading from the old app

                // We'll create a separate migration script if needed to import old data
                // For now, start fresh since it's a complete rewrite
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to item_assessments table
                database.execSQL("ALTER TABLE item_assessments ADD COLUMN isOrnate INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE item_assessments ADD COLUMN isGodforged INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE item_assessments ADD COLUMN isDemonforged INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE item_assessments ADD COLUMN isMasterforged INTEGER NOT NULL DEFAULT 0")
                
                // Update existing records based on item names
                database.execSQL("UPDATE item_assessments SET isOrnate = 1 WHERE itemName LIKE '%Ornate%'")
                database.execSQL("UPDATE item_assessments SET isGodforged = 1 WHERE itemName LIKE '%Godforged%'")
                database.execSQL("UPDATE item_assessments SET isDemonforged = 1 WHERE itemName LIKE '%Demonforged%'")
                database.execSQL("UPDATE item_assessments SET isMasterforged = 1 WHERE itemName LIKE '%Masterforged%'")
            }
        }

        fun create(context: Context): OrnaDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                OrnaDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_LEGACY_TO_1)
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration() // For development - remove in production
                .build()
        }
    }
}