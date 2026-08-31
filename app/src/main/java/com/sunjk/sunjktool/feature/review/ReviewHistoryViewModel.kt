package com.sunjk.sunjktool.feature.review

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.local.dao.LogEntryDao
import com.sunjk.sunjktool.data.local.dao.ReviewStatusDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@Immutable
data class ReviewHistoryUiState(
    val isLoading: Boolean = true,
    val days: List<ReviewDay> = emptyList()
)

class ReviewHistoryViewModel(
    private val reviewDao: ReviewStatusDao,
    private val logDao: LogEntryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewHistoryUiState())
    val uiState: StateFlow<ReviewHistoryUiState> = _uiState.asStateFlow()

    init {
        val todayMillis = java.time.LocalDate.now()
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        viewModelScope.launch {
            combine(reviewDao.getAll(), logDao.getAllEntries()) { reviews, entries ->
                val entryMap = entries.associateBy { it.id }
                // Only show past review dates (before today)
                val grouped = reviews
                    .filter { it.reviewDate < todayMillis }
                    .groupBy { it.reviewDate }
                    .map { (date, list) ->
                        ReviewDay(
                            date = date,
                            items = list.mapNotNull { r ->
                                val e = entryMap[r.logEntryId] ?: return@mapNotNull null
                                ReviewItem(r.id, e.id, e.title, e.subject, r.isCompleted, r.reviewType)
                            }.sortedBy { it.logEntryId }
                        )
                    }
                    .sortedByDescending { it.date } // newest first for history
                _uiState.value = ReviewHistoryUiState(isLoading = false, days = grouped)
            }.collect { }
        }
    }
}
