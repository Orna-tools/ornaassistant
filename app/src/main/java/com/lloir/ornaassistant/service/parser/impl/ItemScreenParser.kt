package com.lloir.ornaassistant.service.parser.impl

import android.util.Log
import com.lloir.ornaassistant.BuildConfig
import com.lloir.ornaassistant.domain.model.AssessmentResult
import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.domain.model.ScreenData
import com.lloir.ornaassistant.domain.repository.SettingsRepository
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
    private val assessItemUseCase: AssessItemUseCase,
    private val settingsRepository: SettingsRepository
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

    // Helper function to check if debug logging is enabled
    private suspend fun isDebugEnabled(): Boolean {
        return try {
            settingsRepository.getSettings().debugMode
        } catch (e: Exception) {
            false // Default to false if we can't read settings
        }
    }

    // Helper function for conditional debug logging
    private suspend fun debugLog(message: String) {
        if (isDebugEnabled()) {
            Log.d(TAG, message)
        }
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
                runBlocking { debugLog("Using cached assessment for: $itemName") }
                _currentAssessment.value = cachedResult.result
                return
            }

            // Mark as processing and start assessment
            if (isProcessing.compareAndSet(false, true)) {
                startAssessment(itemName, level, attributes, cacheKey)
            } else {
                runBlocking { debugLog("Already processing, skipping: $itemName") }
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
                runBlocking { debugLog("Skipping duplicate processing of: $itemName (cooldown)") }
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
                runBlocking { debugLog("Starting assessment for: $itemName (level $level)") }

                val result = assessItemUseCase(itemName, level, attributes)

                // Cache the result
                assessmentCache[cacheKey] = CachedAssessment(result)
                cleanupExpiredCache()

                // Update state
                _currentAssessment.value = result

                runBlocking { debugLog("Assessment completed for: $itemName, quality: ${result.quality}") }

            } catch (e: CancellationException) {
                runBlocking { debugLog("Assessment cancelled for: $itemName") }
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
        runBlocking { debugLog("Clearing current assessment") }
        currentAssessmentJob?.cancel()
        _currentAssessment.value = null
        _currentItemName.value = null
        lastProcessedItem.set(null)
        isProcessing.set(false)
    }

    // Helper method to log warnings that should always be shown
    private fun logWarning(message: String) {
        Log.w(TAG, message)
    }

    private fun extractItemName(screenData: List<ScreenData>): String? {
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
                // Item names should start with uppercase letter
                data.text.first().isUpperCase()
            }
            .filter { data ->
                // Item names should contain mostly letters
                val letterCount = data.text.count { it.isLetter() }
                val totalLength = data.text.length
                letterCount.toFloat() / totalLength >= 0.5f
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

        // Strategy 5: Fall back to first valid candidate
        val firstCandidate = potentialNames.firstOrNull()
        if (firstCandidate != null) {
            runBlocking { debugLog("Using first candidate: ${firstCandidate.text}") }
            return processItemName(firstCandidate.text)
        }

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
            logWarning("Processed name too short: '$processedName' from '$rawName'")
            return null
        }

        // Check against banned names one more time
        if (INVALID_ITEM_NAMES.any { banned ->
                processedName.lowercase().contains(banned.lowercase())
            }) {
            logWarning("Processed name contains banned term: '$processedName'")
            return null
        }

        runBlocking { debugLog("Extracted item name: '$processedName' from original: '$rawName'") }
        return processedName
    }

    private fun extractLevel(screenData: List<ScreenData>): Int? {
        return screenData.find { it.text.startsWith("Level ") }
            ?.text
            ?.replace("Level ", "")
            ?.toIntOrNull()
    }

    /**
     * Extract attributes (stats) from the screen data
     */
    private suspend fun extractAttributes(screenData: List<ScreenData>): Map<String, Int> {
        val attributes = mutableMapOf<String, Int>()

        debugLog("=== EXTRACTING ITEM ATTRIBUTES ===")
        debugLog("Total screen elements: ${screenData.size}")

        // CRITICAL: We need to parse INDIVIDUAL ITEM STATS, not player total stats
        // Look for patterns that indicate item stat lines specifically

        val itemStatPatterns = listOf(
            // Pattern 1: Look for stat increases/decreases with +/- symbols
            Regex("([+-]\\d+)\\s*(?:attack|att|damage)", RegexOption.IGNORE_CASE),
            Regex("([+-]\\d+)\\s*(?:dexterity|dex)", RegexOption.IGNORE_CASE),
            Regex("([+-]\\d+)\\s*(?:hp|health)", RegexOption.IGNORE_CASE),
            Regex("([+-]\\d+)\\s*(?:ward|defense|def)", RegexOption.IGNORE_CASE),
            Regex("([+-]\\d+)\\s*(?:crit|critical)", RegexOption.IGNORE_CASE),
            Regex("([+-]\\d+)\\s*(?:magic|mag)", RegexOption.IGNORE_CASE),
            Regex("([+-]\\d+)\\s*(?:mana|mp)", RegexOption.IGNORE_CASE),
            Regex("([+-]\\d+)\\s*(?:resistance|res)", RegexOption.IGNORE_CASE),

            // Pattern 2: Reverse order - stat name followed by value
            Regex("(?:attack|att|damage)\\s*([+-]\\d+)", RegexOption.IGNORE_CASE),
            Regex("(?:dexterity|dex)\\s*([+-]\\d+)", RegexOption.IGNORE_CASE),
            Regex("(?:hp|health)\\s*([+-]\\d+)", RegexOption.IGNORE_CASE),
            Regex("(?:ward|defense|def)\\s*([+-]\\d+)", RegexOption.IGNORE_CASE),
            Regex("(?:crit|critical)\\s*([+-]\\d+)", RegexOption.IGNORE_CASE),
            Regex("(?:magic|mag)\\s*([+-]\\d+)", RegexOption.IGNORE_CASE),
            Regex("(?:mana|mp)\\s*([+-]\\d+)", RegexOption.IGNORE_CASE),
            Regex("(?:resistance|res)\\s*([+-]\\d+)", RegexOption.IGNORE_CASE)
        )

        // Process each screen element
        for (i in screenData.indices) {
            val currentData = screenData[i]
            val currentText = currentData.text

            debugLog("Processing element $i: '$currentText'")

            // Skip empty or very short text
            if (currentText.length < 3) continue

            // Look for item stat patterns in current text
            for (pattern in itemStatPatterns) {
                val match = pattern.find(currentText)
                if (match != null) {
                    val valueStr = match.groupValues[1]
                    try {
                        val value = valueStr.replace("+", "").toInt()
                        val statName = determineStatName(pattern, currentText)

                        if (statName != null && !attributes.containsKey(statName)) {
                            attributes[statName] = value
                            debugLog("✅ Found item stat: $statName = $value from '$currentText'")
                        }
                    } catch (e: NumberFormatException) {
                        debugLog("Failed to parse number from: $valueStr")
                    }
                    break
                }
            }

            // Also check for multi-line stat blocks (look ahead/behind)
            if (i < screenData.size - 3) {
                val combinedText = listOf(
                    currentText,
                    screenData[i + 1].text,
                    screenData[i + 2].text
                ).joinToString(" ")

                for (pattern in itemStatPatterns) {
                    val match = pattern.find(combinedText)
                    if (match != null) {
                        val valueStr = match.groupValues[1]
                        try {
                            val value = valueStr.replace("+", "").toInt()
                            val statName = determineStatName(pattern, combinedText)

                            if (statName != null && !attributes.containsKey(statName)) {
                                attributes[statName] = value
                                debugLog("✅ Found combined stat: $statName = $value from multi-line")
                            }
                        } catch (e: NumberFormatException) {
                            // Continue searching
                        }
                        break
                    }
                }
            }
        }

        debugLog("=== EXTRACTION COMPLETE ===")
        debugLog("Found attributes: $attributes")

        // CRITICAL VALIDATION: Check if we're getting player totals instead of item stats
        val maxStat = attributes.values.maxOrNull() ?: 0
        if (maxStat > 800) {
            debugLog("🚨 CRITICAL: Detected player total stats instead of item stats!")
            debugLog("Max stat value: $maxStat - this is too high for individual items")
            debugLog("Current attributes: $attributes")
            debugLog("Need to implement proper item detail screen parsing")

            // Return empty to trigger fallback mechanisms
            debugLog("Returning empty attributes to prevent sending wrong data to API")
            return emptyMap()
        }

        return attributes
    }

    /**
     * Determine stat name from regex pattern and matched text
     */
    private fun determineStatName(pattern: Regex, text: String): String? {
        val lowerText = text.lowercase()
        return when {
            lowerText.contains("attack") || lowerText.contains("att") || lowerText.contains("damage") -> "Att"
            lowerText.contains("dexterity") || lowerText.contains("dex") -> "Dex"
            lowerText.contains("hp") || lowerText.contains("health") -> "HP"
            lowerText.contains("ward") || lowerText.contains("defense") || lowerText.contains("def") -> "Ward"
            lowerText.contains("crit") -> "Crit"
            lowerText.contains("magic") || lowerText.contains("mag") -> "Mag"
            lowerText.contains("mana") || lowerText.contains("mp") -> "Mana"
            lowerText.contains("resistance") || lowerText.contains("res") -> "Res"
            else -> null
        }
    }

    /**
     * Extract item-specific stats when regular parsing fails
     */
    private suspend fun extractItemSpecificStats(screenData: List<ScreenData>): Map<String, Int> {
        debugLog("Attempting item-specific stat extraction...")

        // Look for UI elements that specifically show item stats
        // These might be in a different section or format
        val itemStats = mutableMapOf<String, Int>()

        // TODO: Implement more sophisticated item stat detection
        // This might require analyzing screen layout, looking for specific UI elements,
        // or using different parsing strategies based on the game's UI

        debugLog("Item-specific extraction result: $itemStats")
        return itemStats
    }

    /**
     * Validates that extracted stats are reasonable for individual items
     */
    private suspend fun validateItemStats(stats: Map<String, Int>, itemName: String): Boolean {
        if (stats.isEmpty()) return false

        val maxStat = stats.values.maxOrNull() ?: 0
        val minStat = stats.values.minOrNull() ?: 0

        // Individual items should not have stats > 1000 (suggests player totals)
        if (maxStat > 1000) {
            debugLog("❌ Stats validation failed: max stat $maxStat too high for item $itemName")
            return false
        }

        debugLog("✅ Stats validation passed for $itemName: max=$maxStat, min=$minStat")
        return true
    }

    /**
     * Enhanced debugging for stat extraction issues
     */
    private fun debugStatExtraction(screenData: List<ScreenData>, context: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "=== STAT EXTRACTION DEBUG: $context ===")

            screenData.forEachIndexed { index, data ->
                val text = data.text
                if (text.isNotBlank()) {
                    // Look for numbers that might be stats
                    val numbers = Regex("\\d+").findAll(text).map { it.value.toInt() }.toList()
                    if (numbers.isNotEmpty()) {
                        Log.d(TAG, "[$index] '$text' -> numbers: $numbers")

                        // Flag suspiciously high numbers
                        val maxNum = numbers.maxOrNull() ?: 0
                        if (maxNum > 500) {
                            Log.w(TAG, "  ⚠️ Suspiciously high number: $maxNum")
                        }
                    }
                }
            }

            Log.d(TAG, "=== END DEBUG ===")
        }
    }

    /**
     * Detect if we're looking at character stats vs item stats
     */
    private fun isCharacterStatsScreen(screenData: List<ScreenData>): Boolean {
        val allText = screenData.joinToString(" ") { it.text }.lowercase()

        // Indicators that suggest we're on character stats screen
        val characterIndicators = listOf(
            "character", "profile", "stats", "level", "class", "exp", "experience"
        )

        val itemIndicators = listOf(
            "equip", "unequip", "enhance", "upgrade", "slots", "materials"
        )

        return characterIndicators.any { allText.contains(it) } && 
               !itemIndicators.any { allText.contains(it) }
    }
}
