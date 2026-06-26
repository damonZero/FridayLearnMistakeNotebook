package com.friday.mistakenotebook.data.local.entity

import org.junit.Assert.*
import org.junit.Test

class SubjectEntityTest {

    @Test
    fun `default values should be correct`() {
        val subject = SubjectEntity(
            name = "数学"
        )

        assertEquals(0L, subject.id)
        assertEquals("数学", subject.name)
        assertEquals("📚", subject.icon)
        assertEquals("#4CAF50", subject.color)
        assertFalse(subject.isPreset)
        assertNotNull(subject.createdAt)
    }

    @Test
    fun `preset subject should be marked correctly`() {
        val preset = SubjectEntity(
            name = "语文",
            icon = "📖",
            color = "#FF5722",
            isPreset = true
        )

        assertTrue(preset.isPreset)
        assertEquals("📖", preset.icon)
        assertEquals("#FF5722", preset.color)
    }

    @Test
    fun `copy should work correctly`() {
        val original = SubjectEntity(
            id = 1,
            name = "英语",
            icon = "🔤",
            color = "#4CAF50",
            isPreset = false
        )

        val copy = original.copy(name = "新英语")

        assertEquals(1L, copy.id)
        assertEquals("新英语", copy.name)
        assertEquals("🔤", copy.icon)
        assertFalse(copy.isPreset)
    }
}
