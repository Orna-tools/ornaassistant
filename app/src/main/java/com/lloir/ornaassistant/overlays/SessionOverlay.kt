package com.lloir.ornaassistant.overlays

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.lloir.ornaassistant.DungeonMode
import com.lloir.ornaassistant.DungeonVisit
import com.lloir.ornaassistant.R
import com.lloir.ornaassistant.WayvesselSession

class SessionOverlay(
    mWM: WindowManager,
    mCtx: Context,
    mView: View,  // ✅ Correct order: `mView` comes before `mWidth`
    mWidth: Double
) : Overlay(mWM, mCtx, mView, mWidth) {

    private val mGoldHeaderTv: TextView = mView.findViewById(R.id.tvGold)
    private val mSessionHeaderTv: TextView = mView.findViewById(R.id.tvSessionHeader)
    private val mSessionTv: TextView = mView.findViewById(R.id.tvSession)
    private val mSessionGoldTv: TextView = mView.findViewById(R.id.tvGoldSession)
    private val mSessionOrnTv: TextView = mView.findViewById(R.id.tvOrnsSession)
    private val mDungeonGoldTv: TextView = mView.findViewById(R.id.tvGoldDungeon)
    private val mDungeonOrnTv: TextView = mView.findViewById(R.id.tvOrnsDungeon)

    fun update(session: WayvesselSession?, dungeonVisit: DungeonVisit?) {
        mUIRequestHandler.post {
            mSessionHeaderTv.text = session?.name ?: ""
            mSessionTv.text = session?.mDungeonsVisited?.let { "$it dungeons" } ?: ""
            mSessionGoldTv.text = session?.gold?.let { formatNumber(it) } ?: "0"
            mSessionOrnTv.text = session?.orns?.let { formatNumber(it) } ?: "0"

            mGoldHeaderTv.text = if (dungeonVisit?.mode?.mMode == DungeonMode.Modes.ENDLESS) "Exp" else "Gold"
            mDungeonGoldTv.text = dungeonVisit?.gold?.let { formatNumber(it) } ?: "0"
            mDungeonOrnTv.text = dungeonVisit?.orns?.let { formatNumber(it) } ?: "0"
        }

        show()
    }

    private fun formatNumber(value: Long): String {
        return when {
            value > 1_000_000 -> "%.1f m".format(value.toFloat() / 1_000_000)
            value > 1_000 -> "%.1f k".format(value.toFloat() / 1_000)
            else -> value.toString()
        }
    }
}
