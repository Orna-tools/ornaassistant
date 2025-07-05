package com.lloir.ornaassistant.domain.assessment

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.regex.Pattern

/**
 * Parser for baseitem.txt file containing all Orna item base stats
 * Format: <option value="ID" data-attack="X" data-defense="Y" ...>Item Name</option>
 */
class BaseItemParser(private val context: Context) {

    companion object {
        private const val TAG = "BaseItemParser"
        private const val BASEITEM_FILE = "baseitem.txt"

        // Regex pattern to parse the HTML option elements
        private val ITEM_PATTERN = Pattern.compile(
            """<option value="(\d+)" data-attack="(-?\d+)" data-defense="(-?\d+)" data-magic="(-?\d+)" data-resistance="(-?\d+)" data-hp="(-?\d+)" data-mana="(-?\d+)" data-dexterity="(-?\d+)" data-ward="(-?\d+)" data-crit="(-?\d+)">([^<]+)</option>"""
        )

        // Boss item patterns - items that use 12.5% growth instead of 10%
        private val BOSS_PATTERNS = listOf(
            "arisen", "nothren", "apollyon", "world", "kingdom", "raid", "boss",
            "frostforged", "shadowforged", "crimson", "gilded", "ancient",
            "legendary", "mythic", "divine", "cursed", "blessed", "eternal",
            "void", "chaos", "primal", "elder", "greater", "supreme"
        )

        // Tier estimation based on item ID ranges (approximate)
        private val TIER_RANGES = listOf(
            1..100 to 1,      // Tier 1
            101..200 to 2,    // Tier 2  
            201..300 to 3,    // Tier 3
            301..500 to 4,    // Tier 4
            501..800 to 5,    // Tier 5
            801..1200 to 6,   // Tier 6
            1201..1600 to 7,  // Tier 7
            1601..2000 to 8,  // Tier 8
            2001..2400 to 9,  // Tier 9
            2401..3000 to 10  // Tier 10
        )
    }

    /**
     * Parse the baseitem.txt file and return a map of item name to ItemBaseStats
     */
    fun parseBaseItems(): Map<String, ItemBaseStats> {
        val items = mutableMapOf<String, ItemBaseStats>()

        try {
            val inputStream = context.assets.open(BASEITEM_FILE)
            val reader = BufferedReader(InputStreamReader(inputStream))

            var line: String?
            var lineCount = 0
            var parsedCount = 0

            while (reader.readLine().also { line = it } != null) {
                lineCount++
                line?.let { currentLine ->
                    parseItemLine(currentLine)?.let { itemStats ->
                        items[itemStats.name] = itemStats
                        parsedCount++

                        if (parsedCount % 100 == 0) {
                            Log.d(TAG, "Parsed $parsedCount items... (target: 2168)")
                        }
                    }
                }
            }

            reader.close()
            Log.i(TAG, "Successfully parsed $parsedCount items from $lineCount lines (expected: 2168)")

            if (parsedCount < 2000) {
                Log.w(TAG, "⚠️ Parsed fewer items than expected! Check baseitem.txt format")
            } else if (parsedCount >= 2100) {
                Log.i(TAG, "✅ Item database loaded successfully")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing baseitem.txt file", e)
            return createFallbackDatabase()
        }

        return items
    }

    /**
     * Parse a single line from the baseitem.txt file
     */
    private fun parseItemLine(line: String): ItemBaseStats? {
        val matcher = ITEM_PATTERN.matcher(line.trim())

        if (!matcher.find()) {
            return null
        }

        try {
            val id = matcher.group(1)?.toIntOrNull() ?: return null
            val attack = matcher.group(2)?.toIntOrNull() ?: 0
            val defense = matcher.group(3)?.toIntOrNull() ?: 0
            val magic = matcher.group(4)?.toIntOrNull() ?: 0
            val resistance = matcher.group(5)?.toIntOrNull() ?: 0
            val hp = matcher.group(6)?.toIntOrNull() ?: 0
            val mana = matcher.group(7)?.toIntOrNull() ?: 0
            val dexterity = matcher.group(8)?.toIntOrNull() ?: 0
            val ward = matcher.group(9)?.toIntOrNull() ?: 0
            val crit = matcher.group(10)?.toIntOrNull() ?: 0
            val name = matcher.group(11)?.trim() ?: return null

            // Build stats map (only include non-zero stats)
            val baseStats = mutableMapOf<String, Int>()
            if (attack != 0) baseStats["Att"] = attack
            if (defense != 0) baseStats["Def"] = defense
            if (magic != 0) baseStats["Mag"] = magic
            if (resistance != 0) baseStats["Res"] = resistance
            if (hp != 0) baseStats["HP"] = hp
            if (mana != 0) baseStats["Mana"] = mana
            if (dexterity != 0) baseStats["Dex"] = dexterity
            if (ward != 0) baseStats["Ward"] = ward
            if (crit != 0) baseStats["Crit"] = crit

            // Determine if it's a boss item
            val isBossItem = detectBossItem(name)

            // Estimate tier from ID
            val tier = estimateTierFromId(id)

            return ItemBaseStats(
                name = name,
                isBossItem = isBossItem,
                baseStats = baseStats,
                tier = tier
            )

        } catch (e: Exception) {
            Log.w(TAG, "Error parsing line: $line", e)
            return null
        }
    }

    /**
     * Detect if an item is a boss item based on name patterns
     */
    private fun detectBossItem(itemName: String): Boolean {
        val lowerName = itemName.lowercase()
        return BOSS_PATTERNS.any { pattern ->
            lowerName.contains(pattern)
        }
    }

    /**
     * Estimate tier based on item ID ranges
     */
    private fun estimateTierFromId(id: Int): Int {
        for ((range, tier) in TIER_RANGES) {
            if (id in range) {
                return tier
            }
        }
        // For very high IDs, assume tier 10
        return if (id > 3000) 10 else 5
    }

    /**
     * Create a minimal fallback database if parsing fails
     */
    private fun createFallbackDatabase(): Map<String, ItemBaseStats> {
        return mapOf(
            "Adamantine Staff" to ItemBaseStats(
                name = "Adamantine Staff",
                isBossItem = false,
                baseStats = mapOf("Att" to 30, "Mag" to 105),
                tier = 5
            ),
            "Arisen Ankh" to ItemBaseStats(
                name = "Arisen Ankh",
                isBossItem = true,
                baseStats = mapOf("Att" to 150, "Mag" to 150, "Def" to 50, "Res" to 50),
                tier = 10
            )
        )
    }
}

/**
 * Enhanced ItemDatabase that loads from baseitem.txt
 */
object EnhancedItemDatabase {
    private var itemsCache: Map<String, ItemBaseStats>? = null
    private var isLoaded = false

    /**
     * Initialize the database from baseitem.txt
     */
    fun initialize(context: Context): Boolean {
        if (isLoaded) return true

        Log.d("EnhancedItemDatabase", "Loading item database from baseitem.txt...")
        val parser = BaseItemParser(context)
        itemsCache = parser.parseBaseItems()
        isLoaded = true
        val itemCount = itemsCache?.size ?: 0
        Log.i("EnhancedItemDatabase", "Loaded $itemCount items into database")

        return if (itemCount >= 2000) {
            Log.i("EnhancedItemDatabase", "✅ Database initialization successful")
            true
        } else {
            Log.w("EnhancedItemDatabase", "⚠️ Database initialization incomplete - only $itemCount items loaded")
            false
        }
    }

    /**
     * Find item by exact name match
     */
    fun findItemByName(itemName: String): ItemBaseStats? {
        return itemsCache?.get(itemName)
    }

    /**
     * Find item by partial name match with fuzzy search
     */
    fun findItemByPartialName(itemName: String): ItemBaseStats? {
        val cache = itemsCache ?: return null

        // Clean the input name (remove quality prefixes, enchantments)
        val cleanName = cleanItemName(itemName)

        // Try exact match first
        cache[cleanName]?.let { return it }

        // Try case-insensitive exact match
        cache.entries.find { it.key.equals(cleanName, ignoreCase = true) }?.value?.let { return it }

        // Try partial matches (item name contains search term or vice versa)
        return cache.values.find { item ->
            cleanName.contains(item.name, ignoreCase = true) ||
            item.name.contains(cleanName, ignoreCase = true)
        }
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
    fun getItemsByTier(tier: Int): List<ItemBaseStats> {
        return itemsCache?.values?.filter { it.tier == tier } ?: emptyList()
    }

    /**
     * Get all boss items
     */
    fun getBossItems(): List<ItemBaseStats> {
        return itemsCache?.values?.filter { it.isBossItem } ?: emptyList()
    }

    /**
     * Get database statistics
     */
    fun getStats(): DatabaseStats {
        val cache = itemsCache ?: return DatabaseStats(0, 0, 0)
        return DatabaseStats(
            totalItems = cache.size,
            bossItems = cache.values.count { it.isBossItem },
            tiersRepresented = cache.values.map { it.tier }.distinct().size
        )
    }
}

data class DatabaseStats(
    val totalItems: Int,
    val bossItems: Int,
    val tiersRepresented: Int
)
