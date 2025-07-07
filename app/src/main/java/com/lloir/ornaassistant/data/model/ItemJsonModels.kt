package com.lloir.ornaassistant.data.model

import com.google.gson.annotations.SerializedName
import com.lloir.ornaassistant.domain.model.ItemMeta
import com.lloir.ornaassistant.domain.model.OrnaItem
import com.lloir.ornaassistant.domain.model.ItemStats
import com.lloir.ornaassistant.domain.model.ItemType

/**
 * JSON response models for loading items from assets
 */
data class WeaponsResponse(
    val meta: ItemMeta,
    val weapons: List<ItemJson>
)

data class ArmorResponse(
    val meta: ItemMeta,
    val armor: List<ItemJson>
)

data class MixedItemsResponse(
    val meta: ItemMeta,
    @SerializedName("head_items") val headItems: List<ItemJson>,
    @SerializedName("legs_items") val legsItems: List<ItemJson>,
    @SerializedName("offhand_items") val offhandItems: List<ItemJson>,
    @SerializedName("accessory_items") val accessoryItems: List<ItemJson>
)

/**
 * JSON representation of an item
 */
data class ItemJson(
    val name: String,
    val id: Int?,
    val type: String,
    val tier: Int?,
    val boss: Int,
    val stats: ItemStatsJson
) {
    fun toDomainModel(): OrnaItem {
        return OrnaItem(
            name = name,
            id = id,
            type = ItemType.fromString(type),
            tier = tier,
            boss = boss,
            stats = stats.toDomainModel()
        )
    }
}

data class ItemStatsJson(
    val atk: Int = 0,
    val mag: Int = 0,
    val def: Int = 0,
    val res: Int = 0,
    val hp: Int = 0,
    val mana: Int = 0,
    val dex: Int = 0,
    val ward: Int = 0,
    val crit: Int = 0
) {
    fun toDomainModel(): ItemStats {
        return ItemStats(
            atk = atk,
            mag = mag,
            def = def,
            res = res,
            hp = hp,
            mana = mana,
            dex = dex,
            ward = ward,
            crit = crit
        )
    }
}

/**
 * Legacy JSON format for items that come as flat arrays
 * This handles the format from the original Orna Guide API
 */
data class LegacyItemJson(
    @SerializedName("") val name: String?,
    @SerializedName("name") val nameAlternate: String?, // fallback
    val id: String?,
    val description: String?,
    val type: String?,
    val tier: String?,
    val boss: String?,
    val arena: String?,
    val image: String?,
    val atk: String?,
    val mag: String?,
    val def: String?,
    val res: String?,
    val hp: String?,
    val mana: String?,
    val dex: String?,
    val ward: String?,
    val crit: String?
) {
    fun toDomainModel(): OrnaItem {
        return OrnaItem(
            // Use whichever name field is available, with fallback
            name = (name ?: nameAlternate ?: "Unknown Item").trim(),
            id = id?.toIntOrNull(),
            type = ItemType.fromString(type ?: "WEAPON"),
            tier = tier?.toIntOrNull(),
            boss = boss?.toIntOrNull() ?: 0,
            stats = ItemStats(
                atk = atk?.toIntOrNull() ?: 0,
                mag = mag?.toIntOrNull() ?: 0,
                def = def?.toIntOrNull() ?: 0,
                res = res?.toIntOrNull() ?: 0,
                hp = hp?.toIntOrNull() ?: 0,
                mana = mana?.toIntOrNull() ?: 0,
                dex = dex?.toIntOrNull() ?: 0,
                ward = ward?.toIntOrNull() ?: 0,
                crit = crit?.toIntOrNull() ?: 0
            )
        )
    }
}