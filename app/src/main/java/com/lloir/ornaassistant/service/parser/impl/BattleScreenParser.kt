package com.lloir.ornaassistant.service.parser.impl

import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.domain.model.ScreenData
import com.lloir.ornaassistant.service.parser.BaseScreenParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parser for battle screens in Orna
 * Detects when the player enters or exits a battle
 */
@Singleton
class BattleScreenParser @Inject constructor() : BaseScreenParser() {

    private val _inBattle = MutableStateFlow(false)
    val inBattle: StateFlow<Boolean> = _inBattle.asStateFlow()

    private val _lastBattleTime = MutableStateFlow<LocalDateTime?>(null)
    val lastBattleTime: StateFlow<LocalDateTime?> = _lastBattleTime.asStateFlow()

    /**
     * Check if this parser can handle the given screen data
     */
    override fun canParse(data: List<ScreenData>): Boolean {
        // We always check battle screens, regardless of content
        return true
    }

    /**
     * Perform the actual parsing
     */
    override suspend fun doParse(parsedScreen: ParsedScreen) {
        val isInBattle = isBattleScreen(parsedScreen.data)

        if (isInBattle && !_inBattle.value) {
            _inBattle.value = true
            _lastBattleTime.value = LocalDateTime.now()
            debugLog("Entered battle")
        } else if (!isInBattle && _inBattle.value) {
            _inBattle.value = false
            debugLog("Exited battle")
        }
    }

    /**
     * Get the name of this parser for logging
     */
    override fun getParserName(): String = "Battle"

    /**
     * Check if the current screen is a battle screen
     */
    private fun isBattleScreen(screenData: List<ScreenData>): Boolean {
        // Use helper methods from BaseScreenParser
        return findTextElements(screenData) { it == "Codex" }.isNotEmpty() &&
               findTextElements(screenData) { it == "SKILL" }.isNotEmpty()
    }
}
