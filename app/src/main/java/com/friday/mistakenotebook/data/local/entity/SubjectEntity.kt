package com.friday.mistakenotebook.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 科目实体
 */
@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String = "📚",
    val color: String = "#4CAF50",
    val isPreset: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
