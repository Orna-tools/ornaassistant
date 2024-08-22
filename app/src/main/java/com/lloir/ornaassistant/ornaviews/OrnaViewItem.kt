package com.lloir.ornaassistant.ornaviews

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.lloir.ornaassistant.OrnaView
import com.lloir.ornaassistant.OrnaViewType
import com.lloir.ornaassistant.OrnaViewUpdateType
import com.lloir.ornaassistant.ScreenData
import com.lloir.ornaassistant.VolleySingleton
import com.lloir.ornaassistant.startsWithUppercaseLetter
import org.json.JSONObject
import org.json.JSONTokener

class OrnaViewItem(
    data: ArrayList<ScreenData>,
    wm: WindowManager,
    ctx: Context
) : OrnaView(OrnaViewType.ITEM, wm, ctx) {

    val TAG = "OrnaViewItem"
    var itemName: String? = null
    var nameLocation: Rect? = null
    var attributes: MutableMap<String, Int> = mutableMapOf()
    var level: Int = 1

    override fun update(
        data: ArrayList<ScreenData>,
        updateResults: (MutableMap<OrnaViewUpdateType, Any?>) -> Unit
    ): Boolean {
        if (itemName == null) {
            val cleanedData = data
                .filter { it.name.startsWithUppercaseLetter() }
                .filterNot { it.name.startsWith("Inventory") }
                .filterNot { it.name.startsWith("Knights of Inferno") }
                .filterNot { it.name.startsWith("Earthen Legion") }
                .filterNot { it.name.startsWith("FrozenGuard") }
                .filterNot { it.name.startsWith("Party") }
                .filterNot { it.name.startsWith("Arena") }
                .filterNot { it.name.startsWith("Codex") }
                .filterNot { it.name.startsWith("Runeshop") }
                .filterNot { it.name.startsWith("Options") }
                .filterNot { it.name.startsWith("Gauntlet") }
                .filterNot { it.name.startsWith("Character") }
                .filterNot { it.name.startsWith("Exotic") }
            getName(cleanedData)
            getAttributes(cleanedData)
            assessItem(updateResults)
        }

        return false
    }

    private fun getName(data: List<ScreenData>) {
        val qualities = listOf(
            "Broken ", "Poor ", "Superior ", "Famed ", "Legendary ",
            "Ornate ", "Masterforged ", "Demonforged ", "Godforged "
        )

        val prefixes = listOf(
            "burning", "embered", "fiery", "flaming", "infernal",
            "scalding", "warm", "chilling", "icy", "oceanic",
            "snowy", "tidal", "winter", "balanced", "earthly",
            "grounded", "natural", "organic", "rocky", "stony",
            "electric", "shocking", "sparking", "stormy", "thunderous",
            "angelic", "bright", "divine", "moral", "pure",
            "purifying", "revered", "righteous", "saintly", "sublime",
            "corrupted", "diabolic", "demonic", "gloomy", "impious",
            "profane", "unhallowed", "wicked", "beastly", "bestial",
            "chimeric", "dragonic", "wild", "colorless", "customary",
            "normalized", "origin", "reformed", "renewed", "reworked"
        )

        // Dynamically determine the item name
        val nameData = data.firstOrNull()
        var name = nameData?.name
        nameLocation = nameData?.position

        if (name!!.contains("You are")) {
            name = data.getOrNull(1)?.name ?: name
            nameLocation = data.getOrNull(1)?.position ?: nameLocation
        }

        // Remove any detected quality or prefix from the name
        name = qualities.fold(name) { acc, quality -> acc.replace(quality, "") }
        name = prefixes.fold(name) { acc, prefix -> acc.replace(prefix.capitalize() + " ", "") }

        itemName = name
    }

    private fun getAttributes(data: List<ScreenData>) {
        var bAdornments = false
        val acceptedAttributes = listOf("Att", "Mag", "Def", "Res", "Dex", "Crit", "Mana", "Ward")

        for (item in data) {
            Log.d(TAG, "Processing item attribute: ${item.name}")

            if (item.name.contains("ADORNMENTS")) {
                bAdornments = true
            } else if (item.name.contains("Level")) {
                level = item.name.replace("Level ", "").toIntOrNull() ?: 1
            } else {
                // Split attributes in the line
                val attributesList = item.name.split("(?<=[^\\s])(?=[A-Z])".toRegex())

                attributesList.forEach { text ->
                    val cleanedText = text
                        .replace("−", "-")
                        .replace(" ", "")

                    val match = Regex("([A-Za-z]+):\\s*(-?[0-9,]+)").findAll(cleanedText)
                    match.forEach { result ->
                        val (attName, attValString) = result.destructured
                        val attVal = attValString.replace(",", "").toIntOrNull()
                        if (attVal != null && acceptedAttributes.contains(attName)) {
                            if (!bAdornments) {
                                attributes[attName] = attVal
                            } else {
                                val newValue = attributes[attName] ?: 0
                                attributes[attName] = newValue - attVal
                            }
                            Log.d(TAG, "Parsed attribute: $attName = $attVal")
                        } else {
                            Log.d(TAG, "Ignored or invalid attribute: $attName = $attVal")
                        }
                    }
                }
            }
        }
    }

    private fun assessItem(updateResults: (MutableMap<OrnaViewUpdateType, Any?>) -> Unit) {
        val url = "https://orna.guide/api/v1/assess"
        val start = System.currentTimeMillis()

        val params = HashMap<String, Any>()
        attributes.filter { it.value > 0 }.forEach { (attName, attValue) ->
            when (attName) {
                "HP" -> params["hp"] = attValue
                "Mana" -> params["mana"] = attValue
                "Mag" -> params["magic"] = attValue
                "Att" -> params["attack"] = attValue
                "Def" -> params["defense"]= attValue
                "Res" -> params["resistance"] = attValue
                "Dex" -> params["dexterity"] = attValue
                "Ward" -> params["ward"] = attValue
                "Crit" -> {
                    // Handle Crit if necessary
                }
                else -> {
                    Log.d(TAG, "Invalid attribute $attName")
                    return
                }
            }
        }

        params["name"] = itemName!!.substringBefore(" Inventory")
        params["level"] = level
        val jsonObject = JSONObject(params as Map<*, *>)
        Log.d(TAG, "Assessing item $jsonObject")

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            { response ->
                try {
                    Log.d(TAG, "Response in ${System.currentTimeMillis() - start} ms: $response")

                    val jsonObject = JSONTokener(response.toString()).nextValue() as JSONObject
                    updateResults(mutableMapOf(OrnaViewUpdateType.ITEM_ASSESS_RESULTS to jsonObject))
                } catch (e: Exception) {
                    Log.e(TAG, "Exception: $e")
                }

            }, {
                Log.e(TAG, "Volley error: $it")
            })

        request.retryPolicy = DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
            1,
            1f
        )

        VolleySingleton.getInstance(ctx).addToRequestQueue(request)
    }

    private fun createLayout(x_: Int, y_: Int, width_: Int, height_: Int, text: String) {
        if (mLayout != null) {
            return
        }

        Log.d(TAG, "CREATING LAYOUT")

        mLayout = LinearLayout(ctx)
        val layout = mLayout as LinearLayout
        layout.setHorizontalGravity(Gravity.CENTER_HORIZONTAL)

        val layoutParams = WindowManager.LayoutParams().apply {
            y = x_
            x = y_
            width = width_
            height = height_
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            gravity = Gravity.TOP or Gravity.LEFT
            format = PixelFormat.TRANSPARENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }

        val textView = TextView(ctx).apply {
            this.text = text
            this.setTextColor(Color.WHITE)
        }

        layout.addView(textView)
        layout.isVisible = true

        try {
            wm.addView(layout, layoutParams)
        } catch (ex: Exception) {
            Log.i(TAG, "adding view failed", ex)
        }
    }
}
