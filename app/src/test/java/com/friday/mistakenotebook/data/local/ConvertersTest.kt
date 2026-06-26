package com.friday.mistakenotebook.data.local

import com.friday.mistakenotebook.data.local.entity.AiTaskType
import com.friday.mistakenotebook.data.local.entity.ErrorType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ConvertersTest {

    private lateinit var converters: Converters

    @Before
    fun setup() {
        converters = Converters()
    }

    @Test
    fun `fromErrorType should convert to string`() {
        assertEquals("UNKNOWN", converters.fromErrorType(ErrorType.UNKNOWN))
        assertEquals("CARELESS", converters.fromErrorType(ErrorType.CARELESS))
        assertEquals("CONCEPTUAL", converters.fromErrorType(ErrorType.CONCEPTUAL))
        assertEquals("METHOD", converters.fromErrorType(ErrorType.METHOD))
        assertEquals("CALCULATION", converters.fromErrorType(ErrorType.CALCULATION))
    }

    @Test
    fun `toErrorType should convert from string`() {
        assertEquals(ErrorType.UNKNOWN, converters.toErrorType("UNKNOWN"))
        assertEquals(ErrorType.CARELESS, converters.toErrorType("CARELESS"))
        assertEquals(ErrorType.CONCEPTUAL, converters.toErrorType("CONCEPTUAL"))
        assertEquals(ErrorType.METHOD, converters.toErrorType("METHOD"))
        assertEquals(ErrorType.CALCULATION, converters.toErrorType("CALCULATION"))
    }

    @Test
    fun `toErrorType should return UNKNOWN for invalid string`() {
        assertEquals(ErrorType.UNKNOWN, converters.toErrorType("INVALID"))
        assertEquals(ErrorType.UNKNOWN, converters.toErrorType(""))
    }

    @Test
    fun `fromAiTaskType should convert to string`() {
        assertEquals("OCR", converters.fromAiTaskType(AiTaskType.OCR))
        assertEquals("ANALYSIS", converters.fromAiTaskType(AiTaskType.ANALYSIS))
        assertEquals("GENERATE", converters.fromAiTaskType(AiTaskType.GENERATE))
    }

    @Test
    fun `toAiTaskType should convert from string`() {
        assertEquals(AiTaskType.OCR, converters.toAiTaskType("OCR"))
        assertEquals(AiTaskType.ANALYSIS, converters.toAiTaskType("ANALYSIS"))
        assertEquals(AiTaskType.GENERATE, converters.toAiTaskType("GENERATE"))
    }

    @Test
    fun `toAiTaskType should return OCR for invalid string`() {
        assertEquals(AiTaskType.OCR, converters.toAiTaskType("INVALID"))
        assertEquals(AiTaskType.OCR, converters.toAiTaskType(""))
    }

    @Test
    fun `error type conversion should be reversible`() {
        for (type in ErrorType.entries) {
            val converted = converters.fromErrorType(type)
            val backConverted = converters.toErrorType(converted)
            assertEquals(type, backConverted)
        }
    }

    @Test
    fun `ai task type conversion should be reversible`() {
        for (type in AiTaskType.entries) {
            val converted = converters.fromAiTaskType(type)
            val backConverted = converters.toAiTaskType(converted)
            assertEquals(type, backConverted)
        }
    }
}
