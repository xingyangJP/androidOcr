package com.example.logoocr.ui.screen.confirm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logoocr.data.repository.LogoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapLatest
import java.text.DecimalFormat

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ConfirmRecognitionViewModel @Inject constructor(
    private val repository: LogoRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    data class UiState(
        val brandName: String = "未分類",
        val confidenceLabel: String = "-",
        val imagePath: String? = null,
        val status: String = "未確認",
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val resultId: Long? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val decimalFormat = DecimalFormat("#0.00")

    private val resultId: Long? = savedStateHandle["resultId"]

    init {
        val id = resultId
        if (id == null || id < 0) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "結果IDが無効です") }
        } else {
            _uiState.update { it.copy(resultId = id) }
            observeResult(id)
        }
    }

    private fun observeResult(id: Long) {
        viewModelScope.launch {
            repository.observeRecognitionResult(id)
                .filterNotNull()
                .mapLatest { entity ->
                    val brandName = entity.detectedBrandId?.let { repository.getBrandById(it)?.name } ?: "未分類"
                    val status = when (entity.verified) {
                        true -> "確認済み: 正"
                        false -> "確認済み: 誤"
                        null -> "未確認"
                    }
                    UiState(
                        brandName = brandName,
                        confidenceLabel = decimalFormat.format(entity.confidence * 100f) + " %",
                        imagePath = entity.imagePath,
                        status = status,
                        isLoading = false,
                        errorMessage = null,
                        resultId = entity.id
                    )
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun markResult(isCorrect: Boolean) {
        val id = _uiState.value.resultId ?: return
        viewModelScope.launch {
            repository.markRecognitionResult(id, isCorrect)
        }
    }
}
