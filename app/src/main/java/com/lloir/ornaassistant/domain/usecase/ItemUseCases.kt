package com.lloir.ornaassistant.domain.usecase

import com.lloir.ornaassistant.domain.model.*
import com.lloir.ornaassistant.domain.repository.*
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssessItemUseCase @Inject constructor(
    private val itemAssessmentRepository: ItemAssessmentRepository
) {
    suspend operator fun invoke(
        itemName: String,
        level: Int,
        attributes: Map<String, Int>,
        originalItemName: String = itemName,
        adornmentValues: Map<String, Int> = emptyMap()
    ): AssessmentResult {
        val result = itemAssessmentRepository.assessItem(
            itemName = itemName,
            level = level,
            attributes = attributes,
            originalItemName = originalItemName,
            adornmentValues = adornmentValues
        )

        // Save assessment to database
        val assessment = ItemAssessment(
            itemName = itemName,
            level = level,
            attributes = attributes,
            assessmentResult = result,
            timestamp = LocalDateTime.now(),
            quality = result.quality
        )

        itemAssessmentRepository.insertAssessment(assessment)

        return result
    }
}
