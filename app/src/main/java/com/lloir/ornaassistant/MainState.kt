package com.lloir.ornaassistant

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.preference.PreferenceManager
import com.lloir.ornaassistant.db.DungeonVisitDatabaseHelper
import com.lloir.ornaassistant.ornaviews.OrnaViewDungeonEntry
import com.lloir.ornaassistant.overlays.AssessOverlay
import com.lloir.ornaassistant.overlays.InviterOverlay
import com.lloir.ornaassistant.overlays.SessionOverlay
import com.lloir.ornaassistant.assess.AssessResult
import java.lang.ref.WeakReference
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class MainState(
    private val windowManager: WindowManager,
    private val context: Context,
    notificationView: View,
    sessionView: View,
    assessView: View,
    kingdomGauntletView: View,
    private val accessibilityService: AccessibilityService? = null
) {
    companion object {
        private const val TAG = "OrnaMainState"
        private const val MAX_QUEUE_SIZE = 50
        private const val CORE_POOL_SIZE = 1
        private const val MAX_POOL_SIZE = 2
        private const val KEEP_ALIVE_TIME = 30L
    }

    // Thread-safe collections and atomics
    private val isShutdown = AtomicBoolean(false)
    private val isProcessingOrna = AtomicBoolean(false)
    private val isProcessingDiscord = AtomicBoolean(false)

    // Use concurrent collections for thread safety
    private val onholdVisits = ConcurrentHashMap<String, DungeonVisit>()

    // Weak references to prevent memory leaks
    private var currentViewRef: WeakReference<OrnaView>? = null
    private var dungeonVisitRef: WeakReference<DungeonVisit>? = null
    private var sessionRef: WeakReference<WayvesselSession>? = null
    private val battleRef = WeakReference(Battle(accessibilityService))

    // Database helper with lazy initialization
    private val dungeonDbHelper: DungeonVisitDatabaseHelper by lazy {
        DungeonVisitDatabaseHelper(context)
    }

    // Overlay management
    private val inviterOverlay = InviterOverlay(windowManager, context, notificationView, 0.8)
    private val sessionOverlay = SessionOverlay(windowManager, context, sessionView, 0.4)
    private val assessOverlay = AssessOverlay(windowManager, context, assessView, 0.7)

    // Thread pool for data processing
    private val executor = ThreadPoolExecutor(
        CORE_POOL_SIZE,
        MAX_POOL_SIZE,
        KEEP_ALIVE_TIME,
        TimeUnit.SECONDS,
        LinkedBlockingQueue(MAX_QUEUE_SIZE),
        { r -> Thread(r, "OrnaDataProcessor") }
    ).apply {
        allowCoreThreadTimeOut(true)
    }

    // Data queues with size limits
    private val ornaQueue = LinkedBlockingQueue<ArrayList<ScreenData>>(MAX_QUEUE_SIZE)
    private val discordQueue = LinkedBlockingQueue<ArrayList<ScreenData>>(MAX_QUEUE_SIZE)

    // Shared preferences with listener
    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    // State tracking
    var kingdomGauntletNextUpdate: LocalDateTime = LocalDateTime.now()
        private set
    var lastBattleTime: LocalDateTime = LocalDateTime.now().minusDays(1)
        private set

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        handlePreferenceChange(key, prefs)
    }

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        startDataProcessingThreads()
        Log.d(TAG, "MainState initialized successfully")
    }

    private fun handlePreferenceChange(key: String?, prefs: SharedPreferences) {
        when (key) {
            "invites", "inviter_overlay" -> {
                if (!prefs.getBoolean(key, true)) inviterOverlay.hide()
            }
            "session", "session_overlay" -> {
                if (!prefs.getBoolean(key, true)) sessionOverlay.hide()
            }
            "assess", "assess_overlay" -> {
                if (!prefs.getBoolean(key, true)) assessOverlay.hide()
            }
        }
    }

    private fun startDataProcessingThreads() {
        // Orna data processing thread
        thread(name = "OrnaDataProcessor", isDaemon = true) {
            while (!isShutdown.get()) {
                try {
                    val data = ornaQueue.poll(1, TimeUnit.SECONDS)
                    if (data != null && !isShutdown.get()) {
                        if (isProcessingOrna.compareAndSet(false, true)) {
                            try {
                                handleOrnaData(data)
                            } finally {
                                isProcessingOrna.set(false)
                            }
                        }
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing Orna data", e)
                }
            }
        }

        // Discord data processing thread (if needed in future)
        thread(name = "DiscordDataProcessor", isDaemon = true) {
            while (!isShutdown.get()) {
                try {
                    val data = discordQueue.poll(1, TimeUnit.SECONDS)
                    if (data != null && !isShutdown.get()) {
                        if (isProcessingDiscord.compareAndSet(false, true)) {
                            try {
                                handleDiscordData(data)
                            } finally {
                                isProcessingDiscord.set(false)
                            }
                        }
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing Discord data", e)
                }
            }
        }
    }

    fun cleanup() {
        if (!isShutdown.compareAndSet(false, true)) {
            return // Already cleaned up
        }

        try {
            Log.d(TAG, "Starting cleanup...")

            // Finish current session
            sessionRef?.get()?.finish()
            sessionRef = null

            // Close current view
            currentViewRef?.get()?.close()
            currentViewRef = null

            // Clear dungeon visit
            dungeonVisitRef = null

            // Hide overlays
            inviterOverlay.hide()
            sessionOverlay.hide()
            assessOverlay.hide()

            // Unregister preference listener
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)

            // Close database
            try {
                dungeonDbHelper.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing database", e)
            }

            // Shutdown executor
            executor.shutdown()
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }

            // Clear queues
            ornaQueue.clear()
            discordQueue.clear()

            Log.d(TAG, "Cleanup completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    private fun handleOrnaData(data: ArrayList<ScreenData>) {
        if (isShutdown.get()) return

        try {
            // Check for battle state
            if (Battle.inBattle(data)) {
                lastBattleTime = LocalDateTime.now()
            }

            updateView(data)
            processWayvessel(data)
            processSessionEnd(data)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling Orna data", e)
        }
    }

    private fun handleDiscordData(data: ArrayList<ScreenData>) {
        // Future implementation for Discord data processing
        Log.d(TAG, "Discord data processing not yet implemented")
    }

    private fun processWayvessel(data: ArrayList<ScreenData>) {
        val wayvessel = data.find {
            it.name.lowercase().contains("'s wayvessel") && it.rect.left > 70
        } ?: return

        if (data.none { it.name.lowercase().contains("this wayvessel is active") }) {
            val name = wayvessel.name.replace("'s Wayvessel", "")

            try {
                val currentSession = sessionRef?.get()
                if (currentSession == null || currentSession.name != name) {
                    currentSession?.finish()
                    sessionOverlay.hide()

                    val newSession = WayvesselSession(name, context)
                    sessionRef = WeakReference(newSession)

                    dungeonVisitRef?.get()?.sessionID = newSession.mID

                    if (isOverlayEnabled("session")) {
                        sessionOverlay.update(newSession, dungeonVisitRef?.get())
                    }
                    Log.i(TAG, "At wayvessel: $name")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing wayvessel", e)
            }
        }
    }

    private fun processSessionEnd(data: ArrayList<ScreenData>) {
        val leaveParty = data.find {
            it.name.lowercase().contains("are you sure you would like to leave this party?") ||
                    it.name.lowercase().contains("are you sure you would return home")
        }

        if (leaveParty != null) {
            val session = sessionRef?.get()
            Log.d(TAG, "Finished session $session")
            session?.finish()
            sessionOverlay.hide()
            sessionRef = null
        }
    }

    fun processData(packageName: String, data: ArrayList<ScreenData>) {
        if (isShutdown.get()) return

        try {
            when {
                packageName.contains("orna") -> {
                    if (!ornaQueue.offer(data)) {
                        Log.w(TAG, "Orna queue full, dropping data")
                        // Remove oldest item and try again
                        ornaQueue.poll()
                        ornaQueue.offer(data)
                    }
                }
                packageName.contains("discord") -> {
                    if (!discordQueue.offer(data)) {
                        Log.w(TAG, "Discord queue full, dropping data")
                        discordQueue.poll()
                        discordQueue.offer(data)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing data for package: $packageName", e)
        }
    }

    private fun updateView(data: ArrayList<ScreenData>) {
        try {
            val currentView = currentViewRef?.get()
            val isDone = currentView?.update(data, ::processUpdate) == true

            if (isDone) {
                currentView.close()
                currentViewRef = null
            }

            val newType = OrnaViewFactory.getType(data)

            if (currentView == null || (newType != null && newType != currentView.type)) {
                val view = OrnaViewFactory.create(newType, data, windowManager, context)
                var shouldUpdate = true

                if (currentView != null) {
                    if (newType == OrnaViewType.ITEM && !isOverlayEnabled("assess")) {
                        shouldUpdate = false
                    }
                    // Hide assess overlay when leaving ITEM view
                    if (currentView.type == OrnaViewType.ITEM && newType != OrnaViewType.ITEM) {
                        Log.d(TAG, "Leaving item view - hiding assess overlay")
                        assessOverlay.hide()
                    }
                }

                if (view != null) {
                    currentView?.close()
                    currentViewRef = WeakReference(view)

                    if (shouldUpdate) {
                        view.update(data, ::processUpdate)
                    }

                    Log.d(TAG, "VIEW CHANGED TO ${view.type}")

                    if (view.type != OrnaViewType.NOTIFICATIONS) {
                        inviterOverlay.hide()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating view", e)
        }
    }

    private fun isOverlayEnabled(overlayType: String): Boolean {
        return sharedPreferences.getBoolean(overlayType, true) ||
                sharedPreferences.getBoolean("${overlayType}_overlay", true)
    }

    fun processUpdate(update: MutableMap<OrnaViewUpdateType, Any?>) {
        if (isShutdown.get()) return

        try {
            var dungeonDone = false
            var dungeonFailed = false

            update.forEach { (type, data) ->
                when (type) {
                    OrnaViewUpdateType.DUNGEON_ENTERED -> handleDungeonEntered()
                    OrnaViewUpdateType.DUNGEON_NEW_DUNGEON -> handleNewDungeon()
                    OrnaViewUpdateType.DUNGEON_MODE_CHANGED -> handleModeChanged()
                    OrnaViewUpdateType.DUNGEON_GODFORGE -> handleGodforge()
                    OrnaViewUpdateType.DUNGEON_DONE -> {
                        dungeonDone = true
                        if (sessionRef?.get() == null) sessionOverlay.hide()
                    }
                    OrnaViewUpdateType.DUNGEON_FAIL -> {
                        dungeonDone = true
                        dungeonFailed = true
                        if (sessionRef?.get() == null) sessionOverlay.hide()
                    }
                    OrnaViewUpdateType.DUNGEON_NEW_FLOOR -> handleNewFloor(data as? Int)
                    OrnaViewUpdateType.DUNGEON_EXPERIENCE -> handleExperience(data as? Int)
                    OrnaViewUpdateType.DUNGEON_ORNS -> handleOrns(data as? Int)
                    OrnaViewUpdateType.DUNGEON_GOLD -> handleGold(data as? Int)
                    OrnaViewUpdateType.NOTIFICATIONS_INVITERS -> handleInviters(data)
                    OrnaViewUpdateType.ITEM_ASSESS_RESULTS -> handleAssessResults(data)
                    OrnaViewUpdateType.KINGDOM_GAUNTLET_LIST -> {
                        // TODO: Implement kingdom gauntlet handling
                    }
                }
            }

            if (dungeonDone) {
                handleDungeonCompletion(dungeonFailed)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing update", e)
        }
    }

    private fun handleDungeonEntered() {
        val currentView = currentViewRef?.get() as? OrnaViewDungeonEntry ?: return
        var dungeonVisit = dungeonVisitRef?.get()

        if (dungeonVisit == null) {
            if (onholdVisits.containsKey(currentView.mDungeonName)) {
                Log.i(TAG, "Reloading on hold visit to ${currentView.mDungeonName}")
                dungeonVisit = onholdVisits.remove(currentView.mDungeonName)
            } else {
                val sessionID = sessionRef?.get()?.mID

                // Increment session dungeon count
                sessionRef?.get()?.let { session ->
                    session.mDungeonsVisited += 1
                    Log.d(TAG, "Session dungeons visited: ${session.mDungeonsVisited}")
                }

                dungeonVisit = DungeonVisit(sessionID, currentView.mDungeonName, currentView.mMode)
                Log.d(TAG, "Entered: $dungeonVisit")
            }
            dungeonVisitRef = WeakReference(dungeonVisit)
        }

        if (isOverlayEnabled("session")) {
            sessionOverlay.update(sessionRef?.get(), dungeonVisit)
        }
    }

    private fun handleNewDungeon() {
        dungeonVisitRef?.get()?.let { visit ->
            Log.i(TAG, "Putting on hold visit to ${visit.name}")
            onholdVisits[visit.name] = visit
            dungeonVisitRef = null
        }
    }

    private fun handleModeChanged() {
        val currentView = currentViewRef?.get() as? OrnaViewDungeonEntry ?: return
        dungeonVisitRef?.get()?.mode = currentView.mMode
    }

    private fun handleGodforge() {
        dungeonVisitRef?.get()?.let { visit ->
            visit.godforges += 1
            Log.d(TAG, "Godforge detected, total: ${visit.godforges}")
        }
    }

    private fun handleNewFloor(floor: Int?) {
        floor?.let { floorNumber ->
            dungeonVisitRef?.get()?.let { visit ->
                visit.floor = floorNumber.toLong()
                Log.d(TAG, "New floor: ${visit.floor}")
            }
        }
    }

    private fun handleExperience(exp: Int?) {
        exp?.let { experience ->
            // Update dungeon visit
            dungeonVisitRef?.get()?.let { visit ->
                visit.experience += experience
                Log.d(TAG, "Dungeon experience: +$experience, total: ${visit.experience}")
            }

            // Update session
            sessionRef?.get()?.let { session ->
                session.experience += experience
                Log.d(TAG, "Session experience: +$experience, total: ${session.experience}")
            }

            // Update overlay if enabled
            if (isOverlayEnabled("session")) {
                sessionOverlay.update(sessionRef?.get(), dungeonVisitRef?.get())
            }
        }
    }

    private fun handleOrns(orns: Int?) {
        orns?.let { ornValue ->
            // Update dungeon visit
            dungeonVisitRef?.get()?.let { visit ->
                visit.orns += ornValue
                Log.d(TAG, "Dungeon orns: +$ornValue, total: ${visit.orns}")
            }

            // Update session
            sessionRef?.get()?.let { session ->
                session.orns += ornValue
                Log.d(TAG, "Session orns: +$ornValue, total: ${session.orns}")
            }

            // Update overlay if enabled
            if (isOverlayEnabled("session")) {
                sessionOverlay.update(sessionRef?.get(), dungeonVisitRef?.get())
            }
        }
    }

    private fun handleGold(gold: Int?) {
        gold?.let { goldValue ->
            // Update dungeon visit
            dungeonVisitRef?.get()?.let { visit ->
                visit.gold += goldValue
                Log.d(TAG, "Dungeon gold: +$goldValue, total: ${visit.gold}")
            }

            // Update session
            sessionRef?.get()?.let { session ->
                session.gold += goldValue
                Log.d(TAG, "Session gold: +$goldValue, total: ${session.gold}")
            }

            // Update overlay if enabled
            if (isOverlayEnabled("session")) {
                sessionOverlay.update(sessionRef?.get(), dungeonVisitRef?.get())
            }
        }
    }

    private fun handleInviters(data: Any?) {
        if (isOverlayEnabled("invites") || isOverlayEnabled("inviter")) {
            @Suppress("UNCHECKED_CAST")
            val invitersData = data as? MutableMap<String, Rect>
            invitersData?.let { inviterOverlay.update(it) }
        }
    }

    private fun handleAssessResults(data: Any?) {
        if (isOverlayEnabled("assess")) {
            val assessData = data as? AssessResult
            if (assessData != null) {
                if (assessOverlay.shouldHideForNewItem(assessData.itemName)) {
                    Log.d(TAG, "Hiding overlay for different item")
                    assessOverlay.hide()
                }
                Log.d(TAG, "Showing assess overlay with data: $assessData")
                assessOverlay.update(assessData)
            } else {
                Log.e(TAG, "Error: ITEM_ASSESS_RESULTS data is not of type AssessResult: $data")
            }
        } else {
            Log.d(TAG, "Assess overlay is disabled")
        }
    }

    private fun handleDungeonCompletion(failed: Boolean) {
        dungeonVisitRef?.get()?.let { visit ->
            visit.completed = !failed
            visit.finish()

            try {
                dungeonDbHelper.insertData(visit)
                Log.i(TAG, "Stored: $visit")
            } catch (e: Exception) {
                Log.e(TAG, "Error storing dungeon visit", e)
            }
        }
        dungeonVisitRef = null
    }
}