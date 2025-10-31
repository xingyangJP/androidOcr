package com.example.logoocr.data.repository

import com.example.logoocr.data.local.dao.BrandDao
import com.example.logoocr.data.local.dao.LogoDao
import com.example.logoocr.data.local.dao.RecognitionResultDao
import com.example.logoocr.data.local.entity.BrandEntity
import com.example.logoocr.data.local.entity.LogoEntity
import com.example.logoocr.data.local.entity.RecognitionResultEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class LogoRepositoryImpl @Inject constructor(
    private val brandDao: BrandDao,
    private val logoDao: LogoDao,
    private val recognitionResultDao: RecognitionResultDao
) : LogoRepository {
    override fun observeBrands(): Flow<List<BrandEntity>> = brandDao.observeBrands()

    override fun observeLogos(brandId: Long): Flow<List<LogoEntity>> =
        logoDao.observeLogosByBrand(brandId)

    override fun observeRecognitionHistory(): Flow<List<RecognitionResultEntity>> =
        recognitionResultDao.observeAll()

    override fun observeRecognitionResult(id: Long): Flow<RecognitionResultEntity?> =
        recognitionResultDao.observeById(id)

    override suspend fun upsertBrand(brand: BrandEntity): Long = brandDao.upsert(brand)

    override suspend fun upsertLogo(logo: LogoEntity): Long = logoDao.upsert(logo)

    override suspend fun insertRecognitionResult(result: RecognitionResultEntity): Long =
        recognitionResultDao.insert(result)

    override suspend fun markRecognitionResult(id: Long, verified: Boolean) {
        recognitionResultDao.updateVerification(id, verified)
    }

    override suspend fun getBrandByName(name: String): BrandEntity? = brandDao.findByName(name)

    override suspend fun getBrandById(id: Long): BrandEntity? = brandDao.getById(id)

    override suspend fun getLogoById(id: Long): LogoEntity? = logoDao.getById(id)

    override suspend fun getAllLogos(): List<LogoEntity> = logoDao.getAll()
}
