package com.lloir.ornaassistant.overlays

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lloir.ornaassistant.KingdomMember
import com.lloir.ornaassistant.R
import com.lloir.ornaassistant.viewadapters.KGAdapter
import com.lloir.ornaassistant.viewadapters.KGItem
import java.time.*

class KGOverlay(
    mWM: WindowManager,
    mCtx: Context,
    mView: View,
    mWidth: Double
) :
    Overlay(mWM, mCtx, mView, mWidth) {

    var mRv = mView.findViewById<RecyclerView>(R.id.multiClickRecyclerView)
    var mKGList = mutableListOf<KGItem>()

    init {
        mRv.adapter = KGAdapter(mKGList, ::hide)
        mRv.layoutManager = LinearLayoutManager(mCtx)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    override fun show() {
        super.show()
        mRv.setOnClickListener {
            hide()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun update(data: List<KingdomMember>) {
        val list = mutableListOf<KGItem>()

        list.add(
            KGItem(
                "Player",
                "Floors",
                "Im",
                "Local time",
                "Sleep",
                false,
                "Seen"
            )
        )
        val nowUTC = OffsetDateTime.now( ZoneOffset.UTC )
        data.forEach { it ->
            var sleeptime = ""
            if (it.endTimeLeftSeconds > 0) {
                val sleepHours = it.endTimeLeftSeconds / 3600
                val sleepMinutes = (it.endTimeLeftSeconds - sleepHours * 3600) / 60
                sleeptime = "${sleepHours}h ${sleepMinutes}m"
            }

            var localTime = ""
            if (it.timezone < 1000)
            {
                val time = nowUTC.plusHours(it.timezone.toLong())
                localTime = "${time.hour}.${time.minute.toString().padStart(2, '0')}"
            }
            var floorTexts = mutableListOf<String>()
            val floors =
                it.floors.filterValues { f -> !f.loss }.filterValues { f -> !f.win }.forEach { f ->
                    floorTexts.add(f.value.number.toString())
                }

            if (floorTexts.isNotEmpty()) {
                list.add(
                    KGItem(
                        it.character,
                        floorTexts.joinToString(", "),
                        if (it.immunity) "⭐" else "",
                        localTime,
                        sleeptime,
                        it.zerk,
                        it.seenCount.toString()
                    )
                )
            }
        }

        Log.i("OrnaKGOverlay", "Showing ${list.size} / ${data.size} items")
        mRv.post(Runnable {
            mKGList.clear()
            mKGList.addAll(list)
            mRv.adapter?.notifyDataSetChanged()
        })
        show()
    }
}