package com.lloir.ornaassistant.ornaviews

import android.content.Context
import android.util.Log
import android.view.WindowManager
import com.lloir.ornaassistant.*
import com.lloir.ornaassistant.api.OrnaApiClient
import com.lloir.ornaassistant.assess.AssessResult
import com.lloir.ornaassistant.assess.Material
import com.lloir.ornaassistant.assess.StatSeries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrnaViewItem(
    data: ArrayList<ScreenData>,
    wm: WindowManager,
    ctx: Context
) : OrnaView(OrnaViewType.ITEM, wm, ctx) {

    companion object {
        private const val TAG = "OrnaViewItem"
    }

    private var itemName: String? = null
    private var attributes: MutableMap<String, Int> = mutableMapOf()
    private var level: Int = 1
    private val apiClient = OrnaApiClient(VolleySingleton.getInstance(ctx))
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun update(
        data: ArrayList<ScreenData>,
        updateResults: (MutableMap<OrnaViewUpdateType, Any?>) -> Unit
    ): Boolean {
        if (itemName == null) {
            val cleaned = data
                .filter { it.name.startsWithUppercaseLetter() }
                .filterNot {
                    listOf(
                        "Inventory", "Knights of Inferno", "Earthen Legion", "FrozenGuard",
                        "Party", "Arena", "Codex", "Runeshop", "Options", "Gauntlet", "Character",
                        "TIER", "RARITY", "ACQUIRED", "LVL"
                    ).any { prefix -> it.name.startsWith(prefix) }
                }

            extractItemData(cleaned)
            assessItemViaAPI(updateResults)
        }
        return false
    }

    private fun extractItemData(data: List<ScreenData>) {
        extractItemName(data)
        extractAttributes(data)
        extractLevel(data)

        Log.d(TAG, "Extracted - Name: '$itemName', Level: $level, Attributes: $attributes")
    }

    private fun extractItemName(data: List<ScreenData>) {
        val qualities = listOf(
            "Broken ", "Poor ", "Superior ", "Famed ", "Legendary ",
            "Ornate ", "Masterforged ", "Demonforged ", "Godforged "
        )
        val prefixes = listOf(
            "burning", "embered", "fiery", "flaming", "infernal", "scalding", "warm",
            "chilling", "icy", "oceanic", "snowy", "tidal", "winter", "balanced", "earthly",
            "grounded", "natural", "organic", "rocky", "stony", "electric", "shocking",
            "sparking", "stormy", "thunderous", "angelic", "bright", "divine", "moral",
            "pure", "purifying", "revered", "righteous", "saintly", "sublime", "corrupted",
            "diabolic", "demonic", "gloomy", "impious", "profane", "unhallowed", "wicked",
            "beastly", "bestial", "chimeric", "dragonic", "wild", "colorless", "customary",
            "normalized", "origin", "reformed", "renewed", "reworked"
        )

        val first = data.firstOrNull() ?: return
        var name = first.name

        // Handle "You are" prefix
        if (name.contains("You are")) {
            data.getOrNull(1)?.let { name = it.name }
        }

        // Remove quality prefixes
        qualities.forEach { q ->
            if (name.startsWith(q)) name = name.removePrefix(q)
        }

        // Remove element prefixes
        prefixes.forEach { p ->
            val cap = p.replaceFirstChar(Char::titlecase)
            if (name.startsWith("$cap ")) name = name.removePrefix("$cap ")
        }

        itemName = name.trim()
    }

    private fun extractLevel(data: List<ScreenData>) {
        data.forEach { node ->
            if (node.name.startsWith("Level ")) {
                level = node.name.removePrefix("Level ").toIntOrNull() ?: 1
                return
            }
        }
    }

    private fun extractAttributes(data: List<ScreenData>) {
        var inAdornments = false

        // Map display names to API field names
        val statMapping = mapOf(
            "att" to "attack",
            "attack" to "attack",
            "mag" to "magic",
            "magic" to "magic",
            "def" to "defense",
            "defense" to "defense",
            "res" to "resistance",
            "resistance" to "resistance",
            "dex" to "dexterity",
            "dexterity" to "dexterity",
            "hp" to "hp",
            "mana" to "mana",
            "crit" to "crit",
            "ward" to "ward"
        )

        data.forEach { node ->
            when {
                node.name.contains("ADORNMENTS") -> inAdornments = true
                else -> {
                    // Parse stat lines like "Att: 150" or "Magic: 300"
                    val regex = Regex("([A-Za-z\\s]+):\\s*(-?[0-9,]+)")
                    regex.findAll(node.name.replace("−", "-"))
                        .forEach { match ->
                            val (statName, valueStr) = match.destructured
                            val cleanStatName = statName.lowercase().trim()
                            val apiFieldName = statMapping[cleanStatName]

                            if (apiFieldName != null) {
                                val value = valueStr.replace(",", "").toIntOrNull()
                                if (value != null) {
                                    val adjustedValue = if (inAdornments) -value else value
                                    attributes[apiFieldName] = (attributes[apiFieldName] ?: 0) + adjustedValue
                                    Log.d(TAG, "Parsed stat: $cleanStatName -> $apiFieldName = $value (adjusted: $adjustedValue)")
                                }
                            }
                        }
                }
            }
        }

        // Remove any negative or zero values
        attributes = attributes.filter { it.value > 0 }.toMutableMap()
    }

    private fun assessItemViaAPI(updateResults: (MutableMap<OrnaViewUpdateType, Any?>) -> Unit) {
        val currentItemName = itemName
        if (currentItemName.isNullOrBlank()) {
            Log.w(TAG, "Cannot assess: item name is blank")
            return
        }

        coroutineScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    performAssessment(currentItemName)
                }

                if (result != null) {
                    Log.d(TAG, "Assessment successful: ${result.quality}% quality")
                    updateResults(mutableMapOf(
                        OrnaViewUpdateType.ITEM_ASSESS_RESULTS to result
                    ))
                } else {
                    Log.e(TAG, "Assessment failed for item: $currentItemName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during assessment", e)
            }
        }
    }

    private suspend fun performAssessment(itemName: String): AssessResult? {
        // Create API request
        val request = OrnaApiClient.ApiAssessRequest(
            name = itemName,
            level = level,
            hp = attributes["hp"],
            mana = attributes["mana"],
            attack = attributes["attack"],
            magic = attributes["magic"],
            defense = attributes["defense"],
            resistance = attributes["resistance"],
            dexterity = attributes["dexterity"]
        )

        // Call API
        val apiResponse = apiClient.assessItem(request)
        if (apiResponse == null) {
            Log.e(TAG, "API returned null response")
            return null
        }

        // Convert API response to internal format
        val quality = apiResponse.quality.toDoubleOrNull() ?: 0.0
        val stats = apiResponse.stats.mapValues { (_, apiStat) ->
            StatSeries(apiStat.base, apiStat.values)
        }
        val materials = apiResponse.materials?.map { apiMaterial ->
            Material(apiMaterial.name, apiMaterial.id)
        }

        return AssessResult(
            quality = quality,
            stats = stats,
            tier = apiResponse.tier,
            itemType = apiResponse.type,
            itemName = apiResponse.name,
            materials = materials,
            exact = true
        )
    }
}