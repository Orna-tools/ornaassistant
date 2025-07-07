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
import com.lloir.ornaassistant.domain.model.OrnaItem
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
        Log.d(TAG, "🔍 Starting assessment for: $itemName (level $level)")

        // STEP 1: Load JSON database if not already loaded
        try {
            val allItems = ornaItemRepository.loadAllItems()
            Log.d(TAG, "📚 JSON database loaded: ${allItems.size} items available")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load JSON database", e)
            return createFailureResult("Failed to load item database")
        }

        // STEP 2: Search for the item in JSON database
        val foundItem = findItemInDatabase(itemName)

        if (foundItem != null) {
            Log.d(TAG, "✅ Found item in JSON database: ${foundItem.name}")
            Log.d(TAG, "📊 Base stats: mag=${foundItem.stats.mag}, ward=${foundItem.stats.ward}, crit=${foundItem.stats.crit}")
            return assessWithJsonDatabase(foundItem, level, attributes, adornmentValues, anguishLevel)
        } else {
            Log.w(TAG, "❌ Item '$itemName' not found in JSON database")
            return createFailureResult("Item not found in database: $itemName")
        }
    }

    /**
     * Find item in JSON database with flexible name matching
     */
    private suspend fun findItemInDatabase(itemName: String): OrnaItem? {
        // Try exact match first
        var results = ornaItemRepository.searchItems(itemName)
        var foundItem = results.firstOrNull { it.name.equals(itemName, ignoreCase = true) }

        if (foundItem != null) {
            Log.d(TAG, "🎯 Exact name match: ${foundItem.name}")
            return foundItem
        }

        // Try partial match
        foundItem = results.firstOrNull { 
            it.name.contains(itemName, ignoreCase = true) || 
            itemName.contains(it.name, ignoreCase = true) 
        }

        if (foundItem != null) {
            Log.d(TAG, "🎯 Partial name match: ${foundItem.name}")
            return foundItem
        }

        // Try removing rarity prefixes and search again
        val cleanName = itemName.replace(Regex("^(Ornate|Legendary|Famed|Superior|Common|Poor|Broken)\\s+", RegexOption.IGNORE_CASE), "")
        if (cleanName != itemName) {
            Log.d(TAG, "🧹 Trying without rarity prefix: '$cleanName'")
            results = ornaItemRepository.searchItems(cleanName)
            foundItem = results.firstOrNull { it.name.equals(cleanName, ignoreCase = true) }

            if (foundItem != null) {
                Log.d(TAG, "🎯 Match without rarity: ${foundItem.name}")
                return foundItem
            }
        }

        Log.w(TAG, "🚫 No match found for '$itemName' (tried: exact, partial, clean name)")
        return null
    }

    /**
     * Assess item using JSON database stats
     */
    private fun assessWithJsonDatabase(
        item: OrnaItem,
        level: Int,
        attributes: Map<String, Int>,
        adornmentValues: Map<String, Int>,
        anguishLevel: Int
    ): AssessmentResult {
        // Use the item's actual base stats from JSON for quality calculation
        val actualStats = attributes
        val expectedStats = mapOf(
            "Mag" to item.stats.mag,
            "Ward" to item.stats.ward,
            "Crit" to item.stats.crit,
            "Att" to item.stats.atk,
            "Def" to item.stats.def,
            "Res" to item.stats.res,
            "HP" to item.stats.hp,
            "Mana" to item.stats.mana,
            "Dex" to item.stats.dex
        ).filter { it.value > 0 }  // Only include stats that the item actually has

        Log.d(TAG, "📈 Expected base stats from JSON: $expectedStats")
        Log.d(TAG, "📈 Actual item stats at level $level: $actualStats")

        // Calculate quality for each relevant stat
        val statQualities = mutableMapOf<String, Double>()
        var totalQuality = 0.0
        var statCount = 0

        // First, process stats that are in the expected stats map
        for ((statName, expectedBase) in expectedStats) {
            val actualValue = actualStats[statName] ?: continue

            // Calculate what this stat should be at the current level
            val growthRate = if (item.isBossItem) 0.125 else 0.10  // 12.5% vs 10%
            val levelMultiplier = Math.pow(1.0 + growthRate, (level - 1).toDouble())
            val expectedAtLevel = (expectedBase * levelMultiplier).toInt()

            // Calculate quality percentage
            val quality = if (expectedAtLevel > 0) {
                (actualValue.toDouble() / expectedAtLevel.toDouble()) * 100.0
            } else {
                100.0
            }

            // Cap quality at reasonable bounds
            val cappedQuality = Math.max(50.0, Math.min(quality, 200.0))
            statQualities[statName] = cappedQuality
            totalQuality += cappedQuality
            statCount++

            Log.d(TAG, "📊 $statName: actual=$actualValue, expected@L$level=$expectedAtLevel, quality=${cappedQuality.toInt()}%")
        }

        // Then, process stats that are in the actual stats but not in the expected stats
        for ((statName, actualValue) in actualStats) {
            if (statName in expectedStats.keys) continue // Already processed

            // For stats not in expected stats, we need to estimate a reasonable expected value
            // For Mana specifically, we can use a formula based on the item's other stats
            if (statName == "Mana" && actualValue > 0) {
                // Estimate expected Mana based on Mag stat if available, or use a default value
                val baseMag = item.stats.mag
                val expectedBase = if (baseMag > 0) baseMag else 100 // Default base value if no Mag stat

                // Calculate what this stat should be at the current level
                val growthRate = if (item.isBossItem) 0.125 else 0.10
                val levelMultiplier = Math.pow(1.0 + growthRate, (level - 1).toDouble())
                val expectedAtLevel = (expectedBase * levelMultiplier).toInt()

                // Calculate quality percentage
                val quality = (actualValue.toDouble() / expectedAtLevel.toDouble()) * 100.0

                // Cap quality at reasonable bounds
                val cappedQuality = Math.max(50.0, Math.min(quality, 200.0))
                statQualities[statName] = cappedQuality
                totalQuality += cappedQuality
                statCount++

                Log.d(TAG, "📊 $statName (estimated): actual=$actualValue, estimated@L$level=$expectedAtLevel, quality=${cappedQuality.toInt()}%")
            }
        }

        val overallQuality = if (statCount > 0) totalQuality / statCount / 100.0 else 1.0
        Log.d(TAG, "🏆 Overall quality: ${(overallQuality * 100).toInt()}%")

        // Create projected stats for display (simplified)
        val projectedStats = mutableMapOf<String, List<String>>()
        for ((statName, expectedBase) in expectedStats) {
            if (expectedBase > 0) {
                // Calculate 10★, MF, DF projections
                val tenStarValue = (expectedBase * overallQuality * 2.59).toInt()  // rough 10★ scaling
                val mfValue = (tenStarValue * 1.2).toInt()
                val dfValue = (tenStarValue * 1.35).toInt()
                val gfValue = (tenStarValue * 1.5).toInt()

                projectedStats[statName] = listOf(
                    tenStarValue.toString(),
                    mfValue.toString(),
                    dfValue.toString(),
                    gfValue.toString()
                )
            }
        }

        // Calculate material requirements
        val materials = listOf(
            135,  // 10★ materials (fixed)
            (300 * overallQuality).toInt(),  // MF materials
            (666 * overallQuality).toInt(),  // DF materials
            0     // GF materials (variable)
        )

        return AssessmentResult(
            quality = overallQuality,
            stats = projectedStats,
            materials = materials,
            anguishLevel = anguishLevel
        )
    }

    /**
     * Create a failure result when assessment cannot be completed
     */
    private fun createFailureResult(reason: String): AssessmentResult {
        Log.e(TAG, "Assessment failed: $reason")
        return AssessmentResult(
            quality = 0.0,
            stats = emptyMap(),
            materials = listOf(0, 0, 0, 0),
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
