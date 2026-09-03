package com.sunjk.sunjktool.feature.review

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.local.dao.LogEntryDao
import com.sunjk.sunjktool.data.local.dao.ReviewStatusDao
import com.sunjk.sunjktool.data.sync.SyncTrigger
import com.sunjk.sunjktool.data.model.LogEntryEntity
import com.sunjk.sunjktool.data.model.ReviewStatusEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReviewItem(
    val statusId: Long,
    val logEntryId: Long,
    val title: String,
    val subject: String,
    val isCompleted: Boolean,
    val reviewType: String
)

data class ReviewDay(
    val date: Long,
    val items: List<ReviewItem>
)

@Immutable
data class ReviewListUiState(
    val isLoading: Boolean = true,
    val days: List<ReviewDay> = emptyList()
)

class ReviewListViewModel(
    private val reviewDao: ReviewStatusDao,
    private val logRepository: com.sunjk.sunjktool.domain.repository.LogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewListUiState())
    val uiState: StateFlow<ReviewListUiState> = _uiState.asStateFlow()

    init {
        val todayMillis = java.time.LocalDate.now()
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        viewModelScope.launch {
            combine(reviewDao.getAll(), logRepository.getAllEntries()) { reviews, entries ->
                val entryMap = entries.associateBy { it.id }
                // Only show today and future review dates
                val grouped = reviews
                    .filter { it.reviewDate >= todayMillis }
                    .groupBy { it.reviewDate }.map { (date, list) ->
                    ReviewDay(date = date, items = list.mapNotNull { r ->
                        val e = entryMap[r.logEntryId] ?: return@mapNotNull null
                        ReviewItem(r.id, e.id, e.title, e.notebookName, r.isCompleted, r.reviewType)
                    }.sortedBy { it.logEntryId })
                }.sortedBy { it.date }
                _uiState.value = ReviewListUiState(isLoading = false, days = grouped)
            }.collect { }
        }
    }

    fun toggleReview(id: Long, completed: Boolean) {
        viewModelScope.launch {
            reviewDao.setCompleted(id, completed)
            SyncTrigger.requestAutoSync()
            SyncTrigger.bumpEntity("review_status")
        }
    }
}
