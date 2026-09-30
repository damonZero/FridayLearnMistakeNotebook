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

    /** 莱特纳盒子分布：key=盒子 1..5，value=题目数（统计页真实分布用） */
    fun getBoxCounts(): Flow<Map<Int, Int>>

    /** 单科盒子分布（科目概况页用） */
    fun getBoxCountsBySubject(subjectId: Long): Flow<Map<Int, Int>>

    /** 单科今日待复习数（当天到期即算，随分钟时钟刷新） */
    fun getTodayDueCountBySubject(subjectId: Long): Flow<Int>

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

    /** 只更新 AI 分析摘要列（详情页分析结果落库用） */
    suspend fun updateAiAnalysis(id: Long, aiAnalysis: String?)

    suspend fun deleteQuestion(id: Long)

    suspend fun processReviewResult(result: ReviewResult)

    fun searchQuestions(query: String): Flow<List<Question>>
}
