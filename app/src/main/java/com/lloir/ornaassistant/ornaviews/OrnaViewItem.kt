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

class OrnaViewItem : OrnaView {
    val TAG = "OrnaViewItem"
    var itemName: String? = null
    var nameLocation: Rect? = null
    var attributes: MutableMap<String, Int> = mutableMapOf()
    var level: Int = 1

    constructor(
        data: ArrayList<ScreenData>,
        wm: WindowManager,
        ctx: Context
    ) : super(OrnaViewType.ITEM, wm, ctx) {

    }

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
            "Broken ",
            "Poor ",
            "Superior ",
            "Famed ",
            "Legendary ",
            "Ornate ",
            "Masterforged ",
            "Demonforged ",
            "Godforged "
        )

        var prefixes = listOf(
            "burning", "embered", "fiery", "flaming", "infernal", "scalding", "warm",
            "chilling", "icy", "oceanic", "snowy", "tidal", "winter",
            "balanced", "earthly", "grounded", "natural", "organic", "rocky", "stony",
            "electric", "shocking", "sparking", "stormy", "thunderous",
            "angelic", "bright", "divine", "moral", "pure", "purifying", "revered", "righteous", "saintly", "sublime",
            "corrupted", "diabolic", "demonic", "gloomy", "impious", "profane", "unhallowed", "wicked",
            "beastly", "bestial", "chimeric", "dragonic", "wild",
            "colorless", "customary", "normalized", "origin", "reformed", "renewed", "reworked"
        )
        val nameData = data.firstOrNull()
        var name = nameData?.name
        nameLocation = nameData?.position
        if (name!!.contains("You are")) {
            name = data[1].name
            nameLocation = data[1].position
        }

        for (quality in qualities) {
            if (name?.startsWith(quality) == true) {
                name = name.replace(quality, "")
            }
        }

        for (prefix in prefixes) {
            if (name?.startsWith(prefix.capitalize()) == true){
                name = name.replace(prefix.capitalize() + " ", "")
            }
        }

        itemName = name
    }

    private fun getAttributes(data: List<ScreenData>) {
        var bAdornments = false
        val acceptedAttributes = listOf("Att", "Mag", "Def", "Res", "Dex", "Crit", "Mana", "Ward")

        for (item in data) {
            if (item.name.contains("ADORNMENTS")) {
                bAdornments = true
            } else if (item.name.contains("Level")) {
                level = item.name.replace("Level ", "").toIntOrNull() ?: 1
            } else {
                var text = item.name
                    .replace("−", "-")
                    .replace(" ", "")

                val match = Regex("([A-Za-z\\s]+):\\s(-?[0-9,]+)(?:\\s*\\((.*)\\))?").findAll(text)
                match.forEach { result ->
                    val (attName, attValString, extra) = result.destructured
                    val attVal = attValString.replace(",", "").toIntOrNull()
                    if (attVal != null && acceptedAttributes.contains(attName)) {
                        if (!bAdornments) {
                            attributes[attName] = attVal
                        } else {
                            val newValue = attributes[attName] ?: 0
                            attributes[attName] = newValue - attVal
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
        Log.d(TAG, "Assessing item ${jsonObject}")

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

        layout.setHorizontalGravity(Gravity.CENTER_HORIZONTAL)

        val layoutParams = WindowManager.LayoutParams()
        layoutParams.apply {
            y = x_
            x = y_
            width = width_
            height = height_
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            gravity = Gravity.TOP or Gravity.LEFT
            format = PixelFormat.TRANSPARENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }

        var textView = TextView(ctx)
        textView.text = text
        textView.setTextColor(Color.WHITE)

        layout.addView(textView)

        layout.isVisible= true

        try {
            wm.addView(layout, layoutParams)
        } catch (ex: Exception) {
            Log.i(TAG, "adding view failed", ex)
        }
    }
}

class MyAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) { // Corrected line
        if ((event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                    event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) &&
            event.packageName == "playorna.com.orna") {

            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                traverseNodeTree(rootNode)
            }
        }
    }


    override fun onInterrupt() {
        // This method is called when the system interrupts your service.
        // You can handle the interruption here(e.g., stop any ongoing tasks).
    }

    private fun traverseNodeTree(node: AccessibilityNodeInfo) {
        if (node.text != null && node.text.toString().startsWithUppercaseLetter()) {
            val itemName = node.text.toString()
            Log.d("MyAccessibilityService", "Item Name: $itemName")

            // Send itemName to your API (implementation not provided)
            // sendItemNameToApi(itemName)
        }

        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                traverseNodeTree(childNode)
                childNode.recycle()
            }
        }
    }

    // Example API call implementation (using Volley) - you'll need to adapt this
    /*
    private fun sendItemNameToApi(itemName: String) {
        val url = "YOUR_API_ENDPOINT"
        val params = HashMap<String, String>()
        params["itemName"] = itemName

        val jsonObject = JSONObject(params as Map<*, *>)

        val request = JsonObjectRequest(Request.Method.POST, url, jsonObject,
            { response ->
                Log.d("MyAccessibilityService", "API Response: $response")
            },
            { error ->
                Log.e("MyAccessibilityService", "API Error: $error")
            })

        VolleySingleton.getInstance(this).addToRequestQueue(request)
    }
    */
}