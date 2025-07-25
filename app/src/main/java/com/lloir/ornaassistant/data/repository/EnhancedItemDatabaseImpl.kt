package com.lloir.ornaassistant.data.repository

import android.content.Context
import android.util.Log
import com.lloir.ornaassistant.data.network.NetworkClient
import com.lloir.ornaassistant.domain.assessment.ItemBaseStats
import com.lloir.ornaassistant.domain.repository.ItemDatabase
import com.lloir.ornaassistant.domain.repository.ItemParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced ItemDatabase implementation that loads from JSON files
 * File:
 * - items.json (single file containing all items)
 */
@Singleton
class EnhancedItemDatabaseImpl @Inject constructor(
    private val itemParser: ItemParser,
    private val networkClient: NetworkClient
) : ItemDatabase {
    private val TAG = "EnhancedItemDatabase"

    // Using ConcurrentHashMap for thread safety
    private val itemsCache = ConcurrentHashMap<String, ItemBaseStats>()
    private var isLoaded = false
    private var currentLanguage = "en"

    /**
     * Initialize the database from JSON files
     */
    override suspend fun initialize(context: Context): Boolean {
        // If already loaded, return true
        if (isLoaded) return true

        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Loading item database for language: $currentLanguage")

            // Try to load from downloaded files first
            val databaseDir = File(context.filesDir, "databases")
            val hasDownloadedFiles = databaseDir.exists() && 
                databaseDir.listFiles()?.any { it.name.startsWith("${currentLanguage}_") } == true

            val parsedItems = if (hasDownloadedFiles) {
                // Load from downloaded files
                itemParser.parseLanguageSpecificItems(context, currentLanguage)
            } else {
                // Fall back to assets
                itemParser.parseAllItems(context)
            }

            // Clear and update the cache atomically
            synchronized(itemsCache) {
                itemsCache.clear()
                itemsCache.putAll(parsedItems)
                isLoaded = true
            }

            val itemCount = itemsCache.size
            Log.i(TAG, "Loaded $itemCount items into database for language: $currentLanguage")

            // Log some statistics
            val stats = getStats()
            Log.i(TAG, "Boss items: ${stats.bossItems}, Tiers: ${stats.tiersRepresented}")

            itemCount > 0
        }
    }

    /**
     * Find item by exact name match
     */
    override fun findItemByName(itemName: String): ItemBaseStats? {
        return itemsCache[itemName]
    }

    /**
     * Find item by partial name match with fuzzy search
     */
    override fun findItemByPartialName(itemName: String): ItemBaseStats? {
        if (itemsCache.isEmpty()) {
            Log.w(TAG, "Item cache is empty, cannot search for items")
            return null
        }

        // Clean the input name (remove quality prefixes, enchantments)
        val cleanName = cleanItemName(itemName)

        // Try exact match first
        itemsCache[cleanName]?.let {
            Log.d(TAG, "Found exact match for: $cleanName")
            return it
        }

        // Try case-insensitive exact match
        itemsCache.entries.find { it.key.equals(cleanName, ignoreCase = true) }?.value?.let {
            Log.d(TAG, "Found case-insensitive match for: $cleanName")
            return it
        }

        // Try partial matches (item name contains search term or vice versa)
        val partialMatch = itemsCache.values.find { item ->
            cleanName.contains(item.name, ignoreCase = true) ||
                    item.name.contains(cleanName, ignoreCase = true)
        }

        if (partialMatch != null) {
            Log.d(TAG, "Found partial match: ${partialMatch.name} for search: $cleanName")
        } else {
            Log.d(TAG, "No match found for: $cleanName")
        }

        return partialMatch
    }

    /**
     * Clean item name by removing quality prefixes and enchantments
     */
    private fun cleanItemName(itemName: String): String {
        var cleaned = itemName.trim()

        // Remove quality prefixes
        val qualityPrefixes = listOf(
            "Broken", "Poor", "Superior", "Famed", "Legendary", "Ornate",
            "Masterforged", "Demonforged", "Godforged"
        )

        for (prefix in qualityPrefixes) {
            if (cleaned.startsWith(prefix, ignoreCase = true)) {
                cleaned = cleaned.removePrefix(prefix).trim()
                break
            }
        }

        // Remove enchantment prefixes
        val enchantPrefixes = listOf(
            "burning", "embered", "fiery", "flaming", "infernal", "scalding", "warm",
            "chilling", "icy", "oceanic", "snowy", "tidal", "winter",
            "balanced", "earthly", "grounded", "natural", "organic", "rocky", "stony",
            "electric", "shocking", "sparking", "stormy", "thunderous",
            "angelic", "bright", "divine", "moral", "pure", "purifying", "revered",
            "righteous", "saintly", "sublime",
            "corrupted", "diabolic", "demonic", "gloomy", "impious", "profane",
            "unhallowed", "wicked",
            "beastly", "bestial", "chimeric", "dragonic", "wild",
            "colorless", "customary", "normalized", "origin", "reformed", "renewed", "reworked"
        )

        for (prefix in enchantPrefixes) {
            if (cleaned.startsWith(prefix, ignoreCase = true)) {
                cleaned = cleaned.removePrefix(prefix).trim()
                break
            }
        }

        return cleaned
    }

    /**
     * Get all items by tier
     */
    override fun getItemsByTier(tier: Int): List<ItemBaseStats> {
        return itemsCache.values.filter { it.tier == tier }
    }

    /**
     * Get all boss items
     */
    override fun getBossItems(): List<ItemBaseStats> {
        return itemsCache.values.filter { it.isBossItem }
    }

    /**
     * Get all items (for debugging)
     */
    override fun getAllItems(): List<ItemBaseStats> {
        return itemsCache.values.toList()
    }

    /**
     * Search items by stat requirements
     */
    override fun findItemsWithStats(statRequirements: Map<String, Int>): List<ItemBaseStats> {
        return itemsCache.values.filter { item ->
            statRequirements.all { (stat, minValue) ->
                (item.baseStats[stat] ?: 0) >= minValue
            }
        }
    }

    /**
     * Get database statistics
     */
    override fun getStats(): ItemDatabase.DatabaseStats {
        return ItemDatabase.DatabaseStats(
            totalItems = itemsCache.size,
            bossItems = itemsCache.values.count { it.isBossItem },
            tiersRepresented = itemsCache.values.map { it.tier }.distinct().size
        )
    }

    /**
     * Clear the cache (useful for reloading)
     */
    override fun clear() {
        synchronized(itemsCache) {
            itemsCache.clear()
            isLoaded = false
        }
    }

    /**
     * Set the language for the database
     */
    override fun setLanguage(language: String) {
        if (currentLanguage != language) {
            // Clear cache if language changes
            clear()
            currentLanguage = language
        }
    }

    /**
     * Get the current database language
     */
    override fun getLanguage(): String = currentLanguage

    /**
     * Download a database for a specific language
     */
    override suspend fun downloadDatabase(context: Context, language: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Downloading database for language: $language")

                // Create directory if it doesn't exist
                val databaseDir = File(context.filesDir, "databases")
                if (!databaseDir.exists()) {
                    databaseDir.mkdirs()
                }

                // Download the items.json file
                val file = "items.json"
                val url = "https://raw.githubusercontent.com/Orna-tools/OA_Database/main/$language/$file"
                val response = networkClient.downloadFile(url)

                var success = false
                if (response.isSuccessful) {
                    // Save file to internal storage
                    val targetFile = File(databaseDir, "${language}_$file")
                    targetFile.writeBytes(response.body ?: ByteArray(0))
                    Log.d(TAG, "Downloaded $file for $language")
                    success = true
                } else {
                    Log.e(TAG, "Failed to download $file: ${response.errorMessage}")
                }

                if (success) {
                    // Set the new language and reload database
                    setLanguage(language)
                    clear()
                    initialize(context)
                }

                success
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading database", e)
                false
            }
        }
    }
}
