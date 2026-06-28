package com.friday.mistakenotebook.data.remote

import com.google.gson.JsonParser

object OcrResponseParser {
    fun parse(responseBody: String): OcrParseResult {
        return try {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val choices = json.getAsJsonArray("choices")
                ?: return OcrParseResult.MissingChoices

            if (choices.size() == 0) {
                return OcrParseResult.MissingChoices
            }

            val message = choices[0].asJsonObject.getAsJsonObject("message")
                ?: return OcrParseResult.MissingChoices
            val contentElement = message.get("content")
                ?: return OcrParseResult.MissingChoices

            val content = when {
                contentElement.isJsonPrimitive -> contentElement.asString
                contentElement.isJsonArray -> {
                    contentElement.asJsonArray.joinToString(separator = "\n") { part ->
                        when {
                            part.isJsonObject && part.asJsonObject.has("text") -> part.asJsonObject.get("text").asString
                            part.isJsonPrimitive -> part.asString
                            else -> ""
                        }
                    }
                }
                else -> ""
            }.trim()

            if (content.isBlank()) {
                OcrParseResult.EmptyContent
            } else {
                OcrParseResult.Success(
                    OcrResult(
                        text = content,
                        confidence = 0.95f
                    )
                )
            }
        } catch (e: Exception) {
            OcrParseResult.Failure(e.message ?: "未知解析错误")
        }
    }
}
