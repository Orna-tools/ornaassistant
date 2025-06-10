package com.lloir.ornaassistant.service.parser.impl

import android.util.Log
import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.domain.model.ScreenData
import com.lloir.ornaassistant.domain.usecase.AssessItemUseCase
import com.lloir.ornaassistant.service.parser.ScreenParser
import com.lloir.ornaassistant.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemScreenParser @Inject constructor(
    private val assessItemUseCase: AssessItemUseCase
) : ScreenParser {

    private val parserScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        private const val TAG = "ItemScreenParser"
    }

    override suspend fun parseScreen(parsedScreen: ParsedScreen) {
        try {
            val itemName = extractItemName(parsedScreen.data)
            val level = extractLevel(parsedScreen.data)
            val attributes = extractAttributes(parsedScreen.data)

            if (itemName != null && level != null && attributes.isNotEmpty()) {
                // Assess item asynchronously
                parserScope.launch {
                    try {
                        assessItemUseCase(itemName, level, attributes)
                        Log.d(TAG, "Successfully assessed item: $itemName")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to assess item: $itemName", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing item screen", e)
        }
    }

    private fun extractItemName(screenData: List<ScreenData>): String? {
        var name = screenData.firstOrNull()?.text

        if (name?.contains("You are") == true) {
            name = screenData.getOrNull(1)?.text
        }

        // Remove quality prefixes
        Constants.ITEM_QUALITY_PREFIXES.forEach { prefix ->
            if (name?.startsWith(prefix) == true) {
                name = name.replace(prefix, "").trim()
            }
        }

        // Remove enchantment prefixes
        Constants.ENCHANTMENT_PREFIXES.forEach { prefix ->
            if (name?.startsWith(prefix.replaceFirstChar { it.uppercase() }) == true) {
                name = name.replace("${prefix.replaceFirstChar { it.uppercase() }} ", "")
            }
        }

        return name
    }

    private fun extractLevel(screenData: List<ScreenData>): Int? {
        return screenData.find { it.text.startsWith("Level ") }
            ?.text
            ?.replace("Level ", "")
            ?.toIntOrNull()
    }

    private fun extractAttributes(screenData: List<ScreenData>): Map<String, Int> {
        val attributes = mutableMapOf<String, Int>()
        val acceptedAttributes = listOf("Att", "Mag", "Def", "Res", "Dex", "Crit", "Mana", "Ward", "HP")
        var isAdornmentSection = false

        screenData.forEach { item ->
            if (item.text.contains("ADORNMENTS")) {
                isAdornmentSection = true
                return@forEach
            }

            val cleanText = item.text
                .replace("−", "-")
                .replace(" ", "")
                .replace(",", "")
                .replace(".", "")

            val match = Regex("([A-Za-z\\s]+):\\s*(-?[0-9]+)").find(cleanText)
            if (match != null && match.groups.size == 3) {
                val attName = match.groups[1]?.value?.trim()
                val attVal = match.groups[2]?.value?.toIntOrNull()

                if (attName != null && attVal != null && acceptedAttributes.contains(attName)) {
                    if (isAdornmentSection) {
                        // Subtract adornment values from base stats
                        val currentValue = attributes[attName] ?: 0
                        attributes[attName] = currentValue - attVal
                    } else {
                        attributes[attName] = attVal
                    }
                }
            }
        }

        return attributes
    }
}

// FILE: service/parser/impl/WayvesselScreenParser.kt
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

// FILE: service/parser/impl/NotificationScreenParser.kt
package com.lloir.ornaassistant.service.parser.impl

import android.util.Log
import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.domain.model.PartyInvite
import com.lloir.ornaassistant.service.parser.ScreenParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScreenParser @Inject constructor() : ScreenParser {

    private val _partyInvites = MutableStateFlow<List<PartyInvite>>(emptyList())
    val partyInvites: StateFlow<List<PartyInvite>> = _partyInvites.asStateFlow()

    companion object {
        private const val TAG = "NotificationScreenParser"
    }

    override suspend fun parseScreen(parsedScreen: ParsedScreen) {
        try {
            val invites = extractPartyInvites(parsedScreen.data)
            _partyInvites.value = invites

            if (invites.isNotEmpty()) {
                Log.d(TAG, "Found ${invites.size} party invites")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing notification screen", e)
        }
    }

    private fun extractPartyInvites(screenData: List<com.lloir.ornaassistant.domain.model.ScreenData>): List<PartyInvite> {
        val invites = mutableListOf<PartyInvite>()
        var inviterData: com.lloir.ornaassistant.domain.model.ScreenData? = null

        screenData.forEach { data ->
            if (data.text.contains("invited you to their party", ignoreCase = true)) {
                inviterData = data
            } else if (inviterData != null && data.text.lowercase().contains("accept")) {
                val inviterName = inviterData!!.text.replace(" has invited you to their party.", "")
                invites.add(
                    PartyInvite(
                        inviterName = inviterName,
                        bounds = inviterData!!.bounds,
                        timestamp = LocalDateTime.now()
                    )
                )
                inviterData = null
            }
        }

        return invites
    }
}

// FILE: service/parser/impl/BattleScreenParser.kt
package com.lloir.ornaassistant.service.parser.impl

import android.util.Log
import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.service.parser.ScreenParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BattleScreenParser @Inject constructor() : ScreenParser {

    private val _inBattle = MutableStateFlow(false)
    val inBattle: StateFlow<Boolean> = _inBattle.asStateFlow()

    private val _lastBattleTime = MutableStateFlow<LocalDateTime?>(null)
    val lastBattleTime: StateFlow<LocalDateTime?> = _lastBattleTime.asStateFlow()

    companion object {
        private const val TAG = "BattleScreenParser"
    }

    override suspend fun parseScreen(parsedScreen: ParsedScreen) {
        try {
            val isInBattle = isBattleScreen(parsedScreen.data)

            if (isInBattle && !_inBattle.value) {
                _inBattle.value = true
                _lastBattleTime.value = LocalDateTime.now()
                Log.d(TAG, "Entered battle")
            } else if (!isInBattle && _inBattle.value) {
                _inBattle.value = false
                Log.d(TAG, "Exited battle")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing battle screen", e)
        }
    }

    private fun isBattleScreen(screenData: List<com.lloir.ornaassistant.domain.model.ScreenData>): Boolean {
        return screenData.any { it.text == "Codex" } &&
                screenData.any { it.text == "SKILL" }
    }
}