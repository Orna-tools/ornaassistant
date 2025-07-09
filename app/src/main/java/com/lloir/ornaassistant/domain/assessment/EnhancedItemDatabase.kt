package com.lloir.ornaassistant.domain.assessment

import android.content.Context
import android.util.Log
import com.lloir.ornaassistant.domain.repository.ItemDatabase
import kotlinx.coroutines.runBlocking

/**
 * Compatibility layer for EnhancedItemDatabase
 * 
 * This singleton delegates to the injected ItemDatabase instance to maintain
 * backward compatibility with existing code.
 * 
 * @deprecated New code should use the ItemDatabase interface directly through dependency injection.
 * This compatibility layer will be removed in a future release.
 */
@Deprecated("Use ItemDatabase interface directly through dependency injection", 
            ReplaceWith("itemDatabase", "com.lloir.ornaassistant.domain.repository.ItemDatabase"))
object EnhancedItemDatabase {
    private const val TAG = "EnhancedItemDatabase"

    // Static reference to the injected ItemDatabase instance
    private var itemDatabaseInstance: ItemDatabase? = null

    /**
     * Set the ItemDatabase instance for delegation
     * This should be called from the Application class
     */
    fun setItemDatabase(itemDatabase: ItemDatabase) {
        itemDatabaseInstance = itemDatabase
        Log.d(TAG, "ItemDatabase instance set for compatibility layer")
    }

    private fun getItemDatabase(): ItemDatabase {
        return itemDatabaseInstance ?: throw IllegalStateException(
            "ItemDatabase not initialized. Call setItemDatabase() first or use dependency injection."
        )
    }

    /**
     * Initialize the database from JSON files
     * 
     * @deprecated Use ItemDatabase.initialize directly
     */
    @Deprecated("Use ItemDatabase.initialize directly", 
                ReplaceWith("itemDatabase.initialize(context)", 
                "com.lloir.ornaassistant.domain.repository.ItemDatabase"))
    fun initialize(context: Context): Boolean {
        return runBlocking {
            try {
                getItemDatabase().initialize(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing database", e)
                false
            }
        }
    }

    /**
     * Find item by exact name match
     * 
     * @deprecated Use ItemDatabase.findItemByName directly
     */
    @Deprecated("Use ItemDatabase.findItemByName directly", 
                ReplaceWith("itemDatabase.findItemByName(itemName)", 
                "com.lloir.ornaassistant.domain.repository.ItemDatabase"))
    fun findItemByName(itemName: String): ItemBaseStats? {
        return getItemDatabase().findItemByName(itemName)
    }

    /**
     * Find item by partial name match with fuzzy search
     * 
     * @deprecated Use ItemDatabase.findItemByPartialName directly
     */
    @Deprecated("Use ItemDatabase.findItemByPartialName directly", 
                ReplaceWith("itemDatabase.findItemByPartialName(itemName)", 
                "com.lloir.ornaassistant.domain.repository.ItemDatabase"))
    fun findItemByPartialName(itemName: String): ItemBaseStats? {
        return getItemDatabase().findItemByPartialName(itemName)
    }

    /**
     * Get all items by tier
     * 
     * @deprecated Use ItemDatabase.getItemsByTier directly
     */
    @Deprecated("Use ItemDatabase.getItemsByTier directly", 
                ReplaceWith("itemDatabase.getItemsByTier(tier)", 
                "com.lloir.ornaassistant.domain.repository.ItemDatabase"))
    fun getItemsByTier(tier: Int): List<ItemBaseStats> {
        return getItemDatabase().getItemsByTier(tier)
    }

    /**
     * Get all boss items
     * 
     * @deprecated Use ItemDatabase.getBossItems directly
     */
    @Deprecated("Use ItemDatabase.getBossItems directly", 
                ReplaceWith("itemDatabase.getBossItems()", 
                "com.lloir.ornaassistant.domain.repository.ItemDatabase"))
    fun getBossItems(): List<ItemBaseStats> {
        return getItemDatabase().getBossItems()
    }

    /**
     * Get all items (for debugging)
     * 
     * @deprecated Use ItemDatabase.getAllItems directly
     */
    @Deprecated("Use ItemDatabase.getAllItems directly", 
                ReplaceWith("itemDatabase.getAllItems()", 
                "com.lloir.ornaassistant.domain.repository.ItemDatabase"))
    fun getAllItems(): List<ItemBaseStats> {
        return getItemDatabase().getAllItems()
    }

    /**
     * Search items by stat requirements
     * 
     * @deprecated Use ItemDatabase.findItemsWithStats directly
     */
    @Deprecated("Use ItemDatabase.findItemsWithStats directly", 
                ReplaceWith("itemDatabase.findItemsWithStats(statRequirements)", 
                "com.lloir.ornaassistant.domain.repository.ItemDatabase"))
    fun findItemsWithStats(statRequirements: Map<String, Int>): List<ItemBaseStats> {
        return getItemDatabase().findItemsWithStats(statRequirements)
    }

    /**
     * Get database statistics
     * 
     * @deprecated Use ItemDatabase.getStats directly
     */
    @Deprecated("Use ItemDatabase.getStats directly", 
                ReplaceWith("itemDatabase.getStats()", 
                "com.lloir.ornaassistant.domain.repository.ItemDatabase"))
    fun getStats(): ItemDatabase.DatabaseStats {
        return getItemDatabase().getStats()
    }

    /**
     * Clear the cache (useful for reloading)
     * 
     * @deprecated Use ItemDatabase.clear directly
     */
    @Deprecated("Use ItemDatabase.clear directly", 
                ReplaceWith("itemDatabase.clear()", 
                "com.lloir.ornaassistant.domain.repository.ItemDatabase"))
    fun clear() {
        getItemDatabase().clear()
    }
}
