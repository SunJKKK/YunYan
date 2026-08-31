package com.sunjk.sunjktool.feature.review

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
fun ReviewHistoryScreen(
    viewModel: ReviewHistoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFmt = remember { java.text.SimpleDateFormat("yyyy年M月d日", java.util.Locale.getDefault()) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("历史复盘") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.days.isEmpty() && !uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("暂无历史复盘记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(end = 16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(uiState.days) { dayIdx, day ->
                    val completedCount = day.items.count { it.isCompleted }
                    val totalCount = day.items.size
                    val typeLabel = day.items.firstOrNull()?.reviewType?.let { type ->
                        when (type) {
                            "weekly" -> "周复盘"
                            "monthly" -> "月复盘"
                            else -> ""
                        }
                    } ?: ""

                    Column(Modifier.fillMaxWidth().padding(start = 16.dp)) {
                        // Date header
                        val dotColor = MaterialTheme.colorScheme.outline
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
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                " $completedCount/$totalCount 已完成",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // Items — always visible
                        day.items.forEach { item ->
                            Card(
                                onClick = { onNavigateToDetail(item.logEntryId) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (item.isCompleted)
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    else
                                        MaterialTheme.colorScheme.surfaceContainer
                                )
                            ) {
                                Row(
                                    Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (item.isCompleted) Icons.Default.CheckCircle
                                        else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = if (item.isCompleted) "已完成" else "未完成",
                                        modifier = Modifier.size(24.dp),
                                        tint = if (item.isCompleted) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        if (item.subject.isNotBlank())
                                            Text(
                                                item.subject,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        Text(
                                            item.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (item.isCompleted)
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        if (dayIdx != uiState.days.lastIndex) Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}
