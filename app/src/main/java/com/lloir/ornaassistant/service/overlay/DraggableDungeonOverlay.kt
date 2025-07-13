package com.lloir.ornaassistant.service.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.view.WindowManager
import android.widget.TextView
import com.lloir.ornaassistant.domain.model.AppSettings
import com.lloir.ornaassistant.domain.model.DungeonVisit
import com.lloir.ornaassistant.domain.model.DungeonMode
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import com.lloir.ornaassistant.utils.AccessibilityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
    windowManager: WindowManager,
    private val settingsRepository: SettingsRepository
) : DraggableOverlayView(context, windowManager, "dungeon") {

    private var titleView: TextView? = null
    private var modeView: TextView? = null
    private var floorView: TextView? = null
    private var rewardsView: TextView? = null
    private var cooldownView: TextView? = null
    private var specialInfoView: TextView? = null

    // Store current settings
    private var settings: AppSettings = runBlocking { settingsRepository.getSettings() }

    // Coroutine scope for settings updates
    private val overlayScope = CoroutineScope(Dispatchers.Main)

    init {
        android.util.Log.d("DungeonOverlay", "Initial settings loaded: titleSize=${settings.dungeonOverlayTitleSize}, titleColor=${settings.dungeonOverlayTitleColor}")

        // Start observing settings changes
        android.util.Log.d("DungeonOverlay", "Starting to observe settings changes")
        overlayScope.launch {
            settingsRepository.getSettingsFlow().collect { newSettings ->
                android.util.Log.d("DungeonOverlay", "Settings changed: titleSize=${newSettings.dungeonOverlayTitleSize}, titleColor=${newSettings.dungeonOverlayTitleColor}")
                settings = newSettings
                updateAppearance()
            }
        }
    }

    // Update appearance based on current settings
    private fun updateAppearance() {
        android.util.Log.d("DungeonOverlay", "Updating appearance with settings: titleSize=${settings.dungeonOverlayTitleSize}, titleColor=${settings.dungeonOverlayTitleColor}")

        // Apply font sizes and colors using apply blocks
        titleView?.apply {
            textSize = settings.dungeonOverlayTitleSize
            setTextColor(settings.dungeonOverlayTitleColor)
        }

        modeView?.apply {
            textSize = settings.dungeonOverlayModeSize
            setTextColor(settings.dungeonOverlayModeColor)
        }

        floorView?.apply {
            textSize = settings.dungeonOverlayFloorSize
            setTextColor(settings.dungeonOverlayFloorColor)
        }

        rewardsView?.apply {
            textSize = settings.dungeonOverlayRewardsSize
            setTextColor(settings.dungeonOverlayRewardsColor)
        }

        cooldownView?.apply {
            textSize = settings.dungeonOverlayCooldownSize
            setTextColor(settings.dungeonOverlayCooldownColor)
        }

        specialInfoView?.apply {
            textSize = settings.dungeonOverlaySpecialInfoSize
            setTextColor(settings.dungeonOverlaySpecialInfoColor)
        }

        // Apply accessibility settings
        applyDungeonAccessibilitySettings(settings)
    }

    // Apply accessibility settings
    private fun applyDungeonAccessibilitySettings(settings: AppSettings) {
        android.util.Log.d("DungeonOverlay", "Applying accessibility settings: highContrast=${settings.useHighContrastMode}, largerFont=${settings.useLargerFontSize}, tts=${settings.useTextToSpeech}, reducedMotion=${settings.useReducedMotion}")

        // Apply high contrast mode if enabled
        if (settings.useHighContrastMode) {
            android.util.Log.d("DungeonOverlay", "Applying high contrast mode")
            setBackgroundColor(Color.BLACK)
            titleView?.setTextColor(Color.WHITE)
            modeView?.setTextColor(Color.YELLOW)
            floorView?.setTextColor(Color.WHITE)
            rewardsView?.setTextColor(Color.WHITE)
            cooldownView?.setTextColor(Color.WHITE)
            specialInfoView?.setTextColor(Color.WHITE)
        }

        // Apply larger font size if enabled
        if (settings.useLargerFontSize) {
            android.util.Log.d("DungeonOverlay", "Applying larger font size")
            titleView?.textSize = titleView?.textSize?.times(1.3f) ?: 14f
            modeView?.textSize = modeView?.textSize?.times(1.3f) ?: 12f
            floorView?.textSize = floorView?.textSize?.times(1.3f) ?: 12f
            rewardsView?.textSize = rewardsView?.textSize?.times(1.3f) ?: 11f
            cooldownView?.textSize = cooldownView?.textSize?.times(1.3f) ?: 10f
            specialInfoView?.textSize = specialInfoView?.textSize?.times(1.3f) ?: 10f
        }
    }

    override fun setupContent() {
        android.util.Log.d("DungeonOverlay", "Setting up content with settings: titleSize=${settings.dungeonOverlayTitleSize}, titleColor=${settings.dungeonOverlayTitleColor}")

        // Title view for dungeon name
        titleView = createTextView(
            textColor = settings.dungeonOverlayTitleColor,
            textSize = settings.dungeonOverlayTitleSize
        )
        addView(titleView)
        android.util.Log.d("DungeonOverlay", "Created titleView with textSize=${titleView?.textSize}, textColor=${titleView?.currentTextColor}")

        // Mode view (Normal/Hard/Boss/Endless)
        modeView = createTextView(
            textColor = settings.dungeonOverlayModeColor,
            textSize = settings.dungeonOverlayModeSize
        )
        addView(modeView)
        android.util.Log.d("DungeonOverlay", "Created modeView with textSize=${modeView?.textSize}, textColor=${modeView?.currentTextColor}")

        // Floor information
        floorView = createTextView(
            textColor = settings.dungeonOverlayFloorColor,
            textSize = settings.dungeonOverlayFloorSize
        )
        addView(floorView)
        android.util.Log.d("DungeonOverlay", "Created floorView with textSize=${floorView?.textSize}, textColor=${floorView?.currentTextColor}")

        // Rewards (orns, gold, experience)
        rewardsView = createTextView(
            textColor = settings.dungeonOverlayRewardsColor,
            textSize = settings.dungeonOverlayRewardsSize
        )
        addView(rewardsView)
        android.util.Log.d("DungeonOverlay", "Created rewardsView with textSize=${rewardsView?.textSize}, textColor=${rewardsView?.currentTextColor}")

        // Cooldown information
        cooldownView = createTextView(
            textColor = settings.dungeonOverlayCooldownColor,
            textSize = settings.dungeonOverlayCooldownSize
        )
        addView(cooldownView)
        android.util.Log.d("DungeonOverlay", "Created cooldownView with textSize=${cooldownView?.textSize}, textColor=${cooldownView?.currentTextColor}")

        // Special information based on dungeon type
        specialInfoView = createTextView(
            textColor = settings.dungeonOverlaySpecialInfoColor,
            textSize = settings.dungeonOverlaySpecialInfoSize
        )
        addView(specialInfoView)
        android.util.Log.d("DungeonOverlay", "Created specialInfoView with textSize=${specialInfoView?.textSize}, textColor=${specialInfoView?.currentTextColor}")

        // Apply accessibility settings
        android.util.Log.d("DungeonOverlay", "Applying accessibility settings from setupContent")
        applyAccessibilitySettings(settings)
    }

    override fun updateContent(data: Any?) {
        if (data is DungeonOverlayData) {
            updateDungeonInfo(data)
        }
    }

    private fun updateDungeonInfo(data: DungeonOverlayData) {
        val visit = data.dungeonVisit
        val currentSettings = settings

        android.util.Log.d("DungeonOverlay", "Updating dungeon info with settings: ${currentSettings != null}")
        if (currentSettings != null) {
            android.util.Log.d("DungeonOverlay", "Settings values in updateDungeonInfo: titleSize=${currentSettings.dungeonOverlayTitleSize}, titleColor=${currentSettings.dungeonOverlayTitleColor}")
        }

        if (visit == null) {
            android.util.Log.d("DungeonOverlay", "No active dungeon visit")
            titleView?.text = "No Active Dungeon"
            modeView?.text = ""
            floorView?.text = ""
            rewardsView?.text = ""
            cooldownView?.text = ""
            specialInfoView?.text = ""
            return
        }

        android.util.Log.d("DungeonOverlay", "Active dungeon: ${visit.name}, floor: ${visit.floor}, mode: ${visit.mode}")

        // Update dungeon name with type information
        val dungeonName = getDungeonDisplayName(visit.name)
        titleView?.text = dungeonName

        // Update mode information
        val modeText = getModeDisplayText(visit.mode)
        modeView?.text = modeText

        // Update floor information
        val previousFloor = visit.floor
        val currentFloor = data.currentFloor.takeIf { it > 0 } ?: visit.floor
        val totalFloors = data.totalFloors.takeIf { it > 0 } ?: getTotalFloorsForDungeon(visit)

        // Check if floor has changed for announcements
        val floorChanged = previousFloor != currentFloor && previousFloor > 0 && currentFloor > 0

        // Special handling for endless dungeons
        val floorText = if (visit.mode.type == DungeonMode.Type.ENDLESS || totalFloors == -1L) {
            "Floor: $currentFloor/∞"
        } 
        // Show floor progress based on settings
        else if (currentSettings?.showFloorProgress == true && totalFloors > 0) {
            "Floor: $currentFloor/$totalFloors (${(currentFloor * 100 / totalFloors)}%)"
        } else {
            "Floor: $currentFloor${if (totalFloors > 0) "/$totalFloors" else ""}"
        }

        floorView?.text = floorText

        // Update rewards information based on settings
        val rewardsText = if (currentSettings?.showRewardsEstimate == true) {
            // Show estimated rewards based on dungeon type and floor
            val estimatedOrns = estimateRewards(visit, "orns", currentFloor.toInt())
            val estimatedGold = estimateRewards(visit, "gold", currentFloor.toInt())
            val estimatedXP = estimateRewards(visit, "xp", currentFloor.toInt())

            "Est. Orns: ${formatNumber(estimatedOrns)} | Gold: ${formatNumber(estimatedGold)} | XP: ${formatNumber(estimatedXP)}"
        } else {
            // Show actual rewards
            "Orns: ${formatNumber(visit.orns)} | Gold: ${formatNumber(visit.gold)} | XP: ${formatNumber(visit.experience)}"
        }

        rewardsView?.text = rewardsText

        // Update cooldown information
        val cooldownText = if (visit.cooldownHours() > 0) {
            if (visit.isOnCooldown()) {
                "Cooldown: ${formatTimeRemaining(visit.cooldownEndTime().toLocalTime())}"
            } else {
                "Cooldown: ${visit.cooldownHours()} hours"
            }
        } else {
            ""
        }

        cooldownView?.text = cooldownText

        // Update special information based on dungeon type and settings
        if (currentSettings?.showDungeonSpecialInfo == true) {
            val specialInfo = data.specialInfo ?: getSpecialInfoForDungeon(visit.name)
            specialInfoView?.text = specialInfo
            specialInfoView?.visibility = android.view.View.VISIBLE
        } else {
            specialInfoView?.visibility = android.view.View.GONE
        }

        // Apply color coding if enabled
        if (currentSettings?.colorCodeDungeons == true) {
            applyColorCoding(visit.name)
        }

        // Announce floor change with text-to-speech if enabled
        if (floorChanged && currentSettings?.useTextToSpeech == true) {
            announceFloorChange(currentFloor)
        }

        // Flash the overlay when floor changes if enabled
        if (floorChanged && currentSettings?.flashOnFloorChange == true) {
            flashOverlay(currentSettings.useReducedMotion)
        }

        // Log the final state of the TextViews
        android.util.Log.d("DungeonOverlay", "Final TextView states after update:")
        android.util.Log.d("DungeonOverlay", "titleView: text='${titleView?.text}', textSize=${titleView?.textSize}, textColor=${titleView?.currentTextColor}")
        android.util.Log.d("DungeonOverlay", "modeView: text='${modeView?.text}', textSize=${modeView?.textSize}, textColor=${modeView?.currentTextColor}")
        android.util.Log.d("DungeonOverlay", "floorView: text='${floorView?.text}', textSize=${floorView?.textSize}, textColor=${floorView?.currentTextColor}")
        android.util.Log.d("DungeonOverlay", "rewardsView: text='${rewardsView?.text}', textSize=${rewardsView?.textSize}, textColor=${rewardsView?.currentTextColor}")
        android.util.Log.d("DungeonOverlay", "cooldownView: text='${cooldownView?.text}', textSize=${cooldownView?.textSize}, textColor=${cooldownView?.currentTextColor}")
        android.util.Log.d("DungeonOverlay", "specialInfoView: text='${specialInfoView?.text}', textSize=${specialInfoView?.textSize}, textColor=${specialInfoView?.currentTextColor}")
    }

    // Announce floor change with text-to-speech
    private fun announceFloorChange(newFloor: Long) {
        val currentSettings = settings ?: return
        if (currentSettings.useTextToSpeech) {
            AccessibilityUtils.speakIfEnabled(
                "Floor $newFloor", 
                currentSettings.useTextToSpeech
            )
        }
    }

    // Flash the overlay when floor changes
    private fun flashOverlay(useReducedMotion: Boolean) {
        // Save current alpha
        val originalAlpha = alpha

        // Use reduced motion settings if enabled
        val animationDuration = AccessibilityUtils.getAnimationDuration(useReducedMotion)

        // Flash the overlay
        alpha = 1.0f

        // Reset alpha after delay
        CoroutineScope(Dispatchers.Main).launch {
            delay(animationDuration)
            alpha = originalAlpha
        }
    }

    private fun estimateRewards(visit: DungeonVisit, rewardType: String, floor: Int): Long {
        // Simple estimation based on dungeon type and floor
        val baseMultiplier = when {
            visit.name.contains("Beast Den", ignoreCase = true) -> 1.2
            visit.name.contains("Dragon Roost", ignoreCase = true) -> 1.3
            visit.name.contains("Chaos Portal", ignoreCase = true) -> 1.1
            visit.name.contains("Valley Of The Gods", ignoreCase = true) -> 1.5
            else -> 1.0
        }

        val floorMultiplier = 1.0 + (floor * 0.1)
        val hardModeMultiplier = if (visit.mode.isHard) 1.5 else 1.0

        val baseValue = when (rewardType) {
            "orns" -> visit.orns.takeIf { it > 0 } ?: 1000L
            "gold" -> visit.gold.takeIf { it > 0 } ?: 5000L
            "xp" -> visit.experience.takeIf { it > 0 } ?: 2000L
            else -> 1000L
        }

        return (baseValue * baseMultiplier * floorMultiplier * hardModeMultiplier).toLong()
    }

    private fun applyColorCoding(dungeonName: String) {
        val backgroundColor = when {
            dungeonName.contains("Beast Den", ignoreCase = true) -> Color.parseColor("#3A5F0B")  // Dark green
            dungeonName.contains("Dragon Roost", ignoreCase = true) -> Color.parseColor("#8B0000")  // Dark red
            dungeonName.contains("Chaos Portal", ignoreCase = true) -> Color.parseColor("#4B0082")  // Indigo
            dungeonName.contains("Valley Of The Gods", ignoreCase = true) -> Color.parseColor("#663399")  // Purple
            dungeonName.contains("BattleGrounds", ignoreCase = true) -> Color.parseColor("#8B4513")  // Brown
            dungeonName.contains("Goblin Fortress", ignoreCase = true) -> Color.parseColor("#006400")  // Dark green
            dungeonName.contains("Mystic Cave", ignoreCase = true) -> Color.parseColor("#483D8B")  // Dark slate blue
            else -> Color.BLACK
        }

        setBackgroundColor(backgroundColor)
        alpha = 0.85f
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
        // Special case for endless dungeons
        if (visit.mode.type == DungeonMode.Type.ENDLESS) {
            return -1 // Special value to indicate endless dungeon
        }

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
