package com.sunjk.sunjktool.feature.todo

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.remote.TickTickProject
import com.sunjk.sunjktool.data.remote.TickTickTask
import com.sunjk.sunjktool.domain.repository.TickTickRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 待办页筛选：all=所有, today=今天, project:<id>=指定清单 */
const val FILTER_ALL = "all"
const val FILTER_TODAY = "today"
const val FILTER_PROJECT_PREFIX = "project:"

@Immutable
data class TodoUiState(
    val isConfigured: Boolean = false,
    val projects: List<TickTickProject> = emptyList(),
    val selectedKey: String = FILTER_ALL,
    val tasks: List<TickTickTask> = emptyList(),
    val completedMode: String = com.sunjk.sunjktool.data.local.ApiPreferences.COMPLETED_MODE_ALL,
    val isLoading: Boolean = false,
    val error: String? = null
)

class TodoViewModel(
    private val repo: TickTickRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodoUiState(isConfigured = repo.isConfigured, completedMode = repo.completedMode))
    val uiState: StateFlow<TodoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.projects.collect { projects ->
                _uiState.update { it.copy(projects = projects, isConfigured = repo.isConfigured, completedMode = repo.completedMode) }
            }
        }
        viewModelScope.launch {
            combine(repo.tasks, _uiState.map { it.selectedKey }, _uiState.map { it.completedMode }) { tasks, key, mode ->
                val base = when {
                    key == FILTER_ALL -> tasks
                    key == FILTER_TODAY -> tasks.filter { it.dueDate == todayStr() }
                    key.startsWith(FILTER_PROJECT_PREFIX) ->
                        tasks.filter { it.projectId == key.removePrefix(FILTER_PROJECT_PREFIX) }
                    else -> tasks
                }
                com.sunjk.sunjktool.util.TickTickFilters.applyCompletedMode(base, mode)
            }.collect { tasks ->
                _uiState.update { it.copy(tasks = tasks) }
            }
        }
        refresh()
    }

    fun selectKey(key: String) {
        _uiState.update { it.copy(selectedKey = key) }
    }

    fun refreshCompletedMode() {
        _uiState.update { it.copy(completedMode = repo.completedMode) }
    }

    fun refresh() {
        if (!repo.isConfigured) {
            _uiState.update { it.copy(isConfigured = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isConfigured = true) }
            try {
                repo.refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun createTask(title: String, projectId: String?, dueDate: String?) {
        val pid = projectId ?: _uiState.value.projects.firstOrNull()?.id ?: ""
        viewModelScope.launch {
            repo.createTask(title, pid, dueDate).onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun toggle(task: TickTickTask) {
        viewModelScope.launch {
            repo.toggleComplete(task).onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun delete(task: TickTickTask) {
        viewModelScope.launch {
            repo.deleteTask(task).onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun todayStr(): String = java.time.LocalDate.now().toString()
}
