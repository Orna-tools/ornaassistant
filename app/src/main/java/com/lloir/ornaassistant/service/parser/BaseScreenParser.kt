package com.lloir.ornaassistant.service.parser

import android.util.Log
import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.domain.model.ScreenData
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Base class for screen parsers that provides common functionality
 */
abstract class BaseScreenParser : ScreenParser {

    companion object {
        private const val TAG = "BaseScreenParser"
    }

    // State flow for current parsing status
    private val _parsingStatus = MutableStateFlow<String?>(null)
    val parsingStatus: StateFlow<String?> = _parsingStatus.asStateFlow()

    /**
     * Implementation of ScreenParser.parseScreen
     * This template method defines the parsing workflow
     */
    override suspend fun parseScreen(parsedScreen: ParsedScreen) {
        if (!canParse(parsedScreen.data)) {
            return
        }

        try {
            _parsingStatus.value = "Parsing ${getParserName()} screen..."
            doParse(parsedScreen)
            _parsingStatus.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing ${getParserName()} screen", e)
            _parsingStatus.value = "Error: ${e.message}"
        }
    }

    /**
     * Check if this parser can handle the given screen data
     */
    protected abstract fun canParse(data: List<ScreenData>): Boolean

    /**
     * Perform the actual parsing
     */
    protected abstract suspend fun doParse(parsedScreen: ParsedScreen)

    /**
     * Get the name of this parser for logging
     */
    protected abstract fun getParserName(): String

    /**
     * Helper method to find text elements in screen data
     */
    protected fun findTextElements(data: List<ScreenData>, predicate: (String) -> Boolean): List<ScreenData> {
        return data.filter { it.text.isNotEmpty() && predicate(it.text) }
    }

    /**
     * Helper method to find text elements containing a specific string
     */
    protected fun findTextContaining(data: List<ScreenData>, text: String, ignoreCase: Boolean = true): List<ScreenData> {
        return findTextElements(data) { it.contains(text, ignoreCase) }
    }

    /**
     * Helper method to find text elements matching a regex pattern
     */
    protected fun findTextMatching(data: List<ScreenData>, regex: Regex): List<ScreenData> {
        return findTextElements(data) { text -> regex.matches(text) }
    }

    /**
     * Helper method to find text elements containing a regex pattern
     */
    protected fun findTextContainingPattern(data: List<ScreenData>, regex: Regex): List<ScreenData> {
        return findTextElements(data) { text -> regex.containsMatchIn(text) }
    }

    /**
     * Helper method to extract a number from text
     */
    protected fun extractNumber(text: String): Int? {
        val numberRegex = Regex("\\d+")
        val match = numberRegex.find(text)
        return match?.value?.toIntOrNull()
    }

    /**
     * Helper method to extract a long number from text
     */
    protected fun extractLongNumber(text: String): Long? {
        val numberRegex = Regex("\\d+")
        val match = numberRegex.find(text)
        return match?.value?.toLongOrNull()
    }

    /**
     * Helper method to clean up text by removing unwanted characters
     */
    protected fun cleanText(text: String): String {
        return text.trim()
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
    }

    /**
     * Helper method to log debug information
     */
    protected fun debugLog(message: String) {
        Log.d(TAG, "[${getParserName()}] $message")
    }
}
