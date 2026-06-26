package com.friday.mistakenotebook.domain.usecase.question

import com.friday.mistakenotebook.domain.model.ReviewResult
import com.friday.mistakenotebook.domain.repository.QuestionRepository
import javax.inject.Inject

class ProcessReviewResultUseCase @Inject constructor(
    private val questionRepository: QuestionRepository
) {
    suspend operator fun invoke(result: ReviewResult) {
        questionRepository.processReviewResult(result)
    }
}
