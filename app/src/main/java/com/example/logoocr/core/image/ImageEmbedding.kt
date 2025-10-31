package com.example.logoocr.core.image

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.annotation.WorkerThread
import java.io.File
import java.io.InputStream

object ImageEmbedding {

    private const val TARGET_SIZE = 64
    const val EMBEDDING_SIZE = 64

    @WorkerThread
    fun fromFile(file: File): FloatArray? {
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)?.let { bitmap ->
            computeEmbedding(bitmap)
        }
    }

    @WorkerThread
    fun fromUri(context: Context, uri: Uri): FloatArray? {
        val bitmap = decodeBitmapFromUri(context, uri) ?: return null
        return computeEmbedding(bitmap)
    }

    private fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return runCatching {
            return@runCatching when {
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P -> {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }
                else -> {
                    val stream: InputStream? =
                        context.contentResolver.openInputStream(uri)
                    stream?.use { BitmapFactory.decodeStream(it) }
                }
            }
        }.getOrNull()
    }

    private fun computeEmbedding(bitmap: Bitmap): FloatArray {
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            TARGET_SIZE,
            TARGET_SIZE,
            true
        )

        val histogram = FloatArray(EMBEDDING_SIZE)
        val width = scaled.width
        val height = scaled.height
        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)
        val total = pixels.size.coerceAtLeast(1)

        for (color in pixels) {
            val r = (android.graphics.Color.red(color) / 64).coerceIn(0, 3)
            val g = (android.graphics.Color.green(color) / 64).coerceIn(0, 3)
            val b = (android.graphics.Color.blue(color) / 64).coerceIn(0, 3)
            val index = r * 16 + g * 4 + b
            val current = histogram[index]
            histogram[index] = current + 1f
        }

        // Normalize
        val totalFloat = total.toFloat()
        for (i in histogram.indices) {
            histogram[i] = histogram[i] / totalFloat
        }

        if (scaled !== bitmap) {
            scaled.recycle()
        }

        return histogram
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        val size = minOf(a.size, b.size)
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in 0 until size) {
            val va = a[i]
            val vb = b[i]
            dot += va * vb
            normA += va * va
            normB += vb * vb
        }
        if (normA == 0f || normB == 0f) return 0f
        return (dot / (kotlin.math.sqrt(normA.toDouble()) * kotlin.math.sqrt(normB.toDouble()))).toFloat()
    }
}
