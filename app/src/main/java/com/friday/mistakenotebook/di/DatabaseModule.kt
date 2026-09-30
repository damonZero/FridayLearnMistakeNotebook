package com.friday.mistakenotebook.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.friday.mistakenotebook.data.local.MistakeNotebookDatabase
import com.friday.mistakenotebook.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // v1 -> v2：错题表新增 AI 分析摘要列
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE questions ADD COLUMN aiAnalysis TEXT")
        }
    }

    // v2 -> v3：错题表新增知识点标签列（AI 识题预填，列表分组/卡片展示用）
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE questions ADD COLUMN knowledgePoint TEXT")
        }
    }

    // v3 -> v4：错题表新增举一反三持久化列（JSON，练习/打印复用，可刷新覆盖）
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE questions ADD COLUMN similarQuestions TEXT")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MistakeNotebookDatabase {
        return Room.databaseBuilder(
            context,
            MistakeNotebookDatabase::class.java,
            MistakeNotebookDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            // 禁止静默清库：schema 升级必须显式提供 Migration
            .build()
    }

    @Provides
    fun provideSubjectDao(database: MistakeNotebookDatabase): SubjectDao {
        return database.subjectDao()
    }

    @Provides
    fun provideChapterDao(database: MistakeNotebookDatabase): ChapterDao {
        return database.chapterDao()
    }

    @Provides
    fun provideKnowledgePointDao(database: MistakeNotebookDatabase): KnowledgePointDao {
        return database.knowledgePointDao()
    }

    @Provides
    fun provideQuestionDao(database: MistakeNotebookDatabase): QuestionDao {
        return database.questionDao()
    }

    @Provides
    fun provideAiConfigDao(database: MistakeNotebookDatabase): AiConfigDao {
        return database.aiConfigDao()
    }

    @Provides
    fun provideAiUsageLogDao(database: MistakeNotebookDatabase): AiUsageLogDao {
        return database.aiUsageLogDao()
    }
}
