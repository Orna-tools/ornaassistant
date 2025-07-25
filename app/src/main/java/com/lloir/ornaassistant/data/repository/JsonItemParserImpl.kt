package com.lloir.ornaassistant.data.repository

import android.content.Context
import android.util.Log
import com.lloir.ornaassistant.domain.assessment.ItemBaseStats
import com.lloir.ornaassistant.domain.language.LanguageManager
import com.lloir.ornaassistant.domain.repository.ItemParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ItemParser that parses JSON files containing Orna item base stats
 * Files:
 * - Armor.json
 * - head_armor.json
 * - offhand.json
 * - accessory.json
 * - weapons.json
 */
@Singleton
class JsonItemParserImpl @Inject constructor(
    private val languageManager: LanguageManager
) : ItemParser {

    companion object {
        private const val TAG = "JsonItemParser"

        // JSON file names
        private const val ARMOR_FILE = "Armor.json"
        private const val HEAD_ARMOR_FILE = "head_armor.json"
        private const val ARMOR_LEGS_FILE = "armor_legs.json"
        private const val OFFHAND_FILE = "offhand.json"
        private const val ACCESSORY_FILE = "accessory.json"
        private const val WEAPONS_FILE = "weapons.json"

        // Boss item patterns
        private val BOSS_PATTERNS = listOf(
            "arisen", "nothren", "apollyon", "world", "kingdom", "raid", "boss",
            "frostforged", "shadowforged", "crimson", "gilded", "ancient",
            "legendary", "mythic", "divine", "cursed", "blessed", "eternal",
            "void", "chaos", "primal", "elder", "greater", "supreme"
        )
    }

    /**
     * Parse all JSON files and return a map of item name to ItemBaseStats
     */
    override suspend fun parseAllItems(context: Context): Map<String, ItemBaseStats> {
        return withContext(Dispatchers.IO) {
            val allItems = mutableMapOf<String, ItemBaseStats>()

            // Parse each file
            val files = listOf(ARMOR_FILE, HEAD_ARMOR_FILE, ARMOR_LEGS_FILE, OFFHAND_FILE, ACCESSORY_FILE, WEAPONS_FILE)

            files.forEach { fileName ->
                try {
                    Log.d(TAG, "Parsing $fileName...")
                    val items = parseJsonFile(context, fileName)
                    allItems.putAll(items)
                    Log.i(TAG, "Loaded ${items.size} items from $fileName")
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing $fileName", e)
                }
            }

            Log.i(TAG, "Total items loaded: ${allItems.size}")
            allItems
        }
    }

    /**
     * Parse a single JSON file
     */
    private fun parseJsonFile(context: Context, fileName: String): Map<String, ItemBaseStats> {
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
     * - id: String (item name)
     * - tier: Int
     * - stats: Object with hp, attack, defense, etc.
     */
    private fun parseJsonItem(json: JSONObject): ItemBaseStats? {
        try {
            // Get item name from id field
            val name = json.optString("id", "").trim()
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

            // Determine if boss item
            val isBossItem = json.optBoolean("boss", detectBossItem(name))

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

    /**
     * Detect if an item is a boss item based on name patterns
     */
    override fun detectBossItem(itemName: String): Boolean {
        val lowerName = itemName.lowercase()
        return BOSS_PATTERNS.any { pattern ->
            lowerName.contains(pattern)
        }
    }

    /**
     * Parse items from language-specific files in internal storage
     */
    override suspend fun parseLanguageSpecificItems(context: Context, language: String): Map<String, ItemBaseStats> {
        val allItems = mutableMapOf<String, ItemBaseStats>()
        val databaseDir = File(context.filesDir, "databases")

        if (!databaseDir.exists()) {
            Log.w(TAG, "Database directory doesn't exist")
            return emptyMap()
        }

        // Get all files for the specified language
        val languageFiles = databaseDir.listFiles { file: File -> 
            file.name.startsWith("${language}_") && file.name.endsWith(".json") 
        }

        if (languageFiles.isNullOrEmpty()) {
            Log.w(TAG, "No files found for language: $language")
            return emptyMap()
        }

        languageFiles.forEach { file: File ->
            try {
                Log.d(TAG, "Parsing ${file.name}...")
                val jsonString = file.readText()
                val jsonArray = JSONArray(jsonString)

                for (i in 0 until jsonArray.length()) {
                    val jsonItem = jsonArray.getJSONObject(i)
                    val itemStats = parseJsonItem(jsonItem)

                    if (itemStats != null) {
                        allItems[itemStats.name] = itemStats
                    }
                }

                Log.i(TAG, "Loaded ${allItems.size} items from ${file.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing ${file.name}", e)
            }
        }

        return allItems
    }
}
