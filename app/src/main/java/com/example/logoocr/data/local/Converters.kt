package com.example.logoocr.data.local

import androidx.room.TypeConverter

private const val DELIMITER = ","

class Converters {
    @TypeConverter
    fun fromStringToFloatArray(value: String?): FloatArray? {
        if (value.isNullOrBlank()) return null
        return value.split(DELIMITER).mapNotNull { it.toFloatOrNull() }.toFloatArray()
    }

    @TypeConverter
    fun fromFloatArrayToString(array: FloatArray?): String? {
        return array?.joinToString(separator = DELIMITER)
    }
}
