package com.friday.mistakenotebook.data.remote

import android.util.Log
import com.friday.mistakenotebook.data.local.dao.AiConfigDao
import com.friday.mistakenotebook.data.local.entity.AiTaskType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 火山方舟 OCR 服务实现（支持从数据库读取 AI 配置）
 */
@Singleton
class VolcanoOcrService @Inject constructor(
    private val aiConfigDao: AiConfigDao
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
        try {
            val config = aiConfigDao.getEnabledConfigByTaskType(AiTaskType.OCR)
                ?: throw Exception("请先在设置中配置 AI 模型的 API Key")
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

            val response = executeWithRetry(request)
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.body?.string()?.take(300)}")
            }
            val body = response.body?.string() ?: throw Exception("Empty response")

            when (val parsed = OcrResponseParser.parse(body)) {
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
        } catch (e: Exception) {
            OcrResult(
                text = "OCR 识别失败: ${e.message}\n请手动输入题目内容",
                confidence = 0f
            )
        }
    }

    /**
     * 执行请求，遇到瞬时网络错误（断连、超时）自动重试一次；
     * 大图上传 + 大模型推理经常超过单次连接的容忍时间，直接失败对用户来说就是"识别是空的"
     */
    private fun executeWithRetry(request: Request, maxAttempts: Int = 2): Response {
        var lastException: IOException? = null
        repeat(maxAttempts) { attempt ->
            try {
                return client.newCall(request).execute()
            } catch (e: IOException) {
                lastException = e
                Log.w("OCR_HTTP", "请求失败 (第 ${attempt + 1} 次): ${e.message}")
                if (attempt < maxAttempts - 1) {
                    Thread.sleep(1000)
                }
            }
        }
        throw lastException ?: IOException("请求失败")
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
