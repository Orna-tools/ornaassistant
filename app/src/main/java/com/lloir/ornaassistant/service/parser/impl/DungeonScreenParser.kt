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
    
    private var currentDungeonState: DungeonState? = null
    private var onHoldVisits = mutableMapOf<String, DungeonVisit>()

    companion object {
        private const val TAG = "DungeonScreenParser"
    }
    
    fun canParse(data: List<ScreenData>): Boolean {
        return data.any { it.text.lowercase().contains("world dungeon") } ||
               data.any { it.text.lowercase().contains("special dungeon") } ||
               (data.any { it.text.startsWith("Battle a series of opponents") } && data.any { it.text == "Runeshop" })
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

    fun parseState(data: List<ScreenData>, currentState: DungeonState?): DungeonState {
        val state = currentState ?: DungeonState()
        val dungeonName = extractDungeonNameFromData(data) ?: state.dungeonName
        
        if (dungeonName.isNotEmpty() && dungeonName != state.dungeonName && state.dungeonName.isNotEmpty()) {
            return DungeonState(dungeonName = dungeonName, isEnteringNewDungeon = true)
        }
        
        var newState = state.copy(dungeonName = dungeonName)
        newState = parseFloorAndEntry(data, newState)
        newState = parseDungeonMode(data, newState)
        
        when {
            data.any { it.text.lowercase().contains("complete") } -> {
                newState = newState.copy(isDone = true)
            }
            data.any { it.text.lowercase().contains("defeat") } -> {
                newState = newState.copy(isDone = true)
            }
        }
        
        return newState
    }

    private fun extractDungeonNameFromData(data: List<ScreenData>): String? {
        var nameNext = false
        for (item in data) {
            if (nameNext) {
                return item.text
            } else if (item.text.lowercase().contains("world dungeon") ||
                       item.text.lowercase().contains("special dungeon")) {
                nameNext = true
            }
        }
        
        if (data.any { it.text.startsWith("Battle a series of opponents") } &&
            data.any { it.text == "Runeshop" }) {
            return "Personal gauntlet"
        }
        
        return null
    }
    
    private fun extractDungeonName(screenData: List<ScreenData>): String? {
        return extractDungeonNameFromData(screenData)
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
            val text = data.text.trim().replace("−", "-").replace(",", "").replace(".", "")

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

    fun parseLoot(data: List<ScreenData>): Map<String, Int> {
        val loot = mutableMapOf<String, Int>()
        var numberValue: Int? = null
        
        for (item in data) {
            if (numberValue != null) {
                when (item.text) {
                    " experience", " party experience" -> loot["experience"] = numberValue
                    " gold" -> loot["gold"] = numberValue
                    " orns" -> loot["orns"] = numberValue
                }
                numberValue = null
            }
            
            numberValue = try {
                item.text.replace(Regex("[^\\d]"), "").toInt()
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        return loot
    }

    private fun parseFloorAndEntry(data: List<ScreenData>, state: DungeonState): DungeonState {
        var newState = state
        
        newState = when {
            data.any { it.text.lowercase().contains("continue floor") } -> 
                newState.copy(isEnteringNewDungeon = false)
            data.any { it.text.lowercase().contains("hold to enter") } -> 
                newState.copy(isEnteringNewDungeon = true)
            else -> newState
        }
        
        val floorData = data.firstOrNull { it.text.lowercase().contains("floor") }
        floorData?.let {
            val match = Regex("Floor:\\s([0-9]+)\\s/\\s([0-9]+|∞)").find(it.text)
            match?.let { m ->
                val floorNumber = m.groupValues[1].toIntOrNull() ?: 1
                val hasDefeat = data.any { it.text.lowercase().contains("defeat") }
                
                if (!newState.hasEntered && floorNumber == 1 && !hasDefeat) {
                    newState = newState.copy(hasEntered = true)
                } else if (!newState.hasEntered && !newState.isEnteringNewDungeon) {
                    newState = newState.copy(hasEntered = true)
                }
                
                if (newState.hasEntered && floorNumber != newState.floorNumber) {
                    newState = newState.copy(
                        floorNumber = floorNumber,
                        victoryScreenHandledForFloor = false
                    )
                }
            }
        }
        
        return newState
    }

    private fun parseDungeonMode(data: List<ScreenData>, state: DungeonState): DungeonState {
        var modeCandidate: DungeonMode.Type? = null
        var hardCandidate = false
        var newMode = state.mode
        
        for (i in data.indices) {
            val item = data[i]
            
            if (modeCandidate != null || hardCandidate) {
                if (i + 1 < data.size && data[i + 1].text.contains("✓")) {
                    if (hardCandidate) {
                        newMode = newMode.copy(isHard = true)
                    }
                    if (modeCandidate != null) {
                        newMode = newMode.copy(type = modeCandidate)
                    }
                } else {
                    if (hardCandidate) {
                        newMode = newMode.copy(isHard = false)
                    }
                    if (modeCandidate != null && modeCandidate == newMode.type) {
                        newMode = newMode.copy(type = DungeonMode.Type.NORMAL)
                    }
                }
                modeCandidate = null
                hardCandidate = false
            } else if (item.text.lowercase().contains("mode")) {
                when (item.text.lowercase().replace(" mode", "")) {
                    "hard" -> hardCandidate = true
                    "boss" -> modeCandidate = DungeonMode.Type.BOSS
                    "endless" -> modeCandidate = DungeonMode.Type.ENDLESS
                }
            }
        }
        
        return state.copy(mode = newMode)
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