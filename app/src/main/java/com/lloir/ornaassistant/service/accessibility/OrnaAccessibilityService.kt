package com.lloir.ornaassistant.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import com.lloir.ornaassistant.domain.model.DungeonState
import com.lloir.ornaassistant.domain.model.DungeonVisit
import com.lloir.ornaassistant.domain.model.FloorReward
import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.domain.model.ScreenData
import com.lloir.ornaassistant.domain.model.ScreenType
import com.lloir.ornaassistant.domain.model.WayvesselSession
import com.lloir.ornaassistant.service.overlay.OverlayManager
import com.lloir.ornaassistant.service.parser.impl.DungeonScreenParser
import com.lloir.ornaassistant.service.parser.ScreenParserManager
import com.lloir.ornaassistant.service.parser.DungeonStateTracker
import com.lloir.ornaassistant.domain.repository.DungeonRepository
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import com.lloir.ornaassistant.domain.repository.WayvesselRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.LocalDateTime
import javax.inject.Inject
import android.R.attr.data
@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.O)
class OrnaAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var overlayManager: OverlayManager

    @Inject
    lateinit var screenParserManager: ScreenParserManager

    @Inject
    lateinit var dungeonScreenParser: DungeonScreenParser

    @Inject
    lateinit var dungeonStateTracker: DungeonStateTracker

    @Inject
    lateinit var dungeonRepository: DungeonRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var wayvesselRepository: WayvesselRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _screenDataFlow = MutableSharedFlow<ParsedScreen>(replay = 0)
    val screenDataFlow = _screenDataFlow.asSharedFlow()

    private var lastProcessTime = 0L
    private val minProcessInterval = 500L // Minimum 500ms between processing events

    private var isServiceReady = false
    private var initializationJob: Job? = null

    private var currentDungeonState: DungeonState? = null
    private var currentDungeonVisit: DungeonVisit? = null
    private var onHoldVisits = mutableMapOf<String, DungeonVisit>()
    private var currentWayvesselSession: WayvesselSession? = null
    private var lastDungeonCreationTime = 0L // Track when we last created a dungeon
    private val recentlyCreatedDungeons = mutableMapOf<String, Long>() // Track recent dungeons by name

    // Track recent victory/completion for reward parsing
    private var recentVictoryTime = 0L
    private var awaitingRewards = false

    companion object {
        private const val TAG = "OrnaAccessibilityService"
        private const val SERVICE_READY_DELAY = 1000L // 1 second

        // Supported packages
        private val SUPPORTED_PACKAGES = setOf(
            "playorna.com.orna",
            "com.discord"
        )

        // Noise patterns to filter out
        private val NOISE_PATTERNS = listOf(
            Regex("^[0-9_]+$"), // Pure numbers with underscores
            Regex("^chat.*", RegexOption.IGNORE_CASE), // Chat-related
            Regex("^\\d+_[a-z]$") // Patterns like "3_m"
        )
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Accessibility service created")
        observeSettings()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")

        // Configure service info
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
            packageNames = SUPPORTED_PACKAGES.toTypedArray()
        }

        // Set the service reference IMMEDIATELY
        overlayManager.setAccessibilityService(this)

        // Initialize with a shorter delay since we're setting the reference immediately
        initializationJob = serviceScope.launch {
            try {
                Log.d(TAG, "Starting initialization with ${SERVICE_READY_DELAY}ms delay...")
                delay(SERVICE_READY_DELAY)

                // Double-check that the service is still connected
                if (serviceInfo != null) {
                    Log.d(TAG, "Service confirmed connected, initializing overlay manager...")
                    overlayManager.initialize()
                    isServiceReady = true
                    Log.i(TAG, "Accessibility service initialization completed successfully")
                } else {
                    Log.w(TAG, "Service is no longer connected during initialization")
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Initialization cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Error during service initialization", e)
                // Retry initialization once after a delay
                delay(500)
                try {
                    overlayManager.initialize()
                    isServiceReady = true
                    Log.i(TAG, "Accessibility service initialization retry succeeded")
                } catch (retryException: Exception) {
                    Log.e(TAG, "Retry initialization also failed", retryException)
                }
            }
        }
    }

    private fun observeSettings() {
        serviceScope.launch {
            settingsRepository.getSettingsFlow().collect { settings ->
                if (settings.showSessionOverlay) {
                    if (currentWayvesselSession != null || currentDungeonVisit != null) {
                        overlayManager.showSessionOverlay(
                            currentWayvesselSession,
                            currentDungeonVisit
                        )
                    }
                } else {
                    overlayManager.hideSessionOverlay()
                }

                if (!settings.showInvitesOverlay) {
                    overlayManager.hideInvitesOverlay()
                }

                if (!settings.showAssessOverlay) {
                    overlayManager.hideAssessmentOverlay()
                }

                // Update overlay transparency
                overlayManager.setOverlayTransparency(settings.overlayTransparency)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Don't process events until service is ready
        if (!isServiceReady || event?.source == null) {
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessTime < minProcessInterval) {
            return // Throttle events to prevent overwhelming the system
        }
        lastProcessTime = currentTime

        val packageName = event.packageName?.toString() ?: return

        if (!SUPPORTED_PACKAGES.contains(packageName)) {
            return
        }

        // Process the screen data in a background coroutine with error handling
        serviceScope.launch(Dispatchers.Default) {
            var sourceNode: AccessibilityNodeInfo? = null
            try {
                sourceNode = event.source
                if (sourceNode == null) {
                    Log.w(TAG, "Source node is null, skipping event")
                    return@launch
                }

                val screenData = parseAccessibilityTree(sourceNode)
                if (screenData.isEmpty()) {
                    Log.d(TAG, "No screen data extracted, skipping")
                    return@launch
                }

                // Debug: Log first few items to see what we're getting
                if (screenData.size > 0) {
                    Log.d(TAG, "Screen data sample (${screenData.size} items):")
                    screenData.take(10).forEach { data ->
                        Log.d(TAG, "  - '${data.text}'")
                    }
                }

                val screenType = determineScreenType(screenData)
                val parsedScreen = ParsedScreen(
                    screenType = screenType,
                    data = screenData,
                    timestamp = LocalDateTime.now()
                )

                // Clear assessment data if we're not on an item detail screen
                if (screenType != ScreenType.ITEM_DETAIL) {
                    withContext(Dispatchers.Main) {
                        screenParserManager.clearItemAssessment()
                    }
                }

                // Emit the parsed screen data
                _screenDataFlow.emit(parsedScreen)

                // Update overlays on main thread with additional safety checks
                withContext(Dispatchers.Main) {
                    if (isServiceReady) {
                        overlayManager.handleScreenUpdate(parsedScreen)
                    }
                }

                // Always check for dungeon screens, regardless of detected screen type
                val previousDungeonState = currentDungeonState
                val isDungeonScreen = dungeonScreenParser.canParse(screenData)
                Log.d(TAG, "Is dungeon screen: $isDungeonScreen, detected type: $screenType")

                if (isDungeonScreen) {
                    try {
                        val newState =
                            dungeonScreenParser.parseState(screenData, currentDungeonState)
                        Log.d(TAG, "Current dungeon state: $currentDungeonState")
                        Log.d(TAG, "New dungeon state: $newState")

                        // Always update state and handle changes
                        handleDungeonStateChange(newState, screenData)

                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing dungeon state", e)
                    }
                } else if (previousDungeonState?.hasEntered == true && !isDungeonScreen) {
                    // We've left the dungeon screen but haven't seen completion
                    // Don't clear the visit yet - they might be in inventory or something
                    Log.d(TAG, "Left dungeon screen but keeping visit active")
                } else if (screenType == ScreenType.WAYVESSEL && currentDungeonVisit != null) {
                    // Back at wayvessel - dungeon might be done
                    Log.d(TAG, "At wayvessel screen with active dungeon visit")
                }

                // Check for victory or completion screens
                val hasVictoryScreen =
                    screenData.any { it.text.equals("VICTORY!", ignoreCase = true) }
                if (hasVictoryScreen) {
                    Log.d(TAG, "=== VICTORY SCREEN DETECTED ===")
                    Log.d(TAG, "Looking for rewards in ${screenData.size} items")
                }
                
                val hasDungeonComplete =
                    screenData.any { it.text.equals("DUNGEON COMPLETE!", ignoreCase = true) }
                if (hasDungeonComplete) {
                    Log.d(TAG, "=== DUNGEON COMPLETE SCREEN DETECTED ===")
                    Log.d(TAG, "Looking for rewards in ${screenData.size} items")
                }

                if (hasVictoryScreen || hasDungeonComplete) {
                    recentVictoryTime = System.currentTimeMillis()
                    awaitingRewards = true
                    Log.d(TAG, "Victory/completion detected, awaiting rewards...")
                    // Log what comes after victory/complete text
                    val victoryIndex = screenData.indexOfFirst { 
                        it.text.equals("VICTORY!", ignoreCase = true) || 
                        it.text.equals("DUNGEON COMPLETE!", ignoreCase = true) 
                    }
                    if (victoryIndex >= 0 && victoryIndex < screenData.size - 5) {
                        Log.d(TAG, "Items after victory/complete:")
                        for (i in 1..5) {
                            if (victoryIndex + i < screenData.size) {
                                Log.d(TAG, "  +$i: '${screenData[victoryIndex + i].text}'")
                            }
                        }
                    }
                }

                // Check if we're seeing potential reward numbers shortly after victory
                // Remove the incorrect code that treats all numbers as orns
                // The proper parsing is done in DungeonScreenParser

                if (hasVictoryScreen) {
                    try {
                        Log.d(TAG, "Victory screen detected!")
                        val battleLoot = dungeonScreenParser.parseBattleLoot(screenData)

                        Log.d(
                            TAG,
                            "Battle loot parsed: orns=${battleLoot["orns"]}, gold=${battleLoot["gold"]}, exp=${battleLoot["experience"]}"
                        )
                        Log.d(
                            TAG,
                            "Current dungeon visit: ${currentDungeonVisit?.name} (ID: ${currentDungeonVisit?.id})"
                        )

                        if (battleLoot.isNotEmpty()) {
                            // Add to current dungeon visit if one exists
                            currentDungeonVisit?.let { visit ->
                                Log.d(
                                    TAG,
                                    "Current visit before update: orns=${visit.orns}, gold=${visit.gold}, exp=${visit.experience}"
                                )
                                Log.d(
                                    TAG,
                                    "Battle loot to add: orns=${battleLoot["orns"]}, gold=${battleLoot["gold"]}, exp=${battleLoot["experience"]}"
                                )
                                
                                // Create updated visit with new values

                                val updatedVisit = visit.copy(
                                    battleOrns = visit.battleOrns + (battleLoot["orns"] ?: 0),
                                    battleGold = visit.battleGold + (battleLoot["gold"] ?: 0),
                                    battleExperience = visit.battleExperience + (battleLoot["experience"]
                                        ?: 0),
                                    // Update totals
                                    orns = visit.orns + (battleLoot["orns"] ?: 0),
                                    gold = visit.gold + (battleLoot["gold"] ?: 0),
                                    experience = visit.experience + (battleLoot["experience"] ?: 0)
                                )
                                
                                // Important: Update the currentDungeonVisit reference
                                currentDungeonVisit = updatedVisit
                                
                                Log.d(
                                    TAG,
                                    "Updated visit: orns=${updatedVisit.orns}, gold=${updatedVisit.gold}, exp=${updatedVisit.experience}"
                                )
                                
                                
                                // Update database with the new values
                                serviceScope.launch {
                                    dungeonRepository.updateVisit(updatedVisit)
                                    Log.d(TAG, "Database updated with battle loot")
                                }
                                updateOverlay()

                                Log.d(
                                    TAG,
                                    "Added battle loot - orns: ${battleLoot["orns"]}, gold: ${battleLoot["gold"]}, exp: ${battleLoot["experience"]}"
                                )
                            }

                            // Also update wayvessel session if active
                            currentWayvesselSession?.let { session ->
                                val updatedSession = session.copy(
                                    orns = session.orns + (battleLoot["orns"] ?: 0),
                                    gold = session.gold + (battleLoot["gold"] ?: 0),
                                    experience = session.experience + (battleLoot["experience"]
                                        ?: 0)
                                )
                                currentWayvesselSession = updatedSession
                                serviceScope.launch {
                                    updateWayvesselSessionInDatabase()
                                }
                                // Update overlay to show new session totals
                                updateOverlay()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing victory screen", e)
                    }
                }

                // Check for wayvessel activation
                if (screenData.any {
                        it.text.contains(
                            "This wayvessel is active",
                            ignoreCase = true
                        )
                    }) {
                    val wayvesselName = screenData.find { it.text.contains("'s Wayvessel") }
                        ?.text?.replace("'s Wayvessel", "")
                    if (wayvesselName != null && currentWayvesselSession?.name != wayvesselName) {
                        handleWayvesselStart(wayvesselName)
                    }
                }

                // Check if wayvessel is no longer active (user left wayvessel)
                if (currentWayvesselSession != null && 
                    !screenData.any { it.text.contains("This wayvessel is active", ignoreCase = true) } &&
                    !isDungeonScreen &&
                    screenType != ScreenType.BATTLE) {
                    // We're no longer in wayvessel or dungeon - might have left
                    Log.d(TAG, "No longer in wayvessel, checking if we should end session...")
                    // Don't end immediately - they might be in inventory or something
                    // This would need a more sophisticated check
                }

                // Process general screen parsing
                screenParserManager.processScreen(parsedScreen)

            } catch (e: CancellationException) {
                Log.d(TAG, "Processing cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing accessibility event", e)
            } finally {
                // Always recycle the source node to prevent memory leaks
                try {
                    sourceNode?.recycle()
                } catch (e: Exception) {
                    Log.w(TAG, "Error recycling source node", e)
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
        isServiceReady = false
    }

    override fun onDestroy() {
        Log.d(TAG, "Accessibility service destroying...")
        isServiceReady = false

        // Cancel initialization if it's still running
        initializationJob?.cancel()

        // Clean up overlays and clear service reference
        serviceScope.launch {
            try {
                overlayManager.cleanup()
            } catch (e: Exception) {
                Log.e(TAG, "Error during overlay cleanup", e)
            }
        }

        // Cancel the service scope
        serviceScope.cancel()

        super.onDestroy()
        Log.d(TAG, "Accessibility service destroyed")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "Accessibility service unbound")
        isServiceReady = false
        overlayManager.clearAccessibilityService()
        return super.onUnbind(intent)
    }

    private fun parseAccessibilityTree(rootNode: AccessibilityNodeInfo?): List<ScreenData> {
        if (rootNode == null) return emptyList()

        val screenData = mutableListOf<ScreenData>()
        val visitedNodes = mutableSetOf<AccessibilityNodeInfo>()

        try {
            parseNodeRecursively(rootNode, screenData, visitedNodes, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing accessibility tree", e)
        } finally {
            // Clean up visited nodes to prevent memory leaks
            visitedNodes.forEach { node ->
                try {
                    if (node != rootNode) {
                        node.recycle()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error recycling node", e)
                }
            }
        }

        return screenData
    }

    private fun parseNodeRecursively(
        node: AccessibilityNodeInfo,
        screenData: MutableList<ScreenData>,
        visitedNodes: MutableSet<AccessibilityNodeInfo>,
        depth: Int
    ) {
        if (depth > 50 || visitedNodes.contains(node)) {
            return // Prevent infinite recursion and circular references
        }

        visitedNodes.add(node)

        try {
            // Extract text content
            val nodeText = node.text?.toString()
            val contentDesc = node.contentDescription?.toString()
            
            // Combine both text sources
            val textsToProcess = mutableListOf<String>()
            if (!nodeText.isNullOrBlank()) textsToProcess.add(nodeText)
            if (!contentDesc.isNullOrBlank() && contentDesc != nodeText) {
                textsToProcess.add(contentDesc)
            }
            
            // Process each text content
            for (text in textsToProcess) {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)

                // Filter out noise
                val isNoise = NOISE_PATTERNS.any { pattern ->
                    pattern.matches(text)
                }

                // Don't filter out small numbers that could be rewards
                val isSmallNumber = text.matches(Regex("^\\d{1,6}$"))
                val numberValue = text.toIntOrNull()
                val isPotentialReward = isSmallNumber && numberValue != null && numberValue in 1..999999
                
                if (isNoise && !isPotentialReward) {
                    Log.v(TAG, "Filtering noise: '$text'")
                    continue // Skip this text but continue processing other texts/children
                }
                
                if (isPotentialReward) {
                    Log.d(TAG, "Found potential reward number: $text")
                }

                screenData.add(
                    ScreenData(
                        text = text,
                        bounds = bounds,
                        timestamp = System.currentTimeMillis(),
                        depth = depth
                    )
                )
            }

            // Process child nodes
            val childCount = node.childCount
            for (i in 0 until childCount) {
                try {
                    val child = node.getChild(i)
                    if (child != null) {
                        parseNodeRecursively(child, screenData, visitedNodes, depth + 1)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error processing child node at index $i", e)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error processing node at depth $depth", e)
        }
    }

    private fun updateOverlay() {
        serviceScope.launch {
            try {
                val settings = settingsRepository.getSettings()
                if (settings.showSessionOverlay) {
                    if (currentDungeonVisit != null || currentWayvesselSession != null) {
                        Log.d(
                            TAG,
                            "Updating session overlay - session: ${currentWayvesselSession?.name} (orns: ${currentWayvesselSession?.orns}, gold: ${currentWayvesselSession?.gold}), dungeon: ${currentDungeonVisit?.name}"
                        )
                        overlayManager.showSessionOverlay(
                            currentWayvesselSession,
                            currentDungeonVisit
                        )
                    } else {
                        Log.d(TAG, "Hiding session overlay - no active session or dungeon")
                        overlayManager.hideSessionOverlay()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update overlay", e)
            }
        }
    }

    private suspend fun updateWayvesselSessionInDatabase() {
        currentWayvesselSession?.let { session ->
            Log.d(TAG, "Updating wayvessel session in database:")
            Log.d(TAG, "  - Name: ${session.name}")
            Log.d(TAG, "  - Orns: ${session.orns}")
            Log.d(TAG, "  - Gold: ${session.gold}")
            Log.d(TAG, "  - Experience: ${session.experience}")
            Log.d(TAG, "  - Dungeons: ${session.dungeonsVisited}")
            try {
                wayvesselRepository.updateSession(session)
                Log.d(TAG, "Wayvessel session updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update wayvessel session", e)
            }
        }
    }

    private fun updateDungeonInDatabase() {
        currentDungeonVisit?.let { visit ->
            if (visit.id > 0) {
                Log.d(TAG, "=== UPDATING DUNGEON IN DATABASE ===")
                Log.d(TAG, "  - ID: ${visit.id}")
                Log.d(TAG, "  - Name: ${visit.name}")
                Log.d(TAG, "  - Floor: ${visit.floor}")
                Log.d(
                    TAG,
                    "  - Orns: ${visit.orns} (battle: ${visit.battleOrns}, floor: ${visit.floorOrns})"
                )
                Log.d(
                    TAG,
                    "  - Gold: ${visit.gold} (battle: ${visit.battleGold}, floor: ${visit.floorGold})"
                )
                Log.d(
                    TAG,
                    "  - Exp: ${visit.experience} (battle: ${visit.battleExperience}, floor: ${visit.floorExperience})"
                )
                Log.d(TAG, "  - Floor rewards: ${visit.floorRewards}")
                serviceScope.launch {
                    try {
                        dungeonRepository.updateVisit(visit)
                        Log.d(TAG, "Database update completed for visit ID: ${visit.id}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to update dungeon in database", e)
                    }
                }
            }
        }
    }

    private suspend fun handleDungeonStateChange(newState: DungeonState, data: List<ScreenData>) {
        // Create mutable copy of state for modifications
        var updatedState = newState

        Log.d(TAG, "=== DUNGEON STATE CHANGE START ===")
        Log.d(TAG, "Current visit: ${currentDungeonVisit?.name} (ID: ${currentDungeonVisit?.id})")
        Log.d(TAG, "New state dungeon: ${updatedState.dungeonName}")
        Log.d(TAG, "Is entering new: ${updatedState.isEnteringNewDungeon}")

        // CRITICAL FIX: If we already have a visit for this dungeon, don't create a new one
        if (currentDungeonVisit != null &&
            currentDungeonVisit?.name == updatedState.dungeonName &&
            updatedState.dungeonName.isNotEmpty() &&
            updatedState.dungeonName != "Unknown Dungeon"
        ) {

            Log.d(TAG, "DUPLICATE PREVENTION: Already tracking ${updatedState.dungeonName}")

            // Update existing visit instead of creating new one
            var visitUpdated = false

            // Update floor if higher
            if (updatedState.floorNumber > (currentDungeonVisit?.floor ?: 0).toInt()) {
                currentDungeonVisit =
                    currentDungeonVisit?.copy(floor = updatedState.floorNumber.toLong())
                visitUpdated = true
                Log.d(TAG, "Updated floor to ${updatedState.floorNumber}")
            }

            // Handle completion/defeat
            if (updatedState.isDone && currentDungeonVisit?.completed != true) {
                val isComplete = data.any { it.text.contains("COMPLETE", ignoreCase = true) }
                currentDungeonVisit = currentDungeonVisit?.copy(
                    completed = isComplete,
                    durationSeconds = java.time.temporal.ChronoUnit.SECONDS.between(
                        currentDungeonVisit!!.startTime,
                        LocalDateTime.now()
                    )
                )
                visitUpdated = true
                Log.d(TAG, "Marked dungeon as ${if (isComplete) "completed" else "failed"}")
            }

            if (visitUpdated) {
                updateDungeonInDatabase()
                updateOverlay()
            }

            // Don't process further - we're updating existing visit
            currentDungeonState = updatedState
            return
        }

        // DEDUPLICATION CHECK: Don't create duplicate dungeons within 5 seconds
        val currentTime = System.currentTimeMillis()
        val timeSinceLastCreation = currentTime - lastDungeonCreationTime

        if (timeSinceLastCreation < 5000 &&
            currentDungeonVisit?.name == updatedState.dungeonName &&
            updatedState.dungeonName.isNotEmpty()
        ) {
            Log.d(
                TAG,
                "Duplicate dungeon creation prevented (too soon) for: ${updatedState.dungeonName}"
            )
            currentDungeonState = updatedState
            return
        }

        // ADDITIONAL CHECK: Don't create if we recently created this dungeon
        val recentCreationTime = recentlyCreatedDungeons[updatedState.dungeonName] ?: 0L
        if (currentTime - recentCreationTime < 10000) { // 10 seconds window
            Log.d(TAG, "Duplicate prevented: ${updatedState.dungeonName} was created ${currentTime - recentCreationTime}ms ago")
            currentDungeonState = updatedState
            return
        }

        Log.d(
            TAG,
            "handleDungeonStateChange: newState=$newState, hasEntered=${newState.hasEntered}, currentState=$currentDungeonState"
        )

        // Extract dungeon name from completion screen if we don't have it
        if (updatedState.dungeonName == "Unknown Dungeon" || updatedState.dungeonName.isEmpty()) {
            // Try multiple strategies to find dungeon name
            if (data.any {
                    it.text.contains("DUNGEON COMPLETE!", ignoreCase = true) ||
                            it.text.contains("DEFEAT", ignoreCase = true) ||
                            (it.text.contains("Floor", ignoreCase = true) && it.text.contains("/"))
                }) {
                val betterName = dungeonScreenParser.extractDungeonNameFromData(data)
                if (betterName != null && betterName != "Unknown Dungeon") {
                    updatedState = updatedState.copy(dungeonName = betterName)
                    Log.d(TAG, "Updated dungeon name to: $betterName")
                }
            }
        }

        // If we detect we're in a dungeon but don't have a name, try harder to find it
        if (updatedState.hasEntered && updatedState.dungeonName.isEmpty()) {
            // Look for any text that could be a dungeon name
            val possibleDungeonName = data.firstOrNull {
                it.text.endsWith(" Dungeon") ||
                        it.text.endsWith(" Gauntlet") ||
                        it.text.contains("Valley of the Gods") ||
                        it.text.contains("Underworld")
            }?.text

            if (possibleDungeonName != null) {
                updatedState = updatedState.copy(dungeonName = possibleDungeonName)
                Log.d(TAG, "Found dungeon name mid-run: $possibleDungeonName")
            } else {
                // Don't create "Unknown Dungeon" entries
                Log.w(TAG, "In dungeon but couldn't determine name, not creating visit yet")
                return
            }
        }

        // Handle entering new dungeon - only if it's truly a different dungeon
        val isDifferentDungeon = updatedState.dungeonName != currentDungeonState?.dungeonName &&
            updatedState.dungeonName.isNotEmpty() &&
            updatedState.dungeonName != "Unknown Dungeon"
            
        // Check if this is actually a dungeon selection screen (not mid-dungeon)
        val isDungeonSelectionScreen = data.any {
            it.text.contains("world dungeon", ignoreCase = true) ||
            it.text.contains("special dungeon", ignoreCase = true) ||
            it.text.contains("hold to enter", ignoreCase = true)
        }
        
        if (updatedState.dungeonName != currentDungeonState?.dungeonName &&
            updatedState.dungeonName.isNotEmpty() &&
            updatedState.dungeonName != "Unknown Dungeon" &&
            (isDungeonSelectionScreen || isDifferentDungeon)) {

            Log.d(TAG, "New dungeon detected: ${updatedState.dungeonName}")

            // Put current dungeon on hold if it exists and isn't done
            if (currentDungeonState?.dungeonName?.isNotEmpty() == true &&
                currentDungeonState?.isDone != true &&
                currentDungeonVisit != null
            ) {
                Log.d(TAG, "Putting ${currentDungeonState!!.dungeonName} on hold")
                onHoldVisits[currentDungeonState!!.dungeonName] = currentDungeonVisit!!
                updateDungeonInDatabase() // Save current progress
            }
            // Reset for new dungeon
            currentDungeonVisit = null
        }

        // Handle dungeon entry - ONLY if we don't already have a visit
        if (updatedState.hasEntered &&
            updatedState.dungeonName.isNotEmpty() &&
            updatedState.dungeonName != "Unknown Dungeon" &&
            currentDungeonVisit == null
        ) { // THIS IS THE KEY CHECK

            Log.d(TAG, "Dungeon entered: ${updatedState.dungeonName}")

            // Check if we have this dungeon on hold
            val onHoldVisit = onHoldVisits.remove(updatedState.dungeonName)
            if (onHoldVisit != null) {
                currentDungeonVisit = onHoldVisit
                Log.d(TAG, "Resuming dungeon from hold: ${updatedState.dungeonName}")
            } else {
                // Create new visit only if we don't have one for this dungeon
                currentDungeonVisit = DungeonVisit(
                    name = updatedState.dungeonName,
                    mode = updatedState.mode,
                    sessionId = currentWayvesselSession?.id,
                    startTime = LocalDateTime.now()
                )
                Log.d(
                    TAG,
                    "Created new dungeon visit: ${updatedState.dungeonName}, mode: ${updatedState.mode}"
                )

                // Track creation time for deduplication
                lastDungeonCreationTime = System.currentTimeMillis()
                recentlyCreatedDungeons[updatedState.dungeonName] = System.currentTimeMillis()

                // Save the initial visit to database
                currentDungeonVisit?.let { visit ->
                    serviceScope.launch {
                        try {
                            val id = dungeonRepository.insertVisit(visit)
                            currentDungeonVisit = visit.copy(id = id)
                            Log.d(TAG, "Saved initial dungeon visit with id: $id")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to save dungeon visit", e)
                        }

                        // 'data' is the parameter of handleDungeonStateChange, correctly captured.
                        data.forEach { item ->
                            if (item.text.contains("VICTORY", ignoreCase = true) ||
                                item.text.contains("orns", ignoreCase = true) ||
                                item.text.contains("gold", ignoreCase = true) ||
                                item.text.contains("experience", ignoreCase = true)
                            ) {
                                Log.d(TAG, "LOOT INDICATOR (in new visit scope): '${item.text}'")
                            }
                        }
                    }
                }
            }

            // Update overlay if enabled
            updateOverlay()
        }

        // Handle floor change
        if (updatedState.hasEntered && currentDungeonVisit != null) {
            val currentFloor = currentDungeonVisit?.floor ?: 0
            val newFloor = updatedState.floorNumber.toLong()

            if (newFloor > currentFloor) {
                currentDungeonVisit = currentDungeonVisit?.copy(floor = newFloor)
                Log.d(TAG, "Floor updated to: $newFloor (was: $currentFloor)")
                updateDungeonInDatabase()
            }
        }

        // Handle godforge
        if (data.any { it.text.lowercase().contains("godforged") } && updatedState.hasEntered) {
            currentDungeonVisit = currentDungeonVisit?.copy(
                godforges = (currentDungeonVisit?.godforges ?: 0) + 1
            )
            Log.d(TAG, "Godforge detected!")
            updateDungeonInDatabase()
        }

        // Handle loot on victory/complete
        if ((data.any { item -> item.text.lowercase().contains("victory") } || // Changed it to item
                    data.any { item ->
                        item.text.lowercase().contains("complete")
                    }) && // Changed it to item
            !updatedState.victoryScreenHandledForFloor && updatedState.hasEntered
        ) {
            // Check if this is a floor completion (has "Floor" text visible)
            val isFloorCompletion = data.any { item -> // Changed it to item
                item.text.lowercase().contains("floor") &&
                        item.text.contains("/")
            }

            val isDungeonCompleteScreen = data.any { item -> // Changed it to item
                item.text.equals("DUNGEON COMPLETE!", ignoreCase = true)
            }

            if (isFloorCompletion || isDungeonCompleteScreen) {
                Log.d(
                    TAG,
                    "Completion detected (floor: $isFloorCompletion, dungeon: $isDungeonCompleteScreen), parsing loot..."
                )
                val loot = dungeonScreenParser.parseLoot(data)
                Log.d(TAG, "Parsed floor loot: $loot")

                // If no loot found, mark that we're waiting for rewards
                if (loot.isEmpty()) {
                    recentVictoryTime = System.currentTimeMillis()
                    awaitingRewards = true
                    Log.d(TAG, "No loot found, marking as awaiting rewards")
                } else {
                    // Pass the specific floor number and the screen data
                    processLootRewards(loot, updatedState.floorNumber, data)
                    updatedState =
                        updatedState.copy(victoryScreenHandledForFloor = true) // Update state after processing
                }
            }
        }

        // Update the state at the end
        currentDungeonState = updatedState
    }

    // Pass 'screenNodeData' (which is the original 'data' from onAccessibilityEvent)
    private fun processLootRewards(
        loot: Map<String, Int>,
        floorNumber: Int,
        screenNodeData: List<ScreenData>
    ) {
        val ornsToAdd = loot["orns"] ?: 0
        val goldToAdd = loot["gold"] ?: 0
        val expToAdd = loot["experience"] ?: 0

        if (ornsToAdd > 0 || goldToAdd > 0 || expToAdd > 0) { // Process if actual loot is found
            Log.d(TAG, "Processing loot: orns=$ornsToAdd, gold=$goldToAdd, exp=$expToAdd")
            currentDungeonVisit?.let { visit ->
                // Create floor reward entry
                val floorReward = FloorReward(
                    floor = floorNumber,
                    orns = ornsToAdd.toLong(),
                    gold = goldToAdd.toLong(),
                    experience = expToAdd.toLong()
                )

                Log.d(TAG, "=== FLOOR LOOT TRACKING ===")
                Log.d(TAG, "Floor ${floorNumber} loot:")
                Log.d(TAG, "  - Orns to add: $ornsToAdd")
                Log.d(TAG, "  - Gold to add: $goldToAdd")
                Log.d(TAG, "  - Exp to add: $expToAdd")
                Log.d(TAG, "Current visit before update:")
                Log.d(TAG, "  - Total orns: ${currentDungeonVisit?.orns}")
                Log.d(TAG, "  - Total gold: ${currentDungeonVisit?.gold}")
                Log.d(TAG, "  - Total exp: ${currentDungeonVisit?.experience}")

                // Add to floor rewards list
                val updatedFloorRewards =
                    currentDungeonVisit?.floorRewards?.toMutableList() ?: mutableListOf()

                // Check if we already have rewards for this floor (update if so)
                val existingIndex = updatedFloorRewards.indexOfFirst { it.floor == floorNumber }
                if (existingIndex >= 0) {
                    updatedFloorRewards[existingIndex] = floorReward
                } else {
                    updatedFloorRewards.add(floorReward)
                }

                currentDungeonVisit = currentDungeonVisit?.copy(
                    floorOrns = (currentDungeonVisit?.floorOrns ?: 0) + ornsToAdd,
                    floorGold = (currentDungeonVisit?.floorGold ?: 0) + goldToAdd,
                    floorExperience = (currentDungeonVisit?.floorExperience ?: 0) + expToAdd,
                    // Update totals
                    orns = (currentDungeonVisit?.orns ?: 0) + ornsToAdd,
                    gold = (currentDungeonVisit?.gold ?: 0) + goldToAdd,
                    experience = (currentDungeonVisit?.experience ?: 0) + expToAdd,
                    floorRewards = updatedFloorRewards
                )

                Log.d(TAG, "Current visit after update:")
                Log.d(TAG, "  - Total orns: ${currentDungeonVisit?.orns}")
                Log.d(TAG, "  - Total gold: ${currentDungeonVisit?.gold}")
                Log.d(TAG, "  - Total exp: ${currentDungeonVisit?.experience}")
                Log.d(TAG, "  - Floor rewards count: ${currentDungeonVisit?.floorRewards?.size}")
                Log.d(TAG, "=== END FLOOR LOOT TRACKING ===")

                Log.d(
                    TAG,
                    "Updated floor loot - orns: +$ornsToAdd (total: ${currentDungeonVisit?.orns}), " +
                            "gold: +$goldToAdd (total: ${currentDungeonVisit?.gold}), " +
                            "exp: +$expToAdd (total: ${currentDungeonVisit?.experience})"
                )
                Log.d(TAG, "Floor rewards: ${currentDungeonVisit?.floorRewards}")

                updateDungeonInDatabase()

                // Update wayvessel session if active
                currentWayvesselSession?.let { session ->
                    val updatedSession = session.copy(
                        orns = session.orns + ornsToAdd,
                        gold = session.gold + goldToAdd,
                        experience = session.experience + expToAdd
                    )
                    currentWayvesselSession = updatedSession
                    serviceScope.launch {
                        updateWayvesselSessionInDatabase()
                    }
                    // Update overlay immediately
                    updateOverlay()
                }
            }

            // Mark that we've handled victory screen for this floor in our mutable state
            updateOverlay()
            
            // Always update wayvessel session after processing loot
            serviceScope.launch {
                updateWayvesselSessionInDatabase()
            }
        }

        // Handle dungeon completion
        if (currentDungeonVisit != null && 
            (screenNodeData.any { item ->
                item.text.equals(
                    "DUNGEON COMPLETE!",
                    ignoreCase = true
                )
            } || screenNodeData.any { item ->
                item.text.equals(
                    "DEFEAT",
                    ignoreCase = true
                )
            })
        ) {
            // Only mark as complete if we see "COMPLETE", not "DEFEAT"
            val isComplete = screenNodeData.any { item -> item.text.lowercase().contains("complete") }
            Log.d(TAG, "Dungeon ${if (isComplete) "completed" else "failed"}")

            // Parse final dungeon rewards if it's a completion
            if (isComplete) {
                Log.d(TAG, "Parsing dungeon completion rewards...")
                val dungeonLoot = dungeonScreenParser.parseLoot(screenNodeData)

                if (dungeonLoot.isNotEmpty()) { // Process if actual loot is found
                    currentDungeonVisit = currentDungeonVisit?.copy(
                        floorOrns = (currentDungeonVisit?.floorOrns ?: 0) + (dungeonLoot["orns"]
                            ?: 0),
                        floorGold = (currentDungeonVisit?.floorGold ?: 0) + (dungeonLoot["gold"]
                            ?: 0),
                        floorExperience = (currentDungeonVisit?.floorExperience
                            ?: 0) + (dungeonLoot["experience"] ?: 0),
                        // Update totals
                        orns = (currentDungeonVisit?.orns ?: 0) + (dungeonLoot["orns"] ?: 0),
                        gold = (currentDungeonVisit?.gold ?: 0) + (dungeonLoot["gold"] ?: 0),
                        experience = (currentDungeonVisit?.experience
                            ?: 0) + (dungeonLoot["experience"] ?: 0)
                    )

                    Log.d(
                        TAG,
                        "Added dungeon completion rewards - orns: ${dungeonLoot["orns"]}, gold: ${dungeonLoot["gold"]}, exp: ${dungeonLoot["experience"]}"
                    )

                    // Update wayvessel session if active
                    currentWayvesselSession?.let { session ->
                        val updatedSession = session.copy(
                            orns = session.orns + (dungeonLoot["orns"] ?: 0),
                            gold = session.gold + (dungeonLoot["gold"] ?: 0),
                            experience = session.experience + (dungeonLoot["experience"] ?: 0)
                        )
                        currentWayvesselSession = updatedSession
                        serviceScope.launch { // Ensure suspend call is in a coroutine
                            updateWayvesselSessionInDatabase()
                        }
                        // Update overlay to show new totals
                        updateOverlay()
                    }
                }
            }

            currentDungeonVisit?.let { visit ->
                // Calculate final duration
                val duration = java.time.temporal.ChronoUnit.SECONDS.between(
                    visit.startTime,
                    LocalDateTime.now()
                )

                Log.d(TAG, "=== DUNGEON COMPLETION ===")
                Log.d(TAG, "Final totals for ${visit.name}:")
                Log.d(TAG, "  - Orns: ${visit.orns}")
                Log.d(TAG, "  - Gold: ${visit.gold}")
                Log.d(TAG, "  - Experience: ${visit.experience}")
                Log.d(TAG, "  - Floor rewards: ${visit.floorRewards}")
                Log.d(TAG, "  - Duration: $duration seconds")

                val completedVisit = visit.copy(
                    completed = isComplete,
                    durationSeconds = duration
                )

                Log.d(TAG, "Dungeon completed: $completedVisit")

                // Clear from on-hold visits if it was there
                onHoldVisits.remove(visit.name)

                serviceScope.launch {
                    try {
                        dungeonRepository.updateVisit(completedVisit)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to update completed dungeon", e)
                    }
                    updateOverlay()
                }

                // Update dungeon count for wayvessel session
                currentWayvesselSession?.let { session ->
                    val updatedSession = session.copy(dungeonsVisited = session.dungeonsVisited + 1)
                    currentWayvesselSession = updatedSession
                    serviceScope.launch { // Ensure suspend call is in a coroutine
                        updateWayvesselSessionInDatabase()
                    }
                }

                // Only clear current visit if the dungeon is actually done
                if (isComplete || visit.floor > 0) {
                    // Clear current visit
                    currentDungeonVisit = null
                    lastDungeonCreationTime = 0L // Reset creation time
                    
                    // Clear the dungeon state tracker
                    dungeonStateTracker.clear()
                    
                    // Reset current dungeon state
                    currentDungeonState = null
                } else {
                    Log.d(TAG, "Not clearing current visit - dungeon might not be done yet")
                }

                // Clean up old entries from recentlyCreatedDungeons (older than 30 seconds)
                val cutoffTime = System.currentTimeMillis() - 30000
                recentlyCreatedDungeons.entries.removeIf { it.value < cutoffTime }

            }

            updateOverlay()
        }
    }

    private suspend fun handleWayvesselStart(wayvesselName: String) {
        Log.d(TAG, "Starting wayvessel session: $wayvesselName")

        // End any existing session
        currentWayvesselSession?.let { session ->
            val endTime = LocalDateTime.now()
            val duration = java.time.temporal.ChronoUnit.SECONDS.between(session.startTime, endTime)
            val completedSession = session.copy(durationSeconds = duration)
            wayvesselRepository.updateSession(completedSession)
        }
        
        // Clear the current wayvessel session
        val oldSession = currentWayvesselSession
        currentWayvesselSession = null

        // Create new session
        val session = WayvesselSession(
            name = wayvesselName,
            startTime = LocalDateTime.now()
        )
        val id = wayvesselRepository.insertSession(session)
        currentWayvesselSession = session.copy(id = id)

        Log.d(TAG, "Wayvessel session started: $wayvesselName (ID: $id)")
        if (oldSession != null) {
            Log.d(TAG, "Previous session ended: ${oldSession.name} - orns: ${oldSession.orns}, gold: ${oldSession.gold}")
        }
        
        updateOverlay()
    }

    private suspend fun endCurrentWayvesselSession() {
        currentWayvesselSession?.let { session ->
            Log.d(TAG, "Ending wayvessel session: ${session.name}")
            val endTime = LocalDateTime.now()
            val duration = java.time.temporal.ChronoUnit.SECONDS.between(session.startTime, endTime)
            val completedSession = session.copy(durationSeconds = duration)
            
            Log.d(TAG, "Session ended - Total orns: ${completedSession.orns}, gold: ${completedSession.gold}, dungeons: ${completedSession.dungeonsVisited}")
            
            wayvesselRepository.updateSession(completedSession)
            currentWayvesselSession = null
            
            // Clear overlay
            updateOverlay()
        }
    }

    private fun determineScreenType(screenData: List<ScreenData>): ScreenType {
        val texts = screenData.map { it.text.lowercase() }

        return when {
            texts.any { it.contains("acquired") } -> ScreenType.ITEM_DETAIL
            texts.any { it.contains("new") && texts.any { it.contains("inventory") } } -> ScreenType.INVENTORY
            texts.any { it.contains("notifications") } -> ScreenType.NOTIFICATIONS
            texts.any { it.contains("this wayvessel is active") } -> ScreenType.WAYVESSEL
            texts.any { it.contains("special dungeon") || it.contains("world dungeon") } -> ScreenType.DUNGEON_ENTRY
            texts.any { it.contains("battle a series of opponents") } -> ScreenType.DUNGEON_ENTRY
            texts.any { it.contains("codex") && it.contains("skill") } -> ScreenType.BATTLE
            else -> ScreenType.UNKNOWN
        }
    }
}
