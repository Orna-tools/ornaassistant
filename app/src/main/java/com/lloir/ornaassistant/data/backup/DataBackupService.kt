package com.lloir.ornaassistant.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.*
import com.lloir.ornaassistant.data.database.OrnaDatabase
import com.lloir.ornaassistant.domain.model.AppSettings
import com.lloir.ornaassistant.domain.model.BackupFrequency
import com.lloir.ornaassistant.domain.repository.DungeonRepository
import com.lloir.ornaassistant.domain.repository.ItemAssessmentRepository
import com.lloir.ornaassistant.domain.repository.KingdomRepository
import com.lloir.ornaassistant.domain.repository.MaterialRepository
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

/**
 * Service for handling backup and restore operations for app data.
 */
@Singleton
class DataBackupService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dungeonRepository: DungeonRepository,
    private val itemAssessmentRepository: ItemAssessmentRepository,
    private val materialRepository: MaterialRepository,
    private val kingdomRepository: KingdomRepository,
    private val settingsRepository: SettingsRepository,
    private val database: OrnaDatabase,
    private val workManager: WorkManager
) {
    companion object {
        private const val TAG = "DataBackupService"
        private const val AUTO_BACKUP_WORK_NAME = "auto_backup_work"
        private const val AUTO_BACKUP_FILE_PREFIX = "ornaassistant_backup_"
        private const val AUTO_BACKUP_FILE_EXTENSION = ".zip"
    }

    /**
     * Creates a backup of the app data and saves it to the specified URI.
     * 
     * @param uri The URI where the backup will be saved
     * @return Result indicating success or failure
     */
    suspend fun createBackup(uri: Uri): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Create a ZIP file containing:
            // 1. Database file
            // 2. Preferences (settings)
            // 3. Metadata (app version, backup date)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    // Add database file
                    val dbFile = context.getDatabasePath(OrnaDatabase.DATABASE_NAME)
                    if (dbFile.exists()) {
                        addFileToZip(zipOut, dbFile, "database.db")
                    } else {
                        Log.w(TAG, "Database file not found at ${dbFile.absolutePath}")
                    }

                    // Add preferences
                    val prefsFile = File(context.dataDir, "shared_prefs/app_settings.xml")
                    if (prefsFile.exists()) {
                        addFileToZip(zipOut, prefsFile, "preferences.xml")
                    } else {
                        Log.w(TAG, "Preferences file not found at ${prefsFile.absolutePath}")
                    }

                    // Add metadata
                    val metadata = createBackupMetadata()
                    addStringToZip(zipOut, metadata, "metadata.json")
                }
            }

            // Update last backup date in settings
            settingsRepository.updateLastBackupDate(LocalDateTime.now())

            return@withContext Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Restores app data from a backup file.
     * 
     * @param uri The URI of the backup file
     * @return Result indicating success or failure
     */
    suspend fun restoreBackup(uri: Uri): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Close database connection
            database.close()

            // Extract ZIP file
            val tempDir = File(context.cacheDir, "backup_restore")
            tempDir.mkdirs()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipIn ->
                    extractZipContents(zipIn, tempDir)
                }
            }

            // Validate backup metadata
            val metadataFile = File(tempDir, "metadata.json")
            if (!validateBackupMetadata(metadataFile)) {
                return@withContext Result.failure(Exception("Invalid backup file"))
            }

            // Restore database
            val dbFile = File(tempDir, "database.db")
            if (dbFile.exists()) {
                val destFile = context.getDatabasePath(OrnaDatabase.DATABASE_NAME)
                dbFile.copyTo(destFile, overwrite = true)
            }

            // Restore preferences
            val prefsFile = File(tempDir, "preferences.xml")
            if (prefsFile.exists()) {
                val destFile = File(context.dataDir, "shared_prefs/app_settings.xml")
                prefsFile.copyTo(destFile, overwrite = true)
            }

            // Clean up
            tempDir.deleteRecursively()

            return@withContext Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Creates metadata for the backup file.
     */
    private fun createBackupMetadata(): String {
        val metadata = JSONObject().apply {
            put("version", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
            put("timestamp", LocalDateTime.now().toString())
            put("device", android.os.Build.MODEL)
        }
        return metadata.toString()
    }

    /**
     * Validates the metadata of a backup file.
     */
    private fun validateBackupMetadata(metadataFile: File): Boolean {
        if (!metadataFile.exists()) {
            Log.e(TAG, "Metadata file not found")
            return false
        }

        try {
            val metadata = JSONObject(metadataFile.readText())
            val version = metadata.optString("version")
            val timestamp = metadata.optString("timestamp")

            // Basic validation - ensure required fields exist
            return version.isNotEmpty() && timestamp.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse metadata", e)
            return false
        }
    }

    /**
     * Adds a file to the ZIP archive.
     */
    private fun addFileToZip(zipOut: ZipOutputStream, file: File, entryName: String) {
        if (!file.exists()) {
            Log.w(TAG, "File does not exist: ${file.absolutePath}")
            return
        }

        zipOut.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { input ->
            input.copyTo(zipOut)
        }
        zipOut.closeEntry()
    }

    /**
     * Adds a string to the ZIP archive.
     */
    private fun addStringToZip(zipOut: ZipOutputStream, content: String, entryName: String) {
        zipOut.putNextEntry(ZipEntry(entryName))
        zipOut.write(content.toByteArray())
        zipOut.closeEntry()
    }

    /**
     * Extracts the contents of a ZIP file.
     */
    private fun extractZipContents(zipIn: ZipInputStream, destDir: File) {
        var entry = zipIn.nextEntry
        while (entry != null) {
            val file = File(destDir, entry.name)

            if (entry.isDirectory) {
                file.mkdirs()
            } else {
                file.parentFile?.mkdirs()
                file.outputStream().use { output ->
                    zipIn.copyTo(output)
                }
            }

            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }
    }

    /**
     * Schedules automatic backups based on user settings.
     */
    suspend fun scheduleAutomaticBackups() {
        val settings = settingsRepository.getSettingsFlow().first()

        // Cancel any existing work
        workManager.cancelUniqueWork(AUTO_BACKUP_WORK_NAME)

        // If auto backup is not enabled, we're done
        if (!settings.autoBackupEnabled || settings.autoBackupFrequency == BackupFrequency.NEVER) {
            Log.d(TAG, "Automatic backups are disabled")
            return
        }

        // Calculate the interval based on frequency
        val repeatInterval = when (settings.autoBackupFrequency) {
            BackupFrequency.DAILY -> 24L
            BackupFrequency.WEEKLY -> 7 * 24L
            BackupFrequency.MONTHLY -> 30 * 24L
            else -> return // NEVER case, already handled above
        }

        // Create constraints - only run when device is charging and on unmetered network
        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiresBatteryNotLow(true)
            .build()

        // Create the work request
        val backupWorkRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            repeatInterval, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        // Enqueue the work
        workManager.enqueueUniquePeriodicWork(
            AUTO_BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            backupWorkRequest
        )

        Log.d(TAG, "Scheduled automatic backups with frequency: ${settings.autoBackupFrequency}")
    }

    /**
     * Creates an automatic backup in the app's backup directory.
     * Returns the URI of the created backup file, or null if the backup failed.
     */
    suspend fun createAutomaticBackup(): Uri? {
        try {
            // Create backup directory if it doesn't exist
            val backupDir = File(context.filesDir, "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            // Create a new backup file
            val timestamp = LocalDateTime.now().toString().replace(":", "-")
            val backupFile = File(backupDir, "$AUTO_BACKUP_FILE_PREFIX$timestamp$AUTO_BACKUP_FILE_EXTENSION")

            // Create the backup
            ZipOutputStream(backupFile.outputStream()).use { zipOut ->
                // Add database file
                val dbFile = context.getDatabasePath(OrnaDatabase.DATABASE_NAME)
                if (dbFile.exists()) {
                    addFileToZip(zipOut, dbFile, "database.db")
                }

                // Add preferences
                val prefsFile = File(context.dataDir, "shared_prefs/app_settings.xml")
                if (prefsFile.exists()) {
                    addFileToZip(zipOut, prefsFile, "preferences.xml")
                }

                // Add metadata
                val metadata = createBackupMetadata()
                addStringToZip(zipOut, metadata, "metadata.json")
            }

            // Update last backup date in settings
            settingsRepository.updateLastBackupDate(LocalDateTime.now())

            // Manage backup retention
            manageBackupRetention()

            return Uri.fromFile(backupFile)
        } catch (e: Exception) {
            Log.e(TAG, "Automatic backup failed", e)
            return null
        }
    }

    /**
     * Manages backup retention by deleting old backups based on user settings.
     */
    private suspend fun manageBackupRetention() {
        val settings = settingsRepository.getSettingsFlow().first()
        val maxBackups = settings.autoBackupRetention

        if (maxBackups <= 0) {
            return // Keep all backups
        }

        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists() || !backupDir.isDirectory) {
            return
        }

        // Get all backup files sorted by last modified time (newest first)
        val backupFiles = backupDir.listFiles { file ->
            file.name.startsWith(AUTO_BACKUP_FILE_PREFIX) && 
            file.name.endsWith(AUTO_BACKUP_FILE_EXTENSION)
        }?.sortedByDescending { it.lastModified() } ?: return

        // Delete excess backups
        if (backupFiles.size > maxBackups) {
            backupFiles.drop(maxBackups).forEach { file ->
                try {
                    if (file.delete()) {
                        Log.d(TAG, "Deleted old backup: ${file.name}")
                    } else {
                        Log.w(TAG, "Failed to delete old backup: ${file.name}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting old backup: ${file.name}", e)
                }
            }
        }
    }

    /**
     * Gets a list of all available automatic backups.
     */
    suspend fun getAvailableBackups(): List<BackupInfo> {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists() || !backupDir.isDirectory) {
            return emptyList()
        }

        return backupDir.listFiles { file ->
            file.name.startsWith(AUTO_BACKUP_FILE_PREFIX) && 
            file.name.endsWith(AUTO_BACKUP_FILE_EXTENSION)
        }?.sortedByDescending { it.lastModified() }?.map { file ->
            BackupInfo(
                uri = Uri.fromFile(file),
                filename = file.name,
                timestamp = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(file.lastModified()),
                    ZoneId.systemDefault()
                ),
                size = file.length()
            )
        } ?: emptyList()
    }

    /**
     * Data class representing backup file information.
     */
    data class BackupInfo(
        val uri: Uri,
        val filename: String,
        val timestamp: LocalDateTime,
        val size: Long
    ) {
        val formattedSize: String
            get() = when {
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> "${size / 1024} KB"
                else -> "${size / (1024 * 1024)} MB"
            }

        val formattedDate: String
            get() = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }
}
