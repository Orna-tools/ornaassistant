package com.rockethat.ornaassistant

import android.accessibilityservice.AccessibilityService
import java.time.LocalDateTime
import java.util.ArrayList

class Battle(private val mAS: AccessibilityService) {
    private var mLastClick = LocalDateTime.now()

    fun update(data: ArrayList<ScreenData>) {

    }

    companion object {
        fun inBattle(data: ArrayList<ScreenData>): Boolean {
            return data.any { it.name == "Codex" } && data.any { it.name == "SKILL" }
        }
    }
}