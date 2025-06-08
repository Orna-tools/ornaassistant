package com.lloir.ornaassistant.overlays

import android.content.Context
import android.view.View
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lloir.ornaassistant.R
import com.lloir.ornaassistant.assess.AssessResult // Import AssessResult
import com.lloir.ornaassistant.assess.StatSeries // Import StatSeries as it's used in statsMap value
import com.lloir.ornaassistant.viewadapters.AssessAdapter
import com.lloir.ornaassistant.viewadapters.AssessItem
// Removed org.json imports (if any were left)

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

    fun update(assessResult: AssessResult) {
        val newList = mutableListOf<AssessItem>()

        val quality = assessResult.quality
        val statsMap = assessResult.stats // Map<String, StatSeries>

        // Define preferred order of stats and their display names for headers
        val orderedStats = listOf(
            "Attack", "Magic", "Defense", "Resistance", "Dexterity", "HP", "Mana", "Ward"
        )

        val headerList = mutableListOf("${(quality * 100).toInt()}%")
        orderedStats.forEach { statName ->
            if (statsMap.containsKey(statName)) { // Add header only if stat exists in results
                headerList.add(statName.take(3).replaceFirstChar { it.titlecase() })
            }
        }
        headerList.add("Mats")
        newList.add(AssessItem(headerList))

        val levelsToShowIndices = listOf(9, 10, 11, 12) // Level 10, MF, DF, GF (0-indexed for list of 13 levels)

        for (i in 0..3) { // Corresponds to "10", "MF", "DF", "GF" labels
            val currentLevelIndex = levelsToShowIndices[i]
            val itemList = mutableListOf<String>()
            itemList.add(
                when (i) {
                    0 -> "10"
                    1 -> "MF"
                    2 -> "DF"
                    3 -> "GF"
                    else -> ""
                }
            )

            orderedStats.forEach { statName ->
                if (statsMap.containsKey(statName)) { // Add data only if stat exists
                    val statSeries = statsMap[statName]
                    if (statSeries != null && currentLevelIndex < statSeries.values.size) {
                        itemList.add(statSeries.values[currentLevelIndex].toString())
                    } else {
                        itemList.add("-") // Placeholder if value doesn't exist for this level/stat
                    }
                }
            }

            // Material costs
            itemList.add(
                when (i) {
                    0 -> "135" // Materials for level 10
                    1 -> (300 * quality).toInt().toString() // Materials for MF
                    2 -> (666 * quality).toInt().toString() // Materials for DF
                    3 -> "" // No materials for GF, or specific value if applicable
                    else -> ""
                }
            )
            newList.add(AssessItem(itemList))
        }

        mRv.post {
            mAssessList.clear()
            mAssessList.addAll(newList)
            mRv.adapter?.notifyDataSetChanged()
        }
        show()
    }
}
