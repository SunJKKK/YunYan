package com.sunjk.sunjktool.feature.reviewnote.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sunjk.sunjktool.domain.model.ReviewNoteSource
import com.sunjk.sunjktool.ui.components.MarkdownRenderer
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewNoteDetailScreen(
    viewModel: ReviewNoteDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    if (uiState.deleteConfirmVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirm() },
            title = { Text("删除心得") },
            text = { Text("确定要删除这篇复盘心得吗？") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete() }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteConfirm() }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("心得详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        uiState.note?.id?.let { onNavigateToEdit(it) }
                    }) {
                        Icon(Icons.Default.Edit, "编辑")
                    }
                    IconButton(onClick = { viewModel.showDeleteConfirm() }) {
                        Icon(Icons.Default.Delete, "删除")
                    }
                }
            )
        }
    ) { innerPadding ->
        val note = uiState.note
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            note == null -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("心得不存在", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Header: source badge + time
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (badgeText, badgeColor) = when (note.sourceType) {
                            ReviewNoteSource.FLASHCARD -> "闪卡错题" to MaterialTheme.colorScheme.tertiary
                            ReviewNoteSource.MANUAL -> "手动记录" to MaterialTheme.colorScheme.primary
                        }
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = badgeColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            dateFmt.format(Date(note.createdDate.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Markdown content
                    MarkdownRenderer(note.content)

                    // Images
                    if (note.imagePaths.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))
                        Text("图片附件", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        note.imagePaths.forEach { path ->
                            val file = remember(path) { File(path) }
                            val ctx = androidx.compose.ui.platform.LocalContext.current
                            AsyncImage(
                                model = ImageRequest.Builder(ctx)
                                    .data(file)
                                    .size(2048, 8192)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentScale = ContentScale.FillWidth
                            )
                        }
                    }
                }
            }
        }
    }
}
