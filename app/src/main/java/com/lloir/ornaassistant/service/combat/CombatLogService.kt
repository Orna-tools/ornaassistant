package com.lloir.ornaassistant.service.combat

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.lloir.ornaassistant.domain.model.*
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CombatLogService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val _combatLog = MutableStateFlow<List<CombatLogEntry>>(emptyList())
    val combatLog: StateFlow<List<CombatLogEntry>> = _combatLog.asStateFlow()

    private val _recentEntries = MutableStateFlow<List<CombatLogEntry>>(emptyList())
    val recentEntries: StateFlow<List<CombatLogEntry>> = _recentEntries.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    init {
        initializeTTS()
    }

    private fun initializeTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    val result = engine.setLanguage(Locale.US)
                    isTtsInitialized = result != TextToSpeech.LANG_MISSING_DATA && 
                                     result != TextToSpeech.LANG_NOT_SUPPORTED
                    
                    engine.setSpeechRate(1.2f)
                    
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) {}
                        override fun onError(utteranceId: String?) {}
                    })
                }
            }
        }
    }

    suspend fun addEntry(entry: CombatLogEntry) {
        val currentLog = _combatLog.value.toMutableList()
        currentLog.add(0, entry)
        if (currentLog.size > 1000) {
            currentLog.removeLast()
        }
        _combatLog.value = currentLog

        val recent = currentLog.take(10)
        _recentEntries.value = recent

        val settings = settingsRepository.getSettings()
        if (settings.enableVoiceAnnouncements && isTtsInitialized) {
            announceEntry(entry, settings)
        }
    }

    private fun announceEntry(entry: CombatLogEntry, settings: AppSettings) {
        val announcement = when (entry.type) {
            LogType.ORNATE_FOUND -> {
                if (settings.announceOrnates) {
                    "Ornate ${entry.message}, quality ${String.format("%.2f", entry.itemQuality ?: 0.0)}"
                } else null
            }
            
            LogType.GODFORGE_FOUND -> {
                if (settings.announceGodforges) {
                    "Godforge! ${entry.message}"
                } else null
            }
            
            LogType.LEVEL_UP -> {
                if (settings.announceLevelUp) {
                    "Level up! ${entry.message}"
                } else null
            }
            
            LogType.DUNGEON_COMPLETE -> {
                if (settings.announceDungeonComplete) {
                    "Dungeon complete! ${entry.message}"
                } else null
            }
            
            else -> null
        }

        announcement?.let { text ->
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, entry.id.toString())
        }
    }

    fun clearLog() {
        _combatLog.value = emptyList()
        _recentEntries.value = emptyList()
    }

    fun cleanup() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
