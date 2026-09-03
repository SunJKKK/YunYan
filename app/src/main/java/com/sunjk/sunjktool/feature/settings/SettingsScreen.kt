package com.sunjk.sunjktool.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Visibility
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunjk.sunjktool.data.local.ApiPreferences
import com.sunjk.sunjktool.data.local.AiModelOption
import com.sunjk.sunjktool.data.local.PromptKeys
import com.sunjk.sunjktool.data.sync.SyncStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 设置子页面分类 */
enum class SettingsSection(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
) {
    API_KEYS("API 密钥", "DeepSeek、和风天气等密钥", Icons.Outlined.Key),
    AI("AI 设置", "模型选择、并行总结、提示词", Icons.Outlined.AutoAwesome),
    WEBDAV("WebDAV 同步", "坚果云多端同步与自动同步", Icons.Outlined.Sync),
    TICKTICK("滴答清单", "待办登录、Token 与任务展示", Icons.Outlined.Checklist),
    DISPLAY("显示", "主题、动画与平板端模式", Icons.Outlined.Visibility),
    ABOUT("关于", "引导页、更新与开源仓库", Icons.Outlined.Info),
}

/** 设置一级菜单页 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateTo: (SettingsSection) -> Unit,
) {
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

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
            SettingsSection.entries.forEach { sec ->
                SettingsMenuItem(section = sec, onClick = { onNavigateTo(sec) })
                Spacer(Modifier.height(12.dp))
            }

            // ── 同步状态横幅 ────────────────────────────────────────
            SyncStatusBanner(syncStatus = syncStatus, onRetry = viewModel::triggerSync)
        }
    }
}

// ─────────────────────────── 各二级页面 ───────────────────────────

@Composable
fun SettingsApiKeysScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    SettingsSubPage(title = "API 密钥", onBack = onBack) {
        ApiKeysContent(viewModel)
    }
}

@Composable
fun SettingsAiScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    var showPrompts by remember { mutableStateOf(false) }
    BackHandler(enabled = showPrompts) { showPrompts = false }
    if (showPrompts) {
        SettingsPromptsScreen(viewModel = viewModel, onBack = { showPrompts = false })
        return
    }
    SettingsSubPage(title = "AI 设置", onBack = onBack) {
        AiContent(viewModel, onOpenPrompts = { showPrompts = true })
    }
}

@Composable
fun SettingsWebDavScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    SettingsSubPage(title = "WebDAV 同步", onBack = onBack) {
        WebDavContent(viewModel)
    }
}

@Composable
fun SettingsTickTickScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(enabled = uiState.showTickTickWebLogin) { viewModel.dismissTickTickWebLogin() }
    if (uiState.showTickTickWebLogin) {
        TickTickWebLoginScreen(
            onTokenCaptured = viewModel::onTickTickTokenCaptured,
            onClose = viewModel::dismissTickTickWebLogin
        )
        return
    }
    SettingsSubPage(title = "滴答清单", onBack = onBack) {
        TickTickContent(viewModel)
    }
}

@Composable
fun SettingsDisplayScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    SettingsSubPage(title = "显示", onBack = onBack) {
        DisplayContent(viewModel)
    }
}

@Composable
fun SettingsAboutScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 发现新版本 → 弹出下载对话框
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

    SettingsSubPage(title = "关于", onBack = onBack) {
        AboutContent(
            viewModel = viewModel,
            context = context,
            onNavigateToOnboarding = onNavigateToOnboarding
        )
    }
}

/** 子页面通用骨架：顶栏 + 返回 + 滚动内容 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSubPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text(title) },
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
            content()
        }
    }
}

/** 主菜单入口：tonal 圆形图标 + 标题 + 副标题 + 箭头（样式同笔记本图标） */
@Composable
private fun SettingsMenuItem(
    section: SettingsSection,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标放在 tonal 圆形底衬里（与笔记本页一致）
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    section.icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = section.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────── API 密钥 ───────────────────────────

@Composable
private fun ApiKeysContent(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ApiProviderCard(
            title = "DeepSeek",
            description = "主力生成模型 · OpenAI 兼容接口"
        ) {
            ApiKeyField(
                value = uiState.deepSeekKey,
                onValueChange = viewModel::updateDeepSeekKey,
                label = "API Key",
                placeholder = "sk-...",
                password = true
            )
            ApiKeyField(
                value = uiState.deepSeekBaseUrl,
                onValueChange = viewModel::updateDeepSeekBaseUrl,
                label = "API 地址",
                placeholder = "https://api.deepseek.com"
            )
            ApiKeyField(
                value = uiState.deepSeekModel,
                onValueChange = viewModel::setDeepSeekModel,
                label = "模型名",
                placeholder = ApiPreferences.MODEL_V4_FLASH
            )
        }

        ApiProviderCard(
            title = "通义千问（Qwen）",
            description = "可选 · 支持联网搜索，与 DeepSeek 同为 OpenAI 兼容接口"
        ) {
            ApiKeyField(
                value = uiState.qwenKey,
                onValueChange = viewModel::updateQwenKey,
                label = "API Key",
                placeholder = "sk-...",
                password = true
            )
            ApiKeyField(
                value = uiState.qwenBaseUrl,
                onValueChange = viewModel::updateQwenBaseUrl,
                label = "API 地址",
                placeholder = "https://dashscope.aliyuncs.com/compatible-mode/v1"
            )
            ApiKeyField(
                value = uiState.qwenModel,
                onValueChange = viewModel::updateQwenModel,
                label = "模型名",
                placeholder = ApiPreferences.MODEL_QWEN_FLASH
            )
        }

        ApiProviderCard(
            title = "和风天气",
            description = "天气功能数据源"
        ) {
            ApiKeyField(
                value = uiState.qweatherKey,
                onValueChange = viewModel::updateQWeatherKey,
                label = "API Key",
                placeholder = "你的和风天气 Key",
                password = true
            )
        }

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
        }

        Button(
            onClick = viewModel::saveApiKeys,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存 API 密钥")
        }
    }
}

/** 统一设置分组卡片：标题 + 说明 + 内容区 */
@Composable
private fun ApiProviderCard(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content()
            }
        }
    }
}

/** 设置表单字段：M3 OutlinedTextField */
@Composable
private fun ApiKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    password: Boolean = false,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = modifier.fillMaxWidth()
    )
}

/** 统一设置 Switch 行：标题 + 说明 + Switch */
@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** 统一设置导航行：标题 + 说明 + 右箭头（可自定义尾部） */
@Composable
private fun SettingsNavRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────── AI 设置 ───────────────────────────

@Composable
private fun AiContent(
    viewModel: SettingsViewModel,
    onOpenPrompts: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val defaultAiOption = AiModelOption.fromId(uiState.defaultAiModel)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ApiProviderCard(
            title = "默认 AI 模型",
            description = "未单独选择模型时的全局默认"
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AiModelOption.entries.forEachIndexed { index, opt ->
                    SegmentedButton(
                        selected = uiState.defaultAiModel == opt.id,
                        onClick = { viewModel.setDefaultAiModel(opt.id) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = AiModelOption.entries.size)
                    ) { Text(opt.label, style = MaterialTheme.typography.labelMedium) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "当前默认：${defaultAiOption.label} — ${defaultAiOption.desc}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ApiProviderCard(
            title = "生成行为",
            description = "调整多Agent与题集的生成方式"
        ) {
            SettingsSwitchRow(
                title = "多Agent主题总结并行",
                subtitle = "同时总结多个主题以提速，关闭则逐个顺序总结",
                checked = uiState.multiAgentParallel,
                onCheckedChange = viewModel::setMultiAgentParallel
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsSwitchRow(
                title = "题集一键直达",
                subtitle = "生成题目时跳过拆分确认和解析预览，直接保存",
                checked = uiState.questionBankAutoSave,
                onCheckedChange = viewModel::setQuestionBankAutoSave
            )
        }

        ApiProviderCard(
            title = "AI 提示词",
            description = "自定义自检、闪卡等生成的提示词"
        ) {
            SettingsNavRow(
                title = "编辑提示词",
                subtitle = "进入提示词编辑页，支持恢复默认",
                onClick = onOpenPrompts
            )
        }
    }
}

// ─────────────────────────── WebDAV 同步 ───────────────────────────

@Composable
private fun WebDavContent(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ApiProviderCard(
            title = "连接配置",
            description = "WebDAV（坚果云）服务器信息"
        ) {
            ApiKeyField(
                value = uiState.webDavUrl,
                onValueChange = viewModel::updateWebDavUrl,
                label = "WebDAV 地址",
                placeholder = "https://dav.jianguoyun.com/dav/"
            )
            ApiKeyField(
                value = uiState.username,
                onValueChange = viewModel::updateUsername,
                label = "坚果云邮箱"
            )
            ApiKeyField(
                value = uiState.password,
                onValueChange = viewModel::updatePassword,
                label = "应用密码",
                placeholder = "在坚果云安全选项中生成",
                password = true
            )
            uiState.testResult?.let { result ->
                val isSuccess = result.contains("成功") || result.contains("已保存")
                Text(
                    result,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSuccess) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = viewModel::testConnection,
                    enabled = !uiState.isTesting,
                    modifier = Modifier.weight(1f)
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
                    enabled = !uiState.isSaving,
                    modifier = Modifier.weight(1f)
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

        ApiProviderCard(
            title = "同步行为",
            description = "自动上传与手动同步"
        ) {
            SettingsSwitchRow(
                title = "自动同步",
                subtitle = "新数据产生时自动上传",
                checked = uiState.autoSyncEnabled,
                onCheckedChange = viewModel::setAutoSync
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
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
            Spacer(Modifier.height(8.dp))
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
                    Text("同步中: ${(syncStatus as SyncStatus.Syncing).phase}")
                } else {
                    Text("立即同步")
                }
            }
        }
    }
}

// ─────────────────────────── 滴答清单 ───────────────────────────

@Composable
private fun TickTickContent(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ApiProviderCard(
            title = "滴答清单账号",
            description = "通过内嵌浏览器登录，或手动粘贴 Cookie/Token"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (uiState.tickTickToken.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (uiState.tickTickToken.isNotBlank()) "已登录" else "未登录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.tickTickToken.isNotBlank())
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
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
            if (uiState.tickTickToken.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
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
            }
        }

        ApiProviderCard(
            title = "已完成任务展示",
            description = "选择已完成任务的展示范围"
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = uiState.tickTickCompletedMode == ApiPreferences.COMPLETED_MODE_ALL,
                    onClick = { viewModel.setTickTickCompletedMode(ApiPreferences.COMPLETED_MODE_ALL) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) { Text("全部展示", style = MaterialTheme.typography.labelMedium) }
                SegmentedButton(
                    selected = uiState.tickTickCompletedMode == ApiPreferences.COMPLETED_MODE_NONE,
                    onClick = { viewModel.setTickTickCompletedMode(ApiPreferences.COMPLETED_MODE_NONE) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) { Text("不展示", style = MaterialTheme.typography.labelMedium) }
                SegmentedButton(
                    selected = uiState.tickTickCompletedMode == ApiPreferences.COMPLETED_MODE_TODAY,
                    onClick = { viewModel.setTickTickCompletedMode(ApiPreferences.COMPLETED_MODE_TODAY) },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) { Text("仅今天", style = MaterialTheme.typography.labelMedium) }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "全部清单均展示 / 均不展示 / 仅展示今天到期的已完成任务",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ApiProviderCard(
            title = "手动输入 Cookie/Token",
            description = "浏览器登录不可用时的备选方式"
        ) {
            OutlinedTextField(
                value = uiState.tickTickManualToken,
                onValueChange = viewModel::updateTickTickManualToken,
                label = { Text("粘贴 Cookie 或 Token") },
                placeholder = { Text("可含 t=...") },
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
            uiState.tickTickTestResult?.let { result ->
                val isSuccess = result.contains("成功") || result.contains("已清除") || result.contains("已退出") || result.contains("已保存")
                Text(
                    result,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSuccess) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ─────────────────────────── 显示（含平板端模式） ───────────────────────────

@Composable
private fun DisplayContent(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ApiProviderCard(
            title = "布局与主题",
            description = "平板布局与界面主题"
        ) {
            SettingsSwitchRow(
                title = "平板端模式",
                subtitle = "学习记录详情页使用平板双栏布局",
                checked = uiState.tabletMode,
                onCheckedChange = viewModel::setTabletMode
            )
            if (uiState.tabletMode) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSwitchRow(
                    title = "只同步主要内容",
                    subtitle = "只下载、不上传；不下载图片和附件大文件",
                    checked = uiState.readOnlySync,
                    onCheckedChange = viewModel::setReadOnlySync
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("主题模式", style = MaterialTheme.typography.labelLarge)
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
        }

        ApiProviderCard(
            title = "动效",
            description = "界面动画与过渡效果"
        ) {
            SettingsSwitchRow(
                title = "动画效果",
                subtitle = "关闭以优化墨水屏体验",
                checked = uiState.animationEnabled,
                onCheckedChange = viewModel::setAnimationEnabled
            )
        }
    }
}

// ─────────────────────────── 关于 ───────────────────────────

@Composable
private fun AboutContent(
    viewModel: SettingsViewModel,
    context: android.content.Context,
    onNavigateToOnboarding: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.4"
        } catch (_: Exception) { "1.4" }
    }
    ApiProviderCard(
        title = "关于",
        description = "版本信息与功能入口"
    ) {
        SettingsNavRow(
            title = "引导页",
            subtitle = "重新查看功能介绍与配置",
            onClick = onNavigateToOnboarding
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingsNavRow(
            title = "开源仓库",
            subtitle = "github.com/SunJKKK/YunYan",
            onClick = {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://github.com/SunJKKK/YunYan")
                )
                context.startActivity(intent)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingsNavRow(
            title = "检查更新",
            subtitle = uiState.updateStatusText ?: "当前版本 v$versionName",
            onClick = { viewModel.checkUpdate(versionName) },
            trailing = {
                if (uiState.isCheckingUpdate) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else if (uiState.updateDialogUrl != null) {
                    Icon(
                        Icons.Outlined.Update,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }
}

// ─────────────────────────── 同步状态横幅 ───────────────────────────

@Composable
private fun SyncStatusBanner(
    syncStatus: SyncStatus,
    onRetry: () -> Unit
) {
    val animEnabled = com.sunjk.sunjktool.ui.theme.LocalAnimationEnabled.current
    if (animEnabled) {
        AnimatedVisibility(
            visible = syncStatus !is SyncStatus.Idle,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SyncStatusCard(syncStatus, onRetry)
        }
    } else {
        if (syncStatus !is SyncStatus.Idle) {
            SyncStatusCard(syncStatus, onRetry)
        }
    }
}

@Composable
private fun SyncStatusCard(syncStatus: SyncStatus, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
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
                        TextButton(onClick = onRetry) {
                            Text("重试")
                        }
                    }
                }
                SyncStatus.Idle -> {}
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
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "自定义此功能的生成提示词，留空使用内置提示词",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                placeholder = { Text("输入自定义提示词") }
            )
            Spacer(Modifier.height(8.dp))
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
