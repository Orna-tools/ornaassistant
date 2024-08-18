package com.rockethat.ornaassistant.overlays

import android.content.Context
import android.view.View
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rockethat.ornaassistant. overlays. Overlay
import com.rockethat.ornaassistant.R
import com.rockethat.ornaassistant.viewadapters.AssessAdapter
import com.rockethat.ornaassistant.viewadapters.AssessItem
import org.json.JSONObject
import org.json.JSONTokener

class AssessOverlay(
    mWM: WindowManager,
    mCtx: Context,
    mView: View,
    mWidth: Double
) :
    Overlay(mWM, mCtx, mView, mWidth) {

    var mRv = mView.findViewById<RecyclerView>(R.id.rvAssess)
    var mAssessList = mutableListOf<AssessItem>()

    init {
        mRv.adapter = AssessAdapter(mAssessList, ::hide)
        mRv.layoutManager = LinearLayoutManager(mCtx)
    }

    override fun show() {
        super.show()
    }

    fun update(json: JSONObject) {

        val statsJson = (JSONTokener(json.getString("stats")).nextValue() as JSONObject)
        val statsMap = statsJson.keys().asSequence().toList().associate { key ->
            val statBaseJson = statsJson.getJSONObject(key)
            val statValuesArray = statBaseJson.getJSONArray("values")
            key.replaceFirstChar(Char::uppercase) to listOf(statValuesArray.getString(9),
                statValuesArray.getString(10),
                statValuesArray.getString(11),
                statValuesArray.getString(12)
            )
        }


        val quality = json.getDouble("quality")

        val headerList = listOf("${(quality * 100).toInt()} %") +
                statsMap.keys.map { it.take(3).replaceFirstChar(Char::uppercase) } +
                "Mats"

        val list = listOf(AssessItem(headerList)) + PlayerPosition.values().map { position ->
            val values = listOf(position.name) +
                    statsMap.values.map { it[position.ordinal] } +
                    getMaterialCount(position, quality)
            AssessItem(values)
        }

        mRv.post {
            mAssessList.clear()
            mAssessList.addAll(list)
            mRv.adapter?.notifyDataSetChanged()
        }
        show()
    }

    private fun getMaterialCount(position: PlayerPosition, quality: Double): String {
        return when (position) {
            PlayerPosition.TEN -> "135"
            PlayerPosition.MF -> (300 * quality).toInt().toString()
            PlayerPosition.DF -> (666 * quality).toInt().toString()
            PlayerPosition.GF -> ""
        }
    }
}

data class AssessItem(val values: List<String>)
enum class PlayerPosition { TEN, MF, DF, GF }