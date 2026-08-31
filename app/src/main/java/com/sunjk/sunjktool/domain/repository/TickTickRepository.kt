package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.data.remote.TickTickProject
import com.sunjk.sunjktool.data.remote.TickTickTask
import kotlinx.coroutines.flow.Flow

interface TickTickRepository {
    val isConfigured: Boolean
    val completedMode: String
    val projects: Flow<List<TickTickProject>>
    val tasks: Flow<List<TickTickTask>>
    fun tasksByProject(projectId: String): Flow<List<TickTickTask>>
    fun tasksByDueDate(date: String): Flow<List<TickTickTask>>

    /** 校验 API 口令是否可用。返回 null 表示成功，否则为错误信息。 */
    suspend fun testConnection(): String?

    /** 账号登录换取 v2 token 并持久化。 */
    suspend fun signOn(username: String, password: String): String

    /** 清除已保存的 v2 token。 */
    suspend fun signOut()

    /** 清空本地缓存的清单与任务。 */
    suspend fun clearLocalCache()

    /** 拉取全部清单与任务并写入本地缓存。 */
    suspend fun refresh()

    /** 拉取单个清单的任务并写缓存。 */
    suspend fun refreshProject(projectId: String)

    suspend fun createTask(title: String, projectId: String, dueDate: String?): Result<Unit>
    suspend fun toggleComplete(task: TickTickTask): Result<Unit>
    suspend fun deleteTask(task: TickTickTask): Result<Unit>
}
