package com.sunjk.sunjktool.feature.lifelog.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sunjk.sunjktool.feature.lifelog.MoodConfig
import com.sunjk.sunjktool.ui.components.ConfirmDialog
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeLogDetailScreen(
    viewModel: LifeLogDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var fullscreenImageIndex by remember { mutableStateOf<Int?>(null) }

    // Fullscreen image gallery dialog
    fullscreenImageIndex?.let { idx ->
        val entry = uiState.entry
        val files = remember(entry?.imagePaths) { entry?.imagePaths?.map { File(it) } ?: emptyList() }
        if (files.isNotEmpty()) {
            val pagerState = rememberPagerState(initialPage = idx.coerceIn(0, files.size - 1), pageCount = { files.size })
            var scale by remember { mutableFloatStateOf(1f) }
            var offsetX by remember { mutableFloatStateOf(0f) }
            var offsetY by remember { mutableFloatStateOf(0f) }
            LaunchedEffect(pagerState.currentPage) { scale = 1f; offsetX = 0f; offsetY = 0f }
            Dialog(onDismissRequest = { fullscreenImageIndex = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    HorizontalPager(state = pagerState, userScrollEnabled = scale <= 1f, modifier = Modifier.fillMaxSize()) { page ->
                        Box(Modifier.fillMaxSize().pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = { scale = if (scale > 1f) 1f else 3f; if (scale == 1f) { offsetX = 0f; offsetY = 0f } },
                                onTap = { fullscreenImageIndex = null })
                        }.pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale > 1f) { offsetX += pan.x; offsetY += pan.y }
                                else { offsetX = 0f; offsetY = 0f }
                            }
                        }, contentAlignment = Alignment.Center) {
                            AsyncImage(model = files[page], contentDescription = null,
                                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale; translationX = offsetX; translationY = offsetY },
                                contentScale = ContentScale.Fit)
                        }
                    }
                    if (files.size > 1) {
                        Text("${pagerState.currentPage + 1} / ${files.size}", color = Color.White,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                            style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { fullscreenImageIndex = null }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "关闭", tint = Color.White)
                    }
                }
            }
        }
    }

    if (uiState.deleteConfirmId != null) {
        ConfirmDialog(
            title = "删除记录？",
            message = "删除后不可恢复，确定要删除这条生活记录吗？",
            confirmText = "删除",
            onConfirm = {
                viewModel.confirmDelete()
                onNavigateBack()
            },
            onDismiss = viewModel::dismissDelete
        )
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToEdit) {
                        Icon(Icons.Default.Edit, "编辑")
                    }
                    IconButton(onClick = viewModel::requestDelete) {
                        Icon(Icons.Default.Delete, "删除")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val entry = uiState.entry ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Mood row
            if (entry.moods.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    entry.moods.forEach { moodKey ->
                        val mood = MoodConfig.allMoods.find { it.key == moodKey }
                        if (mood != null) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(mood.icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(4.dp))
                                    Text(mood.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Content (Markdown)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Box(Modifier.padding(16.dp)) {
                    com.sunjk.sunjktool.ui.components.MarkdownRenderer(text = entry.content)
                }
            }

            // Images
            if (entry.imagePaths.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("图片", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                entry.imagePaths.forEachIndexed { index, path ->
                    val file = remember(path) { File(path) }
                    if (index > 0) Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { fullscreenImageIndex = index },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        AsyncImage(
                            model = file,
                            contentDescription = "图片 ${index + 1}",
                            modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }
        }
    }
}
