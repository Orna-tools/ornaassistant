package com.lloir.ornaassistant.service.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.lloir.ornaassistant.domain.model.DungeonMode
import com.lloir.ornaassistant.domain.model.DungeonVisit
import com.lloir.ornaassistant.domain.model.WayvesselSession

class DraggableSessionOverlay(
    service: AccessibilityService,
    windowManager: WindowManager
) : DraggableOverlayView(service, windowManager) {

    private var contentView: TextView? = null

    override fun setupLayout() {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.BLACK)
        alpha = 0.8f
        setPadding(16, 16, 16, 16)
        elevation = 10f

        contentView = TextView(service).apply {
            setTextColor(Color.WHITE)
            textSize = 11f
            setPadding(8, 8, 8, 8)
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }
        addView(contentView)
    }

    fun updateContent(data: Pair<WayvesselSession?, DungeonVisit?>) {
        val (wayvesselSession, dungeonVisit) = data

        val displayText = buildString {
            if (wayvesselSession != null) {
                appendLine("@${wayvesselSession.name}")
                if (wayvesselSession.dungeonsVisited > 1) {
                    append("Session: ${formatNumber(wayvesselSession.orns)} orns")
                    if (dungeonVisit?.mode?.type != DungeonMode.Type.ENDLESS) {
                        append(", ${formatNumber(wayvesselSession.gold)} gold")
                    } else {
                        append(", ${formatNumber(wayvesselSession.experience)} exp")
                    }
                    appendLine()
                }
            }
            if (dungeonVisit != null) {
                append("${dungeonVisit.name} ${dungeonVisit.mode}")
                if (dungeonVisit.floor > 0) {
                    append(" Floor ${dungeonVisit.floor}")
                }
                appendLine()
                append("${formatNumber(dungeonVisit.orns)} orns")
                if (dungeonVisit.mode.type == DungeonMode.Type.ENDLESS) {
                    append(", ${formatNumber(dungeonVisit.experience)} exp")
                } else {
                    append(", ${formatNumber(dungeonVisit.gold)} gold")
                }
                if (dungeonVisit.godforges > 0) {
                    append(" [GF: ${dungeonVisit.godforges}]")
                }
            }
        }

        contentView?.text = displayText
    }

    private fun formatNumber(value: Long): String {
        return when {
            value >= 1_000_000 -> "%.1f m".format(value / 1_000_000.0)
            value >= 1_000 -> "%.1f k".format(value / 1_000.0)
            else -> value.toString()
        }
    }

    fun create() {
        setupLayout()
        setupTouchHandling()
    }
}
