package com.example.logoocr.ui.screen.register

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logoocr.core.image.ImageEmbedding
import com.example.logoocr.core.ml.LogoClassifier
import com.example.logoocr.core.storage.ImageStorage
import com.example.logoocr.data.local.entity.BrandEntity
import com.example.logoocr.data.local.entity.LogoEntity
import com.example.logoocr.data.repository.LogoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@HiltViewModel
class RegisterLogoViewModel @Inject constructor(
    private val repository: LogoRepository,
    @ApplicationContext private val context: Context,
    private val logoClassifier: LogoClassifier
) : ViewModel() {

    data class UiState(
        val brandName: String = "",
        val description: String = "",
        val imagePath: String? = null,
        val previewUri: Uri? = null,
        val isProcessing: Boolean = false,
        val errorMessage: String? = null,
        val isCompleted: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var cachedEmbedding: FloatArray? = null

    fun onBrandNameChange(value: String) {
        _uiState.update { it.copy(brandName = value, errorMessage = null) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun setImageFromCamera(file: File) {
        viewModelScope.launch {
            processImage(uri = Uri.fromFile(file), path = file.absolutePath, deleteOnFailure = true)
        }
    }

    fun setImageFromGallery(uri: Uri) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                ImageStorage.copyToInternalStorage(context, uri, "gallery")
            }
            if (path == null) {
                _uiState.update { it.copy(errorMessage = "画像の読み込みに失敗しました") }
                return@launch
            }
            processImage(uri = Uri.fromFile(File(path)), path = path, deleteOnFailure = false)
        }
    }

    private suspend fun processImage(uri: Uri, path: String, deleteOnFailure: Boolean) {
        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
        val embedding = withContext(Dispatchers.IO) {
            logoClassifier.computeEmbedding(File(path))
                ?: ImageEmbedding.fromFile(File(path))
        }
        if (embedding == null) {
            if (deleteOnFailure) {
                runCatching { File(path).delete() }
            }
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    errorMessage = "特徴量の算出に失敗しました"
                )
            }
            return
        }

        cachedEmbedding = embedding
        _uiState.update {
            it.copy(
                imagePath = path,
                previewUri = uri,
                isProcessing = false,
                errorMessage = null,
                isCompleted = false
            )
        }
    }

    fun saveLogo() {
        val state = _uiState.value
        val brandName = state.brandName.trim()
        if (brandName.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "ブランド名を入力してください") }
            return
        }
        val imagePath = state.imagePath
        val embedding = cachedEmbedding
        if (imagePath == null || embedding == null) {
            _uiState.update { it.copy(errorMessage = "ロゴ画像を選択してください") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            val brandId = withContext(Dispatchers.IO) {
                val existing = repository.getBrandByName(brandName)
                existing?.brandId ?: repository.upsertBrand(BrandEntity(name = brandName))
            }

            withContext(Dispatchers.IO) {
                repository.upsertLogo(
                    LogoEntity(
                        brandId = brandId,
                        imagePath = imagePath,
                        embedding = embedding,
                        description = state.description.takeIf { it.isNotBlank() }
                    )
                )
            }

            _uiState.update {
                it.copy(
                    isProcessing = false,
                    errorMessage = null,
                    isCompleted = true,
                    brandName = "",
                    description = "",
                    imagePath = null,
                    previewUri = null
                )
            }
            cachedEmbedding = null
        }
    }

    fun consumeCompletion() {
        _uiState.update { it.copy(isCompleted = false) }
    }
}
