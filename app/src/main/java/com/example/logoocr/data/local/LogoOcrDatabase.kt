package com.example.logoocr.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.logoocr.data.local.dao.BrandDao
import com.example.logoocr.data.local.dao.LogoDao
import com.example.logoocr.data.local.dao.RecognitionResultDao
import com.example.logoocr.data.local.entity.BrandEntity
import com.example.logoocr.data.local.entity.LogoEntity
import com.example.logoocr.data.local.entity.RecognitionResultEntity

@Database(
    entities = [
        BrandEntity::class,
        LogoEntity::class,
        RecognitionResultEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class LogoOcrDatabase : RoomDatabase() {
    abstract fun brandDao(): BrandDao
    abstract fun logoDao(): LogoDao
    abstract fun recognitionResultDao(): RecognitionResultDao
}
