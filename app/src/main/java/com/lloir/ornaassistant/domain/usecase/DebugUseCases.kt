package com.lloir.ornaassistant.domain.usecase

import android.util.Log
import com.google.gson.Gson
import com.lloir.ornaassistant.BuildConfig
import com.lloir.ornaassistant.data.network.api.GitHubApi
import com.lloir.ornaassistant.data.network.api.GitHubError
import com.lloir.ornaassistant.data.network.api.GitHubIssueRequest
import com.lloir.ornaassistant.utils.LogCollector
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendDebugLogsUseCase @Inject constructor(
    private val logCollector: LogCollector,
    private val gitHubApi: GitHubApi
) {
    companion object {
        private const val TAG = "SendDebugLogsUseCase"
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
            // Check if we have a valid token (not debug/placeholder)
            if (BuildConfig.GITHUB_TOKEN == "debug_token_disabled" || 
                BuildConfig.GITHUB_TOKEN == "github_pat_your_token_here") {
                return Result.Error("Debug log submission is not available in this build")
            }

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
                appendLine("**App Version:** ${BuildConfig.VERSION_NAME}")
                appendLine("**Build Type:** ${BuildConfig.BUILD_TYPE}")
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
            val response = gitHubApi.createIssue(
                owner = BuildConfig.GITHUB_REPO_OWNER,
                repo = BuildConfig.GITHUB_REPO_NAME,
                request = request
            )
            
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
