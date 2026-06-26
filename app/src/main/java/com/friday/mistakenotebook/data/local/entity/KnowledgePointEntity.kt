package com.friday.mistakenotebook.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 知识点实体
 */
@Entity(
    tableName = "knowledge_points",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chapterId")]
)
data class KnowledgePointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chapterId: Long,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
