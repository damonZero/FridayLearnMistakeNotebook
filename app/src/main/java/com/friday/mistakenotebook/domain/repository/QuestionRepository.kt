package com.friday.mistakenotebook.domain.repository

import com.friday.mistakenotebook.data.local.entity.ErrorType
import com.friday.mistakenotebook.domain.model.Question
import com.friday.mistakenotebook.domain.model.ReviewResult
import kotlinx.coroutines.flow.Flow

interface QuestionRepository {

    fun getAllQuestions(): Flow<List<Question>>

    fun getQuestionsBySubject(subjectId: Long): Flow<List<Question>>

    fun getQuestionsForReview(): Flow<List<Question>>

    fun getTodayReviewCount(): Flow<Int>

    fun getTotalQuestionCount(): Flow<Int>

    fun getMasteredCount(): Flow<Int>

    suspend fun getQuestionById(id: Long): Question?

    suspend fun addQuestion(
        subjectId: Long,
        content: String,
        answer: String,
        userAnswer: String,
        errorType: ErrorType,
        chapterId: Long? = null,
        knowledgePointId: Long? = null,
        imagePath: String? = null
    ): Long

    suspend fun updateQuestion(question: Question)

    suspend fun deleteQuestion(id: Long)

    suspend fun processReviewResult(result: ReviewResult)

    fun searchQuestions(query: String): Flow<List<Question>>
}
