package com.lloir.ornaassistant

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

data class ScreenData(
    val name: String,
    val rect: Rect,
    val time: Long,
    val depth: Int,
    val mNodeInfo: AccessibilityNodeInfo?
) {
    init {
        require(name.isNotBlank()) { "Screen data name cannot be blank" }
        require(name.length <= 500) { "Screen data name too long" }
        require(depth >= 0) { "Depth cannot be negative" }
    }
    
    fun isValid(): Boolean {
        return name.isNotBlank() && 
               name.length <= 500 && 
               rect.width() > 0 && 
               rect.height() > 0 &&
               depth >= 0
    }
}
