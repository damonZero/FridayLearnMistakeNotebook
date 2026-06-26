package com.friday.mistakenotebook.domain.model

import com.friday.mistakenotebook.data.local.entity.ErrorType

data class Question(
    val id: Long = 0,
    val subjectId: Long,
    val subjectName: String = "",
    val chapterId: Long? = null,
    val chapterName: String = "",
    val knowledgePointId: Long? = null,
    val knowledgePointName: String = "",
    val content: String,
    val answer: String = "",
    val userAnswer: String = "",
    val errorType: ErrorType = ErrorType.UNKNOWN,
    val imagePath: String? = null,
    val leitnerBox: Int = 1,
    val easeFactor: Float = 2.5f,
    val intervalDays: Int = 1,
    val streak: Int = 0,
    val reviewCount: Int = 0,
    val nextReviewDate: Long = System.currentTimeMillis(),
    val lastReviewDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
