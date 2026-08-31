package com.sunjk.sunjktool.feature.weather.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.domain.model.WeatherBundle
import com.sunjk.sunjktool.domain.model.WeatherResult
import com.sunjk.sunjktool.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class WeatherDetailUiState(
    val weatherDetail: WeatherBundle? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class WeatherDetailViewModel(
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherDetailUiState())
    val uiState: StateFlow<WeatherDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            weatherRepository.weatherResult.collect { result ->
                when (result) {
                    is WeatherResult.Idle -> {
                        // If never loaded yet, trigger refresh
                        if (_uiState.value.weatherDetail == null) refresh()
                        else _uiState.update { it.copy(isLoading = false) }
                    }
                    is WeatherResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is WeatherResult.Success -> {
                        _uiState.update { it.copy(isLoading = false, weatherDetail = result.data, error = null) }
                    }
                    is WeatherResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            weatherRepository.refresh()
        }
    }
}
