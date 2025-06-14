package com.lloir.ornaassistant.service.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.lloir.ornaassistant.domain.model.AssessmentResult

class DraggableAssessmentOverlay(
    service: AccessibilityService,
    windowManager: WindowManager
) : DraggableOverlayView(service, windowManager) {

    private var titleView: TextView? = null
    private var qualityView: TextView? = null
    private var statsView: TextView? = null
    private var materialsView: TextView? = null

    override fun setupLayout() {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.BLACK)
        alpha = 0.9f
        setPadding(12, 8, 12, 8)

        // Title
        titleView = TextView(service).apply {
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(0, 0, 0, 4)
        }
        addView(titleView)

        // Quality
        qualityView = TextView(service).apply {
            textSize = 11f
            setPadding(0, 0, 0, 2)
        }
        addView(qualityView)

        // Stats
        statsView = TextView(service).apply {
            setTextColor(Color.CYAN)
            textSize = 10f
            setPadding(0, 0, 0, 2)
        }
        addView(statsView)

        // Materials
        materialsView = TextView(service).apply {
            setTextColor(Color.LTGRAY)
            textSize = 10f
        }
        addView(materialsView)
    }

    fun updateContent(itemName: String, assessment: AssessmentResult?) {
        titleView?.text = itemName

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
    }
}
