package com.lloir.ornaassistant.service.overlay

import android.content.Context
import android.graphics.Color
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.lloir.ornaassistant.domain.model.AssessmentResult

// Data class for adornment warnings
data class AdornmentWarning(
    val slotsUsed: Int,
    val slotsTotal: Int,
    val visibleAdornments: Int
)

// Define a data class to hold the data for updateContent
data class AssessmentOverlayData(
    val itemName: String, 
    val assessment: AssessmentResult?,
    val adornmentWarning: AdornmentWarning? = null
)

class DraggableAssessmentOverlay(
    context: Context,
    windowManager: WindowManager
) : DraggableOverlayView(context, windowManager, "assessment") {

    private var titleView: TextView? = null
    private var qualityView: TextView? = null
    private var statsView: TextView? = null
    private var materialsView: TextView? = null
    private var warningView: TextView? = null

    // The 'context' used in this method is inherited from LinearLayout (via DraggableOverlayView)
    override fun setupContent() {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.BLACK)
        alpha = 0.9f
        setPadding(12, 8, 12, 8)

        // Title
        titleView = TextView(this.context).apply { // Explicitly using 'this.context' for clarity
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(0, 0, 0, 4)
        }
        addView(titleView)

        // Quality
        qualityView = TextView(context).apply {
            textSize = 11f
            setPadding(0, 0, 0, 2)
        }
        addView(qualityView)

        // Stats
        statsView = TextView(this.context).apply { // Explicitly using 'this.context'
            setTextColor(Color.CYAN)
            textSize = 10f
            setPadding(0, 0, 0, 2)
        }
        addView(statsView)

        // Materials
        materialsView = TextView(this.context).apply { // Explicitly using 'this.context'
            setTextColor(Color.LTGRAY)
            textSize = 10f
        }
        addView(materialsView)

        // Warning message for adornments
        warningView = TextView(this.context).apply {
            setTextColor(Color.RED)
            textSize = 11f
            setPadding(0, 4, 0, 0)
            visibility = android.view.View.GONE
        }
        addView(warningView)
    }

    override fun updateContent(data: Any?) {
        if (data is AssessmentOverlayData) {
            val itemName = data.itemName
            val assessment = data.assessment
            val adornmentWarning = data.adornmentWarning

            titleView?.text = itemName

            // Handle adornment warning
            if (adornmentWarning != null) {
                val missingAdornments = adornmentWarning.slotsUsed - adornmentWarning.visibleAdornments
                warningView?.apply {
                    text = "⚠️ SCROLL DOWN to see all adornments! ($missingAdornments missing)"
                    visibility = android.view.View.VISIBLE
                }
            } else {
                warningView?.visibility = android.view.View.GONE
            }

            if (assessment != null) {
                // Quality with color coding
                val qualityColor = when {
                    assessment.quality >= 1.8 -> Color.GREEN
                    assessment.quality >= 1.5 -> Color.YELLOW
                    else -> Color.WHITE
                }
                qualityView?.apply {
                    text = "Quality: ${String.format("%.2f", assessment.quality)}"
                    setTextColor(qualityColor)
                }

                // Stats - show current values
                if (assessment.stats.isNotEmpty()) {
                    val statsText = assessment.stats.mapNotNull { (statName, values) ->
                        if (values.size >= 2) "$statName: ${values[1]}" else null
                    }.joinToString("  ")

                    statsView?.text = statsText
                } else {
                    statsView?.text = ""
                }

                // Materials
                if (assessment.materials.size >= 3) {
                    materialsView?.text = "MF: ${assessment.materials[1]} | DF: ${assessment.materials[2]}"
                } else {
                    materialsView?.text = ""
                }
            } else {
                qualityView?.apply {
                    text = "Assessing..."
                    setTextColor(Color.YELLOW)
                }
                statsView?.text = ""
                materialsView?.text = ""
            }
        } else {
            // Handle cases where data is not of the expected type, or is null
            // For example, clear the views or show a default state
            titleView?.text = "Invalid data"
            qualityView?.text = ""
            statsView?.text = ""
            materialsView?.text = ""
        }
    }
}
