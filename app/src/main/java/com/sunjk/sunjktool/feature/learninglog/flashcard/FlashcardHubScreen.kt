package com.sunjk.sunjktool.feature.learninglog.flashcard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardHubScreen(
    viewModel: FlashcardHubViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSession: (Long) -> Unit,
    onGenerateFlashcards: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val context = LocalContext.current

    // Style selection dialog
    if (uiState.showStyleDialog) {
        val presets = listOf("核心", "易错", "详解", "混淆", "拓展", "检索增强")
        var selectedStyle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.dismissStyleDialog() },
            title = { Text("选择生成风格") },
            text = {
                Column {
                    Text("选择一种预设风格", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presets.size) { idx ->
                            val preset = presets[idx]
                            val isSelected = selectedStyle == preset
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedStyle = if (isSelected) "" else preset },
                                label = { Text(preset, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.customStyle.ifBlank { selectedStyle },
                        onValueChange = { viewModel.updateCustomStyle(it) },
                        label = { Text("自定义风格") },
                        placeholder = { Text("或输入想要的风格...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("卡片数量", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !uiState.useCustomCount,
                            onClick = { viewModel.setUseCustomCount(false) },
                            label = { Text("由AI决定") }
                        )
                        FilterChip(
                            selected = uiState.useCustomCount,
                            onClick = { viewModel.setUseCustomCount(true) },
                            label = { Text("自定义") }
                        )
                    }
                    if (uiState.useCustomCount) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.customCardCount,
                            onValueChange = { viewModel.updateCustomCardCount(it.filter { c -> c.isDigit() }) },
                            label = { Text("数量") },
                            placeholder = { Text("例如: 6") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("生成来源", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = uiState.includeOcr,
                            onClick = viewModel::toggleIncludeOcr,
                            label = { Text("OCR识别", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = uiState.includeDescription,
                            onClick = viewModel::toggleIncludeDescription,
                            label = { Text("描述", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = uiState.includeAiSummary,
                            onClick = viewModel::toggleIncludeAiSummary,
                            label = { Text("AI总结", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("生成题型", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = uiState.enableTrueFalse,
                            onClick = viewModel::toggleTrueFalse,
                            label = { Text("判断", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = uiState.enableSingleChoice,
                            onClick = viewModel::toggleSingleChoice,
                            label = { Text("单选", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = uiState.enableMultiChoice,
                            onClick = viewModel::toggleMultiChoice,
                            label = { Text("多选", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = uiState.enableMemory,
                            onClick = viewModel::toggleMemory,
                            label = { Text("记忆", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val finalStyle = uiState.customStyle.ifBlank { selectedStyle }.ifBlank { "核心" }
                    val count = if (uiState.useCustomCount) uiState.customCardCount.toIntOrNull() ?: 0 else 0
                    viewModel.generateFlashcards(context, finalStyle, count)
                }) {
                    Text("开始生成")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissStyleDialog() }) {
                    Text("取消")
                }
            }
        )
    }

    LaunchedEffect(uiState.navigateToSessionId) {
        uiState.navigateToSessionId?.let { id ->
            viewModel.clearNavigation()
            onNavigateToSession(id)
        }
    }

    if (uiState.deleteConfirmId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDelete() },
            title = { Text("删除闪卡记录") },
            text = { Text("确定要删除这组闪卡吗？删除后无法恢复。") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDelete() }) {
                    Text("取消")
                }
            }
        )
    }

    // Preview dialog — full screen, read-only cards
    uiState.previewSession?.let { session ->
        Dialog(
            onDismissRequest = { viewModel.dismissPreview() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("预览闪卡 (${session.cards.size}张)") },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.dismissPreview() }) {
                                Icon(Icons.Default.Close, "关闭")
                            }
                        }
                    )
                }
            ) { previewPadding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(previewPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(session.cards) { idx, card ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Header: index + type badge + correctness + knowledgePoint
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("#${idx + 1}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    val (typeLabel, typeIcon) = when (card) {
                                        is com.sunjk.sunjktool.domain.model.Flashcard.TrueFalse -> "判断题" to Icons.Default.CheckCircleOutline
                                        is com.sunjk.sunjktool.domain.model.Flashcard.SingleChoice -> "单选题" to Icons.Default.RadioButtonChecked
                                        is com.sunjk.sunjktool.domain.model.Flashcard.MultiChoice -> "多选题" to Icons.Default.CheckBox
                                        is com.sunjk.sunjktool.domain.model.Flashcard.Memory -> "识记卡片" to Icons.Default.SwitchAccessShortcut
                                    }
                                    AssistChip(onClick = {}, label = { Text(typeLabel, style = MaterialTheme.typography.labelSmall) }, leadingIcon = { Icon(typeIcon, null, Modifier.size(14.dp)) })
                                    // Correctness rate box (only if card has been attempted at least once)
                                    val ar = session.answers[idx]
                                    if (ar != null && ar.totalAttempts > 0) {
                                        CorrectnessBox(ar.totalAttempts, ar.correctCount)
                                    }
                                    // Knowledge point badge
                                    if (card.knowledgePoint.isNotBlank()) {
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                card.knowledgePoint,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                // Question / content
                                when (card) {
                                    is com.sunjk.sunjktool.domain.model.Flashcard.TrueFalse -> {
                                        Text(card.question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Spacer(Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(4.dp))
                                            Text("正确答案: ${if (card.answer) "正确" else "错误"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    is com.sunjk.sunjktool.domain.model.Flashcard.SingleChoice -> {
                                        Text(card.question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Spacer(Modifier.height(8.dp))
                                        card.options.forEachIndexed { optIdx, opt ->
                                            val isCorrect = optIdx == card.answerIndex
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                                Text("${('A' + optIdx)}. ${cleanOption(opt)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                                if (isCorrect) Icon(Icons.Default.CheckCircle, "正确", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                    is com.sunjk.sunjktool.domain.model.Flashcard.MultiChoice -> {
                                        Text(card.question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Spacer(Modifier.height(8.dp))
                                        card.options.forEachIndexed { optIdx, opt ->
                                            val isCorrect = optIdx in card.answerIndices
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                                Text("${('A' + optIdx)}. ${cleanOption(opt)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                                if (isCorrect) Icon(Icons.Default.CheckCircle, "正确", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                    is com.sunjk.sunjktool.domain.model.Flashcard.Memory -> {
                                        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text("📝 正面", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(card.front, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                                Spacer(Modifier.height(6.dp))
                                                Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                                                Spacer(Modifier.height(6.dp))
                                                Text("📝 背面", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(card.back, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }
                                }
                                // Explanation
                                if (card.explanation.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text("解析：", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(2.dp))
                                    com.sunjk.sunjktool.ui.components.MarkdownRenderer(card.explanation)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("AI 闪卡") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                // Generation progress — step indicator
                if (uiState.isGenerating) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(12.dp))
                            val isRAG = uiState.generationPhase in listOf("gap", "retrieval", "design")
                            // OCR step
                            StepRow(
                                label = "图片文字识别",
                                state = when {
                                    uiState.generationPhase == "ocr" -> StepState.IN_PROGRESS
                                    uiState.generationPhase in listOf("gap", "retrieval", "design", "idle") && isRAG -> StepState.DONE
                                    uiState.generationPhase == "idle" && !isRAG -> StepState.DONE
                                    else -> StepState.PENDING
                                }
                            )
                            if (isRAG) {
                                // Gap analysis step
                                StepRow(
                                    label = "知识缺口分析",
                                    state = when {
                                        uiState.generationPhase == "gap" -> StepState.IN_PROGRESS
                                        uiState.generationPhase in listOf("retrieval", "design") -> StepState.DONE
                                        else -> StepState.PENDING
                                    }
                                )
                                // Knowledge retrieval step
                                StepRow(
                                    label = "补充知识检索",
                                    state = when {
                                        uiState.generationPhase == "retrieval" -> StepState.IN_PROGRESS
                                        uiState.generationPhase == "design" -> StepState.DONE
                                        else -> StepState.PENDING
                                    }
                                )
                            }
                            // Flashcard design step
                            StepRow(
                                label = "闪卡设计生成",
                                state = when {
                                    uiState.generationPhase == "design" -> StepState.IN_PROGRESS
                                    uiState.generationPhase == "idle" -> StepState.DONE
                                    else -> StepState.PENDING
                                }
                            )
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                // Generate new cards
                item {
                    Card(
                        onClick = { viewModel.showStyleDialog() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddCircle, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text("生成新闪卡", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("根据当前学习内容生成一组全新的闪卡", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ChevronRight, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Auto-advance toggle
                item {
                    val prefs = context.getSharedPreferences("flashcard_prefs", android.content.Context.MODE_PRIVATE)
                    var autoAdvance by remember { mutableStateOf(prefs.getBoolean("auto_advance", false)) }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("答对自动下一张", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "答对闪卡后自动跳转到下一张",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoAdvance,
                                onCheckedChange = {
                                    autoAdvance = it
                                    prefs.edit().putBoolean("auto_advance", it).apply()
                                }
                            )
                        }
                    }
                }

                // Session history
                val summaries = viewModel.sessionSummaries
                if (summaries.isNotEmpty()) {
                    item {
                        Text("闪卡记录", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    }
                    itemsIndexed(summaries) { idx, summary ->
                        Card(
                            onClick = { viewModel.onSessionClick(summary.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Row(modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("第${summary.index}组 · ${summary.cardCount}张卡片", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Spacer(Modifier.width(8.dp))
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                summary.style,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (summary.answeredCount > 0) {
                                            Icon(Icons.Default.CheckCircle, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(4.dp))
                                            Text("${summary.correctCount}/${summary.answeredCount} 正确", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(Modifier.width(8.dp))
                                        }
                                        if (summary.isComplete) {
                                            AssistChip(
                                                onClick = { },
                                                label = { Text("已完成", style = MaterialTheme.typography.labelSmall) },
                                                leadingIcon = { Icon(Icons.Default.Done, null, Modifier.size(14.dp)) }
                                            )
                                        } else {
                                            Text("未完成", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(dateFmt.format(Date(summary.createdDate)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                                IconButton(onClick = { viewModel.showPreview(summary.id) }) {
                                    Icon(Icons.Default.Visibility, "预览", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                }
                                IconButton(onClick = { viewModel.requestDelete(summary.id) }) {
                                    Icon(Icons.Default.Delete, "删除", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                }
                                Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Wrong answers
                val wrong = viewModel.wrongAnswers
                if (wrong.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text("错题集 (${wrong.size}道)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    itemsIndexed(wrong) { _, wrongAns ->
                        Card(
                            onClick = { viewModel.onSessionClick(wrongAns.sessionId) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Cancel, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    val label = when (wrongAns.card) {
                                        is com.sunjk.sunjktool.domain.model.Flashcard.TrueFalse -> "判断题"
                                        is com.sunjk.sunjktool.domain.model.Flashcard.SingleChoice -> "单选题"
                                        is com.sunjk.sunjktool.domain.model.Flashcard.MultiChoice -> "多选题"
                                        else -> "题目"
                                    }
                                    Text("$label: ${when (wrongAns.card) {
                                        is com.sunjk.sunjktool.domain.model.Flashcard.TrueFalse -> wrongAns.card.question.take(20)
                                        is com.sunjk.sunjktool.domain.model.Flashcard.SingleChoice -> wrongAns.card.question.take(20)
                                        is com.sunjk.sunjktool.domain.model.Flashcard.MultiChoice -> wrongAns.card.question.take(20)
                                        is com.sunjk.sunjktool.domain.model.Flashcard.Memory -> wrongAns.card.front.take(20)
                                    }}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    Text("第${wrongAns.sessionIndex}组 · 闪卡 #${wrongAns.cardIndex + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                if (uiState.sessions.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Style, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                Spacer(Modifier.height(8.dp))
                                Text("还没有生成过闪卡", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
internal fun CorrectnessBox(totalAttempts: Int, correctCount: Int) {
    if (totalAttempts <= 0) return
    val ratePct = correctCount * 100 / totalAttempts
    val (bgColor, textColor) = when {
        ratePct >= 80 -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        ratePct >= 50 -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        else          -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = bgColor
    ) {
        Text(
            " ${ratePct}%  ${correctCount}/${totalAttempts} ",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

private enum class StepState { DONE, IN_PROGRESS, PENDING }

@Composable
private fun StepRow(label: String, state: StepState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (state) {
            StepState.DONE -> {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            StepState.IN_PROGRESS -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            StepState.PENDING -> {
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
                StepState.DONE, StepState.IN_PROGRESS -> MaterialTheme.colorScheme.onSurface
                StepState.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            }
        )
    }
}
