package com.lloir.ornaassistant.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.lloir.ornaassistant.data.database.dao.ItemAssessmentDao
import com.lloir.ornaassistant.data.database.entities.ItemAssessmentEntity
import com.lloir.ornaassistant.domain.assessment.LocalItemAssessment
import com.lloir.ornaassistant.domain.assessment.EnhancedItemDatabase
import com.lloir.ornaassistant.domain.model.AssessmentResult
import com.lloir.ornaassistant.domain.model.ItemAssessment
import com.lloir.ornaassistant.domain.repository.ItemAssessmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemAssessmentRepositoryImpl @Inject constructor(
    private val itemAssessmentDao: ItemAssessmentDao,
    private val context: Context
) : ItemAssessmentRepository {

    companion object {
        private const val TAG = "ItemAssessmentRepository"
        private val localAssessment = LocalItemAssessment()
    }

    override fun getAllAssessments(): Flow<List<ItemAssessment>> {
        return itemAssessmentDao.getAllAssessments().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getAssessmentsForItem(itemName: String, limit: Int): List<ItemAssessment> {
        return itemAssessmentDao.getAssessmentsForItem(itemName, limit).map { it.toDomainModel() }
    }

    override suspend fun getAssessmentById(id: Long): ItemAssessment? {
        return itemAssessmentDao.getAssessmentById(id)?.toDomainModel()
    }

    override suspend fun insertAssessment(assessment: ItemAssessment): Long {
        return itemAssessmentDao.insertAssessment(assessment.toEntity())
    }

    override suspend fun deleteAssessment(assessment: ItemAssessment) {
        itemAssessmentDao.deleteAssessment(assessment.toEntity())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun deleteOldAssessments(daysOld: Int) {
        val cutoffDate = LocalDateTime.now().minusDays(daysOld.toLong())
        itemAssessmentDao.deleteOldAssessments(cutoffDate)
    }

    override suspend fun deleteAllAssessments() {
        itemAssessmentDao.deleteAllAssessments()
    }

    private fun ensureDatabaseInitialized() {
        EnhancedItemDatabase.initialize(context)
    }

    override suspend fun assessItem(
        itemName: String,
        level: Int,
        attributes: Map<String, Int>,
        originalItemName: String,
        adornmentValues: Map<String, Int>,
        anguishLevel: Int,
        isCelestialWeapon: Boolean,
        isTwoHanded: Boolean,
        isOffHand: Boolean
    ): AssessmentResult {
        // Check for banned item names first - expanded list
        val bannedNames = setOf(
            // Original banned names
            "Vagrant Beasts", "Daily Login", "Notifications", "Codex", "News", "Party",
            "Arena", "Character", "Options", "Runeshop", "Inventory", "Knights of Inferno",
            "Earthen Legion", "FrozenGuard", "Gauntlet",

            // Additional UI elements that should be banned
            "INBOX", "Mail", "Messages", "Settings", "Profile", "Friends", "Guild",
            "Kingdom", "Chat", "World", "Help", "Tutorial", "Guide", "Shop", "Store",
            "Stats", "Achievements", "Quests", "Events", "Leaderboards", "Rankings",
            "PvP", "Raids", "Dungeons", "Map", "Character", "Equipment", "Weapons",
            "Armor", "Accessories", "Consumables", "Materials", "Keys", "Misc",
            "Followers", "Pets", "Mounts", "Abilities", "Skills", "Spells", "Classes",
            "Specializations", "Masteries", "Passive", "Active", "Buff", "Debuff"
        )

        if (itemName.isBlank() || itemName.length < 3 || bannedNames.any { itemName.contains(it, ignoreCase = true) }) {
            Log.d(TAG, "Skipping banned or invalid item: $itemName")
            return createDefaultAssessmentResult()
        }

        // Always use local assessment
        Log.d(TAG, "Assessing item locally: $itemName")
        ensureDatabaseInitialized()
        return localAssessment.assessItemLocally(
            itemName = itemName,
            level = level,
            attributes = attributes,
            adornmentValues = adornmentValues,
            originalItemName = originalItemName,
            anguishLevel = anguishLevel,
            isCelestialWeapon = isCelestialWeapon,
            isTwoHanded = isTwoHanded,
            isOffHand = isOffHand
        )
    }

    private fun createDefaultAssessmentResult(): AssessmentResult {
        return AssessmentResult(
            quality = 0.0,
            stats = emptyMap(),
            materials = emptyList(),
            anguishLevel = 0
        )
    }
}

// Extension functions
private fun ItemAssessmentEntity.toDomainModel(): ItemAssessment {
    return try {
        ItemAssessment(
            id = id,
            itemName = itemName,
            level = level,
            attributes = attributes.mapValues { it.value.toIntOrNull() ?: 0 },
            assessmentResult = com.google.gson.Gson().fromJson(assessmentResult, AssessmentResult::class.java),
            timestamp = timestamp,
            quality = quality
        )
    } catch (e: Exception) {
        Log.e("ItemAssessmentEntity", "Error converting entity to domain model", e)
        // Return a default item assessment on error
        ItemAssessment(
            id = id,
            itemName = itemName,
            level = level,
            attributes = emptyMap(),
            assessmentResult = AssessmentResult(0.0, emptyMap(), emptyList(), 0),
            timestamp = timestamp,
            quality = 0.0
        )
    }
}

private fun ItemAssessment.toEntity(): ItemAssessmentEntity {
    return try {
        ItemAssessmentEntity(
            id = id,
            itemName = itemName,
            level = level,
            attributes = attributes.mapValues { it.value.toString() },
            assessmentResult = com.google.gson.Gson().toJson(assessmentResult),
            timestamp = timestamp,
            quality = quality
        )
    } catch (e: Exception) {
        Log.e("ItemAssessment", "Error converting domain model to entity", e)
        // Return a minimal entity on error
        ItemAssessmentEntity(
            id = id,
            itemName = itemName,
            level = level,
            attributes = emptyMap(),
            assessmentResult = "{}",
            timestamp = timestamp,
            quality = 0.0
        )
    }
}
