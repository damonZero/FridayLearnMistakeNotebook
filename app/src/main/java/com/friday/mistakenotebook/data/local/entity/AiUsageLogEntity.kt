package com.friday.mistakenotebook.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AI 使用日志实体
 */
@Entity(tableName = "ai_usage_logs")
data class AiUsageLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val provider: String,
    val taskType: AiTaskType,
    val modelName: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val estimatedCost: Double,
    val createdAt: Long = System.currentTimeMillis()
)
