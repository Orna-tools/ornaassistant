package com.lloir.ornaassistant.service.parser.impl

import android.util.Log
import com.lloir.ornaassistant.domain.model.*
import com.lloir.ornaassistant.domain.usecase.*
import com.lloir.ornaassistant.service.parser.ScreenParser
import com.lloir.ornaassistant.service.parser.DungeonStateTracker
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import com.lloir.ornaassistant.utils.LogUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DungeonScreenParser @Inject constructor(
    private val trackDungeonVisitUseCase: TrackDungeonVisitUseCase,
    private val updateDungeonVisitUseCase: UpdateDungeonVisitUseCase,
    private val dungeonStateTracker: DungeonStateTracker,
    private val settingsRepository: SettingsRepository,
    private val logUtils: LogUtils
) : ScreenParser {

    private val _currentDungeonVisit = MutableStateFlow<DungeonVisit?>(null)
    val currentDungeonVisit: StateFlow<DungeonVisit?> = _currentDungeonVisit.asStateFlow()

    private var currentDungeonState: DungeonState? = null
    private var onHoldVisits = mutableMapOf<String, DungeonVisit>()

    companion object {
        private const val TAG = "DungeonScreenParser"

        // List of valid named dungeons to track
        private val VALID_DUNGEONS = setOf(
            "Goblin Fortress",
            "Mystic Cave",
            "Beast Den",
            "Dragon Roost",
            "Chaos Portal",
            "Underworld Portal",
            "BattleGrounds",
            "Valley Of The Gods"
        )

        // Regex to identify generic dungeons (random generated first part + "Dungeon")
        private val GENERIC_DUNGEON_PATTERN = Regex("^[A-Z][a-z]+ Dungeon$")

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
        // Regex to identify common date patterns like DD/MM/YYYY or MM/DD/YYYY
        private val DATE_PATTERN = Regex("^\\d{1,2}/\\d{1,2}/\\d{2,4}$")
    }

    // Helper function to check if debug logging is enabled
    private suspend fun isDebugEnabled(): Boolean {
        return try {
            settingsRepository.getSettings().debugMode
        } catch (e: Exception) {
            false // Default to false if we can't read settings
        }
    }

    private suspend fun debugLog(tag: String, message: String) {
        if (isDebugEnabled()) Log.d(tag, message)
    }

    fun canParse(data: List<ScreenData>): Boolean {
        runBlocking {
            debugLog(TAG, "=== DUNGEON DETECTION START ===")
            debugLog(TAG, "Checking ${data.size} screen items for dungeon indicators")
        }

        Log.d(TAG, "=== DUNGEON DETECTION START ===")
        Log.d(TAG, "Checking ${data.size} screen items for dungeon indicators")

        // Log first 20 items to see what we're working with
        Log.d(TAG, "Screen items sample:")
        data.take(20).forEach { Log.d(TAG, "Screen item: '${it.text}'") }

        // First, look for explicit dungeon indicators
        val explicitWorldDungeon = data.any { it.text.lowercase().contains("world dungeon") }
        val explicitSpecialDungeon = data.any { it.text.lowercase().contains("special dungeon") }
        val explicitGauntlet = data.any { it.text.startsWith("Battle a series of opponents") } &&
                data.any { it.text == "Runeshop" }

        if (explicitWorldDungeon || explicitSpecialDungeon || explicitGauntlet) {
            runBlocking {
                debugLog(
                    TAG,
                    "DUNGEON DETECTED: Explicit dungeon text found (world: $explicitWorldDungeon, special: $explicitSpecialDungeon, gauntlet: $explicitGauntlet)"
                )
            }
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

        // Check for dungeon-specific UI elements
        val hasDungeonMode = data.any {
            val lower = it.text.lowercase()
            lower.contains("normal mode") || lower.contains("hard mode") ||
                    lower.contains("boss mode") || lower.contains("endless mode")
        }
        val hasHoldToEnterButton = data.any { it.text.lowercase().contains("hold to enter") }
        val hasContinueFloor = data.any { it.text.lowercase().contains("continue floor") }
        val hasGeneralEnterOrContinueButton = data.any { // General enter/continue
            it.text.equals("Enter", ignoreCase = true) || it.text.equals("Continue", ignoreCase = true)
        }

        // Check for battle/victory screens in dungeons
        val hasVictory = data.any { it.text.lowercase().contains("victory") }
        val hasComplete = data.any { it.text.lowercase().contains("complete") }
        val hasDefeat = data.any { it.text.lowercase().contains("defeat") }

        // --- Floor detection logic ---
        var hasReliableFloorIndicator = false
        var ambiguousFloorItemText: String? = null
        var reliableFloorItemText: String? = null // For logging

        val floorItem = data.find { // Keep floorItem to access .text if needed for reliableFloorItemText
            val text = it.text
            val lower = text.lowercase()
            // Strong indicators: "Floor:" or "Floor "
            if (lower.contains("floor:") || lower.contains("floor ")) {
                hasReliableFloorIndicator = true
                reliableFloorItemText = text // Capture the text of the reliable indicator
                return@find true // Found a reliable floor item
            }
            // Weaker indicator: "X/Y" pattern, not a date
            if (text.matches(Regex(".*\\d+\\s*/\\s*\\d+.*")) && !DATE_PATTERN.matches(text)) {
                ambiguousFloorItemText = text // Store it, but don't consider it reliable yet
                return@find true // Found a potential floor item
            }
            false
        }

        // An ambiguous "X/Y" pattern is only considered a floor if other dungeon context exists
        val hasAmbiguousFloorWithContext = (ambiguousFloorItemText != null && !hasReliableFloorIndicator) &&
                (hasDungeonMode || hasHoldToEnterButton || hasContinueFloor || hasDungeonName || hasGeneralEnterOrContinueButton || hasVictory || hasComplete || hasDefeat)

        val finalHasFloor = hasReliableFloorIndicator || hasAmbiguousFloorWithContext
        val floorLogText = reliableFloorItemText ?: ambiguousFloorItemText // Prioritize reliable text for logging
        // --- End Floor detection logic ---

        val result = finalHasFloor || hasDungeonMode || hasHoldToEnterButton || hasContinueFloor ||
                (finalHasFloor && data.any { it.text.lowercase().contains("exit") }) ||
                ((hasVictory || hasComplete || hasDefeat) && finalHasFloor) ||
                hasDungeonName || hasGeneralEnterOrContinueButton

        runBlocking {
            debugLog(TAG, "=== DUNGEON DETECTION RESULT: $result ===")
            debugLog(TAG, "Detection details:")
            debugLog(TAG, "  - Explicit World dungeon: $explicitWorldDungeon")
            debugLog(TAG, "  - Explicit Special dungeon: $explicitSpecialDungeon")
        }
        return result
    }

    override suspend fun parseScreen(parsedScreen: ParsedScreen) {
        try {
            val dungeonName = extractDungeonName(parsedScreen.data)
            val dungeonMode = extractDungeonMode(parsedScreen.data)
            val floor = extractFloor(parsedScreen.data)
            val loot = extractLoot(parsedScreen.data)

            // Check if we're entering a new dungeon and validate the dungeon name
            if (dungeonName != null && _currentDungeonVisit.value?.name != dungeonName) {
                // Validate dungeon name against our list of valid dungeons or generic pattern
                val isValidDungeon = VALID_DUNGEONS.contains(dungeonName) || 
                                    GENERIC_DUNGEON_PATTERN.matches(dungeonName)

                if (isValidDungeon) {
                    // Check if we already have a visit for this dungeon in onHoldVisits
                    val existingVisit = onHoldVisits[dungeonName]

                    if (existingVisit != null && existingVisit.mode.type == dungeonMode.type) {
                        // Use the existing visit instead of creating a new one
                        _currentDungeonVisit.value = existingVisit
                        // Remove it from onHoldVisits since we're now tracking it again
                        onHoldVisits.remove(dungeonName)
                        logUtils.d(TAG, "Resumed tracking existing dungeon visit: $dungeonName (mode: ${dungeonMode.type})")
                    } else {
                        // Create a new visit
                        val visit = trackDungeonVisitUseCase(dungeonName, dungeonMode)
                        _currentDungeonVisit.value = visit
                        logUtils.d(TAG, "Started tracking new dungeon visit: $dungeonName (mode: ${dungeonMode.type})")
                    }
                } else {
                    logUtils.d(TAG, "Ignoring invalid dungeon: $dungeonName")
                }
            }

            // Update current visit with new data (only if we're tracking a valid dungeon)
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
                    // Store the current visit in onHoldVisits before clearing it
                    // This allows the player to re-enter the same dungeon and continue with the existing visit
                    val visit = _currentDungeonVisit.value
                    if (visit != null) {
                        onHoldVisits[visit.name] = visit
                        logUtils.d(TAG, "Dungeon visit completed and stored: ${visit.name}")
                    }
                    _currentDungeonVisit.value = null
                }
            }

        } catch (e: Exception) {
            logUtils.e(TAG, "Error parsing dungeon screen", e)
        }
    }

    fun parseState(data: List<ScreenData>, currentState: DungeonState?): DungeonState {
        runBlocking {
            debugLog(TAG, "=== PARSE STATE START ===")
            debugLog(TAG, "Current state: $currentState")
            debugLog(TAG, "Screen items for parsing:")
            debugLog(TAG, "Total items: ${data.size}")
        }
        Log.d(TAG, "=== PARSE STATE START ===")
        Log.d(TAG, "Current state: $currentState")
        Log.d(TAG, "Screen items for parsing:")
        Log.d(TAG, "Total items: ${data.size}")

        val state = currentState ?: DungeonState()

        // Check if we have a stored dungeon name and we're still in a dungeon
        val storedName = dungeonStateTracker.getLastKnownDungeonName()
        if (storedName != null && data.any { it.text.contains("Floor", ignoreCase = true) }) {
            // Validate stored name against our list of valid dungeons or generic pattern
            val isValidDungeon = VALID_DUNGEONS.contains(storedName) || 
                                GENERIC_DUNGEON_PATTERN.matches(storedName)

            if (isValidDungeon) {
                Log.d(TAG, "Using stored dungeon name: $storedName (valid dungeon)")
            } else {
                Log.d(TAG, "Ignoring invalid stored dungeon: $storedName")
                dungeonStateTracker.clear() // Clear invalid stored dungeon
            }
        }

        // Only try to extract new name if we don't have one or if we see clear dungeon entry
        val dungeonName =
            if (state.dungeonName.isEmpty() || state.dungeonName == "Unknown Dungeon" ||
                data.any {
                    it.text.contains("world dungeon", ignoreCase = true) ||
                            it.text.contains("special dungeon", ignoreCase = true)
                }
            ) {
                extractDungeonNameFromData(data) ?: state.dungeonName
            } else state.dungeonName

        Log.d(TAG, "Extracted dungeon name: '$dungeonName' (was: '${state.dungeonName}')")

        // Validate dungeon name against our list of valid dungeons or generic pattern
        val isValidDungeon = dungeonName.isNotEmpty() && 
                            (VALID_DUNGEONS.contains(dungeonName) || 
                            GENERIC_DUNGEON_PATTERN.matches(dungeonName))

        // Store the dungeon name if we found one and it's valid
        if (isValidDungeon && dungeonName != "Unknown Dungeon") {
            Log.d(TAG, "Storing valid dungeon name: $dungeonName")
            dungeonStateTracker.updateDungeonName(dungeonName)
        } else if (dungeonName.isNotEmpty() && !isValidDungeon) {
            Log.d(TAG, "Ignoring invalid dungeon: $dungeonName")
            // Don't update with invalid dungeon name
        } else if (storedName != null && dungeonName.isEmpty() && 
                  (VALID_DUNGEONS.contains(storedName) || GENERIC_DUNGEON_PATTERN.matches(storedName))) {
            // Use stored name if we couldn't extract one but we're still in a dungeon and it's valid
            return state.copy(dungeonName = storedName)
        }

        // Log key screen elements for debugging (only if debugLog would log)
        data.filter {
            it.text.contains("Floor", ignoreCase = true) ||
                    it.text.contains("mode", ignoreCase = true) ||
                    it.text.contains("enter", ignoreCase = true)
        }.forEach { Log.d(TAG, "Key element: '${it.text}'") }

        // Only mark as new dungeon if we're actually seeing a dungeon selection screen
        val isDungeonSelectionScreen = data.any {
            it.text.contains("world dungeon", ignoreCase = true) ||
                    it.text.contains("special dungeon", ignoreCase = true) ||
                    it.text.contains("hold to enter", ignoreCase = true)
        }

        if (isValidDungeon && dungeonName != state.dungeonName && state.dungeonName.isNotEmpty() && isDungeonSelectionScreen) {
            Log.d(TAG, "DIFFERENT DUNGEON DETECTED: '$dungeonName' vs '${state.dungeonName}'")
            return DungeonState(dungeonName = dungeonName, isEnteringNewDungeon = true)
        }

        // Only update state with valid dungeon names
        var newState = if (isValidDungeon) {
            state.copy(dungeonName = dungeonName)
        } else {
            // Keep the existing dungeon name if it's valid, otherwise use empty string
            if (state.dungeonName.isNotEmpty() && 
                (VALID_DUNGEONS.contains(state.dungeonName) || 
                GENERIC_DUNGEON_PATTERN.matches(state.dungeonName))) {
                state
            } else {
                state.copy(dungeonName = "")
            }
        }

        newState = parseFloorAndEntry(data, newState)
        newState = parseDungeonMode(data, newState)

        Log.d(
            TAG,
            "After parsing - hasEntered: ${newState.hasEntered}, floor: ${newState.floorNumber}, mode: ${newState.mode}"
        )

        // Handle endless mode timer
        if (newState.mode.type == DungeonMode.Type.ENDLESS && (currentState == null || !currentState.hasEntered)) {
            // Start timer for endless mode
            Log.d(TAG, "Starting timer for endless mode dungeon")
            // The timer is already tracked via the DungeonVisit's startTime
            // We just need to make sure we're tracking the max floor reached
        }

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

        // Handle endless mode completion or defeat
        if (newState.mode.type == DungeonMode.Type.ENDLESS && 
            (isCompletedScreen(data) || data.any { it.text.contains("DEFEAT", ignoreCase = true) })) {
            // Stop timer and record max floor
            Log.d(TAG, "Endless mode ended at floor: ${newState.floorNumber}")
            newState = newState.copy(isDone = true)
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
                            text.contains("ENDLESS", ignoreCase = true))
                ) {
                    // Extract dungeon name from "Bandit Lord NORMAL Floor 5"
                    val match = Regex(
                        "(.+?)\\s+(NORMAL|HARD|BOSS|ENDLESS)\\s+Floor",
                        RegexOption.IGNORE_CASE
                    )
                        .find(text)
                    match?.groupValues?.get(1)?.trim()?.let { name ->
                        // Validate the extracted name
                        if (VALID_DUNGEONS.contains(name) || GENERIC_DUNGEON_PATTERN.matches(name)) {
                            Log.d(TAG, "Found valid dungeon name from completion screen: $name")
                            return name
                        } else {
                            Log.d(TAG, "Found invalid dungeon name from completion screen: $name")
                        }
                    }
                }
            }
            // Also check for just the dungeon name above DUNGEON COMPLETE
            if (completeIndex >= 2) {
                val possibleName = data[completeIndex - 2].text
                if (!possibleName.contains("Floor") && possibleName.length > 3) {
                    // Validate the extracted name
                    if (VALID_DUNGEONS.contains(possibleName) || GENERIC_DUNGEON_PATTERN.matches(possibleName)) {
                        Log.d(TAG, "Found valid dungeon name above DUNGEON COMPLETE: $possibleName")
                        return possibleName
                    } else {
                        Log.d(TAG, "Found invalid dungeon name above DUNGEON COMPLETE: $possibleName")
                    }
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
                    text.contains("Underworld")
                ) {
                    val extractedName = text.replace(" Dungeon", "")
                        .replace(" Gauntlet", "")

                    // Validate the extracted name
                    if (VALID_DUNGEONS.contains(extractedName) || GENERIC_DUNGEON_PATTERN.matches(extractedName)) {
                        Log.d(TAG, "Found valid dungeon name near Enter button: $extractedName")
                        return extractedName
                    } else {
                        Log.d(TAG, "Found invalid dungeon name near Enter button: $extractedName")
                    }
                }
            }
        }

        // NEW: Look for dungeon name when we're already inside (have floor info)
        if (data.any { it.text.lowercase().contains("floor") && it.text.contains("/") }) {
            // Strategy 1: Look for capitalized phrases that could be dungeon names
            // Skip common UI elements and look for actual dungeon names
            val potentialNames = data.filter { item ->
                val text = item.text.trim()
                text.length in 5..50 &&
                        text[0].isUpperCase() &&
                        !text.contains("Floor") &&
                        !text.contains("HP") &&
                        !text.contains("MP") &&
                        !text.all { it.isDigit() || it == ',' } &&
                        !INVALID_ITEM_NAMES.any { invalid ->
                            text.lowercase().contains(invalid.lowercase())
                        } &&
                        !text.matches(Regex("\\d+,?\\d*")) && // Not just numbers
                        !text.matches(Regex("^[0-9_]+$")) // Not just numbers and underscores
            }
                .sortedByDescending { it.text.length } // Longer names are more likely to be dungeon names

            // Filter potential names to only include valid dungeons
            val validPotentialNames = potentialNames.filter { item ->
                VALID_DUNGEONS.contains(item.text) || GENERIC_DUNGEON_PATTERN.matches(item.text)
            }

            validPotentialNames.firstOrNull()?.let {
                Log.d(TAG, "Found valid dungeon name from mid-dungeon: ${it.text}")
                return it.text
            }

            if (validPotentialNames.isEmpty() && potentialNames.isNotEmpty()) {
                Log.d(TAG, "Found potential dungeon names but none were valid: ${potentialNames.map { it.text }}")
            }
        }

        // Try to extract dungeon name from battle log entries
        var nameNext = false
        for (item in data) {
            // Try to extract dungeon name from battle log entries
            if (item.text.contains("entered", ignoreCase = true) &&
                (item.text.contains("dungeon", ignoreCase = true) ||
                        item.text.contains("gauntlet", ignoreCase = true))
            ) {
                // Extract name from "You entered X Dungeon" style messages
                val match =
                    Regex("entered\\s+(.+?)(?:\\s+[Dd]ungeon|\\s+[Gg]auntlet)?$").find(item.text)
                match?.let {
                    val extractedName = it.groupValues[1].trim()
                    // Validate the extracted name
                    if (VALID_DUNGEONS.contains(extractedName) || GENERIC_DUNGEON_PATTERN.matches(extractedName)) {
                        Log.d(TAG, "Found valid dungeon name from battle log: $extractedName")
                        return extractedName
                    } else {
                        Log.d(TAG, "Found invalid dungeon name from battle log: $extractedName")
                    }
                }
            }

            if (nameNext) {
                val extractedName = item.text
                // Validate the extracted name
                if (VALID_DUNGEONS.contains(extractedName) || GENERIC_DUNGEON_PATTERN.matches(extractedName)) {
                    Log.d(TAG, "Found valid dungeon name after world/special dungeon: $extractedName")
                    return extractedName
                } else {
                    Log.d(TAG, "Found invalid dungeon name after world/special dungeon: $extractedName")
                    nameNext = false // Reset flag since this name was invalid
                }
            } else if (item.text.lowercase().contains("world dungeon") ||
                item.text.lowercase().contains("special dungeon")
            ) {
                nameNext = true
            }
        }

        if (data.any { it.text.startsWith("Battle a series of opponents") } &&
            data.any { it.text == "Runeshop" }) {
            // Personal gauntlet is a special case, check if it's in our valid dungeons list
            if (VALID_DUNGEONS.contains("Personal gauntlet")) {
                Log.d(TAG, "Found Personal gauntlet")
                return "Personal gauntlet"
            } else {
                Log.d(TAG, "Personal gauntlet is not in the valid dungeons list")
            }
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
                (it.text.endsWith(" Dungeon") || it.text.endsWith(" Gauntlet")) &&
                (VALID_DUNGEONS.contains(it.text.replace(" Dungeon", "").replace(" Gauntlet", "")) || 
                GENERIC_DUNGEON_PATTERN.matches(it.text))
            }?.let { 
                val extractedName = it.text.replace(" Dungeon", "").replace(" Gauntlet", "")
                Log.d(TAG, "Found valid dungeon name ending with Dungeon/Gauntlet: $extractedName")
                return extractedName 
            }
        }

        if (data.any { it.text.lowercase().contains("floor") }) {
            // NEW: Try harder to find dungeon name by looking at battle log
            val battleLogStart =
                data.indexOfFirst { it.text.contains("battle_log", ignoreCase = true) }
            if (battleLogStart >= 0) {
                // Look for text after battle_log that might be dungeon name
                for (i in (battleLogStart + 1) until data.size.coerceAtMost(battleLogStart + 10)) {
                    val text = data[i].text
                    if (text.length > 3 &&
                        text[0].isUpperCase() &&
                        !text.contains("Floor") &&
                        !text.matches(Regex("\\d+,?\\d*"))
                    ) {
                        // Validate the extracted name
                        if (VALID_DUNGEONS.contains(text) || GENERIC_DUNGEON_PATTERN.matches(text)) {
                            Log.d(TAG, "Found valid dungeon name near battle_log: $text")
                            return text
                        } else {
                            Log.d(TAG, "Found invalid dungeon name near battle_log: $text")
                        }
                    }
                }
            }

            Log.d(TAG, "Has floor info but couldn't extract dungeon name after extensive search")
            Log.d(TAG, "Using fallback dungeon name: Standard Dungeon")
            return "Standard Dungeon"
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
            else if (texts.any { it.contains("endless") && it.contains("✓") }) type =
                DungeonMode.Type.ENDLESS
        }

        Log.d(TAG, "Extracted mode: $type, hard: $isHard")
        return DungeonMode(type, isHard)
    }

    private fun extractFloor(screenData: List<ScreenData>): Long? {
        Log.d(TAG, "=== EXTRACTING FLOOR ===")

        // First check for endless mode floor pattern with infinity symbol
        val endlessFloorText = screenData.find {
            it.text.contains("Floor", ignoreCase = true) && 
            (it.text.contains("∞") || it.text.contains("infinity", ignoreCase = true))
        }?.text

        if (endlessFloorText != null) {
            // Extract the current floor number from endless format
            val endlessPattern = Regex("Floor\\s*(\\d+)\\s*[/]?\\s*[∞∞infinity]", RegexOption.IGNORE_CASE)
            val match = endlessPattern.find(endlessFloorText)
            if (match != null) {
                val floorNumber = match.groupValues[1].toLongOrNull()
                if (floorNumber != null) {
                    Log.d(TAG, "Extracted endless mode floor: $floorNumber from '$endlessFloorText'")
                    return floorNumber
                }
            }
        }

        // Existing floor extraction code remains the same
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

        // Check if this is a dungeon completion screen
        val isDungeonComplete = data.any { it.text.equals("DUNGEON COMPLETE!", ignoreCase = true) }

        // Find "Here's what you found:" as our starting point
        val foundIndex = data.indexOfFirst {
            it.text.contains("Here's what you found", ignoreCase = true)
        }

        if (foundIndex == -1) {
            Log.d(TAG, "No 'Here's what you found' text found, cannot parse loot")
            return loot
        }

        Log.d(
            TAG,
            "Parsing loot from ${data.size} screen items (dungeon complete: $isDungeonComplete)"
        )

        // Debug: Log all screen items to see what we're working with
        Log.d(TAG, "=== ALL SCREEN ITEMS FOR LOOT PARSING ===")
        data.forEachIndexed { index, item ->
            Log.d(TAG, "[$index]: '${item.text}'")
        }
        Log.d(TAG, "=== END SCREEN ITEMS ===")

        // Parse rewards using label-then-value pattern
        var i = foundIndex + 1
        while (i < data.size) {
            val currentText = data[i].text.trim().lowercase()

            // Stop at UI elements
            if (currentText == "continue" || currentText.contains("floor")) {
                break
            }

            // Check if this is a reward label
            when {
                currentText == "exp" || currentText == "experience" -> {
                    // Next item should be the value
                    if (i + 1 < data.size) {
                        val valueText = data[i + 1].text.trim()
                        val value = valueText.replace(",", "").toIntOrNull()
                        if (value != null) {
                            loot["experience"] = value
                            Log.d(TAG, "Found experience: $value")
                            i++ // Skip the value we just processed
                        } else {
                            Log.d(TAG, "Failed to parse experience value from: '${data[i + 1].text}'")
                        }
                    }
                }
                currentText == "gold" -> {
                    // Look for the gold value - might be a few items ahead
                    Log.d(TAG, "Found gold label at index $i, searching for value...")
                    var goldFound = false
                    for (j in (i + 1) until minOf(i + 4, data.size)) {
                        val valueText = data[j].text.trim()
                        // Skip "party experience" or other labels
                        if (valueText.contains("party", ignoreCase = true) ||
                            valueText.contains("experience", ignoreCase = true)) {
                            continue
                        }
                        val value = valueText.replace(",", "").toIntOrNull()
                        Log.d(TAG, "Checking potential gold value at index $j: '$valueText' -> $value")
                        if (value != null && value > 0) {
                            loot["gold"] = value
                            Log.d(TAG, "Found gold: $value")
                            goldFound = true
                            i = j // Skip to after the value
                            break
                        }
                    }
                    if (!goldFound) {
                        Log.d(TAG, "Gold label found but no value")
                    }
                }
                currentText == "orns" -> {
                    // Next item should be the value
                    if (i + 1 < data.size) {
                        val valueText = data[i + 1].text.trim()
                        val value = valueText.replace(",", "").toIntOrNull()
                        if (value != null) {
                            loot["orns"] = value
                            Log.d(TAG, "Found orns: $value")
                        } else {
                            Log.d(TAG, "Failed to parse orns value from: '${data[i + 1].text}'")
                            i++ // Skip the value we just processed
                        }
                    }
                }
            }

            // NEW: Also check for fragmented patterns where reward type and value are separate
            // Handle cases like: 'gold' -> '23,299' or 'orns' -> '274'
            if (i + 1 < data.size) {
                val nextText = data[i + 1].text.trim()
                val numberMatch = Regex("^(\\d{1,3}(?:,\\d{3})*)$").find(nextText)

                if (numberMatch != null) {
                    val value = numberMatch.groupValues[1].replace(",", "").toIntOrNull() ?: 0

                    when (currentText) {
                        "gold" -> {
                            if (loot["gold"] == null) { // Only add if not already found
                                loot["gold"] = value
                                Log.d(TAG, "Found fragmented gold: $value")
                                i++ // Skip the number
                            }
                        }
                        "orns" -> {
                            if (loot["orns"] == null) { // Only add if not already found
                                loot["orns"] = value
                                Log.d(TAG, "Found fragmented orns: $value")
                                i++ // Skip the number
                            }
                        }
                        "exp", "experience" -> {
                            if (loot["experience"] == null) { // Only add if not already found
                                loot["experience"] = value
                                Log.d(TAG, "Found fragmented experience: $value")
                                i++ // Skip the number
                            }
                        }
                    }
                }
            }

            i++
        }

        // If we didn't find orns but it should be there (305), log more context
        if (!loot.containsKey("orns") && isDungeonComplete) {
            Log.d(TAG, "Orns not found in dungeon complete screen. Looking for small numbers...")
            // Look for any small numbers that might be orns
            for (i in foundIndex until data.size) {
                val text = data[i].text.trim()
                val value = text.replace(",", "").toIntOrNull()
                if (value != null && value < 1000 && value > 0) {
                    Log.d(TAG, "Found small number that might be orns: $value at index $i")
                }
            }
        }

        if (loot.isEmpty()) {
            Log.w(TAG, "No loot found after parsing. Check screen data above for reward text patterns.")
        } else {
            Log.d(TAG, "Successfully parsed loot: $loot")
        }

        Log.d(TAG, "Parsed loot: $loot")
        return loot
    }

    fun parseBattleLoot(data: List<ScreenData>): Map<String, Int> {
        val loot = mutableMapOf<String, Int>()

        // Find the VICTORY! text first
        val victoryIndex = data.indexOfFirst { it.text.equals("VICTORY!", ignoreCase = true) }
        if (victoryIndex == -1) return loot

        // Start parsing after "Here's what you found:"
        val startIndex = data.indexOfFirst { it.text.contains("Here's what you found", ignoreCase = true) }
            .takeIf { it >= 0 } ?: (victoryIndex + 1)

        Log.d(TAG, "Parsing battle loot from victory screen (starting at index $startIndex)")

        // Debug: Log reward structure
        Log.d(TAG, "=== VICTORY REWARDS DATA ===")
        for (i in startIndex until data.size.coerceAtMost(startIndex + 15)) {
            Log.d(TAG, "[$i]: '${data[i].text}'")
        }
        Log.d(TAG, "=== END REWARDS DATA ===")

        // Parse rewards - the structure is: label, number, description
        var i = startIndex + 1
        while (i < data.size) {
            val text = data[i].text.trim()
            val textLower = text.lowercase()

            // Stop at materials or CONTINUE
            if (textLower == "continue" || textLower.contains("iron") || textLower.contains("wood")) break

            when {
                textLower == "exp" || textLower == "experience" -> {
                    // Next element should be the number
                    if (i + 1 < data.size && !loot.containsKey("experience")) {
                        val nextText = data[i + 1].text.trim()
                        val value = nextText.replace(",", "").toIntOrNull()
                        if (value != null) {
                            loot["experience"] = value
                            Log.d(TAG, "Found battle experience: $value")
                            i++ // Skip the number we just processed
                        }
                    }
                }
                textLower == "gold" -> {
                    // Next element should be the number
                    if (i + 1 < data.size) {
                        val nextText = data[i + 1].text.trim()
                        val value = nextText.replace(",", "").toIntOrNull()
                        if (value != null) {
                            // Check if this is kingdom gold (skip if so)
                            if (i + 2 < data.size && data[i + 2].text.contains("kingdom", ignoreCase = true)) {
                                Log.d(TAG, "Skipping kingdom gold: $value")
                                i += 2 // Skip number and "kingdom gold"
                                continue
                            }
                            // Only store first gold value (player gold, not kingdom)
                            if (!loot.containsKey("gold")) {
                                loot["gold"] = value
                                Log.d(TAG, "Found battle gold: $value")
                            }
                            i++ // Skip the number we just processed
                        }
                    }
                }
                textLower == "orns" -> {
                    // Next element should be the number
                    if (i + 1 < data.size && !loot.containsKey("orns")) {
                        val nextText = data[i + 1].text.trim()
                        val value = nextText.replace(",", "").toIntOrNull()
                        if (value != null) {
                            loot["orns"] = value
                            Log.d(TAG, "Found battle orns: $value")
                            i++ // Skip the number we just processed
                        }
                    }
                }
            }
            i++
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
                    // Handle this case
                } else {
                    // Handle all other cases
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
