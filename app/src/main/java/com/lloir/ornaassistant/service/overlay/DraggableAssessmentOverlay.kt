package com.lloir.ornaassistant.service.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.lloir.ornaassistant.domain.model.AssessmentResult

// Define a data class to hold the data for updateContent
data class AssessmentOverlayData(val itemName: String, val assessment: AssessmentResult?)

class DraggableAssessmentOverlay(
    context: Context,
    windowManager: WindowManager
) : DraggableOverlayView(context, windowManager, "assessment") {

    private var titleView: TextView? = null
    private var tableLayout: TableLayout? = null

    // The 'context' used in this method is inherited from LinearLayout (via DraggableOverlayView)
    override fun setupContent() {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.BLACK)
        alpha = 0.9f
        setPadding(12, 8, 12, 8)

        // Title
        titleView = TextView(this.context).apply {
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(0, 0, 0, 4)
            gravity = Gravity.CENTER
        }
        addView(titleView)

        // Table for assessment data
        tableLayout = TableLayout(this.context).apply {
            isStretchAllColumns = true
            isShrinkAllColumns = true
            setPadding(0, 4, 0, 0)
        }
        addView(tableLayout)
    }

    override fun updateContent(data: Any?) {
        if (data is AssessmentOverlayData) {
            val itemName = data.itemName
            val assessment = data.assessment

            titleView?.text = itemName

            if (assessment != null) {
                // Clear existing table
                tableLayout?.removeAllViews()

                // Quality percentage in header
                val qualityPercentage = (assessment.quality * 100).toInt()
                val qualityColor = when {
                    assessment.quality >= 1.8 -> Color.GREEN
                    assessment.quality >= 1.5 -> Color.YELLOW
                    else -> Color.WHITE
                }

                // Create header row
                val headerRow = TableRow(context).apply {
                    setBackgroundColor(Color.DKGRAY)
                    background.alpha = 200
                }

                // Add quality percentage to header
                headerRow.addView(TextView(context).apply {
                    text = "$qualityPercentage %"
                    setTextColor(qualityColor)
                    setTypeface(null, Typeface.BOLD)
                    gravity = Gravity.CENTER
                    setPadding(4, 2, 4, 2)
                })

                // Add stat headers
                assessment.stats.keys.forEach { statName ->
                    headerRow.addView(TextView(context).apply {
                        text = statName.take(3).replaceFirstChar { it.uppercase() }
                        setTextColor(Color.WHITE)
                        setTypeface(null, Typeface.BOLD)
                        gravity = Gravity.CENTER
                        setPadding(4, 2, 4, 2)
                    })
                }

                // Add materials header
                headerRow.addView(TextView(context).apply {
                    text = "Mats"
                    setTextColor(Color.WHITE)
                    setTypeface(null, Typeface.BOLD)
                    gravity = Gravity.CENTER
                    setPadding(4, 2, 4, 2)
                })

                tableLayout?.addView(headerRow)

                // Create rows for different upgrade levels
                val upgradeLabels = listOf("10", "MF", "DF", "GF")

                for (i in 0..3) {
                    val row = TableRow(context)

                    // Add upgrade label
                    row.addView(TextView(context).apply {
                        text = upgradeLabels[i]
                        setTextColor(Color.WHITE)
                        setTypeface(null, Typeface.BOLD)
                        gravity = Gravity.CENTER
                        setPadding(4, 2, 4, 2)
                        setBackgroundColor(Color.DKGRAY)
                        background.alpha = 200
                    })

                    // Add stat values
                    assessment.stats.forEach { (_, values) ->
                        row.addView(TextView(context).apply {
                            text = if (i < values.size) values[i] else ""
                            setTextColor(Color.WHITE)
                            gravity = Gravity.CENTER
                            setPadding(4, 2, 4, 2)
                        })
                    }

                    // Add material values
                    row.addView(TextView(context).apply {
                        text = when (i) {
                            0 -> "135"
                            1 -> if (assessment.materials.size > 1) assessment.materials[1].toString() else ""
                            2 -> if (assessment.materials.size > 2) assessment.materials[2].toString() else ""
                            3 -> if (assessment.materials.size > 3) assessment.materials[3].toString() else ""
                            else -> ""
                        }
                        setTextColor(Color.WHITE)
                        gravity = Gravity.CENTER
                        setPadding(4, 2, 4, 2)
                    })

                    tableLayout?.addView(row)
                }
            } else {
                // Clear table and show loading message
                tableLayout?.removeAllViews()

                val loadingRow = TableRow(context)
                loadingRow.addView(TextView(context).apply {
                    text = "Assessing..."
                    setTextColor(Color.YELLOW)
                    gravity = Gravity.CENTER
                    setPadding(4, 2, 4, 2)
                })

                tableLayout?.addView(loadingRow)
            }
        } else {
            // Handle cases where data is not of the expected type, or is null
            titleView?.text = "Invalid data"
            tableLayout?.removeAllViews()
        }
    }
}
