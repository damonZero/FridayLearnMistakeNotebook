package com.friday.mistakenotebook.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AI 任务类型
 */
enum class AiTaskType {
    OCR,              // 文字识别
    ANALYSIS,         // 错题分析
    GENERATE,         // 生成相似题
    SIMILAR_QUESTION  // 相似题练习
}

/**
 * AI 配置实体
 */
@Entity(tableName = "ai_configs")
data class AiConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val provider: String,
    val apiKey: String,
    val baseUrl: String,
    val modelName: String,
    val taskType: AiTaskType,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
