package com.sunjk.sunjktool.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunjk.sunjktool.di.AiGenerationManager
import com.sunjk.sunjktool.di.AiTaskStatus
import com.sunjk.sunjktool.di.GenerationTask

@Composable
fun HomeGenerationProgressBar(
    onTaskClick: (GenerationTask) -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by AiGenerationManager.tasks.collectAsStateWithLifecycle()
    val visible = tasks.filter { it.status == AiTaskStatus.RUNNING || it.status == AiTaskStatus.SUCCESS }

    AnimatedVisibility(
        visible = visible.isNotEmpty(),
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            visible.forEach { task ->
                GenerationTaskCard(task = task, onClick = { onTaskClick(task) })
            }
        }
    }
}

@Composable
private fun GenerationTaskCard(task: GenerationTask, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (task.status) {
                AiTaskStatus.RUNNING -> MaterialTheme.colorScheme.surfaceContainerLow
                AiTaskStatus.SUCCESS -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                AiTaskStatus.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            }
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (task.status == AiTaskStatus.RUNNING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = if (task.status == AiTaskStatus.SUCCESS) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (task.status == AiTaskStatus.SUCCESS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = MaterialTheme.typography.bodyMedium.fontWeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when (task.status) {
                        AiTaskStatus.RUNNING -> task.phase.ifBlank { "生成中…" }
                        AiTaskStatus.SUCCESS -> "已完成，点击查看"
                        AiTaskStatus.ERROR -> task.error ?: "生成失败"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when (task.status) {
                        AiTaskStatus.ERROR -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

            }
        }
    }
}