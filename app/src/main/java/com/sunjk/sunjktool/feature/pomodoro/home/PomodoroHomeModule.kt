package com.sunjk.sunjktool.feature.pomodoro.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sunjk.sunjktool.domain.model.PomodoroPhase
import com.sunjk.sunjktool.domain.model.PomodoroState

@Composable
fun PomodoroHomeModule(
    state: PomodoroState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onNavigateToDetail: () -> Unit,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false
) {
    val progress = if (state.totalSecs > 0)
        (state.totalSecs - state.remainingSecs).toFloat() / state.totalSecs else 0f
    val mins = state.remainingSecs / 60
    val isActive = state.isRunning || state.remainingSecs > 0
    val phaseText = when (state.phase) {
        PomodoroPhase.FOCUS -> "工作中"
        PomodoroPhase.BREAK -> "休息中"
        PomodoroPhase.IDLE -> "待开始"
    }
    val ringSize = if (isLarge) 88.dp else 56.dp
    val ringWidth = if (isLarge) 7.dp else 5.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNavigateToDetail() }
            .padding(16.dp)
    ) {
        // Ring + phase info
        val ringColor = MaterialTheme.colorScheme.primary
        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(ringSize)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sw = ringWidth.toPx()
                    val tl = Offset(sw / 2, sw / 2)
                    val arcSize = Size(size.width - sw, size.height - sw)
                    drawArc(trackColor, -90f, 360f, false, tl, arcSize, style = Stroke(sw, cap = StrokeCap.Round))
                    if (isActive) {
                        drawArc(
                            ringColor,
                            -90f, 360f * progress, false, tl, arcSize,
                            style = Stroke(sw, cap = StrokeCap.Round)
                        )
                    }
                }
                if (isActive) {
                    Text(
                        "${mins}m",
                        style = if (isLarge) MaterialTheme.typography.headlineSmall
                        else MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        "--",
                        style = if (isLarge) MaterialTheme.typography.headlineSmall
                        else MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(if (isLarge) 20.dp else 12.dp))
            Column {
                Text(
                    phaseText,
                    style = if (isLarge) MaterialTheme.typography.titleMedium
                    else MaterialTheme.typography.bodyMedium,

                )
                if (isActive) {
                    Text(
                        String.format("%02d:%02d", state.remainingSecs / 60, state.remainingSecs % 60),
                        style = if (isLarge) MaterialTheme.typography.headlineMedium
                        else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        "点击进入番茄钟",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (isActive) {
            Spacer(Modifier.height(if (isLarge) 14.dp else 8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = if (isLarge) Modifier.fillMaxWidth() else Modifier
            ) {
                FilledTonalButton(
                    onClick = if (state.isRunning) onPause else onResume,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = if (isLarge) Modifier.weight(1f) else Modifier
                ) {
                    Icon(
                        if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isRunning) "暂停" else "继续",
                        modifier = Modifier.size(18.dp)
                    )
                    if (isLarge) {
                        Spacer(Modifier.width(4.dp))
                        Text(if (state.isRunning) "暂停" else "继续")
                    }
                }
                FilledTonalButton(
                    onClick = onStop,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = if (isLarge) Modifier.weight(1f) else Modifier
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "停止", modifier = Modifier.size(18.dp))
                    if (isLarge) {
                        Spacer(Modifier.width(4.dp))
                        Text("停止")
                    }
                }
            }
        }

        // Bottom stats: large cards show a full stat strip, small cards show one line
        val focusMins = state.totalFocusSecs / 60
        val focusH = focusMins / 60
        val focusRemM = focusMins % 60
        val focusText = if (focusH > 0) "${focusH}小时${focusRemM}分钟" else "${focusRemM}分钟"
        if (isLarge) {
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PomodoroStatCell(
                    label = "今日专注",
                    value = focusText,
                    modifier = Modifier.weight(1f)
                )
                PomodoroStatCell(
                    label = "完成番茄",
                    value = "${state.completedCount} 个",
                    modifier = Modifier.weight(1f)
                )
                PomodoroStatCell(
                    label = "状态",
                    value = phaseText,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "今日专注: $focusText",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PomodoroStatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
