package com.sunjk.sunjktool.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Circle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.sunjk.sunjktool.domain.model.LogEntry
import com.sunjk.sunjktool.util.formatDateTime
import java.io.File
import java.time.LocalDateTime

@Composable
fun LogEntryCard(
    entry: LogEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Subject tag
            if (entry.subject.isNotBlank()) {
                Text(
                    text = entry.subject,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraSmall)
                        .then(
                            Modifier.fillMaxWidth() // to maintain column width
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Image thumbnail (first image only)
            val firstPath = entry.imagePaths.firstOrNull()
            val imageFile = remember(firstPath) {
                firstPath?.let { File(it) }?.takeIf { it.exists() }
            }
            if (imageFile != null) {
                AsyncImage(
                    model = imageFile,
                    contentDescription = "图片",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Title
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom row: time + date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (entry.timeSpent > 0) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatTimeSpent(entry.timeSpent),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = formatDateTime(entry.createdDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun formatTimeSpent(minutes: Int): String {
    return when {
        minutes < 60 -> "${minutes}分钟"
        else -> {
            val hours = minutes / 60
            val mins = minutes % 60
            if (mins == 0) "${hours}小时" else "${hours}小时${mins}分钟"
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LogEntryCardPreview() {
    com.sunjk.sunjktool.ui.theme.SunJKToolTheme {
        val sample = LogEntry(
            id = 1,
            subject = "Kotlin",
            title = "协程与 Flow 学习笔记",
            timeSpent = 90,
            imagePaths = emptyList(),
            createdDate = LocalDateTime.now(),
            updatedDate = LocalDateTime.now()
        )
        LogEntryCard(entry = sample, onClick = {})
    }
}
