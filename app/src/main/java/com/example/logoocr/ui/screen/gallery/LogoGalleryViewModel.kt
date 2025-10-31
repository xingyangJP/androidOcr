package com.example.logoocr.ui.screen.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logoocr.data.repository.LogoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltViewModel
class LogoGalleryViewModel @Inject constructor(
    private val repository: LogoRepository
) : ViewModel() {

    data class LogoItemUi(
        val id: Long,
        val brandName: String,
        val description: String?,
        val imagePath: String?,
        val registeredAtLabel: String
    )

    data class LogoGalleryUiState(
        val items: List<LogoItemUi> = emptyList(),
        val isLoading: Boolean = true
    )

    private val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN)

    private val _uiState = MutableStateFlow(LogoGalleryUiState())
    val uiState: StateFlow<LogoGalleryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeAllLogos(),
                repository.observeBrands()
            ) { logos, brands ->
                val brandMap = brands.associateBy { it.brandId }
                logos.map { logo ->
                    LogoItemUi(
                        id = logo.logoId,
                        brandName = brandMap[logo.brandId]?.name ?: "未分類",
                        description = logo.description,
                        imagePath = logo.imagePath,
                        registeredAtLabel = formatter.format(Date(logo.registeredAt))
                    )
                }
            }.collect { items ->
                _uiState.update {
                    it.copy(
                        items = items,
                        isLoading = false
                    )
                }
            }
        }
    }
}
