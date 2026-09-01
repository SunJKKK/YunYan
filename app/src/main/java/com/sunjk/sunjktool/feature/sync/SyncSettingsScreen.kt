package com.sunjk.sunjktool.feature.sync

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunjk.sunjktool.data.sync.SyncStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettingsScreen(
    viewModel: SyncSettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WebDAV 同步") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── Connection Settings Card ─────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "连接设置",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    TextField(
                        value = uiState.webDavUrl,
                        onValueChange = viewModel::updateWebDavUrl,
                        label = { Text("WebDAV 地址") },
                        placeholder = { Text("https://dav.jianguoyun.com/dav/") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    TextField(
                        value = uiState.username,
                        onValueChange = viewModel::updateUsername,
                        label = { Text("坚果云邮箱") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    TextField(
                        value = uiState.password,
                        onValueChange = viewModel::updatePassword,
                        label = { Text("应用密码") },
                        placeholder = { Text("在坚果云安全选项中生成") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    // Test result
                    uiState.testResult?.let { result ->
                        val isSuccess = result.contains("成功") || result.contains("已保存")
                        Text(
                            result,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSuccess) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = viewModel::testConnection,
                            enabled = !uiState.isTesting
                        ) {
                            if (uiState.isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text("测试连接")
                        }
                        Button(
                            onClick = viewModel::saveCredentials,
                            enabled = !uiState.isSaving
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text("保存")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Sync Controls Card ───────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "同步",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    // Auto-sync toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("自动同步", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "新数据产生时自动上传",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.autoSyncEnabled,
                            onCheckedChange = viewModel::setAutoSync
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Last sync time
                    val lastSync = viewModel.getLastSyncTimestamp()
                    val lastSyncText = if (lastSync > 0) {
                        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        "上次同步: ${fmt.format(Date(lastSync))}"
                    } else "从未同步"

                    Text(
                        lastSyncText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    // Manual sync button
                    val isSyncing = syncStatus is SyncStatus.Syncing
                    Button(
                        onClick = viewModel::triggerSync,
                        enabled = uiState.isConfigured && !isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            val phase = (syncStatus as SyncStatus.Syncing).phase
                            Text("同步中: $phase")
                        } else {
                            Text("立即同步")
                        }
                    }
                }
            }

            // ── Sync Status Banner ───────────────────────────────────────
            if (com.sunjk.sunjktool.ui.theme.LocalAnimationEnabled.current) {
            AnimatedVisibility(
                visible = syncStatus !is SyncStatus.Idle,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (syncStatus) {
                            is SyncStatus.Syncing -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            is SyncStatus.Success -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            is SyncStatus.Error -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        when (val s = syncStatus) {
                            is SyncStatus.Syncing -> {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "正在同步... ${s.phase}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            is SyncStatus.Success -> {
                                Text(
                                    "同步完成 · 已上传 ${s.uploaded} 条，下载 ${s.downloaded} 条",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            is SyncStatus.Error -> {
                                Text(
                                    s.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                if (s.recoverable) {
                                    Spacer(Modifier.height(8.dp))
                                    TextButton(onClick = viewModel::triggerSync) {
                                        Text("重试")
                                    }
                                }
                            }
                            SyncStatus.Idle -> {}
                        }
                    }
                }
            }
            } else {
                if (syncStatus !is SyncStatus.Idle) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (syncStatus) {
                                is SyncStatus.Syncing -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                is SyncStatus.Success -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                is SyncStatus.Error -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            when (val s = syncStatus) {
                                is SyncStatus.Syncing -> {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "正在同步... ${s.phase}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                is SyncStatus.Success -> {
                                    Text(
                                        "同步完成 · 已上传 ${s.uploaded} 条，下载 ${s.downloaded} 条",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                is SyncStatus.Error -> {
                                    Text(
                                        s.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    if (s.recoverable) {
                                        Spacer(Modifier.height(8.dp))
                                        TextButton(onClick = viewModel::triggerSync) {
                                            Text("重试")
                                        }
                                    }
                                }
                                SyncStatus.Idle -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}
