package com.friday.mistakenotebook.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 错误类型枚举
 */
enum class ErrorType {
    UNKNOWN,      // 未知
    CARELESS,     // 粗心
    CONCEPTUAL,   // 概念错误
    METHOD,       // 方法错误
    CALCULATION   // 计算错误
}

/**
 * 错题实体
 */
@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = KnowledgePointEntity::class,
            parentColumns = ["id"],
            childColumns = ["knowledgePointId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("subjectId"),
        Index("chapterId"),
        Index("knowledgePointId"),
        Index("nextReviewDate")
    ]
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val chapterId: Long? = null,
    val knowledgePointId: Long? = null,
    val content: String,
    val answer: String = "",
    val userAnswer: String = "",
    val errorType: ErrorType = ErrorType.UNKNOWN,
    val imagePath: String? = null,
    val aiAnalysis: String? = null,    // AI 知识点分析摘要（详情页展示，可覆盖重存）
    val knowledgePoint: String? = null, // 考点标签（AI 识题预填，卡片展示与列表分组用）

    // 间隔重复算法相关
    val leitnerBox: Int = 1,           // 莱特纳盒子等级 (1-5)
    val easeFactor: Float = 2.5f,      // 难度系数
    val intervalDays: Int = 1,         // 间隔天数
    val streak: Int = 0,               // 连续答对次数
    val reviewCount: Int = 0,          // 复习次数
    val nextReviewDate: Long = System.currentTimeMillis(),  // 下次复习时间
    val lastReviewDate: Long? = null,  // 上次复习时间

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
