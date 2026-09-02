package com.sunjk.sunjktool.feature.stats

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.local.dao.PomodoroRecordDao
import com.sunjk.sunjktool.domain.model.LogEntry
import com.sunjk.sunjktool.domain.model.Notebook
import com.sunjk.sunjktool.domain.model.Question
import com.sunjk.sunjktool.domain.repository.LogRepository
import com.sunjk.sunjktool.domain.repository.NotebookRepository
import com.sunjk.sunjktool.domain.repository.QuestionBankRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

enum class StatsTimeRange(val label: String) { WEEK("周"), MONTH("月"), YEAR("年"), ALL("累计") }

enum class NotebookGroupMode { LEAF, TOP }

/** 趋势柱状图中的一根柱子 */
@Immutable
data class StatsBar(
    val label: String,
    /** 时间范围的代表日期，用于按时间排序 */
    val date: LocalDate,
    /** 分钟 */
    val minutes: Long,
    /** 题目模式下的题目数 */
    val count: Int = 0
)

/** 笔记本分布中的一项 */
@Immutable
data class NotebookSlice(
    val name: String,
    val entryCount: Int,
    val fraction: Float
)

@Immutable
data class LearningStatsUiState(
    val isLoading: Boolean = true,
    val timeRange: StatsTimeRange = StatsTimeRange.WEEK,
    val notebookMode: NotebookGroupMode = NotebookGroupMode.LEAF,
    // 累计核心指标（不受时间范围影响）
    val totalLogs: Int = 0,
    val totalFocusMinutes: Long = 0,
    val streakDays: Int = 0,
    val totalQuestions: Int = 0,
    // 专注时长（番茄钟）
    val focusBars: List<StatsBar> = emptyList(),
    val focusTotalMinutes: Long = 0,
    // 笔记本分布
    val notebookSlices: List<NotebookSlice> = emptyList(),
    // 新增题目
    val questionBars: List<StatsBar> = emptyList(),
    val questionTotal: Int = 0,
    // 年度热力图：每日学习记录条数
    val heatmapData: Map<LocalDate, Int> = emptyMap()
)

class LearningStatsViewModel(
    private val logRepository: LogRepository,
    private val pomodoroRecordDao: PomodoroRecordDao,
    private val notebookRepository: NotebookRepository,
    private val questionBankRepository: QuestionBankRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearningStatsUiState())
    val uiState: StateFlow<LearningStatsUiState> = _uiState.asStateFlow()

    private var allLogs: List<LogEntry> = emptyList()
    private var allPomodoros: Map<LocalDate, Long> = emptyMap() // date -> focusSecs
    private var allNotebooks: List<Notebook> = emptyList()
    private var allQuestions: List<Question> = emptyList()

    init {
        viewModelScope.launch {
            logRepository.getAllEntries().collect { logs ->
                allLogs = logs
                recompute()
            }
        }
        viewModelScope.launch {
            pomodoroRecordDao.getAll().collect { records ->
                allPomodoros = records.associate {
                    LocalDate.parse(it.date) to it.focusSecs
                }
                recompute()
            }
        }
        viewModelScope.launch {
            notebookRepository.getAll().collect { notebooks ->
                allNotebooks = notebooks
                recompute()
            }
        }
        viewModelScope.launch {
            questionBankRepository.getAllQuestions().collect { questions ->
                allQuestions = questions
                recompute()
            }
        }
    }

    fun setTimeRange(range: StatsTimeRange) {
        if (range == _uiState.value.timeRange) return
        _uiState.update { it.copy(timeRange = range) }
        recompute()
    }

    fun setNotebookMode(mode: NotebookGroupMode) {
        if (mode == _uiState.value.notebookMode) return
        _uiState.update { it.copy(notebookMode = mode) }
        recompute()
    }

    private fun recompute() {
        viewModelScope.launch {
            val state = _uiState.value
            val today = LocalDate.now()
            val range = state.timeRange

            val focusBars: List<StatsBar>
            val questionBars: List<StatsBar>
            val focusTotal: Long
            var questionTotal = 0
            val notebookSlices: List<NotebookSlice>

            withContext(Dispatchers.Default) {
                // 时间分桶：WEEK/MONTH 按日、YEAR/ALL 按月
                val buckets: List<Pair<LocalDate, String>> = when (range) {
                    StatsTimeRange.WEEK -> (6 downTo 0).map { d ->
                        val date = today.minusDays(d.toLong())
                        date to "${date.dayOfWeek.value}"  // label 由 UI 端转周几
                    }
                    StatsTimeRange.MONTH -> (29 downTo 0).map { d ->
                        val date = today.minusDays(d.toLong())
                        date to "${date.monthValue}/${date.dayOfMonth}"
                    }
                    StatsTimeRange.YEAR -> (11 downTo 0).map { m ->
                        val ym = today.minusMonths(m.toLong()).withDayOfMonth(1)
                        ym to "${ym.monthValue}月"
                    }
                    StatsTimeRange.ALL -> {
                        if (allLogs.isEmpty() && allPomodoros.isEmpty() && allQuestions.isEmpty()) emptyList()
                        else {
                            val min = listOfNotNull(
                                allLogs.minOfOrNull { it.createdDate.toLocalDate() },
                                allPomodoros.keys.minOrNull(),
                                allQuestions.minOfOrNull { it.createdDate.toLocalDate() }
                            ).min()
                            val max = today.withDayOfMonth(1)
                            generateSequence(max) { it.minusMonths(1).withDayOfMonth(1) }
                                .takeWhile { !it.isBefore(min) }
                                .toList()
                                .asReversed()
                                .map { ym -> ym to "${ym.year % 100}/${ym.monthValue}" }
                        }
                    }
                }

                fun inRange(date: LocalDate): Boolean = when (range) {
                    StatsTimeRange.WEEK -> !date.isBefore(today.minusDays(6))
                    StatsTimeRange.MONTH -> !date.isBefore(today.minusDays(29))
                    StatsTimeRange.YEAR -> !date.isBefore(today.minusMonths(11).withDayOfMonth(1))
                    StatsTimeRange.ALL -> true
                }

                val byDay = range == StatsTimeRange.WEEK || range == StatsTimeRange.MONTH

                focusBars = buckets.map { (bucketDate, label) ->
                    val secs = if (byDay) {
                        allPomodoros[bucketDate] ?: 0L
                    } else {
                        allPomodoros.entries.filter { it.key.year == bucketDate.year && it.key.monthValue == bucketDate.monthValue }
                            .sumOf { it.value }
                    }
                    StatsBar(label, bucketDate, secs / 60)
                }
                focusTotal = focusBars.sumOf { it.minutes }

                questionBars = buckets.map { (bucketDate, label) ->
                    val count = if (byDay) {
                        allQuestions.count { it.createdDate.toLocalDate() == bucketDate }
                    } else {
                        allQuestions.count {
                            val d = it.createdDate.toLocalDate()
                            d.year == bucketDate.year && d.monthValue == bucketDate.monthValue
                        }
                    }
                    StatsBar(label, bucketDate, 0, count)
                }
                questionTotal = questionBars.sumOf { it.count }

                // 笔记本分布：统计范围内学习记录按归属笔记本聚合
                val rangeLogs = allLogs.filter { inRange(it.createdDate.toLocalDate()) }
                val byId = allNotebooks.associateBy { it.id }
                notebookSlices = when (state.notebookMode) {
                    NotebookGroupMode.LEAF -> {
                        rangeLogs.groupingBy { it.notebookId }
                            .eachCount()
                            .map { (id, cnt) ->
                                NotebookSlice(byId[id]?.name ?: "未分类", cnt, 0f)
                            }
                    }
                    NotebookGroupMode.TOP -> {
                        // 沿 parentId 链找到根笔记本
                        fun rootOf(nb: Notebook): Notebook {
                            var cur = nb
                            while (cur.parentId != null) {
                                val parent = byId[cur.parentId] ?: return cur
                                cur = parent
                            }
                            return cur
                        }
                        rangeLogs.groupingBy { it.notebookId }
                            .eachCount()
                            .map { (id, cnt) ->
                                val name = byId[id]?.let { rootOf(it).name } ?: "未分类"
                                NotebookSlice(name, cnt, 0f)
                            }
                            .let { slices ->
                                // 根归并后可能出现同名项，合并
                                slices.groupBy { it.name }
                                    .map { (name, list) -> NotebookSlice(name, list.sumOf { it.entryCount }, 0f) }
                            }
                    }
                }
                    .sortedByDescending { it.entryCount }
                    .let { slices ->
                        val total = slices.sumOf { it.entryCount }.coerceAtLeast(1)
                        slices.map { it.copy(fraction = it.entryCount.toFloat() / total) }
                    }
            }

            val heatmap = allLogs.groupingBy { it.createdDate.toLocalDate() }.eachCount()
            val streak = withContext(Dispatchers.Default) {
                // 有学习记录或有番茄钟专注的一天视为活跃，从今天往回数连续天数
                val activeDays = allLogs.map { it.createdDate.toLocalDate() }.toSet() +
                        allPomodoros.filterValues { it > 0 }.keys
                var streakDays = 0
                var cursor = today
                while (cursor in activeDays) {
                    streakDays++
                    cursor = cursor.minusDays(1)
                }
                streakDays
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    totalLogs = allLogs.size,
                    totalFocusMinutes = allPomodoros.values.sum() / 60,
                    streakDays = streak,
                    totalQuestions = allQuestions.size,
                    focusBars = focusBars,
                    focusTotalMinutes = focusTotal,
                    notebookSlices = notebookSlices,
                    questionBars = questionBars,
                    questionTotal = questionTotal,
                    heatmapData = heatmap
                )
            }
        }
    }
}
