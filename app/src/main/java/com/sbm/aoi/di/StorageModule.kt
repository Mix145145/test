package com.sbm.aoi.di

import android.content.Context
import androidx.room.Room
import com.sbm.aoi.storage.BackupService
import com.sbm.aoi.storage.StickerService
import com.sbm.aoi.storage.StorageDatabase
import com.sbm.aoi.storage.StorageRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides
    @Singleton
    fun provideStorageDb(@ApplicationContext context: Context): StorageDatabase =
        Room.databaseBuilder(context, StorageDatabase::class.java, "qfa.db").build()

    @Provides
    @Singleton
    fun provideStorageRepo(db: StorageDatabase): StorageRepository = StorageRepository(db.dao())

    @Provides
    @Singleton
    fun provideStickerService(): StickerService = StickerService()

    @Provides
    @Singleton
    fun provideBackupService(db: StorageDatabase): BackupService = BackupService(db.dao())
}
