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
    private var lastExtractedItemName: String? = null
    private var lastExtractedLevel: Int? = null
    private var lastExtractedAttributes: Map<String, Int>? = null

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

        // Simplified invalid item names list - only actual UI elements
        private val INVALID_ITEM_NAMES = setOf(
            "acquired", "send to keep", "inventory", "codex", "runeshop", "options",
            "party", "wayvessel", "notifications", "settings", "shop", "store",
            "level", "tier", "stats", "attributes", "adornments", "description"
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
            Regex("^[a-z_]+$"), // All lowercase with underscores (resource names)
            Regex(".*_(icon|image|img|sprite|texture).*", RegexOption.IGNORE_CASE)
        )
    }

    override suspend fun parseScreen(parsedScreen: ParsedScreen) {
        try {
            // Only process ITEM_DETAIL screens
            if (parsedScreen.screenType != com.lloir.ornaassistant.domain.model.ScreenType.ITEM_DETAIL) {
                Log.d(TAG, "Not an item detail screen (type=${parsedScreen.screenType}), clearing assessment")
                clearCurrentAssessment()
                return
            }

            // Debug: Log all screen data for item screens
            Log.d(TAG, "Processing item detail screen with ${parsedScreen.data.size} elements:")
            parsedScreen.data.take(20).forEach { data ->
                Log.d(TAG, "  '${data.text}'")
            }
            if (parsedScreen.data.size > 20) {
                Log.d(TAG, "  ... and ${parsedScreen.data.size - 20} more elements")
            }

            // Check if this is actually an item detail screen
            val isItemScreen = parsedScreen.data.any {
                it.text.contains("acquired", ignoreCase = true) ||
                        (it.text.contains("Level ", ignoreCase = false) &&
                                parsedScreen.data.any { d ->
                                    d.text.contains(
                                        ":",
                                        ignoreCase = false
                                    )
                                })
            }

            if (!isItemScreen) {
                Log.d(TAG, "Screen doesn't contain item detail markers, skipping")
                clearCurrentAssessment()
                return
            }

            val itemName = extractItemName(parsedScreen.data)
            val level = extractLevel(parsedScreen.data)
            val attributes = extractAttributes(parsedScreen.data)

            // Debug logging
            Log.d(TAG, "Parsed item data:")
            Log.d(TAG, "  Item: $itemName")
            Log.d(TAG, "  Level: $level")
            Log.d(TAG, "  Attributes: $attributes")

            // Quick validation
            if (itemName == null || level == null || attributes.isEmpty()) {
                // Clear current state if no valid item found
                if (_currentItemName.value != null) {
                    Log.d(TAG, "Clearing state - no item name found")
                    _currentItemName.value = null
                    _currentAssessment.value = null
                    lastExtractedItemName = null
                    lastExtractedLevel = null
                    lastExtractedAttributes = null
                }
                return
            }

            // Check if this is the exact same item data we just processed
            if (itemName == lastExtractedItemName &&
                level == lastExtractedLevel &&
                attributes == lastExtractedAttributes
            ) {
                // Same item, don't reprocess
                return
            }

            // Update last extracted data
            lastExtractedItemName = itemName
            lastExtractedLevel = level
            lastExtractedAttributes = attributes

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
                // Validate cached result before using
                if (cachedResult.result.quality > 0 &&
                    !hasNegativeStats(cachedResult.result)
                ) {
                    Log.d(TAG, "Using valid cached assessment for: $itemName")
                    _currentAssessment.value = cachedResult.result
                } else {
                    Log.d(
                        TAG,
                        "Invalid cached result (quality=${cachedResult.result.quality}), removing from cache"
                    )
                    assessmentCache.remove(cacheKey)
                    // Continue to reassess
                    if (isProcessing.compareAndSet(false, true)) {
                        startAssessment(itemName, level, attributes, cacheKey)
                    }
                }
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

    private fun startAssessment(
        itemName: String,
        level: Int,
        attributes: Map<String, Int>,
        cacheKey: String
    ) {
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
        Log.d(TAG, "Clearing current assessment")
        currentAssessmentJob?.cancel()
        _currentAssessment.value = null
        _currentItemName.value = null
        lastProcessedItem.set(null)
        lastExtractedItemName = null
        lastExtractedLevel = null
        lastExtractedAttributes = null
        isProcessing.set(false)
    }

    // Add this method to clear all cache
    fun clearAssessmentCache() {
        assessmentCache.clear()
        Log.d(TAG, "Cleared all assessment cache")
    }

    // Add this helper method
    private fun hasNegativeStats(result: AssessmentResult): Boolean {
        return result.stats.any { (_, values) ->
            values.any { value ->
                value.toIntOrNull()?.let { it < 0 } ?: false
            }
        }
    }

    private fun extractItemName(screenData: List<ScreenData>): String? {
        // Strategy 1: Find "X acquired!" pattern
        val acquiredPattern = screenData.find {
            it.text.contains("acquired!", ignoreCase = true)
        }?.text

        if (acquiredPattern != null) {
            val itemName = acquiredPattern.replace(" acquired!", "", ignoreCase = true).trim()
            if (itemName.isNotBlank()) {
                return processItemName(itemName)
            }
        }

        // Strategy 2: Look for item before "Level X"
        val levelIndex = screenData.indexOfFirst { it.text.startsWith("Level ") }
        if (levelIndex > 0) {
            // Look at items before level indicator
            for (i in (levelIndex - 1) downTo maxOf(0, levelIndex - 5)) {
                val candidate = screenData[i].text
                if (isValidItemName(candidate)) {
                    return processItemName(candidate)
                }
            }
        }

        // Strategy 2.5: Look for item that appears before "Inventory" button
        val inventoryIndex =
            screenData.indexOfFirst { it.text.equals("Inventory", ignoreCase = true) }
        if (inventoryIndex > 0) {
            // The item name is often the text element right before "Inventory"
            val candidate = screenData[inventoryIndex - 1].text
            if (isValidItemName(candidate)) {
                return processItemName(candidate)
            }
        }

        // Strategy 3: Look for items with quality/enchantment prefixes
        val itemWithPrefix = screenData.find { data ->
            val text = data.text
            Constants.ITEM_QUALITY_PREFIXES.any { prefix ->
                text.startsWith(prefix, ignoreCase = true)
            } || Constants.ENCHANTMENT_PREFIXES.any { prefix ->
                text.lowercase().startsWith(prefix.lowercase())
            }
        }

        if (itemWithPrefix != null) {
            return processItemName(itemWithPrefix.text)
        }

        // Strategy 4: Find first valid item name that's not a UI element
        val validItems = screenData
            .filter { it.text.isNotBlank() && it.text.length >= 3 }
            .filterNot { data ->
                INVALID_ITEM_NAMES.any { invalid ->
                    data.text.equals(invalid, ignoreCase = true)
                }
            }
            .filterNot { data ->
                UI_ELEMENT_PATTERNS.any { pattern ->
                    pattern.matches(data.text)
                }
            }
            .filter { data ->
                isValidItemName(data.text)
            }

        return validItems.firstOrNull()?.let { processItemName(it.text) }
    }

    private fun isValidItemName(text: String): Boolean {
        return text.isNotBlank() &&
                text.length >= 3 &&
                text.length <= 50 &&
                text.first().isUpperCase() &&
                !UI_ELEMENT_PATTERNS.any { it.matches(text) } &&
                text.count { it.isLetter() } >= text.length / 2
    }

    private fun processItemName(rawName: String): String? {
        var processedName = rawName.trim()

        // Remove quality prefixes
        Constants.ITEM_QUALITY_PREFIXES.forEach { prefix ->
            if (processedName.startsWith(prefix, ignoreCase = true)) {
                processedName = processedName.removePrefix(prefix).trim()
            }
        }
        processedName = processedName.removeSuffix("!").trim()

        // Remove enchantment prefixes
        Constants.ENCHANTMENT_PREFIXES.forEach { prefix ->
            val capitalizedPrefix = prefix.replaceFirstChar { it.uppercase() }
            if (processedName.startsWith(capitalizedPrefix)) {
                processedName = processedName.removePrefix("$capitalizedPrefix ").trim()
            }
        }

        // Final validation
        if (processedName.isBlank() || processedName.length < 3) {
            return null
        }

        Log.d(TAG, "Extracted item name: '$processedName' from original: '$rawName'")
        return processedName
    }

    private fun extractLevel(screenData: List<ScreenData>): Int? {
        // Look for "Level X" pattern
        screenData.forEach { data ->
            val text = data.text.trim()

            // Pattern 1: "Level 6"
            if (text.startsWith("Level ", ignoreCase = true)) {
                val levelStr = text.substring(6).trim()
                val level = levelStr.toIntOrNull()
                if (level != null) {
                    Log.d(TAG, "Found level: $level from text: '$text'")
                    return level
                }
            }

            // Pattern 2: Just a number between 1-10 after item name
            if (text.matches(Regex("^([1-9]|10)$"))) {
                val index = screenData.indexOf(data)
                // Check context - should be after item name and before stats
                if (index > 0 && index < screenData.size - 1) {
                    val prevText = screenData[index - 1].text
                    val nextText =
                        if (index + 1 < screenData.size) screenData[index + 1].text else ""

                    // Common pattern: item name, then level number, then quality
                    if (!prevText.contains(":") && !prevText.matches(Regex("^\\(.*\\)$"))) {
                        val level = text.toIntOrNull()
                        if (level != null) {
                            Log.d(TAG, "Found level (standalone number): $level")
                            return level
                        }
                    }
                }
            }
        }

        Log.d(TAG, "No level found in screen data")
        return null
    }

    private fun extractAttributes(screenData: List<ScreenData>): Map<String, Int> {
        val attributes = mutableMapOf<String, Int>()
        val acceptedAttributes =
            setOf("Att", "Mag", "Def", "Res", "Dex", "Crit", "Mana", "Ward", "HP")

        Log.d(TAG, "extractAttributes: Processing ${screenData.size} items")

        for (item in screenData) {
            val text = item.text.trim()

            // Handle stats with adornments FIRST (e.g., "Att: 1,410 (+1,026)")
            if (text.contains(":") && text.contains("(") && text.contains(")")) {
                try {
                    val regex = Regex("([A-Za-z]+):\\s*([\\d,]+)\\s*\\(([+-][\\d,]+)\\)")
                    val match = regex.find(text)

                    if (match != null) {
                        val statName = match.groupValues[1].trim()
                        val totalStr = match.groupValues[2].replace(",", "")
                        val adornStr = match.groupValues[3].replace(",", "")

                        if (acceptedAttributes.contains(statName)) {
                            val total = totalStr.toIntOrNull() ?: 0
                            val adorn = adornStr.toIntOrNull() ?: 0
                            val base = total - adorn

                            if (base > 0) {
                                attributes[statName] = base
                                Log.d(
                                    TAG,
                                    "Parsed $statName: base=$base (total=$total, adorn=$adorn)"
                                )
                            }
                        }
                        continue
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing adornment stat: $text", e)
                }
            }

            // Check if we're in adornments section
            if (text.uppercase().contains("ADORNMENTS")) {
                Log.d(TAG, "Reached ADORNMENTS section, stopping attribute extraction")
                break
            }

            // Skip parenthetical values like "(+289)" or "(-18)"
            if (text.startsWith("(") && text.endsWith(")")) {
                continue
            }

            // Look for simple attribute patterns without adornments
            // Pattern: "HP: 97" or "Mana: 289" (no parentheses)
            if (!text.contains("(")) {
                val colonPattern = Regex("([A-Za-z]+):\\s*([\\d,]+)")
                colonPattern.find(text)?.let { match ->
                    val attName = match.groupValues[1].trim()
                    val attValStr = match.groupValues[2].replace(",", "")
                    val attVal = attValStr.toIntOrNull()

                    if (attName != null && attVal != null && attVal > 0 && acceptedAttributes.contains(
                            attName
                        )
                    ) {
                        // Only add if we haven't already processed this stat
                        if (!attributes.containsKey(attName)) {
                            attributes[attName] = attVal
                            Log.d(TAG, "Found simple attribute: $attName = $attVal")
                        }
                    }
                }
            }
        }

        Log.d(TAG, "Extracted attributes: $attributes")
        return attributes
    }
}