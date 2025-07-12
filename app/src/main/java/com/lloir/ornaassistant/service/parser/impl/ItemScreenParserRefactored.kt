package com.lloir.ornaassistant.service.parser.impl

import android.util.Log
import com.lloir.ornaassistant.domain.model.AssessmentResult
import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.domain.model.ScreenData
import com.lloir.ornaassistant.domain.repository.OrnaItemRepository
import com.lloir.ornaassistant.domain.usecase.AssessItemUseCase
import com.lloir.ornaassistant.service.parser.BaseScreenParser
import com.lloir.ornaassistant.utils.Constants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * Example of how ItemScreenParser could be refactored to extend BaseScreenParser
 * This is a simplified version that shows the structure but doesn't include all the details
 */
@Singleton
class ItemScreenParserRefactored @Inject constructor(
    private val assessItemUseCase: AssessItemUseCase,
    private val ornaItemRepository: OrnaItemRepository
) : BaseScreenParser() {

    private val parserScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // State for tracking current assessment
    private val _currentAssessment = MutableStateFlow<AssessmentResult?>(null)
    val currentAssessment: StateFlow<AssessmentResult?> = _currentAssessment.asStateFlow()

    private val _currentItemName = MutableStateFlow<String?>(null)
    val currentItemName: StateFlow<String?> = _currentItemName.asStateFlow()

    private val _currentItemLevel = MutableStateFlow<Int?>(null)
    val currentItemLevel: StateFlow<Int?> = _currentItemLevel.asStateFlow()

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
            // ... other invalid names
        )

        // UI element patterns to exclude
        private val UI_ELEMENT_PATTERNS = listOf(
            Regex("^\\d+$"), // Pure numbers
            // ... other patterns
        )

        // Common UI elements/sections to skip
        private val UI_SECTIONS = setOf(
            "adornments", "stats", "attributes", "requirements", "effects", "abilities",
            // ... other sections
        )
    }

    /**
     * Implementation of BaseScreenParser.canParse
     * Check if this parser can handle the given screen data
     */
    override fun canParse(data: List<ScreenData>): Boolean {
        // Check if the screen contains item data
        val itemName = extractItemName(data)
        val level = extractLevel(data)
        val attributes = extractAttributes(data)

        return itemName != null && level != null && attributes.isNotEmpty()
    }

    /**
     * Implementation of BaseScreenParser.doParse
     * Perform the actual parsing
     */
    override suspend fun doParse(parsedScreen: ParsedScreen) {
        val itemName = extractItemName(parsedScreen.data)
        val level = extractLevel(parsedScreen.data)
        val attributes = extractAttributes(parsedScreen.data)

        // Quick validation (should be redundant after canParse, but good for safety)
        if (itemName == null || level == null || attributes.isEmpty()) {
            clearCurrentAssessment()
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
            debugLog("Using cached assessment for: $itemName")
            _currentAssessment.value = cachedResult.result
            return
        }

        // Mark as processing and start assessment
        if (isProcessing.compareAndSet(false, true)) {
            startAssessment(itemName, level, attributes, cacheKey)
        } else {
            debugLog("Already processing, skipping: $itemName")
        }
    }

    /**
     * Implementation of BaseScreenParser.getParserName
     * Get the name of this parser for logging
     */
    override fun getParserName(): String = "ItemScreen"

    /**
     * Check if this item should be processed
     */
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
                debugLog("Skipping duplicate processing of: $itemName (cooldown)")
                false
            }
            // Process if enough time has passed
            else -> {
                lastProcessedTime = currentTime
                true
            }
        }
    }

    /**
     * Start the assessment process
     */
    private fun startAssessment(itemName: String, level: Int, attributes: Map<String, Int>, cacheKey: String) {
        // Cancel any existing assessment job
        currentAssessmentJob?.cancel()

        currentAssessmentJob = parserScope.launch {
            try {
                debugLog("Starting assessment for: $itemName (level $level)")

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

                debugLog("Assessment completed for: $itemName, quality: ${result.quality}")

            } catch (e: CancellationException) {
                debugLog("Assessment cancelled for: $itemName")
            } catch (e: Exception) {
                Log.e(TAG, "Assessment failed for: $itemName", e)
                _currentAssessment.value = null
            } finally {
                isProcessing.set(false)
            }
        }
    }

    /**
     * Create a cache key for the assessment
     */
    private fun createCacheKey(itemName: String, level: Int, attributes: Map<String, Int>): String {
        return "$itemName:$level:${attributes.hashCode()}"
    }

    /**
     * Clean up expired cache entries
     */
    private fun cleanupExpiredCache() {
        val expiredKeys = assessmentCache.entries
            .filter { it.value.isExpired() }
            .map { it.key }

        expiredKeys.forEach { assessmentCache.remove(it) }
    }

    /**
     * Clear the current assessment state
     */
    private fun clearCurrentAssessment() {
        if (_currentItemName.value != null) {
            _currentItemName.value = null
            _originalItemName.value = null
            _currentAssessment.value = null
        }
    }

    /**
     * Extract the item name from screen data
     * This is a simplified version of the original method
     */
    private fun extractItemName(screenData: List<ScreenData>): String? {
        // Use helper methods from BaseScreenParser
        val nameElements = findTextElements(screenData) { text ->
            text.length > 3 && 
            !INVALID_ITEM_NAMES.any { invalid -> text.contains(invalid, ignoreCase = true) } &&
            !UI_ELEMENT_PATTERNS.any { pattern -> pattern.matches(text) } &&
            !UI_SECTIONS.any { section -> text.equals(section, ignoreCase = true) }
        }

        // Process the first valid name found
        return nameElements.firstOrNull()?.text?.let { processItemName(it) }
    }

    /**
     * Process the raw item name
     * This is a simplified version of the original method
     */
    private fun processItemName(rawName: String): String? {
        // Clean the text using BaseScreenParser helper
        val cleanedName = cleanText(rawName)

        // Remove quality prefixes
        val withoutQuality = Constants.ITEM_QUALITY_PREFIXES.fold(cleanedName) { name, prefix ->
            if (name.startsWith(prefix, ignoreCase = true)) {
                name.substring(prefix.length).trim()
            } else {
                name
            }
        }

        // Remove enchantment prefixes
        val withoutEnchantment = Constants.ENCHANTMENT_PREFIXES.fold(withoutQuality) { name, prefix ->
            if (name.startsWith(prefix, ignoreCase = true)) {
                name.substring(prefix.length).trim()
            } else {
                name
            }
        }

        // Final validation
        return if (withoutEnchantment.length >= 3 && 
                  !INVALID_ITEM_NAMES.any { banned -> withoutEnchantment.equals(banned, ignoreCase = true) }) {
            withoutEnchantment
        } else {
            null
        }
    }

    /**
     * Extract the item level from screen data
     * This is a simplified version of the original method
     */
    private fun extractLevel(screenData: List<ScreenData>): Int? {
        // Find level text using BaseScreenParser helper
        val levelElements = findTextContaining(screenData, "level", true)
        
        // Extract the number using BaseScreenParser helper
        return levelElements.firstOrNull()?.text?.let { extractNumber(it) } ?: 1
    }

    /**
     * Extract adornment values from attributes
     * This is a simplified version of the original method
     */
    private fun extractAdornmentValues(attributes: Map<String, Int>): Map<String, Int> {
        // Implementation details omitted for brevity
        return emptyMap()
    }

    /**
     * Extract attributes from screen data
     * This is a simplified version of the original method
     */
    private fun extractAttributes(screenData: List<ScreenData>): Map<String, Int> {
        // Implementation details omitted for brevity
        return emptyMap()
    }
}