package com.friday.mistakenotebook.di

import com.friday.mistakenotebook.data.repository.QuestionRepositoryImpl
import com.friday.mistakenotebook.data.repository.SubjectRepositoryImpl
import com.friday.mistakenotebook.domain.repository.QuestionRepository
import com.friday.mistakenotebook.domain.repository.SubjectRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSubjectRepository(impl: SubjectRepositoryImpl): SubjectRepository

    @Binds
    @Singleton
    abstract fun bindQuestionRepository(impl: QuestionRepositoryImpl): QuestionRepository
}
