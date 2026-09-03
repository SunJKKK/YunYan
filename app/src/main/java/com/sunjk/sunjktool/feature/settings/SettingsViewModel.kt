package com.sunjk.sunjktool.feature.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.local.ApiPreferences
import com.sunjk.sunjktool.data.local.AiModelOption
import com.sunjk.sunjktool.data.local.PromptDefaults
import com.sunjk.sunjktool.data.local.PromptKeys
import com.sunjk.sunjktool.data.sync.SyncEngine
import com.sunjk.sunjktool.data.sync.SyncException
import com.sunjk.sunjktool.data.sync.SyncStatus
import com.sunjk.sunjktool.domain.repository.TickTickRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class SettingsUiState(
    // API keys
    val deepSeekKey: String = "",
    val qweatherKey: String = "",
    val apiKeySaveResult: String? = null,

    // AI model
    val deepSeekModel: String = ApiPreferences.MODEL_V4_FLASH,
    // DeepSeek API base URL
    val deepSeekBaseUrl: String = ApiPreferences.DEFAULT_DEEPSEEK_BASE_URL,

    // Qwen (通义千问) API
    val qwenKey: String = "",
    val qwenBaseUrl: String = ApiPreferences.DEFAULT_QWEN_BASE_URL,
    val qwenModel: String = ApiPreferences.MODEL_QWEN_FLASH,
    // 全局默认 AI 模型（存 AiModelOption.id）
    val defaultAiModel: String = AiModelOption.DEEPSEEK_FLASH.id,


    // WebDAV
    val webDavUrl: String = "",
    val username: String = "",
    val password: String = "",
    val autoSyncEnabled: Boolean = false,
    val isConfigured: Boolean = false,
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val isSaving: Boolean = false,

    // Display
    val animationEnabled: Boolean = true,
    val themeMode: String = ApiPreferences.THEME_MODE_SYSTEM,

    // Tablet / Read-only
    val tabletMode: Boolean = false,
    val readOnlySync: Boolean = false,

    // Question Bank
    val questionBankAutoSave: Boolean = false,

    // Multi-Agent
    val multiAgentParallel: Boolean = false,

    // AI Prompt overrides
    val selfCheckPrompt: String = "",
    val flashcardPrompt: String = "",
    val gapAnalysisPrompt: String = "",
    val knowledgeRetrievalPrompt: String = "",
    val questionSplitPrompt: String = "",
    val questionAnalysisPrompt: String = "",

    // TickTick
    val tickTickToken: String = "",
    val tickTickManualToken: String = "",
    val tickTickCompletedMode: String = ApiPreferences.COMPLETED_MODE_ALL,
    val tickTickTesting: Boolean = false,
    val showTickTickWebLogin: Boolean = false,
    val tickTickTestResult: String? = null,

    // About / update check
    val isCheckingUpdate: Boolean = false,
    val updateStatusText: String? = null,
    /** 发现新版本时弹出的对话框：Release 页面地址；关闭对话框后置空 */
    val updateDialogUrl: String? = null,
    /** 对话框中展示的新版本号 */
    val updateDialogVersion: String? = null
)

class SettingsViewModel(
    private val syncEngine: SyncEngine,
    private val apiPreferences: ApiPreferences,
    private val tickTickRepository: TickTickRepository
) : ViewModel() {

    private val syncPrefs = syncEngine.syncPrefs

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val syncStatus: StateFlow<SyncStatus> = syncEngine.syncStatus

    init {
        _uiState.value = _uiState.value.copy(
            deepSeekKey = apiPreferences.getDeepSeekKey(),
            deepSeekModel = apiPreferences.getDeepSeekModel(),
            deepSeekBaseUrl = apiPreferences.getDeepSeekBaseUrl(),
            qwenKey = apiPreferences.getQwenKey(),
            qwenBaseUrl = apiPreferences.getQwenBaseUrl(),
            qwenModel = apiPreferences.getQwenModel(),
            defaultAiModel = apiPreferences.getDefaultAiModelOption(),
            qweatherKey = apiPreferences.getQWeatherKey(),
            webDavUrl = syncPrefs.getWebDavUrl().ifBlank { "https://dav.jianguoyun.com/dav/" },
            username = syncPrefs.getUsername(),
            password = syncPrefs.getPassword(),
            autoSyncEnabled = syncPrefs.isAutoSyncEnabled(),
            isConfigured = syncPrefs.isConfigured,
            tabletMode = apiPreferences.isTabletMode(),
            readOnlySync = apiPreferences.isReadOnlySync(),
            animationEnabled = apiPreferences.isAnimationEnabled(),
            themeMode = apiPreferences.getThemeMode(),
            questionBankAutoSave = apiPreferences.isQuestionBankAutoSave(),
            multiAgentParallel = apiPreferences.isMultiAgentParallel(),
            selfCheckPrompt = apiPreferences.getPrompt(PromptKeys.SELF_CHECK) ?: PromptDefaults.SELF_CHECK,
            flashcardPrompt = apiPreferences.getPrompt(PromptKeys.FLASHCARD) ?: PromptDefaults.FLASHCARD,
            gapAnalysisPrompt = apiPreferences.getPrompt(PromptKeys.GAP_ANALYSIS) ?: PromptDefaults.GAP_ANALYSIS,
            knowledgeRetrievalPrompt = apiPreferences.getPrompt(PromptKeys.KNOWLEDGE_RETRIEVAL) ?: PromptDefaults.KNOWLEDGE_RETRIEVAL,
            questionSplitPrompt = apiPreferences.getPrompt(PromptKeys.QUESTION_SPLIT) ?: PromptDefaults.QUESTION_SPLIT,
            questionAnalysisPrompt = apiPreferences.getPrompt(PromptKeys.QUESTION_ANALYSIS) ?: PromptDefaults.QUESTION_ANALYSIS,
            tickTickToken = apiPreferences.getTickTickToken(),
            tickTickCompletedMode = apiPreferences.getTickTickCompletedMode()
        )
    }

    // ---- API Keys ----

    fun updateDeepSeekKey(key: String) {
        _uiState.value = _uiState.value.copy(deepSeekKey = key)
    }

    fun updateQWeatherKey(key: String) {
        _uiState.value = _uiState.value.copy(qweatherKey = key)
    }

    fun updateDeepSeekBaseUrl(url: String) {
        _uiState.value = _uiState.value.copy(deepSeekBaseUrl = url)
    }

    fun updateQwenKey(key: String) {
        _uiState.value = _uiState.value.copy(qwenKey = key)
    }

    fun updateQwenBaseUrl(url: String) {
        _uiState.value = _uiState.value.copy(qwenBaseUrl = url)
    }

    fun updateQwenModel(model: String) {
        _uiState.value = _uiState.value.copy(qwenModel = model)
    }

    fun setDefaultAiModel(optionId: String) {
        apiPreferences.setDefaultAiModelOption(optionId)
        _uiState.value = _uiState.value.copy(defaultAiModel = optionId)
    }

    fun saveApiKeys() {
        val s = _uiState.value
        apiPreferences.setDeepSeekKey(s.deepSeekKey)
        apiPreferences.setQWeatherKey(s.qweatherKey)
        apiPreferences.setDeepSeekBaseUrl(s.deepSeekBaseUrl)
        apiPreferences.setQwenKey(s.qwenKey)
        apiPreferences.setQwenBaseUrl(s.qwenBaseUrl)
        apiPreferences.setQwenModel(s.qwenModel)
        _uiState.value = _uiState.value.copy(apiKeySaveResult = "API 密钥已保存")
    }

    fun clearApiKeyResult() {
        _uiState.value = _uiState.value.copy(apiKeySaveResult = null)
    }

    // ---- TickTick (滴答清单) ----

    fun openTickTickWebLogin() {
        _uiState.value = _uiState.value.copy(showTickTickWebLogin = true, tickTickTestResult = null)
    }

    fun dismissTickTickWebLogin() {
        _uiState.value = _uiState.value.copy(showTickTickWebLogin = false)
    }

    fun onTickTickTokenCaptured(token: String, csrf: String) {
        apiPreferences.setTickTickToken(token.trim())
        apiPreferences.setTickTickCsrfToken(csrf.trim())
        _uiState.value = _uiState.value.copy(
            showTickTickWebLogin = false,
            tickTickToken = apiPreferences.getTickTickToken(),
            tickTickTestResult = "登录成功 ✓"
        )
    }

    fun updateTickTickManualToken(token: String) {
        _uiState.value = _uiState.value.copy(tickTickManualToken = token)
    }

    fun setTickTickCompletedMode(mode: String) {
        apiPreferences.setTickTickCompletedMode(mode)
        _uiState.value = _uiState.value.copy(tickTickCompletedMode = mode)
    }

    /** 保存手动粘贴的 Cookie/Token（兼容整段 cookie 或单独的 t 值）。 */
    fun saveTickTickManualToken() {
        val raw = _uiState.value.tickTickManualToken.trim()
        if (raw.isBlank()) {
            _uiState.value = _uiState.value.copy(tickTickTestResult = "请粘贴 Cookie 或 Token")
            return
        }
        // 从整段 cookie 提取 t 与 _csrf_token；若无则整段当作 t
        val t = Regex("(?:^|;\\s*)t=([^;]+)").find(raw)?.groupValues?.getOrNull(1) ?: raw
        val csrf = Regex("(?:^|;\\s*)_csrf_token=([^;]+)").find(raw)?.groupValues?.getOrNull(1) ?: ""
        apiPreferences.setTickTickToken(t)
        apiPreferences.setTickTickCsrfToken(csrf)
        _uiState.value = _uiState.value.copy(
            tickTickManualToken = "",
            tickTickToken = apiPreferences.getTickTickToken(),
            tickTickTestResult = "已保存"
        )
    }

    /** 清除 WebView cookie、已存 token 以及本地缓存的清单/任务，便于重新登录。 */
    fun clearTickTickCache() {
        try {
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.CookieManager.getInstance().flush()
        } catch (_: Exception) {}
        apiPreferences.setTickTickToken("")
        viewModelScope.launch {
            tickTickRepository.clearLocalCache()
        }
        _uiState.value = _uiState.value.copy(
            tickTickToken = "",
            tickTickTestResult = "已清除登录缓存与本地数据"
        )
    }

    fun signOutTickTick() {
        apiPreferences.setTickTickToken("")
        _uiState.value = _uiState.value.copy(
            tickTickToken = "",
            tickTickTestResult = "已退出登录"
        )
    }

    fun testTickTickConnection() {
        if (_uiState.value.tickTickToken.isBlank()) {
            _uiState.value = _uiState.value.copy(tickTickTestResult = "请先登录")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(tickTickTesting = true, tickTickTestResult = null)
            val result = withContext(Dispatchers.IO) {
                val err = tickTickRepository.testConnection()
                if (err == null) "连接成功 ✓" else "连接失败: $err"
            }
            _uiState.value = _uiState.value.copy(tickTickTesting = false, tickTickTestResult = result)
        }
    }

    // ---- AI Model ----

    fun setDeepSeekModel(model: String) {
        apiPreferences.setDeepSeekModel(model)
        _uiState.value = _uiState.value.copy(deepSeekModel = model)
    }

    // ---- WebDAV ----

    fun updateWebDavUrl(url: String) {
        _uiState.value = _uiState.value.copy(webDavUrl = url)
    }

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun setAutoSync(enabled: Boolean) {
        syncPrefs.setAutoSyncEnabled(enabled)
        _uiState.value = _uiState.value.copy(autoSyncEnabled = enabled)
    }

    fun saveCredentials() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.value = s.copy(isSaving = true)
            withContext(Dispatchers.IO) {
                syncPrefs.setWebDavUrl(s.webDavUrl)
                syncPrefs.setUsername(s.username)
                syncPrefs.setPassword(s.password)
            }
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                isConfigured = syncPrefs.isConfigured,
                testResult = "设置已保存"
            )
        }
    }

    fun testConnection() {
        val s = _uiState.value
        if (s.webDavUrl.isBlank() || s.username.isBlank() || s.password.isBlank()) {
            _uiState.value = s.copy(testResult = "请填写完整信息")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTesting = true, testResult = null)
            val result = withContext(Dispatchers.IO) {
                try {
                    val client = syncEngine.createWebDavClient()
                    client.listDirectory("")
                    "连接成功 ✓"
                } catch (e: SyncException.AuthFailure) {
                    "认证失败，请检查邮箱和应用密码"
                } catch (e: SyncException.NetworkError) {
                    "网络错误: ${e.message}"
                } catch (e: Exception) {
                    "连接失败: ${e.message}"
                }
            }
            _uiState.value = _uiState.value.copy(isTesting = false, testResult = result)
        }
    }

    fun setAnimationEnabled(enabled: Boolean) {
        apiPreferences.setAnimationEnabled(enabled)
        _uiState.value = _uiState.value.copy(animationEnabled = enabled)
    }

    fun setQuestionBankAutoSave(enabled: Boolean) {
        apiPreferences.setQuestionBankAutoSave(enabled)
        _uiState.value = _uiState.value.copy(questionBankAutoSave = enabled)
    }

    fun setMultiAgentParallel(enabled: Boolean) {
        apiPreferences.setMultiAgentParallel(enabled)
        _uiState.value = _uiState.value.copy(multiAgentParallel = enabled)
    }

    fun setThemeMode(mode: String) {
        apiPreferences.setThemeMode(mode)
        _uiState.value = _uiState.value.copy(themeMode = mode)
    }


    fun setTabletMode(enabled: Boolean) {
        apiPreferences.setTabletMode(enabled)
        _uiState.value = _uiState.value.copy(tabletMode = enabled)
    }

    fun setReadOnlySync(enabled: Boolean) {
        apiPreferences.setReadOnlySync(enabled)
        _uiState.value = _uiState.value.copy(readOnlySync = enabled)
    }    fun updatePrompt(key: String, value: String) {
        _uiState.value = when (key) {
            PromptKeys.SELF_CHECK -> _uiState.value.copy(selfCheckPrompt = value)
            PromptKeys.FLASHCARD -> _uiState.value.copy(flashcardPrompt = value)
            PromptKeys.GAP_ANALYSIS -> _uiState.value.copy(gapAnalysisPrompt = value)
            PromptKeys.KNOWLEDGE_RETRIEVAL -> _uiState.value.copy(knowledgeRetrievalPrompt = value)
            PromptKeys.QUESTION_SPLIT -> _uiState.value.copy(questionSplitPrompt = value)
            PromptKeys.QUESTION_ANALYSIS -> _uiState.value.copy(questionAnalysisPrompt = value)
            else -> _uiState.value
        }
    }

    fun savePrompt(key: String) {
        val value = when (key) {
            PromptKeys.SELF_CHECK -> _uiState.value.selfCheckPrompt
            PromptKeys.FLASHCARD -> _uiState.value.flashcardPrompt
            PromptKeys.GAP_ANALYSIS -> _uiState.value.gapAnalysisPrompt
            PromptKeys.KNOWLEDGE_RETRIEVAL -> _uiState.value.knowledgeRetrievalPrompt
            PromptKeys.QUESTION_SPLIT -> _uiState.value.questionSplitPrompt
            PromptKeys.QUESTION_ANALYSIS -> _uiState.value.questionAnalysisPrompt
            else -> return
        }
        apiPreferences.setPrompt(key, value)
    }

    fun resetPrompt(key: String) {
        apiPreferences.resetPrompt(key)
        updatePrompt(key, "")
    }

    fun triggerSync() {
        syncEngine.triggerManualSync()
    }


    fun getLastSyncTimestamp(): Long = syncPrefs.getLastSyncTimestamp()

    // ---- 关于 / 检查更新 ----

    companion object {
        private const val REPO_LATEST_RELEASE_API =
            "https://api.github.com/repos/SunJKKK/YunYan/releases/latest"
    }

    /**
     * 对比 GitHub Releases 最新版本与本地版本号（忽略前缀 v，按数字段逐段比较）。
     * 返回最新版本号；无新版本或失败时返回 null 并写入状态文案。
     */
    fun checkUpdate(currentVersion: String) {
        if (_uiState.value.isCheckingUpdate) return
        _uiState.value = _uiState.value.copy(
            isCheckingUpdate = true, updateStatusText = "正在检查更新...",
            updateDialogUrl = null, updateDialogVersion = null
        )
        viewModelScope.launch {
            try {
                val (latestVersion, pageUrl) = withContext(Dispatchers.IO) {
                    val client = okhttp3.OkHttpClient()
                    val request = okhttp3.Request.Builder()
                        .url(REPO_LATEST_RELEASE_API)
                        .header("Accept", "application/vnd.github+json")
                        .build()
                    client.newCall(request).execute().use { resp ->
                        if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                        val body = resp.body?.string() ?: throw IllegalStateException("空响应")
                        val obj = org.json.JSONObject(body)
                        (obj.optString("tag_name") to obj.optString("html_url"))
                    }
                }
                val hasNewer = compareVersions(latestVersion.removePrefix("v"), currentVersion.removePrefix("v")) > 0
                _uiState.value = _uiState.value.copy(
                    isCheckingUpdate = false,
                    updateStatusText = if (hasNewer) "发现新版本 $latestVersion"
                    else "已是最新版本（v$currentVersion）",
                    updateDialogUrl = if (hasNewer) pageUrl else null,
                    updateDialogVersion = if (hasNewer) latestVersion else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCheckingUpdate = false,
                    updateStatusText = "检查失败，请稍后重试"
                )
            }
        }
    }

    fun dismissUpdateDialog() {
        _uiState.value = _uiState.value.copy(updateDialogUrl = null, updateDialogVersion = null)
    }

    /** 逐段数字比较版本号，如 1.5.1 vs 1.4；解析失败的段按 0 处理 */
    private fun compareVersions(a: String, b: String): Int {
        val pa = a.split('.').map { it.toIntOrNull() ?: 0 }
        val pb = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrNull(i) ?: 0
            val y = pb.getOrNull(i) ?: 0
            if (x != y) return x.compareTo(y)
        }
        return 0
    }
}
