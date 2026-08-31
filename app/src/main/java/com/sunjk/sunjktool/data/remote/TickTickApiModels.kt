package com.sunjk.sunjktool.data.remote

import kotlinx.serialization.Serializable

/** 滴答清单 OpenAPI 清单（项目）模型。 */
@Serializable
data class TickTickProject(
    val id: String,
    val name: String,
    val sortOrder: Long = 0,
    val color: String = ""
)

/** 滴答清单 OpenAPI 任务模型（字段与官方 OpenAPI 对齐）。 */
@Serializable
data class TickTickTask(
    val id: String = "",
    val projectId: String = "",
    val title: String = "",
    val content: String = "",
    val desc: String = "",
    val isCompleted: Boolean = false,
    val dueDate: String? = null,
    val priority: Int = 0,
    val sortOrder: Long = 0,
    val status: Int = 0,
    val timeZone: String = ""
)

/** 完成任务请求体。 */
@Serializable
data class TickTickCompleteRequest(
    val taskId: String,
    val projectId: String
)

/** OpenAPI 错误响应（用于测试连接时读取 message）。 */
@Serializable
data class TickTickErrorResponse(
    val error: String = "",
    val message: String = "",
    val errorCode: String = "",
    val errorMessage: String = "",
    val status: Int = 0
)

/** 滴答清单 API 异常：携带服务器返回的错误信息。 */
class TickTickException(message: String) : Exception(message)

/** v2 登录（signon）响应：换取会话 token。 */
@Serializable
data class TickTickSignOnResponse(
    val token: String = "",
    val userId: String = "",
    val username: String = ""
)

/** v2 登录请求体。 */
@Serializable
data class TickTickSignOnRequest(
    val username: String,
    val password: String
)

/** v2 项目 data 接口响应：包含该项目下所有任务与已完成任务。 */
@Serializable
data class TickTickProjectDataResponse(
    val tasks: List<TickTickTask> = emptyList(),
    val completedTasks: List<TickTickTask> = emptyList()
)

/** v3 全量同步响应（GET /api/v3/batch/check/0），包含项目、任务、标签等。 */
@Serializable
data class TickTickBatchCheckResponse(
    val projectProfiles: List<TickTickProject> = emptyList(),
    val syncTaskBean: TickTickSyncTaskBean = TickTickSyncTaskBean(),
    val inboxId: String = ""
)

@Serializable
data class TickTickSyncTaskBean(
    val update: List<TickTickBatchTask> = emptyList(),
    val delete: List<TickTickBatchTask> = emptyList(),
    val add: List<TickTickBatchTask> = emptyList()
)

/** v3 batch/check 中的任务对象（字段与响应体一致）。 */
@Serializable
data class TickTickBatchTask(
    val id: String = "",
    val projectId: String = "",
    val title: String = "",
    val content: String = "",
    val dueDate: String? = null,
    val startDate: String? = null,
    val priority: Int = 0,
    val sortOrder: Long = 0,
    val status: Int = 0
)

/** 批量删除任务条目。 */
@Serializable
data class TickTickDeleteRequest(
    val taskId: String,
    val projectId: String
)

/** 批量任务同步请求体（POST /api/v2/batch/task）。完成/删除等操作都走这里。 */
@Serializable
data class TickTickTaskBatch(
    val add: List<TickTickTask> = emptyList(),
    val update: List<TickTickTask> = emptyList(),
    val delete: List<TickTickDeleteRequest> = emptyList(),
    val addAttachments: List<String> = emptyList(),
    val updateAttachments: List<String> = emptyList(),
    val deleteAttachments: List<String> = emptyList()
)
