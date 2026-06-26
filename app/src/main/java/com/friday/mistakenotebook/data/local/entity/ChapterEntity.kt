package com.friday.mistakenotebook.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 章节实体
 */
@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
