package com.sunjk.sunjktool.feature.deepseek

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.domain.model.BalanceHistoryPoint
import com.sunjk.sunjktool.domain.model.DeepSeekBalance
import com.sunjk.sunjktool.domain.repository.DeepSeekRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
data class DeepSeekUiState(
    val balance: DeepSeekBalance = DeepSeekBalance(),
    val history: List<BalanceHistoryPoint> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class DeepSeekViewModel(
    private val repository: DeepSeekRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeepSeekUiState())
    val uiState: StateFlow<DeepSeekUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.balance.collect { b ->
                _uiState.value = _uiState.value.copy(balance = b, isLoading = false)
            }
        }
        viewModelScope.launch {
            repository.getHistory(7).collect { pts ->
                _uiState.value = _uiState.value.copy(history = pts)
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repository.refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }
}
