package com.sunjk.sunjktool.feature.countdown.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.domain.model.Countdown
import com.sunjk.sunjktool.domain.repository.CountdownRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class CountdownListUiState(
    val items: List<Countdown> = emptyList(),
    val isLoading: Boolean = true
)

class CountdownListViewModel(
    private val repository: CountdownRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CountdownListUiState())
    val uiState: StateFlow<CountdownListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAll().collect { items ->
                _uiState.update { it.copy(items = items, isLoading = false) }
            }
        }
    }
}
