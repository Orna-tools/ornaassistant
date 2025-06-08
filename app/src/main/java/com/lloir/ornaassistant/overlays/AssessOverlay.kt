package com.lloir.ornaassistant.overlays

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lloir.ornaassistant.R
import com.lloir.ornaassistant.assess.AssessResult
import com.lloir.ornaassistant.viewadapters.AssessAdapter
import com.lloir.ornaassistant.viewadapters.AssessItem
import java.util.concurrent.atomic.AtomicBoolean

class AssessOverlay(
    mWM: WindowManager,
    mCtx: Context,
    mView: View,
    mWidth: Double
) : Overlay(mWM, mCtx, mView, mWidth) {

    companion object {
        private const val TAG = "AssessOverlay"
    }

    var mRv = mView.findViewById<RecyclerView>(R.id.rvAssess)
    var mAssessList = mutableListOf<AssessItem>()
    private var currentItemName: String? = null
    private val isShowing = AtomicBoolean(false)

    init {
        mRv.adapter = AssessAdapter(mAssessList, ::hide)
        mRv.layoutManager = LinearLayoutManager(mCtx)
        setupPersistentOverlay()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPersistentOverlay() {
        // Override the default overlay parameters for persistence
        mParamFloat.apply {
            // Make it persistent but allow touches to pass through to hide button
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

            // Position it nicely on screen
            gravity = Gravity.TOP or Gravity.END
            x = 20 // Small margin from right edge
            y = 100 // Below status bar

            // Set explicit size
            width = (mCtx.resources.displayMetrics.widthPixels * mWidth).toInt()
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }

        // Set up touch handling - only hide on touch, don't auto-hide
        mView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d(TAG, "Assess overlay touched - hiding")
                    hide()
                    true
                }
                else -> false
            }
        }

        // Make RecyclerView also respond to touch
        mRv.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d(TAG, "Assess RecyclerView touched - hiding")
                    hide()
                    true
                }
                else -> false
            }
        }
    }

    override fun show() {
        if (isShowing.compareAndSet(false, true)) {
            try {
                mWM.addView(mView, mParamFloat)
                Log.d(TAG, "Assess overlay shown for item: $currentItemName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show assess overlay", e)
                isShowing.set(false)
            }
        }
    }

    override fun hide() {
        if (isShowing.compareAndSet(true, false)) {
            try {
                mWM.removeView(mView)
                Log.d(TAG, "Assess overlay hidden")
                currentItemName = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to hide assess overlay", e)
            }
        }
    }

    fun update(assessResult: AssessResult) {
        // Check if this is a different item - if so, hide current and show new
        if (currentItemName != null && currentItemName != assessResult.itemName) {
            Log.d(TAG, "Different item detected: $currentItemName -> ${assessResult.itemName}")
            hide()
        }

        currentItemName = assessResult.itemName

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

        // Build header row - include item name and quality
        val headerList = mutableListOf("${assessResult.itemName ?: "Item"}")
        newList.add(AssessItem(headerList))

        // Quality row
        val qualityList = mutableListOf("${(quality * 100).toInt()}%")
        orderedStats.forEach { (statKey, displayName) ->
            if (statsMap.containsKey(statKey)) {
                qualityList.add(displayName)
            }
        }
        qualityList.add("Mats")
        newList.add(AssessItem(qualityList))

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

        // Add instructions row
        val instructionsList = mutableListOf("Tap to close")
        newList.add(AssessItem(instructionsList))

        mRv.post {
            mAssessList.clear()
            mAssessList.addAll(newList)
            mRv.adapter?.notifyDataSetChanged()
        }

        show()
    }

    // Method to check if overlay should be hidden for new item
    fun shouldHideForNewItem(newItemName: String?): Boolean {
        return isShowing.get() && currentItemName != null && currentItemName != newItemName
    }
}