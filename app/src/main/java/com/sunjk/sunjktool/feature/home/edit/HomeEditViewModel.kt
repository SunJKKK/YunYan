package com.sunjk.sunjktool.feature.home.edit

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.model.HomeModuleEntity
import com.sunjk.sunjktool.data.sync.SyncTrigger
import com.sunjk.sunjktool.domain.model.Countdown
import com.sunjk.sunjktool.domain.model.Habit
import com.sunjk.sunjktool.domain.repository.CountdownRepository
import com.sunjk.sunjktool.domain.repository.HabitRepository
import com.sunjk.sunjktool.domain.repository.HomeModuleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ModuleToggle(
    val key: String,
    val title: String,
    val description: String,
    val enabled: Boolean,
    val size: String = "small"
)

@Immutable
data class HomeEditUiState(
    val modules: List<ModuleToggle> = emptyList(),
    val isLoading: Boolean = true,
    val saveComplete: Boolean = false
)

class HomeEditViewModel(
    private val repository: HomeModuleRepository,
    private val countdownRepository: CountdownRepository,
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeEditUiState())
    val uiState: StateFlow<HomeEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getAll(),
                countdownRepository.getAll(),
                habitRepository.getAll()
            ) { entities, countdowns, habits ->
                val today = LocalDate.now()
                val modules = buildModulesList(entities, countdowns, habits, today)
                _uiState.update {
                    it.copy(modules = modules, isLoading = false)
                }
            }.collect { }
        }
    }

    private fun buildModulesList(
        entities: List<HomeModuleEntity>,
        countdowns: List<Countdown>,
        habits: List<Habit>,
        today: LocalDate
    ): List<ModuleToggle> {
        val entityMap = entities.associateBy { it.moduleKey }
        val result = mutableListOf<ModuleToggle>()

        // Fixed modules
        for (key in FIXED_MODULE_KEYS) {
            val e = entityMap[key]
            result.add(
                ModuleToggle(
                    key = key,
                    title = moduleTitle(key),
                    description = moduleDescription(key),
                    enabled = e?.enabled ?: (key == "heatmap" || key == "today_logs"),
                    size = e?.size ?: "small"
                )
            )
        }

        // Countdown modules — one per countdown entry
        for (cd in countdowns) {
            val cdKey = "countdown_${cd.id}"
            val e = entityMap[cdKey]
            val days = ChronoUnit.DAYS.between(today, cd.targetDate)
            val daysText = when {
                days > 0 -> "还有${days}天"
                days == 0L -> "就是今天"
                else -> "已过${-days}天"
            }
            result.add(
                ModuleToggle(
                    key = cdKey,
                    title = "倒数日: ${cd.title}",
                    description = daysText,
                    enabled = e?.enabled ?: false,
                    size = e?.size ?: "small"
                )
            )
        }

        // Habit modules — one combined card per habit (check-in + heatmap)
        for (h in habits) {
            val habitKey = "habit_${h.id}"
            val hEntity = entityMap[habitKey]
            result.add(
                ModuleToggle(
                    key = habitKey,
                    title = "习惯: ${h.name}",
                    description = "每日打卡 · 活跃度",
                    enabled = hEntity?.enabled ?: false,
                    size = hEntity?.size ?: "small"
                )
            )
        }

        // Sort by entity's sortOrder if available
        val sortMap = entities.associate { it.moduleKey to it.sortOrder }
        result.sortBy { sortMap[it.key] ?: FIXED_MODULE_KEYS.size }
        return result
    }

    fun toggleModule(key: String) {
        _uiState.update { state ->
            state.copy(
                modules = state.modules.map {
                    if (it.key == key) it.copy(enabled = !it.enabled) else it
                }
            )
        }
    }


    fun updateSize(key: String, size: String) {
        viewModelScope.launch {
            repository.updateSize(key, size)
        }
    }

    /** 在当前卡片大小（小/大）之间切换。 */
    fun toggleSize(key: String) {
        val current = _uiState.value.modules.firstOrNull { it.key == key }?.size ?: "small"
        val next = if (current == "large") "small" else "large"
        updateSize(key, next)
    }

    fun moveUp(key: String) {
        _uiState.update { state ->
            val idx = state.modules.indexOfFirst { it.key == key }
            if (idx <= 0) return@update state
            val list = state.modules.toMutableList()
            val temp = list[idx]
            list[idx] = list[idx - 1]
            list[idx - 1] = temp
            state.copy(modules = list)
        }
    }

    fun moveDown(key: String) {
        _uiState.update { state ->
            val idx = state.modules.indexOfFirst { it.key == key }
            if (idx < 0 || idx >= state.modules.lastIndex) return@update state
            val list = state.modules.toMutableList()
            val temp = list[idx]
            list[idx] = list[idx + 1]
            list[idx + 1] = temp
            state.copy(modules = list)
        }
    }

    fun save() {
        viewModelScope.launch {
            val modules = _uiState.value.modules.mapIndexed { idx, toggle ->
                HomeModuleEntity(
                    moduleKey = toggle.key,
                    enabled = toggle.enabled,
                    sortOrder = idx,
                    size = toggle.size
                )
            }
            repository.updateAll(modules)
            // Clean orphaned countdown modules
            val validKeys = modules.map { it.moduleKey }.toSet()
            repository.cleanOrphaned(validKeys)
            _uiState.update { it.copy(saveComplete = true) }
            SyncTrigger.requestAutoSync()
        }
    }

    companion object {
        private val FIXED_MODULE_KEYS = listOf("heatmap", "today_logs", "review", "weather", "pomodoro", "deepseek", "notebook_shortcuts", "todo_today")

        fun moduleTitle(key: String): String = when (key) {
            "heatmap" -> "学习热力图"
            "today_logs" -> "今日学习记录"
            "weather" -> "天气"
            "pomodoro" -> "番茄钟"
            "deepseek" -> "DeepSeek 额度"
            "review" -> "复盘"
            "notebook_shortcuts" -> "笔记本快捷方式"
            "todo_today" -> "今日待办"
            else -> key // countdown_{id} titles are set dynamically
        }

        fun moduleDescription(key: String): String = when (key) {
            "heatmap" -> "展示最近学习活跃度"
            "today_logs" -> "集中展示今天的学习记录"
            "weather" -> "展示当前天气、预报与穿衣建议"
            "deepseek" -> "展示 DeepSeek API 余额与使用趋势"
            "pomodoro" -> "专注计时器"
            "notebook_shortcuts" -> "快速进入固定在首页的笔记本"
            "todo_today" -> "展示滴答清单今日到期任务"
            else -> "" // countdown descriptions are set dynamically
        }
    }
}
