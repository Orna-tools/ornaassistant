package com.lloir.ornaassistant.service.parser

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.lloir.ornaassistant.domain.model.ScreenData
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * A utility class that uses Google ML Kit to recognize text from screen captures.
 * This is an experimental feature that can be enabled in settings.
 */
@Singleton
class MlKitScreenParser @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val TAG = "MlKitScreenParser"

    /**
     * Processes a bitmap image using ML Kit text recognition.
     * 
     * @param bitmap The bitmap image to process
     * @return A list of ScreenData objects containing the recognized text
     */
    suspend fun processImage(bitmap: Bitmap): List<ScreenData> = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val screenData = extractTextBlocks(visionText)
                    continuation.resume(screenData)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Text recognition failed", e)
                    continuation.resumeWithException(e)
                }
                
            continuation.invokeOnCancellation {
                // No specific cleanup needed for ML Kit
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image with ML Kit", e)
            continuation.resumeWithException(e)
        }
    }
    
    /**
     * Extracts text blocks from ML Kit's vision text result.
     * 
     * @param visionText The result from ML Kit text recognition
     * @return A list of ScreenData objects
     */
    private fun extractTextBlocks(visionText: Text): List<ScreenData> {
        val screenDataList = mutableListOf<ScreenData>()
        val currentTime = System.currentTimeMillis()
        
        // Process text blocks
        for ((blockIndex, block) in visionText.textBlocks.withIndex()) {
            // Add the whole block as one item
            screenDataList.add(
                ScreenData(
                    text = block.text,
                    bounds = Rect(block.boundingBox ?: Rect()),
                    timestamp = currentTime,
                    depth = blockIndex
                )
            )
            
            // Also add each line separately for more granular parsing
            for ((lineIndex, line) in block.lines.withIndex()) {
                screenDataList.add(
                    ScreenData(
                        text = line.text,
                        bounds = Rect(line.boundingBox ?: Rect()),
                        timestamp = currentTime,
                        depth = blockIndex * 100 + lineIndex
                    )
                )
                
                // For very detailed parsing, we could also add each element/word
                // but this might be too granular and create noise
                // Uncomment if needed:
                /*
                for ((elementIndex, element) in line.elements.withIndex()) {
                    screenDataList.add(
                        ScreenData(
                            text = element.text,
                            bounds = Rect(element.boundingBox ?: Rect()),
                            timestamp = currentTime,
                            depth = blockIndex * 10000 + lineIndex * 100 + elementIndex
                        )
                    )
                }
                */
            }
        }
        
        return screenDataList
    }
    
    /**
     * Checks if ML Kit screen parsing is enabled in settings.
     * 
     * @return true if ML Kit is enabled, false otherwise
     */
    suspend fun isEnabled(): Boolean {
        return try {
            settingsRepository.getSettings().useMlKit
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if ML Kit is enabled", e)
            false
        }
    }
}