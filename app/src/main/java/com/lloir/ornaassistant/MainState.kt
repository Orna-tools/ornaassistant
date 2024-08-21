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
import org.json.JSONObject
import java.time.LocalDateTime
import java.util.concurrent.LinkedBlockingDeque
import kotlin.concurrent.thread

class MainState(
    private val mWM: WindowManager,
    private val mCtx: Context,
    mNotificationView: View,
    mSessionView: View,
    mAssessView: View,
    mAS1: View,
    mAS: AccessibilityService
) {
    private val TAG = "OrnaMainState"
    private val mDungeonDbHelper = DungeonVisitDatabaseHelper(mCtx)
    private var mCurrentView: OrnaView? = null
    private var mDungeonVisit: DungeonVisit? = null
    private var mOnholdVisits = mutableMapOf<String, DungeonVisit>()
    private var mSession: WayvesselSession? = null
    private var mBattle = Battle(mAS)

    fun cleanup() {
        mSession?.finish()
        mSession = null
        mCurrentView = null
        mDungeonVisit = null
    }

    private val mInviterOverlay = InviterOverlay(mWM, mCtx, mNotificationView, 0.8)
    private val mSessionOverlay = SessionOverlay(mWM, mCtx, mSessionView, 0.4)
    private val mAssessOverlay = AssessOverlay(mWM, mCtx, mAssessView, 0.7)

    val mOrnaQueue = LinkedBlockingDeque<ArrayList<ScreenData>>()
    val mDiscordQueue = LinkedBlockingDeque<ArrayList<ScreenData>>()

    private val mSharedPreference: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(mCtx)

    var mKGNextUpdate: LocalDateTime = LocalDateTime.now()
    var mInBattle = LocalDateTime.now().minusDays(1)

    var sharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                "invites" -> if (!sharedPreferences.getBoolean(key, true)) mInviterOverlay.hide()
                "session" -> if (!sharedPreferences.getBoolean(key, true)) mSessionOverlay.hide()
            }
        }

    init {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mCtx)
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)

        thread {
            while (true) {
                val data: ArrayList<ScreenData>? = mOrnaQueue.take()
                data?.let { handleOrnaData(it) }
            }
        }
    }

    private fun handleOrnaData(data: ArrayList<ScreenData>) {
        if (Battle.inBattle(data)) {
            mInBattle = LocalDateTime.now()
        }

        updateView(data)

        val wayvessel = data.filter { it.name.lowercase().contains("'s wayvessel") }.firstOrNull()
        if (wayvessel != null && (wayvessel.position.left > 70)) {
            if (data.none { it.name.lowercase().contains("this wayvessel is active") }) {
                val name = wayvessel.name.replace("'s Wayvessel", "")
                try {
                    if (mSession == null || mSession!!.name != name) {if (mSession != null) {
                        mSession!!.finish()
                        mSessionOverlay.hide()
                    }
                        mSession = WayvesselSession(name, mCtx)
                        if (mDungeonVisit != null) {
                            mDungeonVisit!!.sessionID = mSession!!.mID
                        }
                        if (mSharedPreference.getBoolean("session", true)) {
                            mSessionOverlay.update(mSession, mDungeonVisit)
                        }
                        Log.i(TAG, "At wayvessel: $name")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception: $e")
                }
            }
        }

        var leaveParty = data.firstOrNull {
            it.name.lowercase().contains("are you sure you would like to leave this party?")
        }
        if (leaveParty == null) {
            leaveParty =
                data.firstOrNull {
                    it.name.lowercase().contains("are you sure you would return home")
                }
        }
        if (leaveParty != null) {
            Log.d(TAG, "Finished session $mSession")
            mSession?.finish()
            mSessionOverlay.hide()
            mSession = null
        }
    }

    var maxSize = 0

    fun processData(packageName: String, data: ArrayList<ScreenData>) {
        if (packageName.contains("orna")) {
            mOrnaQueue.put(data)
            if (mOrnaQueue.size > maxSize) {
                maxSize = mOrnaQueue.size
                Log.i(TAG, "QUEUE $maxSize")
            }
        } else if (packageName.contains("discord")) {
            mDiscordQueue.put(data)
        }
    }

    private fun updateView(data: ArrayList<ScreenData>) {
        val bDone = mCurrentView?.update(data, ::processUpdate)
        if (bDone != null && bDone) {
            mCurrentView!!.close()
            mCurrentView = null
        }

        val newType = OrnaViewFactory.getType(data)

        if (mCurrentView == null || (newType != null && newType != mCurrentView!!.type)) {
            val view = OrnaViewFactory.create(newType, data, mWM, mCtx)
            var update = true
            if (mCurrentView != null) {
                if (newType == OrnaViewType.ITEM) {
                    if (!mSharedPreference.getBoolean("assess", true)) {
                        update = false
                    }
                }
                if (mCurrentView!!.type == OrnaViewType.ITEM) {
                    mAssessOverlay.hide()
                }
            }

            if (view != null) {
                if (mCurrentView != null) mCurrentView!!.close()
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

                    if (mSharedPreference.getBoolean("session", true)) {
                        mSessionOverlay.update(mSession, mDungeonVisit)
                    }
                }

                OrnaViewUpdateType.DUNGEON_NEW_DUNGEON -> {
                    if (mDungeonVisit != null) {
                        Log.i(TAG, "Putting on hold visit to ${mDungeonVisit!!.name}."
                        )
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
                    if (mSharedPreference.getBoolean("session", true)) {
                        mSessionOverlay.update(mSession, mDungeonVisit)
                    }
                }

                OrnaViewUpdateType.DUNGEON_ORNS -> {
                    if (mDungeonVisit != null) mDungeonVisit!!.orns += data as Int
                    if (mSession != null) mSession!!.orns += data as Int
                    if (mSharedPreference.getBoolean("session", true)) {
                        mSessionOverlay.update(mSession, mDungeonVisit)
                    }
                }

                OrnaViewUpdateType.DUNGEON_GOLD -> {
                    if (mDungeonVisit != null) mDungeonVisit!!.gold += data as Int
                    if (mSession != null) mSession!!.gold += data as Int
                    if (mSharedPreference.getBoolean("session", true)) {
                        mSessionOverlay.update(mSession, mDungeonVisit)
                    }
                }

                OrnaViewUpdateType.NOTIFICATIONS_INVITERS -> {
                    if (mSharedPreference.getBoolean("invites", true)) {
                        mInviterOverlay.update(data as MutableMap<String, Rect>)
                    }
                }

                OrnaViewUpdateType.ITEM_ASSESS_RESULTS -> {
                    mAssessOverlay.update(data as JSONObject)
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