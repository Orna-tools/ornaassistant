package com.lloir.ornaassistant.service.parser

import android.graphics.Bitmap
import android.util.Log
import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.domain.model.ScreenData
import com.lloir.ornaassistant.domain.model.ScreenType
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import com.lloir.ornaassistant.service.parser.impl.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenParserManager @Inject constructor(
    private val dungeonParser: DungeonScreenParser,
    private val itemParser: ItemScreenParser,
    private val battleParser: BattleScreenParser,
    private val mlKitScreenParser: MlKitScreenParser,
    private val settingsRepository: SettingsRepository
) {
    private val parserScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val TAG = "ScreenParserManager"

    fun processScreen(parsedScreen: ParsedScreen) {
        parserScope.launch {
            when (parsedScreen.screenType) {
                ScreenType.DUNGEON_ENTRY -> dungeonParser.parseScreen(parsedScreen)
                ScreenType.ITEM_DETAIL -> itemParser.parseScreen(parsedScreen)
                ScreenType.BATTLE -> battleParser.parseScreen(parsedScreen)
                ScreenType.NOTIFICATIONS -> { /* Handle notifications if needed */ }
                ScreenType.INVENTORY -> { /* Handle inventory if needed */ }
                ScreenType.UNKNOWN -> { /* No specific handling needed */ }
            }
        }
    }

    /**
     * Processes a screen capture using ML Kit if enabled in settings.
     * This is an experimental feature that can be toggled in the app settings.
     *
     * @param bitmap The screen capture as a bitmap
     * @param screenType The type of screen being processed
     * @return true if ML Kit was used, false if the feature is disabled
     */
    suspend fun processScreenWithMlKit(bitmap: Bitmap, screenType: ScreenType): Boolean {
        try {
            // Check if ML Kit is enabled in settings
            if (!mlKitScreenParser.isEnabled()) {
                return false
            }

            Log.d(TAG, "Using ML Kit for screen parsing")

            // Process the image with ML Kit
            val screenData = mlKitScreenParser.processImage(bitmap)

            // Create a ParsedScreen object and process it
            val parsedScreen = ParsedScreen(
                screenType = screenType,
                data = screenData,
                timestamp = java.time.LocalDateTime.now()
            )

            // Process the parsed screen
            processScreen(parsedScreen)
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Error processing screen with ML Kit", e)
            return false
        }
    }

    fun clearItemAssessment() {
        itemParser.clearCurrentAssessment()
    }
}
