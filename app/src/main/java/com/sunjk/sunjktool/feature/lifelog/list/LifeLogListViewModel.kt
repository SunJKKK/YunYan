package com.sunjk.sunjktool.feature.lifelog.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.domain.model.LifeLogEntry
import com.sunjk.sunjktool.domain.model.LifeLogTimelineDay
import com.sunjk.sunjktool.domain.repository.LifeLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class LifeLogListUiState(
    val isLoading: Boolean = true,
    val days: List<LifeLogTimelineDay> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false
)

class LifeLogListViewModel(
    private val repository: LifeLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LifeLogListUiState())
    val uiState: StateFlow<LifeLogListUiState> = _uiState.asStateFlow()

    private val allEntries = MutableStateFlow<List<LifeLogEntry>>(emptyList())

    init {
        viewModelScope.launch {
            repository.getAllEntries().collect { entries ->
                allEntries.value = entries
                applyFilter(entries, _uiState.value.searchQuery)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilter(allEntries.value, query)
    }

    fun toggleSearch() {
        val newState = !_uiState.value.isSearchActive
        _uiState.update { it.copy(isSearchActive = newState, searchQuery = "") }
        if (!newState) applyFilter(allEntries.value, "")
    }

    private fun applyFilter(entries: List<LifeLogEntry>, query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val filtered = withContext(Dispatchers.Default) {
                val matched = if (query.isBlank()) entries
                else entries.filter { entry ->
                    entry.content.contains(query, ignoreCase = true)
                }
                matched.groupBy { it.createdDate.toLocalDate() }
                    .map { (date, list) -> LifeLogTimelineDay(date, list.sortedByDescending { it.createdDate }) }
                    .sortedByDescending { it.date }
            }
            _uiState.update { it.copy(days = filtered, isLoading = false) }
        }
    }
}
