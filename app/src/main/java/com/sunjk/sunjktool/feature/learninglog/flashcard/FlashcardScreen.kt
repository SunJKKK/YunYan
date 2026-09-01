package com.sunjk.sunjktool.feature.learninglog.flashcard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunjk.sunjktool.domain.model.Flashcard
import com.sunjk.sunjktool.domain.model.FlashcardSession
import com.sunjk.sunjktool.ui.components.MarkdownRenderer

/** Strip leading letter prefix (e.g. "A.", "A. ", "A)") from option text, so the app can add its own. */
internal fun cleanOption(text: String): String =
    text.trimStart().replaceFirst(Regex("^[A-Z][.)、]\\s*"), "")

@Composable
private fun AnswerFeedback(isCorrect: Boolean, extra: String = "") {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (isCorrect) "回答正确！" else extra.ifBlank { "回答错误" },
            style = MaterialTheme.typography.titleSmall,
            color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(
    viewModel: FlashcardViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showScratchCanvas by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.isComplete) Text("闪卡完成")
                    else Text("闪卡 (${uiState.currentCardIndex + 1}/${uiState.session?.cards?.size ?: 0})")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    if (!uiState.isComplete && viewModel.isAnswered()) {
                        IconButton(onClick = viewModel::toggleExplanation) {
                            Icon(
                                Icons.Default.Psychology, "AI 解析",
                                tint = if (uiState.showExplanation) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                    if (!uiState.isComplete) {
                        IconButton(onClick = { showScratchCanvas = !showScratchCanvas }) {
                            Icon(
                                Icons.Default.Draw, "演草",
                                tint = if (showScratchCanvas) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.session == null || uiState.session!!.cards.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("暂无闪卡", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text("返回详情页点击 🧠 生成闪卡", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                }
                uiState.isComplete -> FlashcardSummaryView(viewModel, onNavigateBack)
                else -> FlashcardCardView(viewModel, uiState)
            }

            // Scratch canvas overlay (no scrim — truly transparent)
            if (showScratchCanvas) {
                ScratchCanvas(onClose = { showScratchCanvas = false })
            }
        }
    }

    // Continue previous session dialog
    if (uiState.showContinueDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissContinueDialog() },
            title = { Text("继续答题") },
            text = {
                val answered = uiState.userAnswers.size
                val total = uiState.session?.cards?.size ?: 0
                Text("检测到上次未完成的答题进度（已答 ${answered}/${total} 题），是否继续？")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.continueFromPrevious() }) {
                    Text("继续答题")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.restartFromContinue() }) {
                    Text("重新开始")
                }
            }
        )
    }
}

@Composable
private fun FlashcardCardView(viewModel: FlashcardViewModel, uiState: FlashcardUiState) {
    val card = viewModel.currentCard ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header: type badge + knowledgePoints + correctness (right-aligned, always shown)
        val (typeLabel, typeIcon) = when (card) {
            is Flashcard.TrueFalse -> "判断题" to Icons.Default.CheckCircleOutline
            is Flashcard.SingleChoice -> "单选题" to Icons.Default.RadioButtonChecked
            is Flashcard.MultiChoice -> "多选题" to Icons.Default.CheckBox
            is Flashcard.Memory -> "记忆卡片" to Icons.Default.SwitchAccessShortcut
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(
                onClick = { },
                label = { Text(typeLabel, style = MaterialTheme.typography.labelSmall) },
                leadingIcon = { Icon(typeIcon, null, Modifier.size(16.dp)) }
            )
            if (card.knowledgePoint.isNotBlank()) {
                val kps = card.knowledgePoint.split("、", ",", "，", ";").map { it.trim() }.filter { it.isNotEmpty() }
                kps.forEach { kp ->
                    SuggestionChip(
                        onClick = { },
                        label = { Text(kp, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            // Per-card correctness — always visible at right edge
            val ar = uiState.session?.answers?.get(uiState.currentCardIndex)
            if (ar != null && ar.totalAttempts > 0) {
                CorrectnessBox(ar.totalAttempts, ar.correctCount)
            } else {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                ) {
                    Text(
                        " —  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // Card content with swipe
        var dragAccum by remember { mutableFloatStateOf(0f) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragAccum < -120f) viewModel.nextCard()
                            else if (dragAccum > 120f) viewModel.prevCard()
                            dragAccum = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            dragAccum += dragAmount
                        }
                    )
                }
        ) {
            AnimatedContent(
            targetState = uiState.currentCardIndex,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally { dir * it } + fadeIn()) togetherWith
                    (slideOutHorizontally { -dir * it } + fadeOut())
            },
            label = "card"
        ) {
            Column {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                        when (card) {
                            is Flashcard.TrueFalse -> TrueFalseContent(card, viewModel, uiState)
                            is Flashcard.SingleChoice -> SingleChoiceContent(card, viewModel, uiState)
                            is Flashcard.MultiChoice -> MultiChoiceContent(card, viewModel, uiState)
                            is Flashcard.Memory -> MemoryContent(card, viewModel, uiState)
                        }
                    }
                }
            }
        } // close AnimatedContent
        } // close Box

        // AI explanation
        AnimatedVisibility(
            visible = uiState.showExplanation,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            if (uiState.showExplanation) {
                Spacer(Modifier.height(20.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("AI 解析", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        MarkdownRenderer(card.explanation)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Navigation — always visible
        val isAnswered = viewModel.isAnswered()
        val totalCards = uiState.session?.cards?.size ?: 0
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = viewModel::prevCard,
                enabled = uiState.currentCardIndex > 0
            ) {
                Icon(Icons.Default.ChevronLeft, null, Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("上一张")
            }
            TextButton(
                onClick = viewModel::nextCard,
                enabled = isAnswered
            ) {
                Text(if (uiState.currentCardIndex + 1 >= totalCards) "查看结果" else "下一张")
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun TrueFalseContent(card: Flashcard.TrueFalse, viewModel: FlashcardViewModel, uiState: FlashcardUiState) {
    val ans = uiState.userAnswers[uiState.currentCardIndex] as? UserAnswer.TrueFalse
    val isAnswered = ans != null

    Text(card.question, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(20.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        val correctBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        val wrongBg = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
        val correctBorder = MaterialTheme.colorScheme.primary
        val wrongBorder = MaterialTheme.colorScheme.error

        OutlinedButton(
            onClick = { viewModel.answerTrueFalse(true) },
            enabled = !isAnswered,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = when { ans?.isCorrect == true -> correctBg; ans != null && !ans.isCorrect -> correctBg; else -> Color.Transparent }
            ),
            border = if (ans != null && card.answer) androidx.compose.foundation.BorderStroke(2.dp, correctBorder) else ButtonDefaults.outlinedButtonBorder
        ) {
            Icon(Icons.Default.Check, null, Modifier.size(20.dp), tint = if (ans != null && card.answer) correctBorder else MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(6.dp))
            Text("正确", color = if (ans != null && card.answer) correctBorder else MaterialTheme.colorScheme.onSurface)
        }
        OutlinedButton(
            onClick = { viewModel.answerTrueFalse(false) },
            enabled = !isAnswered,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = when { ans?.isCorrect == false -> correctBg; ans != null && ans.isCorrect -> wrongBg; else -> Color.Transparent }
            ),
            border = if (ans != null && !card.answer) androidx.compose.foundation.BorderStroke(2.dp, correctBorder) else ButtonDefaults.outlinedButtonBorder
        ) {
            Icon(Icons.Default.Close, null, Modifier.size(20.dp), tint = if (ans != null && !card.answer) correctBorder else MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(6.dp))
            Text("错误", color = if (ans != null && !card.answer) correctBorder else MaterialTheme.colorScheme.onSurface)
        }
    }
    if (isAnswered) {
        Spacer(Modifier.height(12.dp))
        AnswerFeedback(
            isCorrect = ans!!.isCorrect,
            extra = if (!ans.isCorrect) "正确答案是 ${if (card.answer) "正确" else "错误"}" else ""
        )
    }
}

@Composable
private fun SingleChoiceContent(card: Flashcard.SingleChoice, viewModel: FlashcardViewModel, uiState: FlashcardUiState) {
    val ans = uiState.userAnswers[uiState.currentCardIndex] as? UserAnswer.SingleChoice
    val isAnswered = ans != null

    Text(card.question, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(16.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        card.options.forEachIndexed { idx, option ->
            val isCorrectOpt = idx == card.answerIndex
            val isSelected = ans?.selectedIndex == idx
            val bgColor = when {
                ans != null && isCorrectOpt -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ans != null && isSelected && !isCorrectOpt -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surfaceContainerLow
            }
            val borderColor = when {
                ans != null && isCorrectOpt -> MaterialTheme.colorScheme.primary
                ans != null && isSelected && !isCorrectOpt -> MaterialTheme.colorScheme.error
                else -> Color.Transparent
            }

            Surface(
                onClick = { viewModel.answerSingleChoice(idx) },
                enabled = !isAnswered,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = bgColor,
                border = if (borderColor != Color.Transparent) androidx.compose.foundation.BorderStroke(1.5.dp, borderColor) else null
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${('A' + idx)}.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(24.dp)
                    )
                    Text(cleanOption(option), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    if (isAnswered && isCorrectOpt) Icon(Icons.Default.Check, "正确", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    if (isAnswered && isSelected && !isCorrectOpt) Icon(Icons.Default.Close, "错误", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (isAnswered) {
        Spacer(Modifier.height(12.dp))
        AnswerFeedback(isCorrect = ans!!.isCorrect)
    }
}

@Composable
private fun MultiChoiceContent(card: Flashcard.MultiChoice, viewModel: FlashcardViewModel, uiState: FlashcardUiState) {
    val ans = uiState.userAnswers[uiState.currentCardIndex] as? UserAnswer.MultiChoiceChoice
    val isConfirmed = ans?.confirmed == true

    Text(card.question, style = MaterialTheme.typography.titleMedium)
    Text("可多选", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        card.options.forEachIndexed { idx, option ->
            val isCorrectOpt = idx in card.answerIndices
            val isSelected = idx in (ans?.selectedIndices ?: emptySet())
            val bgColor = when {
                isConfirmed && isCorrectOpt -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                isConfirmed && isSelected && !isCorrectOpt -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceContainerLow
            }
            val borderColor = when {
                isConfirmed && isCorrectOpt -> MaterialTheme.colorScheme.primary
                isConfirmed && isSelected && !isCorrectOpt -> MaterialTheme.colorScheme.error
                else -> Color.Transparent
            }

            Surface(
                onClick = { viewModel.toggleMultiChoiceOption(idx) },
                enabled = !isConfirmed,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = bgColor,
                border = if (borderColor != Color.Transparent) androidx.compose.foundation.BorderStroke(1.5.dp, borderColor) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null,
                        enabled = !isConfirmed
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(cleanOption(option), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    if (isConfirmed && isCorrectOpt) Icon(Icons.Default.Check, "正确", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    if (isConfirmed && isSelected && !isCorrectOpt) Icon(Icons.Default.Close, "错误", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (!isConfirmed && (ans?.selectedIndices?.isNotEmpty() == true)) {
        Spacer(Modifier.height(12.dp))
        Button(onClick = { viewModel.confirmMultiChoice() }, modifier = Modifier.fillMaxWidth()) { Text("确认答案") }
    }
    if (isConfirmed) {
        Spacer(Modifier.height(12.dp))
        AnswerFeedback(isCorrect = ans!!.isCorrect)
    }
}

@Composable
private fun MemoryContent(card: Flashcard.Memory, viewModel: FlashcardViewModel, uiState: FlashcardUiState) {
    val ans = uiState.userAnswers[uiState.currentCardIndex] as? UserAnswer.Memory
    val isRevealed = viewModel.isMemoryRevealed()
    val isAssessed = ans?.assessed == true

    if (!isRevealed) {
        // Front - not yet flipped
        Card(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(card.front, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    FilledTonalButton(onClick = { viewModel.revealMemory() }) {
                        Icon(Icons.Default.Visibility, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("点击查看答案")
                    }
                }
            }
        }
    } else if (!isAssessed) {
        // Back - revealed, not yet assessed
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(card.front, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                Spacer(Modifier.height(12.dp))
                Text(card.back, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { viewModel.markMemoryUnknown() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.VisibilityOff, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("没记住")
            }
            FilledTonalButton(
                onClick = { viewModel.markMemoryKnown() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Done, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("记住了")
            }
        }
    } else {
        // Assessed
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(card.front, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                Spacer(Modifier.height(12.dp))
                Text(card.back, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (ans!!.known) Icons.Default.CheckCircle else Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (ans.known) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (ans.known) "已记住" else "标记为未记住",
                style = MaterialTheme.typography.titleSmall,
                color = if (ans.known) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun FlashcardSummaryView(viewModel: FlashcardViewModel, onNavigateBack: () -> Unit) {
    val total = viewModel.uiState.value.session?.cards?.size ?: 0
    val correct = viewModel.correctCount
    val wrong = viewModel.wrongCards
    val memCount = viewModel.uiState.value.userAnswers.values.count { it is UserAnswer.Memory }
    val accuracy = if (total - memCount > 0) (correct * 100 / (total - memCount)) else 100

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text("恭喜完成！", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(24.dp))

        // Score circle
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
            CircularProgressIndicator(
                progress = { accuracy / 100f },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 8.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = if (accuracy >= 60) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$correct / ${total - memCount}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("$accuracy%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Detail list
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("答题详情", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.uiState.value.session?.cards?.forEachIndexed { idx, _ ->
                        val ans = viewModel.uiState.value.userAnswers[idx]
                        val (icon, tint) = when (ans) {
                            is UserAnswer.TrueFalse -> (if (ans.isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel) to
                                (if (ans.isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            is UserAnswer.SingleChoice -> (if (ans.isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel) to
                                (if (ans.isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            is UserAnswer.MultiChoiceChoice -> (if (ans.isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel) to
                                (if (ans.isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            is UserAnswer.Memory -> Icons.Default.Edit to MaterialTheme.colorScheme.tertiary
                            else -> Icons.Default.HelpOutline to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Icon(icon, "第${idx + 1}题", modifier = Modifier.size(22.dp), tint = tint)
                    }
                }
            }
        }

        // Wrong answers review — flashcard-style cards
        if (wrong.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ErrorOutline, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(6.dp))
                Text("错题回顾", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            wrong.forEach { (idx, card) ->
                val ans = viewModel.uiState.value.userAnswers[idx]
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Header: index + type badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("#${idx + 1}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            val (typeLabel, typeIcon) = when (card) {
                                is Flashcard.TrueFalse -> "判断题" to Icons.Default.CheckCircleOutline
                                is Flashcard.SingleChoice -> "单选题" to Icons.Default.RadioButtonChecked
                                is Flashcard.MultiChoice -> "多选题" to Icons.Default.CheckBox
                                is Flashcard.Memory -> "识记卡片" to Icons.Default.SwitchAccessShortcut
                            }
                            AssistChip(onClick = {}, label = { Text(typeLabel, style = MaterialTheme.typography.labelSmall) }, leadingIcon = { Icon(typeIcon, null, Modifier.size(14.dp)) })
                        }
                        Spacer(Modifier.height(8.dp))
                        // Question / content with correct answer
                        when (card) {
                            is Flashcard.TrueFalse -> {
                                Text(card.question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(4.dp))
                                    Text("正确答案: ${if (card.answer) "正确" else "错误"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                (ans as? UserAnswer.TrueFalse)?.let {
                                    Spacer(Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Cancel, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                        Spacer(Modifier.width(4.dp))
                                        Text("你的答案: ${if (it.userAnswer) "正确" else "错误"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            is Flashcard.SingleChoice -> {
                                Text(card.question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(6.dp))
                                card.options.forEachIndexed { optIdx, opt ->
                                    val isCorrect = optIdx == card.answerIndex
                                    val isSelected = (ans as? UserAnswer.SingleChoice)?.selectedIndex == optIdx
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
                                        Text("${('A' + optIdx)}. ${cleanOption(opt)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                        if (isCorrect) Icon(Icons.Default.CheckCircle, "正确", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                        if (isSelected && !isCorrect) Icon(Icons.Default.Cancel, "错误", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            is Flashcard.MultiChoice -> {
                                Text(card.question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(6.dp))
                                card.options.forEachIndexed { optIdx, opt ->
                                    val isCorrect = optIdx in card.answerIndices
                                    val isSelected = (ans as? UserAnswer.MultiChoiceChoice)?.selectedIndices?.contains(optIdx) == true
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
                                        Text("${('A' + optIdx)}. ${cleanOption(opt)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                        if (isCorrect) Icon(Icons.Default.CheckCircle, "正确", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                        if (isSelected && !isCorrect) Icon(Icons.Default.Cancel, "错误", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            is Flashcard.Memory -> {
                                Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("📝 正面", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(card.front, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Spacer(Modifier.height(4.dp))
                                        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                                        Spacer(Modifier.height(4.dp))
                                        Text("📝 背面", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(card.back, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                        // Explanation always visible
                        if (card.explanation.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                            Spacer(Modifier.height(6.dp))
                            MarkdownRenderer(card.explanation)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            OutlinedButton(onClick = { viewModel.restart() }) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("再做一次")
            }
            Button(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("返回详情")
            }
        }

        // Export wrong cards to review notes
        val wrong = viewModel.wrongCards
        if (wrong.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            val exported = viewModel.uiState.value.exportedToReviewNote
            OutlinedButton(
                onClick = { viewModel.exportWrongCardsToReviewNote() },
                enabled = !exported,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    if (exported) Icons.Default.CheckCircle else Icons.Default.RateReview,
                    null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (exported) "已导出至复盘心得" else "导出错题至复盘心得")
            }
        }
    }
}
