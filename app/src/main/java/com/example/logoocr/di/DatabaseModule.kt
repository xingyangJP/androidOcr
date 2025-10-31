package com.example.logoocr.di

import android.content.Context
import androidx.room.Room
import com.example.logoocr.data.local.LogoOcrDatabase
import com.example.logoocr.data.repository.LogoRepository
import com.example.logoocr.data.repository.LogoRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "logo_ocr.db"

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLogoRepository(
        impl: LogoRepositoryImpl
    ): LogoRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): LogoOcrDatabase =
        Room.databaseBuilder(
            context,
            LogoOcrDatabase::class.java,
            DATABASE_NAME
        ).build()

    @Provides
    fun provideBrandDao(db: LogoOcrDatabase) = db.brandDao()

    @Provides
    fun provideLogoDao(db: LogoOcrDatabase) = db.logoDao()

    @Provides
    fun provideRecognitionResultDao(db: LogoOcrDatabase) = db.recognitionResultDao()
}
