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
        attributes: Map<String, Int>
    ): AssessmentResult {
        val result = itemAssessmentRepository.assessItem(itemName, level, attributes)

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
