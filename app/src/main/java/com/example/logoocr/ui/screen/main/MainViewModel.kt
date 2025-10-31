package com.example.logoocr.ui.screen.main

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logoocr.core.image.ImageEmbedding
import com.example.logoocr.core.ml.LogoClassifier
import com.example.logoocr.data.local.entity.RecognitionResultEntity
import com.example.logoocr.data.repository.LogoRepository
import java.io.File
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: LogoRepository,
    private val logoClassifier: LogoClassifier
) : ViewModel() {

    data class RecognitionSummary(
        val resultId: Long,
        val brandName: String?,
        val confidence: Float,
        val createdAt: Long,
        val message: String
    )

    data class MainUiState(
        val isProcessing: Boolean = false,
        val hasLogos: Boolean = false,
        val message: String? = null,
        val lastRecognition: RecognitionSummary? = null
    )

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _navigateToConfirm = MutableSharedFlow<Long>()
    val navigateToConfirm = _navigateToConfirm.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.observeBrands().collect { brands ->
                _uiState.update { it.copy(hasLogos = brands.isNotEmpty()) }
            }
        }
    }

    fun resetMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun onImageCaptured(file: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, message = null) }
            val result = withContext(Dispatchers.IO) { classifyCapturedImage(file) }
            when (result) {
                is RecognitionResult.Success -> {
                    val summary = RecognitionSummary(
                        resultId = result.resultId,
                        brandName = result.brandName,
                        confidence = result.confidence,
                        createdAt = result.createdAt,
                        message = if (result.brandName != null) {
                            "最も近いロゴ: ${result.brandName}"
                        } else {
                            "既存ロゴと一致しませんでした"
                        }
                    )
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            lastRecognition = summary,
                            message = null
                        )
                    }
                    _navigateToConfirm.emit(result.resultId)
                }
                is RecognitionResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            message = result.message
                        )
                    }
                }
            }
        }
    }

    private suspend fun classifyCapturedImage(file: File): RecognitionResult {
        val embedding = logoClassifier.computeEmbedding(file)
            ?: ImageEmbedding.fromFile(file)
            ?: return RecognitionResult.Error("画像の読み込みに失敗しました")

        val logos = repository.getAllLogos()
        if (logos.isEmpty()) {
            return RecognitionResult.Error("登録済みのロゴがありません。先にロゴを登録してください。")
        }

        var bestSimilarity = Float.MIN_VALUE
        var bestLogoId: Long? = null
        var bestBrandId: Long? = null

        logos.forEach { logo ->
            val storedEmbedding = logo.embedding
            val similarity = ImageEmbedding.cosineSimilarity(embedding, storedEmbedding)
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity
                bestLogoId = logo.logoId
                bestBrandId = logo.brandId
            }
        }

        val threshold = 0.6f
        val matchedBrandName = if (bestSimilarity >= threshold && bestBrandId != null) {
            repository.getBrandById(bestBrandId!!)?.name
        } else {
            null
        }

        val createdAt = System.currentTimeMillis()
        val insertedId = repository.insertRecognitionResult(
            RecognitionResultEntity(
                imagePath = file.absolutePath,
                detectedBrandId = if (bestSimilarity >= threshold) bestBrandId else null,
                matchedLogoId = if (bestSimilarity >= threshold) bestLogoId else null,
                detectedText = null,
                confidence = if (bestSimilarity >= 0f) bestSimilarity else 0f,
                verified = null,
                createdAt = createdAt
            )
        )

        return RecognitionResult.Success(
            resultId = insertedId,
            brandName = matchedBrandName,
            confidence = bestSimilarity.coerceIn(0f, 1f),
            createdAt = createdAt
        )
    }

    private sealed interface RecognitionResult {
        data class Success(
            val resultId: Long,
            val brandName: String?,
            val confidence: Float,
            val createdAt: Long
        ) : RecognitionResult

        data class Error(val message: String) : RecognitionResult
    }
}
