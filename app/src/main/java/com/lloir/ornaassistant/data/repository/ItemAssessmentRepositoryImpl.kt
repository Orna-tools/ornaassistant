package com.lloir.ornaassistant.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.gson.JsonSyntaxException
import com.lloir.ornaassistant.data.database.dao.ItemAssessmentDao
import com.lloir.ornaassistant.data.database.entities.ItemAssessmentEntity
import com.lloir.ornaassistant.data.network.api.OrnaGuideApi
import com.lloir.ornaassistant.data.network.toAssessmentRequest
import com.lloir.ornaassistant.data.network.toAssessmentResult
import com.lloir.ornaassistant.domain.model.AssessmentResult
import com.lloir.ornaassistant.domain.model.ItemAssessment
import com.lloir.ornaassistant.domain.repository.ItemAssessmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemAssessmentRepositoryImpl @Inject constructor(
    private val itemAssessmentDao: ItemAssessmentDao,
    private val ornaGuideApi: OrnaGuideApi
) : ItemAssessmentRepository {

    companion object {
        private const val TAG = "ItemAssessmentRepo"
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

    override suspend fun assessItem(itemName: String, level: Int, attributes: Map<String, Int>): AssessmentResult {
        // Check for banned item names first - exact matches only for UI elements
        val exactBannedNames = setOf(
            // UI elements that should be banned (exact matches)
            "Vagrant Beasts", "Daily Login", "Notifications", "Codex", "News", "Party",
            "Arena", "Character", "Options", "Runeshop", "Inventory", "Knights of Inferno",
            "Earthen Legion", "FrozenGuard", "Gauntlet", "INBOX", "Mail", "Messages", 
            "Settings", "Profile", "Friends", "Guild", "Kingdom", "Chat", "World", "Help", 
            "Tutorial", "Guide", "Shop", "Store", "Stats", "Achievements", "Quests", 
            "Events", "Leaderboards", "Rankings", "PvP", "Raids", "Dungeons", "Map"
        )

        // Common words that might appear in item names - only filter if they're the entire name
        val commonWords = setOf(
            "Equipment", "Weapons", "Armor", "Accessories", "Consumables", 
            "Materials", "Keys", "Misc", "Followers", "Pets", "Mounts", 
            "Abilities", "Skills", "Spells", "Classes", "Specializations", 
            "Masteries", "Passive", "Active", "Buff", "Debuff"
        )

        if (itemName.isBlank() || itemName.length < 3 || 
            exactBannedNames.any { it.equals(itemName, ignoreCase = true) } ||
            (commonWords.any { it.equals(itemName, ignoreCase = true) })) {
            Log.d(TAG, "Skipping banned or invalid item: $itemName")
            return AssessmentResult(
                quality = 0.0,
                stats = emptyMap(),
                materials = emptyList()
            )
        }

        return try {
            Log.d(TAG, "Assessing item: $itemName (level $level) with attributes: $attributes")

            // Validate input parameters
            if (attributes.isEmpty()) {
                Log.w(TAG, "No attributes provided for assessment")
            }

            val request = attributes.toAssessmentRequest(itemName, level)
            Log.d(TAG, "API request: $request")

            val response = ornaGuideApi.assessItem(request)
            Log.d(TAG, "API response received for $itemName")

            val result = response.toAssessmentResult()
            Log.d(TAG, "Assessment result for $itemName: quality=${result.quality}")

            // Save successful assessment to database
            // insertAssessment(ItemAssessment(...)) - uncomment if you want to store results

            result

        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "JSON parsing error for item $itemName", e)
            createDefaultAssessmentResult()

        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error ${e.code()} for item $itemName", e)
            createDefaultAssessmentResult()

        } catch (e: IOException) {
            Log.e(TAG, "Network error for item $itemName", e)
            createDefaultAssessmentResult()

        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error assessing item $itemName", e)
            createDefaultAssessmentResult()
        }
    }

    private fun createDefaultAssessmentResult(): AssessmentResult {
        return AssessmentResult(
            quality = 0.0,
            stats = emptyMap(),
            materials = listOf(0, 0, 0, 0)
        )
    }

    /**
     * Attempt to correct stats when API returns quality 0
     */
    private suspend fun attemptStatCorrection(
        originalAssessment: ItemAssessment
    ): AssessmentResult? {
        Log.d(TAG, "Attempting stat correction for ${originalAssessment.itemName}")

        // Strategy 1: Try different level offsets
        for (levelOffset in -2..2) {
            val correctedLevel = (originalAssessment.level + levelOffset).coerceIn(0, 12)
            if (correctedLevel == originalAssessment.level) continue

            val correctedAssessment = originalAssessment.copy(level = correctedLevel)

            try {
                val response = ornaGuideApi.assessItem(correctedAssessment.attributes.toAssessmentRequest(correctedAssessment.itemName, correctedLevel))
                val result = response.toAssessmentResult()
                if (result.quality > 0) {
                    Log.i(TAG, "Level correction worked! Level $correctedLevel gave quality ${result.quality}")
                    return result
                }
            } catch (e: Exception) {
                Log.d(TAG, "Level correction failed for offset $levelOffset: ${e.message}")
            }
        }

        // Strategy 2: Try stat variations (accounting for potential OCR errors)
        val statVariations = generateStatVariations(originalAssessment.attributes)

        for (variation in statVariations) {
            val correctedAssessment = originalAssessment.copy(attributes = variation)

            try {
                val response = ornaGuideApi.assessItem(variation.toAssessmentRequest(correctedAssessment.itemName, correctedAssessment.level))
                val result = response.toAssessmentResult()
                if (result.quality > 0) {
                    Log.i(TAG, "Stat variation worked! Stats $variation gave quality ${result.quality}")
                    return result
                }
            } catch (e: Exception) {
                Log.d(TAG, "Stat variation failed: ${e.message}")
            }
        }

        return null
    }

    /**
     * Generate stat variations to account for OCR errors
     */
    private fun generateStatVariations(originalStats: Map<String, Int>): List<Map<String, Int>> {
        val variations = mutableListOf<Map<String, Int>>()

        // For each stat, try ±1, ±2, ±5 variations (common OCR errors)
        val offsets = listOf(-5, -2, -1, 1, 2, 5)

        for ((statName, originalValue) in originalStats) {
            for (offset in offsets) {
                val newValue = (originalValue + offset).coerceAtLeast(0)
                if (newValue != originalValue) {
                    val variation = originalStats.toMutableMap()
                    variation[statName] = newValue
                    variations.add(variation)
                }
            }
        }

        return variations.take(10) // Limit to avoid too many API calls
    }

    /**
     * Enhanced debug logging
     */
    private fun debugAssessmentRequest(assessment: ItemAssessment) {
        Log.d(TAG, "=== ASSESSMENT REQUEST DEBUG ===")
        Log.d(TAG, "Item: ${assessment.itemName}")
        Log.d(TAG, "Level: ${assessment.level}")
        Log.d(TAG, "Stats:")
        assessment.attributes.forEach { (stat, value) ->
            Log.d(TAG, "  $stat: $value")
        }
        Log.d(TAG, "================================")
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
            assessmentResult = AssessmentResult(0.0, emptyMap(), emptyList()),
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
