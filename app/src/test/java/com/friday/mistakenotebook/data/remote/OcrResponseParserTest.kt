package com.friday.mistakenotebook.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrResponseParserTest {

    @Test
    fun `parse should extract plain content from real volcano response`() {
        val response = """
            {"choices":[{"finish_reason":"stop","index":0,"logprobs":null,"message":{"content":"2. 把表格补充完整。\n|班级|一班|二班|三班|\n|----|----|----|----|\n|第一轮|28分|25分|22分|\n|第二轮|37分|33分|36分|\n|第三轮|29分|(27)分|32分|\n|总分|(94)分|85分|89分|","role":"assistant"}}],"created":1782624593,"id":"021782624590316e4fd4c285bd49f804239cddc30345c482d93ba","model":"doubao-1-5-vision-pro-32k-250115","service_tier":"default","object":"chat.completion","usage":{"completion_tokens":97,"prompt_tokens":1358,"total_tokens":1455}}
        """.trimIndent()

        val result = OcrResponseParser.parse(response)

        when (result) {
            is OcrParseResult.Success -> {
                assertEquals("2. 把表格补充完整。\n|班级|一班|二班|三班|\n|----|----|----|----|\n|第一轮|28分|25分|22分|\n|第二轮|37分|33分|36分|\n|第三轮|29分|(27)分|32分|\n|总分|(94)分|85分|89分|", result.result.text)
                assertEquals(0.95f, result.result.confidence, 0.0001f)
            }
            else -> throw AssertionError("Expected success, got $result")
        }
    }

    @Test
    fun `parse should handle empty content`() {
        val response = """
            {"choices":[{"message":{"content":"   "}}]}
        """.trimIndent()

        val result = OcrResponseParser.parse(response)

        assertEquals(OcrParseResult.EmptyContent, result)
    }

    @Test
    fun `parse should handle array content`() {
        val response = """
            {"choices":[{"message":{"content":[{"text":"第一行"},{"text":"第二行"}]}}]}
        """.trimIndent()

        val result = OcrResponseParser.parse(response)

        when (result) {
            is OcrParseResult.Success -> {
                assertEquals("第一行\n第二行", result.result.text)
                assertEquals(0.95f, result.result.confidence, 0.0001f)
            }
            else -> throw AssertionError("Expected success, got $result")
        }
    }

    @Test
    fun `parse should detect missing choices`() {
        val response = "{}"

        val result = OcrResponseParser.parse(response)

        assertEquals(OcrParseResult.MissingChoices, result)
    }
}
