package com.lloir.ornaassistant.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.lloir.ornaassistant.data.database.dao.ItemAssessmentDao
import com.lloir.ornaassistant.data.database.entities.ItemAssessmentEntity
import com.lloir.ornaassistant.domain.assessment.LocalItemAssessment
import com.lloir.ornaassistant.domain.repository.ItemDatabase
import com.lloir.ornaassistant.domain.model.OrnaItem
import com.lloir.ornaassistant.domain.model.AssessmentResult
import com.lloir.ornaassistant.domain.model.ItemAssessment
import com.lloir.ornaassistant.domain.model.ItemType
import com.lloir.ornaassistant.domain.repository.ItemAssessmentRepository
import com.lloir.ornaassistant.utils.OrnaCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemAssessmentRepositoryImpl @Inject constructor(
    private val itemAssessmentDao: ItemAssessmentDao,
    private val context: Context,
    private val ornaItemRepository: OrnaItemRepositoryImpl,
    private val itemDatabase: ItemDatabase
) : ItemAssessmentRepository {

    companion object {
        private const val TAG = "ItemAssessmentRepository"
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

    private suspend fun ensureDatabaseInitialized() {
        itemDatabase.initialize(context)
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
            ensureDatabaseInitialized()
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
            return assessWithJsonDatabase(foundItem, level, attributes, adornmentValues, anguishLevel, originalItemName)
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
        anguishLevel: Int,
        originalItemName: String
    ): AssessmentResult {
        // Extract rarity from original item name
        val rarity = extractRarityFromName(originalItemName)
        val rarityMultiplier = com.lloir.ornaassistant.utils.ItemUtils.RARITY_MULTIPLIERS[rarity] ?: 1.0

        Log.d(TAG, "🔍 Item rarity: $rarity (multiplier: $rarityMultiplier)")

        // Adjust actual stats by removing adornment contributions
        val adjustedStats = attributes.mapValues { (statName, value) ->
            val adornValue = adornmentValues[statName] ?: 0
            // Subtract positive adornment values, add negative ones
            if (adornValue > 0) value - adornValue else value + Math.abs(adornValue)
        }

        // Use the adjusted stats for quality calculation
        val actualStats = adjustedStats

        // Log the adjustment for debugging
        Log.d(TAG, "📈 Original stats: $attributes")
        Log.d(TAG, "📈 Adornment values: $adornmentValues")
        Log.d(TAG, "📈 Adjusted stats (without adornments): $actualStats")

        // Get all expected stats from the item
        val allExpectedStats = mapOf(
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

        // Determine which stats are relevant for this item type
        val relevantStats = getRelevantStatsForItemType(item.type, allExpectedStats, item.isBossItem)

        Log.d(TAG, "📈 Expected base stats from JSON: $allExpectedStats")
        Log.d(TAG, "📈 Relevant stats for ${item.type}: ${relevantStats.keys}")
        Log.d(TAG, "📈 Actual item stats at level $level: $actualStats")

        // Calculate quality for each relevant stat
        val statQualities = mutableMapOf<String, Double>()
        var totalQuality = 0.0
        var statCount = 0

        // Process relevant stats
        for ((statName, expectedBase) in relevantStats) {
            val actualValue = actualStats[statName] ?: continue

            // Calculate what this stat should be at the current level
            val isCritStat = statName == "Crit"

            // Special handling for Crit stat - no level scaling per ORNA STAT CALC guide
            val expectedAtLevel = if (isCritStat) {
                // Crit doesn't scale with level
                expectedBase
            } else {
                // Normal level scaling for other stats
                val growthRate = if (item.isBossItem) 0.125 else 0.10  // 12.5% vs 10%
                val levelMultiplier = Math.pow(1.0 + growthRate, (level - 1).toDouble())
                (expectedBase * levelMultiplier).toInt()
            }

            // Calculate quality percentage based on ORNA STAT CALC guide
            val quality = when {
                expectedAtLevel > 0 && actualValue > 0 -> {
                    // Normal positive stats
                    (actualValue.toDouble() / expectedAtLevel.toDouble()) * 100.0
                }
                expectedAtLevel < 0 && actualValue < 0 -> {
                    // Both negative (cursed stats) - closer to 0 is better
                    val expectedDistance = kotlin.math.abs(expectedAtLevel)
                    val actualDistance = kotlin.math.abs(actualValue)
                    (expectedDistance.toDouble() / actualDistance.toDouble()) * 100.0
                }
                expectedAtLevel < 0 && actualValue >= 0 -> {
                    // Expected negative but actual is positive/zero - exceptional quality!
                    200.0 // 200% quality (maximum)
                }
                else -> {
                    // Default to 100% for other cases
                    100.0
                }
            }

            // Apply special handling for Ward stat based on ORNA STAT CALC guide
            val adjustedQuality = if (statName == "Ward") {
                // Ward has special percentage-based calculation and is weighted more heavily
                // Apply a 1.125x multiplier to Ward quality to account for this
                quality * 1.125
            } else {
                quality
            }

            // Cap quality at reasonable bounds
            val cappedQuality = Math.max(50.0, Math.min(adjustedQuality, 200.0))
            statQualities[statName] = cappedQuality
            totalQuality += cappedQuality
            statCount++

            // Enhanced logging to show quality calculation details
            when {
                statName == "Ward" -> {
                    Log.d(TAG, "📊 $statName: actual=$actualValue, expected@L$level=$expectedAtLevel, raw_quality=${quality.toInt()}%, adjusted=${cappedQuality.toInt()}% (with 1.125x multiplier)")
                }
                statName == "Crit" -> {
                    Log.d(TAG, "📊 $statName: actual=$actualValue, expected=$expectedAtLevel, quality=${cappedQuality.toInt()}% (no level scaling)")
                }
                expectedAtLevel < 0 -> {
                    // Cursed stat logging
                    val expectedAbs = kotlin.math.abs(expectedAtLevel)
                    val actualAbs = kotlin.math.abs(actualValue)
                    Log.d(TAG, "📊 $statName: actual=$actualValue, expected@L$level=$expectedAtLevel, quality=${cappedQuality.toInt()}% (cursed stat: |expected|=$expectedAbs, |actual|=$actualAbs)")
                }
                else -> {
                    Log.d(TAG, "📊 $statName: actual=$actualValue, expected@L$level=$expectedAtLevel, quality=${cappedQuality.toInt()}%")
                }
            }
        }

        // Calculate overall quality - use maximum quality instead of average
        // This better aligns with the ORNA STAT CALC guide which emphasizes certain stats
        val maxQuality = statQualities.values.maxOrNull() ?: 100.0
        val maxStatName = statQualities.entries.firstOrNull { it.value == maxQuality }?.key ?: "unknown"

        // Apply rarity multiplier to the quality
        val qualityWithRarity = maxQuality * rarityMultiplier / 100.0
        val overallQuality = qualityWithRarity.coerceAtMost(2.0) // Cap at 200%

        // Enhanced logging for quality calculation
        Log.d(TAG, "📊 All stat qualities: $statQualities")
        Log.d(TAG, "📊 Raw quality: ${maxQuality.toInt()}% (from $maxStatName)")
        Log.d(TAG, "📊 Quality with rarity ($rarity): ${(qualityWithRarity * 100).toInt()}%")
        Log.d(TAG, "🏆 Overall quality: ${(overallQuality * 100).toInt()}% (capped at 200%)")

        // Create projected stats for display (simplified)
        val projectedStats = mutableMapOf<String, List<String>>()
        for ((statName, expectedBase) in allExpectedStats) {
            // Calculate the expected stat at the current level (same as for quality calculation)
            val isCritStat = statName == "Crit"
            val expectedAtLevel = if (isCritStat) {
                // Crit doesn't scale with level
                expectedBase
            } else {
                // Normal level scaling for other stats
                val growthRate = if (item.isBossItem) 0.125 else 0.10  // 12.5% vs 10%
                val levelMultiplier = Math.pow(1.0 + growthRate, (level - 1).toDouble())
                (expectedBase * levelMultiplier).toInt()
            }

            // Special handling for different stat types
            val isWard = statName == "Ward"
            val isCursed = expectedBase < 0
            val isCrit = statName == "Crit"

            // Use OrnaCalculator for proper stat calculation with the level-adjusted stat
            val tenStarValue = when {
                isCursed -> {
                    // Cursed stats (negative) - lower is better, so we want to reduce the absolute value
                    // For cursed stats, we invert the quality effect: higher quality means closer to 0
                    val baseAbs = kotlin.math.abs(expectedAtLevel)  // Use expectedAtLevel instead of expectedBase
                    val qualityFactor = 2.0 - overallQuality // Invert quality effect (1.0 becomes 1.0, 2.0 becomes 0.0)
                    val adjustedQuality = qualityFactor.coerceIn(0.1, 1.0)
                    -OrnaCalculator.calculateFinalStat(
                        baseStat = baseAbs,
                        isBoss = item.isBossItem,
                        upgradeLevel = "10",
                        quality = adjustedQuality,
                        isWard = false
                    ).toInt()
                }
                else -> {
                    // Standard calculation using OrnaCalculator
                    OrnaCalculator.calculateFinalStat(
                        baseStat = expectedAtLevel,  // Use expectedAtLevel instead of expectedBase
                        isBoss = item.isBossItem,
                        upgradeLevel = "10",
                        quality = overallQuality,
                        isWard = isWard
                    ).toInt()
                }
            }

            // Calculate MF, DF, and GF values using OrnaCalculator
            val mfValue = if (isCursed) {
                // For cursed stats, use the same approach with adjusted quality
                val baseAbs = kotlin.math.abs(expectedAtLevel)  // Use expectedAtLevel instead of expectedBase
                val qualityFactor = 2.0 - overallQuality
                val adjustedQuality = qualityFactor.coerceIn(0.1, 1.0)
                -OrnaCalculator.calculateFinalStat(
                    baseStat = baseAbs,
                    isBoss = item.isBossItem,
                    upgradeLevel = "MF",
                    quality = adjustedQuality,
                    isWard = false
                ).toInt()
            } else {
                OrnaCalculator.calculateFinalStat(
                    baseStat = expectedAtLevel,  // Use expectedAtLevel instead of expectedBase
                    isBoss = item.isBossItem,
                    upgradeLevel = "MF",
                    quality = overallQuality,
                    isWard = isWard
                ).toInt()
            }

            val dfValue = if (isCursed) {
                val baseAbs = kotlin.math.abs(expectedAtLevel)  // Use expectedAtLevel instead of expectedBase
                val qualityFactor = 2.0 - overallQuality
                val adjustedQuality = qualityFactor.coerceIn(0.1, 1.0)
                -OrnaCalculator.calculateFinalStat(
                    baseStat = baseAbs,
                    isBoss = item.isBossItem,
                    upgradeLevel = "DF",
                    quality = adjustedQuality,
                    isWard = false
                ).toInt()
            } else {
                OrnaCalculator.calculateFinalStat(
                    baseStat = expectedAtLevel,  // Use expectedAtLevel instead of expectedBase
                    isBoss = item.isBossItem,
                    upgradeLevel = "DF",
                    quality = overallQuality,
                    isWard = isWard
                ).toInt()
            }

            val gfValue = if (isCursed) {
                val baseAbs = kotlin.math.abs(expectedAtLevel)  // Use expectedAtLevel instead of expectedBase
                val qualityFactor = 2.0 - overallQuality
                val adjustedQuality = qualityFactor.coerceIn(0.1, 1.0)
                -OrnaCalculator.calculateFinalStat(
                    baseStat = baseAbs,
                    isBoss = item.isBossItem,
                    upgradeLevel = "GF",
                    quality = adjustedQuality,
                    isWard = false
                ).toInt()
            } else {
                OrnaCalculator.calculateFinalStat(
                    baseStat = expectedAtLevel,  // Use expectedAtLevel instead of expectedBase
                    isBoss = item.isBossItem,
                    upgradeLevel = "GF",
                    quality = overallQuality,
                    isWard = isWard
                ).toInt()
            }

            // Add to projected stats
            projectedStats[statName] = listOf(
                tenStarValue.toString(),
                mfValue.toString(),
                dfValue.toString(),
                gfValue.toString()
            )

            // Log projected stats for debugging
            when {
                isCursed -> {
                    Log.d(TAG, "📈 Projected cursed $statName: base=${expectedBase}, 10★=${tenStarValue}, MF=${mfValue}, DF=${dfValue}, GF=${gfValue}")
                }
                isWard -> {
                    Log.d(TAG, "📈 Projected Ward $statName: base=${expectedBase}, 10★=${tenStarValue}, MF=${mfValue}, DF=${dfValue}, GF=${gfValue}")
                }
                isCrit -> {
                    Log.d(TAG, "📈 Projected Crit $statName: base=${expectedBase}, 10★=${tenStarValue}, MF=${mfValue}, DF=${dfValue}, GF=${gfValue} (no level scaling)")
                }
                else -> {
                    Log.d(TAG, "📈 Projected $statName: base=${expectedBase}, 10★=${tenStarValue}, MF=${mfValue}, DF=${dfValue}, GF=${gfValue}")
                }
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
     * Determine which stats are relevant for a specific item type
     */
    private fun getRelevantStatsForItemType(
        itemType: ItemType,
        availableStats: Map<String, Int>,
        isBossItem: Boolean = false
    ): Map<String, Int> {
        // For boss items, consider all available stats
        if (isBossItem) {
            return availableStats
        }

        // For non-boss items, use the existing logic
        return when (itemType) {
            ItemType.WEAPON -> {
                // For weapons, prioritize Att/Mag and other offensive stats
                // Also include Ward and HP which are important for quality assessment
                availableStats.filter { (key, _) ->
                    key in listOf("Att", "Mag", "Crit", "Dex", "Ward", "HP")
                }
            }
            ItemType.ARMOR -> {
                // For armor, prioritize defensive stats
                availableStats.filter { (key, _) ->
                    key in listOf("Def", "Res", "HP", "Ward")
                }
            }
            ItemType.HEAD, ItemType.LEGS -> {
                // For head/legs, similar to armor
                availableStats.filter { (key, _) ->
                    key in listOf("Def", "Res", "HP", "Ward")
                }
            }
            ItemType.OFF_HAND -> {
                // For off-hands (shields, etc.), prioritize defensive stats
                // but also consider offensive if present (for tomes, etc.)
                val stats = availableStats.filter { (key, _) ->
                    key in listOf("Def", "Res", "HP", "Ward")
                }
                // If no defensive stats or very few, include offensive stats
                if (stats.size <= 1) {
                    stats + availableStats.filter { (key, _) ->
                        key in listOf("Att", "Mag")
                    }
                } else {
                    stats
                }
            }
            ItemType.ACCESSORY -> {
                // For accessories, consider all stats as they can be varied
                // but prioritize the highest value stats (top 2-3)
                val sortedStats = availableStats.entries.sortedByDescending { it.value }
                sortedStats.take(3).associate { it.key to it.value }
            }
            else -> {
                // Default case: use all available stats
                availableStats
            }
        }
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

    /**
     * Extract rarity from item name
     */
    private fun extractRarityFromName(itemName: String): String {
        // Check for exact matches at the start
        val rarities = listOf("Broken", "Poor", "Common", "Superior", "Famed", "Legendary", "Ornate")
        for (rarity in rarities) {
            if (itemName.startsWith(rarity, ignoreCase = true)) {
                return rarity
            }
        }

        // Check for upgrade prefixes which imply Ornate
        val upgradeKeywords = listOf("Masterforged", "Demonforged", "Godforged")
        for (upgrade in upgradeKeywords) {
            if (itemName.startsWith(upgrade, ignoreCase = true)) {
                return "Ornate"
            }
        }

        return "Common" // Default
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
