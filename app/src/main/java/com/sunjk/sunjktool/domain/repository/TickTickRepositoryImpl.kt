package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.data.local.ApiPreferences
import com.sunjk.sunjktool.data.local.dao.TickTickProjectDao
import com.sunjk.sunjktool.data.local.dao.TickTickTaskDao
import com.sunjk.sunjktool.data.model.TickTickProjectEntity
import com.sunjk.sunjktool.data.model.TickTickTaskEntity
import com.sunjk.sunjktool.data.remote.TickTickApi
import com.sunjk.sunjktool.data.remote.TickTickBatchTask
import com.sunjk.sunjktool.data.remote.TickTickProject
import com.sunjk.sunjktool.data.remote.TickTickTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TickTickRepositoryImpl(
    private val api: TickTickApi,
    private val projectDao: TickTickProjectDao,
    private val taskDao: TickTickTaskDao,
    private val apiPreferences: ApiPreferences
) : TickTickRepository {

    override val isConfigured: Boolean
        get() = apiPreferences.getTickTickToken().isNotBlank()

    override val completedMode: String
        get() = apiPreferences.getTickTickCompletedMode()

    override val projects: Flow<List<TickTickProject>> =
        projectDao.getAll().map { list -> list.map { it.toApi() } }

    override val tasks: Flow<List<TickTickTask>> =
        taskDao.getAll().map { list -> list.map { it.toApi() } }

    override fun tasksByProject(projectId: String): Flow<List<TickTickTask>> =
        taskDao.getByProject(projectId).map { list -> list.map { it.toApi() } }

    override fun tasksByDueDate(date: String): Flow<List<TickTickTask>> =
        taskDao.getByDueDate(date).map { list -> list.map { it.toApi() } }

    /** 登录：换取 v2 token 并持久化。 */
    override suspend fun signOn(username: String, password: String): String {
        val token = api.signOn(username.trim(), password)
        apiPreferences.setTickTickToken(token)
        return token
    }

    override suspend fun signOut() {
        apiPreferences.setTickTickToken("")
    }

    override suspend fun clearLocalCache() {
        projectDao.clear()
        taskDao.clear()
    }

    override suspend fun testConnection(): String? = try {
        api.getProjects()
        null
    } catch (e: Exception) {
        e.message ?: "未知错误"
    }

    override suspend fun refresh() {
        // v3 全量同步：一次返回所有清单 + 任务（含未完成；已完成另接口拉取）
        val resp = try {
            api.getBatchCheck()
        } catch (e: Exception) {
            android.util.Log.e("TickTick", "refresh: batch/check 失败", e)
            return
        }
        projectDao.clear()
        projectDao.insertAll(resp.projectProfiles.map { it.toEntity() })
        val tasks = resp.syncTaskBean.update.map { it.toEntity() }.toMutableList()

        // 拉取已完成任务并合入
        val completed = try {
            api.getCompletedTasks()
        } catch (e: Exception) {
            android.util.Log.e("TickTick", "refresh: completedInAll 失败", e)
            emptyList()
        }
        android.util.Log.d("TickTick", "refresh 拉取到已完成任务数: ${completed.size}, 前3条: ${completed.take(3).map { it.title }}")
        tasks += completed.map { it.toEntity() }

        taskDao.clear()
        taskDao.upsertAll(tasks)
    }

    override suspend fun refreshProject(projectId: String) {
        val tasks = try {
            api.getProjectTasks(projectId)
        } catch (e: Exception) {
            return
        }
        taskDao.clearByProject(projectId)
        taskDao.upsertAll(tasks.map { it.toEntity() })
    }

    override suspend fun createTask(title: String, projectId: String, dueDate: String?): Result<Unit> = runCatching {
        val created = api.createTask(
            TickTickTask(title = title.trim(), projectId = projectId, dueDate = dueDate)
        )
        taskDao.upsertAll(listOf(created.toEntity()))
    }

    override suspend fun toggleComplete(task: TickTickTask): Result<Unit> = runCatching {
        if (task.isCompleted) {
            // 取消完成：status=0
            api.batchTask(update = listOf(task.copy(isCompleted = false, status = 0)))
        } else {
            // 完成：status=2
            api.batchTask(update = listOf(task.copy(isCompleted = true, status = 2)))
        }
        // 以服务器为准重新拉取，确保首页/待办页状态同步更新
        try { refresh() } catch (_: Exception) {
            // 拉取失败时不阻塞；下一次进入/刷新自会纠正
            taskDao.upsertAll(listOf(task.copy(isCompleted = !task.isCompleted).toEntity()))
        }
    }

    override suspend fun deleteTask(task: TickTickTask): Result<Unit> = runCatching {
        api.batchTask(delete = listOf(com.sunjk.sunjktool.data.remote.TickTickDeleteRequest(task.id, task.projectId)))
        taskDao.deleteById(task.id)
    }
}

// ─── Mappers ─────────────────────────────────────────────────────────

private fun TickTickProject.toEntity() = TickTickProjectEntity(id, name, sortOrder, color)
private fun TickTickTask.toEntity() = TickTickTaskEntity(
    id = id, projectId = projectId, title = title,
    isCompleted = isCompleted || status == 2,
    dueDate = normalizeDate(dueDate), priority = priority, sortOrder = sortOrder, content = content
)
private fun TickTickBatchTask.toEntity() = TickTickTaskEntity(
    id = id, projectId = projectId, title = title,
    isCompleted = (status == 2), // status: 0=进行中, 2=已完成
    dueDate = normalizeDate(dueDate), priority = priority, sortOrder = sortOrder, content = content
)

private fun TickTickProjectEntity.toApi() = TickTickProject(id, name, sortOrder, color)
private fun TickTickTaskEntity.toApi() = TickTickTask(
    id = id, projectId = projectId, title = title, content = content, desc = "",
    isCompleted = isCompleted, dueDate = dueDate, priority = priority, sortOrder = sortOrder,
    status = if (isCompleted) 2 else 0, timeZone = ""
)

/**
 * 把 v2/v3 的 dueDate（如 "2026-08-29T16:00:00.000+0000"，UTC）转换为**本地时区**的 "yyyy-MM-dd"。
 *
 * 注意：滴答返回的偏移是 "+0000"（无冒号），OffsetDateTime.parse 只认 "+00:00"，
 * 直接解析会抛异常。这里先归一化偏移再解析，避免退化成截取 UTC 日期（导致"今天"差一天）。
 */
private fun normalizeDate(dueDate: String?): String? {
    if (dueDate.isNullOrBlank()) return null
    val fixed = dueDate.replace(Regex("([+-]\\d{2})(\\d{2})$"), "$1:$2")
    return try {
        java.time.OffsetDateTime.parse(fixed)
            .atZoneSameInstant(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .toString()
    } catch (_: Exception) {
        val s = dueDate.trim()
        if (s.length >= 10 && s[4] == '-' && s[7] == '-') s.substring(0, 10) else null
    }
}
