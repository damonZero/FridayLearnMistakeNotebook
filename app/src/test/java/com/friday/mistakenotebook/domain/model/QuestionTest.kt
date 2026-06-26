package com.friday.mistakenotebook.domain.model

import com.friday.mistakenotebook.data.local.entity.ErrorType
import org.junit.Assert.*
import org.junit.Test

class QuestionTest {

    @Test
    fun `default values should be correct`() {
        val question = Question(
            id = 1,
            subjectId = 1,
            content = "Test question"
        )

        assertEquals(1L, question.id)
        assertEquals(1L, question.subjectId)
        assertEquals("", question.subjectName)
        assertNull(question.chapterId)
        assertEquals("", question.chapterName)
        assertNull(question.knowledgePointId)
        assertEquals("", question.knowledgePointName)
        assertEquals("Test question", question.content)
        assertEquals("", question.answer)
        assertEquals("", question.userAnswer)
        assertEquals(ErrorType.UNKNOWN, question.errorType)
        assertNull(question.imagePath)
        assertEquals(1, question.leitnerBox)
        assertEquals(2.5f, question.easeFactor)
        assertEquals(1, question.intervalDays)
        assertEquals(0, question.streak)
        assertEquals(0, question.reviewCount)
        assertNotNull(question.nextReviewDate)
        assertNull(question.lastReviewDate)
        assertNotNull(question.createdAt)
    }

    @Test
    fun `copy should preserve all fields`() {
        val original = Question(
            id = 1,
            subjectId = 2,
            subjectName = "数学",
            content = "1 + 1 = ?",
            answer = "2",
            leitnerBox = 3,
            streak = 5
        )

        val copy = original.copy(content = "2 + 2 = ?")

        assertEquals(1L, copy.id)
        assertEquals(2L, copy.subjectId)
        assertEquals("数学", copy.subjectName)
        assertEquals("2 + 2 = ?", copy.content)
        assertEquals("2", copy.answer)
        assertEquals(3, copy.leitnerBox)
        assertEquals(5, copy.streak)
    }

    @Test
    fun `question with all fields should work correctly`() {
        val question = Question(
            id = 100,
            subjectId = 1,
            subjectName = "语文",
            chapterId = 10,
            chapterName = "第一章",
            knowledgePointId = 100,
            knowledgePointName = "拼音",
            content = "写出下列汉字的拼音",
            answer = "hàn zì",
            userAnswer = "han zi",
            errorType = ErrorType.CARELESS,
            imagePath = "/path/to/image.jpg",
            leitnerBox = 4,
            easeFactor = 2.3f,
            intervalDays = 7,
            streak = 3,
            reviewCount = 10,
            nextReviewDate = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000,
            lastReviewDate = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000
        )

        assertEquals(100L, question.id)
        assertEquals("语文", question.subjectName)
        assertEquals("第一章", question.chapterName)
        assertEquals("拼音", question.knowledgePointName)
        assertEquals(ErrorType.CARELESS, question.errorType)
        assertEquals(4, question.leitnerBox)
        assertEquals(2.3f, question.easeFactor)
        assertEquals(3, question.streak)
        assertEquals(10, question.reviewCount)
    }
}
