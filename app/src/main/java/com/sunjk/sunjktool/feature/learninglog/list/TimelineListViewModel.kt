package com.sunjk.sunjktool.feature.learninglog.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.domain.model.LogEntry
import com.sunjk.sunjktool.domain.model.TimelineDay
import com.sunjk.sunjktool.domain.repository.LogRepository
import com.sunjk.sunjktool.util.PomodoroManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class TimelineListUiState(
    val isLoading: Boolean = true,
    val days: List<TimelineDay> = emptyList(),
    val todayFocusSecs: Long = 0L,
    val searchQuery: String = ""
)

class TimelineListViewModel(
    logRepository: LogRepository,
    pomodoroManager: PomodoroManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineListUiState())
    val uiState: StateFlow<TimelineListUiState> = _uiState.asStateFlow()

    private var allEntries: List<LogEntry> = emptyList()

    init {
        viewModelScope.launch {
            logRepository.getAllEntries().collect { entries: List<LogEntry> ->
                allEntries = entries
                applyFilter(entries, _uiState.value.searchQuery)
            }
        }
        viewModelScope.launch {
            pomodoroManager.state.collect { ps ->
                _uiState.value = _uiState.value.copy(todayFocusSecs = ps.totalFocusSecs)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilter(allEntries, query)
    }

    private fun applyFilter(entries: List<LogEntry>, query: String) {
        val filtered = if (query.isBlank()) entries
        else entries.filter { entry ->
            entry.title.contains(query, ignoreCase = true) ||
            entry.subject.contains(query, ignoreCase = true) ||
            entry.description.contains(query, ignoreCase = true) ||
            entry.aiSummary.contains(query, ignoreCase = true)
        }
        viewModelScope.launch(Dispatchers.Default) {
            val grouped = filtered
                .groupBy { it.createdDate.toLocalDate() }
                .map { (date, list) -> TimelineDay(date, list) }
                .sortedByDescending { it.date }
            _uiState.value = _uiState.value.copy(isLoading = false, days = grouped)
        }
    }
}
