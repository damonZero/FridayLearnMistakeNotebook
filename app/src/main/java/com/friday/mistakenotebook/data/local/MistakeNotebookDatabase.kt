package com.friday.mistakenotebook.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.friday.mistakenotebook.data.local.dao.*
import com.friday.mistakenotebook.data.local.entity.*

@Database(
    entities = [
        SubjectEntity::class,
        ChapterEntity::class,
        KnowledgePointEntity::class,
        QuestionEntity::class,
        AiConfigEntity::class,
        AiUsageLogEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MistakeNotebookDatabase : RoomDatabase() {

    abstract fun subjectDao(): SubjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun knowledgePointDao(): KnowledgePointDao
    abstract fun questionDao(): QuestionDao
    abstract fun aiConfigDao(): AiConfigDao
    abstract fun aiUsageLogDao(): AiUsageLogDao

    companion object {
        const val DATABASE_NAME = "mistake_notebook.db"
    }
}
