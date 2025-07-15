package com.lloir.ornaassistant.data.retention

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lloir.ornaassistant.domain.repository.DungeonRepository
import com.lloir.ornaassistant.domain.repository.ItemAssessmentRepository
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing data retention policies and pruning old data.
 */
@Singleton
class DataRetentionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dungeonRepository: DungeonRepository,
    private val itemAssessmentRepository: ItemAssessmentRepository,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val TAG = "DataRetentionService"
        private const val DATA_RETENTION_WORK_NAME = "data_retention_job"
    }

    /**
     * Schedules a periodic job to prune old data according to retention settings.
     */
    suspend fun scheduleDataRetentionJob() {
        // Create constraints to ensure the job doesn't run when the battery is low
        val constraints = androidx.work.Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val retentionWorkRequest = PeriodicWorkRequestBuilder<DataRetentionWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DATA_RETENTION_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            retentionWorkRequest
        )

        Log.d(TAG, "Scheduled data retention job")
    }

    /**
     * Prunes old data according to retention settings.
     * 
     * @return true if pruning was successful, false otherwise
     */
    suspend fun pruneOldData(): Boolean = withContext(Dispatchers.IO) {
        try {
            val settings = settingsRepository.getSettings()
            var prunedAnyData = false

            // Prune dungeon data
            if (settings.dungeonDataRetentionDays > 0) {
                val cutoffDate = LocalDateTime.now().minusDays(settings.dungeonDataRetentionDays.toLong())
                val oldVisits = dungeonRepository.getVisitsBetween(
                    LocalDateTime.MIN, 
                    cutoffDate
                )

                Log.d(TAG, "Found ${oldVisits.size} dungeon visits older than ${settings.dungeonDataRetentionDays} days")

                for (visit in oldVisits) {
                    dungeonRepository.deleteVisit(visit)
                    prunedAnyData = true
                }

                Log.d(TAG, "Pruned ${oldVisits.size} old dungeon visits")
            }

            // Prune assessment data
            if (settings.assessmentDataRetentionDays > 0) {
                itemAssessmentRepository.deleteOldAssessments(settings.assessmentDataRetentionDays)
                // Since we don't know how many were deleted, we'll assume some were
                prunedAnyData = true
                Log.d(TAG, "Pruned old item assessments older than ${settings.assessmentDataRetentionDays} days")
            }

            return@withContext prunedAnyData
        } catch (e: Exception) {
            Log.e(TAG, "Error pruning old data", e)
            return@withContext false
        }
    }
}

/**
 * Worker class for background execution of data retention tasks.
 */
class DataRetentionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @Inject
    lateinit var dataRetentionService: DataRetentionService

    override suspend fun doWork(): Result {
        return try {
            val prunedData = dataRetentionService.pruneOldData()
            if (prunedData) {
                Log.d("DataRetentionWorker", "Successfully pruned old data")
            } else {
                Log.d("DataRetentionWorker", "No data needed pruning")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("DataRetentionWorker", "Error pruning old data", e)
            Result.failure()
        }
    }
}
