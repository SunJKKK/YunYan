package com.sunjk.sunjktool.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.size(width = 240.dp, height = 40.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingIndicatorPreview() {
    com.sunjk.sunjktool.ui.theme.SunJKToolTheme {
        LoadingIndicator()
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    com.sunjk.sunjktool.ui.theme.SunJKToolTheme {
        EmptyState(
            title = "还没有学习记录",
            subtitle = "点击右下角 + 按钮，开始记录你的学习吧"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmDialogPreview() {
    com.sunjk.sunjktool.ui.theme.SunJKToolTheme {
        ConfirmDialog(
            title = "确认删除",
            message = "删除后无法恢复，确定要删除这条学习记录吗？",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "确认",
    dismissText: String = "取消",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}
