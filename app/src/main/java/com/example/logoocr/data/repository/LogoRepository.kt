package com.example.logoocr.data.repository

import com.example.logoocr.data.local.entity.BrandEntity
import com.example.logoocr.data.local.entity.LogoEntity
import com.example.logoocr.data.local.entity.RecognitionResultEntity
import kotlinx.coroutines.flow.Flow

interface LogoRepository {
    fun observeBrands(): Flow<List<BrandEntity>>
    fun observeLogos(brandId: Long): Flow<List<LogoEntity>>
    fun observeRecognitionHistory(): Flow<List<RecognitionResultEntity>>
    fun observeRecognitionResult(id: Long): Flow<RecognitionResultEntity?>
    fun observeAllLogos(): Flow<List<LogoEntity>>

    suspend fun upsertBrand(brand: BrandEntity): Long
    suspend fun upsertLogo(logo: LogoEntity): Long
    suspend fun insertRecognitionResult(result: RecognitionResultEntity): Long
    suspend fun markRecognitionResult(id: Long, verified: Boolean)
    suspend fun getBrandByName(name: String): BrandEntity?
    suspend fun getBrandById(id: Long): BrandEntity?
    suspend fun getLogoById(id: Long): LogoEntity?
    suspend fun getAllLogos(): List<LogoEntity>
}
