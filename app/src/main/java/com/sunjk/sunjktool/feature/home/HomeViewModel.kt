package com.sunjk.sunjktool.feature.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.model.HomeModuleEntity
import com.sunjk.sunjktool.domain.model.Countdown
import com.sunjk.sunjktool.domain.model.LogEntry
import com.sunjk.sunjktool.domain.model.BalanceHistoryPoint
import com.sunjk.sunjktool.domain.model.DeepSeekBalance
import com.sunjk.sunjktool.domain.model.PomodoroState
import com.sunjk.sunjktool.domain.model.WeatherResult
import com.sunjk.sunjktool.domain.model.Habit
import com.sunjk.sunjktool.domain.model.Notebook
import com.sunjk.sunjktool.data.remote.TickTickTask
import com.sunjk.sunjktool.domain.repository.CountdownRepository
import com.sunjk.sunjktool.domain.repository.HabitRepository
import com.sunjk.sunjktool.domain.repository.HomeModuleRepository
import com.sunjk.sunjktool.domain.repository.LogRepository
import com.sunjk.sunjktool.domain.repository.NotebookRepository
import com.sunjk.sunjktool.domain.repository.TickTickRepository
import com.sunjk.sunjktool.domain.repository.WeatherRepository
import com.sunjk.sunjktool.data.local.dao.ReviewStatusDao
import com.sunjk.sunjktool.domain.repository.DeepSeekRepository
import com.sunjk.sunjktool.util.PomodoroManager
import com.sunjk.sunjktool.util.ReviewHelper
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class HomeReviewItem(
    val statusId: Long,
    val logEntryId: Long,
    val title: String,
    val subject: String,
    val isCompleted: Boolean,
    val reviewType: String
)

data class HomeHabitItem(
    val habit: Habit,
    val isCompleted: Boolean,
    val completedDates: Set<LocalDate> = emptySet()
)

@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val enabledModules: List<HomeModuleEntity> = emptyList(),
    val todayLogs: List<LogEntry> = emptyList(),
    val heatmapData: Map<LocalDate, Int> = emptyMap(),
    val countdownModules: Map<Long, Countdown> = emptyMap(),
    val weatherResult: WeatherResult = WeatherResult.Idle,
    val pomodoroState: PomodoroState = PomodoroState(),
    val deepSeekBalance: DeepSeekBalance = DeepSeekBalance(),
    val deepSeekHistory: List<BalanceHistoryPoint> = emptyList(),
    val reviewItems: List<HomeReviewItem> = emptyList(),
    val habitItems: Map<Long, HomeHabitItem> = emptyMap(),
    val pinnedNotebooks: List<Notebook> = emptyList(),
    val todayTasks: List<TickTickTask> = emptyList(),
    val todoProjects: List<com.sunjk.sunjktool.data.remote.TickTickProject> = emptyList()
)

class HomeViewModel(
    private val logRepository: LogRepository,
    private val homeModuleRepository: HomeModuleRepository,
    private val countdownRepository: CountdownRepository,
    private val weatherRepository: WeatherRepository,
    private val pomodoroManager: PomodoroManager,
    private val deepSeekRepository: DeepSeekRepository,
    reviewHelper: ReviewHelper,
    private val reviewDao: ReviewStatusDao,
    private val habitRepository: HabitRepository,
    private val notebookRepository: NotebookRepository,
    private val tickTickRepository: TickTickRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var weatherLoadedDate: String? = null

    init {
        viewModelScope.launch {
            combine(
                logRepository.getAllEntries(),
                homeModuleRepository.getAll(),
                countdownRepository.getAll(),
                habitRepository.getAll()
            ) { entries: List<LogEntry>,
                modules: List<HomeModuleEntity>,
                countdowns: List<Countdown>,
                habits: List<Habit> ->

                val today = LocalDate.now()
                val todayStr = today.toString() // yyyy-MM-dd
                val heatmap = computeHeatmap(entries)
                val todayLogs = entries.filter { it.createdDate.toLocalDate() == today }
                val cdMap = mutableMapOf<Long, Countdown>()
                for (m in modules) {
                    if (m.enabled && m.moduleKey.startsWith("countdown_")) {
                        val id = m.moduleKey.removePrefix("countdown_").toLongOrNull() ?: continue
                        val cd = countdowns.find { it.id == id } ?: continue
                        cdMap[id] = cd
                    }
                }

                // Fetch weather only once per day
                val weatherEnabled = modules.any { it.moduleKey == "weather" && it.enabled }
                if (weatherEnabled && weatherLoadedDate != todayStr) {
                    weatherLoadedDate = todayStr
                    refreshWeather()
                }

                HomeUiState(
                    isLoading = false,
                    enabledModules = modules.filter { it.enabled },
                    heatmapData = heatmap,
                    todayLogs = todayLogs,
                    countdownModules = cdMap
                )
            }.collect { newState ->
                // Preserve fields managed by other collectors (review, weather, pomodoro, etc.)
                // to avoid the combine emission overwriting them with defaults.
                _uiState.update { current ->
                    newState.copy(
                        reviewItems = current.reviewItems,
                        weatherResult = current.weatherResult,
                        pomodoroState = current.pomodoroState,
                        deepSeekBalance = current.deepSeekBalance,
                        deepSeekHistory = current.deepSeekHistory,
                        habitItems = current.habitItems,
                        pinnedNotebooks = current.pinnedNotebooks,
                        todayTasks = current.todayTasks,
                        todoProjects = current.todoProjects
                    )
                }
            }
        }

        // Collect weather result into UI state
        viewModelScope.launch {
            weatherRepository.weatherResult.collect { result ->
                _uiState.value = _uiState.value.copy(weatherResult = result)
            }
        }
        // Collect pomodoro state
        viewModelScope.launch {
            pomodoroManager.state.collect { ps ->
                _uiState.value = _uiState.value.copy(pomodoroState = ps)
            }
        }
        // Subtitle: delay API call to avoid cold-start network storm
        viewModelScope.launch {
            delay(1500)
    
        }

        // DeepSeek balance + history — merged into one collection
        viewModelScope.launch {
            deepSeekRepository.balance.collect { b ->
                _uiState.value = _uiState.value.copy(deepSeekBalance = b)
            }
        }
        viewModelScope.launch {
            deepSeekRepository.getHistory(7).collect { pts ->
                _uiState.value = _uiState.value.copy(deepSeekHistory = pts)
            }
        }
        // Collect today's review items joined with log entry titles.
        // Driven by reviewDao changes; pulls a fresh log snapshot each time to avoid
        // the combine race where reviewDao emits before logRepository sees the new entry.
        viewModelScope.launch {
            reviewDao.getAll().collect { reviews ->
                val entries = logRepository.getAllEntries().first()
                val (todayDate, _) = reviewHelper.todayReviewDates()
                val todayItems = reviews.filter { it.reviewDate == todayDate }
                val entryMap = entries.associateBy { it.id }
                val items = todayItems.mapNotNull { r ->
                    val e = entryMap[r.logEntryId] ?: return@mapNotNull null
                    HomeReviewItem(r.id, r.logEntryId, e.title, e.subject, r.isCompleted, r.reviewType)
                }
                _uiState.value = _uiState.value.copy(reviewItems = items)
            }
        }

        // Collect today's habit completion status and heatmap data.
        // Combine habits + records so record changes (check-in) trigger UI refresh.
        viewModelScope.launch {
            combine(
                habitRepository.getAll(),
                habitRepository.getAllRecords()
            ) { habits, _ ->
                val today = LocalDate.now()
                val items = mutableMapOf<Long, HomeHabitItem>()
                val since = today.minusWeeks(5).with(java.time.DayOfWeek.MONDAY)
                for (h in habits) {
                    val completed = habitRepository.isCompleted(h.id, today)
                    val records = habitRepository.getRecordsByHabitIdSince(h.id, since).first()
                    val completedDates = records
                        .filter { it.isCompleted }
                        .map { it.date }
                        .toSet()
                    items[h.id] = HomeHabitItem(h, completed, completedDates)
                }
                items.toMap()
            }.collect { items ->
                _uiState.value = _uiState.value.copy(habitItems = items)
            }
        }

        // Collect pinned notebooks for the "笔记本快捷方式" module.
        viewModelScope.launch {
            notebookRepository.getPinned().collect { list ->
                _uiState.value = _uiState.value.copy(pinnedNotebooks = list)
            }
        }

        // Collect today's tasks for the "今日待办" module (due today).
        if (tickTickRepository.isConfigured) {
            viewModelScope.launch {
                tickTickRepository.tasksByDueDate(java.time.LocalDate.now().toString())
                    .collect { tasks ->
                        val filtered = com.sunjk.sunjktool.util.TickTickFilters.applyCompletedMode(
                            tasks, tickTickRepository.completedMode
                        )
                        _uiState.value = _uiState.value.copy(todayTasks = filtered)
                    }
            }
            // 收集清单，用于首页"今日待办"的新建任务与分组标注
            viewModelScope.launch {
                tickTickRepository.projects.collect { projects ->
                    _uiState.value = _uiState.value.copy(todoProjects = projects)
                }
            }
        }
    }

    fun pausePomodoro() = pomodoroManager.pause()
    fun resumePomodoro() = pomodoroManager.resume()
    fun stopPomodoro() = pomodoroManager.stop()

    fun toggleReviewItem(id: Long, completed: Boolean) {
        viewModelScope.launch { reviewDao.setCompleted(id, completed) }
    }

    fun toggleHabitCheckIn(habitId: Long) {
        viewModelScope.launch {
            val today = java.time.LocalDate.now()
            habitRepository.toggleRecord(habitId, today)
        }
    }


    fun updateModuleSize(key: String, size: String) {
        viewModelScope.launch {
            homeModuleRepository.updateSize(key, size)
        }
    }    fun refreshDeepSeek() {
        viewModelScope.launch { deepSeekRepository.refresh() }
    }

    fun refreshWeather() {
        viewModelScope.launch {
            weatherRepository.refresh()
        }
    }

    fun refreshTodayTasks() {
        if (!tickTickRepository.isConfigured) return
        viewModelScope.launch {
            try {
                tickTickRepository.refresh()
            } catch (_: Exception) {
                // 拉取失败不崩溃，仅跳过本次刷新
            }
        }
    }

    fun toggleTodoTask(task: com.sunjk.sunjktool.data.remote.TickTickTask) {
        viewModelScope.launch {
            tickTickRepository.toggleComplete(task)
        }
    }

    fun createTodoTask(title: String, projectId: String?, dueDate: String?) {
        val pid = projectId ?: _uiState.value.todoProjects.firstOrNull()?.id ?: ""
        viewModelScope.launch {
            tickTickRepository.createTask(title, pid, dueDate)
            refreshTodayTasks()
        }
    }

    /** Called every time the home screen becomes visible. Refreshes non-weather data sources. */
    fun refreshAll() {

        refreshDeepSeek()
        refreshTodayTasks()
    }

    /**
     * Pull-to-refresh entry point: refreshes ALL home widget states.
     * Weather + DeepSeek + TickTick are network-backed and need an explicit refresh;
     * local DB widgets (heatmap, logs, countdown, habits, notebooks, review, pomodoro)
     * update automatically through their reactive flows.
     */
    fun refreshAllHome() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                coroutineScope {
                    launch { runCatching { weatherRepository.refresh() } }
                    launch { runCatching { deepSeekRepository.refresh() } }
                    if (tickTickRepository.isConfigured) {
                        launch { runCatching { tickTickRepository.refresh() } }
                    }
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private var cachedHeatmapHash = 0
    private var cachedHeatmap: Map<LocalDate, Int> = emptyMap()

    private fun computeHeatmap(entries: List<LogEntry>): Map<LocalDate, Int> {
        val hash = entries.hashCode()
        if (hash == cachedHeatmapHash) return cachedHeatmap
        cachedHeatmapHash = hash
        val today = LocalDate.now()
        val since = today.minus(12, ChronoUnit.WEEKS).with(java.time.DayOfWeek.MONDAY)
        cachedHeatmap = entries
            .filter { it.createdDate.toLocalDate() >= since }
            .groupBy { it.createdDate.toLocalDate() }
            .mapValues { it.value.size }
        return cachedHeatmap
    }
}
