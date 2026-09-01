package com.sunjk.sunjktool.feature.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.sunjk.sunjktool.R
import com.sunjk.sunjktool.data.local.ApiPreferences
import com.sunjk.sunjktool.data.sync.SyncPreferencesManager
import kotlinx.coroutines.launch

/**
 * 首次启动引导（Onboarding）。
 *
 * 仅首次安装启动时展示：欢迎 → 功能(4) → 配置(3，可跳过) → 权限(2 项，可跳过) → 首页。
 * 全部采用 Material 3 组件与主题配色。完成后写入 ApiPreferences#setOnboarded(true)，
 * 后续启动直接进入首页。
 */
private const val PAGE_WELCOME = 0
private const val PAGE_FEATURE_FIRST = 1
private const val PAGE_FEATURE_LAST = 4
private const val PAGE_CONFIG_DEEPSEEK = 5
private const val PAGE_CONFIG_WEBDAV = 6
private const val PAGE_CONFIG_WEATHER = 7
private const val PAGE_PERMISSION = 8
private const val TOTAL_PAGES = 9

@Composable
fun OnboardingScreen(
    apiPreferences: ApiPreferences,
    syncPreferencesManager: SyncPreferencesManager,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { TOTAL_PAGES }

    var deepSeekKey by remember { mutableStateOf(apiPreferences.getDeepSeekKey()) }
    var webDavUrl by remember { mutableStateOf(syncPreferencesManager.getWebDavUrl()) }
    var webDavUser by remember { mutableStateOf(syncPreferencesManager.getUsername()) }
    var webDavPass by remember { mutableStateOf(syncPreferencesManager.getPassword()) }
    var weatherKey by remember { mutableStateOf(apiPreferences.getQWeatherKey()) }

    // ---- 权限状态（回到本页时刷新） ----
    var notifGranted by remember { mutableStateOf(context.notificationsGranted()) }
    var batteryExempt by remember { mutableStateOf(context.isBatteryOptimizationExempt()) }

    LifecycleResumeEffect(Unit) {
        notifGranted = context.notificationsGranted()
        batteryExempt = context.isBatteryOptimizationExempt()
        onPauseOrDispose { }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notifGranted = granted }

    fun goTo(page: Int) = scope.launch { pagerState.animateScrollToPage(page) }

    fun finish() = onFinished()

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false, // 引导页不允许自由滑动，仅按钮驱动
    ) { page ->
        when (page) {
            PAGE_WELCOME -> WelcomePage(
                currentPage = page,
                onBottom = { goTo(PAGE_FEATURE_FIRST) },
            )

            in PAGE_FEATURE_FIRST..PAGE_FEATURE_LAST -> FeaturePage(
                index = page - PAGE_FEATURE_FIRST,
                currentPage = page,
                onSkip = { goTo(PAGE_CONFIG_DEEPSEEK) },
                isLast = page == PAGE_FEATURE_LAST,
                onBottom = {
                    if (page == PAGE_FEATURE_LAST) goTo(PAGE_CONFIG_DEEPSEEK)
                    else goTo(page + 1)
                },
            )

            PAGE_CONFIG_DEEPSEEK -> ConfigPage(
                currentPage = page,
                icon = Icons.Outlined.Key,
                title = "配置 DeepSeek API",
                subtitle = "用于 AI 总结、闪卡、解题、自检等功能",
                skipLabel = "以后再说",
                onSkip = { goTo(PAGE_CONFIG_WEBDAV) },
                bottomLabel = "保存并继续",
                onBottom = {
                    apiPreferences.setDeepSeekKey(deepSeekKey.trim())
                    goTo(PAGE_CONFIG_WEBDAV)
                },
            ) {
                OutlinedTextField(
                    value = deepSeekKey,
                    onValueChange = { deepSeekKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("DeepSeek API Key") },
                    placeholder = { Text("sk-xxxxxxxxxxxxxxxx") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "如何获取？在 platform.deepseek.com 申请",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            PAGE_CONFIG_WEBDAV -> ConfigPage(
                currentPage = page,
                icon = Icons.Outlined.Cloud,
                title = "配置坚果云同步",
                subtitle = "通过 WebDAV 实现多端数据同步（可选）",
                skipLabel = "以后再说",
                onSkip = { goTo(PAGE_CONFIG_WEATHER) },
                bottomLabel = "保存并继续",
                onBottom = {
                    syncPreferencesManager.setWebDavUrl(webDavUrl.trim())
                    syncPreferencesManager.setUsername(webDavUser.trim())
                    syncPreferencesManager.setPassword(webDavPass)
                    goTo(PAGE_CONFIG_WEATHER)
                },
            ) {
                OutlinedTextField(
                    value = webDavUrl,
                    onValueChange = { webDavUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("WebDAV 服务器地址") },
                    placeholder = { Text("https://dav.jianguoyun.com/dav/") },
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = webDavUser,
                    onValueChange = { webDavUser = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("账号（邮箱）") },
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = webDavPass,
                    onValueChange = { webDavPass = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("应用密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "坚果云「安全选项」中创建应用密码，勿使用登录密码",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PAGE_CONFIG_WEATHER -> ConfigPage(
                currentPage = page,
                icon = Icons.Outlined.WbSunny,
                title = "配置和风天气",
                subtitle = "用于首页天气卡片实时显示（可选）",
                skipLabel = "以后再说",
                onSkip = { goTo(PAGE_PERMISSION) },
                bottomLabel = "保存并继续",
                onBottom = {
                    apiPreferences.setQWeatherKey(weatherKey.trim())
                    goTo(PAGE_PERMISSION)
                },
            ) {
                OutlinedTextField(
                    value = weatherKey,
                    onValueChange = { weatherKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("和风天气 API Key") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "如何获取？在 dev.qweather.com 注册并创建 Key",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            else -> PermissionPage(
                currentPage = page,
                notifGranted = notifGranted,
                batteryExempt = batteryExempt,
                onRequestNotification = {
                    if (!notifGranted) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                onRequestBattery = { context.requestBatteryOptimizationExemption() },
                onFinish = ::finish,
            )
        }
    }
}

// ─────────────────────────── 布局骨架 ───────────────────────────

@Composable
private fun OnboardingLayout(
    currentPage: Int,
    bottomLabel: String,
    onBottom: () -> Unit,
    showSkip: Boolean,
    skipLabel: String = "跳过",
    onSkip: (() -> Unit)? = null,
    showDots: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
        ) {
            // 顶栏（可放跳过按钮）
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (showSkip && onSkip != null) {
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    ) { Text(skipLabel) }
                }
            }

            // 中部内容
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) { content() }

            // 底部按钮 + 指示器
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = onBottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(bottomLabel, style = MaterialTheme.typography.titleMedium)
                }
                if (showDots) {
                    Spacer(Modifier.height(16.dp))
                    PageDots(current = currentPage, total = TOTAL_PAGES)
                }
            }
        }
    }
}

@Composable
private fun PageDots(current: Int, total: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            val active = i == current
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (active) 9.dp else 7.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
            )
        }
    }
}

@Composable
private fun FeatureArtwork(icon: ImageVector, containerColor: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(140.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

// ─────────────────────────── 欢迎页 ───────────────────────────

@Composable
private fun WelcomePage(
    currentPage: Int,
    onBottom: () -> Unit,
) {
    OnboardingLayout(
        currentPage = currentPage,
        bottomLabel = "开始体验",
        onBottom = onBottom,
        showSkip = false,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "云砚",
                    modifier = Modifier.size(84.dp),
                )
            }
            Spacer(Modifier.height(32.dp))
            Text(
                text = "从记录到掌握，\n让 AI 陪你进化",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "智能学习记录管家 · AI 驱动 · 多端协同\n考研/考公 · 知识闭环 · 你的第二大脑",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─────────────────────────── 功能介绍页 ───────────────────────────

private data class Feature(val icon: ImageVector, val title: String, val desc: String)

private val features = listOf(
    Feature(
        icon = Icons.Outlined.Description,
        title = "记录你的每一份输入",
        desc = "网课截图、板书、PDF 讲义一键 OCR 提取\n自动生成结构化笔记，构建你的知识库",
    ),
    Feature(
        icon = Icons.Outlined.AutoAwesome,
        title = "AI 智能总结",
        desc = "标准 / 检索增强 / 多 Agent 三种模式\n按需深度提炼，高亮概念与易错点",
    ),
    Feature(
        icon = Icons.Outlined.Style,
        title = "闪卡答题 & 自检",
        desc = "一键生成记忆卡片，透明画板演算\n色块遮罩自检，像评论区一样检验记忆",
    ),
    Feature(
        icon = Icons.Outlined.Dashboard,
        title = "概览复盘 & 多端协同",
        desc = "日历视图聚合每日数据，番茄钟专注计时\n坚果云 WebDAV 多端同步你的学习资产",
    ),
)

@Composable
private fun FeaturePage(
    index: Int,
    currentPage: Int,
    onSkip: () -> Unit,
    isLast: Boolean,
    onBottom: () -> Unit,
) {
    val feature = features[index]
    OnboardingLayout(
        currentPage = currentPage,
        bottomLabel = if (isLast) "开始配置" else "下一步",
        onBottom = onBottom,
        showSkip = true,
        onSkip = onSkip,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FeatureArtwork(
                icon = feature.icon,
                containerColor = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(32.dp))
            Text(
                text = feature.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = feature.desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─────────────────────────── 配置页 ───────────────────────────

@Composable
private fun ConfigPage(
    currentPage: Int,
    icon: ImageVector,
    title: String,
    subtitle: String,
    skipLabel: String,
    onSkip: () -> Unit,
    bottomLabel: String,
    onBottom: () -> Unit,
    content: @Composable () -> Unit,
) {
    OnboardingLayout(
        currentPage = currentPage,
        bottomLabel = bottomLabel,
        onBottom = onBottom,
        showSkip = true,
        skipLabel = skipLabel,
        onSkip = onSkip,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
            content()
        }
    }
}

// ─────────────────────────── 权限申请页 ───────────────────────────

@Composable
private fun PermissionPage(
    currentPage: Int,
    notifGranted: Boolean,
    batteryExempt: Boolean,
    onRequestNotification: () -> Unit,
    onRequestBattery: () -> Unit,
    onFinish: () -> Unit,
) {
    OnboardingLayout(
        currentPage = currentPage,
        bottomLabel = "开始使用云砚",
        onBottom = onFinish,
        showSkip = false,
        showDots = false,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = "完善番茄钟体验",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "两项权限可选，均可稍后在设置中开启",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            PermissionCard(
                icon = Icons.Outlined.Notifications,
                title = "通知权限",
                desc = "用于番茄钟专注结束、休息提醒等通知",
                granted = notifGranted,
                grantedText = "已授予",
                actionText = "去开启",
                onAction = onRequestNotification,
            )
            Spacer(Modifier.height(16.dp))

            PermissionCard(
                icon = Icons.Outlined.BatteryChargingFull,
                title = "后台运行",
                desc = "允许在息屏/后台持续计时，不被系统杀死",
                granted = batteryExempt,
                grantedText = "已豁免",
                actionText = "去开启",
                onAction = onRequestBattery,
            )
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    desc: String,
    granted: Boolean,
    grantedText: String,
    actionText: String,
    onAction: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onAction) {
                Text(
                    text = if (granted) grantedText else actionText,
                    color = if (granted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    fontWeight = if (granted) FontWeight.Normal else FontWeight.SemiBold,
                )
            }
        }
    }
}

// ─────────────────────────── 权限辅助函数 ───────────────────────────

private fun Context.notificationsGranted(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        this, Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

private fun Context.isBatteryOptimizationExempt(): Boolean {
    val pm = getSystemService(PowerManager::class.java)
    return pm.isIgnoringBatteryOptimizations(packageName)
}

private fun Context.requestBatteryOptimizationExemption() {
    try {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:$packageName"),
        )
        startActivity(intent)
    } catch (_: Exception) {
        try {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        } catch (_: Exception) {
            // 无可用入口，静默忽略
        }
    }
}
