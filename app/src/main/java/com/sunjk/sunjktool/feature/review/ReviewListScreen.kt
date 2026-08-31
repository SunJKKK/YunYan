package com.sunjk.sunjktool.feature.review

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewListScreen(
    viewModel: ReviewListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFmt = remember { java.text.SimpleDateFormat("yyyy年M月d日", java.util.Locale.getDefault()) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("复盘") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "历史复盘")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.days.isEmpty() && !uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("暂无复盘任务", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(end = 16.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                itemsIndexed(uiState.days) { dayIdx, day ->
                    val pendingCount = day.items.count { !it.isCompleted }
                    val typeLabel = day.items.firstOrNull()?.reviewType?.let { type ->
                        when(type) { "weekly" -> "周复盘" ; "monthly" -> "月复盘" ; else -> "" }
                    } ?: ""

                    Column(Modifier.fillMaxWidth().padding(start = 16.dp)) {
                        // Date header
                        val dotColor = MaterialTheme.colorScheme.primary
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Canvas(modifier = Modifier.size(8.dp)) {
                                drawCircle(
                                    color = dotColor,
                                    radius = 4.dp.toPx(),
                                    center = center
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${dateFmt.format(java.util.Date(day.date))} $typeLabel",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                " ${pendingCount}项待复盘",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // Items — always visible (today+future only)
                        day.items.forEach { item ->
                            ReviewItemCard(
                                item = item,
                                onToggle = { viewModel.toggleReview(item.statusId, !item.isCompleted) },
                                onNavigate = { onNavigateToDetail(item.logEntryId) }
                            )
                        }

                        if (dayIdx != uiState.days.lastIndex) Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewItemCard(
    item: ReviewItem,
    onToggle: () -> Unit,
    onNavigate: () -> Unit
) {
    Card(
        onClick = onNavigate,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = if (item.isCompleted) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (item.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (item.isCompleted) "已完成" else "标记完成",
                modifier = Modifier.size(24.dp).clickable { onToggle() },
                tint = if (item.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (item.subject.isNotBlank()) Text(item.subject, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(item.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
