package com.sunjk.sunjktool.feature.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.domain.model.LogEntry
import com.sunjk.sunjktool.domain.repository.LogRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Immutable
data class HomeUiState(
    val entries: List<LogEntry> = emptyList(),
    val heatmapData: Map<LocalDate, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class HomeViewModel(
    private val repository: LogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllEntries().collect { entries ->
                val heatmap = withContext(Dispatchers.Default) { computeHeatmap(entries) }
                _uiState.update {
                    it.copy(
                        entries = entries,
                        heatmapData = heatmap,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun computeHeatmap(entries: List<LogEntry>): Map<LocalDate, Int> {
        val today = LocalDate.now()
        val since = today.minus(12, ChronoUnit.WEEKS).with(java.time.DayOfWeek.MONDAY)
        return entries
            .filter { it.createdDate.toLocalDate() >= since }
            .groupBy { it.createdDate.toLocalDate() }
            .mapValues { it.value.size }
    }
}
