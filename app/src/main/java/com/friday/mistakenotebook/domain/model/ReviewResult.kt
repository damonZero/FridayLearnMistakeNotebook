package com.friday.mistakenotebook.domain.model

/**
 * 复习结果
 */
data class ReviewResult(
    val questionId: Long,
    val isCorrect: Boolean,
    val score: Int = if (isCorrect) 5 else 2  // 1-5分
)
