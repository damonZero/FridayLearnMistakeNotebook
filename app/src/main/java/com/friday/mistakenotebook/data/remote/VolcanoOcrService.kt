package com.friday.mistakenotebook.data.remote

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 火山方舟 OCR 服务实现
 */
@Singleton
class VolcanoOcrService @Inject constructor(
    private val gson: Gson
) : OcrService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun recognizeText(imageBase64: String): OcrResult = withContext(Dispatchers.IO) {
        try {
            // 构建请求体
            val requestBody = buildRequestBody(imageBase64)

            // 发送请求
            val request = Request.Builder()
                .url("https://ark.cn-beijing.volces.com/api/v3/chat/completions")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer YOUR_API_KEY") // 需要从配置中获取
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")

            // 解析响应
            parseResponse(responseBody)
        } catch (e: Exception) {
            // 降级处理：返回模拟结果
            OcrResult(
                text = "OCR 识别失败: ${e.message}\n请手动输入题目内容",
                confidence = 0f
            )
        }
    }

    private fun buildRequestBody(imageBase64: String): String {
        return """
        {
            "model": "doubao-vision-pro-32k",
            "messages": [
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "image_url",
                            "image_url": {
                                "url": "data:image/jpeg;base64,$imageBase64"
                            }
                        },
                        {
                            "type": "text",
                            "text": "请识别图片中的所有文字，包括印刷体中文、英文和数学公式。只返回识别到的文字内容，不需要其他说明。"
                        }
                    ]
                }
            ],
            "max_tokens": 2048
        }
        """.trimIndent()
    }

    private fun parseResponse(responseBody: String): OcrResult {
        return try {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val choices = json.getAsJsonArray("choices")
            if (choices != null && choices.size() > 0) {
                val message = choices[0].asJsonObject.getAsJsonObject("message")
                val content = message.get("content").asString
                OcrResult(
                    text = content,
                    confidence = 0.95f
                )
            } else {
                OcrResult(
                    text = "无法识别图片内容",
                    confidence = 0f
                )
            }
        } catch (e: Exception) {
            OcrResult(
                text = "解析响应失败: ${e.message}",
                confidence = 0f
            )
        }
    }
}
