package com.friday.mistakenotebook.data.remote

import com.friday.mistakenotebook.data.local.dao.AiConfigDao
import com.friday.mistakenotebook.data.local.entity.AiTaskType
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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
 * 知识点分析结果
 */
data class KnowledgeAnalysis(
    val knowledgePoints: List<String>,
    val errorTypeGuess: String,
    val analysis: String
)

/**
 * 生成的相似题
 */
data class GeneratedQuestion(
    val content: String,
    val answer: String
)

/**
 * 通用 AI 文本对话服务：按任务类型读取启用配置，POST {baseUrl}/chat/completions（OpenAI 兼容）。
 * 所有计费调用统一写用量日志（含失败记 0），瞬时网络错误自动重试一次。
 */
@Singleton
class AiChatService @Inject constructor(
    private val aiConfigDao: AiConfigDao,
    private val usageLogger: AiUsageLogger
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * 通用文本对话：按 taskType 读启用配置（无则返回失败），
     * 成功时返回 choices[0].message.content
     */
    suspend fun chat(taskType: AiTaskType, userPrompt: String, maxTokens: Int = 2048): Result<String> {
        return withContext(Dispatchers.IO) {
            val config = resolveConfig(taskType)
                ?: return@withContext Result.failure(
                    IllegalStateException("请先在设置中配置该任务的 AI 模型")
                )
            chatInternal(config, taskType, userPrompt, maxTokens)
        }
    }

    /** SIMILAR_QUESTION 向下兼容更名前的 GENERATE 配置，避免老用户配好的槽位读不到 */
    private suspend fun resolveConfig(taskType: AiTaskType) = when (taskType) {
        AiTaskType.SIMILAR_QUESTION ->
            aiConfigDao.getEnabledConfigByTaskType(AiTaskType.SIMILAR_QUESTION)
                ?: aiConfigDao.getEnabledConfigByTaskType(AiTaskType.GENERATE)
        else -> aiConfigDao.getEnabledConfigByTaskType(taskType)
    }

    private suspend fun chatInternal(
        config: com.friday.mistakenotebook.data.local.entity.AiConfigEntity,
        taskType: AiTaskType,
        userPrompt: String,
        maxTokens: Int
    ): Result<String> {
        return try {
            val jsonBody = JsonObject().apply {
                addProperty("model", config.modelName)
                add(
                    "messages",
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("role", "user")
                                addProperty("content", userPrompt)
                            }
                        )
                    }
                )
                addProperty("max_tokens", maxTokens)
            }

            val request = Request.Builder()
                .url("${config.baseUrl.trimEnd('/')}/chat/completions")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            executeWithRetry(client, request).use { response ->
                if (!response.isSuccessful) {
                    usageLogger.log(config, taskType, 0, 0)
                    val message = when {
                        response.code == 401 || response.code == 403 ->
                            "API Key 无效或无权限"
                        response.code == 404 ->
                            "地址或模型名错误，请检查 Base URL 与模型名称是否匹配（可用模板一键重新预填）"
                        response.code == 429 ->
                            "调用频率超限，请稍后再试"
                        else ->
                            "服务返回 HTTP ${response.code}"
                    }
                    return Result.failure(IOException(message))
                }

                val body = response.body?.string()
                if (body == null) {
                    usageLogger.log(config, taskType, 0, 0)
                    return Result.failure(IOException("服务返回为空"))
                }

                val usage = parseUsageTokens(body)
                val content = extractContent(body)
                usageLogger.log(config, taskType, usage.first, usage.second)

                content?.let { Result.success(it) }
                    ?: Result.failure(IOException("AI 返回格式异常，未找到回复内容，请重试"))
            }
        } catch (e: Exception) {
            usageLogger.log(config, taskType, 0, 0)
            Result.failure(IOException(mapNetworkError(e)))
        }
    }

    /** 解析 usage.prompt_tokens / completion_tokens，缺失按 0 记，不影响主流程 */
    private fun parseUsageTokens(responseBody: String): Pair<Int, Int> {
        return try {
            val usage = JsonParser.parseString(responseBody).asJsonObject.getAsJsonObject("usage")
                ?: return 0 to 0
            fun read(name: String): Int =
                usage.get(name)?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
            read("prompt_tokens") to read("completion_tokens")
        } catch (e: Exception) {
            0 to 0
        }
    }

    private fun mapNetworkError(e: Exception): String = when (e) {
        is SocketTimeoutException -> "网络错误：连接超时"
        is UnknownHostException -> "网络错误：无法解析地址，请检查 baseUrl"
        is ConnectException -> "网络错误：无法连接服务器"
        is IllegalArgumentException ->
            // OkHttp 对非法 URL 抛 IllegalArgumentException
            "网络错误：地址格式不正确，请检查 baseUrl"
        is IOException -> "网络错误：${e.message ?: "请求失败"}"
        else -> "请求失败：${e.message ?: "未知错误"}"
    }

    /**
     * AI 知识点分析：分析题目涉及的知识点、猜测错因并给出讲解
     */
    suspend fun analyzeKnowledge(
        questionContent: String,
        userAnswer: String?,
        correctAnswer: String?
    ): Result<KnowledgeAnalysis> {
        val prompt = buildString {
            appendLine("你是一位经验丰富的小学老师，请分析下面这道错题。")
            appendLine()
            appendLine("题目：$questionContent")
            if (!userAnswer.isNullOrBlank()) {
                appendLine("学生的答案：$userAnswer")
            }
            if (!correctAnswer.isNullOrBlank()) {
                appendLine("正确答案：$correctAnswer")
            }
            appendLine()
            appendLine("请只输出一个 JSON 对象，不要输出任何其他文字或代码块标记，格式如下：")
            appendLine("""{"knowledgePoints": ["知识点1", "知识点2"], "errorTypeGuess": "错误原因的简短描述", "analysis": "详细分析：涉及的知识点、错因、正确解法"}""")
        }

        return chat(AiTaskType.ANALYSIS, prompt, maxTokens = 2048).mapCatching { content ->
            parseKnowledgeAnalysis(content)
                ?: throw IllegalStateException("AI 返回的分析内容无法解析，请重试")
        }
    }

    /**
     * 生成相似题：仿照原题出 count 道考查相同知识点的练习题
     */
    suspend fun generateSimilarQuestions(
        questionContent: String,
        count: Int = 3
    ): Result<List<GeneratedQuestion>> {
        val prompt = buildString {
            appendLine("你是一位经验丰富的小学老师，请仿照下面的题目，出 $count 道考查相同知识点、难度相近的相似题。")
            appendLine()
            appendLine("原题：$questionContent")
            appendLine()
            appendLine("请只输出一个 JSON 数组，不要输出任何其他文字或代码块标记，格式如下：")
            appendLine("""[{"content": "题目内容", "answer": "答案"}]""")
        }

        return chat(AiTaskType.SIMILAR_QUESTION, prompt, maxTokens = 2048).mapCatching { content ->
            parseGeneratedQuestions(content)
                ?: throw IllegalStateException("AI 返回的相似题内容无法解析，请重试")
        }
    }

    /**
     * 从 OpenAI 兼容响应中提取 choices[0].message.content（兼容 content 为字符串或分段数组）
     */
    private fun extractContent(responseBody: String): String? {
        return try {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val choices = json.getAsJsonArray("choices") ?: return null
            if (choices.size() == 0) return null

            val message = choices[0].asJsonObject.getAsJsonObject("message") ?: return null
            val contentElement = message.get("content") ?: return null

            when {
                contentElement.isJsonPrimitive -> contentElement.asString
                contentElement.isJsonArray -> contentElement.asJsonArray.joinToString(separator = "\n") { part ->
                    when {
                        part.isJsonObject && part.asJsonObject.has("text") -> part.asJsonObject.get("text").asString
                        part.isJsonPrimitive -> part.asString
                        else -> ""
                    }
                }
                else -> ""
            }.trim().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析知识点分析 JSON（容错：剥掉代码块标记、截取首尾大括号之间的内容）
     */
    private fun parseKnowledgeAnalysis(content: String): KnowledgeAnalysis? {
        val json = extractBetween(content, '{', '}') ?: return null
        return try {
            val obj = JsonParser.parseString(json).asJsonObject
            val knowledgePoints = obj.getAsJsonArray("knowledgePoints")
                ?.mapNotNull { element ->
                    if (element.isJsonPrimitive) element.asString.trim().takeIf { it.isNotBlank() } else null
                }
                .orEmpty()
            val errorTypeGuess = obj.get("errorTypeGuess")
                ?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
            val analysis = obj.get("analysis")
                ?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()

            if (knowledgePoints.isEmpty() && analysis.isBlank()) {
                null
            } else {
                KnowledgeAnalysis(
                    knowledgePoints = knowledgePoints,
                    errorTypeGuess = errorTypeGuess,
                    analysis = analysis
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析相似题 JSON 数组（容错：剥掉代码块标记、截取首尾中括号之间的内容；
     * 模型误输出对象包裹数组时也兼容）
     */
    private fun parseGeneratedQuestions(content: String): List<GeneratedQuestion>? {
        return try {
            val trimmed = stripCodeFence(content)
            val jsonArrayText = when {
                trimmed.contains('[') -> extractBetween(trimmed, '[', ']')
                else -> extractBetween(trimmed, '{', '}')
            } ?: return null

            val root = JsonParser.parseString(jsonArrayText)
            val array = when {
                root.isJsonArray -> root.asJsonArray
                root.isJsonObject -> root.asJsonObject.getAsJsonArray("questions") ?: return null
                else -> return null
            }

            val questions = array.mapNotNull { element ->
                try {
                    val obj = element.asJsonObject
                    val questionContent = obj.get("content")
                        ?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
                    val answer = obj.get("answer")
                        ?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
                    if (questionContent.isNotBlank()) {
                        GeneratedQuestion(content = questionContent, answer = answer)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }

            questions.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 剥掉 Markdown 代码块标记
     */
    private fun stripCodeFence(content: String): String {
        return content
            .replace("```json", "", ignoreCase = true)
            .replace("```", "")
            .trim()
    }

    /**
     * 容错提取：取 first 与 last 之间的文本（含 first/last 本身）；
     * 模型在 JSON 前后附带说明文字时仍能解析
     */
    private fun extractBetween(text: String, first: Char, last: Char): String? {
        val normalized = stripCodeFence(text)
        val start = normalized.indexOf(first)
        val end = normalized.lastIndexOf(last)
        if (start < 0 || end <= start) return null
        return normalized.substring(start, end + 1)
    }
}
