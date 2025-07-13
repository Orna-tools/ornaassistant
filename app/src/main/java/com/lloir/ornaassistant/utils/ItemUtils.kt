package com.lloir.ornaassistant.utils

/**
 * Utility functions for item-related operations
 * 
 * This class centralizes common functionality used across different components
 * that deal with items, such as detecting boss items, cleaning item names, etc.
 */
object ItemUtils {
    
    /**
     * Comprehensive list of patterns that indicate an item is a boss item
     */
    private val BOSS_ITEM_PATTERNS = listOf(
        "arisen", "nothren", "apollyon", "world", "kingdom", "raid", "boss",
        "frostforged", "shadowforged", "crimson", "gilded", "ancient",
        "legendary", "mythic", "divine", "cursed", "blessed", "eternal",
        "void", "chaos", "primal", "elder", "greater", "supreme"
    )
    
    /**
     * Detect if an item is a boss item based on name patterns
     * 
     * @param itemName The name of the item to check
     * @return true if the item is likely a boss item, false otherwise
     */
    fun detectBossItem(itemName: String): Boolean {
        val lowerName = itemName.lowercase()
        return BOSS_ITEM_PATTERNS.any { pattern ->
            lowerName.contains(pattern)
        }
    }
    
    /**
     * Item quality prefixes in order of quality
     */
    val ITEM_QUALITY_PREFIXES = listOf(
        "Broken", "Poor", "Common", "Superior", "Famed", "Legendary", "Ornate"
    )
    
    /**
     * Special upgrade levels for items
     */
    val UPGRADE_LEVELS = mapOf(
        "1" to 1, "2" to 2, "3" to 3, "4" to 4, "5" to 5,
        "6" to 6, "7" to 7, "8" to 8, "9" to 9, "10" to 10,
        "MF" to 11, "DF" to 12, "GF" to 13
    )
    
    /**
     * Quality bonuses for special upgrades
     */
    val QUALITY_BONUSES = mapOf(
        "MF" to 0.01, "DF" to 0.02, "GF" to 0.03
    )
    
    /**
     * Rarity multipliers for different item rarities
     */
    val RARITY_MULTIPLIERS = mapOf(
        "Broken" to 0.5,
        "Poor" to 0.75,
        "Common" to 1.0,
        "Superior" to 1.25,
        "Famed" to 1.5,
        "Legendary" to 1.75,
        "Ornate" to 2.0
    )
}