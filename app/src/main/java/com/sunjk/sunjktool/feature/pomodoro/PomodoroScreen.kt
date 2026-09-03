package com.sunjk.sunjktool.feature.pomodoro

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunjk.sunjktool.domain.model.PomodoroPhase
import com.sunjk.sunjktool.ui.components.ConfirmDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    viewModel: PomodoroViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pomodoroState by viewModel.managerState.collectAsStateWithLifecycle()

    val isActive = pomodoroState.isRunning || pomodoroState.remainingSecs > 0
    val isRunning = pomodoroState.isRunning
    val progress = if (pomodoroState.totalSecs > 0)
        (pomodoroState.totalSecs - pomodoroState.remainingSecs).toFloat() / pomodoroState.totalSecs else 0f
    val mins = pomodoroState.remainingSecs / 60
    val secs = pomodoroState.remainingSecs % 60
    val timeText = String.format("%02d:%02d", mins, secs)
    val phaseText = when (pomodoroState.phase) {
        PomodoroPhase.FOCUS -> "工作中"
        PomodoroPhase.BREAK -> "休息中"
        PomodoroPhase.IDLE -> "待开始"
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("番茄钟") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.DateRange, "历史记录")
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress ring：外层圆形底 + 进度圆环 + 中心时间与阶段胶囊
            val progressColor = if (pomodoroState.phase == PomodoroPhase.BREAK)
                MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            val ringShared = sharedTransitionScope?.let { s ->
                with(s) {
                    animatedVisibilityScope?.let { scope ->
                        Modifier.sharedBounds(rememberSharedContentState("pomodoro_home_card"), scope)
                    } ?: Modifier
                }
            } ?: Modifier
            Box(contentAlignment = Alignment.Center, modifier = ringShared.size(260.dp)) {
                // 圆形底色容器 + 进度圆环
                val circleColor = MaterialTheme.colorScheme.surfaceContainerLow
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = circleColor)
                    // 进度圆环
                    val strokeW = 14.dp.toPx()
                    val inset = strokeW / 2 + 8.dp.toPx()
                    val topLeft = Offset(inset, inset)
                    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                    drawArc(
                        color = trackColor,
                        startAngle = -90f, sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft, size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = topLeft, size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        timeText,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    // 阶段胶囊标签
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isActive) progressColor.copy(alpha = 0.14f)
                                else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            phaseText,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isActive) progressColor
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // 进行中：轮次信息
            if (isActive) {
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = progressColor
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "第 ${pomodoroState.completedCount + 1} 个番茄 · 完成后休息 ${uiState.breakMinutes} 分钟",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(Modifier.height(20.dp))

                // 时长设置卡片
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "时长设置",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // 预设分段按钮
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            listOf(30 to "30分钟", 60 to "1小时", 120 to "2小时")
                                .forEachIndexed { index, (min, label) ->
                                    SegmentedButton(
                                        selected = uiState.workMinutes == min,
                                        onClick = { viewModel.setWorkMinutes(min) },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                                        label = { Text(label, style = MaterialTheme.typography.labelLarge) }
                                    )
                                }
                        }

                        // 工作时长滑杆（5 分钟一档）
                        Text(
                            "工作时长：${uiState.workMinutes} 分钟",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = uiState.workMinutes.toFloat(),
                            onValueChange = { viewModel.setWorkMinutes(Math.round(it / 5).toInt() * 5) },
                            valueRange = 5f..120f, steps = 23, // (120-5)/5=23 档间隔，恰好 5 分钟一档
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "完成后自动休息",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(checked = !uiState.skipBreak, onCheckedChange = { viewModel.setSkipBreak(!it) })
                        }
                        if (!uiState.skipBreak) {
                            Text(
                                "休息时长：${uiState.breakMinutes} 分钟",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = uiState.breakMinutes.toFloat(),
                                onValueChange = { viewModel.setBreakMinutes(Math.round(it / 5).toInt() * 5) },
                                valueRange = 5f..60f, steps = 10,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 控制按钮
            if (!isActive) {
                Button(
                    onClick = { viewModel.start() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("开始专注", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isRunning) {
                        Button(
                            onClick = { viewModel.pause() },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Pause, null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("暂停", style = MaterialTheme.typography.titleMedium)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.resume() },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("继续", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    FilledTonalButton(
                        onClick = { viewModel.preStop() },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Default.Stop, null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("结束", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 今日统计卡片
            val totalFocusMins = pomodoroState.totalFocusSecs / 60
            val focusHours = totalFocusMins / 60
            val focusMins = totalFocusMins % 60
            val focusText = if (focusHours > 0) "${focusHours}小时${focusMins}分" else "${focusMins}分钟"
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    icon = Icons.Default.Whatshot,
                    value = focusText,
                    label = "今日专注",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Default.CheckCircle,
                    value = "${pomodoroState.completedCount} 个",
                    label = "今日完成",
                    modifier = Modifier.weight(1f)
                )
            }

            // Confirm dialog: keep or discard elapsed time
            if (pomodoroState.pendingStopSecs > 0) {
                val pendingMins = pomodoroState.pendingStopSecs / 60
                ConfirmDialog(
                    title = "计入专注时长？",
                    message = "已进行了 ${pendingMins} 分钟，是否计入总专注时长？",
                    confirmText = "计入",
                    dismissText = "放弃",
                    onConfirm = { viewModel.confirmStop(true) },
                    onDismiss = { viewModel.confirmStop(false) }
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
