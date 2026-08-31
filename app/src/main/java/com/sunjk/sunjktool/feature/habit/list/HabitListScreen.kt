package com.sunjk.sunjktool.feature.habit.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitListScreen(
    viewModel: HabitListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<HabitWithStatus?>(null) }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除习惯") },
            text = { Text("确定要删除「${deleteTarget?.habit?.name}」吗？打卡记录将一并删除。") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget?.let { viewModel.deleteHabit(it.habit.id) }
                    deleteTarget = null
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("习惯") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToEdit(null) }) {
                Icon(Icons.Default.Add, contentDescription = "新建习惯")
            }
        }
    ) { padding ->
        if (uiState.items.isEmpty() && !uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("还没有习惯，点击右下角 + 创建", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.items, key = { it.habit.id }) { item ->
                    HabitCard(
                        item = item,
                        onToggle = { viewModel.toggleHabit(item.habit.id) },
                        onEdit = { onNavigateToEdit(item.habit.id) },
                        onDelete = { deleteTarget = item }
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitCard(
    item: HabitWithStatus,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val habitColor = Color(item.habit.colorArgb)

    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
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
            // Check/uncheck button
            Icon(
                if (item.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (item.isCompleted) "取消打卡" else "打卡",
                modifier = Modifier.size(28.dp).clickable { onToggle() },
                tint = if (item.isCompleted) habitColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.width(12.dp))
            // Color dot
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(habitColor)
            )
            Spacer(Modifier.width(10.dp))
            // Name + description
            Column(Modifier.weight(1f)) {
                Text(
                    item.habit.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (item.isCompleted)
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                if (item.habit.description.isNotBlank()) {
                    Text(
                        item.habit.description,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}
