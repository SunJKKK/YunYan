package com.sunjk.sunjktool.feature.learninglog.detail

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.util.Log
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import com.sunjk.sunjktool.ui.components.MarkdownRenderer
import com.sunjk.sunjktool.ui.components.MarkdownRenderBlock
import com.sunjk.sunjktool.ui.components.splitMarkdownBlocks
import com.sunjk.sunjktool.ui.components.SelfCheckRenderer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import coil.request.ImageRequest
import com.sunjk.sunjktool.domain.model.LogEntry
import com.sunjk.sunjktool.feature.learninglog.edit.CropScreen
import com.sunjk.sunjktool.ui.components.ConfirmDialog
import com.sunjk.sunjktool.data.local.ApiPreferences
import com.sunjk.sunjktool.ui.components.EmptyState
import com.sunjk.sunjktool.ui.components.LoadingIndicator
import com.sunjk.sunjktool.util.formatDateTime
import com.sunjk.sunjktool.util.MarkdownOutlineParser
import com.sunjk.sunjktool.util.MarkdownSection
import com.sunjk.sunjktool.util.OutlineItem
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDetailScreen(
    viewModel: LogDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    onNavigateToFlashcards: () -> Unit = {},
    onNavigateToFlashcardHub: () -> Unit = {},
    onNavigateToReviewNotes: () -> Unit = {},
    onNavigateToReviewNoteDetail: (Long) -> Unit = {},
    initialHeading: String? = null,
    onNavigateToQuestionLinks: (logId: Long, headingId: String) -> Unit = { _, _ -> },
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    // When no existing flashcards, generate directly
    LaunchedEffect(uiState.generatedSessionId) {
        if (uiState.generatedSessionId != null) {
            viewModel.clearFlashcardNavigation()
            onNavigateToFlashcards()
        }
    }

    LaunchedEffect(uiState.navigateToHub) {
        if (uiState.navigateToHub) {
            viewModel.clearHubNavigation()
            onNavigateToFlashcardHub()
        }
    }

    LaunchedEffect(uiState.navigateToReviewNotes) {
        if (uiState.navigateToReviewNotes) {
            viewModel.clearReviewNoteNavigation()
            onNavigateToReviewNotes()
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "确认删除",
            message = "删除后无法恢复，确定要删除这条学习记录吗？",
            confirmText = "删除",
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteEntry()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    val titleSharedModifier = sharedTransitionScope?.let { s ->
        with(s) {
            animatedVisibilityScope?.let { scope ->
                uiState.entry?.let { entry ->
                    Modifier.sharedBounds(rememberSharedContentState("log_card_${entry.id}"), scope)
                } ?: Modifier
            } ?: Modifier
        }
    } ?: Modifier

    val summaryText = uiState.summaryText.ifBlank { uiState.entry?.aiSummary ?: "" }
    val isTabletMode = uiState.isTabletMode
    val tabTitles = listOf("AI总结", "描述", "心得", "自检")

    var selectedTab by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabTitles.size })
    val scope = rememberCoroutineScope()

    // Sync pager → tab (covers user swipe & programmatic scroll)
    LaunchedEffect(Unit) {
        snapshotFlow { pagerState.currentPage }.collect { page -> selectedTab = page }
    }
    // Init tab once entry loads: summary tab if has summary/generating, else description tab
    LaunchedEffect(uiState.entry?.id) {
        val entry = uiState.entry ?: return@LaunchedEffect
        val target = if (uiState.isGeneratingSummary || !entry.aiSummary.isNullOrBlank()) 0 else 1
        if (pagerState.currentPage != target) pagerState.scrollToPage(target)
    }

    val selectTab: (Int) -> Unit = { index ->
        selectedTab = index
        scope.launch { pagerState.animateScrollToPage(index) }
    }
    val noteCountFor: (Int) -> Int = { index -> if (index == 2) uiState.reviewNotes.size else 0 }

    // 大纲控制按钮仅在平板端、AI总结页、且有内容且非编辑/生成时展示
    val showOutlineControls = isTabletMode && selectedTab == 0 &&
        !uiState.isEditingSummary && !uiState.isGeneratingSummary && summaryText.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = titleSharedModifier,
                title = {
                    Text(
                        text = uiState.entry?.title ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                windowInsets = WindowInsets(0.dp),
                actions = {
                    if (isTabletMode) {
                        // 平板端：胶囊菜单（标签页）放到顶部栏
                        SummaryTabBar(
                            tabTitles = tabTitles,
                            selectedTab = selectedTab,
                            onSelect = selectTab,
                            noteCount = noteCountFor,
                            modifier = Modifier.widthIn(max = 360.dp),
                            horizontalPadding = 4.dp
                        )
                        if (showOutlineControls) {
                            // 大纲位置（左/右）切换，放在原有图标左侧
                            IconButton(onClick = viewModel::toggleSummaryOutlinePosition) {
                                Icon(
                                    if (uiState.summaryOutlineOnLeft) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = if (uiState.summaryOutlineOnLeft) "大纲移到右侧" else "大纲移到左侧",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            // 大纲收起/展开
                            IconButton(onClick = viewModel::toggleSummaryOutline) {
                                Icon(
                                    if (uiState.summaryOutlineExpanded) Icons.Default.MenuOpen else Icons.Default.Menu,
                                    contentDescription = if (uiState.summaryOutlineExpanded) "收起大纲" else "展开大纲",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    if (uiState.entry != null) {
                        IconButton(
                            onClick = { viewModel.onFlashcardButtonClick() },
                            enabled = !uiState.isGeneratingSummary && !uiState.isGeneratingFlashcards
                        ) {
                            Icon(Icons.Default.Style, contentDescription = "AI 闪卡")
                        }
                    }
                    IconButton(onClick = onNavigateToEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingIndicator()
            uiState.entry == null -> EmptyState(
                title = "记录不存在",
                subtitle = "该记录可能已被删除"
            )
            else -> {
                val entry = uiState.entry!!
                var fullscreenImageIndex by remember { mutableStateOf<Int?>(null) }
                Column(modifier = modifier.fillMaxSize().padding(innerPadding)) {

                    // 非平板端：胶囊菜单（标签页）显示在内容区顶部
                    if (!isTabletMode) {
                        SummaryTabBar(
                            tabTitles = tabTitles,
                            selectedTab = selectedTab,
                            onSelect = selectTab,
                            noteCount = noteCountFor
                        )
                    }
                    // Pager
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        when (page) {
                            0 -> SummaryTab(uiState, viewModel, initialHeading, onNavigateToQuestionLinks)
                            1 -> DescriptionTab(uiState, entry, { fullscreenImageIndex = it }, viewModel)
                            2 -> ReviewNotesTab(viewModel, onNavigateToReviewNotes, onNavigateToReviewNoteDetail, entry)
                            3 -> SelfCheckTab(uiState, viewModel)
                        }
                    }
                }
                // Fullscreen gallery
                fullscreenImageIndex?.let { idx ->
                    val files = remember(entry.imagePaths) {
                        entry.imagePaths.map { File(it) }
                    }
                    if (files.isNotEmpty()) {
                        val context = LocalContext.current
                        FullscreenImageGallery(
                            files = files,
                            initialIndex = idx.coerceIn(0, files.lastIndex),
                            onDismiss = { fullscreenImageIndex = null },
                            onSave = { file -> saveToGallery(context, file) },
                            onCrop = { old, new -> viewModel.replaceImagePath(old, new) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryTabBar(
    tabTitles: List<String>,
    selectedTab: Int,
    onSelect: (Int) -> Unit,
    noteCount: (Int) -> Int,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 12.dp
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .animateContentSize()
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabTitles.forEachIndexed { index, title ->
            val count = noteCount(index)
            FilterChip(
                selected = selectedTab == index,
                onClick = { onSelect(index) },
                label = {
                    Text(
                        if (count > 0) "$title ($count)" else title,
                        maxLines = 1
                    )
                }
            )
        }
    }
}

@Composable
private fun FullscreenImageGallery(
    files: List<File>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onSave: (File) -> Unit,
    onCrop: ((String, String) -> Unit)? = null
) {
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { files.size })
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var cropFile by remember { mutableStateOf<String?>(null) }

    // Reset zoom when page changes
    LaunchedEffect(pagerState.currentPage) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    cropFile?.let { path ->
        CropScreen(
            filePath = path,
            onDismiss = { cropFile = null },
            onCropped = { croppedPath ->
                cropFile = null
                val originalPath = files[pagerState.currentPage].absolutePath
                onCrop?.invoke(originalPath, croppedPath)
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = scale <= 1f,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val file = files[page]

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = file,
                        contentDescription = "图片 ${page + 1}",
                        modifier = Modifier
                            .fillMaxSize()
                            // Tap: dismiss (single) / toggle zoom (double)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { if (scale <= 1f) onDismiss() },
                                    onDoubleTap = {
                                        if (scale > 1f) {
                                            scale = 1f; offsetX = 0f; offsetY = 0f
                                        } else {
                                            scale = 3f
                                        }
                                    }
                                )
                            }
                            // Pinch zoom — only multi-touch; single-finger events pass through to pager
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    var initialScale = scale
                                    var initialOffsetX = offsetX
                                    var initialOffsetY = offsetY
                                    var centroid0 = Offset.Zero
                                    var spacing0 = 0f
                                    var isActive = false

                                    do {
                                        val event = awaitPointerEvent()
                                        val pointers = event.changes.filter { it.pressed }

                                        when {
                                            pointers.size >= 2 -> {
                                                val p1 = pointers[0].position
                                                val p2 = pointers[1].position
                                                val spacing = (p1 - p2).getDistance()
                                                val centroid = (p1 + p2) / 2f
                                                if (!isActive) {
                                                    isActive = true
                                                    initialScale = scale
                                                    initialOffsetX = offsetX
                                                    initialOffsetY = offsetY
                                                    centroid0 = centroid
                                                    spacing0 = spacing
                                                }
                                                pointers.forEach { it.consume() }
                                                val zoom = if (spacing0 > 0f) spacing / spacing0 else 1f
                                                val newScale = (initialScale * zoom).coerceIn(1f, 5f)
                                                val pan = centroid - centroid0
                                                scale = newScale
                                                if (newScale > 1f) {
                                                    offsetX = initialOffsetX + pan.x
                                                    offsetY = initialOffsetY + pan.y
                                                } else {
                                                    offsetX = 0f; offsetY = 0f
                                                }
                                            }
                                            pointers.size == 1 && scale > 1f -> {
                                                // Single-finger pan while zoomed: consume so pager doesn't steal
                                                val change = pointers.first()
                                                if (event.type == PointerEventType.Move) {
                                                    change.consume()
                                                    offsetX += change.position.x - change.previousPosition.x
                                                    offsetY += change.position.y - change.previousPosition.y
                                                }
                                            }
                                            // Single finger & not zoomed: don't consume -> pager handles swipe
                                        }
                                    } while (pointers.isNotEmpty())
                                }
                            }
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            },
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Close button — top right
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 8.dp)
            ) {
                Icon(Icons.Default.Close, "关闭", tint = Color.White)
            }

            // Save + Crop buttons — bottom
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (onCrop != null) {
                    IconButton(onClick = { cropFile = files[pagerState.currentPage].absolutePath }) {
                        Icon(Icons.Default.Crop, "裁剪", tint = Color.White)
                    }
                }
                IconButton(onClick = { onSave(files[pagerState.currentPage]) }) {
                    Icon(Icons.Default.SaveAlt, "保存至相册", tint = Color.White)
                }
            }

            // Page indicator — bottom
            if (files.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${files.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier

                        .padding(bottom = 16.dp)
                )
            }
        }
    }
}

private fun saveToGallery(context: Context, file: File) {
    try {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "SunJKTool_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SunJKTool")
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        )
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out ->
                file.inputStream().use { inp -> inp.copyTo(out) }
            }
            Toast.makeText(context, "已保存至相册", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun SummaryTab(
    uiState: LogDetailUiState,
    viewModel: LogDetailViewModel,
    initialHeading: String? = null,
    onNavigateToQuestionLinks: (logId: Long, headingId: String) -> Unit = { _, _ -> }
) {
    val summary = uiState.summaryText.ifBlank { uiState.entry?.aiSummary ?: "" }
    val context = LocalContext.current
    var showOverwriteDialog by remember { mutableStateOf(false) }
    var showRegenerateConfirmDialog by remember { mutableStateOf(false) }

    // ── 渲染策略诊断日志：进入 AI 总结 Tab 时打印一次判定依据 ──
    LaunchedEffect(Unit) {
        val predicted = when {
            uiState.isTabletMode && !uiState.isEditingSummary && !uiState.isGeneratingSummary && summary.isNotBlank() ->
                "平板-双栏 SummaryDualPane"
            !uiState.isTabletMode && !uiState.isEditingSummary && !uiState.isGeneratingSummary && summary.isNotBlank() && initialHeading != null ->
                "手机-分章节定位 MobileSectionedSummary"
            !uiState.isTabletMode && !uiState.isEditingSummary && !uiState.isGeneratingSummary && summary.isNotBlank() ->
                "手机-全量渲染 EagerMobileSummary"
            uiState.isGeneratingSummary -> "生成中态(phase=${uiState.summaryGenerationPhase})"
            else -> "空态/编辑态/其他(editing=${uiState.isEditingSummary})"
        }
        Log.d(
            "AI总结渲染",
            "进入SummaryTab | predicted=$predicted | tablet=${uiState.isTabletMode} editing=${uiState.isEditingSummary} " +
                "generating=${uiState.isGeneratingSummary} summaryLen=${summary.length} hasSummary=${summary.isNotBlank()} " +
                "heading=${initialHeading ?: "null"} phase=${uiState.summaryGenerationPhase}"
        )
    }

    // 引用失效确认：重新生成会破坏题集解析 → 本章节的跳转链接
    if (showRegenerateConfirmDialog) {
        val refCount = uiState.referenceCounts.values.sum()
        ConfirmDialog(
            title = "重新生成 AI 总结",
            message = "当前总结被 $refCount 道题目的解析所引用。重新生成后，这些解析中的跳转链接将失效（降级跳转到总结顶部）。确定继续吗？",
            confirmText = "继续生成",
            onConfirm = {
                showRegenerateConfirmDialog = false
                viewModel.showSummaryModeDialog()
            },
            onDismiss = { showRegenerateConfirmDialog = false }
        )
    }

    // Overwrite confirmation dialog
    if (showOverwriteDialog) {
        ConfirmDialog(
            title = "覆盖 AI 总结",
            message = "已有 AI 总结，生成新的总结将覆盖现有内容。确定继续吗？",
            confirmText = "覆盖",
            onConfirm = { showOverwriteDialog = false; viewModel.showSummaryModeDialog() },
            onDismiss = { showOverwriteDialog = false }
        )
    }

    // Mode selection dialog — unified with model selection
    if (uiState.showSummaryModeDialog) {
        val isMultiAgent = uiState.summaryGenerationMode == "multi_agent"
        AlertDialog(
            onDismissRequest = { viewModel.dismissSummaryModeDialog() },
            title = { Text("生成 AI 总结") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // Mode selection — SegmentedButton style
                    Text("生成模式", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("standard", "标准", "直接生成，适合常规内容"),
                            Triple("rag", "检索增强", "先补知识再总结"),
                            Triple("multi_agent", "多Agent", "分块→总结→整合")
                        ).forEach { (mode, label, desc) ->
                            FilterChip(
                                selected = uiState.summaryGenerationMode == mode,
                                onClick = { viewModel.setSummaryGenerationMode(mode) },
                                modifier = Modifier.weight(1f),
                                label = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(label, style = MaterialTheme.typography.labelMedium)
                                        Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            )
                        }
                    }

                    // Chunk strategy — only for multi-agent
                    if (isMultiAgent) {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Text("分块策略", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple("chapter", "按章", "遵循原文章节大块切分"),
                                Triple("section", "按节", "按小节切分"),
                                Triple("auto", "自动", "AI自行判断粒度")
                            ).forEach { (strategy, label, desc) ->
                                FilterChip(
                                    selected = uiState.summaryChunkStrategy == strategy,
                                    onClick = { viewModel.setSummaryChunkStrategy(strategy) },
                                    modifier = Modifier.weight(1f),
                                    label = {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(label, style = MaterialTheme.typography.labelMedium)
                                            Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    // Model selection — SegmentedButton style
                    if (isMultiAgent) {
                        Text("预处理模型", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        ModelToggle(uiState.summaryPreprocessModel) { viewModel.setSummaryPreprocessModel(it) }
                        Spacer(Modifier.height(8.dp))
                        Text("总结模型", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        ModelToggle(uiState.summaryAgentModel) { viewModel.setSummaryAgentModel(it) }
                        Spacer(Modifier.height(8.dp))
                        Text("整合模型", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        ModelToggle(uiState.summaryIntegrateModel) { viewModel.setSummaryIntegrateModel(it) }
                    } else {
                        Text("总结模型", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        ModelToggle(uiState.summaryAgentModel) { viewModel.setSummaryAgentModel(it) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissSummaryModeDialog()
                    viewModel.generateSummary(context)
                }) { Text("生成") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissSummaryModeDialog() }) { Text("取消") }
            }
        )
    }

    // 平板端双栏（仅"有内容"且非编辑态）
    if (uiState.isTabletMode && !uiState.isEditingSummary && !uiState.isGeneratingSummary && summary.isNotBlank()) {
        SummaryDualPane(
            summary = summary,
            outlineExpanded = uiState.summaryOutlineExpanded,
            outlineOnLeft = uiState.summaryOutlineOnLeft,
            initialHeading = initialHeading,
            referenceCounts = uiState.referenceCounts,
            onReferenceClick = { headingId -> uiState.entry?.id?.let { onNavigateToQuestionLinks(it, headingId) } }
        )
        return
    }

    // 手机端：有内容且非编辑/生成时，按章节分块渲染（支持初始定位 + 章节被引用入口）
    if (!uiState.isTabletMode && !uiState.isEditingSummary && !uiState.isGeneratingSummary && summary.isNotBlank()) {
        if (!initialHeading.isNullOrBlank()) {
            MobileSectionedSummary(
                summary = summary,
                uiState = uiState,
                viewModel = viewModel,
                initialHeading = initialHeading,
                onNavigateToQuestionLinks = onNavigateToQuestionLinks
            )
        } else {
            EagerMobileSummary(
                summary = summary,
                uiState = uiState,
                viewModel = viewModel,
                onNavigateToQuestionLinks = onNavigateToQuestionLinks
            )
        }
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (uiState.isGeneratingSummary) {
            // State 1: generating
            Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(16.dp)) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    val isRAG = uiState.summaryRetrievalAugmented
                    val isMulti = uiState.summaryGenerationPhase in listOf("preprocess", "topic_summary", "integrate")
                    if (isMulti) {
                        // Multi-agent steps
                        val phasesAfter = listOf("topic_summary", "integrate")
                        val phasesAfter2 = listOf("integrate")
                        SummaryStepRow("图片文字识别", if (uiState.summaryGenerationPhase in phasesAfter) SummaryStepState.DONE else if (uiState.summaryGenerationPhase == "ocr") SummaryStepState.IN_PROGRESS else SummaryStepState.PENDING)
                        SummaryStepRow("预处理分块", if (uiState.summaryGenerationPhase in phasesAfter) SummaryStepState.DONE else if (uiState.summaryGenerationPhase == "preprocess") SummaryStepState.IN_PROGRESS else SummaryStepState.PENDING)
                        val topicLabel = if (uiState.summaryMultiAgentTotal > 0) "主题总结 ${uiState.summaryMultiAgentCurrent}/${uiState.summaryMultiAgentTotal}" else "主题总结"
                        SummaryStepRow(topicLabel, if (uiState.summaryGenerationPhase in phasesAfter2) SummaryStepState.DONE else if (uiState.summaryGenerationPhase == "topic_summary") SummaryStepState.IN_PROGRESS else SummaryStepState.PENDING)
                        SummaryStepRow("整合生成", if (uiState.summaryGenerationPhase == "idle" && !uiState.isGeneratingSummary) SummaryStepState.DONE else if (uiState.summaryGenerationPhase == "integrate") SummaryStepState.IN_PROGRESS else SummaryStepState.PENDING)
                    } else {
                        SummaryStepRow("图片文字识别", if (uiState.summaryGenerationPhase in listOf("gap","retrieval","summary")) SummaryStepState.DONE else if (uiState.summaryGenerationPhase == "ocr") SummaryStepState.IN_PROGRESS else SummaryStepState.PENDING)
                        if (isRAG) {
                            SummaryStepRow("知识缺口分析", if (uiState.summaryGenerationPhase in listOf("retrieval","summary")) SummaryStepState.DONE else if (uiState.summaryGenerationPhase == "gap") SummaryStepState.IN_PROGRESS else SummaryStepState.PENDING)
                            SummaryStepRow("补充知识检索", if (uiState.summaryGenerationPhase == "summary") SummaryStepState.DONE else if (uiState.summaryGenerationPhase == "retrieval") SummaryStepState.IN_PROGRESS else SummaryStepState.PENDING)
                        }
                        SummaryStepRow("总结生成", if (uiState.summaryGenerationPhase == "idle" && !uiState.isGeneratingSummary) SummaryStepState.DONE else if (uiState.summaryGenerationPhase == "summary") SummaryStepState.IN_PROGRESS else SummaryStepState.PENDING)
                    }
                }
            }
            // Stream output during summary/integrate phase
            val isStreaming = uiState.summaryGenerationPhase in listOf("summary", "integrate")
            if (isStreaming && summary.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("流式输出中…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                MarkdownRenderer(summary)
            }
        } else if (summary.isNotBlank()) {
            // State 2: has content
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("AI 总结", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (!uiState.isGeneratingSummary) {
                        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                        IconButton(onClick = { clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(summary)) }) { Icon(Icons.Default.ContentCopy, "复制", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = { viewModel.toggleEditSummary() }) { Icon(if (uiState.isEditingSummary) Icons.Default.Check else Icons.Default.Edit, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (uiState.isEditingSummary) TextField(uiState.summaryText, { viewModel.updateSummaryText(it) }, Modifier.fillMaxWidth(), minLines = 4)
                else MarkdownRenderer(summary)

                // Regenerate footer
                if (!uiState.isEditingSummary) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            val hasExisting = !uiState.entry?.aiSummary.isNullOrBlank()
                            if (hasExisting) {
                                val refCount = uiState.referenceCounts.values.sum()
                                if (refCount > 0) showRegenerateConfirmDialog = true
                                else showOverwriteDialog = true
                            } else viewModel.showSummaryModeDialog()
                        },
                        enabled = !uiState.isGeneratingFlashcards,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("重新生成 AI 总结")
                    }
                }
            }
        } else {
            // State 3: empty
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(8.dp))
                    Text("暂无 AI 总结", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("AI 将识别图片文字并结合描述，\n归纳知识点、考点与易错点", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.showSummaryModeDialog() },
                        enabled = !uiState.isGeneratingFlashcards
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("生成 AI 总结")
                    }
                }
            }
        }
    }
}

@Composable
private fun EagerMobileSummary(
    summary: String,
    uiState: LogDetailUiState,
    viewModel: LogDetailViewModel,
    onNavigateToQuestionLinks: (Long, String) -> Unit
) {
    val sections = remember(summary) { MarkdownOutlineParser.splitSections(summary) }
    val sectionBlocks = remember(sections) { sections.map { it to splitMarkdownBlocks(it.body) } }
    val scrollState = rememberScrollState()
    LaunchedEffect(Unit) {
        Log.d("AI总结渲染", "实际进入 -> 手机-全量渲染 EagerMobileSummary（sections=${sections.size}）")
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("AI 总结", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        sectionBlocks.forEach { (section, blocks) ->
            val heading = section.heading
            heading?.let {
                SectionHeading(it, uiState.referenceCounts[it.headingId] ?: 0) { headingId ->
                    uiState.entry?.id?.let { entryId -> onNavigateToQuestionLinks(entryId, headingId) }
                }
            }
            blocks.forEach { block -> MarkdownRenderer(block.content) }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { viewModel.showSummaryModeDialog() },
            enabled = !uiState.isGeneratingFlashcards,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("重新生成 AI 总结")
        }
    }
}

@Composable
private fun MobileSectionedSummary(
    summary: String,
    uiState: LogDetailUiState,
    viewModel: LogDetailViewModel,
    initialHeading: String?,
    onNavigateToQuestionLinks: (logId: Long, headingId: String) -> Unit
) {
    val sections = remember(summary) { MarkdownOutlineParser.splitSections(summary) }
    val sectionBlocks = remember(sections) { sections.map { it to splitMarkdownBlocks(it.body) } }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        Log.d("AI总结渲染", "实际进入 -> 手机-分章节定位 MobileSectionedSummary（heading=$initialHeading sections=${sections.size}）")
    }
    var showOverwriteDialog by remember { mutableStateOf(false) }
    var showRegenerateConfirmDialog by remember { mutableStateOf(false) }

    if (showOverwriteDialog) {
        ConfirmDialog(
            title = "覆盖 AI 总结",
            message = "已有 AI 总结，生成新的总结将覆盖现有内容。确定继续吗？",
            confirmText = "覆盖",
            onConfirm = { showOverwriteDialog = false; viewModel.showSummaryModeDialog() },
            onDismiss = { showOverwriteDialog = false }
        )
    }
    if (showRegenerateConfirmDialog) {
        val refCount = uiState.referenceCounts.values.sum()
        ConfirmDialog(
            title = "重新生成 AI 总结",
            message = "当前总结被 $refCount 道题目的解析所引用。重新生成后，这些解析中的跳转链接将失效（降级跳转到总结顶部）。确定继续吗？",
            confirmText = "继续生成",
            onConfirm = {
                showRegenerateConfirmDialog = false
                viewModel.showSummaryModeDialog()
            },
            onDismiss = { showRegenerateConfirmDialog = false }
        )
    }

    // 初始定位：从题集解析跳转过来时滚动到对应章节（+1：标题栏占第 0 项）
    LaunchedEffect(initialHeading, sections) {
        val target = initialHeading ?: return@LaunchedEffect
        var itemIndex = 1 // summary header
        for (section in sections) {
            if (section.heading?.headingId == target) {
                listState.scrollToItem(itemIndex)
                return@LaunchedEffect
            }
            itemIndex += (if (section.heading != null) 1 else 0) + splitMarkdownBlocks(section.body).size
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 标题栏（AI 总结 + 操作按钮）随正文内容一起滚动，不再固定顶部
        item(key = "summary_header") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("AI 总结", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (!uiState.isGeneratingSummary) {
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    IconButton(onClick = { clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(summary)) }) {
                        Icon(Icons.Default.ContentCopy, "复制", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = { viewModel.toggleEditSummary() }) {
                        Icon(Icons.Default.Edit, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        sectionBlocks.forEach { (section, blocks) ->
            val heading = section.heading
            val sectionKey = heading?.headingId ?: "preamble"
            if (heading != null) {
                item(key = "$sectionKey-heading") {
                    HighlightSection(highlighted = heading.headingId == initialHeading) {
                        val entryId = uiState.entry?.id ?: 0L
                        SectionHeading(
                            heading,
                            uiState.referenceCounts[heading.headingId] ?: 0,
                            { hid -> if (entryId > 0L) onNavigateToQuestionLinks(entryId, hid) }
                        )
                    }
                }
            }
            blocks.forEach { block ->
                item(key = "$sectionKey-${block.key}") {
                    HighlightSection(highlighted = heading?.headingId == initialHeading) {
                        MarkdownRenderer(block.content)
                    }
                }
            }
        }
        item(key = "regenerate") {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        val hasExisting = !uiState.entry?.aiSummary.isNullOrBlank()
                        if (hasExisting) {
                            val refCount = uiState.referenceCounts.values.sum()
                            if (refCount > 0) showRegenerateConfirmDialog = true
                            else showOverwriteDialog = true
                        } else viewModel.showSummaryModeDialog()
                    },
                    enabled = !uiState.isGeneratingFlashcards,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("重新生成 AI 总结")
                }
            }
        }
}

@Composable
private fun SummaryDualPane(
    summary: String,
    outlineExpanded: Boolean,
    outlineOnLeft: Boolean,
    initialHeading: String? = null,
    referenceCounts: Map<String, Int> = emptyMap(),
    onReferenceClick: ((String) -> Unit)? = null
) {
    val sections = remember(summary) { MarkdownOutlineParser.splitSections(summary) }
    val outlineItems = remember(sections) { sections.mapNotNull { it.heading } }
    val listState = rememberLazyListState()
    // 平板全量分支（无 heading）使用共享 ScrollState + 标题偏移表，驱动大纲跳转/高亮
    val scrollState = rememberScrollState()
    val headingOffsets = remember { mutableStateMapOf<String, Float>() }
    LaunchedEffect(Unit) {
        Log.d("AI总结渲染", "实际进入 -> 平板-双栏 SummaryDualPane（heading=${initialHeading ?: "null"} sections=${sections.size}）")
    }
    val scope = rememberCoroutineScope()

    var currentHeadingId by remember { mutableStateOf<String?>(null) }

    val items = remember(sections) { buildSummaryLazyItems(sections) }
    val headingItemIndex = remember(items) {
        items.mapIndexedNotNull { index, item ->
            item.section.heading?.headingId?.let { headingId -> headingId to index }
        }.toMap()
    }
    val sectionAtItem = remember(items) { items.map { it.section } }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                currentHeadingId = sectionAtItem.getOrNull(index)?.heading?.headingId
            }
    }

    // 无 heading（全量 Column 渲染）：由滚动偏移 + 标题偏移表推导当前章节
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value }
            .collect { offset ->
                currentHeadingId = headingOffsets.entries
                    .filter { it.value <= offset + 4f }
                    .maxByOrNull { it.value }?.key
            }
    }

    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            if (outlineExpanded && outlineOnLeft) {
                OutlinePanel(
                    items = outlineItems,
                    currentHeadingId = currentHeadingId,
                    onItemClick = { item ->
                        scope.launch {
                            if (!initialHeading.isNullOrBlank()) {
                                val idx = headingItemIndex[item.headingId]
                                if (idx != null) listState.animateScrollToItem(idx)
                            } else {
                                val offset = headingOffsets[item.headingId]
                                if (offset != null) scrollState.animateScrollTo(offset.toInt())
                            }
                        }
                    },
                    modifier = Modifier.width(280.dp)
                )
            }
            if (!initialHeading.isNullOrBlank()) {
                SectionedSummary(
                    sections = sections,
                    listState = listState,
                    modifier = Modifier.weight(1f),
                    referenceCounts = referenceCounts,
                    onReferenceClick = onReferenceClick,
                    initialHeading = initialHeading
                )
            } else {
                EagerTabletSummary(
                    sections = sections,
                    modifier = Modifier.weight(1f),
                    referenceCounts = referenceCounts,
                    onReferenceClick = onReferenceClick,
                    scrollState = scrollState,
                    onHeadingPositioned = { id, y -> headingOffsets[id] = y }
                )
            }
            if (outlineExpanded && !outlineOnLeft) {
                OutlinePanel(
                    items = outlineItems,
                    currentHeadingId = currentHeadingId,
                    onItemClick = { item ->
                        scope.launch {
                            if (!initialHeading.isNullOrBlank()) {
                                val idx = headingItemIndex[item.headingId]
                                if (idx != null) listState.animateScrollToItem(idx)
                            } else {
                                val offset = headingOffsets[item.headingId]
                                if (offset != null) scrollState.animateScrollTo(offset.toInt())
                            }
                        }
                    },
                    modifier = Modifier.width(280.dp)
                )
            }
        }
    }
}

private data class SummaryLazyItem(
    val key: String,
    val section: MarkdownSection,
    val isHeading: Boolean,
    val block: MarkdownRenderBlock? = null
)

private fun buildSummaryLazyItems(sections: List<MarkdownSection>): List<SummaryLazyItem> = buildList {
    sections.forEach { section ->
        val sectionKey = section.heading?.headingId ?: "preamble"
        section.heading?.let { add(SummaryLazyItem("$sectionKey-heading", section, true)) }
        splitMarkdownBlocks(section.body).forEach { block ->
            add(SummaryLazyItem("$sectionKey-${block.key}", section, false, block))
        }
    }
}

@Composable
private fun EagerTabletSummary(
    sections: List<MarkdownSection>,
    modifier: Modifier,
    referenceCounts: Map<String, Int>,
    onReferenceClick: ((String) -> Unit)?,
    scrollState: ScrollState,
    onHeadingPositioned: (String, Float) -> Unit
) {
    val items = remember(sections) { buildSummaryLazyItems(sections) }
    LaunchedEffect(Unit) {
        Log.d("AI总结渲染", "实际进入 -> 平板-全量渲染 EagerTabletSummary（sections=${sections.size}）")
    }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp)
    ) {
        items.forEach { item ->
            val heading = item.section.heading
            if (item.isHeading && heading != null) {
                // 记录章节标题在内容内的 Y 偏移（verticalScroll 滚动不改变内容坐标系），供大纲跳转/高亮
                Box(
                    Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            onHeadingPositioned(heading.headingId, coords.positionInParent().y)
                        }
                ) {
                    SectionHeading(heading, referenceCounts[heading.headingId] ?: 0, onReferenceClick)
                }
            } else {
                item.block?.let { MarkdownRenderer(it.content) }
            }
        }
    }
}

@Composable
private fun SectionedSummary(
    sections: List<MarkdownSection>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier,
    referenceCounts: Map<String, Int> = emptyMap(),
    onReferenceClick: ((String) -> Unit)? = null,
    initialHeading: String? = null
) {
    val items = remember(sections) { buildSummaryLazyItems(sections) }
    LaunchedEffect(Unit) {
        Log.d("AI总结渲染", "实际进入 -> 平板-分章节定位 SectionedSummary（heading=$initialHeading items=${items.size}）")
    }
    // 初始定位：从题集解析跳转到目标章节的标题 item
    LaunchedEffect(initialHeading, items) {
        val target = initialHeading ?: return@LaunchedEffect
        val idx = items.indexOfFirst { it.isHeading && it.section.heading?.headingId == target }
        if (idx >= 0) listState.scrollToItem(idx)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp)
    ) {
        items.forEach { lazyItem ->
            val section = lazyItem.section
            val heading = section.heading
            item(key = lazyItem.key) {
                HighlightSection(highlighted = heading?.headingId == initialHeading) {
                    if (lazyItem.isHeading && heading != null) {
                        SectionHeading(
                            heading,
                            referenceCounts[heading.headingId] ?: 0,
                            onReferenceClick
                        )
                    } else {
                        lazyItem.block?.let { MarkdownRenderer(it.content) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightSection(
    highlighted: Boolean,
    content: @Composable () -> Unit
) {
    if (!highlighted) {
        content()
        return
    }
    // 整个章节块（标题 + 全部所属正文）以 primaryContainer 色矩形高亮，闪烁一次后渐隐
    var pulseDone by rememberSaveable { mutableStateOf(false) }
    val pulseAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (!pulseDone) {
            pulseDone = true
            pulseAlpha.animateTo(1f, animationSpec = tween(durationMillis = 250))
            pulseAlpha.animateTo(0f, animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing))
        }
    }
    // 用 Column 纵向排列（Box 是叠层容器会导致标题与正文重叠）
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = pulseAlpha.value),
                MaterialTheme.shapes.medium
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        content()
    }
}

@Composable
private fun SectionHeading(
    item: OutlineItem,
    referenceCount: Int = 0,
    onReferenceClick: ((String) -> Unit)? = null
) {
    val style = when (item.level) {
        1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        2 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        3 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        else -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    }
    val topPadding = when (item.level) {
        1 -> 16.dp
        2 -> 14.dp
        else -> 10.dp
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.title,
            style = style,
            modifier = Modifier.weight(1f)
        )
        if (referenceCount > 0 && onReferenceClick != null) {
            val refText = if (referenceCount > 1) "被 $referenceCount 题引用" else "被 1 题引用"
            Text(
                text = refText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f), MaterialTheme.shapes.extraSmall)
                    .clickable { onReferenceClick(item.headingId) }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun OutlinePanel(
    items: List<OutlineItem>,
    currentHeadingId: String?,
    onItemClick: (OutlineItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    // 大纲过长时，随 AI 总结内容下滑自动滚动到当前高亮标题（若已可见则不打扰用户手动滚动）
    LaunchedEffect(currentHeadingId, items) {
        val idx = items.indexOfFirst { it.headingId == currentHeadingId }
        if (idx >= 0) {
            val alreadyVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == idx }
            if (!alreadyVisible) listState.animateScrollToItem(idx)
        }
    }

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.fillMaxSize()) {
            Text(
                text = "大纲",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            if (items.isEmpty()) {
                Text(
                    text = "无标题",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(items, key = { it.headingId }) { item ->
                        val isCurrent = item.headingId == currentHeadingId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemClick(item) }
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.secondaryContainer
                                    else androidx.compose.ui.graphics.Color.Transparent
                                )
                                .padding(start = 8.dp + 12.dp * (item.level - 1)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isCurrent) MaterialTheme.colorScheme.onSecondaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DescriptionTab(uiState: LogDetailUiState, entry: LogEntry, onImageClick: (Int) -> Unit, viewModel: LogDetailViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (entry.description.isNotBlank()) {
            Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(12.dp).animateContentSize()) {
                    Row(Modifier.fillMaxWidth().clickable { viewModel.toggleDescriptionExpanded() }, verticalAlignment = Alignment.CenterVertically) {
                        Text("描述", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Icon(if (uiState.descriptionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }
                    AnimatedVisibility(visible = uiState.descriptionExpanded) { Column { Spacer(Modifier.height(6.dp)); Text(entry.description, style = MaterialTheme.typography.bodyMedium) } }
                }
            }
        }
        if (entry.imagePaths.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("图片", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            entry.imagePaths.forEachIndexed { index, path ->
                val file = remember(path) { File(path) }
                if (index > 0) Spacer(Modifier.height(8.dp))
                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(file).size(2048,8192).build(), contentDescription = "图片 ${index + 1}", modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { onImageClick(index) }, contentScale = ContentScale.FillWidth)
            }
        }
        if (entry.description.isBlank() && entry.imagePaths.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("暂无描述和图片", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun ReviewNotesTab(
    viewModel: LogDetailViewModel,
    onNavigateToReviewNotes: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    entry: LogEntry
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notes = uiState.reviewNotes
    var deleteConfirmId by remember { mutableStateOf<Long?>(null) }

    if (deleteConfirmId != null) {
        ConfirmDialog(
            title = "删除心得",
            message = "删除后无法恢复，确定要删除这篇复盘心得吗？",
            confirmText = "删除",
            onConfirm = { viewModel.deleteReviewNote(deleteConfirmId!!); deleteConfirmId = null },
            onDismiss = { deleteConfirmId = null }
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("复盘心得", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onNavigateToReviewNotes) {
                Icon(Icons.Default.Add, "新建心得", Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        if (notes.isNotEmpty()) {
            notes.forEach { note ->
                var showDeleteDialog by remember { mutableStateOf(false) }
                if (showDeleteDialog) {
                    ConfirmDialog(
                        title = "删除心得",
                        message = "删除后无法恢复，确定要删除这篇复盘心得吗？",
                        confirmText = "删除",
                        onConfirm = { viewModel.deleteReviewNote(note.id); showDeleteDialog = false },
                        onDismiss = { showDeleteDialog = false }
                    )
                }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onNavigateToDetail(note.id) },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val (badgeText, badgeColor) = when (note.sourceType) {
                                com.sunjk.sunjktool.domain.model.ReviewNoteSource.FLASHCARD -> "闪卡错题" to MaterialTheme.colorScheme.tertiary
                                com.sunjk.sunjktool.domain.model.ReviewNoteSource.MANUAL -> "手动记录" to MaterialTheme.colorScheme.primary
                            }
                            Surface(shape = MaterialTheme.shapes.small, color = badgeColor.copy(alpha = 0.15f)) {
                                Text(badgeText, style = MaterialTheme.typography.labelSmall, color = badgeColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                            Spacer(Modifier.weight(1f))
                            Text(formatDateTime(note.createdDate), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            Spacer(Modifier.width(4.dp))
                            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, "删除", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(note.content.take(200), style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
                        if (note.imagePaths.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                note.imagePaths.take(5).forEach { path ->
                                    AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(java.io.File(path)).size(96, 96).build(), contentDescription = null, modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small), contentScale = ContentScale.Crop)
                                }
                                if (note.imagePaths.size > 5) {
                                    Box(Modifier.size(48.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                        Text("+${note.imagePaths.size - 5}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.RateReview, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(8.dp))
                    Text("暂无复盘心得", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SelfCheckTab(uiState: LogDetailUiState, viewModel: LogDetailViewModel) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val hasContent = uiState.selfCheckContent.isNotBlank()
    val hasSummary = !uiState.entry?.aiSummary.isNullOrBlank()
    var showRegenerateDialog by remember { mutableStateOf(false) }

    if (showRegenerateDialog) {
        ConfirmDialog(
            title = "重新生成自检内容",
            message = "重新生成将覆盖现有内容，确定继续吗？",
            confirmText = "确定",
            onConfirm = {
                showRegenerateDialog = false
                viewModel.generateSelfCheck(context)
            },
            onDismiss = { showRegenerateDialog = false }
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        // Header
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("自检", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (hasContent) {
                TextButton(
                    onClick = { viewModel.resetRevealed() },
                    enabled = uiState.selfCheckRevealedSet.isNotEmpty()
                ) { Text("全部遮挡", style = MaterialTheme.typography.labelSmall) }
            }
        }
        Spacer(Modifier.height(8.dp))

        when {
            uiState.isGeneratingSelfCheck -> {
                Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(16.dp)) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        Text("正在分析知识点并生成自检内容…", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            hasContent -> {
                // Hint card
                Card(Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.08f))) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TouchApp, null, Modifier.size(16.dp), tint = primaryColor)
                        Spacer(Modifier.width(8.dp))
                        Text("点击色块显示隐藏的知识点，再次点击重新遮挡",
                            style = MaterialTheme.typography.labelSmall, color = primaryColor)
                    }
                }
                // Self-check content
                SelfCheckRenderer(
                    text = uiState.selfCheckContent,
                    revealedSet = uiState.selfCheckRevealedSet,
                    primaryColor = primaryColor,
                    onBlankClick = { viewModel.toggleReveal(it) }
                )
                // Footer: regenerate
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showRegenerateDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("重新生成自检内容")
                }
            }
            !hasSummary -> {
                // No AI summary
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, null, Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(8.dp))
                        Text("暂无 AI 总结", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("请先生成 AI 总结后再使用自检功能",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }
            else -> {
                // Has summary but no self-check — show generate button
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.HelpOutline, null, Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(12.dp))
                        Text("自检模式", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("AI 将识别总结中的关键知识点并隐藏，\n点击色块可查看，测试自己的记忆效果",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.generateSelfCheck(context) },
                            shape = MaterialTheme.shapes.medium) {
                            Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("生成自检内容")
                        }
                    }
                }
            }
        }
    }
}

private enum class SummaryStepState { DONE, IN_PROGRESS, PENDING }

@Composable
private fun SummaryStepRow(label: String, state: SummaryStepState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (state) {
            SummaryStepState.DONE -> {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            SummaryStepState.IN_PROGRESS -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            SummaryStepState.PENDING -> {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(7.dp)
                        )
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = when (state) {
                SummaryStepState.DONE, SummaryStepState.IN_PROGRESS -> MaterialTheme.colorScheme.onSurface
                SummaryStepState.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            }
        )
    }
}

private fun formatTimeSpent(minutes: Int): String {
    return when {
        minutes < 60 -> "${minutes}分钟"
        else -> {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0) "${h}小时" else "${h}小时${m}分钟"
        }
    }
}

@Composable
private fun ModelToggle(selectedModel: String, onSelect: (String) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = selectedModel == ApiPreferences.MODEL_V4_FLASH,
            onClick = { onSelect(ApiPreferences.MODEL_V4_FLASH) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
        ) { Text("V4 Flash", style = MaterialTheme.typography.labelMedium) }
        SegmentedButton(
            selected = selectedModel == ApiPreferences.MODEL_V4_PRO,
            onClick = { onSelect(ApiPreferences.MODEL_V4_PRO) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
        ) { Text("V4 Pro", style = MaterialTheme.typography.labelMedium) }
    }
}
