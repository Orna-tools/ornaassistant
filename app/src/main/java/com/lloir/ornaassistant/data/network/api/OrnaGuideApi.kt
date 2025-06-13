package com.lloir.ornaassistant.data.network.api

import com.lloir.ornaassistant.data.network.dto.AssessmentRequestDto
import com.lloir.ornaassistant.data.network.dto.AssessmentResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface OrnaGuideApi {

    @POST("api/assess/")
    suspend fun assessItem(@Body request: AssessmentRequestDto): AssessmentResponseDto

    companion object {
        const val BASE_URL = "https://api.orna.guide/"
    }
}