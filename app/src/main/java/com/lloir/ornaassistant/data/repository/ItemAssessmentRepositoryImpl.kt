package com.lloir.ornaassistant.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.lloir.ornaassistant.data.database.dao.ItemAssessmentDao
import com.lloir.ornaassistant.data.database.entities.ItemAssessmentEntity
import com.lloir.ornaassistant.domain.assessment.LocalItemAssessment
import com.lloir.ornaassistant.domain.assessment.EnhancedItemDatabase
import com.lloir.ornaassistant.data.repository.OrnaItemRepository
import com.lloir.ornaassistant.domain.model.AssessmentResult
import com.lloir.ornaassistant.domain.model.ItemAssessment
import com.lloir.ornaassistant.domain.repository.ItemAssessmentRepository
import com.lloir.ornaassistant.utils.PerfectOrnaCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemAssessmentRepositoryImpl @Inject constructor(
    private val itemAssessmentDao: ItemAssessmentDao,
    private val context: Context,
    private val ornaItemRepository: OrnaItemRepository
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

        // NEW: Try to find item in our JSON database first
        val foundItem = ornaItemRepository.searchItems(itemName).firstOrNull()
        if (foundItem != null) {
            Log.d(TAG, "Found item in database: ${foundItem.name} (T${foundItem.tier})")
            return assessItemWithDatabase(foundItem, level, attributes, adornmentValues, anguishLevel)
        }

        // Fallback to old method for items not in database
        Log.d(TAG, "Item not found in database, using legacy assessment: $itemName")
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

    /**
     * Assess item using new JSON database and perfect calculator
     */
    private suspend fun assessItemWithDatabase(
        item: com.lloir.ornaassistant.domain.model.OrnaItem,
        level: Int,
        attributes: Map<String, Int>,
        adornmentValues: Map<String, Int>,
        anguishLevel: Int
    ): AssessmentResult {
        try {
            // Calculate final stats using perfect calculator
            val calculatedStats = PerfectOrnaCalculator.calculateItemStats(
                baseStats = item.stats.toMap(),
                isBoss = item.isBossItem,
                upgradeLevel = "10", // Default to 10★ for comparison
                quality = 1.0, // 100% quality
                adornments = adornmentValues
            )

            // Estimate quality based on actual vs calculated stats
            val primaryStat = findPrimaryStat(attributes)
            val quality = if (primaryStat != null) {
                val baseStat = item.stats.toMap()[primaryStat] ?: 0
                val actualStat = attributes[primaryStat]?.toDouble() ?: 0.0
                PerfectOrnaCalculator.estimateQuality(actualStat, baseStat, item.isBossItem, "10")
            } else 1.0

            return createAssessmentResult(calculatedStats, quality, item)
        } catch (e: Exception) {
            Log.e(TAG, "Error in database assessment", e)
            return createDefaultAssessmentResult()
        }
    }

    private fun createDefaultAssessmentResult(): AssessmentResult {
        return AssessmentResult(
            quality = 0.0,
            stats = emptyMap(),
            materials = emptyList(),
            anguishLevel = 0
        )
    }

    private fun findPrimaryStat(attributes: Map<String, Int>): String? {
        // Find the highest non-zero stat
        return attributes.entries
            .filter { it.value > 0 }
            .maxByOrNull { it.value }
            ?.key?.lowercase()
    }

    private fun createAssessmentResult(
        calculatedStats: Map<String, Double>,
        quality: Double,
        item: com.lloir.ornaassistant.domain.model.OrnaItem
    ): AssessmentResult {
        // Convert calculated stats to format expected by UI
        val statsMap = calculatedStats.mapValues { (_, value) ->
            listOf(
                value.toInt().toString(), // 10★
                (value * 1.7).toInt().toString(), // MF estimate
                (value * 2.0).toInt().toString(), // DF estimate
                (value * 2.5).toInt().toString()  // GF estimate
            )
        }

        return AssessmentResult(
            quality = quality,
            stats = statsMap,
            materials = listOf(135, 300, 666, 0), // Standard material costs
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
