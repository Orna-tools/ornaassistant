package com.lloir.ornaassistant.service.parser

import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.domain.model.ScreenType
import com.lloir.ornaassistant.service.parser.impl.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenParserManager @Inject constructor(
    private val dungeonParser: DungeonScreenParser,
    private val itemParser: ItemScreenParser,
    private val wayvesselParser: WayvesselScreenParser,
    private val notificationParser: NotificationScreenParser,
    private val battleParser: BattleScreenParser
) {
    private val parserScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun processScreen(parsedScreen: ParsedScreen) {
        parserScope.launch {
            when (parsedScreen.screenType) {
                ScreenType.DUNGEON_ENTRY -> dungeonParser.parseScreen(parsedScreen)
                ScreenType.ITEM_DETAIL -> itemParser.parseScreen(parsedScreen)
                ScreenType.WAYVESSEL -> wayvesselParser.parseScreen(parsedScreen)
                ScreenType.NOTIFICATIONS -> notificationParser.parseScreen(parsedScreen)
                ScreenType.BATTLE -> battleParser.parseScreen(parsedScreen)
                ScreenType.INVENTORY -> { /* Handle inventory if needed */ }
                ScreenType.UNKNOWN -> { /* No specific handling needed */ }
            }
        }
    }
}

// Base Screen Parser Interface
package com.lloir.ornaassistant.service.parser

import com.lloir.ornaassistant.domain.model.ParsedScreen

interface ScreenParser {
    suspend fun parseScreen(parsedScreen: ParsedScreen)
}

// Example implementation for Dungeon Screen Parser
package com.lloir.ornaassistant.service.parser.impl

import android.util.Log
import com.lloir.ornaassistant.domain.model.*
import com.lloir.ornaassistant.domain.usecase.*
import com.lloir.ornaassistant.service.parser.ScreenParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DungeonScreenParser @Inject constructor(
    private val trackDungeonVisitUseCase: TrackDungeonVisitUseCase,
    private val updateDungeonVisitUseCase: UpdateDungeonVisitUseCase
) : ScreenParser {

    private val _currentDungeonVisit = MutableStateFlow<DungeonVisit?>(null)
    val currentDungeonVisit: StateFlow<DungeonVisit?> = _currentDungeonVisit.asStateFlow()

    companion object {
        private const val TAG = "DungeonScreenParser"
    }

    override suspend fun parseScreen(parsedScreen: ParsedScreen) {
        try {
            val dungeonName = extractDungeonName(parsedScreen.data)
            val dungeonMode = extractDungeonMode(parsedScreen.data)
            val floor = extractFloor(parsedScreen.data)
            val loot = extractLoot(parsedScreen.data)

            // Check if we're entering a new dungeon
            if (dungeonName != null && _currentDungeonVisit.value?.name != dungeonName) {
                val visit = trackDungeonVisitUseCase(dungeonName, dungeonMode)
                _currentDungeonVisit.value = visit
                Log.d(TAG, "Started tracking dungeon visit: $dungeonName")
            }

            // Update current visit with new data
            _currentDungeonVisit.value?.let { currentVisit ->
                if (loot.isNotEmpty() || floor != null) {
                    updateDungeonVisitUseCase(
                        visit = currentVisit,
                        orns = loot["orns"]?.toLongOrNull(),
                        gold = loot["gold"]?.toLongOrNull(),
                        experience = loot["experience"]?.toLongOrNull(),
                        floor = floor,
                        completed = isVictoryScreen(parsedScreen.data)
                    )
                }

                // Check for completion or failure
                if (isCompletedScreen(parsedScreen.data)) {
                    _currentDungeonVisit.value = null
                    Log.d(TAG, "Dungeon visit completed")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing dungeon screen", e)
        }
    }

    private fun extractDungeonName(screenData: List<ScreenData>): String? {
        // Look for dungeon name patterns
        val texts = screenData.map { it.text }

        return texts.find { text ->
            text.contains("Dungeon", ignoreCase = true) ||
                    text.contains("Valley", ignoreCase = true) ||
                    text.contains("Battle", ignoreCase = true) ||
                    text.contains("Dragon", ignoreCase = true) ||
                    text.contains("Underworld", ignoreCase = true) ||
                    text.contains("Chaos", ignoreCase = true)
        }
    }

    private fun extractDungeonMode(screenData: List<ScreenData>): DungeonMode {
        val texts = screenData.map { it.text.lowercase() }

        val isHard = texts.any { it.contains("hard") && it.contains("✓") }

        val type = when {
            texts.any { it.contains("boss") && it.contains("✓") } -> DungeonMode.Type.BOSS
            texts.any { it.contains("endless") && it.contains("✓") } -> DungeonMode.Type.ENDLESS
            else -> DungeonMode.Type.NORMAL
        }

        return DungeonMode(type, isHard)
    }

    private fun extractFloor(screenData: List<ScreenData>): Long? {
        return screenData.find { it.text.contains("Floor:", ignoreCase = true) }
            ?.text
            ?.let { text ->
                Regex("Floor:\\s*(\\d+)").find(text)?.groupValues?.get(1)?.toLongOrNull()
            }
    }

    private fun extractLoot(screenData: List<ScreenData>): Map<String, String> {
        val loot = mutableMapOf<String, String>()
        var lastNumber: String? = null

        screenData.forEach { data ->
            val text = data.text.trim()

            // Check if this is a number
            if (text.matches(Regex("\\d+,?\\d*"))) {
                lastNumber = text.replace(",", "")
            } else if (lastNumber != null) {
                // Check if this follows a number and is a loot type
                when {
                    text.contains("orns", ignoreCase = true) -> loot["orns"] = lastNumber!!
                    text.contains("gold", ignoreCase = true) -> loot["gold"] = lastNumber!!
                    text.contains("experience", ignoreCase = true) -> loot["experience"] = lastNumber!!
                }
                lastNumber = null
            }
        }

        return loot
    }

    private fun isVictoryScreen(screenData: List<ScreenData>): Boolean {
        return screenData.any { it.text.contains("victory", ignoreCase = true) }
    }

    private fun isCompletedScreen(screenData: List<ScreenData>): Boolean {
        return screenData.any {
            it.text.contains("complete", ignoreCase = true) ||
                    it.text.contains("defeat", ignoreCase = true)
        }
    }
}