package com.example.logoocr.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logoocr.data.repository.LogoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: LogoRepository
) : ViewModel() {

    data class HistoryItemUi(
        val id: Long,
        val brandName: String,
        val statusLabel: String,
        val createdAtLabel: String,
        val imagePath: String?
    )

    data class HistoryUiState(
        val items: List<HistoryItemUi> = emptyList(),
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN)

    init {
        viewModelScope.launch {
            repository.observeRecognitionHistory()
                .mapLatest { list ->
                    list.map { entity ->
                        val brandName = entity.detectedBrandId?.let { id ->
                            repository.getBrandById(id)?.name
                        } ?: "未分類"

                        val status = when (entity.verified) {
                            true -> "確認済み: 正"
                            false -> "確認済み: 誤"
                            null -> "未確認"
                        }

                        HistoryItemUi(
                            id = entity.id,
                            brandName = brandName,
                            statusLabel = status,
                            createdAtLabel = formatter.format(Date(entity.createdAt)),
                            imagePath = entity.imagePath
                        )
                    }
                }
                .collect { items ->
                    _uiState.update {
                        it.copy(items = items, isLoading = false)
                    }
                }
        }
    }
}
