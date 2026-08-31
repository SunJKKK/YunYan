package com.sunjk.sunjktool.feature.overview

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.local.dao.PomodoroRecordDao
import com.sunjk.sunjktool.data.local.dao.ReviewStatusDao
import com.sunjk.sunjktool.domain.model.Habit
import com.sunjk.sunjktool.domain.model.LifeLogEntry
import com.sunjk.sunjktool.domain.model.LogEntry
import com.sunjk.sunjktool.data.remote.TickTickTask
import com.sunjk.sunjktool.domain.repository.HabitRepository
import com.sunjk.sunjktool.domain.repository.LifeLogRepository
import com.sunjk.sunjktool.domain.repository.LogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

data class HabitWithStatus(
    val habit: Habit,
    val isCompleted: Boolean
)

data class OverviewReviewItem(
    val statusId: Long,
    val logEntryId: Long,
    val title: String,
    val subject: String,
    val reviewType: String,
    val isCompleted: Boolean
)

@Immutable
data class OverviewUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val markedDates: Set<LocalDate> = emptySet(),
    val logEntries: List<LogEntry> = emptyList(),
    val logEntryCount: Int = 0,
    val focusSecs: Long = 0L,
    val habits: List<HabitWithStatus> = emptyList(),
    val reviewTasks: List<OverviewReviewItem> = emptyList(),
    val lifeLogs: List<LifeLogEntry> = emptyList(),
    val todoTasks: List<TickTickTask> = emptyList(),
    val isLoading: Boolean = true
)

class OverviewViewModel(
    private val logRepository: LogRepository,
    private val pomodoroRecordDao: PomodoroRecordDao,
    private val habitRepository: HabitRepository,
    private val reviewStatusDao: ReviewStatusDao,
    private val lifeLogRepository: LifeLogRepository,
    private val tickTickRepository: com.sunjk.sunjktool.domain.repository.TickTickRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverviewUiState())
    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()

    private var allLogEntries: List<LogEntry> = emptyList()
    private var allLifeLogs: List<LifeLogEntry> = emptyList()
    private var allTodoTasks: List<TickTickTask> = emptyList()

    init {
        viewModelScope.launch {
            logRepository.getAllEntries().collect { logs ->
                allLogEntries = logs
                val markedDates = logs.map { it.createdDate.toLocalDate() }.toSet()
                _uiState.update { it.copy(markedDates = markedDates) }
                loadDateData(_uiState.value.selectedDate)
            }
        }
        viewModelScope.launch {
            lifeLogRepository.getAllEntries().collect { logs ->
                allLifeLogs = logs
                loadDateData(_uiState.value.selectedDate)
            }
        }
        // 滴答清单本地缓存（含已完成任务的全量同步结果），按选中日期过滤展示
        tickTickRepository?.let { repo ->
            if (!repo.isConfigured) return@let
            viewModelScope.launch {
                repo.tasks.collect { tasks ->
                    allTodoTasks = tasks
                    loadDateData(_uiState.value.selectedDate)
                }
            }
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        loadDateData(date)
    }

    fun toggleHabit(habitId: Long) {
        viewModelScope.launch {
            habitRepository.toggleRecord(habitId, _uiState.value.selectedDate)
            loadDateData(_uiState.value.selectedDate)
        }
    }

    private fun loadDateData(date: LocalDate) {
        viewModelScope.launch {
            val logs = withContext(Dispatchers.Default) {
                allLogEntries.filter { it.createdDate.toLocalDate() == date }
                    .sortedByDescending { it.createdDate }
            }
            val lifeLogs = withContext(Dispatchers.Default) {
                allLifeLogs.filter { it.createdDate.toLocalDate() == date }
                    .sortedByDescending { it.createdDate }
            }
            // 滴答清单：已完成任务是全量返回的，只保留截止日期为当天的任务
            val todoTasks = withContext(Dispatchers.Default) {
                allTodoTasks.filter { it.dueDate == date.toString() }
                    .sortedWith(compareBy({ it.isCompleted }, { -it.priority }))
            }
            val epochDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val reviews = reviewStatusDao.getByDate(epochDay).first().map { r ->
                val entry = allLogEntries.firstOrNull { it.id == r.logEntryId }
                OverviewReviewItem(
                    statusId = r.id,
                    logEntryId = r.logEntryId,
                    title = entry?.title ?: "学习记录",
                    subject = entry?.subject ?: "",
                    reviewType = r.reviewType,
                    isCompleted = r.isCompleted
                )
            }
            val habits = habitRepository.getAll().first()
            val records = habitRepository.getAllRecords().first()
            val focusSecs = pomodoroRecordDao.getByDate(date.toString())?.focusSecs ?: 0L
            val habitsWithStatus = habits.map { h ->
                val key = "${h.id}_$date"
                val completed = records.find { it.date == key }?.isCompleted ?: false
                HabitWithStatus(h, completed)
            }
            _uiState.update {
                it.copy(
                    logEntries = logs, logEntryCount = logs.size,
                    lifeLogs = lifeLogs,
                    todoTasks = todoTasks,
                    reviewTasks = reviews,
                    habits = habitsWithStatus,
                    focusSecs = focusSecs,
                    isLoading = false
                )
            }
        }
    }
}
