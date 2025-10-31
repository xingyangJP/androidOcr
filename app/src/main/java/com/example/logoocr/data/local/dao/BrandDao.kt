package com.example.logoocr.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.example.logoocr.data.local.entity.BrandEntity

@Dao
interface BrandDao {
    @Query("SELECT * FROM brands ORDER BY name ASC")
    fun observeBrands(): Flow<List<BrandEntity>>

    @Query("SELECT * FROM brands WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): BrandEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(brand: BrandEntity): Long

    @Query("SELECT * FROM brands WHERE brandId = :id LIMIT 1")
    suspend fun getById(id: Long): BrandEntity?
}
