package com.sunjk.sunjktool.di

import android.util.Log
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class AiTaskType { SUMMARY, FLASHCARDS, SELF_CHECK }

enum class AiTaskStatus { RUNNING, SUCCESS, ERROR }

@Immutable
data class GenerationTask(
    val taskId: String,
    val type: AiTaskType,
    val logId: Long,
    val title: String,
    val phase: String = "",
    val progress: Float = 0f,
    val status: AiTaskStatus = AiTaskStatus.RUNNING,
    val error: String? = null
)

/**
 * App-scoped AI generation task tracker.
 * Coroutines run in [scope], which is not tied to any screen's ViewModel,
 * so generation keeps running after the user leaves the page.
 */
object AiGenerationManager {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _tasks = MutableStateFlow<Map<String, GenerationTask>>(emptyMap())
    val tasks: StateFlow<List<GenerationTask>> = _tasks
        .map { it.values.sortedBy { t -> t.taskId } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun taskIdFor(type: AiTaskType, logId: Long): String = "${type.name.lowercase()}_$logId"

    fun start(type: AiTaskType, logId: Long, title: String) {
        val taskId = taskIdFor(type, logId)
        _tasks.update { map ->
            map + (taskId to GenerationTask(taskId = taskId, type = type, logId = logId, title = title))
        }
    }

    fun updatePhase(type: AiTaskType, logId: Long, phase: String, progress: Float? = null) {
        val taskId = taskIdFor(type, logId)
        _tasks.update { map ->
            val current = map[taskId] ?: return@update map
            map + (taskId to current.copy(
                phase = phase,
                progress = progress ?: current.progress,
                status = AiTaskStatus.RUNNING
            ))
        }
    }

    fun complete(type: AiTaskType, logId: Long) {
        val taskId = taskIdFor(type, logId)
        _tasks.update { map ->
            val current = map[taskId] ?: return@update map
            map + (taskId to current.copy(status = AiTaskStatus.SUCCESS, phase = "完成", progress = 1f))
        }
    }

    fun fail(type: AiTaskType, logId: Long, error: String) {
        val taskId = taskIdFor(type, logId)
        _tasks.update { map ->
            val current = map[taskId] ?: return@update map
            map + (taskId to current.copy(status = AiTaskStatus.ERROR, phase = "失败", error = error))
        }
    }

    fun remove(type: AiTaskType, logId: Long) {
        val taskId = taskIdFor(type, logId)
        _tasks.update { map -> map - taskId }
    }

    fun task(type: AiTaskType, logId: Long): GenerationTask? = _tasks.value[taskIdFor(type, logId)]
}