package com.lloir.ornaassistant.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.lloir.ornaassistant.data.model.ArmorResponse
import com.lloir.ornaassistant.data.model.MixedItemsResponse
import com.lloir.ornaassistant.data.model.WeaponsResponse
import com.lloir.ornaassistant.domain.model.ItemType
import com.lloir.ornaassistant.domain.model.OrnaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrnaItemRepository @Inject constructor(
    private val context: Context
) : com.lloir.ornaassistant.domain.repository.OrnaItemRepository {
    companion object {
        private const val TAG = "OrnaItemRepository"
        private const val WEAPONS_FILE = "weapons_json.json"
        private const val ARMOR_FILE = "armor_json.json"
        private const val MIXED_FILE = "head_legs_offhand_accessory_json.json"
    }

    private val gson = Gson()
    private val _allItems = MutableStateFlow<List<OrnaItem>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    override val allItems: Flow<List<OrnaItem>> = _allItems.asStateFlow()
    override val isLoading: Flow<Boolean> = _isLoading.asStateFlow()
    override val error: Flow<String?> = _error.asStateFlow()

    /**
     * Load all items from JSON assets
     */
    override suspend fun loadAllItems(): List<OrnaItem> {
        if (_allItems.value.isNotEmpty()) {
            Log.d(TAG, "Items already loaded: ${_allItems.value.size}")
            return _allItems.value
        }

        return withContext(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _error.value = null

                val allItems = mutableListOf<OrnaItem>()

                // Load weapons
                Log.d(TAG, "Loading weapons...")
                allItems.addAll(loadWeapons())

                // Load armor
                Log.d(TAG, "Loading armor...")
                allItems.addAll(loadArmor())

                // Load other types
                Log.d(TAG, "Loading other item types...")
                allItems.addAll(loadOtherItems())

                Log.d(TAG, "Successfully loaded ${allItems.size} items")
                _allItems.value = allItems
                allItems

            } catch (e: Exception) {
                val errorMsg = "Failed to load items: ${e.message}"
                Log.e(TAG, errorMsg, e)
                _error.value = errorMsg
                emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Search items by name
     */
    override suspend fun searchItems(query: String): List<OrnaItem> {
        if (query.isBlank()) return _allItems.value

        return _allItems.value.filter { item ->
            item.name.contains(query, ignoreCase = true)
        }
    }

    /**
     * Get items by type
     */
    override suspend fun getItemsByType(type: ItemType): List<OrnaItem> {
        return _allItems.value.filter { it.type == type }
    }

    /**
     * Get items by tier range
     */
    override suspend fun getItemsByTierRange(minTier: Int, maxTier: Int): List<OrnaItem> {
        return _allItems.value.filter { item ->
            item.tier?.let { it >= minTier && it <= maxTier } ?: false
        }
    }

    /**
     * Get boss items only
     */
    override suspend fun getBossItems(): List<OrnaItem> {
        return _allItems.value.filter { it.isBossItem }
    }

    /**
     * Find item by ID
     */
    override suspend fun getItemById(id: Int): OrnaItem? {
        return _allItems.value.find { it.id == id }
    }

    private suspend fun loadWeapons(): List<OrnaItem> = withContext(Dispatchers.IO) {
        try {
            val json = context.assets.open(WEAPONS_FILE).bufferedReader().use { it.readText() }
            val response = gson.fromJson(json, WeaponsResponse::class.java)
            response.weapons.map { it.toDomainModel() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load weapons", e)
            emptyList()
        }
    }

    private suspend fun loadArmor(): List<OrnaItem> = withContext(Dispatchers.IO) {
        try {
            val json = context.assets.open(ARMOR_FILE).bufferedReader().use { it.readText() }
            val response = gson.fromJson(json, ArmorResponse::class.java)
            response.armor.map { it.toDomainModel() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load armor", e)
            emptyList()
        }
    }

    private suspend fun loadOtherItems(): List<OrnaItem> = withContext(Dispatchers.IO) {
        try {
            val json = context.assets.open(MIXED_FILE).bufferedReader().use { it.readText() }
            val response = gson.fromJson(json, MixedItemsResponse::class.java)

            listOf(response.headItems, response.legsItems, response.offhandItems, response.accessoryItems)
                .flatten()
                .map { it.toDomainModel() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load other items", e)
            emptyList()
        }
    }
}
