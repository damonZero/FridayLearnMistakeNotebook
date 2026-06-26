package com.friday.mistakenotebook.domain.model

import org.junit.Assert.*
import org.junit.Test

class SubjectTest {

    @Test
    fun `default values should be correct`() {
        val subject = Subject(
            id = 1,
            name = "数学"
        )

        assertEquals(1L, subject.id)
        assertEquals("数学", subject.name)
        assertEquals("📚", subject.icon)
        assertEquals("#4CAF50", subject.color)
        assertFalse(subject.isPreset)
        assertEquals(0, subject.questionCount)
    }

    @Test
    fun `preset subject should be marked correctly`() {
        val subject = Subject(
            id = 1,
            name = "语文",
            icon = "📖",
            color = "#FF5722",
            isPreset = true,
            questionCount = 10
        )

        assertTrue(subject.isPreset)
        assertEquals("📖", subject.icon)
        assertEquals("#FF5722", subject.color)
        assertEquals(10, subject.questionCount)
    }

    @Test
    fun `copy should preserve all fields`() {
        val original = Subject(
            id = 1,
            name = "英语",
            icon = "🔤",
            color = "#4CAF50",
            isPreset = false,
            questionCount = 5
        )

        val copy = original.copy(name = "新英语")

        assertEquals(1L, copy.id)
        assertEquals("新英语", copy.name)
        assertEquals("🔤", copy.icon)
        assertEquals("#4CAF50", copy.color)
        assertFalse(copy.isPreset)
        assertEquals(5, copy.questionCount)
    }

    @Test
    fun `subjects with same data should be equal`() {
        val subject1 = Subject(id = 1, name = "数学", icon = "🔢")
        val subject2 = Subject(id = 1, name = "数学", icon = "🔢")

        assertEquals(subject1, subject2)
        assertEquals(subject1.hashCode(), subject2.hashCode())
    }

    @Test
    fun `subjects with different ids should not be equal`() {
        val subject1 = Subject(id = 1, name = "数学")
        val subject2 = Subject(id = 2, name = "数学")

        assertNotEquals(subject1, subject2)
    }
}
