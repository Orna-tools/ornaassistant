package com.lloir.ornaassistant.domain.assessment

import android.content.Context
import android.util.Log
import com.lloir.ornaassistant.utils.ItemUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Parser for JSON item files containing Orna item base stats
 * Files:
 * - armor_json.json
 * - head_legs_offhand_accessory_json.json
 * - weapons_json.json
 */
class JsonItemParser(private val context: Context) {

    companion object {
        private const val TAG = "JsonItemParser"

        // JSON file names
        private const val ARMOR_FILE = "armor_json.json"
        private const val ACCESSORIES_FILE = "head_legs_offhand_accessory_json.json"
        private const val WEAPONS_FILE = "weapons_json.json"
    }

    /**
     * Parse all JSON files and return a map of item name to ItemBaseStats
     */
    fun parseAllItems(): Map<String, ItemBaseStats> {
        val allItems = mutableMapOf<String, ItemBaseStats>()

        // Parse each file
        val files = listOf(ARMOR_FILE, ACCESSORIES_FILE, WEAPONS_FILE)

        files.forEach { fileName ->
            try {
                Log.d(TAG, "Parsing $fileName...")
                val items = parseJsonFile(fileName)
                allItems.putAll(items)
                Log.i(TAG, "Loaded ${items.size} items from $fileName")
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing $fileName", e)
            }
        }

        Log.i(TAG, "Total items loaded: ${allItems.size}")
        return allItems
    }

    /**
     * Parse a single JSON file
     */
    private fun parseJsonFile(fileName: String): Map<String, ItemBaseStats> {
        val items = mutableMapOf<String, ItemBaseStats>()

        try {
            // Read the JSON file
            val inputStream = context.assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() }

            // Parse JSON array
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val jsonItem = jsonArray.getJSONObject(i)
                val itemStats = parseJsonItem(jsonItem)

                if (itemStats != null) {
                    items[itemStats.name] = itemStats

                    // Debug logging for specific items
                    if (itemStats.name.contains("Arisen", ignoreCase = true)) {
                        Log.d(TAG, "Found Arisen item: ${itemStats.name} - Stats: ${itemStats.baseStats}")
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error reading $fileName", e)
        }

        return items
    }

    /**
     * Parse a single JSON item object
     * Expected format varies by file but typically includes:
     * - name: String
     * - tier: Int
     * - stats: Object with hp, attack, defense, etc.
     */
    private fun parseJsonItem(json: JSONObject): ItemBaseStats? {
        try {
            // Get item name
            val name = json.optString("name", "").trim()
            if (name.isEmpty()) {
                Log.w(TAG, "Item with empty name, skipping")
                return null
            }

            // Get tier (might be under different keys)
            val tier = json.optInt("tier", json.optInt("level", 5))

            // Parse stats - try different possible structures
            val baseStats = mutableMapOf<String, Int>()

            // Direct stat properties
            if (json.has("attack")) baseStats["Att"] = json.optInt("attack", 0)
            if (json.has("magic")) baseStats["Mag"] = json.optInt("magic", 0)
            if (json.has("defense")) baseStats["Def"] = json.optInt("defense", 0)
            if (json.has("resistance")) baseStats["Res"] = json.optInt("resistance", 0)
            if (json.has("hp")) baseStats["HP"] = json.optInt("hp", 0)
            if (json.has("mana")) baseStats["Mana"] = json.optInt("mana", 0)
            if (json.has("dexterity")) baseStats["Dex"] = json.optInt("dexterity", 0)
            if (json.has("ward")) baseStats["Ward"] = json.optInt("ward", 0)
            if (json.has("crit")) baseStats["Crit"] = json.optInt("crit", 0)

            // Alternative: stats might be in a nested object
            if (json.has("stats")) {
                val stats = json.getJSONObject("stats")
                if (stats.has("attack")) baseStats["Att"] = stats.optInt("attack", 0)
                if (stats.has("magic")) baseStats["Mag"] = stats.optInt("magic", 0)
                if (stats.has("defense")) baseStats["Def"] = stats.optInt("defense", 0)
                if (stats.has("resistance")) baseStats["Res"] = stats.optInt("resistance", 0)
                if (stats.has("hp")) baseStats["HP"] = stats.optInt("hp", 0)
                if (stats.has("mana")) baseStats["Mana"] = stats.optInt("mana", 0)
                if (stats.has("dexterity")) baseStats["Dex"] = stats.optInt("dexterity", 0)
                if (stats.has("ward")) baseStats["Ward"] = stats.optInt("ward", 0)
                if (stats.has("crit")) baseStats["Crit"] = stats.optInt("crit", 0)
            }

            // Alternative stat names (sometimes used in JSON)
            if (json.has("att")) baseStats["Att"] = json.optInt("att", 0)
            if (json.has("mag")) baseStats["Mag"] = json.optInt("mag", 0)
            if (json.has("def")) baseStats["Def"] = json.optInt("def", 0)
            if (json.has("res")) baseStats["Res"] = json.optInt("res", 0)

            // Remove zero stats
            baseStats.entries.removeIf { it.value == 0 }

            // Determine if boss item - use the explicit boss flag from JSON if available,
            // otherwise use the standardized boss item detection from ItemUtils
            val isBossItem = json.optBoolean("boss", ItemUtils.detectBossItem(name))

            return ItemBaseStats(
                name = name,
                isBossItem = isBossItem,
                baseStats = baseStats.toMap(),
                tier = tier
            )

        } catch (e: Exception) {
            Log.w(TAG, "Error parsing item: ${e.message}")
            return null
        }
    }
}
