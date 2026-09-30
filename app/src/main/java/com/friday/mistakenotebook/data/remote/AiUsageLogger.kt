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
                    estimatedCost = estimateCost(config.modelName, inputTokens, outputTokens)
                )
            )
        } catch (e: Exception) {
            Log.w("AI_USAGE", "写入 AI 用量日志失败: ${e.message}")
        }
    }

    /**
     * 按模型名估算本次费用（元）。供应商名是自由文本不能作为计价依据
     * （同名供应商可指向任意中转/任意模型），模型名可识别才计价，否则按 0。
     * 单价为官方每 100 万 tokens 定价（2026-09-10 起，闲时口径，高峰 ×2）：
     * DeepSeek Flash：输入 ¥1/M、输出 ¥4/M；豆包(火山方舟)：输入 ¥8/M、输出 ¥20/M
     */
    private fun estimateCost(modelName: String, inputTokens: Int, outputTokens: Int): Double {
        val (inputPrice, outputPrice) = when {
            modelName.contains("deepseek", ignoreCase = true) -> 0.000001 to 0.000004
            modelName.contains("doubao", ignoreCase = true) ||
                modelName.contains("ep-") -> 0.000008 to 0.00002
            else -> 0.0 to 0.0
        }
        return inputTokens * inputPrice + outputTokens * outputPrice
    }
}
