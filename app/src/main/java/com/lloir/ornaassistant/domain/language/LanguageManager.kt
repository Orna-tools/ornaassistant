package com.lloir.ornaassistant.domain.language

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageManager @Inject constructor(
    private val context: Context
) {
    companion object {
        const val PREF_APP_LANGUAGE = "app_language"
        const val PREF_DATABASE_LANGUAGE = "database_language"
        const val SYSTEM_LANGUAGE = "system"
    }

    fun getAppLanguage(): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(PREF_APP_LANGUAGE, SYSTEM_LANGUAGE) ?: SYSTEM_LANGUAGE
    }

    fun getDatabaseLanguage(): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(PREF_DATABASE_LANGUAGE, "en") ?: "en"
    }

    fun setDatabaseLanguage(language: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(PREF_DATABASE_LANGUAGE, language).apply()
    }

    fun applyAppLanguage(context: Context): Context {
        val language = getAppLanguage()
        if (language == SYSTEM_LANGUAGE) {
            return context
        }

        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}
