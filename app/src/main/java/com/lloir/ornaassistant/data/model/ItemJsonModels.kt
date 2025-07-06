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