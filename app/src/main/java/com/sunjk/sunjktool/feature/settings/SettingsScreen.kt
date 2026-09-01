package com.sunjk.sunjktool.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneAndroid
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunjk.sunjktool.data.local.ApiPreferences
import com.sunjk.sunjktool.data.local.PromptKeys
import com.sunjk.sunjktool.data.sync.SyncStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    var showPrompts by remember { mutableStateOf(false) }

    if (showPrompts) {
        SettingsPromptsScreen(viewModel = viewModel, onBack = { showPrompts = false })
        return
    }

    if (uiState.showTickTickWebLogin) {
        TickTickWebLoginScreen(
            onTokenCaptured = viewModel::onTickTickTokenCaptured,
            onClose = viewModel::dismissTickTickWebLogin
        )
        return
    }

    // 发现新版本 → 弹出下载对话框
    val context = androidx.compose.ui.platform.LocalContext.current
    val updateDialogUrl = uiState.updateDialogUrl
    if (updateDialogUrl != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = viewModel::dismissUpdateDialog,
            title = { Text("发现新版本") },
            text = {
                Text("最新版本 ${uiState.updateDialogVersion ?: ""} 已发布，是否前往 GitHub 下载？")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissUpdateDialog()
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(updateDialogUrl)
                    )
                    context.startActivity(intent)
                }) { Text("前往下载") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissUpdateDialog) { Text("取消") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("设置") },
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
            // ── API Keys Card ──────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "API 密钥",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.deepSeekKey,
                        onValueChange = viewModel::updateDeepSeekKey,
                        label = { Text("DeepSeek API Key") },
                        placeholder = { Text("sk-...") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.deepSeekBaseUrl,
                        onValueChange = viewModel::updateDeepSeekBaseUrl,
                        label = { Text("DeepSeek API 地址") },
                        placeholder = { Text("https://api.deepseek.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.qweatherKey,
                        onValueChange = viewModel::updateQWeatherKey,
                        label = { Text("和风天气 API Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))

                    uiState.apiKeySaveResult?.let { result ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                result,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Button(
                        onClick = viewModel::saveApiKeys,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存 API 密钥")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── AI 设置 Card ──────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "AI 设置",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "AI 总结、闪卡等生成功能的模型与行为",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = uiState.deepSeekModel == ApiPreferences.MODEL_V4_FLASH,
                            onClick = { viewModel.setDeepSeekModel(ApiPreferences.MODEL_V4_FLASH) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) { Text("V4 Flash", style = MaterialTheme.typography.labelMedium) }
                        SegmentedButton(
                            selected = uiState.deepSeekModel == ApiPreferences.MODEL_V4_PRO,
                            onClick = { viewModel.setDeepSeekModel(ApiPreferences.MODEL_V4_PRO) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) { Text("V4 Pro", style = MaterialTheme.typography.labelMedium) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (uiState.deepSeekModel == ApiPreferences.MODEL_V4_PRO)
                            "V4 Pro：能力更强，适合复杂内容，费用较高"
                        else
                            "V4 Flash：速度快、成本低，适合日常使用",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("多Agent主题总结并行", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "同时总结多个主题以提速，关闭则逐个顺序总结",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.multiAgentParallel,
                            onCheckedChange = viewModel::setMultiAgentParallel
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("题集一键直达", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "生成题目时跳过拆分确认和解析预览，直接保存",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.questionBankAutoSave,
                            onCheckedChange = viewModel::setQuestionBankAutoSave
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // AI 提示词 → 二级页面
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPrompts = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AI 提示词", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "自定义自检、闪卡等生成的提示词",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            // ── WebDAV Card ───────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "WebDAV 同步",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.webDavUrl,
                        onValueChange = viewModel::updateWebDavUrl,
                        label = { Text("WebDAV 地址") },
                        placeholder = { Text("https://dav.jianguoyun.com/dav/") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = viewModel::updateUsername,
                        label = { Text("坚果云邮箱") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = viewModel::updatePassword,
                        label = { Text("应用密码") },
                        placeholder = { Text("在坚果云安全选项中生成") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))

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

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

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

            Spacer(Modifier.height(16.dp))


            // ── TickTick Card ─────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "滴答清单",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "用于待办功能。通过内嵌浏览器登录同步任务，也可手动粘贴 Cookie/Token。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    // 登录状态
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (uiState.tickTickToken.isNotBlank()) "已登录" else "未登录",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (uiState.tickTickToken.isNotBlank())
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    // 登录 + 清除缓存
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = viewModel::openTickTickWebLogin,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("登录滴答清单")
                        }
                        OutlinedButton(
                            onClick = viewModel::clearTickTickCache,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("清除缓存")
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    if (uiState.tickTickToken.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = viewModel::testTickTickConnection,
                                enabled = !uiState.tickTickTesting,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (uiState.tickTickTesting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text("测试连接")
                            }
                            OutlinedButton(
                                onClick = viewModel::signOutTickTick,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("退出登录")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // 已完成任务显示模式
                    Text(
                        "已完成任务展示",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(6.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = uiState.tickTickCompletedMode == ApiPreferences.COMPLETED_MODE_ALL,
                            onClick = { viewModel.setTickTickCompletedMode(ApiPreferences.COMPLETED_MODE_ALL) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                        ) { Text("全部展示", style = MaterialTheme.typography.labelSmall) }
                        SegmentedButton(
                            selected = uiState.tickTickCompletedMode == ApiPreferences.COMPLETED_MODE_NONE,
                            onClick = { viewModel.setTickTickCompletedMode(ApiPreferences.COMPLETED_MODE_NONE) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                        ) { Text("不展示", style = MaterialTheme.typography.labelSmall) }
                        SegmentedButton(
                            selected = uiState.tickTickCompletedMode == ApiPreferences.COMPLETED_MODE_TODAY,
                            onClick = { viewModel.setTickTickCompletedMode(ApiPreferences.COMPLETED_MODE_TODAY) },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                        ) { Text("仅今天", style = MaterialTheme.typography.labelSmall) }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "已完成任务的展示范围：全部清单均展示 / 均不展示 / 仅展示今天到期的已完成任务",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // 手动输入 Cookie/Token
                    Text(
                        "手动输入 Cookie/Token",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = uiState.tickTickManualToken,
                        onValueChange = viewModel::updateTickTickManualToken,
                        label = { Text("粘贴 Cookie 或 Token（可含 t=...）") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = viewModel::saveTickTickManualToken,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存")
                    }

                    Spacer(Modifier.height(8.dp))

                    uiState.tickTickTestResult?.let { result ->
                        val isSuccess = result.contains("成功") || result.contains("已清除") || result.contains("已退出") || result.contains("已保存")
                        Text(
                            result,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSuccess) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))


            // ── Tablet Card ────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("平板端模式", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "开启后学习记录详情页使用平板双栏布局",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.tabletMode,
                            onCheckedChange = viewModel::setTabletMode
                        )
                    }

                    if (uiState.tabletMode) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("只同步主要内容", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "只下载、不上传；不下载图片和附件大文件",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.readOnlySync,
                                onCheckedChange = viewModel::setReadOnlySync
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            // ── Display Card ────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "显示",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    Text(
                        "主题模式",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = uiState.themeMode == ApiPreferences.THEME_MODE_LIGHT,
                            onClick = { viewModel.setThemeMode(ApiPreferences.THEME_MODE_LIGHT) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                            icon = { Icon(Icons.Outlined.LightMode, null, Modifier.size(18.dp)) }
                        ) { Text("浅色", style = MaterialTheme.typography.labelMedium) }
                        SegmentedButton(
                            selected = uiState.themeMode == ApiPreferences.THEME_MODE_SYSTEM,
                            onClick = { viewModel.setThemeMode(ApiPreferences.THEME_MODE_SYSTEM) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                            icon = { Icon(Icons.Outlined.PhoneAndroid, null, Modifier.size(18.dp)) }
                        ) { Text("自动", style = MaterialTheme.typography.labelMedium) }
                        SegmentedButton(
                            selected = uiState.themeMode == ApiPreferences.THEME_MODE_DARK,
                            onClick = { viewModel.setThemeMode(ApiPreferences.THEME_MODE_DARK) },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                            icon = { Icon(Icons.Outlined.DarkMode, null, Modifier.size(18.dp)) }
                        ) { Text("深色", style = MaterialTheme.typography.labelMedium) }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("动画效果", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "关闭以优化墨水屏体验",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.animationEnabled,
                            onCheckedChange = viewModel::setAnimationEnabled
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── About Card ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val versionName = remember {
                        try {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.4"
                        } catch (_: Exception) { "1.4" }
                    }

                    // 开源仓库
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://github.com/SunJKKK/YunYan")
                                )
                                context.startActivity(intent)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("开源仓库", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "github.com/SunJKKK/YunYan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // 检查更新
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.checkUpdate(versionName) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("检查更新", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                uiState.updateStatusText ?: "当前版本 v$versionName",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiState.updateDialogUrl != null)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (uiState.isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

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
                    shape = MaterialTheme.shapes.medium,
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
                        shape = MaterialTheme.shapes.medium,
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

// AI 提示词二级页面
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsPromptsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("AI 提示词") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            Text(
                "默认显示当前内置提示词，修改后点击保存；恢复默认会还原为内置提示词。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            PromptEditField(
                label = "自检生成",
                value = uiState.selfCheckPrompt,
                onValueChange = { viewModel.updatePrompt(PromptKeys.SELF_CHECK, it) },
                onSave = { viewModel.savePrompt(PromptKeys.SELF_CHECK) },
                onReset = { viewModel.resetPrompt(PromptKeys.SELF_CHECK) }
            )
            PromptEditField(
                label = "闪卡生成",
                value = uiState.flashcardPrompt,
                onValueChange = { viewModel.updatePrompt(PromptKeys.FLASHCARD, it) },
                onSave = { viewModel.savePrompt(PromptKeys.FLASHCARD) },
                onReset = { viewModel.resetPrompt(PromptKeys.FLASHCARD) }
            )
            PromptEditField(
                label = "缺口分析",
                value = uiState.gapAnalysisPrompt,
                onValueChange = { viewModel.updatePrompt(PromptKeys.GAP_ANALYSIS, it) },
                onSave = { viewModel.savePrompt(PromptKeys.GAP_ANALYSIS) },
                onReset = { viewModel.resetPrompt(PromptKeys.GAP_ANALYSIS) }
            )
            PromptEditField(
                label = "知识检索",
                value = uiState.knowledgeRetrievalPrompt,
                onValueChange = { viewModel.updatePrompt(PromptKeys.KNOWLEDGE_RETRIEVAL, it) },
                onSave = { viewModel.savePrompt(PromptKeys.KNOWLEDGE_RETRIEVAL) },
                onReset = { viewModel.resetPrompt(PromptKeys.KNOWLEDGE_RETRIEVAL) }
            )
            PromptEditField(
                label = "题目拆分",
                value = uiState.questionSplitPrompt,
                onValueChange = { viewModel.updatePrompt(PromptKeys.QUESTION_SPLIT, it) },
                onSave = { viewModel.savePrompt(PromptKeys.QUESTION_SPLIT) },
                onReset = { viewModel.resetPrompt(PromptKeys.QUESTION_SPLIT) }
            )
            PromptEditField(
                label = "题目解析",
                value = uiState.questionAnalysisPrompt,
                onValueChange = { viewModel.updatePrompt(PromptKeys.QUESTION_ANALYSIS, it) },
                onSave = { viewModel.savePrompt(PromptKeys.QUESTION_ANALYSIS) },
                onReset = { viewModel.resetPrompt(PromptKeys.QUESTION_ANALYSIS) }
            )
        }
    }
}

@Composable
private fun PromptEditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6,
            placeholder = { Text("输入自定义提示词") }
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                Text("保存")
            }
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                Text("恢复默认")
            }
        }
    }
}

// 滴答清单内嵌浏览器登录页：顶部栏 + WebView，自动抓取 token
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TickTickWebLoginScreen(
    onTokenCaptured: (String, String) -> Unit,
    onClose: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("滴答清单登录") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "关闭")
                    }
                },
                actions = {
                    Text(
                        "登录成功后自动关闭",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            DidaLoginWebView(onTokenCaptured = { t, c -> onTokenCaptured(t, c) })
        }
    }
}