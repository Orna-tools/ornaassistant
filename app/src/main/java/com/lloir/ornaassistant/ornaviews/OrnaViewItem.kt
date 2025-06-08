// OrnaViewItem.kt
package com.lloir.ornaassistant.ornaviews

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.lloir.ornaassistant.OrnaView
import com.lloir.ornaassistant.OrnaViewType
import com.lloir.ornaassistant.OrnaViewUpdateType
import com.lloir.ornaassistant.ScreenData
import com.lloir.ornaassistant.startsWithUppercaseLetter
import com.lloir.ornaassistant.assess.assess

class OrnaViewItem(
    data: ArrayList<ScreenData>,
    wm: WindowManager,
    ctx: Context,
    private val bossScaling: Int = 0, // Added default value
    private val isCelestial: Boolean = false, // Added default value
    private val isTwoHanded: Boolean = false, // Added default value
    private val isUpgradable: Boolean = true, // Added default value
    private val isOffHand: Boolean = false // Added default value
) : OrnaView(OrnaViewType.ITEM, wm, ctx) {

    companion object {
        private const val TAG = "OrnaViewItem"
    }

    private var itemName: String? = null
    private var nameLocation: Rect? = null
    private var attributes: MutableMap<String, Int> = mutableMapOf()
    private var level: Int = 1

    override fun update(
        data: ArrayList<ScreenData>,
        updateResults: (MutableMap<OrnaViewUpdateType, Any?>) -> Unit
    ): Boolean {
        if (itemName == null) {
            val cleaned = data
                .filter { it.name.startsWithUppercaseLetter() }
                .filterNot {
                    listOf(
                        "Inventory","Knights of Inferno","Earthen Legion","FrozenGuard",
                        "Party","Arena","Codex","Runeshop","Options","Gauntlet","Character"
                    ).any { prefix -> it.name.startsWith(prefix) }
                }

            getName(cleaned)
            getAttributes(cleaned)
            assessItem(updateResults)
        }
        return false
    }

    private fun getName(data: List<ScreenData>) {
        val qualities = listOf(
            "Broken ","Poor ","Superior ","Famed ","Legendary ",
            "Ornate ","Masterforged ","Demonforged ","Godforged "
        )
        val prefixes = listOf(
            "burning","embered","fiery","flaming","infernal","scalding","warm",
            "chilling","icy","oceanic","snowy","tidal","winter","balanced","earthly",
            "grounded","natural","organic","rocky","stony","electric","shocking",
            "sparking","stormy","thunderous","angelic","bright","divine","moral",
            "pure","purifying","revered","righteous","saintly","sublime","corrupted",
            "diabolic","demonic","gloomy","impious","profane","unhallowed","wicked",
            "beastly","bestial","chimeric","dragonic","wild","colorless","customary",
            "normalized","origin","reformed","renewed","reworked"
        )

        val first = data.firstOrNull() ?: return
        var name = first.name
        nameLocation = first.rect

        if (name.contains("You are")) {
            data.getOrNull(1)?.let {
                name = it.name
                nameLocation = it.rect
            }
        }

        qualities.forEach { q ->
            if (name.startsWith(q)) name = name.removePrefix(q)
        }
        prefixes.forEach { p ->
            val cap = p.replaceFirstChar(Char::titlecase)
            if (name.startsWith("$cap ")) name = name.removePrefix("$cap ")
        }

        itemName = name
    }

    private fun getAttributes(data: List<ScreenData>) {
        var inAdornments = false
        val accepted = setOf("Att","Mag","Def","Res","Dex","Crit","Mana","Ward")

        data.forEach { node ->
            when {
                node.name.contains("ADORNMENTS") -> inAdornments = true
                node.name.startsWith("Level") -> {
                    level = node.name.removePrefix("Level ").toIntOrNull() ?: 1
                }
                else -> {
                    Regex("([A-Za-z\\s]+):\\s(-?[0-9,]+)").findAll(
                        node.name.replace("−","-").replace(" ","")
                    ).forEach { m ->
                        val (k, raw) = m.destructured
                        raw.replace(",","").toIntOrNull()?.let { v ->
                            attributes[k] = (attributes[k] ?: 0) + if (!inAdornments) v else -v
                        }
                    }
                }
            }
        }
    }

    private fun assessItem(updateResults: (MutableMap<OrnaViewUpdateType, Any?>) -> Unit) {
        val stats = attributes
            .filter { it.value > 0 }
            .mapKeys { it.key.lowercase() }
            .toMutableMap()
        stats["level"] = level

        val result = assess(
            attrs        = stats,
            level        = level,
            bossScaling  = bossScaling,
            isCelestial  = isCelestial,
            isUpgradable = isUpgradable,
            isOffHand    = isOffHand,
            qualityCalc  = false
        )

        updateResults(mutableMapOf(
            OrnaViewUpdateType.ITEM_ASSESS_RESULTS to result
        ))
    }

    private fun createLayout(
        x_: Int, y_: Int,
        width_: Int, height_: Int,
        text: String
    ) {
        if (mLayout != null) return

        Log.d(TAG, "CREATING LAYOUT")
        mLayout = LinearLayout(ctx).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            isVisible = true
        }

        val params = WindowManager.LayoutParams().apply {
            y = x_; x = y_
            width = width_; height = height_
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            gravity = Gravity.TOP or Gravity.LEFT
            format = PixelFormat.TRANSPARENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }

        val tv = TextView(ctx).apply {
            this.text = text
            setTextColor(Color.WHITE)
        }

        (mLayout as LinearLayout).addView(tv)

        try {
            wm.addView(mLayout, params)
        } catch (ex: Exception) {
            Log.i(TAG, "adding view failed", ex)
        }
    }
}
