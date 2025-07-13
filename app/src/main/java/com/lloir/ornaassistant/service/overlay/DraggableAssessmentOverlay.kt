package com.lloir.ornaassistant.service.overlay

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.lloir.ornaassistant.domain.model.AppSettings
import com.lloir.ornaassistant.domain.model.AssessmentResult
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// Define a data class to hold the data for updateContent
data class AssessmentOverlayData(
    val itemName: String, 
    val assessment: AssessmentResult?
)

class DraggableAssessmentOverlay(
    context: Context,
    windowManager: WindowManager,
    private val settingsRepository: SettingsRepository
) : DraggableOverlayView(context, windowManager, "assessment") {

    private var titleView: TextView? = null
    private var qualityView: TextView? = null
    private var statsView: TextView? = null
    private var materialsView: TextView? = null
    
    // Store current settings
    private var settings: AppSettings = runBlocking { settingsRepository.getSettings() }
    
    // Coroutine scope for settings updates
    private val overlayScope = CoroutineScope(Dispatchers.Main)
    
    init {
        // Start observing settings changes
        overlayScope.launch {
            settingsRepository.getSettingsFlow().collect { newSettings ->
                settings = newSettings
                updateAppearance()
            }
        }
    }

    override fun setupContent() {
        // Title
        titleView = createTextView(
            textColor = settings.assessOverlayTitleColor,
            textSize = settings.assessOverlayTitleSize
        )
        addView(titleView)

        // Quality
        qualityView = createTextView(
            textColor = settings.assessOverlayQualityColor,
            textSize = settings.assessOverlayQualitySize
        )
        addView(qualityView)

        // Stats
        statsView = createTextView(
            textColor = settings.assessOverlayStatsColor,
            textSize = settings.assessOverlayStatsSize
        )
        addView(statsView)

        // Materials
        materialsView = createTextView(
            textColor = settings.assessOverlayMaterialsColor,
            textSize = settings.assessOverlayMaterialsSize
        )
        addView(materialsView)
    }
    
    private fun updateAppearance() {
        // Update text sizes and colors based on current settings
        titleView?.apply {
            textSize = settings.assessOverlayTitleSize
            setTextColor(settings.assessOverlayTitleColor)
        }
        
        qualityView?.apply {
            textSize = settings.assessOverlayQualitySize
            setTextColor(settings.assessOverlayQualityColor)
        }
        
        statsView?.apply {
            textSize = settings.assessOverlayStatsSize
            setTextColor(settings.assessOverlayStatsColor)
            visibility = if (settings.assessOverlayShowStats) View.VISIBLE else View.GONE
        }
        
        materialsView?.apply {
            textSize = settings.assessOverlayMaterialsSize
            setTextColor(settings.assessOverlayMaterialsColor)
            visibility = if (settings.assessOverlayShowMaterials) View.VISIBLE else View.GONE
        }
    }

    override fun updateContent(data: Any?) {
        if (data is AssessmentOverlayData) {
            val itemName = data.itemName
            val assessment = data.assessment

            titleView?.text = itemName

            if (assessment != null) {
                // Quality
                qualityView?.apply {
                    text = "Quality: ${String.format("%.2f", assessment.quality * 100)}%"
                }

                // Stats - show current values
                if (assessment.stats.isNotEmpty() && settings.assessOverlayShowStats) {
                    val statsText = assessment.stats.mapNotNull { (statName, values) ->
                        if (values.size >= 2) "$statName: ${values[1]}" else null
                    }.joinToString("  ")

                    statsView?.text = statsText
                    statsView?.visibility = View.VISIBLE
                } else {
                    statsView?.text = ""
                    statsView?.visibility = if (settings.assessOverlayShowStats) View.VISIBLE else View.GONE
                }

                // Materials
                if (assessment.materials.size >= 3 && settings.assessOverlayShowMaterials) {
                    materialsView?.text = "MF: ${assessment.materials[1]} | DF: ${assessment.materials[2]}"
                    materialsView?.visibility = View.VISIBLE
                } else {
                    materialsView?.text = ""
                    materialsView?.visibility = if (settings.assessOverlayShowMaterials) View.VISIBLE else View.GONE
                }
            } else {
                qualityView?.apply {
                    text = "Assessing..."
                }
                statsView?.text = ""
                materialsView?.text = ""
            }
        } else {
            // Handle cases where data is not of the expected type, or is null
            titleView?.text = "Invalid data"
            qualityView?.text = ""
            statsView?.text = ""
            materialsView?.text = ""
        }
    }
}