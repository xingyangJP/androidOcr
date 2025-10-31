package com.example.logoocr.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recognition_results",
    foreignKeys = [
        ForeignKey(
            entity = LogoEntity::class,
            parentColumns = ["logoId"],
            childColumns = ["matchedLogoId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["matchedLogoId"])
    ]
)
data class RecognitionResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imagePath: String,
    val detectedBrandId: Long?,
    val matchedLogoId: Long?,
    val detectedText: String?,
    val confidence: Float,
    val verified: Boolean?, // null = 未確認, true = 正しい, false = 誤り
    val createdAt: Long = System.currentTimeMillis()
)
