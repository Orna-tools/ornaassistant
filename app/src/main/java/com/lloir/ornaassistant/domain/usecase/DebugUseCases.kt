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
            Log.d(TAG, "Collecting logs for debug submission...")
            
            // Collect logs
            val logs = logCollector.collectLogs()
            
            // Create issue title
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val title = "Debug Log Submission - $timestamp"
            
            // Create issue body
            val body = buildString {
                appendLine("## User-Reported Issue")
                appendLine()
                appendLine("**User Description:**")
                appendLine(userDescription)
                appendLine()
                
                if (!userEmail.isNullOrBlank()) {
                    appendLine("**Contact:** $userEmail")
                    appendLine()
                }
                
                appendLine("**Submission Time:** $timestamp")
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
