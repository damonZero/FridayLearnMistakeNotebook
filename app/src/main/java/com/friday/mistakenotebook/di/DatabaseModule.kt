package com.friday.mistakenotebook.di

import android.content.Context
import androidx.room.Room
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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MistakeNotebookDatabase {
        return Room.databaseBuilder(
            context,
            MistakeNotebookDatabase::class.java,
            MistakeNotebookDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
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
