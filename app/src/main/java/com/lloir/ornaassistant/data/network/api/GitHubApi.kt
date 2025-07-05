package com.lloir.ornaassistant.data.network.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface GitHubApi {

    @Headers("Accept: application/vnd.github.v3+json")
    @POST("repos/Orna-tools/OA-ISSUES/issues")
    suspend fun createIssue(@Body request: GitHubIssueRequest): Response<GitHubIssueResponse>

    companion object {
        const val BASE_URL = "https://api.github.com/"
    }
}

data class GitHubIssueRequest(
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String,
    @SerializedName("labels") val labels: List<String> = listOf("debug-log", "auto-generated")
)

data class GitHubIssueResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("number") val number: Int,
    @SerializedName("title") val title: String,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("state") val state: String
)

data class GitHubError(
    @SerializedName("message") val message: String,
    @SerializedName("errors") val errors: List<GitHubErrorDetail>? = null
)

data class GitHubErrorDetail(
    @SerializedName("resource") val resource: String,
    @SerializedName("field") val field: String,
    @SerializedName("code") val code: String
)
