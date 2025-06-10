package com.lloir.ornaassistant.service.parser.impl

import android.util.Log
import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.domain.model.ScreenData
import com.lloir.ornaassistant.domain.usecase.AssessItemUseCase
import com.lloir.ornaassistant.service.parser.ScreenParser
import com.lloir.ornaassistant.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemScreenParser @Inject constructor(
    private val assessItemUseCase: AssessItemUseCase
) : ScreenParser {

    private val parserScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        private const val TAG = "ItemScreenParser"

        // Invalid item names that should be filtered out
        private val INVALID_ITEM_NAMES = setOf(
            "gold", "orns", "exp", "experience", "level", "tier", "you are", "acquired",
            "drop", "new", "send to keep", "map", "character", "inventory", "codex",
            "runeshop", "options", "gauntlet", "party", "arena", "knights of inferno",
            "earthen legion", "frozenguard", "wayvessel", "notifications"
        )

        // Pattern for invalid names (numbers, single characters, currency symbols, etc.)
        private val INVALID_NAME_PATTERNS = listOf(
            Regex("^\\d+$"), // Pure numbers
            Regex("^\\d+[km]$"), // Numbers with k/m suffix (like "3_m")
            Regex("^[a-zA-Z]$"), // Single letters
            Regex("^.{1,2}$"), // Very short strings
            Regex("^[^a-zA-Z]+$"), // No letters at all
            Regex(".*[_]{2,}.*"), // Multiple underscores
        )
    }

    override suspend fun parseScreen(parsedScreen: ParsedScreen) {
        try {
            val itemName = extractItemName(parsedScreen.data)
            val level = extractLevel(parsedScreen.data)
            val attributes = extractAttributes(parsedScreen.data)

            if (itemName != null && level != null && attributes.isNotEmpty()) {
                Log.d(TAG, "Attempting to assess item: $itemName (level $level)")

                // Assess item asynchronously
                parserScope.launch {
                    try {
                        assessItemUseCase(itemName, level, attributes)
                        Log.d(TAG, "Successfully assessed item: $itemName")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to assess item: $itemName", e)
                    }
                }
            } else {
                Log.d(TAG, "Skipping assessment - itemName: $itemName, level: $level, attributes count: ${attributes.size}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing item screen", e)
        }
    }

    private fun extractItemName(screenData: List<ScreenData>): String? {
        // Filter screen data to exclude UI elements and get potential item names
        val cleanedData = screenData
            .filter { it.text.isNotBlank() }
            .filterNot { data ->
                INVALID_ITEM_NAMES.any { invalid ->
                    data.text.lowercase().contains(invalid)
                }
            }
            .filter { data ->
                // Must start with uppercase letter (item names are capitalized)
                data.text.first().isUpperCase()
            }
            .filterNot { data ->
                // Filter out invalid patterns
                INVALID_NAME_PATTERNS.any { pattern ->
                    pattern.matches(data.text)
                }
            }

        var name = cleanedData.firstOrNull()?.text

        if (name?.contains("You are") == true) {
            name = cleanedData.getOrNull(1)?.text
        }

        // Use let to work with non-null name safely
        return name?.takeIf { it.isNotBlank() }?.let { nonNullName ->
            var processedName = nonNullName

            // Remove quality prefixes
            Constants.ITEM_QUALITY_PREFIXES.forEach { prefix ->
                if (processedName.startsWith(prefix)) {
                    processedName = processedName.replace(prefix, "").trim()
                }
            }

            // Remove enchantment prefixes
            Constants.ENCHANTMENT_PREFIXES.forEach { prefix ->
                val capitalizedPrefix = prefix.replaceFirstChar { it.uppercase() }
                if (processedName.startsWith(capitalizedPrefix)) {
                    processedName = processedName.replace("$capitalizedPrefix ", "")
                }
            }

            val finalName = processedName.takeIf { it.isNotBlank() }

            // Final validation - ensure the processed name is still valid
            if (finalName != null && isValidItemName(finalName)) {
                Log.d(TAG, "Extracted item name: '$finalName' from original: '$nonNullName'")
                finalName
            } else {
                Log.w(TAG, "Rejected invalid item name: '$finalName' from original: '$nonNullName'")
                null
            }
        }
    }

    private fun isValidItemName(name: String): Boolean {
        // Must be at least 3 characters
        if (name.length < 3) return false

        // Must contain at least one letter
        if (!name.any { it.isLetter() }) return false

        // Must not be in invalid names list
        if (INVALID_ITEM_NAMES.contains(name.lowercase())) return false

        // Must not match invalid patterns
        if (INVALID_NAME_PATTERNS.any { it.matches(name) }) return false

        // Must start with a letter
        if (!name.first().isLetter()) return false

        return true
    }

    private fun extractLevel(screenData: List<ScreenData>): Int? {
        return screenData.find { it.text.startsWith("Level ") }
            ?.text
            ?.replace("Level ", "")
            ?.toIntOrNull()
    }

    private fun extractAttributes(screenData: List<ScreenData>): Map<String, Int> {
        val attributes = mutableMapOf<String, Int>()
        val acceptedAttributes = listOf("Att", "Mag", "Def", "Res", "Dex", "Crit", "Mana", "Ward", "HP")
        var isAdornmentSection = false

        screenData.forEach { item ->
            if (item.text.contains("ADORNMENTS")) {
                isAdornmentSection = true
                return@forEach
            }

            val cleanText = item.text
                .replace("−", "-")
                .replace(" ", "")
                .replace(",", "")
                .replace(".", "")

            val match = Regex("([A-Za-z\\s]+):\\s*(-?[0-9]+)").find(cleanText)
            if (match != null && match.groups.size == 3) {
                val attName = match.groups[1]?.value?.trim()
                val attVal = match.groups[2]?.value?.toIntOrNull()

                if (attName != null && attVal != null && acceptedAttributes.contains(attName)) {
                    if (isAdornmentSection) {
                        // Subtract adornment values from base stats
                        val currentValue = attributes[attName] ?: 0
                        attributes[attName] = currentValue - attVal
                    } else {
                        attributes[attName] = attVal
                    }
                }
            }
        }

        return attributes
    }
}