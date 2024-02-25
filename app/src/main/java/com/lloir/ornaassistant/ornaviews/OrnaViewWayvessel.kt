package com.lloir.ornaassistant.ornaviews

import android.content.Context
import android.view.WindowManager
import com.lloir.ornaassistant.OrnaView
import com.lloir.ornaassistant.OrnaViewType
import com.lloir.ornaassistant.ScreenData
import java.util.ArrayList

class OrnaViewWayvessel(data: ArrayList<ScreenData>, wm: WindowManager, ctx: Context) :
    OrnaView(OrnaViewType.WAYVESSEL, wm, ctx) {
}