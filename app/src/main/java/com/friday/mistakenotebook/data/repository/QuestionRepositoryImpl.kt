package com.friday.mistakenotebook.data.repository

import com.friday.mistakenotebook.data.local.dao.QuestionDao
import com.friday.mistakenotebook.data.local.entity.ErrorType
import com.friday.mistakenotebook.data.local.entity.QuestionEntity
import com.friday.mistakenotebook.domain.algorithm.SpacedRepetitionAlgorithm
import com.friday.mistakenotebook.domain.model.Question
import com.friday.mistakenotebook.domain.model.ReviewResult
import com.friday.mistakenotebook.domain.repository.QuestionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class QuestionRepositoryImpl @Inject constructor(
    private val questionDao: QuestionDao
) : QuestionRepository {

    /**
     * 待复习相关查询的时间源：每分钟重发一次"明天 0 点"的截止时间。
     * 否则查询参数在订阅时固化，跨午夜后统计陈旧、当天新录入的错题也不出现。
     */
    private val dueClock = flow {
        while (true) {
            emit(endOfToday())
            delay(60_000)
        }
    }

    private fun endOfToday(): Long =
        LocalDate.now().plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

    override fun getAllQuestions(): Flow<List<Question>> {
        return questionDao.getAllQuestions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getQuestionsBySubject(subjectId: Long): Flow<List<Question>> {
        return questionDao.getQuestionsBySubject(subjectId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getQuestionsForReview(): Flow<List<Question>> {
        return dueClock.flatMapLatest { dueUntil ->
            questionDao.getQuestionsForReview(dueUntil).map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    override fun getTodayReviewCount(): Flow<Int> {
        return dueClock.flatMapLatest { dueUntil ->
            questionDao.getTodayReviewCount(dueUntil)
        }
    }

    override fun getBoxCounts(): Flow<Map<Int, Int>> {
        return questionDao.getBoxCounts().map { rows ->
            rows.associate { it.leitnerBox to it.count }
        }
    }

    override fun getTotalQuestionCount(): Flow<Int> {
        return questionDao.getTotalQuestionCount()
    }

    override fun getMasteredCount(): Flow<Int> {
        return questionDao.getMasteredCount()
    }

    override suspend fun getQuestionById(id: Long): Question? {
        return questionDao.getQuestionById(id)?.toDomain()
    }

    override suspend fun addQuestion(
        subjectId: Long,
        content: String,
        answer: String,
        userAnswer: String,
        errorType: ErrorType,
        chapterId: Long?,
        knowledgePointId: Long?,
        imagePath: String?
    ): Long {
        val entity = QuestionEntity(
            subjectId = subjectId,
            content = content,
            answer = answer,
            userAnswer = userAnswer,
            errorType = errorType,
            chapterId = chapterId,
            knowledgePointId = knowledgePointId,
            imagePath = imagePath
        )
        return questionDao.insertQuestion(entity)
    }

    override suspend fun updateQuestion(question: Question) {
        val entity = question.toEntity()
        questionDao.updateQuestion(entity)
    }

    override suspend fun deleteQuestion(id: Long) {
        questionDao.deleteQuestionById(id)
    }

    override suspend fun processReviewResult(result: ReviewResult) {
        val question = questionDao.getQuestionById(result.questionId) ?: return
        val updatedQuestion = SpacedRepetitionAlgorithm.calculateNextReview(
            question = question,
            score = result.score
        )
        questionDao.updateQuestion(updatedQuestion)
    }

    override fun searchQuestions(query: String): Flow<List<Question>> {
        return questionDao.searchQuestions(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private fun QuestionEntity.toDomain(): Question {
        return Question(
            id = id,
            subjectId = subjectId,
            chapterId = chapterId,
            knowledgePointId = knowledgePointId,
            content = content,
            answer = answer,
            userAnswer = userAnswer,
            errorType = errorType,
            imagePath = imagePath,
            leitnerBox = leitnerBox,
            easeFactor = easeFactor,
            intervalDays = intervalDays,
            streak = streak,
            reviewCount = reviewCount,
            nextReviewDate = nextReviewDate,
            lastReviewDate = lastReviewDate,
            createdAt = createdAt
        )
    }

    private fun Question.toEntity(): QuestionEntity {
        return QuestionEntity(
            id = id,
            subjectId = subjectId,
            chapterId = chapterId,
            knowledgePointId = knowledgePointId,
            content = content,
            answer = answer,
            userAnswer = userAnswer,
            errorType = errorType,
            imagePath = imagePath,
            leitnerBox = leitnerBox,
            easeFactor = easeFactor,
            intervalDays = intervalDays,
            streak = streak,
            reviewCount = reviewCount,
            nextReviewDate = nextReviewDate,
            lastReviewDate = lastReviewDate,
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }
}
