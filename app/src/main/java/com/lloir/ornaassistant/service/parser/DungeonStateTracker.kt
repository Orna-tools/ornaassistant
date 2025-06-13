package com.lloir.ornaassistant.service.parser

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DungeonStateTracker @Inject constructor() {
    private var lastKnownDungeonName: String? = null
    private var lastUpdateTime: Long = 0
    private val STALE_TIMEOUT = 5 * 60 * 1000L // 5 minutes

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

    fun clear() {
        lastKnownDungeonName = null
        lastUpdateTime = 0
    }

    fun isInDungeon(): Boolean {
        return getLastKnownDungeonName() != null
    }
}
