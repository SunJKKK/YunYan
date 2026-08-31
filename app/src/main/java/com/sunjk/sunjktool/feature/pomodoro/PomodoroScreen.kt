package com.sunjk.sunjktool.feature.pomodoro

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap


import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunjk.sunjktool.ui.components.ConfirmDialog
import com.sunjk.sunjktool.domain.model.PomodoroPhase

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
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress ring
            val progressColor = MaterialTheme.colorScheme.primary
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            val ringShared = sharedTransitionScope?.let { s ->
                with(s) {
                    animatedVisibilityScope?.let { scope ->
                        Modifier.sharedBounds(rememberSharedContentState("pomodoro_home_card"), scope)
                    } ?: Modifier
                }
            } ?: Modifier
            Box(contentAlignment = Alignment.Center, modifier = ringShared.size(220.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 12.dp.toPx()
                    val topLeft = Offset(strokeW / 2, strokeW / 2)
                    val arcSize = Size(size.width - strokeW, size.height - strokeW)
                    // Background arc
                    drawArc(
                        color = trackColor,
                        startAngle = -90f, sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft, size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                    // Progress arc
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
                    Text(timeText, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                    Text(phaseText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Preset buttons
            if (!isActive) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30 to "30分钟", 60 to "1小时", 120 to "2小时").forEach { (min, label) ->
                        FilledTonalButton(
                            onClick = { viewModel.setWorkMinutes(min) },
                            modifier = Modifier.weight(1f)
                        ) { Text(label, style = MaterialTheme.typography.labelLarge) }
                    }
                }
                Spacer(Modifier.height(20.dp))

                Text("工作时长: ${uiState.workMinutes}分钟", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = uiState.workMinutes.toFloat(),
                    onValueChange = { viewModel.setWorkMinutes(it.toInt()) },
                    valueRange = 5f..120f, steps = 22,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("完成后自动休息", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(checked = !uiState.skipBreak, onCheckedChange = { viewModel.setSkipBreak(!it) })
                }
                if (!uiState.skipBreak) {
                    Text("休息时长: ${uiState.breakMinutes}分钟", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = uiState.breakMinutes.toFloat(),
                        onValueChange = { viewModel.setBreakMinutes(it.toInt()) },
                        valueRange = 5f..60f, steps = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()

            }

            Spacer(Modifier.height(24.dp))

            // Control buttons
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isActive) {
                    Button(
                        onClick = { viewModel.start() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("开始")
                    }
                } else {
                    if (isRunning) {
                        Button(
                            onClick = { viewModel.pause() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Pause, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("暂停")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.resume() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("继续")
                        }
                    }
                    Button(
                        onClick = { viewModel.preStop() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Stop, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("停止")
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Stats
            val totalFocusMins = pomodoroState.totalFocusSecs / 60
            val focusHours = totalFocusMins / 60
            val focusMins = totalFocusMins % 60
            val focusText = if (focusHours > 0) "${focusHours}小时${focusMins}分钟" else "${focusMins}分钟"
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Text("今日专注: $focusText", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("今日完成: ${pomodoroState.completedCount}个", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
