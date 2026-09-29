package com.friday.mistakenotebook.data.remote

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 连接测试结果
 */
sealed class TestResult {
    data class Success(val message: String) : TestResult()
    data class Failure(val message: String) : TestResult()
}

/**
 * AI 连接测试器：向 {baseUrl}/chat/completions 发送一条最小探测请求，
 * 用于验证用户填写的 baseUrl / API Key / 模型名是否真实可用。
 */
@Singleton
class AiConnectivityTester @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun testConnection(baseUrl: String, apiKey: String, modelName: String): TestResult {
        return withContext(Dispatchers.IO) {
            val url = "${baseUrl.trimEnd('/')}/chat/completions"

            // 最小探测请求：只发一个词、max_tokens=1，尽量把测试成本降到最低
            val jsonBody = JsonObject().apply {
                addProperty("model", modelName)
                add(
                    "messages",
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("role", "user")
                                addProperty("content", "hi")
                            }
                        )
                    }
                )
                addProperty("max_tokens", 1)
            }

            try {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    when {
                        response.isSuccessful -> TestResult.Success("连接成功，模型可用")
                        response.code == 401 || response.code == 403 ->
                            TestResult.Failure("API Key 无效或无权限")
                        response.code == 404 ->
                            TestResult.Failure("地址或模型名错误，请检查 baseUrl（一般以 /v1 结尾）与模型名")
                        response.code == 429 ->
                            TestResult.Failure("调用频率超限，Key 本身有效")
                        else -> TestResult.Failure("服务返回 HTTP ${response.code}")
                    }
                }
            } catch (e: SocketTimeoutException) {
                TestResult.Failure("网络错误：连接超时")
            } catch (e: UnknownHostException) {
                TestResult.Failure("网络错误：无法解析地址，请检查 baseUrl")
            } catch (e: ConnectException) {
                TestResult.Failure("网络错误：无法连接服务器")
            } catch (e: IOException) {
                TestResult.Failure("网络错误：${e.message ?: "请求失败"}")
            } catch (e: IllegalArgumentException) {
                // OkHttp 对非法 URL 抛 IllegalArgumentException
                TestResult.Failure("网络错误：地址格式不正确，请检查 baseUrl")
            }
        }
    }
}
