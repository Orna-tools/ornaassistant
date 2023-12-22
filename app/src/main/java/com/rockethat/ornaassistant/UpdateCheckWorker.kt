package com.rockethat.ornaassistant

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class UpdateCheckWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val isSuccess = UpdateChecker.checkForUpdates(applicationContext)
        return if (isSuccess) Result.success() else Result.retry()
    }
}