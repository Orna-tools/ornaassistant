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
import com.lloir.ornaassistant.assess.AssessResult // Import AssessResult
// import org.json.JSONObject // No longer needed here if only used for AssessResult
import java.time.LocalDateTime
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class MainState(
    private val mWM: WindowManager,
    private val mCtx: Context,
    mNotificationView: View,
    mSessionView: View,
    mAssessView: View,
    mAS1: View,
    private val mAS: AccessibilityService? = null // Made optional for MediaProjection compatibility
) {
    companion object {
        private const val TAG = "OrnaMainState"
        private const val MAX_QUEUE_SIZE = 100
    }

    private val mDungeonDbHelper = DungeonVisitDatabaseHelper(mCtx)
    private var mCurrentView: OrnaView? = null
    private var mDungeonVisit: DungeonVisit? = null
    private var mOnholdVisits = mutableMapOf<String, DungeonVisit>()
    private var mSession: WayvesselSession? = null
    private var mBattle = Battle(mAS)

    private val isShutdown = AtomicBoolean(false)

    private val mInviterOverlay = InviterOverlay(mWM, mCtx, mNotificationView, 0.8)
    private val mSessionOverlay = SessionOverlay(mWM, mCtx, mSessionView, 0.4)
    private val mAssessOverlay = AssessOverlay(mWM, mCtx, mAssessView, 0.7)

    private val mOrnaQueue = LinkedBlockingDeque<ArrayList<ScreenData>>()
    private val mDiscordQueue = LinkedBlockingDeque<ArrayList<ScreenData>>()

    private val mSharedPreference: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(mCtx)

    var mKGNextUpdate: LocalDateTime = LocalDateTime.now()
    var mInBattle = LocalDateTime.now().minusDays(1)

    private val sharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            "invites", "inviter_overlay" -> if (!sharedPreferences.getBoolean(key, true)) mInviterOverlay.hide()
            "session", "session_overlay" -> if (!sharedPreferences.getBoolean(key, true)) mSessionOverlay.hide()
            "assess", "assess_overlay" -> if (!sharedPreferences.getBoolean(key, true)) mAssessOverlay.hide()
        }
    }

    init {
        mSharedPreference.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
        startDataProcessingThread()
    }

    private fun startDataProcessingThread() {
        thread(name = "OrnaDataProcessor") {
            while (!isShutdown.get()) {
                try {
                    val data: ArrayList<ScreenData>? = mOrnaQueue.take()
                    if (data != null && !isShutdown.get()) {
                        handleOrnaData(data)
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing Orna data", e)
                }
            }
        }
    }

    fun cleanup() {
        try {
            isShutdown.set(true)
            mSession?.finish()
            mSession = null
            mCurrentView?.close()
            mCurrentView = null
            mDungeonVisit = null
            mInviterOverlay.hide()
            mSessionOverlay.hide()
            mAssessOverlay.hide()
            mSharedPreference.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
            mDungeonDbHelper.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    private fun handleOrnaData(data: ArrayList<ScreenData>) {
        if (isShutdown.get()) return

        try {
            if (Battle.inBattle(data)) {
                mInBattle = LocalDateTime.now()
            }

            updateView(data)
            processWayvessel(data)
            processSessionEnd(data)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling Orna data", e)
        }
    }

    private fun processWayvessel(data: ArrayList<ScreenData>) {
        val wayvessel = data.filter { it.name.lowercase().contains("'s wayvessel") }.firstOrNull()
        if (wayvessel != null && wayvessel.rect.left > 70) {
            if (data.none { it.name.lowercase().contains("this wayvessel is active") }) {
                val name = wayvessel.name.replace("'s Wayvessel", "")
                try {
                    if (mSession == null || mSession!!.name != name) {
                        mSession?.finish()
                        mSessionOverlay.hide()

                        mSession = WayvesselSession(name, mCtx)
                        mDungeonVisit?.let { it.sessionID = mSession!!.mID }

                        if (isOverlayEnabled("session")) {
                            mSessionOverlay.update(mSession, mDungeonVisit)
                        }
                        Log.i(TAG, "At wayvessel: $name")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing wayvessel", e)
                }
            }
        }
    }

    private fun processSessionEnd(data: ArrayList<ScreenData>) {
        val leaveParty = data.firstOrNull {
            it.name.lowercase().contains("are you sure you would like to leave this party?") ||
                    it.name.lowercase().contains("are you sure you would return home")
        }

        if (leaveParty != null) {
            Log.d(TAG, "Finished session $mSession")
            mSession?.finish()
            mSessionOverlay.hide()
            mSession = null
        }
    }

    fun processData(packageName: String, data: ArrayList<ScreenData>) {
        if (isShutdown.get()) return

        try {
            when {
                packageName.contains("orna") -> {
                    if (mOrnaQueue.size < MAX_QUEUE_SIZE) {
                        mOrnaQueue.put(data)
                    } else {
                        Log.w(TAG, "Orna queue full, dropping data")
                    }
                }
                packageName.contains("discord") -> {
                    if (mDiscordQueue.size < MAX_QUEUE_SIZE) {
                        mDiscordQueue.put(data)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing data for package: $packageName", e)
        }
    }

    private fun updateView(data: ArrayList<ScreenData>) {
        try {
            val bDone = mCurrentView?.update(data, ::processUpdate)
            if (bDone == true) {
                mCurrentView?.close()
                mCurrentView = null
            }

            val newType = OrnaViewFactory.getType(data)

            if (mCurrentView == null || (newType != null && newType != mCurrentView!!.type)) {
                val view = OrnaViewFactory.create(newType, data, mWM, mCtx)
                var update = true

                if (mCurrentView != null) {
                    if (newType == OrnaViewType.ITEM && !isOverlayEnabled("assess")) {
                        update = false
                    }
                    // Hide assess overlay when leaving ITEM view
                    if (mCurrentView!!.type == OrnaViewType.ITEM && newType != OrnaViewType.ITEM) {
                        Log.d(TAG, "Leaving item view - hiding assess overlay")
                        mAssessOverlay.hide()
                    }
                }

                if (view != null) {
                    mCurrentView?.close()
                    mCurrentView = view
                    if (update) {
                        mCurrentView?.update(data, ::processUpdate)
                    }
                    Log.d(TAG, "VIEW CHANGED TO ${view.type}")
                    if (view.type != OrnaViewType.NOTIFICATIONS) {
                        mInviterOverlay.hide()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating view", e)
        }
    }

    private fun isOverlayEnabled(overlayType: String): Boolean {
        // Check both old and new preference keys for compatibility
        return mSharedPreference.getBoolean(overlayType, true) ||
                mSharedPreference.getBoolean("${overlayType}_overlay", true)
    }

    fun processUpdate(update: MutableMap<OrnaViewUpdateType, Any?>) {
        var dungeonDone = false
        var dungeonFailed = false
        update.forEach { (type, data) ->
            when (type) {
                OrnaViewUpdateType.DUNGEON_ENTERED -> {
                    if (mDungeonVisit == null) {
                        val view: OrnaViewDungeonEntry = mCurrentView as OrnaViewDungeonEntry
                        if (mOnholdVisits.containsKey(view.mDungeonName)) {
                            Log.i(TAG, "Reloading on hold visit to ${view.mDungeonName}.")
                            mDungeonVisit = mOnholdVisits[view.mDungeonName]
                            mOnholdVisits.remove(view.mDungeonName)
                        } else {
                            var sessionID: Long? = null
                            if (mSession != null) {
                                sessionID = mSession!!.mID
                                mSession!!.mDungeonsVisited++
                            }
                            mDungeonVisit = DungeonVisit(sessionID, view.mDungeonName, view.mMode)
                            Log.d(TAG, "Entered: $mDungeonVisit")
                        }
                    }

                    if (isOverlayEnabled("session")) {
                        mSessionOverlay.update(mSession, mDungeonVisit)
                    }
                }

                OrnaViewUpdateType.DUNGEON_NEW_DUNGEON -> {
                    if (mDungeonVisit != null) {
                        Log.i(TAG, "Putting on hold visit to ${mDungeonVisit!!.name}.")
                        mOnholdVisits[mDungeonVisit!!.name] = mDungeonVisit!!
                        mDungeonVisit = null
                    }
                }

                OrnaViewUpdateType.DUNGEON_MODE_CHANGED -> {
                    if (mDungeonVisit != null) {
                        val view: OrnaViewDungeonEntry = mCurrentView as OrnaViewDungeonEntry
                        mDungeonVisit!!.mode = view.mMode
                    }
                }

                OrnaViewUpdateType.DUNGEON_GODFORGE -> if (mDungeonVisit != null) mDungeonVisit!!.godforges++
                OrnaViewUpdateType.DUNGEON_DONE -> if (mDungeonVisit != null) {
                    if (mSession == null) {
                        mSessionOverlay.hide()
                    }
                    dungeonDone = true
                }

                OrnaViewUpdateType.DUNGEON_FAIL -> if (mDungeonVisit != null) {
                    if (mSession == null) {
                        mSessionOverlay.hide()
                    }
                    dungeonDone = true
                    dungeonFailed = true
                }

                OrnaViewUpdateType.DUNGEON_NEW_FLOOR -> if (mDungeonVisit != null) mDungeonVisit!!.floor =
                    (data as Int).toLong()

                OrnaViewUpdateType.DUNGEON_EXPERIENCE -> {
                    if (mDungeonVisit != null) mDungeonVisit!!.experience += data as Int
                    if (mSession != null) mSession!!.experience += data as Int
                    if (isOverlayEnabled("session")) {
                        mSessionOverlay.update(mSession, mDungeonVisit)
                    }
                }

                OrnaViewUpdateType.DUNGEON_ORNS -> {
                    if (mDungeonVisit != null) mDungeonVisit!!.orns += data as Int
                    if (mSession != null) mSession!!.orns += data as Int
                    if (isOverlayEnabled("session")) {
                        mSessionOverlay.update(mSession, mDungeonVisit)
                    }
                }

                OrnaViewUpdateType.DUNGEON_GOLD -> {
                    if (mDungeonVisit != null) mDungeonVisit!!.gold += data as Int
                    if (mSession != null) mSession!!.gold += data as Int
                    if (isOverlayEnabled("session")) {
                        mSessionOverlay.update(mSession, mDungeonVisit)
                    }
                }

                OrnaViewUpdateType.NOTIFICATIONS_INVITERS -> {
                    if (isOverlayEnabled("invites") || isOverlayEnabled("inviter")) {
                        mInviterOverlay.update(data as MutableMap<String, Rect>)
                    }
                }

                OrnaViewUpdateType.ITEM_ASSESS_RESULTS -> {
                    if (isOverlayEnabled("assess")) {
                        val assessData = data as? AssessResult
                        if (assessData != null) {
                            // Check if we should hide existing overlay for different item
                            if (mAssessOverlay.shouldHideForNewItem(assessData.itemName)) {
                                Log.d(TAG, "Hiding overlay for different item")
                                mAssessOverlay.hide()
                            }

                            Log.d(TAG, "Showing assess overlay with data: $assessData")
                            mAssessOverlay.update(assessData)
                        } else {
                            Log.e(TAG, "Error: ITEM_ASSESS_RESULTS data is not of type AssessResult: $data")
                        }
                    } else {
                        Log.d(TAG, "Assess overlay is disabled")
                    }
                }

                OrnaViewUpdateType.KINGDOM_GAUNTLET_LIST -> TODO()
            }
        }

        if (dungeonDone) {
            if (mDungeonVisit != null) {
                mDungeonVisit!!.completed = !dungeonFailed
                mDungeonVisit!!.finish()
                mDungeonDbHelper.insertData(mDungeonVisit!!)
                Log.i(TAG, "Stored: $mDungeonVisit")
            }
            mDungeonVisit = null
        }
    }
}