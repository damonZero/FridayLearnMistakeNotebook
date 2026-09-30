package com.friday.mistakenotebook.data.remote

import android.util.Log
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

private const val TAG = "AI_CHAT"

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
    val answer: String,
    val knowledgePoint: String
)

/**
 * 举一反三题目的 JSON 编解码：持久化到 questions.similarQuestions 列，
 * 下次进入练习/打印直接复用，无需重新生成
 */
object SimilarQuestionCodec {
    private val gson = com.google.gson.Gson()

    fun encode(list: List<GeneratedQuestion>): String = gson.toJson(list)

    fun decode(json: String): List<GeneratedQuestion> = try {
        JsonParser.parseString(json).asJsonArray.mapNotNull { element ->
            try {
                val obj = element.asJsonObject
                val content = obj.get("content")?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
                if (content.isBlank()) null else GeneratedQuestion(
                    content = content,
                    answer = obj.get("answer")?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty(),
                    variation = obj.get("variation")?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
                )
            } catch (e: Exception) {
                null
            }
        }
    } catch (e: Exception) {
        emptyList()
    }
}

/**
 * 通用 AI 文本对话服务：按任务类型读取启用配置，POST {baseUrl}/chat/completions（OpenAI 兼容）。
 * 所有计费调用统一写用量日志（含失败记 0），瞬时网络错误自动重试一次。
 */
@Singleton
class AiChatService @Inject constructor(
    private val aiConfigDao: AiConfigDao,
    private val usageLogger: AiUsageLogger
) {

    private val gson = com.google.gson.Gson()

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
        imageBase64: String?,
        jsonMode: Boolean = false
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
                if (jsonMode) {
                    // JSON 模式：端点不支持时 HTTP 4xx，由调用方退回普通模式
                    add("response_format", JsonObject().apply { addProperty("type", "json_object") })
                }
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
     * 结构化生成管线（所有 JSON 型 AI 调用的统一入口），四层保障：
     * ① 常规请求；② 解析失败 → 附加纠错指令 + response_format=json_object 重试
     *    （端点不支持 JSON 模式自动退普通模式）；③ 宽松解析（剥代码块/截取括号）；
     * ④ 正则逐字段兜底恢复。网络/配置类错误不浪费重试。
     */
    private suspend fun <T> generateParsed(
        taskType: AiTaskType,
        prompt: String,
        maxTokens: Int,
        imageBase64: String? = null,
        parse: (String) -> T?,
        failureMsg: String
    ): Result<T> = withContext(Dispatchers.IO) {
        val config = resolveConfig(taskType)
            ?: return@withContext Result.failure(IllegalStateException("请先在设置中配置该任务的 AI 模型"))

        // 第一层：常规请求
        val first = chatInternal(config, taskType, prompt, maxTokens, imageBase64, jsonMode = false)
        first.getOrNull()?.let { raw ->
            parse(raw)?.let { return@withContext Result.success(it) }
        }

        // 只有"拿到内容但解析失败"或"正文为空"才值得换格式重试；网络/配置错误直接返回
        val failure = first.exceptionOrNull()
        val worthRetry = first.getOrNull() != null || (
            failure is IOException &&
                (failure.message?.contains("格式异常") == true ||
                    failure.message?.contains("未找到回复") == true)
            )
        if (!worthRetry) {
            return@withContext Result.failure(
                first.exceptionOrNull() ?: IllegalStateException(failureMsg)
            )
        }

        Log.w(TAG, "JSON 第一次解析失败，启用严格 JSON 模式重试")
        // 第二层：附加纠错指令，优先 JSON 模式（端点不支持则退普通模式）
        val strictPrompt = prompt + "\n\n注意：上一次的输出无法解析。请重新回答，并且只输出一个合法 JSON：不要代码块标记、不要解释文字、字符串内部不要出现未转义的换行符。"
        val strict = chatInternal(config, taskType, strictPrompt, maxTokens, imageBase64, jsonMode = true)
        val strictBody = strict.getOrNull() ?: run {
            chatInternal(config, taskType, strictPrompt, maxTokens, imageBase64, jsonMode = false)
                .getOrNull()
        }
        strictBody?.let { raw ->
            parse(raw)?.let { return@withContext Result.success(it) }
        }

        Result.failure(IllegalStateException(failureMsg))
    }

    /** 从原始文本中平衡扫描提取所有顶层 {...} 对象（容忍个别对象损坏） */
    private fun extractJsonObjects(raw: String): List<String> {
        val out = mutableListOf<String>()
        var depth = 0
        var start = -1
        var inStr = false
        var esc = false
        raw.forEachIndexed { i, ch ->
            if (esc) {
                esc = false
                return@forEachIndexed
            }
            when {
                ch == '\\' && inStr -> esc = true
                ch == '"' -> inStr = !inStr
                ch == '{' && !inStr -> {
                    if (depth == 0) start = i
                    depth++
                }
                ch == '}' && !inStr -> {
                    depth--
                    if (depth == 0 && start >= 0) {
                        out.add(raw.substring(start, i + 1))
                        start = -1
                    }
                }
            }
        }
        return out
    }

    /** 字段级正则兜底：匹配字符串值（可跨未转义换行）并反转义 */
    private fun regexField(raw: String, field: String): String? {
        val m = Regex("\"$field\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").find(raw) ?: return null
        val captured = m.groupValues[1]
        return try {
            gson.fromJson("\"$captured\"", String::class.java)
        } catch (e: Exception) {
            captured
        }
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
            appendLine("如果原题包含多个小问，analysis 请按小问分别展开。")
        }

        return generateParsed(
            taskType = AiTaskType.ANALYSIS,
            prompt = prompt,
            maxTokens = 8192,
            parse = { parseKnowledgeAnalysis(it) },
            failureMsg = "AI 返回的分析内容无法解析，请重试"
        )
    }

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
            appendLine("如果原题包含多个小问（如 (1)(2)）：优先针对其中最核心或学生最可能出错的小问出题，也可以出一道把多小问要素整合在一起的综合变式；题干保持清晰的小问编号结构。")
            appendLine()
            appendLine("请只输出一个 JSON 数组，不要输出任何其他文字或代码块标记，每个元素格式：")
            appendLine("""{"content": "题目内容", "answer": "答案与简要思路", "variation": "同型巩固"}""")
            appendLine("其中 variation 只能填：同型巩固 / 情境变换 / 逆向综合；answer 不能为空，必须先给出最终答案，再附 1~2 句简要思路")
        }

        // 思考型模型推理消耗大，多小问长题干的输出也长，预算放宽到 8192
        return generateParsed(
            taskType = AiTaskType.SIMILAR_QUESTION,
            prompt = prompt,
            maxTokens = 8192,
            parse = { parseGeneratedQuestions(it) },
            failureMsg = "AI 返回的相似题内容无法解析，请重试"
        )
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
            appendLine("4. 判断这道题考查的核心知识点，用 2~8 个字的短语概括（例如：鸡兔同笼、分数加减、单位换算、多边形面积）")
            appendLine()
            appendLine("重要——如果原题是一道大题包含多个小问（如 (1)(2)(3)）：")
            appendLine("- content 必须完整保留题干和全部小问及其编号，不要遗漏、不要合并")
            appendLine("- userAnswer 按小问分别提取学生的手写作答，用 (1)(2) 标注对应关系")
            appendLine("- answer 按小问分别给出正确答案与解法，同样用 (1)(2) 标注，每个小问尽量给出多种解法")
            appendLine()
            appendLine("请只输出一个 JSON 对象，不要输出任何其他文字或代码块标记，格式：")
            appendLine("""{"content": "题目内容", "userAnswer": "学生的答案，没有则留空", "answer": "正确答案与多种解法思路", "knowledgePoint": "核心知识点短语"}""")
        }
        // 思考型模型推理消耗大，多小问长题干的 JSON 输出也长，预算放宽到 8192
        return generateParsed(
            taskType = AiTaskType.OCR,
            prompt = prompt,
            maxTokens = 8192,
            imageBase64 = imageBase64,
            parse = { parseQuestionExtraction(it) },
            failureMsg = "AI 返回的识题内容无法解析，请重试"
        )
    }

    /** 宽松解析首尾大括号之间的 JSON 对象 */
    private fun parseJsonObject(raw: String): com.google.gson.JsonObject? = try {
        extractBetween(raw, '{', '}')?.let { JsonParser.parseString(it).asJsonObject }
    } catch (e: Exception) {
        null
    }

    /** 解析识题 JSON：宽松 JSON 优先，失败退正则逐字段兜底 */
    private fun parseQuestionExtraction(content: String): QuestionExtraction? {
        parseJsonObject(content)?.let { obj ->
            fun read(name: String): String =
                obj.get(name)?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
            val questionContent = read("content")
            if (questionContent.isNotBlank()) {
                return QuestionExtraction(
                    content = questionContent,
                    userAnswer = read("userAnswer"),
                    answer = read("answer"),
                    knowledgePoint = read("knowledgePoint")
                )
            }
        }
        // 正则兜底：字符串内有未转义换行等非法 JSON 时仍能恢复
        val contentField = regexField(content, "content")?.trim().takeIf { !it.isNullOrBlank() }
            ?: return null
        return QuestionExtraction(
            content = contentField,
            userAnswer = regexField(content, "userAnswer").orEmpty(),
            answer = regexField(content, "answer").orEmpty(),
            knowledgePoint = regexField(content, "knowledgePoint").orEmpty()
        )
    }

    /**
     * 从 OpenAI 兼容响应中提取 choices[0].message.content
     * （兼容 content 为字符串或分段数组；思考型模型正文为空时兜底读 reasoning_content）
     */    private fun extractContent(responseBody: String): String? {
        return try {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val choices = json.getAsJsonArray("choices") ?: return null
            if (choices.size() == 0) return null

            val message = choices[0].asJsonObject.getAsJsonObject("message") ?: return null
            val contentElement = message.get("content")

            // 注意：不要回退到 reasoning_content——思考文本里的草稿/模板 JSON
            // 会被宽松解析器误当结果。正文为空时直接报错让用户重试
            val content = when {
                contentElement == null || contentElement.isJsonNull -> ""
                contentElement.isJsonPrimitive -> contentElement.asString
                contentElement.isJsonArray -> contentElement.asJsonArray.joinToString(separator = "\n") { part ->
                    when {
                        part.isJsonObject && part.asJsonObject.has("text") -> part.asJsonObject.get("text").asString
                        part.isJsonPrimitive -> part.asString
                        else -> ""
                    }
                }
                else -> ""
            }

            content.trim().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    /** 解析知识点分析：宽松 JSON 优先，失败退正则兜底 */
    private fun parseKnowledgeAnalysis(content: String): KnowledgeAnalysis? {
        parseJsonObject(content)?.let { obj ->
            val knowledgePoints = obj.getAsJsonArray("knowledgePoints")
                ?.mapNotNull { element ->
                    if (element.isJsonPrimitive) element.asString.trim().takeIf { it.isNotBlank() } else null
                }
                .orEmpty()
            val errorTypeGuess = obj.get("errorTypeGuess")
                ?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
            val analysis = obj.get("analysis")
                ?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
            if (knowledgePoints.isNotEmpty() || analysis.isNotBlank()) {
                return KnowledgeAnalysis(knowledgePoints, errorTypeGuess, analysis)
            }
        }
        // 正则兜底
        val kps = Regex("\"knowledgePoints\"\\s*:\\s*\\[(.*?)]", RegexOption.DOT_MATCHES_ALL)
            .find(content)?.groupValues?.get(1)
            ?.let { block -> Regex("\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(block).mapNotNull { m ->
                runCatching { gson.fromJson("\"${m.groupValues[1]}\"", String::class.java) }.getOrNull()
            }.filter { it.isNotBlank() }.toList() }
            .orEmpty()
        val analysis = regexField(content, "analysis") ?: return null
        return KnowledgeAnalysis(
            knowledgePoints = kps,
            errorTypeGuess = regexField(content, "errorTypeGuess").orEmpty(),
            analysis = analysis
        )
    }

    /**
     * 解析相似题 JSON 数组（容错：剥掉代码块标记、截取首尾中括号之间的内容；
     * 模型误输出对象包裹数组时也兼容）
     */
    private fun parseGeneratedQuestions(content: String): List<GeneratedQuestion>? {
        val fromJson = try {
            val trimmed = stripCodeFence(content)
            val jsonArrayText = when {
                trimmed.contains('[') -> extractBetween(trimmed, '[', ']')
                else -> extractBetween(trimmed, '{', '}')
            } ?: null

            val root = jsonArrayText?.let { JsonParser.parseString(it) }
            val array = when {
                root == null -> null
                root.isJsonArray -> root.asJsonArray
                root.isJsonObject -> root.asJsonObject.getAsJsonArray("questions")
                else -> null
            }

            array?.mapNotNull { element ->
                try {
                    val obj = element.asJsonObject
                    generatedFrom(obj)
                } catch (e: Exception) {
                    null
                }
            }?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
        if (fromJson != null) return fromJson

        // 兜底：平衡扫描逐对象恢复——数组里混入一个坏对象只影响它自己
        val recovered = extractJsonObjects(stripCodeFence(content)).mapNotNull { objStr ->
            try {
                generatedFrom(JsonParser.parseString(objStr).asJsonObject)
            } catch (e: Exception) {
                null
            }
        }.filter { it.content.isNotBlank() }
        return recovered.takeIf { it.isNotEmpty() }
    }

    private fun generatedFrom(obj: com.google.gson.JsonObject): GeneratedQuestion? {
        val questionContent = obj.get("content")
            ?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
        if (questionContent.isBlank()) return null
        return GeneratedQuestion(
            content = questionContent,
            answer = obj.get("answer")?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty(),
            variation = obj.get("variation")?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
        )
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
