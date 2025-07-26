package com.lloir.ornaassistant.utils

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.lloir.ornaassistant.domain.language.LanguageManager
import com.lloir.ornaassistant.domain.repository.ItemDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseDownloadManager @Inject constructor(
    private val itemDatabase: ItemDatabase,
    private val languageManager: LanguageManager
) {
    companion object {
        private const val TAG = "DatabaseDownloadManager"

        // Flag to indicate whether the database needs to be downloaded
        // This is used by MainActivity to show the download dialog
        var shouldPromptForDownload = false

        // Supported languages
        val SUPPORTED_LANGUAGES = mapOf(
            "en" to "English",
            "es" to "Spanish",
            "fr" to "French",
            "de" to "German",
            "it" to "Italian"
        )
    }

    /**
     * Show a dialog to prompt the user to select a database language and download it
     */
    fun promptForDatabaseDownload(context: Context) {
        val currentLanguage = languageManager.getDatabaseLanguage()

        // Create language options array
        val languageCodes = SUPPORTED_LANGUAGES.keys.toTypedArray()
        val languageNames = SUPPORTED_LANGUAGES.values.toTypedArray()

        // Find index of current language
        val currentIndex = languageCodes.indexOf(currentLanguage)

        AlertDialog.Builder(context)
            .setTitle("Select Database Language")
            .setMessage("Choose a language for the item database. The database will be downloaded from the server.")
            .setSingleChoiceItems(languageNames, currentIndex) { dialog, which ->
                val selectedLanguage = languageCodes[which]
                downloadDatabase(context, selectedLanguage)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Check if the database needs to be downloaded
     * This sets the shouldPromptForDownload flag if needed
     */
    suspend fun checkIfDatabaseNeeded(context: Context) {
        try {
            // Check if database files exist in internal storage
            val databaseDir = java.io.File(context.filesDir, "databases")
            val language = languageManager.getDatabaseLanguage()
            val databaseFile = java.io.File(databaseDir, "${language}_items.json")

            // Set the flag if the database file doesn't exist
            shouldPromptForDownload = !databaseFile.exists()

            Log.d(TAG, "Database check: shouldPromptForDownload = $shouldPromptForDownload")
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if database needed", e)
            // Default to true if there's an error
            shouldPromptForDownload = true
        }
    }

    /**
     * Download the database for the specified language
     */
    private fun downloadDatabase(context: Context, language: String) {
        // Show a toast to indicate download is starting
        Toast.makeText(context, "Downloading database for ${SUPPORTED_LANGUAGES[language]}...", Toast.LENGTH_SHORT).show()

        // Launch a coroutine to download the database
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = itemDatabase.downloadDatabase(context, language)

                withContext(Dispatchers.Main) {
                    if (success) {
                        // Update the language preference
                        languageManager.setDatabaseLanguage(language)

                        // Show success message
                        Toast.makeText(
                            context, 
                            "Database downloaded successfully!", 
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // Show error message
                        Toast.makeText(
                            context, 
                            "Failed to download database. Please try again later.", 
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading database", e)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context, 
                        "Error downloading database: ${e.message}", 
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
