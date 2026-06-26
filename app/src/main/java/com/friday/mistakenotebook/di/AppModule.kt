package com.friday.mistakenotebook.di

import com.friday.mistakenotebook.data.remote.OcrService
import com.friday.mistakenotebook.data.remote.VolcanoOcrService
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindOcrService(impl: VolcanoOcrService): OcrService
}

@Module
@InstallIn(SingletonComponent::class)
object GsonModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()
}
