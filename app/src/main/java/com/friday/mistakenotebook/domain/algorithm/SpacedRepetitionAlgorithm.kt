package com.friday.mistakenotebook.domain.algorithm

import com.friday.mistakenotebook.data.local.entity.QuestionEntity
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 间隔重复算法
 * 结合艾宾浩斯遗忘曲线和莱特纳卡片盒系统
 */
object SpacedRepetitionAlgorithm {

    // 莱特纳盒子对应的天数间隔
    private val BOX_INTERVALS = mapOf(
        1 to 1,   // 盒子1：每天复习
        2 to 2,   // 盒子2：每2天复习
        3 to 4,   // 盒子3：每4天复习
        4 to 7,   // 盒子4：每7天复习
        5 to 15   // 盒子5：每15天复习
    )

    /**
     * 计算下次复习时间
     */
    fun calculateNextReview(question: QuestionEntity, score: Int): QuestionEntity {
        val isCorrect = score >= 3
        val newLeitnerBox = if (isCorrect) {
            minOf(question.leitnerBox + 1, 5)
        } else {
            1  // 答错回到盒子1
        }

        val newStreak = if (isCorrect) question.streak + 1 else 0
        val newEaseFactor = calculateNewEaseFactor(question.easeFactor, score)
        val baseInterval = BOX_INTERVALS[newLeitnerBox] ?: 1
        val intervalDays = (baseInterval * newEaseFactor).roundToInt()
            .coerceAtLeast(1)

        val nextReviewDate = System.currentTimeMillis() + intervalDays * 24 * 60 * 60 * 1000L

        return question.copy(
            leitnerBox = newLeitnerBox,
            easeFactor = newEaseFactor,
            intervalDays = intervalDays,
            streak = newStreak,
            reviewCount = question.reviewCount + 1,
            nextReviewDate = nextReviewDate,
            lastReviewDate = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * 计算新的难度系数
     * 公式：EF' = EF + (0.1 - (5-score) * (0.08 + (5-score) * 0.02))
     */
    private fun calculateNewEaseFactor(oldEF: Float, score: Int): Float {
        val diff = 5 - score
        val newEF = oldEF + (0.1f - diff * (0.08f + diff * 0.02f))
        return max(1.3f, newEF)
    }

    /**
     * 获取莱特纳盒子描述
     */
    fun getBoxDescription(box: Int): String {
        return when (box) {
            1 -> "新题/答错"
            2 -> "初步掌握"
            3 -> "基本掌握"
            4 -> "熟练掌握"
            5 -> "完全掌握"
            else -> "未知"
        }
    }

    /**
     * 获取掌握百分比
     */
    fun getMasteryPercentage(box: Int): Int {
        return when (box) {
            1 -> 0
            2 -> 25
            3 -> 50
            4 -> 75
            5 -> 100
            else -> 0
        }
    }
}
