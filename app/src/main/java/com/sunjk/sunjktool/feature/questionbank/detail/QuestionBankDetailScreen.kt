package com.sunjk.sunjktool.feature.questionbank.detail

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sunjk.sunjktool.domain.model.QuestionBankCategory
import com.sunjk.sunjktool.domain.model.SplitQuestionItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import com.sunjk.sunjktool.ui.components.ConfirmDialog
import com.sunjk.sunjktool.ui.components.EmptyState
import com.sunjk.sunjktool.ui.components.LoadingIndicator
import com.sunjk.sunjktool.ui.components.MarkdownRenderer
import com.sunjk.sunjktool.util.ImageUtil
import com.sunjk.sunjktool.util.SummaryLinkHelper
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionBankDetailScreen(
    viewModel: QuestionBankDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSubCategory: (Long) -> Unit,
    onNavigateToEdit: () -> Unit,
    onNavigateToAddSubCategory: () -> Unit,
    onNavigateToBreadcrumb: (Long) -> Unit,
    initialQuestionId: Long? = null,
    onNavigateToLogDetail: (logId: Long, heading: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 内部链接点击：跳转到学习记录 AI 总结对应章节
    val handleInternalLink: (String) -> Unit = { url ->
        SummaryLinkHelper.parseInternalLinkUrl(url)?.let { target ->
            onNavigateToLogDetail(target.logEntryId, target.headingId)
        }
    }

    // 反向跳入：初始定位并展开指定题目（仅首次进入定位；从下级返回时保持原滚动位置）
    val listState = rememberLazyListState()
    var didInitialScroll by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(initialQuestionId, uiState.questions) {
        if (didInitialScroll) return@LaunchedEffect
        val targetId = initialQuestionId ?: return@LaunchedEffect
        val qIndex = uiState.questions.indexOfFirst { it.id == targetId }
        if (qIndex >= 0) {
            var idx = 0
            if (uiState.breadcrumbs.isNotEmpty()) idx += 1
            if (uiState.subCategories.isNotEmpty()) idx += 2 + uiState.subCategories.size
            if (uiState.questions.isNotEmpty()) idx += 1
            listState.scrollToItem(idx + qIndex)
            didInitialScroll = true
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }

    // 一键直达：生成进度改用页面进度卡片展示，不使用生成解析对话框
    val isDirectGenerating = uiState.isGeneratingAnalysis && uiState.questionBankAutoSave

    // Image launchers
    var cropFile by remember { mutableStateOf<String?>(null) }

    val multiImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        val paths = uris.mapNotNull { ImageUtil.copyToInternal(context, it) }
        if (paths.isNotEmpty()) viewModel.addNewQuestionImages(paths)
    }

    val singleImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val path = ImageUtil.copyToInternal(context, uri)
            if (path != null) viewModel.addNewQuestionImages(listOf(path))
        }
    }

    var cameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraUri
        if (success && uri != null) {
            val file = File(context.filesDir, "images/${uri.lastPathSegment}")
            if (file.exists()) {
                cropFile = file.absolutePath
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider",
                File(context.filesDir, "images/camera_${System.currentTimeMillis()}.jpg").also {
                    it.parentFile?.mkdirs()
                }
            )
            cameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    // Crop screen
    cropFile?.let { path ->
        com.sunjk.sunjktool.feature.learninglog.edit.CropScreen(
            filePath = path,
            onDismiss = { cropFile = null },
            onCropped = { croppedPath ->
                cropFile = null
                viewModel.addNewQuestionImages(listOf(croppedPath))
            }
        )
    }

    // Delete dialogs
    if (uiState.deleteCategoryConfirm) {
        ConfirmDialog(
            title = "删除题集？",
            message = "删除后，子题集将移至上级目录，该题集内所有题目将被删除。",
            confirmText = "删除",
            onConfirm = viewModel::confirmDeleteCategory,
            onDismiss = viewModel::dismissDeleteCategory
        )
    }
    if (uiState.deleteQuestionId != null) {
        ConfirmDialog(
            title = "删除题目？",
            message = "该题目及其解析将被永久删除。",
            confirmText = "删除",
            onConfirm = viewModel::confirmDeleteQuestion,
            onDismiss = viewModel::dismissDeleteQuestion
        )
    }

    // Add dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加内容") },
            text = {
                Column {
                    TextButton(onClick = {
                        showAddDialog = false
                        onNavigateToAddSubCategory()
                    }) {
                        Text("新建子题集")
                    }
                    TextButton(onClick = {
                        showAddDialog = false
                        viewModel.showCreateQuestionForm()
                    }) {
                        Text("新建题目")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("取消") }
            }
        )
    }

    // New question form
    if (uiState.isCreatingQuestion && !isDirectGenerating) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isGeneratingAnalysis && !uiState.isSavingQuestions) {
                    viewModel.dismissCreateQuestionForm()
                }
            },
            title = {
                Text(
                    if (uiState.showSplitReview) "AI 识别到 ${uiState.splitQuestions.size} 道题目，请确认"
                    else if (uiState.generatedAnalyses.isNotEmpty()) "解析已生成"
                    else "新建题目"
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (uiState.isGeneratingAnalysis) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.generationProgress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (uiState.showSplitReview) {
                        uiState.splitQuestions.forEach { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "第 ${item.index + 1} 题",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    TextField(
                                        value = uiState.editedSplitContent[item.index] ?: item.content,
                                        onValueChange = { viewModel.updateSplitContent(item.index, it) },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                        maxLines = 6
                                    )
                                    TextButton(onClick = { viewModel.removeSplitQuestion(item.index) }) {
                                        Text("移除", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        // Style hint input
                        TextField(
                            value = uiState.analysisStyleHint,
                            onValueChange = viewModel::updateAnalysisStyleHint,
                            label = { Text("解析风格偏好（可选）") },
                            placeholder = { Text("如：重点分析错因、侧重解题技巧、总结考点规律...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else if (uiState.generatedAnalyses.isNotEmpty()) {
                        uiState.splitQuestions.forEach { item ->
                            val analysis = uiState.generatedAnalyses[item.index] ?: return@forEach
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "第 ${item.index + 1} 题",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = uiState.editedSplitContent[item.index] ?: item.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(4.dp))
                                    MarkdownRenderer(analysis)
                                }
                            }
                        }
                    } else {
                        // Input form
                        TextField(
                            value = uiState.newQuestionContent,
                            onValueChange = viewModel::updateNewQuestionContent,
                            label = { Text("题目内容") },
                            placeholder = { Text("输入题目，多道题可一次性粘贴，AI 会自动拆分") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 10
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Image thumbnails
                        if (uiState.newQuestionImages.isNotEmpty()) {
                            Text(
                                text = "已选图片 (${uiState.newQuestionImages.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(uiState.newQuestionImages) { index, path ->
                                    Box {
                                        AsyncImage(
                                            model = File(path),
                                            contentDescription = null,
                                            modifier = Modifier.size(80.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                        IconButton(
                                            onClick = { viewModel.removeNewQuestionImage(index) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "移除",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Image picker buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                if (Build.VERSION.SDK_INT >= 33) {
                                    multiImagePicker.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                } else {
                                    singleImagePicker.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                            }) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("相册")
                            }
                            OutlinedButton(onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    val uri = FileProvider.getUriForFile(
                                        context, "${context.packageName}.fileprovider",
                                        File(context.filesDir, "images/camera_${System.currentTimeMillis()}.jpg").also {
                                            it.parentFile?.mkdirs()
                                        }
                                    )
                                    cameraUri = uri
                                    cameraLauncher.launch(uri)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("拍照")
                            }
                        }
                    }

                    uiState.newQuestionError?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                if (uiState.showSplitReview) {
                    Button(
                        onClick = { viewModel.generateAnalyses(context) },
                        enabled = !uiState.isGeneratingAnalysis
                    ) {
                        Text("确认并生成解析")
                    }
                } else if (uiState.generatedAnalyses.isNotEmpty()) {
                    Button(
                        onClick = viewModel::saveQuestions,
                        enabled = !uiState.isSavingQuestions
                    ) {
                        if (uiState.isSavingQuestions) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("保存全部题目")
                    }
                } else if (!uiState.isGeneratingAnalysis) {
                    Button(
                        onClick = { viewModel.startGeneration(context) },
                        enabled = uiState.newQuestionContent.isNotBlank() || uiState.newQuestionImages.isNotEmpty()
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("开始生成")
                    }
                }
            },
            dismissButton = {
                if (!uiState.isGeneratingAnalysis && !uiState.isSavingQuestions) {
                    TextButton(onClick = viewModel::dismissCreateQuestionForm) {
                        Text("取消")
                    }
                }
            }
        )
    }

    // Main content
    if (uiState.isLoading) {
        LoadingIndicator()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = {
                    Text(
                        text = uiState.category?.name ?: "题集",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleExpandAll) {
                        Icon(
                            if (uiState.globalExpandAll) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                            contentDescription = if (uiState.globalExpandAll) "折叠全部" else "展开全部"
                        )
                    }
                    IconButton(onClick = onNavigateToEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = viewModel::requestDeleteCategory) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        // 一键直达：生成进度以页面卡片展示，不使用生成解析对话框
        if (isDirectGenerating) {
            QuestionGenerationProgressCard(
                phase = uiState.generationPhase,
                progressText = uiState.generationProgress,
                error = uiState.newQuestionError,
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        val showEmpty = uiState.subCategories.isEmpty() && uiState.questions.isEmpty()

        if (showEmpty) {
            EmptyState(
                title = "暂无题目",
                subtitle = "点击右上角 + 添加题目或子题集",
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Breadcrumbs
            if (uiState.breadcrumbs.isNotEmpty()) {
                item(key = "breadcrumbs") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.breadcrumbs) { crumb ->
                            AssistChip(
                                onClick = { onNavigateToBreadcrumb(crumb.categoryId) },
                                label = {
                                    Text(
                                        crumb.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Sub-categories
            if (uiState.subCategories.isNotEmpty()) {
                item(key = "sub_header") {
                    Text(
                        text = "子题集",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(uiState.subCategories, key = { "sub_${it.id}" }) { sub ->
                    SubCategoryCard(
                        category = sub,
                        onClick = { onNavigateToSubCategory(sub.id) }
                    )
                }

                item(key = "divider1") {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }

            // Questions section header
            if (uiState.questions.isNotEmpty()) {
                item(key = "q_header") {
                    Text(
                        text = "题目 (${uiState.questions.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Questions
            items(uiState.questions, key = { "q_${it.id}" }) { question ->
                val isExpanded = uiState.globalExpandAll || question.id in uiState.expandedQuestionIds
                QuestionCard(
                    question = question,
                    isExpanded = isExpanded,
                    onToggle = { viewModel.toggleQuestion(question.id) },
                    onDelete = { viewModel.requestDeleteQuestion(question.id) },
                    isGlobalExpanded = uiState.globalExpandAll,
                    onInternalLinkClick = handleInternalLink
                )
            }
        }
    }
}

@Composable
private fun SubCategoryCard(
    category: QuestionBankCategory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Quiz,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (category.subCategoryCount > 0 || category.questionCount > 0) {
                    Text(
                        text = buildString {
                            if (category.subCategoryCount > 0) append("${category.subCategoryCount} 个子分类")
                            if (category.subCategoryCount > 0 && category.questionCount > 0) append(" · ")
                            if (category.questionCount > 0) append("${category.questionCount} 道题")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuestionCard(
    question: com.sunjk.sunjktool.domain.model.Question,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    isGlobalExpanded: Boolean,
    onInternalLinkClick: ((String) -> Unit)? = null
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Parse structured card JSON
    val card = remember(question.aiAnalysis) { parseCard(question.aiAnalysis) }
    val cardType = card?.type ?: "open"
    val options = card?.options ?: emptyList()
    val explanation = card?.explanation ?: question.aiAnalysis
    val knowledgePoint = card?.knowledgePoint ?: ""

    // Determine answer display
    val answerText = remember(card) {
        if (card == null) ""
        else buildAnswerText(card)
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "删除题目？",
            message = "该题目及其解析将被永久删除。",
            confirmText = "删除",
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    val interactionSource = remember { MutableInteractionSource() }
    val typeLabel = remember(cardType) {
        when (cardType) {
            "single_choice" -> "单选题"
            "multi_choice" -> "多选题"
            "true_false" -> "判断题"
            else -> "简答题"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = interactionSource,
                onClick = onToggle,
                onLongClick = { showDeleteConfirm = true }
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: type badge + knowledge point
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                if (knowledgePoint.isNotBlank()) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = knowledgePoint,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Question stem
            Text(
                text = question.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Options — flashcard-style
            if (options.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                options.forEachIndexed { index, option ->
                    val isAnswer = remember(card) {
                        when (cardType) {
                            "single_choice" -> card?.answerInt == index
                            "multi_choice" -> card?.answerIntList?.contains(index) == true
                            "true_false" -> (card?.answerBool == true && index == 0) || (card?.answerBool == false && index == 1)
                            else -> false
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = if (isExpanded && isAnswer)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Option letter badge
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = if (isExpanded && isAnswer)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${'A' + index}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isExpanded && isAnswer)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = option.replace(Regex("^[A-F][.、．]\\s*"), ""),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            // Show answer indicator when expanded
                            if (isExpanded && isAnswer) {
                                Text(
                                    text = "✓ 正确答案",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Image thumbnails
            if (question.imagePaths.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(question.imagePaths.take(5)) { path ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            AsyncImage(
                                model = File(path),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    if (question.imagePaths.size > 5) {
                        item {
                            Surface(
                                modifier = Modifier.size(64.dp),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "+${question.imagePaths.size - 5}",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Expand indicator
            if (!isGlobalExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isExpanded) "点击收起解析" else "点击展开解析",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }

            // Expanded: answer + explanation
            AnimatedVisibility(
                visible = isExpanded && explanation.isNotBlank(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))
                    // Answer summary
                    if (answerText.isNotBlank()) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = answerText,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    // Full explanation
                    MarkdownRenderer(explanation, onInternalLinkClick = onInternalLinkClick)
                }
            }
        }
    }
}

// ── Card parser helpers ─────────────────────────────────────────────

private data class ParsedCard(
    val type: String = "open",
    val options: List<String> = emptyList(),
    val answerInt: Int = -1,
    val answerIntList: List<Int> = emptyList(),
    val answerBool: Boolean? = null,
    val knowledgePoint: String = "",
    val explanation: String = ""
)

private val cardParser = Json { ignoreUnknownKeys = true; isLenient = true }

private fun parseCard(aiAnalysis: String): ParsedCard? {
    if (aiAnalysis.isBlank()) return null
    return try {
        val json = aiAnalysis.trim()
        if (!json.startsWith("{")) return null // old plain-text format, no structured card
        val raw = cardParser.decodeFromString<kotlinx.serialization.json.JsonObject>(json)
        val type = raw["type"]?.let { cardParser.decodeFromJsonElement<String>(it) } ?: "open"
        val options = raw["options"]?.let { cardParser.decodeFromJsonElement<List<String>>(it) } ?: emptyList()
        val explanation = raw["explanation"]?.let { cardParser.decodeFromJsonElement<String>(it) } ?: ""
        val knowledgePoint = raw["knowledgePoint"]?.let { cardParser.decodeFromJsonElement<String>(it) } ?: ""

        val answerElement = raw["answer"]
        val answerInt = if (answerElement != null) {
            try { cardParser.decodeFromJsonElement<Int>(answerElement) } catch (_: Exception) { -1 }
        } else -1

        val answerIntList = if (answerElement != null) {
            try { cardParser.decodeFromJsonElement<List<Int>>(answerElement) } catch (_: Exception) { emptyList() }
        } else emptyList()

        val answerBool = if (answerElement != null) {
            try { cardParser.decodeFromJsonElement<Boolean>(answerElement) } catch (_: Exception) { null }
        } else null

        ParsedCard(type, options, answerInt, answerIntList, answerBool, knowledgePoint, explanation)
    } catch (_: Exception) {
        null
    }
}

private fun buildAnswerText(card: ParsedCard): String = buildString {
    when (card.type) {
        "single_choice" -> {
            if (card.answerInt >= 0 && card.answerInt < card.options.size) {
                append("正确答案：${('A' + card.answerInt)}. ")
                append(card.options[card.answerInt].replace(Regex("^[A-F][.、．]\\s*"), ""))
            }
        }
        "multi_choice" -> {
            if (card.answerIntList.isNotEmpty()) {
                append("正确答案：")
                card.answerIntList.forEachIndexed { i, idx ->
                    if (i > 0) append("、")
                    append("${('A' + idx)}")
                }
            }
        }
        "true_false" -> {
            when {
                card.answerBool == true -> append("答案：正确 ✓")
                card.answerBool == false -> append("答案：错误 ✗")
            }
        }
    }
}


// ── 题集一键直达：生成进度卡片（仿学习记录 AI 总结风格）──────────────

private enum class QbStepState { DONE, IN_PROGRESS, PENDING }

@Composable
private fun QuestionGenerationProgressCard(
    phase: String,
    progressText: String,
    error: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "正在生成题目…",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))

            QbStepRow(
                label = "图片文字识别",
                state = qbStepState(phase, doneIn = setOf("split", "retrieval", "generation"), inProgressIn = "ocr")
            )
            QbStepRow(
                label = "题目拆分",
                state = qbStepState(phase, doneIn = setOf("retrieval", "generation"), inProgressIn = "split")
            )
            QbStepRow(
                label = "知识检索",
                state = qbStepState(phase, doneIn = setOf("generation"), inProgressIn = "retrieval")
            )
            QbStepRow(
                label = "解析生成",
                state = qbStepState(phase, doneIn = setOf("idle"), inProgressIn = "generation")
            )

            if (progressText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            error?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun qbStepState(phase: String, doneIn: Set<String>, inProgressIn: String): QbStepState =
    when {
        phase in doneIn -> QbStepState.DONE
        phase == inProgressIn -> QbStepState.IN_PROGRESS
        else -> QbStepState.PENDING
    }

@Composable
private fun QbStepRow(label: String, state: QbStepState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (state) {
            QbStepState.DONE -> {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            QbStepState.IN_PROGRESS -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            QbStepState.PENDING -> {
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
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = when (state) {
                QbStepState.DONE, QbStepState.IN_PROGRESS -> MaterialTheme.colorScheme.onSurface
                QbStepState.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            }
        )
    }
}
