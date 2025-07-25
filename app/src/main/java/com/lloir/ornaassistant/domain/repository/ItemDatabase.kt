package com.lloir.ornaassistant.domain.repository

import android.content.Context
import com.lloir.ornaassistant.domain.assessment.ItemBaseStats

/**
 * Interface for the item database that loads and provides access to Orna items
 */
interface ItemDatabase {
    /**
     * Initialize the database from data sources
     * @param context Application context
     * @return true if initialization was successful
     */
    suspend fun initialize(context: Context): Boolean

    /**
     * Find item by exact name match
     * @param itemName The exact name of the item to find
     * @return The item if found, null otherwise
     */
    fun findItemByName(itemName: String): ItemBaseStats?

    /**
     * Find item by partial name match with fuzzy search
     * @param itemName The partial name of the item to find
     * @return The best matching item if found, null otherwise
     */
    fun findItemByPartialName(itemName: String): ItemBaseStats?

    /**
     * Get all items by tier
     * @param tier The tier to filter by
     * @return List of items in the specified tier
     */
    fun getItemsByTier(tier: Int): List<ItemBaseStats>

    /**
     * Get all boss items
     * @return List of boss items
     */
    fun getBossItems(): List<ItemBaseStats>

    /**
     * Get all items (for debugging)
     * @return List of all items in the database
     */
    fun getAllItems(): List<ItemBaseStats>

    /**
     * Search items by stat requirements
     * @param statRequirements Map of stat names to minimum values
     * @return List of items that meet the requirements
     */
    fun findItemsWithStats(statRequirements: Map<String, Int>): List<ItemBaseStats>

    /**
     * Get database statistics
     * @return Statistics about the database
     */
    fun getStats(): DatabaseStats

    /**
     * Clear the cache (useful for reloading)
     */
    fun clear()

    /**
     * Set the language for the database
     * @param language The language code (e.g., "en", "es")
     */
    fun setLanguage(language: String)

    /**
     * Get the current database language
     * @return The current language code
     */
    fun getLanguage(): String

    /**
     * Download a database for a specific language
     * @param context Application context
     * @param language The language code to download
     * @return true if download was successful
     */
    suspend fun downloadDatabase(context: Context, language: String): Boolean

    /**
     * Database statistics data class
     */
    data class DatabaseStats(
        val totalItems: Int,
        val bossItems: Int,
        val tiersRepresented: Int
    )
}
