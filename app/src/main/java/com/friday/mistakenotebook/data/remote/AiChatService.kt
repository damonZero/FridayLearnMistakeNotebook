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
    val answer: String,
    // 变化梯度标签：同型巩固 / 情境变换 / 逆向综合（旧数据可能为空）
    val variation: String = ""
)

/**
 * AI 识题结果：题干、学生作答（可为空）、参考答案（含多解法思路）
 */
data class QuestionExtraction(
    val content: String,
    val userAnswer: String,
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
            chatInternal(config, taskType, userPrompt, maxTokens, imageBase64 = null)
        }
    }

    /**
     * 图文混合对话：图片 + 文本一起发送（识题 = 提取题干 + 学生作答 + AI 解答一步完成）。
     * 需要 OCR 任务配置的多模态模型
     */
    suspend fun chatWithImage(
        taskType: AiTaskType,
        imageBase64: String,
        userPrompt: String,
        maxTokens: Int = 2048
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            val config = resolveConfig(taskType)
                ?: return@withContext Result.failure(
                    IllegalStateException("请先在设置中配置该任务的 AI 模型")
                )
            chatInternal(config, taskType, userPrompt, maxTokens, imageBase64)
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
        maxTokens: Int,
        imageBase64: String?
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
                                if (imageBase64 != null) {
                                    // OpenAI 兼容的多模态 content：图片在前、文本在后
                                    add(
                                        "content",
                                        JsonArray().apply {
                                            add(
                                                JsonObject().apply {
                                                    addProperty("type", "image_url")
                                                    add(
                                                        "image_url",
                                                        JsonObject().apply {
                                                            addProperty(
                                                                "url",
                                                                "data:image/jpeg;base64,$imageBase64"
                                                            )
                                                        }
                                                    )
                                                }
                                            )
                                            add(
                                                JsonObject().apply {
                                                    addProperty("type", "text")
                                                    addProperty("text", userPrompt)
                                                }
                                            )
                                        }
                                    )
                                } else {
                                    addProperty("content", userPrompt)
                                }
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
    /**
     * 生成相似题：「举一反三」按由近及远的变化梯度出题——
     * 第 1 题同型巩固（最接近原题，确认基本方法）、第 2 题情境变换（换壳不换考点，
     * 迫使学生重新识别）、第 3 题逆向/综合（反问或小综合，考本质理解）。
     * 考查目标始终是同一个核心知识点，但方向与描述逐步变化
     */
    suspend fun generateSimilarQuestions(
        questionContent: String,
        count: Int = 3
    ): Result<List<GeneratedQuestion>> {
        val prompt = buildString {
            appendLine("你是一位经验丰富的小学老师，正在为下面的错题设计「举一反三」巩固练习。")
            appendLine()
            appendLine("原题：$questionContent")
            appendLine()
            appendLine("请出 $count 道题。所有题目都必须考查与原题相同的那个核心知识点，但按「由近及远」的变化梯度设计，让学生从不同角度反复运用这个知识点：")
            appendLine("1. 第 1 题【同型巩固】：与原题同类型、同结构，只替换数字或表面细节，用于确认基本方法已经掌握；")
            appendLine("2. 第 2 题【情境变换】：保留同一知识点，但更换生活情境、叙述顺序或已知条件的位置，迫使学生重新识别考点，而不是套用模板；")
            appendLine("3. 第 3 题【逆向综合】：优先把问题反过来问（已知结果反求条件），或与一个简单的已学知识组合成小综合题，考查对知识点本质的理解。")
            if (count > 3) {
                appendLine("4. 第 4 题及以后在【情境变换】与【逆向综合】之间交替，保持同一考点、描述多变。")
            }
            appendLine()
            appendLine("要求：难度与原题相近，不引入超纲概念；数学应用题的情境要贴近小学生生活、叙述方式可以大胆变换；语言简洁清楚，适合小学生独立阅读。")
            appendLine()
            appendLine("请只输出一个 JSON 数组，不要输出任何其他文字或代码块标记，每个元素格式：")
            appendLine("""{"content": "题目内容", "answer": "答案与简要思路", "variation": "同型巩固"}""")
            appendLine("其中 variation 只能填：同型巩固 / 情境变换 / 逆向综合")
        }

        return chat(AiTaskType.SIMILAR_QUESTION, prompt, maxTokens = 2048).mapCatching { content ->
            parseGeneratedQuestions(content)
                ?: throw IllegalStateException("AI 返回的相似题内容无法解析，请重试")
        }
    }

    /**
     * AI 识题：一次视觉调用完成"提取题干 + 提取学生手写作答 + 给出多解法参考答案"，
     * 供拍照录入页直接预填表单。失败时调用方回退纯 OCR
     */
    suspend fun analyzeQuestionImage(imageBase64: String): Result<QuestionExtraction> {
        val prompt = buildString {
            appendLine("这是一张小学生的错题照片，可能包含印刷体题目和学生手写的作答。请完成：")
            appendLine("1. 提取题目内容（题干与选项，不要包含学生手写的作答）")
            appendLine("2. 如果照片里有学生手写的答案或解题过程，原样提取为学生的答案；没有则输出空字符串")
            appendLine("3. 给出正确答案与解析：尽量提供两种以上解法思路（用 1. 2. 编号区分），语言适合小学生理解")
            appendLine()
            appendLine("请只输出一个 JSON 对象，不要输出任何其他文字或代码块标记，格式：")
            appendLine("""{"content": "题目内容", "userAnswer": "学生的答案，没有则留空", "answer": "正确答案与多种解法思路"}""")
        }
        return chatWithImage(AiTaskType.OCR, imageBase64, prompt, maxTokens = 2048).mapCatching { content ->
            parseQuestionExtraction(content)
                ?: throw IllegalStateException("AI 返回的识题内容无法解析，请重试")
        }
    }

    /** 解析识题 JSON（容错：剥代码块、截取首尾大括号之间内容） */
    private fun parseQuestionExtraction(content: String): QuestionExtraction? {
        val json = extractBetween(content, '{', '}') ?: return null
        return try {
            val obj = JsonParser.parseString(json).asJsonObject
            fun read(name: String): String =
                obj.get(name)?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
            val questionContent = read("content")
            if (questionContent.isBlank()) null else {
                QuestionExtraction(
                    content = questionContent,
                    userAnswer = read("userAnswer"),
                    answer = read("answer")
                )
            }
        } catch (e: Exception) {
            null
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
                    val variation = obj.get("variation")
                        ?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
                    if (questionContent.isNotBlank()) {
                        GeneratedQuestion(content = questionContent, answer = answer, variation = variation)
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
