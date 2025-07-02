package com.lloir.ornaassistant.service.parser.impl

import android.util.Log
import com.lloir.ornaassistant.domain.model.AssessmentResult
import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.domain.model.ScreenData
import com.lloir.ornaassistant.domain.usecase.AssessItemUseCase
import com.lloir.ornaassistant.service.parser.ScreenParser
import com.lloir.ornaassistant.utils.Constants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemScreenParser @Inject constructor(
    private val assessItemUseCase: AssessItemUseCase
) : ScreenParser {

    private val parserScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // State for tracking current assessment
    private val _currentAssessment = MutableStateFlow<AssessmentResult?>(null)
    val currentAssessment: StateFlow<AssessmentResult?> = _currentAssessment.asStateFlow()

    private val _currentItemName = MutableStateFlow<String?>(null)
    val currentItemName: StateFlow<String?> = _currentItemName.asStateFlow()

    // Enhanced debouncing - track processing state
    private val isProcessing = AtomicBoolean(false)
    private val lastProcessedItem = AtomicReference<String?>(null)
    private var lastProcessedTime: Long = 0
    private val minProcessInterval = 5000L // Increased to 5 seconds
    private var currentAssessmentJob: Job? = null

    // Cache recent assessments to avoid repeated API calls
    private val assessmentCache = mutableMapOf<String, CachedAssessment>()
    private val cacheExpiryMs = 60000L // 1 minute cache

    data class CachedAssessment(
        val result: AssessmentResult,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > 60000L
    }

    companion object {
        private const val TAG = "ItemScreenParser"

        // Invalid item names that should be filtered out
        private val INVALID_ITEM_NAMES = setOf(
            "gold", "orns", "exp", "experience", "level", "tier", "you are", "acquired",
            "drop", "new", "send to keep", "map", "character", "inventory", "codex",
            "runeshop", "options", "gauntlet", "party", "arena", "knights of inferno",
            "earthen legion", "frozenguard", "wayvessel", "notifications", "inbox",
            "vagrant beasts", "daily login", "news", "settings", "profile", "mail",
            "messages", "friends", "guild", "kingdom", "chat", "world", "help",
            "tutorial", "guide", "shop", "store", "buy", "sell", "trade", "market",
            "items", "equipment", "weapons", "armor", "accessories", "consumables",
            "materials", "keys", "misc", "followers", "pets", "mounts", "stats",
            "achievements", "quests", "events", "leaderboards", "rankings", "pvp",
            "raids", "dungeons", "bosses", "monsters", "npcs", "locations", "areas",
            "regions", "territories", "towns", "cities", "kingdoms", "guilds",
            "alliances", "wars", "battles", "competitions", "tournaments", "seasons",
            "rewards", "prizes", "loot", "drops", "finds", "discoveries", "treasures"
        )

        // UI element patterns to exclude
        private val UI_ELEMENT_PATTERNS = listOf(
            Regex("^\\d+$"), // Pure numbers
            Regex("^\\d+[km]$"), // Numbers with k/m suffix
            Regex("^[a-zA-Z]$"), // Single letters
            Regex("^.{1,2}$"), // Very short strings
            Regex("^[^a-zA-Z]+$"), // No letters at all
            Regex(".*[_]{2,}.*"), // Multiple underscores
            Regex("^(Level|Tier|★|\\+|-).*"), // Starts with level/tier indicators
            Regex(".*\\d{2,}.*"), // Contains multiple consecutive digits
            Regex("^(\\+\\d+|\\-\\d+)"), // Starts with +/- numbers
            Regex(".*%.*"), // Contains percentage
        )

        // Common UI elements/sections to skip
        private val UI_SECTIONS = setOf(
            "adornments", "stats", "attributes", "requirements", "effects", "abilities",
            "description", "lore", "flavor", "text", "details", "info", "information",
            "properties", "characteristics", "features", "bonuses", "penalties",
            "modifiers", "enhancements", "augmentations", "improvements", "upgrades"
        )
    }

    override suspend fun parseScreen(parsedScreen: ParsedScreen) {
        try {
            val itemName = extractItemName(parsedScreen.data)
            val level = extractLevel(parsedScreen.data)
            val attributes = extractAttributes(parsedScreen.data)

            // Quick validation
            if (itemName == null || level == null || attributes.isEmpty()) {
                // Clear current state if no valid item found
                if (_currentItemName.value != null) {
                    _currentItemName.value = null
                    _currentAssessment.value = null
                }
                return
            }

            // Check if this is a new item or we should skip processing
            if (!shouldProcessItem(itemName)) {
                return
            }

            // Update current item name immediately for overlay
            _currentItemName.value = itemName

            // Check cache first
            val cacheKey = createCacheKey(itemName, level, attributes)
            val cachedResult = assessmentCache[cacheKey]
            if (cachedResult != null && !cachedResult.isExpired()) {
                Log.d(TAG, "Using cached assessment for: $itemName")
                _currentAssessment.value = cachedResult.result
                return
            }

            // Mark as processing and start assessment
            if (isProcessing.compareAndSet(false, true)) {
                startAssessment(itemName, level, attributes, cacheKey)
            } else {
                Log.d(TAG, "Already processing, skipping: $itemName")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing item screen", e)
        }
    }

    private fun shouldProcessItem(itemName: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastItem = lastProcessedItem.get()

        return when {
            // Always process if it's a different item
            lastItem != itemName -> {
                lastProcessedItem.set(itemName)
                lastProcessedTime = currentTime
                true
            }
            // Skip if same item and within cooldown
            (currentTime - lastProcessedTime) < minProcessInterval -> {
                Log.d(TAG, "Skipping duplicate processing of: $itemName (cooldown)")
                false
            }
            // Process if enough time has passed
            else -> {
                lastProcessedTime = currentTime
                true
            }
        }
    }

    private fun startAssessment(itemName: String, level: Int, attributes: Map<String, Int>, cacheKey: String) {
        // Cancel any existing assessment job
        currentAssessmentJob?.cancel()

        currentAssessmentJob = parserScope.launch {
            try {
                Log.d(TAG, "Starting assessment for: $itemName (level $level)")

                val result = assessItemUseCase(itemName, level, attributes)

                // Cache the result
                assessmentCache[cacheKey] = CachedAssessment(result)
                cleanupExpiredCache()

                // Update state
                _currentAssessment.value = result

                Log.d(TAG, "Assessment completed for: $itemName, quality: ${result.quality}")

            } catch (e: CancellationException) {
                Log.d(TAG, "Assessment cancelled for: $itemName")
            } catch (e: Exception) {
                Log.e(TAG, "Assessment failed for: $itemName", e)
                _currentAssessment.value = null
            } finally {
                isProcessing.set(false)
            }
        }
    }

    private fun createCacheKey(itemName: String, level: Int, attributes: Map<String, Int>): String {
        val sortedAttributes = attributes.toSortedMap().toString()
        return "$itemName|$level|$sortedAttributes"
    }

    private fun cleanupExpiredCache() {
        val iterator = assessmentCache.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.isExpired()) {
                iterator.remove()
            }
        }
    }

    // Clear current assessment (called when screen changes)
    fun clearCurrentAssessment() {
        val currentItem = _currentItemName.value
        Log.d(TAG, "Clearing current assessment${if (currentItem != null) " for item: $currentItem" else ""}")

        // Log stack trace for debugging purposes
        try {
            throw Exception("Assessment cleared")
        } catch (e: Exception) {
            Log.d(TAG, "Assessment cleared call stack", e)
        }

        currentAssessmentJob?.cancel()
        _currentAssessment.value = null
        _currentItemName.value = null
        lastProcessedItem.set(null)
        isProcessing.set(false)
    }

    private fun extractItemName(screenData: List<ScreenData>): String? {
        // Log the raw screen data for debugging
        Log.d(TAG, "Extracting item name from ${screenData.size} screen data items")
        if (screenData.size > 0) {
            Log.d(TAG, "First 10 screen data items:")
            screenData.take(10).forEach { data ->
                Log.d(TAG, "  - '${data.text}' (depth: ${data.depth})")
            }
        }

        // First, let's get all potential item names by filtering out obvious UI elements
        val potentialNames = screenData
            .filter { it.text.isNotBlank() && it.text.length >= 3 }
            .filterNot { data ->
                // Filter out known invalid names (case insensitive)
                INVALID_ITEM_NAMES.any { invalid ->
                    data.text.lowercase().contains(invalid.lowercase())
                }
            }
            .filterNot { data ->
                // Filter out UI sections
                UI_SECTIONS.any { section ->
                    data.text.lowercase().contains(section)
                }
            }
            .filterNot { data ->
                // Filter out UI patterns
                UI_ELEMENT_PATTERNS.any { pattern ->
                    pattern.matches(data.text)
                }
            }
            .filter { data ->
                // Item names should start with uppercase letter or a special character followed by uppercase
                data.text.isNotEmpty() && (data.text.first().isUpperCase() || 
                    (data.text.length > 1 && !data.text.first().isLetterOrDigit() && data.text[1].isUpperCase()))
            }
            .filter { data ->
                // Item names should contain mostly letters (less strict than before)
                val letterCount = data.text.count { it.isLetter() }
                val totalLength = data.text.length
                letterCount.toFloat() / totalLength >= 0.4f // Reduced from 0.5f
            }

        // Log potential names for debugging
        Log.d(TAG, "Found ${potentialNames.size} potential item names:")
        potentialNames.take(5).forEach { data ->
            Log.d(TAG, "  - '${data.text}'")
        }

        // Strategy 1: Look for items that appear near "Level" indicators
        val levelIndex = screenData.indexOfFirst { it.text.startsWith("Level ") }
        if (levelIndex > 0) {
            // Look backwards from level for the item name
            for (i in (levelIndex - 1) downTo 0) {
                val candidate = screenData[i].text
                if (potentialNames.any { it.text == candidate }) {
                    return processItemName(candidate)
                }
            }
        }

        // Strategy 2: Look for items with quality prefixes
        val itemWithQuality = potentialNames.find { data ->
            Constants.ITEM_QUALITY_PREFIXES.any { prefix ->
                data.text.startsWith(prefix, ignoreCase = true)
            }
        }
        if (itemWithQuality != null) {
            return processItemName(itemWithQuality.text)
        }

        // Strategy 3: Look for items with enchantment prefixes
        val itemWithEnchantment = potentialNames.find { data ->
            Constants.ENCHANTMENT_PREFIXES.any { prefix ->
                data.text.lowercase().startsWith(prefix.lowercase())
            }
        }
        if (itemWithEnchantment != null) {
            return processItemName(itemWithEnchantment.text)
        }

        // Strategy 4: Take the longest reasonable candidate
        val longestCandidate = potentialNames
            .filter { it.text.length in 3..50 } // Reasonable length range
            .maxByOrNull { it.text.length }

        if (longestCandidate != null) {
            return processItemName(longestCandidate.text)
        }

        // Strategy 5: Samsung-specific strategy - look for items at specific depths
        // Samsung devices often have a different accessibility tree structure
        val samsungCandidate = potentialNames
            .filter { it.depth in 3..6 } // Items are often at these depths on Samsung
            .filter { it.text.length in 5..40 } // Reasonable length for item names
            .firstOrNull()

        if (samsungCandidate != null) {
            Log.d(TAG, "Using Samsung-specific candidate: ${samsungCandidate.text} at depth ${samsungCandidate.depth}")
            return processItemName(samsungCandidate.text)
        }

        // Strategy 6: Look for items with specific patterns common in Orna
        val ornaPatternCandidate = potentialNames.find { data ->
            // Common patterns in Orna item names
            data.text.contains(" of ", ignoreCase = true) || // "Sword of Light"
            data.text.contains("'s ", ignoreCase = true) ||  // "Dragon's Breath"
            data.text.contains(" the ", ignoreCase = true)    // "Blade the Destroyer"
        }

        if (ornaPatternCandidate != null) {
            Log.d(TAG, "Using Orna pattern candidate: ${ornaPatternCandidate.text}")
            return processItemName(ornaPatternCandidate.text)
        }

        // Strategy 7: Fall back to first valid candidate
        val firstCandidate = potentialNames.firstOrNull()
        if (firstCandidate != null) {
            Log.d(TAG, "Using first candidate: ${firstCandidate.text}")
            return processItemName(firstCandidate.text)
        }

        // Log failure for debugging
        Log.w(TAG, "Failed to extract item name from ${screenData.size} screen data items")
        return null
    }

    private fun processItemName(rawName: String): String? {
        var processedName = rawName.trim()

        // Remove quality prefixes
        Constants.ITEM_QUALITY_PREFIXES.forEach { prefix ->
            if (processedName.startsWith(prefix, ignoreCase = true)) {
                processedName = processedName.removePrefix(prefix).trim()
            }
        }

        // Remove enchantment prefixes
        Constants.ENCHANTMENT_PREFIXES.forEach { prefix ->
            val capitalizedPrefix = prefix.replaceFirstChar { it.uppercase() }
            if (processedName.startsWith(capitalizedPrefix)) {
                processedName = processedName.removePrefix("$capitalizedPrefix ").trim()
            }
        }

        // Final validation
        if (processedName.isBlank() || processedName.length < 3) {
            Log.w(TAG, "Processed name too short: '$processedName' from '$rawName'")
            return null
        }

        // Check against banned names one more time
        if (INVALID_ITEM_NAMES.any { banned ->
                processedName.lowercase().contains(banned.lowercase())
            }) {
            Log.w(TAG, "Processed name contains banned term: '$processedName'")
            return null
        }

        Log.d(TAG, "Extracted item name: '$processedName' from original: '$rawName'")
        return processedName
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
        // Map of game attribute names to our internal names
        val attributeNameMap = mapOf(
            "attack" to "Att",
            "magic" to "Mag",
            "defense" to "Def",
            "resistance" to "Res",
            "dexterity" to "Dex",
            "critical" to "Crit",
            "mana" to "Mana",
            "ward" to "Ward",
            "hp" to "HP",
            "health" to "HP"
        )
        var isAdornmentSection = false

        // Log all screen data for debugging
        Log.d(TAG, "Extracting attributes from ${screenData.size} screen data items")
        screenData.forEach { item ->
            if (item.text.contains(":") || item.text.contains("HP") || item.text.contains("Mana")) {
                Log.d(TAG, "Potential attribute: '${item.text}'")
            }
        }

        screenData.forEach { item ->
            if (item.text.contains("ADORNMENTS")) {
                isAdornmentSection = true
                return@forEach
            }

            // First, preserve the original text for logging
            val originalText = item.text

            // Then clean it for processing
            val cleanText = originalText
                .replace("−", "-")
                .replace(" ", "")
                .replace(",", "")
                .replace(".", "")

            // Try different regex patterns to match attributes
            // Pattern 1: Standard "Attribute: Value" format
            val pattern1 = Regex("([A-Za-z]+):\\s*(-?[0-9,]+)")
            // Pattern 2: Just "HP: Value" format (special case)
            val pattern2 = Regex("HP:\\s*(-?[0-9,]+)")
            // Pattern 3: Just "Mana: Value" format (special case)
            val pattern3 = Regex("Mana:\\s*(-?[0-9,]+)")
            // Pattern 4: "Magic: Value" format (special case)
            val pattern4 = Regex("Magic:\\s*(-?[0-9,]+)")
            // Pattern 5: "MAG Value" format (without colon)
            val pattern5 = Regex("MAG\\s+(-?[0-9,]+)")
            // Pattern 6: "Magic Value" format (without colon)
            val pattern6 = Regex("Magic\\s+(-?[0-9,]+)")
            // Pattern 7: "MAG Value" format with comma (e.g., "MAG 1,381")
            val pattern7 = Regex("MAG\\s+(-?[0-9,]+,[0-9]+)")
            // Pattern 8: "Magic Value" format with comma (e.g., "Magic 1,381")
            val pattern8 = Regex("Magic\\s+(-?[0-9,]+,[0-9]+)")

            // Try all patterns
            val match = pattern1.find(cleanText) ?: pattern2.find(cleanText) ?: pattern3.find(cleanText) ?: 
                       pattern4.find(cleanText) ?: pattern5.find(cleanText) ?: pattern6.find(cleanText) ?: 
                       pattern7.find(cleanText) ?: pattern8.find(cleanText)

            if (match != null) {
                // Extract attribute name and value based on which pattern matched
                val attName: String?
                val attVal: Int?

                if (match.groups.size == 3) {
                    // Pattern 1 matched (has both name and value groups)
                    attName = match.groups[1]?.value?.trim()?.lowercase()
                    val rawValue = match.groups[2]?.value?.replace(",", "")
                    attVal = rawValue?.toIntOrNull()

                    Log.d(TAG, "Matched attribute with pattern 1: $attName = $attVal (from '$originalText')")
                } else if (match.groups.size == 2) {
                    // Pattern 2, 3, 4, 5, 6, 7, or 8 matched (special cases with only value group)
                    if (pattern2.matches(cleanText)) {
                        attName = "hp"
                    } else if (pattern3.matches(cleanText)) {
                        attName = "mana"
                    } else if (pattern4.matches(cleanText) || pattern6.matches(cleanText) || pattern8.matches(cleanText)) {
                        attName = "magic"
                    } else if (pattern5.matches(cleanText) || pattern7.matches(cleanText)) {
                        attName = "mag" // This will be mapped to "Mag" later
                    } else {
                        attName = null
                    }

                    val rawValue = match.groups[1]?.value?.replace(",", "")
                    attVal = rawValue?.toIntOrNull()

                    Log.d(TAG, "Matched attribute with special pattern: $attName = $attVal (from '$originalText')")
                } else {
                    attName = null
                    attVal = null
                }

                // Map the attribute name to our internal name
                val mappedName = attributeNameMap[attName]

                if (mappedName != null && attVal != null && acceptedAttributes.contains(mappedName)) {
                    if (isAdornmentSection) {
                        // Subtract adornment values from base stats
                        val currentValue = attributes[mappedName] ?: 0
                        attributes[mappedName] = currentValue - attVal
                        Log.d(TAG, "Added adornment attribute: $mappedName = ${currentValue - attVal} (subtracted $attVal)")
                    } else {
                        attributes[mappedName] = attVal
                        Log.d(TAG, "Added attribute: $mappedName = $attVal (mapped from $attName)")
                    }
                } else if (attName != null && attVal != null && acceptedAttributes.contains(attName)) {
                    // Direct match without mapping
                    if (isAdornmentSection) {
                        val currentValue = attributes[attName] ?: 0
                        attributes[attName] = currentValue - attVal
                        Log.d(TAG, "Added adornment attribute (direct): $attName = ${currentValue - attVal} (subtracted $attVal)")
                    } else {
                        attributes[attName] = attVal
                        Log.d(TAG, "Added attribute (direct): $attName = $attVal")
                    }
                }
            }

            // Special case for HP, Mana, MAG, and Magic that might appear without a colon
            if (originalText.contains("HP") && !originalText.contains(":")) {
                // Don't replace commas in the original text yet
                val hpMatch = Regex("HP\\s+(-?[0-9,]+)").find(originalText)
                if (hpMatch != null && hpMatch.groups.size == 2) {
                    // Now replace commas when parsing the value
                    val hpVal = hpMatch.groups[1]?.value?.replace(",", "")?.toIntOrNull()
                    if (hpVal != null) {
                        attributes["HP"] = hpVal
                        Log.d(TAG, "Added HP attribute from special format: HP = $hpVal (from '${hpMatch.groups[1]?.value}')")
                    }
                }

                // Also try with comma format (e.g., "HP 1,381")
                val hpCommaMatch = Regex("HP\\s+(-?[0-9,]+,[0-9]+)").find(originalText)
                if (hpCommaMatch != null && hpCommaMatch.groups.size == 2) {
                    val hpVal = hpCommaMatch.groups[1]?.value?.replace(",", "")?.toIntOrNull()
                    if (hpVal != null) {
                        attributes["HP"] = hpVal
                        Log.d(TAG, "Added HP attribute from comma format: HP = $hpVal (from '${hpCommaMatch.groups[1]?.value}')")
                    }
                }
            }

            if (originalText.contains("Mana") && !originalText.contains(":")) {
                // Don't replace commas in the original text yet
                val manaMatch = Regex("Mana\\s+(-?[0-9,]+)").find(originalText)
                if (manaMatch != null && manaMatch.groups.size == 2) {
                    // Now replace commas when parsing the value
                    val manaVal = manaMatch.groups[1]?.value?.replace(",", "")?.toIntOrNull()
                    if (manaVal != null) {
                        attributes["Mana"] = manaVal
                        Log.d(TAG, "Added Mana attribute from special format: Mana = $manaVal (from '${manaMatch.groups[1]?.value}')")
                    }
                }

                // Also try with comma format (e.g., "Mana 1,381")
                val manaCommaMatch = Regex("Mana\\s+(-?[0-9,]+,[0-9]+)").find(originalText)
                if (manaCommaMatch != null && manaCommaMatch.groups.size == 2) {
                    val manaVal = manaCommaMatch.groups[1]?.value?.replace(",", "")?.toIntOrNull()
                    if (manaVal != null) {
                        attributes["Mana"] = manaVal
                        Log.d(TAG, "Added Mana attribute from comma format: Mana = $manaVal (from '${manaCommaMatch.groups[1]?.value}')")
                    }
                }
            }

            // Special case for MAG without colon
            if (originalText.contains("MAG") && !originalText.contains(":")) {
                // Don't replace commas in the original text yet
                val magMatch = Regex("MAG\\s+(-?[0-9,]+)").find(originalText)
                if (magMatch != null && magMatch.groups.size == 2) {
                    // Now replace commas when parsing the value
                    val magVal = magMatch.groups[1]?.value?.replace(",", "")?.toIntOrNull()
                    if (magVal != null) {
                        attributes["Mag"] = magVal
                        Log.d(TAG, "Added Mag attribute from special format: MAG = $magVal (from '${magMatch.groups[1]?.value}')")
                    }
                }

                // Also try with comma format (e.g., "MAG 1,381")
                val magCommaMatch = Regex("MAG\\s+(-?[0-9,]+,[0-9]+)").find(originalText)
                if (magCommaMatch != null && magCommaMatch.groups.size == 2) {
                    val magVal = magCommaMatch.groups[1]?.value?.replace(",", "")?.toIntOrNull()
                    if (magVal != null) {
                        attributes["Mag"] = magVal
                        Log.d(TAG, "Added Mag attribute from comma format: MAG = $magVal (from '${magCommaMatch.groups[1]?.value}')")
                    }
                }
            }

            // Special case for Magic without colon
            if (originalText.contains("Magic") && !originalText.contains(":")) {
                // Don't replace commas in the original text yet
                val magicMatch = Regex("Magic\\s+(-?[0-9,]+)").find(originalText)
                if (magicMatch != null && magicMatch.groups.size == 2) {
                    // Now replace commas when parsing the value
                    val magicVal = magicMatch.groups[1]?.value?.replace(",", "")?.toIntOrNull()
                    if (magicVal != null) {
                        attributes["Mag"] = magicVal
                        Log.d(TAG, "Added Mag attribute from special format: Magic = $magicVal (from '${magicMatch.groups[1]?.value}')")
                    }
                }

                // Also try with comma format (e.g., "Magic 1,381")
                val magicCommaMatch = Regex("Magic\\s+(-?[0-9,]+,[0-9]+)").find(originalText)
                if (magicCommaMatch != null && magicCommaMatch.groups.size == 2) {
                    val magicVal = magicCommaMatch.groups[1]?.value?.replace(",", "")?.toIntOrNull()
                    if (magicVal != null) {
                        attributes["Mag"] = magicVal
                        Log.d(TAG, "Added Mag attribute from comma format: Magic = $magicVal (from '${magicCommaMatch.groups[1]?.value}')")
                    }
                }
            }
        }

        // Log the final extracted attributes
        Log.d(TAG, "Extracted attributes: $attributes")

        return attributes
    }
}
