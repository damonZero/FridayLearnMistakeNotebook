package com.friday.mistakenotebook.domain.usecase

import com.friday.mistakenotebook.data.local.entity.ErrorType
import com.friday.mistakenotebook.domain.repository.QuestionRepository
import com.friday.mistakenotebook.domain.usecase.question.AddQuestionUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AddQuestionUseCaseTest {

    private lateinit var questionRepository: FakeQuestionRepository
    private lateinit var addQuestionUseCase: AddQuestionUseCase

    @Before
    fun setup() {
        questionRepository = FakeQuestionRepository()
        addQuestionUseCase = AddQuestionUseCase(questionRepository)
    }

    @Test
    fun `invoke should call repository addQuestion`() = runTest {
        val result = addQuestionUseCase(
            subjectId = 1,
            content = "1 + 1 = ?",
            answer = "2",
            userAnswer = "3",
            errorType = ErrorType.CALCULATION
        )

        assertEquals(1L, result)
        assertEquals(1, questionRepository.addQuestionCallCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke should throw exception for blank content`() = runTest {
        addQuestionUseCase(
            subjectId = 1,
            content = "",
            answer = "2",
            userAnswer = "3",
            errorType = ErrorType.CALCULATION
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke should throw exception for whitespace content`() = runTest {
        addQuestionUseCase(
            subjectId = 1,
            content = "   ",
            answer = "2",
            userAnswer = "3",
            errorType = ErrorType.CALCULATION
        )
    }

    @Test
    fun `invoke should pass optional parameters`() = runTest {
        addQuestionUseCase(
            subjectId = 1,
            content = "Test",
            answer = "Answer",
            userAnswer = "User",
            errorType = ErrorType.CONCEPTUAL,
            chapterId = 10,
            knowledgePointId = 20,
            imagePath = "/path/to/image"
        )

        assertEquals(1, questionRepository.addQuestionCallCount)
        assertEquals(10L, questionRepository.lastChapterId)
        assertEquals(20L, questionRepository.lastKnowledgePointId)
        assertEquals("/path/to/image", questionRepository.lastImagePath)
    }
}

class FakeQuestionRepository : QuestionRepository {
    var addQuestionCallCount = 0
    var lastChapterId: Long? = null
    var lastKnowledgePointId: Long? = null
    var lastImagePath: String? = null

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
        addQuestionCallCount++
        lastChapterId = chapterId
        lastKnowledgePointId = knowledgePointId
        lastImagePath = imagePath
        return 1L
    }

    // 其他方法暂时不实现
    override fun getAllQuestions(): kotlinx.coroutines.flow.Flow<List<com.friday.mistakenotebook.domain.model.Question>> = kotlinx.coroutines.flow.flowOf(emptyList())
    override fun getQuestionsBySubject(subjectId: Long): kotlinx.coroutines.flow.Flow<List<com.friday.mistakenotebook.domain.model.Question>> = kotlinx.coroutines.flow.flowOf(emptyList())
    override fun getQuestionsForReview(): kotlinx.coroutines.flow.Flow<List<com.friday.mistakenotebook.domain.model.Question>> = kotlinx.coroutines.flow.flowOf(emptyList())
    override fun getTodayReviewCount(): kotlinx.coroutines.flow.Flow<Int> = kotlinx.coroutines.flow.flowOf(0)
    override fun getTotalQuestionCount(): kotlinx.coroutines.flow.Flow<Int> = kotlinx.coroutines.flow.flowOf(0)
    override fun getMasteredCount(): kotlinx.coroutines.flow.Flow<Int> = kotlinx.coroutines.flow.flowOf(0)
    override fun getBoxCounts(): kotlinx.coroutines.flow.Flow<Map<Int, Int>> = kotlinx.coroutines.flow.flowOf(emptyMap())
    override suspend fun getQuestionById(id: Long): com.friday.mistakenotebook.domain.model.Question? = null
    override suspend fun updateQuestion(question: com.friday.mistakenotebook.domain.model.Question) {}
    override suspend fun updateAiAnalysis(id: Long, aiAnalysis: String?) {}
    override suspend fun deleteQuestion(id: Long) {}
    override suspend fun processReviewResult(result: com.friday.mistakenotebook.domain.model.ReviewResult) {}
    override fun searchQuestions(query: String): kotlinx.coroutines.flow.Flow<List<com.friday.mistakenotebook.domain.model.Question>> = kotlinx.coroutines.flow.flowOf(emptyList())
}
