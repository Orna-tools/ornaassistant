package com.lloir.ornaassistant.domain.repository

import com.lloir.ornaassistant.domain.model.ItemType
import com.lloir.ornaassistant.domain.model.OrnaItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Orna items
 */
interface OrnaItemRepository {
    
    /**
     * Get all items as a flow
     */
    val allItems: Flow<List<OrnaItem>>
    
    /**
     * Loading state
     */
    val isLoading: Flow<Boolean>
    
    /**
     * Error state
     */
    val error: Flow<String?>
    
    /**
     * Load all items from assets
     */
    suspend fun loadAllItems(): List<OrnaItem>
    
    /**
     * Search items by name
     */
    suspend fun searchItems(query: String): List<OrnaItem>
    
    /**
     * Get items by type
     */
    suspend fun getItemsByType(type: ItemType): List<OrnaItem>
    
    /**
     * Get items by tier range
     */
    suspend fun getItemsByTierRange(minTier: Int, maxTier: Int = 10): List<OrnaItem>
    
    /**
     * Get boss items only
     */
    suspend fun getBossItems(): List<OrnaItem>
    
    /**
     * Find item by ID
     */
    suspend fun getItemById(id: Int): OrnaItem?
}