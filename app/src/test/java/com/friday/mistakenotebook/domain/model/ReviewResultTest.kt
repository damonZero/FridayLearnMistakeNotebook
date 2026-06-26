package com.friday.mistakenotebook.domain.model

import org.junit.Assert.*
import org.junit.Test

class ReviewResultTest {

    @Test
    fun `correct result should have score 5`() {
        val result = ReviewResult(questionId = 1, isCorrect = true)

        assertEquals(1L, result.questionId)
        assertTrue(result.isCorrect)
        assertEquals(5, result.score)
    }

    @Test
    fun `incorrect result should have score 2`() {
        val result = ReviewResult(questionId = 1, isCorrect = false)

        assertEquals(1L, result.questionId)
        assertFalse(result.isCorrect)
        assertEquals(2, result.score)
    }

    @Test
    fun `custom score should be preserved`() {
        val result = ReviewResult(questionId = 1, isCorrect = true, score = 4)

        assertEquals(4, result.score)
    }

    @Test
    fun `copy should work correctly`() {
        val original = ReviewResult(questionId = 1, isCorrect = true, score = 5)
        val copy = original.copy(questionId = 2)

        assertEquals(2L, copy.questionId)
        assertTrue(copy.isCorrect)
        assertEquals(5, copy.score)
    }
}
