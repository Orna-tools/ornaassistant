package com.lloir.ornaassistant.service.parser

import android.util.Log
import com.lloir.ornaassistant.domain.model.DungeonState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DungeonStateTracker @Inject constructor() {
    private var lastKnownDungeonName: String? = null
    private var lastUpdateTime: Long = 0
    private val STALE_TIMEOUT = 5 * 60 * 1000L // 5 minutes

    // Enhanced tracking
    private var currentFloor: Int = 1
    private var maxFloorReached: Int = 1
    private var dungeonState: DungeonState = DungeonState()

    fun updateDungeonName(name: String) {
        if (name.isNotEmpty() && name != "Unknown Dungeon") {
            lastKnownDungeonName = name
            lastUpdateTime = System.currentTimeMillis()
        }
    }

    fun getLastKnownDungeonName(): String? {
        val now = System.currentTimeMillis()
        return if (now - lastUpdateTime < STALE_TIMEOUT) {
            lastKnownDungeonName
        } else {
            null
        }
    }

    fun updateFloor(floor: Int) {
        currentFloor = floor
        if (floor > maxFloorReached) {
            maxFloorReached = floor
            Log.d(TAG, "New max floor reached: $maxFloorReached")
        }
        // Reset stale timeout when floor changes
        lastUpdateTime = System.currentTimeMillis()
    }

    fun getCurrentFloor(): Int = currentFloor

    fun getMaxFloorReached(): Int = maxFloorReached

    fun getDungeonState(): DungeonState = dungeonState

    fun updateDungeonState(newState: DungeonState) {
        dungeonState = newState
        lastUpdateTime = System.currentTimeMillis()
    }

    fun attemptRecovery(screenData: List<com.lloir.ornaassistant.domain.model.ScreenData>) {
        // If we're in a dungeon but lost track of which one
        if (isInDungeon() && getLastKnownDungeonName() == null) {
            // Try to infer from context clues
            val possibleDungeonNames = DUNGEON_NAMES.filter { name ->
                screenData.any { it.text.contains(name, ignoreCase = true) }
            }

            if (possibleDungeonNames.isNotEmpty()) {
                updateDungeonName(possibleDungeonNames.first())
                Log.d(TAG, "Recovery: Restored dungeon name to ${possibleDungeonNames.first()}")
            }
        }
    }

    fun clear() {
        lastKnownDungeonName = null
        lastUpdateTime = 0
        currentFloor = 1
        maxFloorReached = 1
        dungeonState = DungeonState()
    }

    fun isInDungeon(): Boolean {
        return getLastKnownDungeonName() != null
    }

    companion object {
        private const val TAG = "DungeonStateTracker"

        // Known dungeon names
        private val DUNGEON_NAMES = listOf(
            "Beast Den", "Dragon Roost", "Chaos Portal", "Underworld Portal",
            "BattleGrounds", "Valley Of The Gods", "Goblin Fortress", "Mystic Cave"
        )
    }
}
