package com.sunjk.sunjktool.data.remote

import android.webkit.CookieManager
import com.sunjk.sunjktool.data.local.ApiPreferences
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/**
 * 滴答清单（Dida365）内部 v2 接口客户端（非官方）。
 *
 * v2 接口需要较完整的鉴权：Bearer token + Cookie（t、_csrf_token）+ x-csrftoken 头 + x-device 头，
 * 否则会返回 405/401。登录后这些 cookie 存在 CookieManager 中，这里一并读取带上。
 *
 * 所有请求均先校验 HTTP 状态码，失败抛 [TickTickException]，避免反序列化错误对象闪退。
 */
class TickTickApi(
    private val client: HttpClient,
    private val apiPreferences: ApiPreferences
) {

    companion object {
        private const val BASE_URL = "https://api.dida365.com/api/v2"
        private const val BASE_URL_V3 = "https://api.dida365.com/api/v3"
        // coerceInputValues: JSON 中 null 落到非空字段时用默认值（如 "color":null），否则解析抛异常
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        // 类浏览器请求头，规避 v2 内部接口的反爬/403 门槛
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
    }

    private val token: String get() = apiPreferences.getTickTickToken()

    // v2 接口要求的设备头（来自网页版 signon 的 x-device 结构）
    private val deviceHeader = """{"platform":"web","os":"Android","device":"Chrome","name":"","version":0,"id":"","channel":"website","campaign":"","websocket":""}"""

    private val csrfToken: String get() = apiPreferences.getTickTickCsrfToken()

    /** 从 CookieManager 读取指定 cookie 值（登录后存在）。 */
    private fun readCookieValue(name: String): String? {
        val hosts = listOf(
            "https://dida365.com",
            "https://www.dida365.com",
            "https://api.dida365.com"
        )
        val regex = Regex("(?:^|;\\s*)$name=([^;]+)")
        for (h in hosts) {
            val cookies = try { CookieManager.getInstance().getCookie(h) } catch (_: Exception) { null }
                ?: continue
            val m = regex.find(cookies)
            if (m != null && m.groupValues.getOrNull(1).isNullOrBlank().not()) {
                return m.groupValues[1]
            }
        }
        return null
    }

    private fun io.ktor.client.request.HttpRequestBuilder.didaHeaders() {
        header("User-Agent", USER_AGENT)
        header("Referer", "https://dida365.com/")
        header("Origin", "https://dida365.com")
        header("Accept", "application/json")
        header("x-requested-with", "XMLHttpRequest")
        header("x-device", deviceHeader)
        header("x-tz", "Asia/Shanghai")
        header("hl", "zh_CN")

        // 优先用登录时存储的 token/csrf，避免重登后 CookieManager 状态不可靠；缺失则回退 CookieManager
        val t = token
        val csrf = csrfToken.ifBlank { readCookieValue("_csrf_token") ?: "" }
        val cookieParts = buildList {
            if (t.isNotBlank()) add("t=$t")
            if (csrf.isNotBlank()) add("_csrf_token=$csrf")
        }
        if (cookieParts.isNotEmpty()) header("Cookie", cookieParts.joinToString("; "))
        if (csrf.isNotBlank()) header("x-csrftoken", csrf)
    }

    /** 账号登录，返回会话 token（调用方负责持久化）。 */
    suspend fun signOn(username: String, password: String): String {
        val resp: TickTickSignOnResponse = client.post("$BASE_URL/user/signon") {
            didaHeaders()
            contentType(ContentType.Application.Json)
            setBody(TickTickSignOnRequest(username = username, password = password))
        }.ensureSuccess().body()
        if (resp.token.isBlank()) throw TickTickException("登录失败：未返回 token")
        return resp.token
    }

    suspend fun getProjects(): List<TickTickProject> =
        client.get("$BASE_URL/projects") {
            didaHeaders()
        }.ensureSuccess().body()

    /** v3 全量同步：一次返回所有项目 + 任务（网页版真实数据源）。 */
    suspend fun getBatchCheck(): TickTickBatchCheckResponse {
        val response = client.get("$BASE_URL_V3/batch/check/0") {
            didaHeaders()
        }.ensureSuccess()
        val raw = response.bodyAsText()
        android.util.Log.d("TickTick", "batch/check 响应体(截断): " + raw.take(4000))
        return try {
            json.decodeFromString<TickTickBatchCheckResponse>(raw)
        } catch (e: Exception) {
            android.util.Log.e("TickTick", "batch/check 解析失败", e)
            throw e
        }
    }

    /** 拉取已完成任务（独立接口，batch/check 不含已完成的）。 */
    suspend fun getCompletedTasks(): List<TickTickTask> {
        val to = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val response = client.get("$BASE_URL/project/all/completedInAll/") {
            didaHeaders()
            parameter("from", "")
            parameter("to", to)
            parameter("limit", 100)
        }.ensureSuccess()
        val raw = response.bodyAsText()
        android.util.Log.d("TickTick", "completedInAll 响应体(截断): " + raw.take(4000))
        return try {
            json.decodeFromString<List<TickTickTask>>(raw)
        } catch (e: Exception) {
            android.util.Log.e("TickTick", "completedInAll 解析失败", e)
            throw e
        }
    }

    /** 某项目下全部任务（进行中 + 已完成）。 */
    suspend fun getProjectTasks(projectId: String): List<TickTickTask> {
        val data: TickTickProjectDataResponse = client.get("$BASE_URL/project/$projectId/data") {
            didaHeaders()
        }.ensureSuccess().body()
        return data.tasks + data.completedTasks
    }

    suspend fun createTask(task: TickTickTask): TickTickTask =
        client.post("$BASE_URL/task") {
            didaHeaders()
            contentType(ContentType.Application.Json)
            setBody(task)
        }.ensureSuccess().body()

    suspend fun updateTask(task: TickTickTask): TickTickTask =
        client.post("$BASE_URL/task/${task.id}") {
            didaHeaders()
            contentType(ContentType.Application.Json)
            setBody(task)
        }.ensureSuccess().body()

    /** 批量任务同步：完成/取消/删除都走 POST /api/v2/batch/task。 */
    suspend fun batchTask(
        add: List<TickTickTask> = emptyList(),
        update: List<TickTickTask> = emptyList(),
        delete: List<TickTickDeleteRequest> = emptyList()
    ) {
        client.post("$BASE_URL/batch/task") {
            didaHeaders()
            contentType(ContentType.Application.Json)
            setBody(TickTickTaskBatch(add = add, update = update, delete = delete))
        }.ensureSuccess()
    }

    /** 校验 HTTP 状态码，非成功时解析错误信息并抛出 [TickTickException]。 */
    private suspend fun HttpResponse.ensureSuccess(): HttpResponse {
        if (status.isSuccess()) return this
        val raw = try { bodyAsText() } catch (_: Exception) { "" }
        val msg = try {
            json.decodeFromString<TickTickErrorResponse>(raw)
        } catch (_: Exception) {
            null
        }
        val detail = msg?.errorMessage?.takeIf { it.isNotBlank() }
            ?: msg?.message?.takeIf { it.isNotBlank() }
            ?: "HTTP ${status.value}"
        throw TickTickException("${status.value}: $detail")
    }
}
