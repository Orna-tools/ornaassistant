package com.lloir.ornaassistant.service.parser.impl

import android.util.Log
import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.domain.usecase.StartWayvesselSessionUseCase
import com.lloir.ornaassistant.service.parser.ScreenParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WayvesselScreenParser @Inject constructor(
    private val startWayvesselSessionUseCase: StartWayvesselSessionUseCase
) : ScreenParser {

    private val _currentSession = MutableStateFlow<String?>(null)
    val currentSession: StateFlow<String?> = _currentSession.asStateFlow()

    companion object {
        private const val TAG = "WayvesselScreenParser"
    }

    override suspend fun parseScreen(parsedScreen: ParsedScreen) {
        try {
            val wayvesselName = extractWayvesselName(parsedScreen.data)

            if (wayvesselName != null && _currentSession.value != wayvesselName) {
                val session = startWayvesselSessionUseCase(wayvesselName)
                _currentSession.value = wayvesselName
                Log.d(TAG, "Started wayvessel session: $wayvesselName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing wayvessel screen", e)
        }
    }

    private fun extractWayvesselName(screenData: List<com.lloir.ornaassistant.domain.model.ScreenData>): String? {
        return screenData.find { it.text.contains("'s Wayvessel") }
            ?.text
            ?.replace("'s Wayvessel", "")
    }
}