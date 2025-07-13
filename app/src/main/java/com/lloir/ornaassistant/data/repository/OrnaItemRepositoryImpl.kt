package com.lloir.ornaassistant.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.lloir.ornaassistant.domain.model.ItemStats
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
class OrnaItemRepositoryImpl @Inject constructor(
    private val context: Context
) : com.lloir.ornaassistant.domain.repository.OrnaItemRepository {
    companion object {
        private const val TAG = "OrnaItemRepository"
        private const val WEAPONS_FILE = "weapons.json"
        private const val ARMOR_FILE = "Armor.json"
        private const val HEAD_ARMOR_FILE = "head_armor.json"
        private const val OFFHAND_FILE = "offhand.json"
        private const val ACCESSORY_FILE = "accessory.json"
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
                Log.d(TAG, "Loaded ${allItems.size} weapons")

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

    /**
     * Load weapons from JSON file
     */
    private suspend fun loadWeapons(): List<OrnaItem> = withContext(Dispatchers.IO) {
        try {
            val json = context.assets.open(WEAPONS_FILE).bufferedReader().use { it.readText() }
            Log.d(TAG, "Loaded weapons JSON, first 200 chars: ${json.take(200)}")

            val jsonArray = gson.fromJson(json, JsonArray::class.java)
            Log.d(TAG, "Parsed weapons JSON array with ${jsonArray.size()} items")

            val items = jsonArray.map { jsonElement ->
                val jsonObject = jsonElement.asJsonObject
                parseJsonObjectToOrnaItem(jsonObject, ItemType.WEAPON)
            }

            Log.d(TAG, "Successfully converted ${items.size} weapons to OrnaItem objects")
            items
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Failed to load weapons: JSON syntax error", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load weapons: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Load armor from JSON file
     */
    private suspend fun loadArmor(): List<OrnaItem> = withContext(Dispatchers.IO) {
        try {
            val json = context.assets.open(ARMOR_FILE).bufferedReader().use { it.readText() }

            val jsonArray = gson.fromJson(json, JsonArray::class.java)
            Log.d(TAG, "Parsed armor JSON array with ${jsonArray.size()} items")

            val items = jsonArray.map { jsonElement ->
                val jsonObject = jsonElement.asJsonObject
                parseJsonObjectToOrnaItem(jsonObject, ItemType.ARMOR)
            }

            Log.d(TAG, "Successfully converted ${items.size} armor items to OrnaItem objects")
            items
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Failed to load armor: JSON syntax error", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load armor: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Load other items (head, legs, offhand, accessory) from JSON files
     */
    private suspend fun loadOtherItems(): List<OrnaItem> = withContext(Dispatchers.IO) {
        val allItems = mutableListOf<OrnaItem>()

        // Load head armor
        try {
            val json = context.assets.open(HEAD_ARMOR_FILE).bufferedReader().use { it.readText() }
            val jsonArray = gson.fromJson(json, JsonArray::class.java)
            Log.d(TAG, "Parsed head armor JSON array with ${jsonArray.size()} items")

            val items = jsonArray.map { jsonElement ->
                val jsonObject = jsonElement.asJsonObject
                parseJsonObjectToOrnaItem(jsonObject, ItemType.HEAD)
            }
            allItems.addAll(items)
            Log.d(TAG, "Loaded ${items.size} head armor items")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load head armor: ${e.message}", e)
        }

        // Load offhand items
        try {
            val json = context.assets.open(OFFHAND_FILE).bufferedReader().use { it.readText() }
            val jsonArray = gson.fromJson(json, JsonArray::class.java)
            Log.d(TAG, "Parsed offhand JSON array with ${jsonArray.size()} items")

            val items = jsonArray.map { jsonElement ->
                val jsonObject = jsonElement.asJsonObject
                parseJsonObjectToOrnaItem(jsonObject, ItemType.OFF_HAND)
            }
            allItems.addAll(items)
            Log.d(TAG, "Loaded ${items.size} offhand items")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load offhand items: ${e.message}", e)
        }

        // Load accessories
        try {
            val json = context.assets.open(ACCESSORY_FILE).bufferedReader().use { it.readText() }
            val jsonArray = gson.fromJson(json, JsonArray::class.java)
            Log.d(TAG, "Parsed accessory JSON array with ${jsonArray.size()} items")

            val items = jsonArray.map { jsonElement ->
                val jsonObject = jsonElement.asJsonObject
                parseJsonObjectToOrnaItem(jsonObject, ItemType.ACCESSORY)
            }
            allItems.addAll(items)
            Log.d(TAG, "Loaded ${items.size} accessory items")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load accessory items: ${e.message}", e)
        }

        Log.d(TAG, "Successfully loaded ${allItems.size} other items")
        return@withContext allItems
    }

    /**
     * Parse a JSON object to an OrnaItem
     * Uses the "id" field as the name
     */
    private fun parseJsonObjectToOrnaItem(jsonObject: JsonObject, defaultType: ItemType): OrnaItem {
        // Get the name from the "id" field
        val name = jsonObject.get("id")?.asString ?: "Unknown Item"

        // Parse other fields with safe conversions
        val id = jsonObject.get("description")?.asString?.toIntOrNull()
        val typeStr = jsonObject.get("type")?.asString
        val type = if (typeStr != null) ItemType.fromString(typeStr) else defaultType
        val tier = jsonObject.get("tier")?.asString?.toIntOrNull()

        // Handle boss field - can be "TRUE", "FALSE", or a number
        val bossStr = jsonObject.get("boss")?.asString
        val boss = when {
            bossStr == null -> 0
            bossStr.equals("TRUE", ignoreCase = true) -> 1
            bossStr.equals("FALSE", ignoreCase = true) -> 0
            else -> bossStr.toIntOrNull() ?: 0
        }

        // Parse stats
        val stats = ItemStats(
            atk = jsonObject.get("atk")?.asString?.toIntOrNull() ?: 0,
            mag = jsonObject.get("mag")?.asString?.toIntOrNull() ?: 0,
            def = jsonObject.get("def")?.asString?.toIntOrNull() ?: 0,
            res = jsonObject.get("res")?.asString?.toIntOrNull() ?: 0,
            hp = jsonObject.get("hp")?.asString?.toIntOrNull() ?: 0,
            mana = jsonObject.get("mana")?.asString?.toIntOrNull() ?: 0,
            dex = jsonObject.get("dex")?.asString?.toIntOrNull() ?: 0,
            ward = jsonObject.get("ward")?.asString?.toIntOrNull() ?: 0,
            crit = jsonObject.get("crit")?.asString?.toIntOrNull() ?: 0
        )

        return OrnaItem(
            name = name,
            id = id,
            type = type,
            tier = tier,
            boss = boss,
            stats = stats
        )
    }
}
