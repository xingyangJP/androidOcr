package com.example.logoocr.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.example.logoocr.data.local.entity.RecognitionResultEntity

@Dao
interface RecognitionResultDao {
    @Query("SELECT * FROM recognition_results ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RecognitionResultEntity>>

    @Query("SELECT * FROM recognition_results WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<RecognitionResultEntity?>

    @Query("SELECT * FROM recognition_results WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RecognitionResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecognitionResultEntity): Long

    @Query("UPDATE recognition_results SET verified = :verified WHERE id = :id")
    suspend fun updateVerification(id: Long, verified: Boolean)
}
