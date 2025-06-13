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
        
        // Common UI elements to exclude when looking for dungeon names
        private val INVALID_ITEM_NAMES = setOf(
            "gold", "orns", "exp", "experience", "level", "tier", "you are", "acquired",
            "drop", "new", "send to keep", "map", "character", "inventory", "codex",
            "runeshop", "options", "gauntlet", "party", "arena", "knights of inferno",
            "earthen legion", "frozenguard", "wayvessel", "notifications", "inbox",
            "vagrant beasts", "daily login", "news", "settings", "profile", "mail",
            "messages", "friends", "guild", "kingdom", "chat", "world", "help",
            "tutorial", "guide", "shop", "store", "buy", "sell", "trade", "market",
            "items", "equipment", "weapons", "armor", "accessories", "consumables",
            "materials", "keys", "misc", "followers", "pets", "mounts", "stats",
            "achievements", "quests", "events", "leaderboards", "rankings", "pvp"
        )
    }

    fun canParse(data: List<ScreenData>): Boolean {
        Log.d(TAG, "=== DUNGEON DETECTION START ===")
        Log.d(TAG, "Checking ${data.size} screen items for dungeon indicators")
        
        // Log first 20 items to see what we're working with
        data.take(20).forEach { Log.d(TAG, "Screen item: '${it.text}'") }
        // First, look for explicit dungeon indicators
        val hasWorldDungeon = data.any { it.text.lowercase().contains("world dungeon") }
        val hasSpecialDungeon = data.any { it.text.lowercase().contains("special dungeon") }
        val hasGauntlet = data.any { it.text.startsWith("Battle a series of opponents") } && 
                         data.any { it.text == "Runeshop" }
        
        // If we found explicit dungeon text, we're definitely on a dungeon screen
        if (hasWorldDungeon || hasSpecialDungeon || hasGauntlet) {
            Log.d(TAG, "DUNGEON DETECTED: Explicit dungeon text found (world: $hasWorldDungeon, special: $hasSpecialDungeon, gauntlet: $hasGauntlet)")
            Log.d(TAG, "Found explicit dungeon text")
            return true
        }
        
        // ADD: Check for dungeon name patterns
        val hasDungeonName = data.any { 
            val text = it.text
            // Common dungeon name patterns
            text.endsWith(" Dungeon") || 
            text.endsWith(" Gauntlet") ||
            text.contains("Valley of the Gods") ||
            text.contains("Underworld") ||
            text.contains("Chaos Portal") ||
            text.contains("Dragon's Roost")
        }
        
        if (hasDungeonName) {
            Log.d(TAG, "DUNGEON DETECTED: Found dungeon name pattern")
        }
        
        // ADD: Check for "Enter" button which appears on dungeon screens
        val hasEnterOrContinueButton = data.any { 
            it.text.equals("Enter", ignoreCase = true) ||
            it.text.equals("Continue", ignoreCase = true)
        }
        
        // Check for floor indicators (when already in dungeon)
        val hasFloor = data.any { 
            val lower = it.text.lowercase()
            lower.contains("floor:") || 
            lower.contains("floor ") ||
            it.text.matches(Regex(".*\\d+\\s*/\\s*\\d+.*")) // matches "1 / 10" format
        }
        
        if (hasFloor) {
            val floorText = data.find { 
                val lower = it.text.lowercase()
                lower.contains("floor:") || lower.contains("floor ") || it.text.matches(Regex(".*\\d+\\s*/\\s*\\d+.*"))
            }?.text
            Log.d(TAG, "DUNGEON DETECTED: Found floor indicator: '$floorText'")
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
                    ((hasVictory || hasComplete || hasDefeat) && hasFloor) ||
                    hasDungeonName || hasEnterOrContinueButton
        
        Log.d(TAG, "=== DUNGEON DETECTION RESULT: $result ===")
        Log.d(TAG, "Detection details:")
        Log.d(TAG, "  - World dungeon: $hasWorldDungeon")
        Log.d(TAG, "  - Special dungeon: $hasSpecialDungeon")
        Log.d(TAG, "  - Gauntlet: $hasGauntlet")
        Log.d(TAG, "  - Has floor: $hasFloor")
        Log.d(TAG, "  - Has mode: $hasDungeonMode")
        Log.d(TAG, "  - Has enter button: $hasEnterButton")
        Log.d(TAG, "  - Has continue: $hasContinueFloor")
        Log.d(TAG, "  - Has dungeon name: $hasDungeonName")
        Log.d(TAG, "  - Has enter/continue: $hasEnterOrContinueButton")
        Log.d(TAG, "  - Has victory: $hasVictory")
        Log.d(TAG, "  - Has complete: $hasComplete")
        Log.d(TAG, "  - Has defeat: $hasDefeat")
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
        Log.d(TAG, "=== PARSE STATE START ===")
        Log.d(TAG, "Current state: $currentState")
        
        val state = currentState ?: DungeonState()
        
        // Only try to extract new name if we don't have one or if we see clear dungeon entry
        val dungeonName = if (state.dungeonName.isEmpty() || state.dungeonName == "Unknown Dungeon" ||
                           data.any { it.text.contains("world dungeon", ignoreCase = true) || 
                                     it.text.contains("special dungeon", ignoreCase = true) }) {
            extractDungeonNameFromData(data) ?: state.dungeonName
        } else state.dungeonName
        
        Log.d(TAG, "Extracted dungeon name: '$dungeonName' (was: '${state.dungeonName}')")
        
        // Log key screen elements for debugging
        data.filter { it.text.contains("Floor", ignoreCase = true) || 
                     it.text.contains("mode", ignoreCase = true) ||
                     it.text.contains("enter", ignoreCase = true) }.forEach { Log.d(TAG, "Key element: '${it.text}'") }

        // Only mark as new dungeon if we're actually seeing a dungeon selection screen
        val isDungeonSelectionScreen = data.any { 
            it.text.contains("world dungeon", ignoreCase = true) || 
            it.text.contains("special dungeon", ignoreCase = true) ||
            it.text.contains("hold to enter", ignoreCase = true)
        }
        
        if (dungeonName.isNotEmpty() && dungeonName != state.dungeonName && state.dungeonName.isNotEmpty() && isDungeonSelectionScreen) {
            Log.d(TAG, "DIFFERENT DUNGEON DETECTED: '$dungeonName' vs '${state.dungeonName}'")
            return DungeonState(dungeonName = dungeonName, isEnteringNewDungeon = true)
        }

        var newState = state.copy(dungeonName = dungeonName)
        newState = parseFloorAndEntry(data, newState)
        newState = parseDungeonMode(data, newState)
        
        Log.d(TAG, "After parsing - hasEntered: ${newState.hasEntered}, floor: ${newState.floorNumber}, mode: ${newState.mode}")

        when {
            data.any { it.text.lowercase().contains("complete") } -> {
                Log.d(TAG, "DUNGEON COMPLETE detected")
                newState = newState.copy(isDone = true)
            }

            data.any { it.text.lowercase().contains("defeat") } -> {
                Log.d(TAG, "DUNGEON DEFEAT detected")
                newState = newState.copy(isDone = true)
            }
        }
        
        Log.d(TAG, "=== PARSE STATE END - New state: $newState ===")

        return newState
    }

    fun extractDungeonNameFromData(data: List<ScreenData>): String? {
        Log.d(TAG, "=== EXTRACTING DUNGEON NAME ===")
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
        
        // ADD: Look for dungeon names that appear near "Enter" or "Continue" buttons
        val enterButtonIndex = data.indexOfFirst { 
            it.text.equals("Enter", ignoreCase = true) ||
            it.text.equals("Continue", ignoreCase = true)
        }
        
        if (enterButtonIndex > 0) {
            // Look for dungeon name above the Enter button
            for (i in (enterButtonIndex - 1) downTo 0) {
                val text = data[i].text
                if (text.endsWith(" Dungeon") || 
                    text.endsWith(" Gauntlet") ||
                    text.contains("Valley of the Gods") ||
                    text.contains("Underworld")) {
                    return text.replace(" Dungeon", "")
                               .replace(" Gauntlet", "")
                }
            }
        }
        
        // Try to extract dungeon name from battle log entries
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

        // ADD: If we're already in a dungeon (have floor info), try to extract name from other elements
        if (data.any { it.text.lowercase().contains("floor") }) {
            // Look for capitalized multi-word phrases that could be dungeon names
            val potentialNames = data.filter { 
                it.text.length > 5 && 
                it.text.contains(" ") &&
                it.text[0].isUpperCase() &&
                !INVALID_ITEM_NAMES.any { invalid -> 
                    it.text.lowercase().contains(invalid.lowercase()) 
                }
            }
            
            // Prioritize names ending with "Dungeon" or known dungeon types
            potentialNames.firstOrNull { 
                it.text.endsWith(" Dungeon") || 
                it.text.endsWith(" Gauntlet") 
            }?.let { return it.text }
        }
        
        if (data.any { it.text.lowercase().contains("floor") }) {
            Log.d(TAG, "Has floor info but couldn't extract dungeon name, returning null")
            return null // Don't default to "Unknown Dungeon" - let it keep the existing name
        }

        return null
    }

    private fun extractDungeonName(screenData: List<ScreenData>): String? {
        return extractDungeonNameFromData(screenData)
    }

    private fun extractDungeonMode(screenData: List<ScreenData>): DungeonMode {
        Log.d(TAG, "=== EXTRACTING DUNGEON MODE ===")
        // Look for mode text and check if it has a checkmark
        var isHard = false
        var type = DungeonMode.Type.NORMAL
        
        for (i in screenData.indices) {
            val text = screenData[i].text.lowercase()
            
            // Check if this item or the next has a checkmark
            val hasCheck = screenData[i].text.contains("✓") || 
                          (i + 1 < screenData.size && screenData[i + 1].text.contains("✓"))
            
            if (text.contains("mode")) {
                Log.d(TAG, "Found mode text: '${screenData[i].text}' (has check: $hasCheck)")
            }
            
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
        Log.d(TAG, "=== EXTRACTING FLOOR ===")
        return screenData.find { 
            it.text.contains("Floor:", ignoreCase = true) &&
            !it.text.contains(",") // Exclude HP/MP values with commas
        }?.text?.let { text ->
                // Try different floor patterns
                val patterns = listOf(
                    Regex("Floor:\\s*(\\d+)\\s*/\\s*\\d+"), // "Floor: 2 / 10"
                    Regex("Floor\\s+(\\d+)\\s*/\\s*\\d+"),  // "Floor 2 / 10"
                    Regex("Floor:\\s*(\\d+)$"),              // "Floor: 2"
                    Regex("Floor\\s+(\\d+)$")                // "Floor 2"
                )
                // Only accept reasonable floor numbers (1-999)
                .filter { pattern -> text.matches(pattern) }
                patterns.firstNotNullOfOrNull { pattern ->
                    pattern.find(text)?.groupValues?.get(1)?.toLongOrNull()?.also { floor ->
                        Log.d(TAG, "Extracted floor number: $floor from '$text'")
                    }
                } ?: run {
                    Log.d(TAG, "Failed to extract floor from: '$text'")
                    null
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
        Log.d(TAG, "=== PARSING FLOOR AND ENTRY ===")
        var newState = state

        val hasContinue = data.any { it.text.lowercase().contains("continue floor") }
        val hasHoldToEnter = data.any { it.text.lowercase().contains("hold to enter") }
        Log.d(TAG, "Has continue floor: $hasContinue, Has hold to enter: $hasHoldToEnter")
        
        newState = when {
            data.any { it.text.lowercase().contains("continue floor") } ->
                // Continuing in same dungeon
                newState.copy(isEnteringNewDungeon = false, hasEntered = true)

            data.any { it.text.lowercase().contains("hold to enter") } ->
                // Only mark as new if we don't already have a dungeon name
                if (state.dungeonName.isEmpty()) newState.copy(isEnteringNewDungeon = true)
                else newState

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
            !lower.contains("hp") && !lower.contains("mp") && !lower.contains("mana") &&
            !it.text.contains(",") // Exclude numbers with commas (HP/MP values)
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
                Regex("^([1-9]\\d{0,2})\\s*/\\s*([1-9]\\d{0,2}|∞)$"),
                Regex("Floor\\s+([1-9]\\d{0,2})$", RegexOption.IGNORE_CASE)
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
        Log.d(TAG, "=== PARSING DUNGEON MODE ===")
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

        Log.d(TAG, "Final mode: ${newMode.type}, hard: ${newMode.isHard}")
        
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
