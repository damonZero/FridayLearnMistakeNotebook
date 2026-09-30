package com.friday.mistakenotebook.data.remote

import android.util.Log
import com.friday.mistakenotebook.data.local.dao.AiUsageLogDao
import com.friday.mistakenotebook.data.local.entity.AiConfigEntity
import com.friday.mistakenotebook.data.local.entity.AiTaskType
import com.friday.mistakenotebook.data.local.entity.AiUsageLogEntity
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 调用的瞬时网络错误重试：断连/超时自动重试一次。
 * 大图上传 + 大模型推理经常超过单次连接的容忍时间，OCR 与文本对话共用。
 */
internal fun executeWithRetry(client: OkHttpClient, request: Request, maxAttempts: Int = 2): Response {
    var lastException: IOException? = null
    repeat(maxAttempts) { attempt ->
        try {
            return client.newCall(request).execute()
        } catch (e: IOException) {
            lastException = e
            Log.w("AI_HTTP", "请求失败 (第 ${attempt + 1} 次): ${e.message}")
            if (attempt < maxAttempts - 1) {
                Thread.sleep(1000)
            }
        }
    }
    throw lastException ?: IOException("请求失败")
}

/**
 * AI 调用用量日志统一落库：OCR 与文本对话（分析/相似题）共用。
 * 写库失败只告警，绝不影响调用方返回。
 */
@Singleton
class AiUsageLogger @Inject constructor(private val aiUsageLogDao: AiUsageLogDao) {

    suspend fun log(config: AiConfigEntity, taskType: AiTaskType, inputTokens: Int, outputTokens: Int) {
        try {
            aiUsageLogDao.insertUsageLog(
                AiUsageLogEntity(
                    provider = config.provider,
                    taskType = taskType,
                    modelName = config.modelName,
                    inputTokens = inputTokens,
                    outputTokens = outputTokens,
                    estimatedCost = estimateCost(config.provider, config.modelName, inputTokens, outputTokens)
                )
            )
        } catch (e: Exception) {
            Log.w("AI_USAGE", "写入 AI 用量日志失败: ${e.message}")
        }
    }

    /**
     * 按供应商优先计价（供应商托管的模型名可能与别家撞名，如火山托管的 deepseek），
     * 自定义供应商退回模型名匹配；无法识别不计费。
     * 单价为各官方每 100 万 tokens 定价：
     * DeepSeek Flash（V4.1，2026-09-10 起闲时）：输入 ¥1/M、输出 ¥4/M（高峰 ×2，此处按闲时估）
     * 豆包(火山方舟)：输入 ¥8/M、输出 ¥20/M
     */
    private fun estimateCost(
        provider: String,
        modelName: String,
        inputTokens: Int,
        outputTokens: Int
    ): Double {
        val (inputPrice, outputPrice) = when {
            provider == "DeepSeek" -> 0.000001 to 0.000004
            provider == "火山方舟" -> 0.000008 to 0.00002
            modelName.contains("deepseek", ignoreCase = true) -> 0.000001 to 0.000004
            modelName.contains("doubao", ignoreCase = true) ||
                modelName.contains("ep-") -> 0.000008 to 0.00002
            else -> 0.0 to 0.0
        }
        return inputTokens * inputPrice + outputTokens * outputPrice
    }
}
