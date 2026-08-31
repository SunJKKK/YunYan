package com.sunjk.sunjktool.feature.sync

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.sync.SyncEngine
import com.sunjk.sunjktool.data.sync.SyncException
import com.sunjk.sunjktool.data.sync.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class SyncSettingsUiState(
    val webDavUrl: String = "",
    val username: String = "",
    val password: String = "",
    val autoSyncEnabled: Boolean = false,
    val isConfigured: Boolean = false,
    val isTesting: Boolean = false,
    val testResult: String? = null, // null = no result, "ok" or error message
    val isSaving: Boolean = false
)

class SyncSettingsViewModel(
    private val syncEngine: SyncEngine
) : ViewModel() {

    private val syncPrefs = syncEngine.syncPrefs

    private val _uiState = MutableStateFlow(SyncSettingsUiState())
    val uiState: StateFlow<SyncSettingsUiState> = _uiState.asStateFlow()

    val syncStatus: StateFlow<SyncStatus> = syncEngine.syncStatus

    init {
        // Load saved credentials
        _uiState.value = _uiState.value.copy(
            webDavUrl = syncPrefs.getWebDavUrl().ifBlank { "https://dav.jianguoyun.com/dav/" },
            username = syncPrefs.getUsername(),
            password = syncPrefs.getPassword(),
            autoSyncEnabled = syncPrefs.isAutoSyncEnabled(),
            isConfigured = syncPrefs.isConfigured
        )
    }

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
                    // Test by listing root directory
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

    fun triggerSync() {
        syncEngine.triggerManualSync()
    }

    fun getLastSyncTimestamp(): Long = syncPrefs.getLastSyncTimestamp()
}
