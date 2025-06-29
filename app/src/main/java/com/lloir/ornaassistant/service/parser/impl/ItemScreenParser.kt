package com.lloir.ornaassistant.service.parser.impl

import android.util.Log
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

    private fun extractAttributes(screenData: List<ScreenData>): Map<String, Int> {
        Log.d(TAG, "=== EXTRACTING ATTRIBUTES ===")
        val attributes = mutableMapOf<String, Int>()
        val acceptedAttributes = listOf("Att", "Mag", "Def", "Res", "Dex", "Crit", "Mana", "Ward", "HP")
        // Define percentage-based stats that should not have adornments subtracted
        val percentageStats = setOf("Ward", "Crit")
        var isAdornmentSection = false
        val adornmentStats = mutableMapOf<String, Int>()
        // Track which stats had % in original text
        val isPercentageStat = mutableMapOf<String, Boolean>()

        // First pass: collect all stats and identify adornments
        screenData.forEachIndexed { index, item ->
            val text = item.text.trim()
            Log.d(TAG, "[$index] Processing: '$text'")

            // Check for adornment section markers
            if (text.contains("ADORNMENTS", ignoreCase = true) || 
                text.contains("ADORNMENT", ignoreCase = true)) {
                isAdornmentSection = true
                Log.d(TAG, "  → Entered ADORNMENTS section")
                return@forEachIndexed
            }

            // Reset adornment section when we hit other major sections
            if ((text.contains("EFFECTS", ignoreCase = true) || 
                 text.contains("ABILITIES", ignoreCase = true) ||
                 text.contains("DESCRIPTION", ignoreCase = true) ||
                 text.contains("LORE", ignoreCase = true)) && isAdornmentSection) {
                isAdornmentSection = false
                Log.d(TAG, "  → Exited ADORNMENTS section")
            }

            // Check if this stat contains a percentage
            val hasPercentage = text.contains("%")

            // Parse stat values
            val cleanText = item.text
                .replace("−", "-")
                .replace(",", "")
                .replace("%", "")
                .trim()

            // Enhanced regex to handle various stat formats
            val match = Regex("([A-Za-z]+)\\s*[:\\.\\s]\\s*([+-]?[0-9,]+)").find(cleanText)
            if (match != null && match.groups.size == 3) {
                val attName = match.groups[1]?.value?.trim()
                val attValStr = match.groups[2]?.value?.replace(",", "")?.trim()
                val attVal = attValStr?.toIntOrNull()

                Log.d(TAG, "  → Found stat: $attName = $attVal (adornment: $isAdornmentSection)")

                if (attName != null && attVal != null && acceptedAttributes.contains(attName)) {
                    // Record if this stat had a percentage
                    if (hasPercentage) {
                        isPercentageStat[attName] = true
                        Log.d(TAG, "    → Marked as percentage stat: $attName")
                    }

                    if (isAdornmentSection) {
                        // Store adornment stats separately
                        adornmentStats[attName] = (adornmentStats[attName] ?: 0) + attVal
                        Log.d(TAG, "    → Stored adornment: $attName += $attVal")
                    } else {
                        // Store base item stats
                        attributes[attName] = attVal
                        Log.d(TAG, "    → Stored base stat: $attName = $attVal")
                    }
                }
            }
        }

        // Second pass: subtract adornment bonuses from base stats to get true item stats
        adornmentStats.forEach { (statName, adornmentBonus) ->
            val currentBase = attributes[statName] ?: 0

            // Skip subtraction for percentage-based stats
            if (isPercentageStat[statName] == true || percentageStats.contains(statName)) {
                Log.d(TAG, "Skipping adornment subtraction for percentage stat: $statName")
                // Keep the base value without subtraction
            } else if (currentBase == 0 && adornmentBonus > 0) {
                // If base stat is 0 but adornment is present, it's likely the base stat wasn't detected
                // In this case, we should use the adornment value as is instead of subtracting
                Log.d(TAG, "Base stat for $statName is 0 but adornment is $adornmentBonus - using adornment value")
                attributes[statName] = adornmentBonus
            } else {
                // For non-percentage stats, subtract adornment bonuses
                val trueItemStat = currentBase - adornmentBonus
                // Ensure we don't end up with negative values for stats that should be positive
                val finalStat = if (statName != "Dex" && trueItemStat < 0) {
                    Log.d(TAG, "Warning: Negative value detected for $statName ($trueItemStat) - using original value $currentBase")
                    currentBase
                } else {
                    trueItemStat
                }
                attributes[statName] = finalStat
                Log.d(TAG, "Adjusting $statName: $currentBase - $adornmentBonus = $finalStat")
            }
        }

        Log.d(TAG, "=== FINAL EXTRACTED ATTRIBUTES ===")
        attributes.forEach { (name, value) ->
            Log.d(TAG, "$name: $value")
        }
        Log.d(TAG, "=== END ATTRIBUTE EXTRACTION ===")

        return attributes
    }
}
