package com.sbm.aoi.di

import android.content.Context
import com.sbm.aoi.data.repository.AnnotationRepository
import com.sbm.aoi.data.repository.ModelRepository
import com.sbm.aoi.data.repository.SettingsRepository
import com.sbm.aoi.data.storage.FileManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFileManager(
        @ApplicationContext context: Context,
    ): FileManager = FileManager(context)

    @Provides
    @Singleton
    fun provideModelRepository(
        fileManager: FileManager,
    ): ModelRepository = ModelRepository(fileManager)

    @Provides
    @Singleton
    fun provideAnnotationRepository(
        fileManager: FileManager,
    ): AnnotationRepository = AnnotationRepository(fileManager)

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context,
    ): SettingsRepository = SettingsRepository(context)
}
