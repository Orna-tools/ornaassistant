package com.lloir.ornaassistant.service.parser

import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.domain.model.ScreenType
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
    private val wayvesselParser: WayvesselScreenParser,
    private val notificationParser: NotificationScreenParser,
    private val battleParser: BattleScreenParser
) {
    private val parserScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun processScreen(parsedScreen: ParsedScreen) {
        parserScope.launch {
            when (parsedScreen.screenType) {
                ScreenType.DUNGEON_ENTRY -> dungeonParser.parseScreen(parsedScreen)
                ScreenType.ITEM_DETAIL -> itemParser.parseScreen(parsedScreen)
                ScreenType.WAYVESSEL -> wayvesselParser.parseScreen(parsedScreen)
                ScreenType.NOTIFICATIONS -> notificationParser.parseScreen(parsedScreen)
                ScreenType.BATTLE -> battleParser.parseScreen(parsedScreen)
                ScreenType.INVENTORY -> { /* Handle inventory if needed */ }
                ScreenType.UNKNOWN -> { /* No specific handling needed */ }
            }
        }
    }

    fun clearItemAssessment() {
        itemParser.clearCurrentAssessment()
    }
}