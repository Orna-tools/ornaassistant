package com.lloir.ornaassistant.domain.repository

import android.content.Context
import com.lloir.ornaassistant.domain.assessment.ItemBaseStats

/**
 * Interface for parsing item data from various sources
 */
interface ItemParser {
    /**
     * Parse all items from the data source
     * @param context Application context needed to access assets
     * @return Map of item name to ItemBaseStats
     */
    suspend fun parseAllItems(context: Context): Map<String, ItemBaseStats>
    
    /**
     * Detect if an item is a boss item based on name patterns
     * @param itemName The name of the item to check
     * @return true if the item is a boss item, false otherwise
     */
    fun detectBossItem(itemName: String): Boolean
}