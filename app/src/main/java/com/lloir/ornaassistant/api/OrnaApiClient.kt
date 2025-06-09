package com.lloir.ornaassistant.api

import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.lloir.ornaassistant.ScreenData
import com.lloir.ornaassistant.VolleySingleton
import com.lloir.ornaassistant.assess.AssessResult
import com.lloir.ornaassistant.assess.Material
import com.lloir.ornaassistant.assess.StatSeries
import com.lloir.ornaassistant.startsWithUppercaseLetter
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max
import kotlin.math.min

class OrnaApiClient(private val volleySingleton: VolleySingleton) {

    companion object {
        private const val TAG = "OrnaApiClient"
        private const val BASE_URL = "https://orna.guide/api/v1"
        private const val ASSESS_ENDPOINT = "$BASE_URL/assess"
        private const val ITEM_ENDPOINT = "$BASE_URL/item"
    }

    // Data classes for API
    data class ApiAssessRequest(
        val name: String? = null,
        val id: Int? = null,
        val level: Int,
        val hp: Int? = null,
        val mana: Int? = null,
        val attack: Int? = null,
        val magic: Int? = null,
        val defense: Int? = null,
        val resistance: Int? = null,
        val dexterity: Int? = null
    )

    data class ApiAssessResponse(
        val quality: String,
        val stats: Map<String, ApiStatInfo>,
        val tier: Int,
        val type: String,
        val name: String,
        val materials: List<ApiMaterial>?
    )

    data class ApiStatInfo(
        val base: Int,
        val values: List<Int>
    )

    data class ApiMaterial(
        val name: String,
        val id: Int
    )

    // Parsed item data container
    data class ParsedItemData(
        val itemName: String,
        val level: Int,
        val attributes: Map<String, Int>
    )

    /**
     * Main function to process screen data and return assessment results
     */
    suspend fun processItemScreenData(screenData: ArrayList<ScreenData>): AssessResult? {
        Log.d(TAG, "=== STARTING ITEM PROCESSING ===")
        Log.d(TAG, "Raw screen data (${screenData.size} items):")
        screenData.forEachIndexed { index, data ->
            Log.d(TAG, "[$index] '${data.name}' (depth: ${data.depth})")
        }

        val cleanedData = cleanScreenData(screenData)
        Log.d(TAG, "Cleaned data (${cleanedData.size} items):")
        cleanedData.forEachIndexed { index, data ->
            Log.d(TAG, "[$index] '${data.name}'")
        }

        val parsedData = parseItemDataWithDebug(cleanedData)

        if (parsedData?.itemName.isNullOrBlank()) {
            Log.w(TAG, "Could not extract item name from screen data")
            return null
        }

        Log.d(TAG, "=== FINAL PARSED DATA ===")
        Log.d(TAG, "Item: ${parsedData?.itemName}")
        Log.d(TAG, "Level: ${parsedData?.level}")
        Log.d(TAG, "Stats: ${parsedData?.attributes}")
        Log.d(TAG, "=== SENDING TO API ===")

        return assessParsedItem(parsedData!!)
    }

    /**
     * Clean and filter screen data to relevant item information
     */
    private fun cleanScreenData(data: ArrayList<ScreenData>): List<ScreenData> {
        return data
            .filter { it.name.startsWithUppercaseLetter() }
            .filterNot {
                listOf(
                    "Inventory", "Knights of Inferno", "Earthen Legion", "FrozenGuard",
                    "Party", "Arena", "Codex", "Runeshop", "Options", "Gauntlet", "Character",
                    "TIER", "RARITY", "ACQUIRED", "LVL"
                ).any { prefix -> it.name.startsWith(prefix) }
            }
    }

    /**
     * Parse all item data with extensive debugging
     */
    private fun parseItemDataWithDebug(data: List<ScreenData>): ParsedItemData? {
        Log.d(TAG, "=== PARSING ITEM DATA ===")

        val itemName = extractItemNameWithDebug(data)
        val level = extractLevelWithDebug(data)
        val attributes = extractAttributesWithDebug(data)

        if (itemName.isNullOrBlank()) {
            Log.e(TAG, "Failed to extract item name!")
            return null
        }

        return ParsedItemData(itemName, level, attributes)
    }

    /**
     * Extract item name with debugging
     */
    private fun extractItemNameWithDebug(data: List<ScreenData>): String? {
        Log.d(TAG, "--- Extracting Item Name ---")

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

        val first = data.firstOrNull()
        if (first == null) {
            Log.e(TAG, "No data available for name extraction")
            return null
        }

        var name = first.name
        Log.d(TAG, "Starting with: '$name'")

        // Handle "You are" prefix
        if (name.contains("You are")) {
            data.getOrNull(1)?.let {
                name = it.name
                Log.d(TAG, "Found 'You are' prefix, using second item: '$name'")
            }
        }

        // Remove quality prefixes
        qualities.forEach { quality ->
            if (name.startsWith(quality)) {
                name = name.removePrefix(quality)
                Log.d(TAG, "Removed quality '$quality', now: '$name'")
            }
        }

        // Remove element prefixes
        prefixes.forEach { prefix ->
            val capitalizedPrefix = prefix.replaceFirstChar(Char::titlecase)
            if (name.startsWith("$capitalizedPrefix ")) {
                name = name.removePrefix("$capitalizedPrefix ")
                Log.d(TAG, "Removed prefix '$capitalizedPrefix', now: '$name'")
            }
        }

        val finalName = name.trim().takeIf { it.isNotBlank() }
        Log.d(TAG, "Final item name: '$finalName'")
        return finalName
    }

    /**
     * Extract level with debugging
     */
    private fun extractLevelWithDebug(data: List<ScreenData>): Int {
        Log.d(TAG, "--- Extracting Level ---")

        data.forEach { node ->
            if (node.name.startsWith("Level ")) {
                val levelStr = node.name.removePrefix("Level ")
                val rawLevel = levelStr.toIntOrNull() ?: 1
                val clampedLevel = max(1, min(10, rawLevel))

                Log.d(TAG, "Found level text: '${node.name}'")
                Log.d(TAG, "Extracted level string: '$levelStr'")
                Log.d(TAG, "Raw level: $rawLevel")
                Log.d(TAG, "Clamped level (1-10): $clampedLevel")

                return clampedLevel
            }
        }

        Log.d(TAG, "No level found, defaulting to 1")
        return 1
    }

    /**
     * Extract attributes with extensive debugging - COMPLETELY REWRITTEN
     */
    private fun extractAttributesWithDebug(data: List<ScreenData>): Map<String, Int> {
        Log.d(TAG, "--- Extracting Attributes ---")

        // Only these stats are accepted by the API
        val validApiStats = setOf("hp", "mana", "attack", "magic", "defense", "resistance", "dexterity")

        // Map what we see in game to API field names
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
            "mana" to "mana"
        )

        val baseStats = mutableMapOf<String, Int>()
        val adornmentStats = mutableMapOf<String, Int>()
        var currentSection = "unknown"

        Log.d(TAG, "Processing ${data.size} data items...")

        data.forEachIndexed { index, node ->
            val text = node.name
            Log.d(TAG, "[$index] Processing: '$text'")

            when {
                text.contains("ADORNMENTS", ignoreCase = true) -> {
                    currentSection = "adornments"
                    Log.d(TAG, "  → ENTERED ADORNMENTS SECTION")
                }
                text.contains("ABILITIES", ignoreCase = true) ||
                        text.contains("GIVEN BY", ignoreCase = true) ||
                        text.contains("DESCRIPTIONS", ignoreCase = true) ||
                        text.contains("DROP", ignoreCase = true) -> {
                    currentSection = "other"
                    Log.d(TAG, "  → ENTERED OTHER SECTION: $text")
                }
                text.contains("Level") -> {
                    currentSection = "level"
                    Log.d(TAG, "  → Found level info")
                }
                else -> {
                    // Try to parse as stat
                    val regex = Regex("([A-Za-z\\s]+):\\s*(-?[0-9,]+)")
                    val matches = regex.findAll(text.replace("−", "-"))

                    matches.forEach { match ->
                        val (statName, valueStr) = match.destructured
                        val cleanStatName = statName.lowercase().trim()
                        val apiFieldName = statMapping[cleanStatName]
                        val value = valueStr.replace(",", "").toIntOrNull()

                        Log.d(TAG, "  → Found stat: '$statName' = '$valueStr'")
                        Log.d(TAG, "    Clean name: '$cleanStatName'")
                        Log.d(TAG, "    API field: '$apiFieldName'")
                        Log.d(TAG, "    Value: $value")
                        Log.d(TAG, "    Section: $currentSection")

                        if (apiFieldName != null && validApiStats.contains(apiFieldName) && value != null) {
                            when (currentSection) {
                                "adornments" -> {
                                    adornmentStats[apiFieldName] = (adornmentStats[apiFieldName] ?: 0) + value
                                    Log.d(TAG, "    → Added to adornments: ${adornmentStats[apiFieldName]}")
                                }
                                else -> {
                                    baseStats[apiFieldName] = (baseStats[apiFieldName] ?: 0) + value
                                    Log.d(TAG, "    → Added to base stats: ${baseStats[apiFieldName]}")
                                }
                            }
                        } else {
                            Log.d(TAG, "    → SKIPPED (invalid stat or value)")
                        }
                    }
                }
            }
        }

        Log.d(TAG, "=== STAT CALCULATION ===")
        Log.d(TAG, "Base stats: $baseStats")
        Log.d(TAG, "Adornment stats: $adornmentStats")

        // Calculate final stats: base stats - adornment penalties
        val finalStats = mutableMapOf<String, Int>()

        // Start with base stats
        baseStats.forEach { (stat, value) ->
            finalStats[stat] = value
        }

        // Subtract adornment penalties (adornments usually show negative effects)
        adornmentStats.forEach { (stat, adornmentValue) ->
            val currentValue = finalStats[stat] ?: 0
            val newValue = currentValue - adornmentValue  // Subtract because adornments reduce stats
            finalStats[stat] = newValue
            Log.d(TAG, "$stat: $currentValue - $adornmentValue = $newValue")
        }

        // Only keep positive values
        val positiveStats = finalStats.filter { it.value > 0 }

        Log.d(TAG, "Final positive stats: $positiveStats")
        return positiveStats
    }

    /**
     * Assess parsed item data via API
     */
    private suspend fun assessParsedItem(parsedData: ParsedItemData): AssessResult? {
        // Create API request - only include stats that have positive values
        val request = ApiAssessRequest(
            name = parsedData.itemName,
            level = parsedData.level,
            hp = parsedData.attributes["hp"]?.takeIf { it > 0 },
            mana = parsedData.attributes["mana"]?.takeIf { it > 0 },
            attack = parsedData.attributes["attack"]?.takeIf { it > 0 },
            magic = parsedData.attributes["magic"]?.takeIf { it > 0 },
            defense = parsedData.attributes["defense"]?.takeIf { it > 0 },
            resistance = parsedData.attributes["resistance"]?.takeIf { it > 0 },
            dexterity = parsedData.attributes["dexterity"]?.takeIf { it > 0 }
        )

        Log.d(TAG, "API Request: $request")

        // Call API
        val apiResponse = assessItem(request)
        if (apiResponse == null) {
            Log.e(TAG, "API returned null response")
            return null
        }

        // Convert API response to internal format
        return convertApiResponseToAssessResult(apiResponse)
    }

    /**
     * Convert API response to AssessResult
     */
    private fun convertApiResponseToAssessResult(apiResponse: ApiAssessResponse): AssessResult {
        val quality = apiResponse.quality.toDoubleOrNull() ?: 0.0
        val stats = apiResponse.stats.mapValues { (_, apiStat) ->
            StatSeries(apiStat.base, apiStat.values)
        }
        val materials = apiResponse.materials?.map { apiMaterial ->
            Material(apiMaterial.name, apiMaterial.id)
        }

        Log.d(TAG, "API Response converted: quality=$quality, stats=${stats.keys}, tier=${apiResponse.tier}")

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

    /**
     * Make API call to assess item
     */
    suspend fun assessItem(request: ApiAssessRequest): ApiAssessResponse? = suspendCoroutine { continuation ->
        val jsonRequest = JSONObject().apply {
            request.name?.let { put("name", it) }
            request.id?.let { put("id", it) }
            put("level", request.level)
            request.hp?.let { put("hp", it) }
            request.mana?.let { put("mana", it) }
            request.attack?.let { put("attack", it) }
            request.magic?.let { put("magic", it) }
            request.defense?.let { put("defense", it) }
            request.resistance?.let { put("resistance", it) }
            request.dexterity?.let { put("dexterity", it) }
        }

        Log.d(TAG, "Assessing item with request: $jsonRequest")

        val volleyRequest = JsonObjectRequest(
            Request.Method.POST,
            ASSESS_ENDPOINT,
            jsonRequest,
            { response ->
                try {
                    val assessResponse = parseAssessResponse(response)
                    Log.d(TAG, "Assessment successful: quality=${assessResponse.quality}")
                    continuation.resume(assessResponse)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse assess response", e)
                    continuation.resume(null)
                }
            },
            { error ->
                Log.e(TAG, "API assess request failed", error)
                val errorMsg = error.networkResponse?.let {
                    String(it.data, Charsets.UTF_8)
                } ?: error.message
                Log.e(TAG, "Error details: $errorMsg")
                continuation.resume(null)
            }
        )

        volleySingleton.addToRequestQueue(volleyRequest)
    }

    /**
     * Find item by name via API
     */
    suspend fun findItemByName(itemName: String): Int? = suspendCoroutine { continuation ->
        val jsonRequest = JSONObject().apply {
            put("name", itemName)
        }

        Log.d(TAG, "Finding item: $itemName")

        val volleyRequest = JsonObjectRequest(
            Request.Method.POST,
            ITEM_ENDPOINT,
            jsonRequest,
            { response ->
                try {
                    val itemId = response.getInt("id")
                    Log.d(TAG, "Found item ID: $itemId for $itemName")
                    continuation.resume(itemId)
                } catch (e: Exception) {
                    Log.w(TAG, "Item not found or parse error for: $itemName", e)
                    continuation.resume(null)
                }
            },
            { error ->
                Log.w(TAG, "Failed to find item: $itemName", error)
                continuation.resume(null)
            }
        )

        volleySingleton.addToRequestQueue(volleyRequest)
    }

    /**
     * Parse API assess response
     */
    private fun parseAssessResponse(response: JSONObject): ApiAssessResponse {
        val quality = response.getString("quality")
        val tier = response.getInt("tier")
        val type = response.getString("type")
        val name = response.getString("name")

        val statsJson = response.getJSONObject("stats")
        val stats = mutableMapOf<String, ApiStatInfo>()

        val statKeys = listOf("attack", "magic", "defense", "resistance", "dexterity", "hp", "mana")
        for (key in statKeys) {
            if (statsJson.has(key)) {
                val statObj = statsJson.getJSONObject(key)
                val base = statObj.getInt("base")
                val valuesArray = statObj.getJSONArray("values")
                val values = mutableListOf<Int>()
                for (i in 0 until valuesArray.length()) {
                    values.add(valuesArray.getInt(i))
                }
                stats[key] = ApiStatInfo(base, values)
            }
        }

        val materials = if (response.has("materials")) {
            val materialsArray = response.getJSONArray("materials")
            val materialsList = mutableListOf<ApiMaterial>()
            for (i in 0 until materialsArray.length()) {
                val materialObj = materialsArray.getJSONObject(i)
                materialsList.add(
                    ApiMaterial(
                        materialObj.getString("name"),
                        materialObj.getInt("id")
                    )
                )
            }
            materialsList
        } else null

        return ApiAssessResponse(quality, stats, tier, type, name, materials)
    }
}