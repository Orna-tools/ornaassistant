package com.lloir.ornaassistant.overlays

import android.content.Context
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lloir.ornaassistant.R
import com.lloir.ornaassistant.db.DungeonVisitDatabaseHelper
import com.lloir.ornaassistant.db.WayvesselSessionDatabaseHelper
import com.lloir.ornaassistant.viewadapters.NotificationsAdapter
import com.lloir.ornaassistant.viewadapters.NotificationsItem
import java.time.Duration
import java.time.LocalDateTime
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class InviterOverlay(
    mWM: WindowManager,
    mCtx: Context,
    mView: View,  // ✅ Correct order: `mView` comes before `mWidth`
    mWidth: Double
) : Overlay(mWM, mCtx, mView, mWidth){

    private val mRv: RecyclerView = mView.findViewById(R.id.rvList)
    private val mList = mutableListOf<NotificationsItem>()
    private val mUpdating = AtomicBoolean(false)

    init {
        mRv.layoutManager = LinearLayoutManager(mCtx)
        mRv.adapter = NotificationsAdapter(mList, ::hide)
        mRv.setOnClickListener { hide() }
    }

    fun update(data: MutableMap<String, Rect>) {
        if (!mUpdating.compareAndSet(false, true)) return

        val wvDB = WayvesselSessionDatabaseHelper(mCtx)
        val dungeonDbHelper = DungeonVisitDatabaseHelper(mCtx)
        val newList = mutableListOf<NotificationsItem>()

        data.forEach { (inviter, _) ->
            newList.add(NotificationsItem(inviter, "N", "VoG", "D", "BG", "UW", "CG", "Cooldown", true))
        }

        wvDB.close()
        dungeonDbHelper.close()

        mRv.post {
            mList.clear()
            mList.addAll(newList)
            mRv.adapter?.notifyDataSetChanged()
            mUpdating.set(false)
        }

        show()
    }
}
