// Replace your existing AssessOverlay.kt with this updated version
package com.lloir.ornaassistant.overlays

import android.content.Context
import android.view.View
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lloir.ornaassistant.R
import com.lloir.ornaassistant.assess.AssessResult
import com.lloir.ornaassistant.viewadapters.AssessAdapter
import com.lloir.ornaassistant.viewadapters.AssessItem

class AssessOverlay(
    mWM: WindowManager,
    mCtx: Context,
    mView: View,
    mWidth: Double
) : Overlay(mWM, mCtx, mView, mWidth) {

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
        val statsMap = assessResult.stats

        // Define preferred order of stats and their display names for headers
        val orderedStats = listOf(
            "attack" to "Att",
            "magic" to "Mag",
            "defense" to "Def",
            "resistance" to "Res",
            "dexterity" to "Dex",
            "hp" to "HP",
            "mana" to "Mana"
        )

        // Build header row
        val headerList = mutableListOf("${(quality * 100).toInt()}%")
        orderedStats.forEach { (statKey, displayName) ->
            if (statsMap.containsKey(statKey)) {
                headerList.add(displayName)
            }
        }
        headerList.add("Mats")
        newList.add(AssessItem(headerList))

        // Determine available upgrade levels from the API response
        val maxLevels = statsMap.values.maxOfOrNull { it.values.size } ?: 0
        val levelsToShow = when {
            maxLevels >= 13 -> listOf(9, 10, 11, 12) // 0-indexed: Level 10, MF, DF, GF
            maxLevels >= 10 -> listOf(maxLevels - 1) // Just show max level
            else -> listOf(maxLevels - 1).filter { it >= 0 }
        }

        val levelLabels = when {
            maxLevels >= 13 -> listOf("10", "MF", "DF", "GF")
            maxLevels >= 10 -> listOf("Max")
            else -> listOf("Cur")
        }

        // Build data rows
        for (i in levelsToShow.indices) {
            val levelIndex = levelsToShow[i]
            val itemList = mutableListOf<String>()

            // Add level label
            itemList.add(levelLabels.getOrElse(i) { levelIndex.toString() })

            // Add stat values for this level
            orderedStats.forEach { (statKey, _) ->
                if (statsMap.containsKey(statKey)) {
                    val statSeries = statsMap[statKey]
                    if (statSeries != null && levelIndex < statSeries.values.size) {
                        itemList.add(statSeries.values[levelIndex].toString())
                    } else {
                        itemList.add("-")
                    }
                }
            }

            // Add material costs
            val materialCost = when {
                levelLabels.getOrNull(i) == "10" -> "135"
                levelLabels.getOrNull(i) == "MF" -> (300 * quality).toInt().toString()
                levelLabels.getOrNull(i) == "DF" -> (666 * quality).toInt().toString()
                levelLabels.getOrNull(i) == "GF" -> ""
                else -> ""
            }
            itemList.add(materialCost)

            newList.add(AssessItem(itemList))
        }

        // Add item info row if we have materials data
        if (assessResult.materials?.isNotEmpty() == true || assessResult.tier != null) {
            val infoList = mutableListOf<String>()
            infoList.add("Info")

            // Add tier if available
            if (assessResult.tier != null) {
                infoList.add("T${assessResult.tier}")
            }

            // Add item type if available
            if (!assessResult.itemType.isNullOrBlank()) {
                infoList.add(assessResult.itemType)
            }

            // Add material names if available
            if (assessResult.materials?.isNotEmpty() == true) {
                val materialNames = assessResult.materials.joinToString(", ") { it.name }
                infoList.add(materialNames)
            }

            // Only add if we have meaningful info
            if (infoList.size > 1) {
                newList.add(AssessItem(infoList))
            }
        }

        mRv.post {
            mAssessList.clear()
            mAssessList.addAll(newList)
            mRv.adapter?.notifyDataSetChanged()
        }
        show()
    }
}