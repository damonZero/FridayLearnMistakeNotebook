package com.friday.mistakenotebook.domain.usecase.question

import com.friday.mistakenotebook.domain.model.Question
import com.friday.mistakenotebook.domain.repository.QuestionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetQuestionsForReviewUseCase @Inject constructor(
    private val questionRepository: QuestionRepository
) {
    operator fun invoke(): Flow<List<Question>> {
        return questionRepository.getQuestionsForReview()
    }
}
