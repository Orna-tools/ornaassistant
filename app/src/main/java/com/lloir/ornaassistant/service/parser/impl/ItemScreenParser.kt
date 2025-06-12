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
            // Check if this is actually an item detail screen
            val isItemScreen = parsedScreen.data.any {
                it.text.contains("acquired", ignoreCase = true) ||
                        (it.text.contains("Level ", ignoreCase = false) &&
                                parsedScreen.data.any { d -> d.text.contains(":", ignoreCase = false) })
            }

            if (!isItemScreen || parsedScreen.screenType != com.lloir.ornaassistant.domain.model.ScreenType.ITEM_DETAIL) {
                clearCurrentAssessment()
                return
            }

            val itemName = extractItemName(parsedScreen.data)
            val level = extractLevel(parsedScreen.data)
            val attributes = extractAttributes(parsedScreen.data)

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
                attributes == lastExtractedAttributes) {
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
        return screenData.find { it.text.startsWith("Level ") }
            ?.text
            ?.replace("Level ", "")
            ?.toIntOrNull()
    }

    private fun extractAttributes(screenData: List<ScreenData>): Map<String, Int> {
        val attributes = mutableMapOf<String, Int>()
        val acceptedAttributes = setOf("Att", "Mag", "Def", "Res", "Dex", "Crit", "Mana", "Ward", "HP")
        var isAdornmentSection = false

        screenData.forEach { item ->
            // Check if we're in adornments section
            if (item.text.uppercase().contains("ADORNMENTS")) {
                isAdornmentSection = true
                return@forEach
            }

            // Try to parse attribute patterns
            val cleanText = item.text
                .replace("−", "-")
                .replace(" ", "")
                .replace(",", "")

            // Match patterns like "Att: 123" or "Att:123" or "Att 123"
            val patterns = listOf(
                Regex("([A-Za-z]+):\\s*(-?\\d+)"),
                Regex("([A-Za-z]+)\\s+(-?\\d+)"),
                Regex("([A-Za-z]+)(-?\\d+)")
            )

            for (pattern in patterns) {
                val match = pattern.find(cleanText)
                if (match != null && match.groups.size >= 3) {
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
                        break
                    }
                }
            }
        }

        Log.d(TAG, "Extracted attributes: $attributes")
        return attributes
    }
}