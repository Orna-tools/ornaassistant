package com.lloir.ornaassistant.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents an Orna item with all its properties
 * Updated to use JSON-based data instead of CSV
 */
@Parcelize
data class OrnaItem(
    val name: String,
    val id: Int?,
    val type: ItemType,
    val tier: Int?,
    val boss: Int, // 0 = regular, 1 = boss item
    val stats: ItemStats
) : Parcelable {
    
    val isBossItem: Boolean
        get() = boss == 1
    
    val displayName: String
        get() = name
    
    val tierDisplay: String
        get() = tier?.let { "T$it" } ?: "Unknown"
    
    val typeDisplay: String
        get() = type.displayName
}

@Parcelize
data class ItemStats(
    val atk: Int = 0,
    val mag: Int = 0,
    val def: Int = 0,
    val res: Int = 0,
    val hp: Int = 0,
    val mana: Int = 0,
    val dex: Int = 0,
    val ward: Int = 0,
    val crit: Int = 0
) : Parcelable {
    
    /**
     * Convert to Map for calculator compatibility
     */
    fun toMap(): Map<String, Int> = mapOf(
        "atk" to atk,
        "mag" to mag,
        "def" to def,
        "res" to res,
        "hp" to hp,
        "mana" to mana,
        "dex" to dex,
        "ward" to ward,
        "crit" to crit
    )
    
    val hasStats: Boolean
        get() = atk > 0 || mag > 0 || def > 0 || res > 0 || hp > 0 || mana > 0 || dex > 0 || ward > 0 || crit > 0
}

enum class ItemType(val displayName: String, val jsonKey: String) {
    WEAPON("Weapon", "WEAPON"),
    ARMOR("Armor", "ARMOR"),
    HEAD("Head", "HEAD"),
    LEGS("Legs", "LEGS"),
    OFF_HAND("Off-Hand", "OFF-HAND"),
    ACCESSORY("Accessory", "ACCESSORY");
    
    companion object {
        fun fromString(value: String): ItemType {
            return values().find { it.jsonKey == value.uppercase() } ?: WEAPON
        }
    }
}

@Parcelize
data class ItemMeta(
    val type: String,
    val count: Int,
    val description: String
) : Parcelable