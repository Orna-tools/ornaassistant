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
        val hasBattleDungeon = data.any { it.text.lowercase().contains("battle dungeon") }
        val hasGauntlet = data.any { it.text.contains("Battle a series of opponents") }

        // If we found explicit dungeon text, we're definitely on a dungeon screen
        if (hasWorldDungeon || hasSpecialDungeon || hasBattleDungeon || hasGauntlet) {
            Log.d(TAG, "Found explicit dungeon text")
            return true
        }

        // Check for floor indicators
        val hasFloor = data.any {
            val text = it.text
            val lower = text.lowercase()

            // Check for "Floor: X / Y" pattern (dungeons)
            // Note: Towers might have different patterns in the future
            (lower.contains("floor:") && text.matches(Regex(".*Floor:\\s*\\d+\\s*/\\s*\\d+.*", RegexOption.IGNORE_CASE))) ||
                    // Also check for just "Floor: X" without total
                    (lower.contains("floor:") && text.matches(Regex(".*Floor:\\s*\\d+.*", RegexOption.IGNORE_CASE)))
        }

        // Check for dungeon-specific UI elements
        val hasDungeonMode = data.any {
            val lower = it.text.lowercase()
            lower.contains("normal mode") || lower.contains("hard mode") ||
                    lower.contains("boss mode") || lower.contains("endless mode")
        }
        val hasEnterButton = data.any { it.text.equals("HOLD TO ENTER", ignoreCase = true) }
        val hasContinueButton = data.any { it.text.equals("CONTINUE", ignoreCase = true) }

        // Check for battle/victory screens in dungeons
        val hasVictory = data.any { it.text.lowercase().contains("victory") }
        val hasComplete = data.any { it.text.lowercase().contains("complete") }
        val hasDefeat = data.any { it.text.lowercase().contains("defeat") }

        // If we have floor info or other dungeon indicators, we're in a dungeon
        val result = hasFloor || hasDungeonMode || hasEnterButton ||
                hasContinueButton || hasVictory || hasComplete || hasDefeat

        Log.d(TAG, "canParse result: $result (world: $hasWorldDungeon, special: $hasSpecialDungeon, " +
                "battle: $hasBattleDungeon, gauntlet: $hasGauntlet, floor: $hasFloor, mode: $hasDungeonMode, " +
                "enter: $hasEnterButton, continue: $hasContinueButton)")
        return result
    }

    override suspend fun parseScreen(parsedScreen: ParsedScreen) {
        try {
            val dungeonName = extractDungeonName(parsedScreen.data)
            val dungeonMode = extractDungeonMode(parsedScreen.data)
            val floor = extractFloor(parsedScreen.data)
            val loot = parseLoot(parsedScreen.data)

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
                        orns = loot["orns"]?.toLong(),
                        gold = loot["gold"]?.toLong(),
                        experience = loot["experience"]?.toLong(),
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

        // Extract dungeon name
        val dungeonName = extractDungeonNameFromData(data) ?: state.dungeonName

        // If entering a new dungeon
        if (dungeonName.isNotEmpty() && dungeonName != state.dungeonName && state.dungeonName.isNotEmpty()) {
            Log.d(TAG, "Switching from ${state.dungeonName} to $dungeonName")
            return DungeonState(dungeonName = dungeonName)
        }

        var newState = state.copy(dungeonName = dungeonName)

        // Parse floor and entry status
        newState = parseFloorAndEntry(data, newState)

        // Parse dungeon mode
        newState = parseDungeonMode(data, newState)

        // Check for victory/complete screens
        val hasVictory = data.any { it.text.equals("VICTORY!", ignoreCase = true) }
        val hasDungeonComplete = data.any { it.text.equals("DUNGEON COMPLETE!", ignoreCase = true) }
        val hasDefeat = data.any { it.text.lowercase().contains("defeat") }

        // Handle floor victory screens (between floors)
        if (hasVictory && !hasDungeonComplete && !newState.victoryScreenHandledForFloor) {
            newState = newState.copy(victoryScreenHandledForFloor = true)
            Log.d(TAG, "Floor victory screen detected")
        }

        // Handle dungeon completion
        if (hasDungeonComplete) {
            newState = newState.copy(isDone = true)
            Log.d(TAG, "Dungeon completed successfully")
        }

        // Handle defeat
        if (hasDefeat) {
            newState = newState.copy(isDone = true)
            Log.d(TAG, "Dungeon failed")
        }

        return newState
    }

    private fun extractDungeonNameFromData(data: List<ScreenData>): String? {
        // Look for dungeon name patterns
        for (i in data.indices) {
            val item = data[i]
            val text = item.text.trim()

            // Pattern 1: "X Dungeon" where X is the name
            if (text.endsWith(" Dungeon", ignoreCase = true)) {
                // Skip generic dungeon type labels
                if (!text.equals("Battle Dungeon", ignoreCase = true) &&
                    !text.equals("World Dungeon", ignoreCase = true) &&
                    !text.equals("Special Dungeon", ignoreCase = true)) {
                    val dungeonName = text.substring(0, text.length - 8).trim() // Remove " Dungeon"
                    if (dungeonName.isNotEmpty()) {
                        Log.d(TAG, "Found dungeon name: $dungeonName")
                        return dungeonName
                    }
                }
            }

            // Pattern 2: Check if previous item was a dungeon type label
            if (i > 0) {
                val prevText = data[i-1].text.lowercase()
                if ((prevText == "battle dungeon" ||
                            prevText == "world dungeon" ||
                            prevText == "special dungeon") &&
                    !text.contains("Battle a series", ignoreCase = true) &&
                    text.length > 2) {
                    Log.d(TAG, "Found dungeon name after type label: $text")
                    return text
                }
            }

            // Pattern 3: Look for "entered X" messages in battle log
            if (text.contains("entered", ignoreCase = true) &&
                (text.contains("dungeon", ignoreCase = true) ||
                        text.contains("gauntlet", ignoreCase = true))) {
                val match = Regex("entered\\s+(.+?)(?:\\s+[Dd]ungeon|\\s+[Gg]auntlet)?$").find(text)
                match?.let {
                    val name = it.groupValues[1].trim()
                    if (name.isNotEmpty()) {
                        Log.d(TAG, "Found dungeon name from battle log: $name")
                        return name
                    }
                }
            }
        }

        // Check for gauntlet
        if (data.any { it.text.contains("Battle a series of opponents") }) {
            return "Personal Gauntlet"
        }

        // If we're in a dungeon (have floor info) but can't find name
        if (data.any { it.text.lowercase().contains("floor:") }) {
            // Try to find any text that might be a dungeon name
            // Look for capitalized multi-word phrases that could be names
            val potentialNames = data.filter {
                val text = it.text.trim()
                text.length in 5..30 && // Reasonable length for a dungeon name
                        text.contains(" ") && // Multi-word
                        text[0].isUpperCase() && // Starts with capital
                        !text.contains("Floor") && // Not floor info
                        !text.contains("HOLD") && // Not button text
                        !text.contains("CONTINUE") && // Not button text
                        !text.all { c -> c.isUpperCase() || c.isWhitespace() } // Not all caps
            }

            // Return the first reasonable looking name
            potentialNames.firstOrNull()?.let {
                Log.d(TAG, "Using potential dungeon name: ${it.text}")
                return it.text
            }

            return "Dungeon" // Generic fallback
        }

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

        Log.d(TAG, "Extracted mode: $type, hard: $isHard")
        return DungeonMode(type, isHard)
    }

    private fun extractFloor(screenData: List<ScreenData>): Long? {
        // Look for floor patterns
        val floorData = screenData.firstOrNull {
            it.text.matches(Regex(".*Floor:\\s*\\d+.*", RegexOption.IGNORE_CASE))
        }

        return floorData?.let { data ->
            // Try different floor patterns
            val patterns = listOf(
                Regex("Floor:\\s*(\\d+)\\s*/\\s*\\d+", RegexOption.IGNORE_CASE), // "Floor: 3 / 9"
                Regex("Floor:\\s*(\\d+)", RegexOption.IGNORE_CASE), // "Floor: 3"
                Regex("Floor\\s+(\\d+)\\s*/\\s*\\d+", RegexOption.IGNORE_CASE), // "Floor 3 / 9"
                Regex("Floor\\s+(\\d+)", RegexOption.IGNORE_CASE) // "Floor 3"
            )

            patterns.firstNotNullOfOrNull { pattern ->
                pattern.find(data.text)?.groupValues?.get(1)?.toLongOrNull()
            }
        }
    }

    fun parseLoot(data: List<ScreenData>): Map<String, Int> {
        val loot = mutableMapOf<String, Int>()
        Log.d(TAG, "Parsing loot from ${data.size} screen items")

        // Look for loot patterns in the text
        data.forEach { item ->
            val text = item.text.trim()

            // Skip kingdom gold - it's different from regular gold
            if (text.contains("kingdom gold", ignoreCase = true)) {
                Log.d(TAG, "Skipping kingdom gold")
                return@forEach
            }

            // Remove emojis and special characters
            val cleanText = text.replace(Regex("[🗡️💰🔷⚔️🛡️🎯🏆💎✨]"), "").trim()

            // Pattern 1: "2,720 experience" or "24,229 gold" or "277 orns"
            val lootPattern = Regex("([\\d,]+)\\s+(experience|exp|gold|orns|orn)", RegexOption.IGNORE_CASE)
            lootPattern.find(cleanText)?.let { match ->
                val number = match.groupValues[1].replace(",", "").toIntOrNull()
                val type = match.groupValues[2].lowercase()

                if (number != null && number > 0) {
                    when {
                        type.contains("exp") -> {
                            loot["experience"] = (loot["experience"] ?: 0) + number
                            Log.d(TAG, "Found experience: $number (total: ${loot["experience"]})")
                        }
                        type == "gold" -> { // Exact match to avoid kingdom gold
                            loot["gold"] = (loot["gold"] ?: 0) + number
                            Log.d(TAG, "Found gold: $number (total: ${loot["gold"]})")
                        }
                        type.contains("orn") -> {
                            loot["orns"] = (loot["orns"] ?: 0) + number
                            Log.d(TAG, "Found orns: $number (total: ${loot["orns"]})")
                        }
                    }
                }
            }

            // Pattern 2: Look for standalone numbers followed by type
            if (cleanText.matches(Regex("\\d+[,.]?\\d*"))) {
                val number = cleanText.replace(",", "").replace(".", "").toIntOrNull()
                if (number != null && data.indexOf(item) + 1 < data.size) {
                    val nextText = data[data.indexOf(item) + 1].text.lowercase()
                    when {
                        nextText.contains("experience") || nextText.contains("exp") -> {
                            loot["experience"] = (loot["experience"] ?: 0) + number
                            Log.d(TAG, "Found experience from pattern 2: $number")
                        }
                        nextText == "gold" && !nextText.contains("kingdom") -> { // Avoid kingdom gold
                            loot["gold"] = (loot["gold"] ?: 0) + number
                            Log.d(TAG, "Found gold from pattern 2: $number")
                        }
                        nextText.contains("orns") || nextText.contains("orn") -> {
                            loot["orns"] = (loot["orns"] ?: 0) + number
                            Log.d(TAG, "Found orns from pattern 2: $number")
                        }
                    }
                }
            }
        }

        Log.d(TAG, "Total parsed loot: $loot")
        return loot
    }

    private fun parseFloorAndEntry(data: List<ScreenData>, state: DungeonState): DungeonState {
        var newState = state

        // Check for "HOLD TO ENTER" button
        if (data.any { it.text.equals("HOLD TO ENTER", ignoreCase = true) }) {
            newState = newState.copy(isEnteringNewDungeon = true)
            Log.d(TAG, "Found HOLD TO ENTER button")
        }

        // Check for "CONTINUE" button (between floors)
        if (data.any { it.text.equals("CONTINUE", ignoreCase = true) }) {
            if (!newState.hasEntered) {
                newState = newState.copy(hasEntered = true)
                Log.d(TAG, "Found CONTINUE button, marking as entered")
            }
        }

        // Look for floor pattern
        val floorData = data.firstOrNull {
            it.text.matches(Regex(".*Floor:\\s*\\d+.*", RegexOption.IGNORE_CASE))
        }

        floorData?.let {
            Log.d(TAG, "Found floor data: ${it.text}")

            // Extract floor number from various patterns
            val patterns = listOf(
                Regex("Floor:\\s*(\\d+)\\s*/\\s*(\\d+)", RegexOption.IGNORE_CASE), // "Floor: 3 / 9"
                Regex("Floor:\\s*(\\d+)", RegexOption.IGNORE_CASE), // "Floor: 3"
                Regex("Floor\\s+(\\d+)\\s*/\\s*(\\d+)", RegexOption.IGNORE_CASE), // "Floor 3 / 9"
                Regex("Floor\\s+(\\d+)", RegexOption.IGNORE_CASE) // "Floor 3"
            )

            for (pattern in patterns) {
                pattern.find(it.text)?.let { match ->
                    val floorNumber = match.groupValues[1].toIntOrNull() ?: return@let

                    // If we see a floor number, we're definitely in the dungeon
                    if (!newState.hasEntered) {
                        newState = newState.copy(hasEntered = true)
                        Log.d(TAG, "Marking as entered due to floor data")
                    }

                    if (floorNumber != newState.floorNumber) {
                        newState = newState.copy(
                            floorNumber = floorNumber,
                            victoryScreenHandledForFloor = false
                        )
                        Log.d(TAG, "Floor changed to $floorNumber")
                    }
                    return@let
                }
            }
        }

        return newState
    }

    private fun parseDungeonMode(data: List<ScreenData>, state: DungeonState): DungeonState {
        var modeTypeCandidate: DungeonMode.Type? = null
        var hardCandidate = false
        var newMode = state.mode

        for (i in data.indices) {
            val item = data[i]
            val itemTextLower = item.text.lowercase()

            if (modeTypeCandidate != null || hardCandidate) {
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
                    if (modeTypeCandidate != null && modeTypeCandidate == newMode.type) {
                        newMode = newMode.copy(type = DungeonMode.Type.NORMAL)
                    }
                }
                modeTypeCandidate = null
                hardCandidate = false
            } else if (itemTextLower.contains("mode")) {
                val modeText = itemTextLower.replace(" mode", "").trim()
                when (modeText) {
                    "hard" -> hardCandidate = true
                    "boss" -> modeTypeCandidate = DungeonMode.Type.BOSS
                    "endless" -> modeTypeCandidate = DungeonMode.Type.ENDLESS
                }
            }
        }

        return state.copy(mode = newMode)
    }

    private fun isVictoryScreen(screenData: List<ScreenData>): Boolean {
        return screenData.any {
            it.text.equals("VICTORY!", ignoreCase = true) ||
                    it.text.equals("DUNGEON COMPLETE!", ignoreCase = true)
        }
    }

    private fun isCompletedScreen(screenData: List<ScreenData>): Boolean {
        return screenData.any {
            it.text.equals("DUNGEON COMPLETE!", ignoreCase = true) ||
                    it.text.contains("defeat", ignoreCase = true)
        }
    }
}