package com.lloir.ornaassistant

import android.content.Context
import android.view.ViewGroup
import android.view.WindowManager
import com.lloir.ornaassistant.ornaviews.*
import com.lloir.ornaassistant.settings.Settings

object OrnaViewFactory {
    private val bannedNames = listOf("TIER", "RARITY", "ACQUIRED", "LVL", "INVENTORY")

    fun create(
        type: OrnaViewType?,
        data: ArrayList<ScreenData>,
        wm: WindowManager,
        ctx: Context
    ): OrnaView? {
        return when (type) {
            OrnaViewType.ITEM -> OrnaViewItem(data, wm, ctx)
            OrnaViewType.WAYVESSEL -> OrnaViewWayvessel(data, wm, ctx)
            OrnaViewType.NOTIFICATIONS -> OrnaViewNotifications(data, wm, ctx)
            OrnaViewType.DUNGEON_ENTRY -> OrnaViewDungeonEntry(data, wm, ctx)
            OrnaViewType.INVENTORY -> OrnaViewInventory(data, wm, ctx)
            null -> null
            OrnaViewType.KINGDOM_GAUNTLET -> TODO()
        }
    }

    fun getType(data: ArrayList<ScreenData>): OrnaViewType? {
        val itemNames = data.map { it.name }

        val likelyValidItems = itemNames.filter { name ->
            val cleanedName = name.trim().uppercase()

            if (bannedNames.contains(cleanedName)) {
                if (Settings.isDebugEnabled) {
                    android.util.Log.d("OrnaViewFilter", "❌ Skipping banned name: $cleanedName")
                }
                return@filter false
            }

            if (name.length <= 2) {
                if (Settings.isDebugEnabled) {
                    android.util.Log.d("OrnaViewFilter", "❌ Skipping short name: $name")
                }
                return@filter false
            }

            if (name == name.uppercase() || name.firstOrNull()?.isUpperCase() == false) {
                if (Settings.isDebugEnabled) {
                    android.util.Log.d("OrnaViewFilter", "❌ Skipping due to suspicious casing: $name")
                }
                return@filter false
            }

            true
        }

        if (likelyValidItems.isEmpty()) {
            if (Settings.isDebugEnabled) {
                android.util.Log.d("OrnaViewFilter", "❌ No valid item names found in screen data")
            }
        }

        return when {
            data.any { it.name == "ACQUIRED" } -> OrnaViewType.ITEM
            data.any { it.name == "New" } -> OrnaViewType.INVENTORY
            data.any { it.name == "Notifications" } -> OrnaViewType.NOTIFICATIONS
            data.any { it.name.lowercase().contains("this wayvessel is active") } -> OrnaViewType.WAYVESSEL
            data.any { it.name.lowercase().contains("special dungeon") } -> OrnaViewType.DUNGEON_ENTRY
            data.any { it.name.lowercase().contains("world dungeon") } -> OrnaViewType.DUNGEON_ENTRY
            data.any { it.name.startsWith("Battle a series of opponents") } -> OrnaViewType.DUNGEON_ENTRY
            else -> null
        }
    }
}

enum class OrnaViewType {
    INVENTORY, ITEM, WAYVESSEL, NOTIFICATIONS, DUNGEON_ENTRY, KINGDOM_GAUNTLET
}

enum class OrnaViewUpdateType {
    NOTIFICATIONS_INVITERS,

    DUNGEON_MODE_CHANGED, DUNGEON_NEW_DUNGEON, DUNGEON_ENTERED, DUNGEON_GODFORGE,
    DUNGEON_DONE, DUNGEON_FAIL, DUNGEON_NEW_FLOOR, DUNGEON_EXPERIENCE, DUNGEON_ORNS, DUNGEON_GOLD,

    ITEM_ASSESS_RESULTS,
    KINGDOM_GAUNTLET_LIST
}

abstract class OrnaView(val type: OrnaViewType, val wm: WindowManager, val ctx: Context) {
    var mLayout: ViewGroup? = null

    fun close() {
        mLayout?.let { wm.removeView(it) }
    }

    open fun drawOverlay() {}

    open fun update(
        data: ArrayList<ScreenData>,
        updateResults: (MutableMap<OrnaViewUpdateType, Any?>) -> Unit
    ): Boolean {
        return false
    }
}

fun String.startsWithUppercaseLetter(): Boolean {
    return this.matches(Regex("[A-Z]{1}.*"))
}