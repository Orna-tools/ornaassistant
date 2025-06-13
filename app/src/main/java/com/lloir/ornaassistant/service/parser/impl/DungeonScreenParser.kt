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
        
        // Check for floor indicators (when already in dungeon)
        val hasFloor = data.any { 
            val lower = it.text.lowercase()
            lower.contains("floor:") || 
            lower.contains("floor ") ||
            it.text.matches(Regex(".*\\d+\\s*/\\s*\\d+.*")) // matches "1 / 10" format
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
        
        // If we have floor info, that's a strong indicator we're in a dungeon
        val result = hasFloor || hasDungeonMode || hasEnterButton || hasContinueFloor ||
                    (hasFloor && data.any { it.text.lowercase().contains("exit") }) ||
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

    fun extractDungeonNameFromData(data: List<ScreenData>): String? {
        // First check for dungeon completion screen pattern
        val completeIndex = data.indexOfFirst { 
            it.text.equals("DUNGEON COMPLETE!", ignoreCase = true) 
        }
        if (completeIndex > 0) {
            // Look for pattern like "Bandit Lord NORMAL Floor 5" before the complete message
            for (i in (completeIndex - 1) downTo 0) {
                val text = data[i].text
                if (text.contains("Floor", ignoreCase = true) && 
                    (text.contains("NORMAL", ignoreCase = true) || 
                     text.contains("HARD", ignoreCase = true) ||
                     text.contains("BOSS", ignoreCase = true) ||
                     text.contains("ENDLESS", ignoreCase = true))) {
                    // Extract dungeon name from "Bandit Lord NORMAL Floor 5"
                    val match = Regex("(.+?)\\s+(NORMAL|HARD|BOSS|ENDLESS)\\s+Floor", RegexOption.IGNORE_CASE)
                        .find(text)
                    match?.groupValues?.get(1)?.trim()?.let { name ->
                        Log.d(TAG, "Found dungeon name from completion screen: $name")
                        return name
                    }
                }
            }
            // Also check for just the dungeon name above DUNGEON COMPLETE
            if (completeIndex >= 2) {
                val possibleName = data[completeIndex - 2].text
                if (!possibleName.contains("Floor") && possibleName.length > 3) {
                    return possibleName
                }
            }
        }
        
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
        var lastNumber: String? = null

        // Check if this is a dungeon completion screen
        val isDungeonComplete = data.any { it.text.equals("DUNGEON COMPLETE!", ignoreCase = true) }
        val startIndex = if (isDungeonComplete) {
            // Start parsing after "Here's what you found:"
            data.indexOfFirst { it.text.contains("Here's what you found", ignoreCase = true) }
                .takeIf { it >= 0 } ?: 0
        } else {
            0
        }

        Log.d(TAG, "Parsing loot from ${data.size} screen items (dungeon complete: $isDungeonComplete)")

        for (i in startIndex until data.size) {
            val item = data[i]
            val text = item.text.trim()

            // Skip item names and materials
            if (text.contains("Token", ignoreCase = true) || 
                text.contains("Axe", ignoreCase = true) ||
                text.contains("Cloak", ignoreCase = true) ||
                text.contains("Hood", ignoreCase = true) ||
                text.contains("Robe", ignoreCase = true) ||
                text.contains("Draconite", ignoreCase = true) ||
                text.contains("Iron", ignoreCase = true) ||
                text.contains("Earthstone", ignoreCase = true) ||
                text.contains("Soul", ignoreCase = true) ||
                text.contains("Silk", ignoreCase = true) ||
                text.contains("Wood", ignoreCase = true)) {
                lastNumber = null
                continue
            }

            // Check for direct format "X experience/gold/orns"
            val directMatch = Regex("(\\d+(?:,\\d+)*)\\s*(experience|gold|orns)", RegexOption.IGNORE_CASE)
                .find(text)
            if (directMatch != null) {
                val value = directMatch.groupValues[1].replace(",", "").toIntOrNull() ?: 0
                val type = directMatch.groupValues[2].lowercase()
                when (type) {
                    "experience" -> loot["experience"] = (loot["experience"] ?: 0) + value
                    "gold" -> if (!text.contains("kingdom", ignoreCase = true)) {
                        loot["gold"] = (loot["gold"] ?: 0) + value
                    }
                    "orns" -> loot["orns"] = (loot["orns"] ?: 0) + value
                }
                Log.d(TAG, "Found $type: $value from direct match")
                continue
            }

            // Check if this is a standalone number
            val cleanedNumber = text
                .replace(",", "")
                .replace(".", "")
                .replace(" ", "")

            if (cleanedNumber.matches(Regex("\\d+"))) {
                lastNumber = cleanedNumber
                Log.d(TAG, "Found number: $lastNumber")
            } else if (lastNumber != null) {
                // Check if this text describes what the number was for
                val lowerText = text.lowercase()
                when {
                    lowerText == "kingdom gold" -> {
                        // Skip kingdom gold
                        lastNumber = null
                    }
                    lowerText.contains("experience") || lowerText.contains("exp") -> {
                        loot["experience"] = lastNumber!!.toIntOrNull() ?: 0
                        Log.d(TAG, "Found experience: ${loot["experience"]}")
                        lastNumber = null
                    }

                    lowerText.contains("gold") -> {
                        loot["gold"] = lastNumber!!.toIntOrNull() ?: 0
                        Log.d(TAG, "Found gold: ${loot["gold"]}")
                        lastNumber = null
                    }

                    lowerText.contains("orns") -> {
                        loot["orns"] = lastNumber!!.toIntOrNull() ?: 0
                        Log.d(TAG, "Found orns: ${loot["orns"]}")
                        lastNumber = null
                    }
                }
            }
        }

        Log.d(TAG, "Parsed loot: $loot")
        return loot
    }

    fun parseBattleLoot(data: List<ScreenData>): Map<String, Int> {
        val loot = mutableMapOf<String, Int>()

        // Find the VICTORY! text first
        val victoryIndex = data.indexOfFirst { it.text.equals("VICTORY!", ignoreCase = true) }
        if (victoryIndex == -1) return loot

        Log.d(TAG, "Parsing battle loot from victory screen")

        // Look for rewards after VICTORY!
        var currentNumber: Int? = null

        for (i in (victoryIndex + 1) until data.size) {
            val text = data[i].text.trim()
            val cleanText = text.replace(",", "").replace(".", "")

            // Check if this is a number
            val number = cleanText.toIntOrNull()
            if (number != null) {
                currentNumber = number
                Log.d(TAG, "Found potential reward number: $currentNumber")
            } else if (currentNumber != null) {
                // Check what type of reward this number represents
                val lowerText = text.lowercase()
                when {
                    lowerText == "experience" || lowerText == "exp" -> {
                        loot["experience"] = (loot["experience"] ?: 0) + currentNumber
                        Log.d(TAG, "Found battle experience: $currentNumber")
                        currentNumber = null
                    }
                    lowerText == "gold" && !lowerText.contains("kingdom") -> {
                        loot["gold"] = (loot["gold"] ?: 0) + currentNumber
                        Log.d(TAG, "Found battle gold: $currentNumber")
                        currentNumber = null
                    }
                    lowerText.contains("kingdom gold") -> {
                        // Track kingdom gold separately if needed
                        Log.d(TAG, "Found kingdom gold: $currentNumber (not tracked)")
                        currentNumber = null
                    }
                    lowerText == "orns" -> {
                        loot["orns"] = (loot["orns"] ?: 0) + currentNumber
                        Log.d(TAG, "Found battle orns: $currentNumber")
                    }
                }
                currentNumber = null
            }
            
            // Also check for format "Experience: 123" or "123 orns"
            val directPattern = Regex("(\\d+)\\s*(experience|exp|gold|orns)", RegexOption.IGNORE_CASE)
            directPattern.find(text)?.let { match ->
                val value = match.groupValues[1].toIntOrNull() ?: 0
                val type = match.groupValues[2].lowercase()
                when (type) {
                    "experience", "exp" -> loot["experience"] = (loot["experience"] ?: 0) + value
                    "gold" -> loot["gold"] = (loot["gold"] ?: 0) + value
                    "orns" -> loot["orns"] = (loot["orns"] ?: 0) + value
                }
                Log.d(TAG, "Found $type from pattern: $value")
            }
        }

        Log.d(TAG, "Parsed battle loot: $loot")
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

        // Look for floor data more comprehensively
        val floorData = data.firstOrNull { 
            val lower = it.text.lowercase()
            // Must contain "floor" to be a floor indicator
            (lower.contains("floor:") || 
             lower.contains("floor ") ||
             (lower.contains("floor") && it.text.contains("/"))) &&
            // Must NOT be part of a stat display (HP/MP have much larger numbers typically)
            !lower.contains("hp") && !lower.contains("mp") && !lower.contains("mana")
        }
        
        // Also check for floor info in dungeon entry/complete screens
        if (floorData == null && data.any { it.text.contains("NORMAL Floor", ignoreCase = true) || 
                                           it.text.contains("HARD Floor", ignoreCase = true) ||
                                           it.text.contains("BOSS Floor", ignoreCase = true) ||
                                           it.text.contains("ENDLESS Floor", ignoreCase = true) }) {
            val floorMatch = data.find { 
                Regex("(NORMAL|HARD|BOSS|ENDLESS)\\s+Floor\\s+(\\d+)", RegexOption.IGNORE_CASE)
                    .containsMatchIn(it.text)
            }
            floorMatch?.let {
                val match = Regex("Floor\\s+(\\d+)", RegexOption.IGNORE_CASE).find(it.text)
                match?.groupValues?.get(1)?.toIntOrNull()?.let { floorNum ->
                    newState = newState.copy(floorNumber = floorNum, hasEntered = true)
                }
            }
        }
        
        floorData?.let {
            Log.d(TAG, "Found floor data: ${it.text}")
            
            val patterns = listOf(
                Regex("Floor:\\s*([0-9]+)\\s*/\\s*([0-9]+|∞)", RegexOption.IGNORE_CASE),
                Regex("Floor\\s+([0-9]+)\\s*/\\s*([0-9]+|∞)", RegexOption.IGNORE_CASE),
                // Only match standalone numbers if they're small (floor numbers)
                Regex("^([1-9]\\d{0,2})\\s*/\\s*([1-9]\\d{0,2}|∞)$")
            )
            
            patterns.firstNotNullOfOrNull { pattern -> pattern.find(it.text) }?.let { m ->
                val floorNumber = m.groupValues[1].toIntOrNull() ?: 1
                val hasDefeat = data.any { it.text.lowercase().contains("defeat") }

                // If we see a floor number, we're in the dungeon
                if (!newState.hasEntered) {
                    newState = newState.copy(hasEntered = true)
                    Log.d(TAG, "Marking as entered due to floor data")
                }

                if (newState.hasEntered && floorNumber != newState.floorNumber) {
                    val oldFloor = newState.floorNumber
                    newState = newState.copy(
                        floorNumber = floorNumber,
                        victoryScreenHandledForFloor = false
                    )
                    Log.d(TAG, "Floor changed from ${state.floorNumber} to $floorNumber")
                } else if (floorNumber == newState.floorNumber) {
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
                    if (modeCandidate != null) {
                        newMode = newMode.copy(type = modeCandidate)
                        Log.d(TAG, "Mode changed to: $modeCandidate")
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
