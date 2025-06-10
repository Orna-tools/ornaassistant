package com.lloir.ornaassistant.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.lloir.ornaassistant.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val SHOW_SESSION_OVERLAY = booleanPreferencesKey("show_session_overlay")
        val SHOW_INVITES_OVERLAY = booleanPreferencesKey("show_invites_overlay")
        val SHOW_ASSESS_OVERLAY = booleanPreferencesKey("show_assess_overlay")
        val WAYVESSEL_NOTIFICATIONS = booleanPreferencesKey("wayvessel_notifications")
        val NOTIFICATION_SOUNDS = booleanPreferencesKey("notification_sounds")
        val OVERLAY_TRANSPARENCY = floatPreferencesKey("overlay_transparency")
        val AUTO_HIDE_OVERLAYS = booleanPreferencesKey("auto_hide_overlays")
    }

    val settingsFlow: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            showSessionOverlay = preferences[PreferencesKeys.SHOW_SESSION_OVERLAY] ?: true,
            showInvitesOverlay = preferences[PreferencesKeys.SHOW_INVITES_OVERLAY] ?: true,
            showAssessOverlay = preferences[PreferencesKeys.SHOW_ASSESS_OVERLAY] ?: true,
            wayvesselNotifications = preferences[PreferencesKeys.WAYVESSEL_NOTIFICATIONS] ?: true,
            notificationSounds = preferences[PreferencesKeys.NOTIFICATION_SOUNDS] ?: true,
            overlayTransparency = preferences[PreferencesKeys.OVERLAY_TRANSPARENCY] ?: 0.8f,
            autoHideOverlays = preferences[PreferencesKeys.AUTO_HIDE_OVERLAYS] ?: false
        )
    }

    suspend fun getSettings(): AppSettings {
        return settingsFlow.first()
    }

    suspend fun updateSettings(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_SESSION_OVERLAY] = settings.showSessionOverlay
            preferences[PreferencesKeys.SHOW_INVITES_OVERLAY] = settings.showInvitesOverlay
            preferences[PreferencesKeys.SHOW_ASSESS_OVERLAY] = settings.showAssessOverlay
            preferences[PreferencesKeys.WAYVESSEL_NOTIFICATIONS] = settings.wayvesselNotifications
            preferences[PreferencesKeys.NOTIFICATION_SOUNDS] = settings.notificationSounds
            preferences[PreferencesKeys.OVERLAY_TRANSPARENCY] = settings.overlayTransparency
            preferences[PreferencesKeys.AUTO_HIDE_OVERLAYS] = settings.autoHideOverlays
        }
    }

    suspend fun updateSessionOverlay(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_SESSION_OVERLAY] = enabled
        }
    }

    suspend fun updateInvitesOverlay(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_INVITES_OVERLAY] = enabled
        }
    }

    suspend fun updateAssessOverlay(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_ASSESS_OVERLAY] = enabled
        }
    }

    suspend fun updateWayvesselNotifications(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.WAYVESSEL_NOTIFICATIONS] = enabled
        }
    }

    suspend fun updateOverlayTransparency(transparency: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.OVERLAY_TRANSPARENCY] = transparency
        }
    }
}