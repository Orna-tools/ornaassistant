package com.lloir.ornaassistant.service.parser

import com.lloir.ornaassistant.domain.model.ParsedScreen

interface ScreenParser {
    suspend fun parseScreen(parsedScreen: ParsedScreen)
}