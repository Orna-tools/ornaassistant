package com.lloir.ornaassistant.data.backup

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Worker class for performing automatic backups in the background.
 */
class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "AutoBackupWorker"
    }

    @Inject
    lateinit var dataBackupService: DataBackupService

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting automatic backup")
            
            // Create the backup
            val backupUri = dataBackupService.createAutomaticBackup()
            
            return@withContext if (backupUri != null) {
                Log.d(TAG, "Automatic backup completed successfully: $backupUri")
                Result.success()
            } else {
                Log.e(TAG, "Automatic backup failed")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during automatic backup", e)
            return@withContext Result.failure()
        }
    }
}