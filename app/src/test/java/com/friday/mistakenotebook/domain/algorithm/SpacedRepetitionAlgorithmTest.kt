package com.friday.mistakenotebook.domain.algorithm

import com.friday.mistakenotebook.data.local.entity.ErrorType
import com.friday.mistakenotebook.data.local.entity.QuestionEntity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SpacedRepetitionAlgorithmTest {

    private lateinit var testQuestion: QuestionEntity

    @Before
    fun setup() {
        testQuestion = QuestionEntity(
            id = 1,
            subjectId = 1,
            content = "1 + 1 = ?",
            answer = "2",
            userAnswer = "3",
            errorType = ErrorType.CALCULATION,
            leitnerBox = 1,
            easeFactor = 2.5f,
            intervalDays = 1,
            streak = 0,
            reviewCount = 0,
            nextReviewDate = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
    }

    @Test
    fun `calculateNextReview with correct answer should increase leitner box`() {
        val result = SpacedRepetitionAlgorithm.calculateNextReview(testQuestion, score = 5)

        assertEquals(2, result.leitnerBox)
        assertEquals(1, result.streak)
        assertEquals(1, result.reviewCount)
        assertTrue(result.nextReviewDate > System.currentTimeMillis())
    }

    @Test
    fun `calculateNextReview with incorrect answer should reset to box 1 and interval one day`() {
        val questionInBox3 = testQuestion.copy(leitnerBox = 3, streak = 2, easeFactor = 2.5f)
        val before = System.currentTimeMillis()
        val result = SpacedRepetitionAlgorithm.calculateNextReview(questionInBox3, score = 1)

        assertEquals(1, result.leitnerBox)
        assertEquals(0, result.streak)
        // 答错不乘 EF，固定间隔 1 天，保证次日必现
        assertEquals(1, result.intervalDays)
        val oneDay = 24 * 60 * 60 * 1000L
        assertTrue(result.nextReviewDate - before in (oneDay - 1000)..(oneDay + 1000))
    }

    @Test
    fun `calculateNextReview should not exceed box 5`() {
        val questionInBox5 = testQuestion.copy(leitnerBox = 5, streak = 10)
        val result = SpacedRepetitionAlgorithm.calculateNextReview(questionInBox5, score = 5)

        assertEquals(5, result.leitnerBox)
        assertEquals(11, result.streak)
    }

    @Test
    fun `calculateNextReview score 3 is considered correct`() {
        val result = SpacedRepetitionAlgorithm.calculateNextReview(testQuestion, score = 3)

        assertEquals(2, result.leitnerBox)
        assertEquals(1, result.streak)
    }

    @Test
    fun `calculateNextReview score 2 is considered incorrect`() {
        val result = SpacedRepetitionAlgorithm.calculateNextReview(testQuestion, score = 2)

        assertEquals(1, result.leitnerBox)
        assertEquals(0, result.streak)
    }

    @Test
    fun `score 0 should be clamped to 1 and treated as incorrect`() {
        val result = SpacedRepetitionAlgorithm.calculateNextReview(testQuestion, score = 0)

        assertEquals(1, result.leitnerBox)
        assertEquals(0, result.streak)
        assertEquals(1, result.intervalDays)
    }

    @Test
    fun `score 6 should be clamped to 5 and treated as correct`() {
        val result = SpacedRepetitionAlgorithm.calculateNextReview(testQuestion, score = 6)

        assertEquals(2, result.leitnerBox)
        assertEquals(1, result.streak)
    }

    @Test
    fun `ease factor should decrease with low score`() {
        val result = SpacedRepetitionAlgorithm.calculateNextReview(testQuestion, score = 1)

        assertTrue(result.easeFactor < testQuestion.easeFactor)
        assertTrue(result.easeFactor >= 1.3f) // 最小值
    }

    @Test
    fun `ease factor should increase with high score when below cap`() {
        val midEFQuestion = testQuestion.copy(easeFactor = 2.0f)
        val result = SpacedRepetitionAlgorithm.calculateNextReview(midEFQuestion, score = 5)

        assertEquals(2.1f, result.easeFactor, 0.0001f)
    }

    @Test
    fun `ease factor should be capped at 2_5`() {
        val maxEFQuestion = testQuestion.copy(easeFactor = 2.5f)
        val result = SpacedRepetitionAlgorithm.calculateNextReview(maxEFQuestion, score = 5)

        // 2.5 + 0.1 被上限截住，不再无限膨胀
        assertEquals(2.5f, result.easeFactor, 0.0001f)
    }

    @Test
    fun `ease factor should not go below 1_3`() {
        val lowEFQuestion = testQuestion.copy(easeFactor = 1.3f)
        val result = SpacedRepetitionAlgorithm.calculateNextReview(lowEFQuestion, score = 1)

        assertEquals(1.3f, result.easeFactor, 0.0001f)
    }

    @Test
    fun `interval days should be at least 1`() {
        val result = SpacedRepetitionAlgorithm.calculateNextReview(testQuestion, score = 1)

        assertTrue(result.intervalDays >= 1)
    }

    @Test
    fun `correct answer interval should increase with box`() {
        var question = testQuestion
        val intervals = mutableListOf<Int>()

        // 连续答对：盒1 → 盒2 → 盒3 → 盒4 → 盒5
        for (i in 1..4) {
            question = SpacedRepetitionAlgorithm.calculateNextReview(question, score = 5)
            intervals.add(question.intervalDays)
        }

        assertEquals(5, intervals[0])   // round(2 × 2.5)
        assertEquals(10, intervals[1])  // round(4 × 2.5)
        assertEquals(18, intervals[2])  // round(7 × 2.5)
        assertEquals(38, intervals[3])  // round(15 × 2.5)
        assertTrue(intervals[0] < intervals[1])
        assertTrue(intervals[1] < intervals[2])
        assertTrue(intervals[2] < intervals[3])
    }

    @Test
    fun `getBoxDescription should return correct descriptions`() {
        assertEquals("新题/答错", SpacedRepetitionAlgorithm.getBoxDescription(1))
        assertEquals("初步掌握", SpacedRepetitionAlgorithm.getBoxDescription(2))
        assertEquals("基本掌握", SpacedRepetitionAlgorithm.getBoxDescription(3))
        assertEquals("熟练掌握", SpacedRepetitionAlgorithm.getBoxDescription(4))
        assertEquals("完全掌握", SpacedRepetitionAlgorithm.getBoxDescription(5))
        assertEquals("未知", SpacedRepetitionAlgorithm.getBoxDescription(6))
    }

    @Test
    fun `getMasteryPercentage should return correct percentages`() {
        assertEquals(0, SpacedRepetitionAlgorithm.getMasteryPercentage(1))
        assertEquals(25, SpacedRepetitionAlgorithm.getMasteryPercentage(2))
        assertEquals(50, SpacedRepetitionAlgorithm.getMasteryPercentage(3))
        assertEquals(75, SpacedRepetitionAlgorithm.getMasteryPercentage(4))
        assertEquals(100, SpacedRepetitionAlgorithm.getMasteryPercentage(5))
        assertEquals(0, SpacedRepetitionAlgorithm.getMasteryPercentage(6))
    }

    @Test
    fun `review count should increment`() {
        val result = SpacedRepetitionAlgorithm.calculateNextReview(testQuestion, score = 4)

        assertEquals(1, result.reviewCount)

        val result2 = SpacedRepetitionAlgorithm.calculateNextReview(result, score = 4)
        assertEquals(2, result2.reviewCount)
    }

    @Test
    fun `last review date should be updated`() {
        val before = System.currentTimeMillis()
        val result = SpacedRepetitionAlgorithm.calculateNextReview(testQuestion, score = 4)
        val after = System.currentTimeMillis()

        assertNotNull(result.lastReviewDate)
        assertTrue(result.lastReviewDate!! >= before)
        assertTrue(result.lastReviewDate!! <= after)
    }

    @Test
    fun `consecutive correct answers should increase streak`() {
        var question = testQuestion

        for (i in 1..5) {
            question = SpacedRepetitionAlgorithm.calculateNextReview(question, score = 5)
            assertEquals(i, question.streak)
        }
    }

    @Test
    fun `incorrect answer should reset streak`() {
        val questionWithStreak = testQuestion.copy(streak = 5)
        val result = SpacedRepetitionAlgorithm.calculateNextReview(questionWithStreak, score = 1)

        assertEquals(0, result.streak)
    }
}
