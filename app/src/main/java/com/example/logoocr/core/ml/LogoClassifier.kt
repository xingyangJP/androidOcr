package com.example.logoocr.core.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File

/**
 * TensorFlow Lite ベースのロゴ特徴量抽出器。
 * `assets/logo_classifier.tflite` が存在しない場合は利用不可として null を返す。
 */
@Singleton
class LogoClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val interpreter: Interpreter? by lazy { loadInterpreter() }

    private val inputShape: IntArray? by lazy {
        interpreter?.getInputTensor(0)?.shape()
    }

    private val outputShape: IntArray? by lazy {
        interpreter?.getOutputTensor(0)?.shape()
    }

    private val outputDataType by lazy {
        interpreter?.getOutputTensor(0)?.dataType()
    }

    val isAvailable: Boolean
        get() = interpreter != null

    suspend fun computeEmbedding(file: File): FloatArray? = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext null
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext null
        val embedding = computeEmbeddingInternal(bitmap)
        if (bitmap != null && !bitmap.isRecycled) {
            bitmap.recycle()
        }
        embedding
    }

    suspend fun computeEmbedding(bitmap: Bitmap): FloatArray? = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext null
        computeEmbeddingInternal(bitmap)
    }

    private fun computeEmbeddingInternal(sourceBitmap: Bitmap): FloatArray? {
        val interpreter = interpreter ?: return null
        val inputShape = inputShape ?: return null
        val outputShape = outputShape ?: return null
        val outputDataType = outputDataType ?: return null

        // 入力テンソルは [1, height, width, channels] 前提
        if (inputShape.size < 4) return null
        val targetHeight = inputShape[1]
        val targetWidth = inputShape[2]

        val imageProcessor = ImageProcessor.Builder()
            .add(
                ResizeOp(
                    targetHeight,
                    targetWidth,
                    ResizeOp.ResizeMethod.BILINEAR
                )
            )
            .build()

        var tensorImage = TensorImage(interpreter.getInputTensor(0).dataType())
        tensorImage.load(sourceBitmap)
        tensorImage = imageProcessor.process(tensorImage)

        val outputBuffer = TensorBuffer.createFixedSize(outputShape, outputDataType)

        interpreter.run(tensorImage.buffer, outputBuffer.buffer)

        return when (outputDataType) {
            org.tensorflow.lite.DataType.FLOAT32 -> outputBuffer.floatArray
            org.tensorflow.lite.DataType.UINT8 -> {
                outputBuffer.byteArray.map { (it.toInt() and 0xFF).toFloat() / 255f }.toFloatArray()
            }
            else -> null
        }
    }

    private fun loadInterpreter(): Interpreter? {
        return try {
            val mappedModel = FileUtil.loadMappedFile(context, MODEL_FILE_NAME)
            Interpreter(mappedModel, Interpreter.Options())
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val MODEL_FILE_NAME = "logo_classifier.tflite"
    }
}
