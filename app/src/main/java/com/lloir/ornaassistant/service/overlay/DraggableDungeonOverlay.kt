package com.lloir.ornaassistant.service.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.view.WindowManager
import android.widget.TextView
import com.lloir.ornaassistant.domain.model.DungeonVisit
import com.lloir.ornaassistant.domain.model.DungeonMode

/**
 * Data class to hold dungeon information for the overlay
 */
data class DungeonOverlayData(
    val dungeonVisit: DungeonVisit?,
    val currentFloor: Long = 0,
    val totalFloors: Long = 0,
    val specialInfo: String? = null
)

/**
 * Draggable overlay for displaying dungeon information
 */
class DraggableDungeonOverlay(
    context: AccessibilityService,
    windowManager: WindowManager
) : DraggableOverlayView(context, windowManager, "dungeon") {

    private var titleView: TextView? = null
    private var modeView: TextView? = null
    private var floorView: TextView? = null
    private var rewardsView: TextView? = null
    private var cooldownView: TextView? = null
    private var specialInfoView: TextView? = null

    override fun setupContent() {
        // Title view for dungeon name
        titleView = createTitleTextView()
        addView(titleView)

        // Mode view (Normal/Hard/Boss/Endless)
        modeView = createSubtitleTextView(Color.YELLOW)
        addView(modeView)

        // Floor information
        floorView = createSubtitleTextView(Color.WHITE)
        addView(floorView)

        // Rewards (orns, gold, experience)
        rewardsView = createDetailTextView(Color.CYAN)
        addView(rewardsView)

        // Cooldown information
        cooldownView = createSmallInfoTextView(Color.LTGRAY)
        addView(cooldownView)

        // Special information based on dungeon type
        specialInfoView = createSmallInfoTextView(Color.GREEN)
        addView(specialInfoView)
    }

    override fun updateContent(data: Any?) {
        if (data is DungeonOverlayData) {
            updateDungeonInfo(data)
        }
    }

    private fun updateDungeonInfo(data: DungeonOverlayData) {
        val visit = data.dungeonVisit

        if (visit == null) {
            titleView?.text = "No Active Dungeon"
            modeView?.text = ""
            floorView?.text = ""
            rewardsView?.text = ""
            cooldownView?.text = ""
            specialInfoView?.text = ""
            return
        }

        // Update dungeon name with type information
        titleView?.text = getDungeonDisplayName(visit.name)

        // Update mode information
        modeView?.text = getModeDisplayText(visit.mode)

        // Update floor information
        val currentFloor = data.currentFloor.takeIf { it > 0 } ?: visit.floor
        val totalFloors = data.totalFloors.takeIf { it > 0 } ?: getTotalFloorsForDungeon(visit)
        floorView?.text = "Floor: $currentFloor${if (totalFloors > 0) "/$totalFloors" else ""}"

        // Update rewards information
        rewardsView?.text = "Orns: ${formatNumber(visit.orns)} | Gold: ${formatNumber(visit.gold)} | XP: ${formatNumber(visit.experience)}"

        // Update cooldown information
        if (visit.cooldownHours() > 0) {
            val cooldownText = if (visit.isOnCooldown()) {
                "Cooldown: ${formatTimeRemaining(visit.cooldownEndTime().toLocalTime())}"
            } else {
                "Cooldown: ${visit.cooldownHours()} hours"
            }
            cooldownView?.text = cooldownText
        } else {
            cooldownView?.text = ""
        }

        // Update special information based on dungeon type
        specialInfoView?.text = data.specialInfo ?: getSpecialInfoForDungeon(visit.name)
    }

    private fun getDungeonDisplayName(name: String): String {
        // Add emoji or special formatting based on dungeon type
        return when {
            name.contains("Beast Den", ignoreCase = true) -> "🐺 Beast Den"
            name.contains("Dragon Roost", ignoreCase = true) -> "🐉 Dragon Roost"
            name.contains("Chaos Portal", ignoreCase = true) -> "🌀 Chaos Portal"
            name.contains("Underworld Portal", ignoreCase = true) -> "🔥 Underworld Portal"
            name.contains("BattleGrounds", ignoreCase = true) -> "⚔️ BattleGrounds"
            name.contains("Valley Of The Gods", ignoreCase = true) -> "🏔️ Valley Of The Gods"
            name.contains("Goblin Fortress", ignoreCase = true) -> "🏰 Goblin Fortress"
            name.contains("Mystic Cave", ignoreCase = true) -> "🔮 Mystic Cave"
            name.endsWith("Dungeon", ignoreCase = true) -> "🏛️ $name"
            else -> name
        }
    }

    private fun getModeDisplayText(mode: DungeonMode): String {
        val modeText = when (mode.type) {
            DungeonMode.Type.NORMAL -> "Normal"
            DungeonMode.Type.BOSS -> "Boss"
            DungeonMode.Type.ENDLESS -> "Endless"
        }

        return if (mode.isHard) "Hard $modeText Mode" else "$modeText Mode"
    }

    private fun getTotalFloorsForDungeon(visit: DungeonVisit): Long {
        // Based on the documentation in the issue description
        val playerTier = 10 // This should be obtained from player data

        // For exploration dungeons
        if (visit.name.contains("Goblin Fortress") || visit.name.contains("Mystic Cave")) {
            return when (playerTier) {
                1, 2 -> 5
                3, 4 -> 5
                5 -> 6
                6 -> 7
                7 -> 8
                8 -> 9
                9 -> 10
                10 -> 11
                11 -> 12
                else -> 5
            }
        }

        // For battle dungeons (simplified)
        return when (playerTier) {
            1 -> 5
            2 -> 9
            3 -> 10
            4 -> 10
            5 -> 13
            6 -> 13
            7 -> 13
            8 -> 20
            9 -> 20
            10 -> 22
            11 -> 25
            else -> 10
        }
    }

    private fun getSpecialInfoForDungeon(dungeonName: String): String {
        return when {
            dungeonName.contains("Beast Den", ignoreCase = true) -> 
                "Animal family monsters, +Orns reward"
            dungeonName.contains("Dragon Roost", ignoreCase = true) -> 
                "Draconian & Dragon families, +Orns reward"
            dungeonName.contains("Chaos Portal", ignoreCase = true) -> 
                "Weather/time monsters, +Items reward"
            dungeonName.contains("Underworld Portal", ignoreCase = true) -> 
                "Balor Forces family, +Items reward"
            dungeonName.contains("BattleGrounds", ignoreCase = true) -> 
                "Lyonesse & Nothren Forces, +Items reward"
            dungeonName.contains("Valley Of The Gods", ignoreCase = true) -> 
                "Arisen monsters, +Gold/Orns/Items reward"
            dungeonName.contains("Goblin Fortress", ignoreCase = true) -> 
                "Orc & Goblin Hordes, Exploration Dungeon"
            dungeonName.contains("Mystic Cave", ignoreCase = true) -> 
                "Magical & Ancient families, Exploration Dungeon"
            else -> ""
        }
    }


    private fun formatTimeRemaining(time: java.time.LocalTime): String {
        return String.format("%02d:%02d:%02d", time.hour, time.minute, time.second)
    }
}
