package com.lloir.ornaassistant.ornaviews

import android.content.Context
import android.view.WindowManager
import com.lloir.ornaassistant.OrnaView
import com.lloir.ornaassistant.OrnaViewType
import com.lloir.ornaassistant.ScreenData

class OrnaViewInventory(data: ArrayList<ScreenData>, wm: WindowManager, ctx: Context) :
    OrnaView(OrnaViewType.INVENTORY, wm, ctx) {
}