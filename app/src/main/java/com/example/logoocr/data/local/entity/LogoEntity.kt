package com.example.logoocr.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "logos",
    foreignKeys = [
        ForeignKey(
            entity = BrandEntity::class,
            parentColumns = ["brandId"],
            childColumns = ["brandId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["brandId"])
    ]
)
data class LogoEntity(
    @PrimaryKey(autoGenerate = true) val logoId: Long = 0,
    val brandId: Long,
    val imagePath: String,
    val embedding: FloatArray,
    val description: String? = null,
    val registeredAt: Long = System.currentTimeMillis()
)
