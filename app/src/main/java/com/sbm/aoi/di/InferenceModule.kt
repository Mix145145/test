package com.sbm.aoi.di

import android.content.Context
import com.sbm.aoi.data.storage.FileManager
import com.sbm.aoi.inference.OnnxInferenceEngine
import com.sbm.aoi.inference.SbmModelLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InferenceModule {

    @Provides
    @Singleton
    fun provideOnnxInferenceEngine(): OnnxInferenceEngine = OnnxInferenceEngine()

    @Provides
    @Singleton
    fun provideSbmModelLoader(
        @ApplicationContext context: Context,
        fileManager: FileManager,
    ): SbmModelLoader = SbmModelLoader(context, fileManager)
}
