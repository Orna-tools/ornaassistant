package com.lloir.ornaassistant.domain.usecase

import android.util.Log
import com.google.gson.Gson
import com.lloir.ornaassistant.data.network.api.GitHubApi
import com.lloir.ornaassistant.data.network.api.GitHubError
import com.lloir.ornaassistant.data.network.api.GitHubIssueRequest
import com.lloir.ornaassistant.utils.LogCollector
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugUseCase @Inject constructor(
    private val logCollector: LogCollector,
    private val gitHubApi: GitHubApi
) {
    companion object {
        private const val TAG = "DebugUseCase"
    }

    sealed class Result {
        data class Success(val issueNumber: Int, val issueUrl: String) : Result()
        data class Error(val message: String) : Result()
    }

    suspend operator fun invoke(
        userDescription: String,
        userEmail: String? = null
    ): Result {
        return try {
            Log.d(TAG, "Collecting logs for debug submission...")

            // Collect logs
            val logs = logCollector.collectLogs()

            // Create issue title with timestamp
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val title = "Debug Log Submission - $timestamp"

            // Create issue body with proper formatting
            val body = buildString {
                appendLine("## User-Reported Issue")
                appendLine()
                appendLine("**User Description:**")
                appendLine(userDescription.trim())
                appendLine()

                if (!userEmail.isNullOrBlank()) {
                    appendLine("**Contact:** $userEmail")
                    appendLine()
                }

                appendLine("**Submission Time:** $timestamp")
                appendLine("**App Version:** Unknown")
                appendLine()
                appendLine("---")
                appendLine()
                appendLine("## Debug Logs")
                appendLine()
                appendLine("```")
                appendLine(logs)
                appendLine("```")
            }

            val request = GitHubIssueRequest(title = title, body = body)
            val response = gitHubApi.createIssue(request)

            if (response.isSuccessful) {
                val issue = response.body()!!
                Log.i(TAG, "Successfully created GitHub issue #${issue.number}")
                Result.Success(issue.number, issue.htmlUrl)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val gitHubError = Gson().fromJson(errorBody, GitHubError::class.java)
                    "GitHub API Error: ${gitHubError.message}"
                } catch (e: Exception) {
                    "HTTP ${response.code()}: ${response.message()}"
                }
                Log.e(TAG, "Failed to create GitHub issue: $errorMessage")
                Result.Error("Failed to submit logs: $errorMessage")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while submitting debug logs", e)
            Result.Error("Failed to submit logs: ${e.message}")
        }
    }
}



