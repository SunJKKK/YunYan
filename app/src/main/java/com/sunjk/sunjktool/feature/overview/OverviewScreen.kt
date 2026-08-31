package com.sunjk.sunjktool.feature.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunjk.sunjktool.data.model.ReviewStatusEntity
import com.sunjk.sunjktool.domain.model.Habit
import com.sunjk.sunjktool.domain.model.LifeLogEntry
import com.sunjk.sunjktool.domain.model.LogEntry
import com.sunjk.sunjktool.feature.lifelog.MoodConfig
import com.sunjk.sunjktool.ui.components.OverviewCalendar
import com.sunjk.sunjktool.util.formatDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    viewModel: OverviewViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLogDetail: (Long) -> Unit,
    onNavigateToReviewList: () -> Unit,
    onNavigateToLifeLogDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFmt = remember { DateTimeFormatter.ofPattern("yyyy年M月d日") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("概览") },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
        ) {
            // Calendar
            item {
                Card(
                    Modifier.fillMaxWidth().padding(12.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    OverviewCalendar(
                        selectedDate = uiState.selectedDate,
                        markedDates = uiState.markedDates,
                        onDateSelected = viewModel::selectDate,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Selected date header
            item {
                Text(
                    "${uiState.selectedDate.format(dateFmt)} 概览",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Stats summary
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatChip(Icons.AutoMirrored.Filled.MenuBook, "${uiState.logEntryCount}条学习记录", Modifier.weight(1f))
                    val mins = uiState.focusSecs / 60
                    StatChip(Icons.Default.Timer, if (mins > 0) "专注${mins}分钟" else "暂无专注", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }

            // TickTick todos for the day
            if (uiState.todoTasks.isNotEmpty()) {
                item {
                    SectionHeader("待办任务 (${uiState.todoTasks.count { !it.isCompleted }}项待完成)")
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            uiState.todoTasks.forEach { task ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        null, Modifier.size(18.dp),
                                        tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    Spacer(Modifier.width(8.dp))
                                    Text(task.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Habits
            if (uiState.habits.isNotEmpty()) {
                item {
                    SectionHeader("习惯打卡")
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            uiState.habits.forEach { hws ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { viewModel.toggleHabit(hws.habit.id) }.padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(if (hws.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        null, Modifier.size(20.dp),
                                        tint = if (hws.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    Spacer(Modifier.width(8.dp))
                                    Box(Modifier.size(12.dp).clip(CircleShape).background(ComposeColor(hws.habit.colorArgb)))
                                    Spacer(Modifier.width(8.dp))
                                    Text(hws.habit.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Learning records
            if (uiState.logEntries.isNotEmpty()) {
                item {
                    SectionHeader("学习记录 (${uiState.logEntryCount})", onClick = { /* navigate to list */ })
                }
                items(uiState.logEntries.take(5).size) { idx ->
                    val entry = uiState.logEntries[idx]
                    Card(
                        onClick = { onNavigateToLogDetail(entry.id) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (entry.subject.isNotBlank()) {
                                    Text(entry.subject, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(entry.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                if (entry.timeSpent > 0) Text("${entry.timeSpent}min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(formatDate(entry.createdDate.toLocalDate()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                    }
                }
            }

            // Review tasks
            if (uiState.reviewTasks.isNotEmpty()) {
                item {
                    SectionHeader("复盘任务 (${uiState.reviewTasks.size})", onClick = onNavigateToReviewList)
                }
                item {
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            uiState.reviewTasks.forEach { task ->
                                val typeLabel = when (task.reviewType) {
                                    "daily" -> "每日复盘"
                                    "weekly" -> "每周复盘"
                                    "monthly" -> "每月复盘"
                                    else -> "复盘任务"
                                }
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        null, Modifier.size(18.dp),
                                        tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        val label = buildString {
                                            if (task.subject.isNotBlank()) append(task.subject).append(" ")
                                            append(task.title)
                                        }
                                        Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface)
                                        Text(typeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Life logs
            if (uiState.lifeLogs.isNotEmpty()) {
                item {
                    SectionHeader("生活记录 (${uiState.lifeLogs.size})")
                }
                items(uiState.lifeLogs.take(5).size) { idx ->
                    val entry = uiState.lifeLogs[idx]
                    Card(
                        onClick = { onNavigateToLifeLogDetail(entry.id) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            if (entry.moods.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    entry.moods.forEach { moodKey ->
                                        Text(MoodConfig.moodMap[moodKey] ?: moodKey, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                            Text(entry.content, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // Empty
            if (uiState.logEntries.isEmpty() && uiState.reviewTasks.isEmpty() && uiState.lifeLogs.isEmpty() && uiState.todoTasks.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("当天暂无记录", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = onClick != null) { onClick?.invoke() }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        if (onClick != null) Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}
