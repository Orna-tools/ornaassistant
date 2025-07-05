package com.lloir.ornaassistant.domain.assessment

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Parser for baseitem.csv file containing all Orna item base stats
 * Format: Name,Attack,Defense,Magic,Resistance,HP,Mana,Dexterity,Ward,Crit,Boss
 */
class BaseItemParser(private val context: Context) {

    companion object {
        private const val TAG = "BaseItemParser"
        private const val BASEITEM_FILE = "baseitem.csv"

        // Boss item patterns - items that use 12.5% growth instead of 10%
        private val BOSS_PATTERNS = listOf(
            "arisen", "nothren", "apollyon", "world", "kingdom", "raid", "boss",
            "frostforged", "shadowforged", "crimson", "gilded", "ancient",
            "legendary", "mythic", "divine", "cursed", "blessed", "eternal",
            "void", "chaos", "primal", "elder", "greater", "supreme"
        )
    }

    /**
     * Parse the baseitem.csv file and return a map of item name to ItemBaseStats
     */
    fun parseBaseItems(): Map<String, ItemBaseStats> {
        val items = mutableMapOf<String, ItemBaseStats>()

        try {
            val inputStream = context.assets.open(BASEITEM_FILE)
            val reader = BufferedReader(InputStreamReader(inputStream))

            var isFirstLine = true
            var line: String?
            var lineCount = 0
            var parsedCount = 0
            var duplicateCount = 0

            while (reader.readLine().also { line = it } != null) {
                lineCount++
                line?.let { currentLine ->
                    // Skip CSV header line
                    if (isFirstLine) {
                        isFirstLine = false
                        return@let
                    }

                    parseCsvLine(currentLine)?.let { itemStats ->
                        // Check for duplicates
                        if (items.containsKey(itemStats.name)) {
                            duplicateCount++
                            Log.w(TAG, "🔄 DUPLICATE #$duplicateCount: ${itemStats.name} (keeping newer version)")

                            // Special logging for Arisen items
                            if (itemStats.name.contains("Arisen", ignoreCase = true)) {
                                Log.w(TAG, "⚠️ ARISEN DUPLICATE: ${itemStats.name}")
                            }
                        }

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
            Log.i(TAG, "Found $duplicateCount duplicate item names")
            Log.i(TAG, "Final database contains ${items.size} unique items")

            if (parsedCount < 2000) {
                Log.w(TAG, "⚠️ Parsed fewer items than expected! Check baseitem.csv format")
            } else if (parsedCount >= 2100) {
                Log.i(TAG, "✅ Item database loaded successfully")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing baseitem.csv file", e)
            return createFallbackDatabase()
        }

        return items
    }

    /**
     * Parse a single CSV line
     * Format: Name,Attack,Defense,Magic,Resistance,HP,Mana,Dexterity,Ward,Crit
     */
    private fun parseCsvLine(line: String): ItemBaseStats? {
        val trimmedLine = line.trim()
        if (trimmedLine.isEmpty()) {
            return null
        }

        val parts = trimmedLine.split(",")
        if (parts.size < 10) {
            Log.w(TAG, "❌ REJECTED: Invalid CSV line format (${parts.size} columns): $trimmedLine")
            return null
        }

        try {
            val name = parts[0].trim()
            val attack = parts[1].trim().toIntOrNull() ?: 0
            val defense = parts[2].trim().toIntOrNull() ?: 0
            val magic = parts[3].trim().toIntOrNull() ?: 0
            val resistance = parts[4].trim().toIntOrNull() ?: 0
            val hp = parts[5].trim().toIntOrNull() ?: 0
            val mana = parts[6].trim().toIntOrNull() ?: 0
            val dexterity = parts[7].trim().toIntOrNull() ?: 0
            val ward = parts[8].trim().toIntOrNull() ?: 0
            val crit = parts[9].trim().toIntOrNull() ?: 0

            // Check if boss column exists and use it
            val bossValue = if (parts.size > 10) parts[10].trim().toIntOrNull() else null

            // Validate name
            if (name.isEmpty() || name.length < 2) {
                Log.w(TAG, "❌ REJECTED: Invalid item name: '$name'")
                return null
            }

            // Debug logging for Arisen Nagamaki specifically
            if (name.contains("Arisen Nagamaki", ignoreCase = true)) {
                Log.d(TAG, "🔍 Processing: $name -> Att=$attack, Dex=$dexterity, Ward=$ward, Crit=$crit")
            }

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

            // Determine if it's a boss item based on the boss column or fallback to name detection
            val isBossItem = when (bossValue) {
                1 -> true
                -1 -> false
                0 -> false
                null -> detectBossItem(name)
                else -> detectBossItem(name)
            }

            // Estimate tier from stats and item type
            val tier = estimateTierFromStats(baseStats, isBossItem)

            val result = ItemBaseStats(
                name = name,
                isBossItem = isBossItem,
                baseStats = baseStats,
                tier = tier
            )

            // Debug logging for successful creation
            if (name.contains("Arisen Nagamaki", ignoreCase = true)) {
                Log.d(TAG, "✅ Created ItemBaseStats for: $name (boss=$isBossItem, tier=$tier)")
                Log.d(TAG, "✅ Base stats: $baseStats")
            }

            return result

        } catch (e: Exception) {
            Log.w(TAG, "❌ REJECTED: Parse error for line: $trimmedLine", e)
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
     * Estimate tier based on stats and item type
     */
    private fun estimateTierFromStats(baseStats: Map<String, Int>, isBossItem: Boolean): Int {
        // Get the highest stat value to estimate tier
        val maxStat = baseStats.values.maxOrNull() ?: 0

        // Rough tier estimation based on stat ranges
        val estimatedTier = when {
            maxStat < 20 -> 1
            maxStat < 40 -> 2
            maxStat < 60 -> 3
            maxStat < 80 -> 4
            maxStat < 120 -> 5
            maxStat < 160 -> 6
            maxStat < 200 -> 7
            maxStat < 250 -> 8
            maxStat < 300 -> 9
            else -> 10
        }

        // Boss items tend to be higher tier
        return if (isBossItem && estimatedTier < 8) {
            estimatedTier + 2
        } else {
            estimatedTier
        }
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
 * Enhanced ItemDatabase that loads from baseitem.csv
 */
object EnhancedItemDatabase {
    private var itemsCache: Map<String, ItemBaseStats>? = null
    private var isLoaded = false

    /**
     * Initialize the database from baseitem.csv
     */
    fun initialize(context: Context): Boolean {
        if (isLoaded) return true

        Log.d("EnhancedItemDatabase", "Loading item database from baseitem.csv...")
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
