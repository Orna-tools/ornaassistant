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
