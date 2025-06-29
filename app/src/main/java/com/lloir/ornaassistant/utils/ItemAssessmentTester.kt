package com.lloir.ornaassistant.utils

import android.util.Log
import com.lloir.ornaassistant.domain.model.AssessmentResult
import com.lloir.ornaassistant.domain.usecase.AssessItemUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for testing item assessment functionality without relying on overlays.
 * 
 * This class provides methods to directly assess items by name, level, and attributes,
 * and logs the results to the console. It can be used for debugging and testing the
 * item assessment functionality independently of the overlay UI.
 */
@Singleton
class ItemAssessmentTester @Inject constructor(
    private val assessItemUseCase: AssessItemUseCase
) {
    private val TAG = "ItemAssessmentTester"
    private val testerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Assess an item with the given name, level, and attributes.
     * 
     * @param itemName The name of the item to assess
     * @param level The level of the item
     * @param attributes A map of attribute names to values
     * @param callback Optional callback to receive the assessment result
     */
    fun assessItem(
        itemName: String,
        level: Int,
        attributes: Map<String, Int>,
        callback: ((AssessmentResult) -> Unit)? = null
    ) {
        testerScope.launch {
            try {
                Log.d(TAG, "Assessing item: $itemName (level $level) with attributes: $attributes")
                
                val result = assessItemUseCase(itemName, level, attributes)
                
                // Log the result
                logAssessmentResult(itemName, result)
                
                // Invoke the callback on the main thread if provided
                callback?.let {
                    withContext(Dispatchers.Main) {
                        it(result)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error assessing item: $itemName", e)
            }
        }
    }
    
    /**
     * Log the assessment result to the console.
     */
    private fun logAssessmentResult(itemName: String, result: AssessmentResult) {
        Log.d(TAG, "===== ASSESSMENT RESULT FOR: $itemName =====")
        Log.d(TAG, "Quality: ${result.quality}")
        
        if (result.assessmentFailed) {
            Log.w(TAG, "Assessment failed - possible reasons:")
            Log.w(TAG, "1. Adornments not properly subtracted")
            Log.w(TAG, "2. Wrong item level detected")
            Log.w(TAG, "3. Item name parsing issues")
            return
        }
        
        Log.d(TAG, "Stats:")
        result.stats.forEach { (statName, values) ->
            Log.d(TAG, "  $statName: ${values.joinToString(" → ")}")
        }
        
        Log.d(TAG, "Materials:")
        if (result.materials.isNotEmpty()) {
            Log.d(TAG, "  10★: ${result.materials.getOrNull(0) ?: 0}")
            Log.d(TAG, "  MF: ${result.materials.getOrNull(1) ?: 0}")
            Log.d(TAG, "  DF: ${result.materials.getOrNull(2) ?: 0}")
            Log.d(TAG, "  GF: ${result.materials.getOrNull(3) ?: 0}")
        }
        Log.d(TAG, "===========================================")
    }
    
    /**
     * Example method to test with a predefined item.
     * This can be called from anywhere in the app to test the assessment functionality.
     */
    fun testWithExampleItem(callback: ((AssessmentResult) -> Unit)? = null) {
        val exampleItem = "Ornate Arisen Vulcan's Longsword"
        val exampleLevel = 10
        val exampleAttributes = mapOf(
            "Att" to 120,
            "Mag" to 0,
            "Def" to 0,
            "Res" to 0,
            "Dex" to 10
        )
        
        assessItem(exampleItem, exampleLevel, exampleAttributes, callback)
    }
}