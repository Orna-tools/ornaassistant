package com.lloir.ornaassistant.service.parser.impl

import android.util.Log
import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.service.parser.ScreenParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BattleScreenParser @Inject constructor() : ScreenParser {

    private val _inBattle = MutableStateFlow(false)
    val inBattle: StateFlow<Boolean> = _inBattle.asStateFlow()

    private val _lastBattleTime = MutableStateFlow<LocalDateTime?>(null)
    val lastBattleTime: StateFlow<LocalDateTime?> = _lastBattleTime.asStateFlow()

    companion object {
        private const val TAG = "BattleScreenParser"
    }

    override suspend fun parseScreen(parsedScreen: ParsedScreen) {
        try {
            val isInBattle = isBattleScreen(parsedScreen.data)

            if (isInBattle && !_inBattle.value) {
                _inBattle.value = true
                _lastBattleTime.value = LocalDateTime.now()
                Log.d(TAG, "Entered battle")
            } else if (!isInBattle && _inBattle.value) {
                _inBattle.value = false
                Log.d(TAG, "Exited battle")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing battle screen", e)
        }
    }

    private fun isBattleScreen(screenData: List<com.lloir.ornaassistant.domain.model.ScreenData>): Boolean {
        return screenData.any { it.text == "Codex" } &&
                screenData.any { it.text == "SKILL" }
    }
}