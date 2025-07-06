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

    private val _originalItemName = MutableStateFlow<String?>(null)

    // State for tracking adornment warnings
    private val _adornmentWarning = MutableStateFlow<AdornmentWarning?>(null)
    val adornmentWarning: StateFlow<AdornmentWarning?> = _adornmentWarning.asStateFlow()

    // Store adornment values for the current item
    private val adornmentValues = mutableMapOf<String, Int>()

    // Data class for adornment warnings
    data class AdornmentWarning(
        val slotsUsed: Int,
        val slotsTotal: Int,
        val visibleAdornments: Int
    )

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
                    _originalItemName.value = null
                    _currentAssessment.value = null
                }
                return
            }

            // Store the original item name from the screen data
            val originalItemName = parsedScreen.data.find { it.text.contains(itemName) }?.text ?: itemName

            // Check if this is a new item or we should skip processing
            if (!shouldProcessItem(itemName)) {
                return
            }

            // Update current item name immediately for overlay
            _currentItemName.value = itemName
            _originalItemName.value = originalItemName

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

                // Use the stored original item name if available
                val originalItemName = _originalItemName.value ?: itemName

                // Extract adornment values from the screen data
                val adornmentValues = extractAdornmentValues(attributes)

                val result = assessItemUseCase(
                    itemName = itemName,
                    level = level,
                    attributes = attributes,
                    originalItemName = originalItemName,
                    adornmentValues = adornmentValues
                )

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
        Log.d(TAG, "Clearing current assessment")
        currentAssessmentJob?.cancel()
        _currentAssessment.value = null
        _currentItemName.value = null
        _adornmentWarning.value = null
        lastProcessedItem.set(null)
        isProcessing.set(false)
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
            Log.d(TAG, "Using first candidate: ${firstCandidate.text}")
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

    private fun extractAdornmentValues(attributes: Map<String, Int>): Map<String, Int> {
        // This method is called during assessment to get adornment values
        // that were previously extracted during screen parsing
        return adornmentValues.toMap()
    }

    private fun extractAttributes(screenData: List<ScreenData>): Map<String, Int> {
        val attributes = mutableMapOf<String, Int>()
        adornmentValues.clear() // Clear previous adornment values
        val acceptedAttributes = listOf("Att", "Mag", "Def", "Res", "Dex", "Crit", "Mana", "Ward", "HP")
        var isAdornmentSection = false
        var hasAdornments = false
        var adornmentSlotsUsed = 0
        var adornmentSlotsTotal = 0

        // First pass: check for adornment slots
        screenData.forEach { item ->
            // Look for SLOTS pattern like "SLOTS 7/7" or "SLOTS 0/10"
            val slotsMatch = Regex("SLOTS\\s*(\\d+)/(\\d+)").find(item.text)
            if (slotsMatch != null && slotsMatch.groups.size == 3) {
                val used = slotsMatch.groups[1]?.value?.toIntOrNull() ?: 0
                val total = slotsMatch.groups[2]?.value?.toIntOrNull() ?: 0

                adornmentSlotsUsed = used
                adornmentSlotsTotal = total

                if (used > 0) {
                    hasAdornments = true
                    Log.d(TAG, "Detected adornments: $used/$total slots used")
                }
            }
        }

        // Second pass: extract attributes
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
                        // Store adornment values separately instead of subtracting
                        adornmentValues[attName] = (adornmentValues[attName] ?: 0) + attVal
                    } else {
                        attributes[attName] = attVal
                    }
                }
            }
        }

        // If we have adornments but haven't seen all of them, show a warning
        if (hasAdornments && adornmentValues.isEmpty()) {
            Log.w(TAG, "⚠️ Adornments detected ($adornmentSlotsUsed/$adornmentSlotsTotal) but none visible in current view")
            // Set adornment warning to show in overlay
            _adornmentWarning.value = AdornmentWarning(
                slotsUsed = adornmentSlotsUsed,
                slotsTotal = adornmentSlotsTotal,
                visibleAdornments = 0
            )
        }

        // If we have adornments but haven't seen all of them based on slot count
        if (hasAdornments && adornmentValues.size < adornmentSlotsUsed) {
            Log.w(TAG, "⚠️ Not all adornments visible: found ${adornmentValues.size} of $adornmentSlotsUsed")
            // Set adornment warning to show in overlay
            _adornmentWarning.value = AdornmentWarning(
                slotsUsed = adornmentSlotsUsed,
                slotsTotal = adornmentSlotsTotal,
                visibleAdornments = adornmentValues.size
            )
        } else if (hasAdornments) {
            // All adornments are visible, clear the warning
            _adornmentWarning.value = null
        }

        // Log the adornment values for debugging
        if (adornmentValues.isNotEmpty()) {
            Log.d(TAG, "Adornment values: $adornmentValues")
        }

        // Return the base attributes without modifying them
        return attributes
    }
}
