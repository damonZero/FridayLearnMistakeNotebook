package com.friday.mistakenotebook.data.local.entity

import org.junit.Assert.*
import org.junit.Test

class QuestionEntityTest {

    @Test
    fun `default values should be correct`() {
        val question = QuestionEntity(
            subjectId = 1,
            content = "Test question"
        )

        assertEquals(0L, question.id)
        assertEquals(1L, question.subjectId)
        assertEquals("Test question", question.content)
        assertEquals("", question.answer)
        assertEquals("", question.userAnswer)
        assertEquals(ErrorType.UNKNOWN, question.errorType)
        assertNull(question.imagePath)
        assertNull(question.chapterId)
        assertNull(question.knowledgePointId)
        assertEquals(1, question.leitnerBox)
        assertEquals(2.5f, question.easeFactor)
        assertEquals(1, question.intervalDays)
        assertEquals(0, question.streak)
        assertEquals(0, question.reviewCount)
        assertNotNull(question.nextReviewDate)
        assertNull(question.lastReviewDate)
        assertNotNull(question.createdAt)
        assertNotNull(question.updatedAt)
    }

    @Test
    fun `copy should preserve all fields`() {
        val original = QuestionEntity(
            id = 1,
            subjectId = 2,
            content = "Original",
            answer = "Answer",
            userAnswer = "User",
            errorType = ErrorType.CARELESS,
            leitnerBox = 3,
            easeFactor = 2.0f,
            streak = 5
        )

        val copy = original.copy(content = "Updated")

        assertEquals(1L, copy.id)
        assertEquals(2L, copy.subjectId)
        assertEquals("Updated", copy.content)
        assertEquals("Answer", copy.answer)
        assertEquals("User", copy.userAnswer)
        assertEquals(ErrorType.CARELESS, copy.errorType)
        assertEquals(3, copy.leitnerBox)
        assertEquals(2.0f, copy.easeFactor)
        assertEquals(5, copy.streak)
    }

    @Test
    fun `error types should be distinct`() {
        val types = ErrorType.entries

        assertEquals(5, types.size)
        assertTrue(types.contains(ErrorType.UNKNOWN))
        assertTrue(types.contains(ErrorType.CARELESS))
        assertTrue(types.contains(ErrorType.CONCEPTUAL))
        assertTrue(types.contains(ErrorType.METHOD))
        assertTrue(types.contains(ErrorType.CALCULATION))
    }
}
