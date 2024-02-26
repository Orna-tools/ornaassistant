package com.lloir.ornaassistant

import android.accessibilityservice.AccessibilityService
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDateTime
import java.util.ArrayList

@RequiresApi(Build.VERSION_CODES.O)
class Battle(private val mAS: AccessibilityService) {
    private val TAG = "OrnaBattle"
    private var mLastClick = LocalDateTime.now()

    fun update(data: ArrayList<ScreenData>) {
    }

    companion object {
        fun inBattle(data: ArrayList<ScreenData>): Boolean
        {
            return data.any { it.name == "Codex" } && data.any{ it.name == "SKILL"}
        }
    }
}