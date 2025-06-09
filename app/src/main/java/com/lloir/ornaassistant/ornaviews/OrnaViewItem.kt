package com.lloir.ornaassistant.ornaviews

import android.content.Context
import android.util.Log
import android.view.WindowManager
import com.lloir.ornaassistant.*
import com.lloir.ornaassistant.api.OrnaApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OrnaViewItem(
    data: ArrayList<ScreenData>,
    wm: WindowManager,
    ctx: Context
) : OrnaView(OrnaViewType.ITEM, wm, ctx) {

    companion object {
        private const val TAG = "OrnaViewItem"
    }

    private val apiClient = OrnaApiClient(VolleySingleton.getInstance(ctx))
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var hasProcessed = false

    override fun update(
        data: ArrayList<ScreenData>,
        updateResults: (MutableMap<OrnaViewUpdateType, Any?>) -> Unit
    ): Boolean {
        // Only process once per item view
        if (!hasProcessed) {
            hasProcessed = true
            processItemData(data, updateResults)
        }
        return false
    }

    /**
     * Process item data by delegating everything to OrnaApiClient
     */
    private fun processItemData(
        data: ArrayList<ScreenData>,
        updateResults: (MutableMap<OrnaViewUpdateType, Any?>) -> Unit
    ) {
        Log.d(TAG, "Processing item screen with ${data.size} screen elements")

        coroutineScope.launch {
            try {
                // Let OrnaApiClient handle ALL the heavy lifting
                val assessResult = apiClient.processItemScreenData(data)

                if (assessResult != null) {
                    Log.d(TAG, "Successfully assessed item: ${assessResult.itemName} (${assessResult.quality}% quality)")

                    // Pass result back to MainState via update callback
                    updateResults(mutableMapOf(
                        OrnaViewUpdateType.ITEM_ASSESS_RESULTS to assessResult
                    ))
                } else {
                    Log.w(TAG, "Failed to assess item from screen data")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing item data", e)
            }
        }
    }

    override fun drawOverlay() {
        // Override if you need to draw custom overlays
        // For now, assessment results are handled by AssessOverlay in MainState
    }
}