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
    version = 1,
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

        fun create(context: Context): OrnaDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                OrnaDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_LEGACY_TO_1)
                .fallbackToDestructiveMigration() // For development - remove in production
                .build()
        }
    }
}