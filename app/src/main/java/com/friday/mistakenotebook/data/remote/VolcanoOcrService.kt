package com.friday.mistakenotebook.data.remote

import android.util.Log
import com.friday.mistakenotebook.data.local.dao.AiConfigDao
import com.friday.mistakenotebook.data.local.entity.AiConfigEntity
import com.friday.mistakenotebook.data.local.entity.AiTaskType
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 火山方舟 OCR 服务实现（支持从数据库读取 AI 配置）
 */
@Singleton
class VolcanoOcrService @Inject constructor(
    private val aiConfigDao: AiConfigDao,
    private val usageLogger: AiUsageLogger
) : OcrService {

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor { message ->
            Log.d("OCR_HTTP", message)
        }.apply {
            // BASIC 级别：BODY 会把整张图的 base64 和 Authorization 头写进 logcat
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    override suspend fun recognizeText(imageBase64: String): OcrResult = withContext(Dispatchers.IO) {
        // config 一旦拿到，失败路径上也记一条 0 token 的用量日志
        var loggedConfig: AiConfigEntity? = null
        try {
            val config = aiConfigDao.getEnabledConfigByTaskType(AiTaskType.OCR)
                ?: throw Exception("请先在设置中配置 AI 模型的 API Key")
            loggedConfig = config
            val apiKey = config.apiKey
            val baseUrl = config.baseUrl.trimEnd('/')
            val modelName = config.modelName

            val requestBody = buildRequestBody(imageBase64, modelName)
            val request = Request.Builder()
                .url("$baseUrl/chat/completions")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = executeWithRetry(client, request)
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.body?.string()?.take(300)}")
            }
            val body = response.body?.string() ?: throw Exception("Empty response")

            val result = when (val parsed = OcrResponseParser.parse(body)) {
                is OcrParseResult.Success -> parsed.result
                OcrParseResult.EmptyContent -> OcrResult(
                    text = "OCR 返回为空，请检查图片是否清晰，或手动输入题目内容",
                    confidence = 0f
                )
                OcrParseResult.MissingChoices -> OcrResult(
                    text = "OCR 返回格式异常，未找到识别结果，请手动输入题目内容",
                    confidence = 0f
                )
                is OcrParseResult.Failure -> OcrResult(
                    text = "OCR 解析失败: ${parsed.message}\n请手动输入题目内容",
                    confidence = 0f
                )
            }

            val usage = parseTokenUsage(body)
            usageLogger.log(config, AiTaskType.OCR, usage.first, usage.second)
            result
        } catch (e: Exception) {
            loggedConfig?.let { usageLogger.log(it, AiTaskType.OCR, 0, 0) }
            OcrResult(
                text = "OCR 识别失败: ${e.message}\n请手动输入题目内容",
                confidence = 0f
            )
        }
    }

    /**
     * 解析响应 JSON 里的 usage.prompt_tokens / usage.completion_tokens；
     * 字段缺失或结构异常都不抛出，按 0 处理，不影响识别结果
     */
    private fun parseTokenUsage(responseBody: String): Pair<Int, Int> {
        return try {
            val usage = JsonParser.parseString(responseBody)
                .asJsonObject.getAsJsonObject("usage") ?: return 0 to 0
            fun readTokens(name: String): Int =
                usage.get(name)?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
            readTokens("prompt_tokens") to readTokens("completion_tokens")
        } catch (e: Exception) {
            Log.w("OCR_USAGE", "解析 token 用量失败: ${e.message}")
            0 to 0
        }
    }

    private fun buildRequestBody(imageBase64: String, modelName: String): String {
        return """
        {
            "model": "$modelName",
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
}
