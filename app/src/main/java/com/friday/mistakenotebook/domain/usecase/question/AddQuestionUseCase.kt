package com.friday.mistakenotebook.domain.usecase.question

import com.friday.mistakenotebook.data.local.entity.ErrorType
import com.friday.mistakenotebook.domain.repository.QuestionRepository
import javax.inject.Inject

class AddQuestionUseCase @Inject constructor(
    private val questionRepository: QuestionRepository
) {
    suspend operator fun invoke(
        subjectId: Long,
        content: String,
        answer: String,
        userAnswer: String,
        errorType: ErrorType,
        chapterId: Long? = null,
        knowledgePointId: Long? = null,
        imagePath: String? = null
    ): Long {
        require(content.isNotBlank()) { "题目内容不能为空" }
        return questionRepository.addQuestion(
            subjectId = subjectId,
            content = content,
            answer = answer,
            userAnswer = userAnswer,
            errorType = errorType,
            chapterId = chapterId,
            knowledgePointId = knowledgePointId,
            imagePath = imagePath
        )
    }
}
