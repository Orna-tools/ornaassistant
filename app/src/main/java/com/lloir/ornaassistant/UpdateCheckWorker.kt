package com.lloir.ornaassistant

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            withContext(Dispatchers.IO) {
                UpdateChecker.checkForUpdates(applicationContext)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
