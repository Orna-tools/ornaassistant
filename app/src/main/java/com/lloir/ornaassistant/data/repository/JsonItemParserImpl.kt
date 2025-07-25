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
 * File:
 * - items.json (single file containing all items)
 */
@Singleton
class JsonItemParserImpl @Inject constructor(
    private val languageManager: LanguageManager
) : ItemParser {

    companion object {
        private const val TAG = "JsonItemParser"

        // JSON file name - all items are now in a single file
        private const val ITEMS_FILE = "items.json"

        // Deprecated file names (kept for reference)
        // private const val ARMOR_FILE = "Armor.json"
        // private const val HEAD_ARMOR_FILE = "head_armor.json"
        // private const val ARMOR_LEGS_FILE = "armor_legs.json"
        // private const val OFFHAND_FILE = "offhand.json"
        // private const val ACCESSORY_FILE = "accessory.json"
        // private const val WEAPONS_FILE = "weapons.json"

        // Boss item patterns by language
        private val BOSS_PATTERNS_BY_LANGUAGE = mapOf(
            "en" to listOf(
                "arisen", "nothren", "apollyon", "world", "kingdom", "raid", "boss",
                "frostforged", "shadowforged", "crimson", "gilded", "ancient",
                "legendary", "mythic", "divine", "cursed", "blessed", "eternal",
                "void", "chaos", "primal", "elder", "greater", "supreme"
            ),
            "es" to listOf(
                "elevado", "nothren", "apollyon", "mundo", "reino", "incursión", "jefe",
                "forjado de hielo", "forjado de sombra", "carmesí", "dorado", "antiguo",
                "legendario", "mítico", "divino", "maldito", "bendito", "eterno",
                "vacío", "caos", "primordial", "anciano", "mayor", "supremo"
            ),
            "fr" to listOf(
                "élevé", "nothren", "apollyon", "monde", "royaume", "raid", "boss",
                "forgé de glace", "forgé d'ombre", "cramoisi", "doré", "ancien",
                "légendaire", "mythique", "divin", "maudit", "béni", "éternel",
                "vide", "chaos", "primordial", "ancien", "supérieur", "suprême"
            ),
            "de" to listOf(
                "erhoben", "nothren", "apollyon", "welt", "königreich", "raid", "boss",
                "frostgeschmiedet", "schattengeschmiedet", "purpur", "vergoldet", "uralt",
                "legendär", "mythisch", "göttlich", "verflucht", "gesegnet", "ewig",
                "leere", "chaos", "urzeitlich", "älterer", "größer", "höchst"
            ),
            "it" to listOf(
                "asceso", "nothren", "apollyon", "mondo", "regno", "incursione", "boss",
                "forgiato dal gelo", "forgiato dall'ombra", "cremisi", "dorato", "antico",
                "leggendario", "mitico", "divino", "maledetto", "benedetto", "eterno",
                "vuoto", "caos", "primordiale", "anziano", "maggiore", "supremo"
            )
        )

        // Default to English if language not supported
        private fun getBossPatterns(language: String): List<String> {
            return BOSS_PATTERNS_BY_LANGUAGE[language] ?: BOSS_PATTERNS_BY_LANGUAGE["en"] ?: emptyList()
        }
    }

    /**
     * Parse all JSON files and return a map of item name to ItemBaseStats
     */
    override suspend fun parseAllItems(context: Context): Map<String, ItemBaseStats> {
        return withContext(Dispatchers.IO) {
            val allItems = mutableMapOf<String, ItemBaseStats>()

            try {
                Log.d(TAG, "Parsing $ITEMS_FILE...")
                val items = parseJsonFile(context, ITEMS_FILE)
                allItems.putAll(items)
                Log.i(TAG, "Loaded ${items.size} items from $ITEMS_FILE")
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing $ITEMS_FILE", e)
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
     * New format:
     * {
     *   "name": "Item Name",
     *   "stats": [
     *     ["Stat Name", "Stat Value"],
     *     ...
     *   ]
     * }
     */
    private fun parseJsonItem(json: JSONObject): ItemBaseStats? {
        try {
            // Get item name directly from name field
            val name = json.optString("name", "").trim()
            if (name.isEmpty()) {
                Log.w(TAG, "Item with empty name, skipping")
                return null
            }

            // Get tier (might be under different keys)
            val tier = json.optInt("tier", json.optInt("level", 5))

            // Parse stats from the new format
            val baseStats = mutableMapOf<String, Int>()

            // Check if stats array exists
            if (json.has("stats")) {
                val statsArray = json.getJSONArray("stats")

                for (i in 0 until statsArray.length()) {
                    val statPair = statsArray.getJSONArray(i)
                    if (statPair.length() >= 2) {
                        val statName = statPair.getString(0)
                        val statValueStr = statPair.getString(1)

                        // Convert stat value to integer, handling percentage and plus signs
                        val statValue = parseStatValue(statValueStr)

                        // Map stat names to standardized format
                        val mappedStatName = mapStatName(statName)
                        if (mappedStatName.isNotEmpty() && statValue > 0) {
                            baseStats[mappedStatName] = statValue
                        }
                    }
                }
            }

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
     * Parse stat value from string, handling percentage and plus signs
     */
    private fun parseStatValue(valueStr: String): Int {
        return try {
            // Remove % and + characters
            val cleanValue = valueStr.replace("%", "").replace("+", "").trim()
            cleanValue.toInt()
        } catch (e: NumberFormatException) {
            0 // Default to 0 if parsing fails
        }
    }

    /**
     * Map stat names to standardized format
     */
    private fun mapStatName(statName: String): String {
        return when (statName.trim().lowercase()) {
            "hp" -> "HP"
            "attack" -> "Att"
            "magic" -> "Mag"
            "defense" -> "Def"
            "resistance" -> "Res"
            "mana" -> "Mana"
            "dexterity" -> "Dex"
            "ward" -> "Ward"
            "crit" -> "Crit"
            else -> statName // Keep original if no mapping
        }
    }

    /**
     * Detect if an item is a boss item based on name patterns
     */
    override fun detectBossItem(itemName: String): Boolean {
        val lowerName = itemName.lowercase()
        val currentLanguage = languageManager.getDatabaseLanguage()
        return getBossPatterns(currentLanguage).any { pattern ->
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

        // Look for the items.json file for the specified language
        val itemsFile = File(databaseDir, "${language}_$ITEMS_FILE")

        if (!itemsFile.exists()) {
            Log.w(TAG, "Items file not found for language: $language")
            return emptyMap()
        }

        try {
            Log.d(TAG, "Parsing ${itemsFile.name}...")
            val jsonString = itemsFile.readText()
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val jsonItem = jsonArray.getJSONObject(i)
                val itemStats = parseJsonItem(jsonItem)

                if (itemStats != null) {
                    allItems[itemStats.name] = itemStats
                }
            }

            Log.i(TAG, "Loaded ${allItems.size} items from ${itemsFile.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing ${itemsFile.name}", e)
        }

        return allItems
    }
}
