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
        // First, look for explicit dungeon indicators
        val hasWorldDungeon = data.any { it.text.lowercase().contains("world dungeon") }
        val hasSpecialDungeon = data.any { it.text.lowercase().contains("special dungeon") }
        val hasGauntlet = data.any { it.text.startsWith("Battle a series of opponents") } && 
                         data.any { it.text == "Runeshop" }
        
        // If we found explicit dungeon text, we're definitely on a dungeon screen
        if (hasWorldDungeon || hasSpecialDungeon || hasGauntlet) {
            Log.d(TAG, "Found explicit dungeon text")
            return true
        }
        
        // Check for floor indicators (when already in dungeon) - but exclude dates
        val hasFloor = data.any { 
            val lower = it.text.lowercase()
            val text = it.text
            
            // First check if it's NOT a date pattern (DD/MM/YYYY or MM/DD/YYYY)
            val isDate = text.matches(Regex("\\d{1,2}/\\d{1,2}/\\d{4}"))
            
            if (!isDate) {
                lower.contains("floor:") || 
                lower.contains("floor ") ||
                // Only match "X / Y" pattern if it looks like floor numbers
                (text.matches(Regex("^\\d{1,3}\\s*/\\s*\\d{1,3}$")) && 
                 !text.contains("/20") && // Exclude years
                 !text.contains("/19"))
            } else {
                false
            }
        }
        
        // Check for dungeon-specific UI elements
        val hasDungeonMode = data.any { 
            val lower = it.text.lowercase()
            lower.contains("normal mode") || lower.contains("hard mode") || 
            lower.contains("boss mode") || lower.contains("endless mode")
        }
        val hasEnterButton = data.any { it.text.lowercase().contains("hold to enter") }
        val hasContinueFloor = data.any { it.text.lowercase().contains("continue floor") }
        
        // Check for battle/victory screens in dungeons
        val hasVictory = data.any { it.text.lowercase().contains("victory") }
        val hasComplete = data.any { it.text.lowercase().contains("complete") }
        val hasDefeat = data.any { it.text.lowercase().contains("defeat") }
        
        // If we have floor info AND other dungeon indicators, that's a strong indicator we're in a dungeon
        val result = (hasFloor && data.any { it.text.lowercase().contains("exit") }) ||
                    hasDungeonMode || hasEnterButton || hasContinueFloor ||
                    ((hasVictory || hasComplete || hasDefeat) && hasFloor)
        
        Log.d(TAG, "canParse result: $result (world: $hasWorldDungeon, special: $hasSpecialDungeon, " +
                "gauntlet: $hasGauntlet, floor: $hasFloor, mode: $hasDungeonMode, " +
                "enter: $hasEnterButton, continue: $hasContinueFloor)")
        return result
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

        // Check for between-floor loot screens (Victory after each floor)
        if (data.any { it.text.lowercase().contains("victory") } && 
            !data.any { it.text.lowercase().contains("complete") }) {
            // This is a between-floor victory screen
            newState = newState.copy(
                isBetweenFloorLoot = true,
                victoryScreenHandledForFloor = true
            )
        }

        when {
            data.any { it.text.lowercase().contains("complete") } -> {
                newState = newState.copy(isDone = true, isDungeonComplete = true)
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
            // Try to extract dungeon name from battle log entries
            if (item.text.contains("entered", ignoreCase = true) &&
                (item.text.contains("dungeon", ignoreCase = true) ||
                 item.text.contains("gauntlet", ignoreCase = true))) {
                // Extract name from "You entered X Dungeon" style messages
                val match = Regex("entered\\s+(.+?)(?:\\s+[Dd]ungeon|\\s+[Gg]auntlet)?$").find(item.text)
                match?.let {
                    return it.groupValues[1].trim()
                }
            }

            if (nameNext) {
                Log.d(TAG, "Found dungeon name: ${item.text}")
                return item.text
            } else if (item.text.lowercase().contains("world dungeon") ||
                item.text.lowercase().contains("special dungeon")
            ) {
                nameNext = true
            }
        }

        if (data.any { it.text.startsWith("Battle a series of opponents") } &&
            data.any { it.text == "Runeshop" }) {
            return "Personal gauntlet"
        }

        if (data.any { it.text.lowercase().contains("floor") }) return "Unknown Dungeon"

        return null
    }

    private fun extractDungeonName(screenData: List<ScreenData>): String? {
        return extractDungeonNameFromData(screenData)
    }

    private fun extractDungeonMode(screenData: List<ScreenData>): DungeonMode {
        // Look for mode text and check if it has a checkmark
        var isHard = false
        var type = DungeonMode.Type.NORMAL
        
        for (i in screenData.indices) {
            val text = screenData[i].text.lowercase()
            
            // Check if this item or the next has a checkmark
            val hasCheck = screenData[i].text.contains("✓") || 
                          (i + 1 < screenData.size && screenData[i + 1].text.contains("✓"))
            
            when {
                text.contains("hard mode") && hasCheck -> isHard = true
                text.contains("boss mode") && hasCheck -> type = DungeonMode.Type.BOSS
                text.contains("endless mode") && hasCheck -> type = DungeonMode.Type.ENDLESS
            }
        }

        // Legacy check for old format
        if (type == DungeonMode.Type.NORMAL) {
            val texts = screenData.map { it.text.lowercase() }
            if (texts.any { it.contains("boss") && it.contains("✓") }) type = DungeonMode.Type.BOSS
            else if (texts.any { it.contains("endless") && it.contains("✓") }) type = DungeonMode.Type.ENDLESS
        }

        Log.d(TAG, "Extracted mode: $type, hard: $isHard")
        return DungeonMode(type, isHard)
    }

    private fun extractFloor(screenData: List<ScreenData>): Long? {
        return screenData.find { it.text.contains("Floor:", ignoreCase = true) }
            ?.text
            ?.let { text ->
                // Try different floor patterns
                val patterns = listOf(
                    Regex("Floor:\\s*(\\d+)"),
                    Regex("Floor\\s+(\\d+)"),
                    Regex("(\\d+)\\s*/\\s*\\d+")
                )
                patterns.firstNotNullOfOrNull { pattern ->
                    pattern.find(text)?.groupValues?.get(1)?.toLongOrNull()
                }
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
                    text.contains("experience", ignoreCase = true) -> loot["experience"] =
                        lastNumber!!
                }
                lastNumber = null
            }
        }

        return loot
    }

    fun parseLoot(data: List<ScreenData>): Map<String, Int> {
        val loot = mutableMapOf<String, Int>()
        Log.d(TAG, "Parsing loot from ${data.size} screen items")

        // Strategy 1: Look for patterns like "1234 orns" or "orns: 1234"
        data.forEach { item ->
            val text = item.text.trim()
            
            // Pattern: "1234 orns" or "1234 gold" etc
            val inlinePattern = Regex("(\\d+[,.]?\\d*)\\s*(orns|gold|experience|exp)", RegexOption.IGNORE_CASE)
            inlinePattern.find(text)?.let { match ->
                val number = match.groupValues[1].replace(",", "").replace(".", "").toIntOrNull()
                val type = match.groupValues[2].lowercase()
                
                if (number != null) {
                    when {
                        type.contains("orn") -> loot["orns"] = number
                        type.contains("gold") -> loot["gold"] = number
                        type.contains("exp") -> loot["experience"] = number
                    }
                    Log.d(TAG, "Found $type: $number from inline pattern")
                }
            }
            
            // Pattern: "orns: 1234"
            val colonPattern = Regex("(orns|gold|experience|exp)\\s*:\\s*(\\d+[,.]?\\d*)", RegexOption.IGNORE_CASE)
            colonPattern.find(text)?.let { match ->
                val type = match.groupValues[1].lowercase()
                val number = match.groupValues[2].replace(",", "").replace(".", "").toIntOrNull()
                
                if (number != null) {
                    when {
                        type.contains("orn") -> loot["orns"] = number
                        type.contains("gold") -> loot["gold"] = number
                        type.contains("exp") -> loot["experience"] = number
                    }
                }
            }
        }
        
        // Strategy 2: Look for number followed by type on next line
        for (i in data.indices) {
            val text = data[i].text.trim()
            val cleanedNumber = text.replace(",", "").replace(".", "").replace(" ", "")
            
            if (cleanedNumber.matches(Regex("\\d+")) && loot.isEmpty()) {
                val number = cleanedNumber.toIntOrNull() ?: continue
                
                // Look at next item for the type
                if (i + 1 < data.size) {
                    val nextText = data[i + 1].text.lowercase()
                    when {
                        nextText == "experience" || nextText == "exp" -> {
                            if (!loot.containsKey("experience")) {
                                loot["experience"] = number
                                Log.d(TAG, "Found experience: $number (from separate lines)")
                            }
                        }
                        nextText == "gold" -> {
                            if (!loot.containsKey("gold")) {
                                loot["gold"] = number
                                Log.d(TAG, "Found gold: $number (from separate lines)")
                            }
                        }
                        nextText == "orns" -> {
                            if (!loot.containsKey("orns")) {
                                loot["orns"] = number
                                Log.d(TAG, "Found orns: $number (from separate lines)")
                            }
                        }
                    }
                }
            }
        }

        Log.d(TAG, "Parsed loot: $loot")
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

        // Look for floor data more comprehensively - but exclude dates
        val floorData = data.firstOrNull { 
            val lower = it.text.lowercase()
            val text = it.text
            
            // Exclude dates and other non-floor patterns
            val isDate = text.matches(Regex("\\d{1,2}/\\d{1,2}/\\d{2,4}"))
            val isTime = text.matches(Regex("\\d{1,2}:\\d{2}"))
            val isYear = text.matches(Regex(".*20\\d{2}.*"))
            
            if (!isDate && !isTime && !isYear) {
                lower.contains("floor:") || 
                lower.contains("floor ") ||
                // Match patterns like "5/10" or "5 / 10" but not dates
                (text.matches(Regex("^\\d{1,3}\\s*/\\s*\\d{1,3}$")))
            } else {
                false
            }
        }
        
        floorData?.let {
            Log.d(TAG, "Found floor data: ${it.text}")
            
            val patterns = listOf(
                Regex("Floor:\\s*(\\d+)\\s*/\\s*(\\d+|∞)", RegexOption.IGNORE_CASE),
                Regex("Floor\\s+(\\d+)\\s*/\\s*(\\d+|∞)", RegexOption.IGNORE_CASE),
                Regex("^(\\d{1,3})\\s*/\\s*(\\d{1,3}|∞)$"),
                Regex("Floor:\\s*(\\d+)", RegexOption.IGNORE_CASE),
                Regex("Floor\\s+(\\d+)", RegexOption.IGNORE_CASE)
            )
            
            patterns.firstNotNullOfOrNull { pattern -> pattern.find(it.text) }?.let { m ->
                val floorNumber = m.groupValues[1].toIntOrNull() ?: 1
                val maxFloor = m.groupValues[2].let { max ->
                    if (max == "∞") Int.MAX_VALUE else max.toIntOrNull() ?: 1
                }
                
                // Additional validation: floor numbers should be reasonable
                if (floorNumber in 1..999 && (maxFloor == Int.MAX_VALUE || maxFloor in 1..999)) {
                    // If we see a floor number, we're in the dungeon
                    if (!newState.hasEntered) {
                        newState = newState.copy(hasEntered = true)
                        Log.d(TAG, "Marking as entered due to floor data")
                    }

                    if (newState.hasEntered && floorNumber != newState.floorNumber) {
                        newState = newState.copy(
                            floorNumber = floorNumber,
                            victoryScreenHandledForFloor = false,
                            totalFloors = if (maxFloor != Int.MAX_VALUE) maxFloor else null
                        )
                        Log.d(TAG, "Floor changed from ${state.floorNumber} to $floorNumber")
                    }
                    Log.d(TAG, "Found $type: $number from colon pattern")
                }
            }
        }
        
        // Strategy 2: Look for number followed by type on next line
        for (i in data.indices) {
            val text = data[i].text.trim()
            val cleanedNumber = text.replace(",", "").replace(".", "").replace(" ", "")
            
            if (cleanedNumber.matches(Regex("\\d+")) && loot.isEmpty()) {
                val number = cleanedNumber.toIntOrNull() ?: continue
                
                // Look at next item for the type
                if (i + 1 < data.size) {
                    val nextText = data[i + 1].text.lowercase()
                    when {
                        nextText == "experience" || nextText == "exp" -> {
                            if (!loot.containsKey("experience")) {
                                loot["experience"] = number
                                Log.d(TAG, "Found experience: $number (from separate lines)")
                            }
                        }
                        nextText == "gold" -> {
                            if (!loot.containsKey("gold")) {
                                loot["gold"] = number
                                Log.d(TAG, "Found gold: $number (from separate lines)")
                            }
                        }
                        nextText == "orns" -> {
                            if (!loot.containsKey("orns")) {
                                loot["orns"] = number


        return newState
    }

    private fun parseDungeonMode(data: List<ScreenData>, state: DungeonState): DungeonState {
        var modeCandidate: DungeonMode.Type? = null
        var hardCandidate = false
        var newMode = state.mode

        for (i in data.indices) {
            val item = data[i]
            val itemTextLower = item.text.lowercase()

            if (modeCandidate != null || hardCandidate) {
                // Check if the next item or current item contains checkmark
                val hasCheckmark = item.text.contains("✓") ||
                        (i + 1 < data.size && data[i + 1].text.contains("✓"))

                if (hasCheckmark) {
                    if (hardCandidate) {
                        newMode = newMode.copy(isHard = true)
                        Log.d(TAG, "Hard mode enabled")
                    }
                    if (modeTypeCandidate != null) {
                        newMode = newMode.copy(type = modeTypeCandidate)
                        Log.d(TAG, "Mode changed to: $modeTypeCandidate")
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
            } else if (itemTextLower.contains("mode")) {
                val modeText = itemTextLower.replace(" mode", "").trim()
                when (modeText) {
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
