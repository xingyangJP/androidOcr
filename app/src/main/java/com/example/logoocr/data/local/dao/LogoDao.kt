package com.example.logoocr.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.example.logoocr.data.local.entity.LogoEntity

@Dao
interface LogoDao {
    @Query("SELECT * FROM logos WHERE brandId = :brandId ORDER BY registeredAt DESC")
    fun observeLogosByBrand(brandId: Long): Flow<List<LogoEntity>>

    @Query("SELECT * FROM logos ORDER BY registeredAt DESC")
    fun observeAll(): Flow<List<LogoEntity>>

    @Query("SELECT * FROM logos")
    suspend fun getAll(): List<LogoEntity>

    @Query("SELECT * FROM logos WHERE logoId = :id LIMIT 1")
    suspend fun getById(id: Long): LogoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(logo: LogoEntity): Long
}
