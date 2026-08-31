package com.sunjk.sunjktool.feature.learninglog.detail

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.local.ApiPreferences
import com.sunjk.sunjktool.data.local.PromptKeys
import com.sunjk.sunjktool.di.AiGenerationManager
import com.sunjk.sunjktool.di.AiTaskStatus
import com.sunjk.sunjktool.di.AiTaskType
import com.sunjk.sunjktool.data.model.FlashcardSetJson
import com.sunjk.sunjktool.data.remote.DeepSeekApi
import com.sunjk.sunjktool.data.sync.SyncTrigger
import com.sunjk.sunjktool.domain.model.Flashcard
import com.sunjk.sunjktool.domain.model.LogEntry
import com.sunjk.sunjktool.domain.model.toDomain
import com.sunjk.sunjktool.domain.repository.FlashcardRepository
import com.sunjk.sunjktool.domain.repository.LogRepository
import com.sunjk.sunjktool.domain.model.ReviewNote
import com.sunjk.sunjktool.domain.repository.ReviewNoteRepository
import com.sunjk.sunjktool.util.ReviewHelper
import com.sunjk.sunjktool.util.ocr.OcrManager
import kotlinx.serialization.json.Json
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@Immutable
data class LogDetailUiState(
    val entry: LogEntry? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val isGeneratingSummary: Boolean = false,
    val summaryProgress: String = "",
    val summaryGenerationPhase: String = "idle",  // idle / ocr / gap / retrieval / summary
    val summaryRetrievalAugmented: Boolean = false,
    val isEditingSummary: Boolean = false,
    val summaryExpanded: Boolean = false,
    val descriptionExpanded: Boolean = false,
    val summaryText: String = "",
    val isGeneratingFlashcards: Boolean = false,
    val flashcardProgress: String = "",
    val generatedSessionId: Long? = null,
    val hasExistingFlashcards: Boolean = false,
    val requestGenerateFlashcards: Boolean = false,
    val navigateToHub: Boolean = false,
    val navigateToReviewNotes: Boolean = false,
    val showSummaryModeDialog: Boolean = false,
    val summaryGenerationMode: String = "standard",  // standard / rag / multi_agent
    val summaryPreprocessModel: String = ApiPreferences.MODEL_V4_FLASH,
    val summaryAgentModel: String = ApiPreferences.MODEL_V4_PRO,
    val summaryIntegrateModel: String = ApiPreferences.MODEL_V4_PRO,
    val summaryChunkStrategy: String = "chapter",  // chapter / section / auto — 多Agent分块策略
    val summaryMultiAgentStep: String = "",
    val summaryMultiAgentTotal: Int = 0,
    val summaryMultiAgentCurrent: Int = 0,
    // Tablet dual-pane outline
    val isTabletMode: Boolean = false,
    val summaryOutlineExpanded: Boolean = true,
    val summaryOutlineOnLeft: Boolean = false,
    val reviewNotes: List<ReviewNote> = emptyList(),
    val error: String? = null,
    // Self-check
    val selfCheckContent: String = "",
    val isGeneratingSelfCheck: Boolean = false,
    val selfCheckRevealedSet: Set<Int> = emptySet()
)


class LogDetailViewModel(
    private val repository: LogRepository,
    private val reviewHelper: ReviewHelper,
    private val deepSeekApi: DeepSeekApi,
    private val flashcardRepository: FlashcardRepository,
    private val reviewNoteRepository: ReviewNoteRepository,

    private val apiPreferences: ApiPreferences,
    private val logId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogDetailUiState())
    val uiState: StateFlow<LogDetailUiState> = _uiState.asStateFlow()

    private var cachedOcrText: String? = null

    init {
        _uiState.update {
            it.copy(
                isTabletMode = apiPreferences.isTabletMode(),
                summaryOutlineExpanded = apiPreferences.isSummaryOutlineExpanded(),
                summaryOutlineOnLeft = apiPreferences.isSummaryOutlineOnLeft()
            )
        }
        viewModelScope.launch {
            repository.getEntryById(logId).collect { entry ->
                val hasExisting = !entry?.aiSummary.isNullOrBlank()
                _uiState.update {
                    it.copy(
                        entry = entry,
                        isLoading = false,
                        summaryText = entry?.aiSummary ?: it.summaryText,
                        selfCheckContent = entry?.selfCheckContent ?: "",
                        summaryExpanded = it.summaryExpanded || !hasExisting
                    )
                }
            }
        }
        viewModelScope.launch {
            flashcardRepository.getLatestSession(logId).collect { session ->
                _uiState.update { it.copy(hasExistingFlashcards = session != null) }
            }
        }
        viewModelScope.launch {
            reviewNoteRepository.getByLogEntryId(logId).collect { notes ->
                _uiState.update { it.copy(reviewNotes = notes) }
            }
        }
        viewModelScope.launch {
            AiGenerationManager.tasks.collect { tasks ->
                val summary = tasks.firstOrNull { it.taskId == AiGenerationManager.taskIdFor(AiTaskType.SUMMARY, logId) }
                val selfCheck = tasks.firstOrNull { it.taskId == AiGenerationManager.taskIdFor(AiTaskType.SELF_CHECK, logId) }
                val flashcards = tasks.firstOrNull { it.taskId == AiGenerationManager.taskIdFor(AiTaskType.FLASHCARDS, logId) }
                _uiState.update {
                    it.copy(
                        isGeneratingSummary = summary?.status == AiTaskStatus.RUNNING,
                        summaryProgress = summary?.phase ?: "",
                        isGeneratingSelfCheck = selfCheck?.status == AiTaskStatus.RUNNING,
                        isGeneratingFlashcards = flashcards?.status == AiTaskStatus.RUNNING,
                        flashcardProgress = flashcards?.phase ?: ""
                    )
                }
            }
        }

    }

    fun showSummaryModeDialog() {
        // Load persisted model preferences
        _uiState.update {
            it.copy(
                showSummaryModeDialog = true,
                summaryPreprocessModel = apiPreferences.getSummaryPreprocessModel(),
                summaryAgentModel = apiPreferences.getSummaryAgentModel(),
                summaryIntegrateModel = apiPreferences.getSummaryIntegrateModel()
            )
        }
    }

    fun dismissSummaryModeDialog() {
        _uiState.update { it.copy(showSummaryModeDialog = false) }
    }

    fun setSummaryGenerationMode(mode: String) {
        _uiState.update { it.copy(summaryGenerationMode = mode) }
    }

    fun setSummaryPreprocessModel(model: String) {
        _uiState.update { it.copy(summaryPreprocessModel = model) }
    }

    fun setSummaryAgentModel(model: String) {
        _uiState.update { it.copy(summaryAgentModel = model) }
    }

    fun setSummaryIntegrateModel(model: String) {
        _uiState.update { it.copy(summaryIntegrateModel = model) }
    }

    fun setSummaryChunkStrategy(strategy: String) {
        _uiState.update { it.copy(summaryChunkStrategy = strategy) }
    }

    fun generateSummary(context: Context) {
        // Persist model choices
        val s = _uiState.value
        apiPreferences.setSummaryPreprocessModel(s.summaryPreprocessModel)
        apiPreferences.setSummaryAgentModel(s.summaryAgentModel)
        apiPreferences.setSummaryIntegrateModel(s.summaryIntegrateModel)

        AiGenerationManager.start(AiTaskType.SUMMARY, logId, s.entry?.title ?: "AI 总结")
        AiGenerationManager.updatePhase(AiTaskType.SUMMARY, logId, "准备中")

        when (s.summaryGenerationMode) {
            "rag" -> generateRagSummary(context)
            "multi_agent" -> generateMultiAgentSummary(context)
            else -> generateStandardSummary(context)
        }
    }

    private fun generateStandardSummary(context: Context) {
        val entry = _uiState.value.entry ?: return
        AiGenerationManager.scope.launch {
            _uiState.update { it.copy(isGeneratingSummary = true, summaryProgress = "正在识别图片文字…", summaryGenerationPhase = "ocr", summaryRetrievalAugmented = false) }
            try {
                val ocrAll = performOcr(context, entry)
                if (ocrAll.isBlank() && entry.description.isBlank() && entry.title.isBlank()) {
                    _uiState.update { it.copy(isGeneratingSummary = false, error = "未识别到任何文字，请确保图片包含文字或填写标题/描述") }
                    return@launch
                }

                val userMessage = buildUserMessage(ocrAll, entry)

                // Stream tokens from DeepSeek
                _uiState.update { it.copy(summaryProgress = "正在生成总结…", summaryGenerationPhase = "summary") }
                val fullSummary = StringBuilder()
                _uiState.update { it.copy(summaryText = "", summaryExpanded = true) }
                withTimeout(300000) {
                    deepSeekApi.chatCompletionStream(
                        systemPrompt = buildSummarySystemPrompt(
                            hasOcr = ocrAll.isNotBlank(),
                            retrievalAugmented = false
                        ),
                        userMessage = userMessage,
                        temperature = 0.3
                    ).collect { token ->
                        fullSummary.append(token)
                        _uiState.update { it.copy(summaryText = fullSummary.toString()) }
                    }
                }

                val final = fullSummary.toString().trim()
                Log.d("AI总结", "standard final blank=${final.isBlank()} len=${final.length}")
                if (final.isNotBlank()) {
                    Log.d("AI总结", "AI返回完成: ${final.length}字符")
                    repository.updateSummary(logId, final, System.currentTimeMillis())
                    Log.d("AI总结", "updateSummary done logId=$logId")
                }
                AiGenerationManager.updatePhase(AiTaskType.SUMMARY, logId, "生成完成")
                if (final.isBlank()) {
                    AiGenerationManager.fail(AiTaskType.SUMMARY, logId, "生成结果为空")
                } else {
                    AiGenerationManager.complete(AiTaskType.SUMMARY, logId)
                }
                _uiState.update {
                    it.copy(isGeneratingSummary = false, summaryProgress = "", summaryGenerationPhase = "idle", summaryText = final.ifBlank { "生成失败，请重试" })
                }
            } catch (e: Exception) {
                AiGenerationManager.fail(AiTaskType.SUMMARY, logId, "总结失败: ${e.message}")
                _uiState.update { it.copy(isGeneratingSummary = false, summaryGenerationPhase = "idle", error = "总结失败: ${e.message}") }
            }
        }
    }

    private fun generateRagSummary(context: Context) {
        val entry = _uiState.value.entry ?: return
        AiGenerationManager.scope.launch {
            _uiState.update { it.copy(isGeneratingSummary = true, summaryProgress = "正在识别图片文字…", summaryGenerationPhase = "ocr", summaryRetrievalAugmented = true) }
            try {
                val ocrAll = performOcr(context, entry)
                if (ocrAll.isBlank() && entry.description.isBlank() && entry.title.isBlank()) {
                    _uiState.update { it.copy(isGeneratingSummary = false, error = "未识别到任何文字，请确保图片包含文字或填写标题/描述") }
                    return@launch
                }
                val userMessage = buildUserMessage(ocrAll, entry)
                val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

                // Stage 1: Gap analysis
                _uiState.update { it.copy(summaryProgress = "正在分析知识缺口…", summaryGenerationPhase = "gap") }
                val gapResult = withTimeout(300000) {
                    deepSeekApi.chatCompletion(
                        systemPrompt = apiPreferences.getPrompt(PromptKeys.GAP_ANALYSIS) ?: "你是学习分析专家。仔细分析用户的学习材料，找出其中的知识缺口和可以深入扩展的方向。\n\n输出纯 JSON，格式：{\"gaps\":[{\"topic\":\"知识点名\",\"description\":\"缺口说明\",\"importance\":\"high|medium|low\"}],\"extensions\":[{\"topic\":\"知识点名\",\"direction\":\"可扩展方向\"}],\"missingDetails\":[\"缺失的细节1\",\"缺失的细节2\"]}\n\n分析要点：\n1. 是否有前置知识未覆盖？\n2. 是否有重要的关联概念未提及？\n3. 是否有容易混淆的相似概念需要区分？\n4. 是否可以深入挖掘某个知识点的原理或应用？\n5. 是否有实际案例或练习题可以补充？",
                        userMessage = "请分析以下学习材料中的知识缺口：\n\n$userMessage",
                        temperature = 0.4
                    )
                }
                val gapText = gapResult.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val gapAnalysis = jsonParser.decodeFromString<com.sunjk.sunjktool.data.model.GapAnalysisResult>(gapText)

                // Stage 2: Knowledge retrieval
                _uiState.update { it.copy(summaryProgress = "正在检索补充知识…", summaryGenerationPhase = "retrieval") }
                val gapSummary = buildString {
                    append("知识缺口：\n")
                    gapAnalysis.gaps.forEach { g -> append("- ${g.topic}（重要性：${g.importance}）：${g.description}\n") }
                    if (gapAnalysis.extensions.isNotEmpty()) {
                        append("\n可扩展方向：\n")
                        gapAnalysis.extensions.forEach { e -> append("- ${e.topic}：${e.direction}\n") }
                    }
                    if (gapAnalysis.missingDetails.isNotEmpty()) {
                        append("\n缺失细节：\n")
                        gapAnalysis.missingDetails.forEach { d -> append("- $d\n") }
                    }
                }
                val knowledgeResult = withTimeout(300000) {
                    deepSeekApi.chatCompletion(
                        systemPrompt = apiPreferences.getPrompt(PromptKeys.KNOWLEDGE_RETRIEVAL) ?: "你是知识检索专家。基于缺口分析结果，为每个缺口和扩展方向提供补充知识。请使用你的训练知识来填补这些空白。\n\n输出纯 JSON，格式：{\"supplements\":[{\"topic\":\"知识点名\",\"content\":\"补充的核心知识内容\",\"keyPoints\":[\"要点1\",\"要点2\"]}]}\n\n要求：\n1. 内容准确、有深度，不仅仅是表面定义\n2. 覆盖缺口分析中的所有重要缺口\n3. 对每个扩展方向提供有实质性内容的知识补充",
                        userMessage = "请根据以下缺口分析，提供补充知识：\n\n$gapSummary",
                        temperature = 0.3
                    )
                }
                val knowledgeText = knowledgeResult.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val knowledgeSupplement = jsonParser.decodeFromString<com.sunjk.sunjktool.data.model.KnowledgeSupplementResult>(knowledgeText)
                val supplementaryContext = buildString {
                    append("\n\n--- 补充知识（基于缺口分析检索）---\n")
                    knowledgeSupplement.supplements.forEach { s ->
                        append("【${s.topic}】${s.content}\n")
                        if (s.keyPoints.isNotEmpty()) append("要点：${s.keyPoints.joinToString("；")}\n")
                    }
                }

                // Stream summary with supplementary context
                _uiState.update { it.copy(summaryProgress = "正在生成总结…", summaryGenerationPhase = "summary") }
                val fullSummary = StringBuilder()
                _uiState.update { it.copy(summaryText = "", summaryExpanded = true) }
                withTimeout(300000) {
                    deepSeekApi.chatCompletionStream(
                        systemPrompt = buildSummarySystemPrompt(hasOcr = ocrAll.isNotBlank(), retrievalAugmented = true),
                        userMessage = userMessage + supplementaryContext,
                        temperature = 0.3
                    ).collect { token ->
                        fullSummary.append(token)
                        _uiState.update { it.copy(summaryText = fullSummary.toString()) }
                    }
                }
                val final = fullSummary.toString().trim()
                if (final.isNotBlank()) {
                    repository.updateSummary(logId, final, System.currentTimeMillis())
                }
                AiGenerationManager.updatePhase(AiTaskType.SUMMARY, logId, "生成完成")
                if (final.isBlank()) {
                    AiGenerationManager.fail(AiTaskType.SUMMARY, logId, "生成结果为空")
                } else {
                    AiGenerationManager.complete(AiTaskType.SUMMARY, logId)
                }
                _uiState.update {
                    it.copy(isGeneratingSummary = false, summaryProgress = "", summaryGenerationPhase = "idle", summaryText = final.ifBlank { "生成失败，请重试" })
                }
            } catch (e: Exception) {
                AiGenerationManager.fail(AiTaskType.SUMMARY, logId, "总结失败: ${e.message}")
                _uiState.update { it.copy(isGeneratingSummary = false, summaryGenerationPhase = "idle", error = "总结失败: ${e.message}") }
            }
        }
    }

    private fun generateMultiAgentSummary(context: Context) {
        val entry = _uiState.value.entry ?: return
        val s = _uiState.value
        AiGenerationManager.scope.launch {
            _uiState.update { it.copy(isGeneratingSummary = true, summaryProgress = "正在识别图片文字…", summaryGenerationPhase = "ocr", summaryRetrievalAugmented = false, summaryMultiAgentStep = "", summaryMultiAgentTotal = 0, summaryMultiAgentCurrent = 0) }
            try {
                // Phase 0: OCR
                val ocrAll = performOcr(context, entry)
                if (ocrAll.isBlank() && entry.description.isBlank() && entry.title.isBlank()) {
                    _uiState.update { it.copy(isGeneratingSummary = false, error = "未识别到任何文字") }
                    return@launch
                }
                val userMessage = buildUserMessage(ocrAll, entry)
                val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

                // Phase 1: Preprocessing — chunk following original structure
                _uiState.update { it.copy(summaryProgress = "正在分析内容结构…", summaryGenerationPhase = "preprocess", summaryMultiAgentStep = "预处理") }

                val chunkStrategyPrompt = when (s.summaryChunkStrategy) {
                    "chapter" -> "优先按原文的\"章/大节\"边界切分。相邻的、内容紧密关联的小节可以合并到同一章内。每块应包含该章/大节的完整内容。跨来源的相同内容合并到对应章下。"
                    "section" -> "优先按原文的\"节/小节\"边界切分。每个小节独立成块。内容过短的小节可合并相邻节，过长的节可以按知识点自然断开。"
                    else -> "AI自行判断最合理的切分粒度，平衡块数和每块内容量。优先遵循原文顺序结构，在合理的地方切分，不强行过细拆分。"
                }

                val preprocessResult = withTimeout(300000) {
                    deepSeekApi.chatCompletion(
                        systemPrompt = buildString {
                            append("你是一位文本分析专家。用户提供了学习材料，可能混合了多种来源（如OCR识别的讲义图片、课堂字幕文本、笔记等），这些来源描述的是同一堂课/同一份材料的内容，但以不同形式呈现。\n\n")
                            append("处理步骤：\n\n")
                            append("第一步：跨来源内容合并\n")
                            append("- 识别不同来源中讲述相同内容的段落。例如字幕的第3-5分钟可能在讲解\"二叉树的遍历\"，而讲义第2页也列出了\"二叉树的遍历\"的定义和例题\n")
                            append("- 将描述同一知识点的字幕内容和讲义内容合并到同一块中，标注来源（如\"字幕：...\\n讲义：...\"）\n")
                            append("- 注意：字幕和讲义不一定是逐段一一对应的——有时字幕详细讲而讲义只列要点，有时讲义有例题而字幕只是带过，合并时需要将两部分内容互补整合\n\n")
                            append("第二步：按结构切分\n")
                            append("- $chunkStrategyPrompt\n")
                            append("- 合并后的内容按原文的章节/进度顺序排列，不要按主题打乱重排\n")
                            append("- 如果没有明显章节标记，按自然段落语义分组\n")
                            append("- 不要遗漏次要内容，也不要切得太碎\n\n")
                            append("输出纯 JSON：{\"topics\":[{\"title\":\"块标题（用原文章节名或概括）\",\"relevance\":\"high|medium\",\"content\":\"合并后的完整原文（含来源标注）\"}]}")
                        },
                        userMessage = "请先合并跨来源内容，再按章节结构切分：\n\n$userMessage",
                        temperature = 0.3,
                        modelOverride = s.summaryPreprocessModel
                    )
                }
                val preText = preprocessResult.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val preData = jsonParser.decodeFromString<PreprocessResult>(preText)
                val topics = preData.topics

                if (topics.isEmpty()) {
                    _uiState.update { it.copy(isGeneratingSummary = false, error = "预处理未能识别出主题，请尝试其他模式") }
                    return@launch
                }

                // Phase 2: Per-topic summaries
                val topicSummaries = mutableListOf<TopicSummary>()
                val parallel = apiPreferences.isMultiAgentParallel()
                _uiState.update { it.copy(summaryMultiAgentTotal = topics.size, summaryMultiAgentCurrent = 0, summaryGenerationPhase = "topic_summary", summaryMultiAgentStep = "主题总结") }

                if (parallel) {
                    // Parallel execution
                    _uiState.update { it.copy(summaryProgress = "正在并行总结 ${topics.size} 个主题…") }
                    val results = coroutineScope {
                        topics.map { topic ->
                            async {
                                try {
                                    val summary = generateTopicSummary(topic, userMessage, s.summaryAgentModel)
                                    TopicSummary(topic.title, summary, true)
                                } catch (e: Exception) {
                                    TopicSummary(topic.title, "[总结失败: ${e.message}]", false)
                                }
                            }
                        }.map { deferred ->
                            val result = deferred.await()
                            _uiState.update { it.copy(summaryMultiAgentCurrent = it.summaryMultiAgentCurrent + 1) }
                            result
                        }
                    }
                    topicSummaries.addAll(results)
                }

                // Sequential execution (default, or fallback)
                if (!parallel || topicSummaries.isEmpty()) {
                    _uiState.update { it.copy(summaryMultiAgentCurrent = 0) }
                    for ((idx, topic) in topics.withIndex()) {
                        _uiState.update {
                            it.copy(
                                summaryProgress = "正在总结第 ${idx + 1}/${topics.size} 个主题：${topic.title}",
                                summaryMultiAgentCurrent = idx
                            )
                        }
                        try {
                            val summary = withTimeout(300000) {
                                generateTopicSummary(topic, userMessage, s.summaryAgentModel)
                            }
                            topicSummaries.add(TopicSummary(topic.title, summary, true))
                        } catch (e: Exception) {
                            topicSummaries.add(TopicSummary(topic.title, "[总结失败: ${e.message}]", false))
                        }
                        _uiState.update { it.copy(summaryMultiAgentCurrent = idx + 1) }
                    }
                }

                if (topicSummaries.isEmpty()) {
                    _uiState.update { it.copy(isGeneratingSummary = false, error = "所有主题总结均失败，请重试") }
                    return@launch
                }

                // Phase 3: Integration — stream final summary
                _uiState.update { it.copy(summaryProgress = "正在整合生成最终总结…", summaryGenerationPhase = "integrate", summaryMultiAgentStep = "整合生成") }
                val topicsText = topicSummaries.joinToString("\n\n---\n\n") { ts ->
                    "【${ts.title}】\n${ts.summary}"
                }

                val fullSummary = StringBuilder()
                _uiState.update { it.copy(summaryText = "", summaryExpanded = true) }
                withTimeout(300000) {
                    deepSeekApi.chatCompletionStream(
                        systemPrompt = buildString {
                            append("你是一位学习总结专家。以下是对学习材料各主题的分段总结，请整合为一份完整、连贯的最终总结。\n\n")
                            append("要求：\n1. 消除重复内容\n2. 统一行文风格\n3. 补充跨主题的关联和逻辑关系\n4. 确保整体结构流畅\n")
                            append("5. 使用与 AI 总结一致的排版：Markdown 标题、列表、表格、**粗体**、*斜体*\n")
                            append("6. 使用 <span style=\"...\"> 标签：color、background-color、font-size、font-weight、font-style、text-decoration\n")
                            append("   三色：<span style=\"background-color:#90CAF9\">概念</span> <span style=\"background-color:#FFF176\">知识点</span> <span style=\"background-color:#EF9A9A\">易错点</span>\n")
                            append("7. 重要内容可用夸张样式：特大字号、特粗、醒目颜色\n")
                            append("只允许 <span>，禁止 <script>/<iframe>/<object>/<embed>/<form>/<input>。中文，条理清晰。\n")
                            if (entry.subject.isNotBlank()) append("科目：${entry.subject}\n")
                            append("标题：${entry.title}")
                        },
                        userMessage = "请整合以下各主题总结为一份完整总结：\n\n$topicsText",
                        temperature = 0.3,
                        modelOverride = s.summaryIntegrateModel
                    ).collect { token ->
                        fullSummary.append(token)
                        _uiState.update { it.copy(summaryText = fullSummary.toString()) }
                    }
                }
                val final = fullSummary.toString().trim()
                if (final.isNotBlank()) {
                    repository.updateSummary(logId, final, System.currentTimeMillis())
                }
                AiGenerationManager.updatePhase(AiTaskType.SUMMARY, logId, "生成完成")
                if (final.isBlank()) {
                    AiGenerationManager.fail(AiTaskType.SUMMARY, logId, "生成结果为空")
                } else {
                    AiGenerationManager.complete(AiTaskType.SUMMARY, logId)
                }
                _uiState.update {
                    it.copy(isGeneratingSummary = false, summaryProgress = "", summaryGenerationPhase = "idle", summaryText = final.ifBlank { "生成失败，请重试" },
                        summaryMultiAgentStep = "", summaryMultiAgentTotal = 0, summaryMultiAgentCurrent = 0)
                }
            } catch (e: Exception) {
                AiGenerationManager.fail(AiTaskType.SUMMARY, logId, "总结失败: ${e.message}")
                _uiState.update { it.copy(isGeneratingSummary = false, summaryGenerationPhase = "idle", error = "总结失败: ${e.message}") }
            }
        }
    }

    private suspend fun generateTopicSummary(topic: PreprocessTopic, userMessage: String, model: String): String {
        return withTimeout(300000) {
            deepSeekApi.chatCompletion(
                systemPrompt = buildString {
                    append("你是一位学习总结专家。请为以下主题生成结构化 Markdown 总结。\n\n")
                    append("主题：${topic.title}\n\n")
                    append("排版：使用 Markdown（标题、列表、表格、粗体斜体）和 HTML <span style=\"...\"> 标签\n")
                    append("三色：<span style=\"background-color:#90CAF9\">概念</span> <span style=\"background-color:#FFF176\">知识点</span> <span style=\"background-color:#EF9A9A\">易错点</span>\n")
                    append("重要内容可用夸张样式突出。中文，条理清晰，深入透彻。")
                },
                userMessage = "请总结以下关于「${topic.title}」的内容：\n\n${topic.content}",
                temperature = 0.3,
                modelOverride = model
            )
        }
    }

    fun toggleSummaryExpanded() {
        _uiState.update { it.copy(summaryExpanded = !it.summaryExpanded) }
    }

    fun toggleSummaryOutline() {
        val expanded = !_uiState.value.summaryOutlineExpanded
        apiPreferences.setSummaryOutlineExpanded(expanded)
        _uiState.update { it.copy(summaryOutlineExpanded = expanded) }
    }

    fun toggleSummaryOutlinePosition() {
        val onLeft = !_uiState.value.summaryOutlineOnLeft
        apiPreferences.setSummaryOutlineOnLeft(onLeft)
        _uiState.update { it.copy(summaryOutlineOnLeft = onLeft) }
    }

    fun toggleDescriptionExpanded() {
        _uiState.update { it.copy(descriptionExpanded = !it.descriptionExpanded) }
    }

    fun toggleEditSummary() {
        val editing = _uiState.value.isEditingSummary
        if (editing) {
            // Save edited summary
            val text = _uiState.value.summaryText
            viewModelScope.launch {
                repository.updateSummary(logId, text, System.currentTimeMillis())
            }
        }
        _uiState.update { it.copy(isEditingSummary = !editing) }
    }

    fun updateSummaryText(text: String) {
        _uiState.update { it.copy(summaryText = text) }
    }

    fun deleteEntry() {
        viewModelScope.launch {
            try {
                _uiState.value.entry?.imagePaths?.forEach {
                    com.sunjk.sunjktool.util.ImageUtil.deleteInternal(it)
                }
                reviewHelper.deleteByEntryId(logId)
                repository.deleteEntry(logId)
                _uiState.update { it.copy(isDeleted = true) }
                SyncTrigger.bumpEntity("review_status")
                SyncTrigger.requestAutoSync()
                SyncTrigger.bumpEntity("log_entries")
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "删除失败: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearFlashcardNavigation() {
        _uiState.update { it.copy(generatedSessionId = null) }
    }

    fun onFlashcardButtonClick() {
        _uiState.update { it.copy(navigateToHub = true) }
    }

    fun clearHubNavigation() {
        _uiState.update { it.copy(navigateToHub = false) }
    }

    fun onReviewNoteClick() {
        _uiState.update { it.copy(navigateToReviewNotes = true) }
    }

    fun clearReviewNoteNavigation() {
        _uiState.update { it.copy(navigateToReviewNotes = false) }
    }

    fun deleteReviewNote(noteId: Long) {
        viewModelScope.launch {
            reviewNoteRepository.delete(noteId)
        }
    }

    fun replaceImagePath(oldPath: String, newPath: String) {
        val entry = _uiState.value.entry ?: return
        val newPaths = entry.imagePaths.map { if (it == oldPath) newPath else it }
        viewModelScope.launch {
            repository.saveEntry(entry.copy(imagePaths = newPaths))
        }
    }

    fun generateFlashcards(context: Context) {
        val entry = _uiState.value.entry ?: return
        AiGenerationManager.start(AiTaskType.FLASHCARDS, logId, entry.title)
        AiGenerationManager.scope.launch {
            _uiState.update { it.copy(isGeneratingFlashcards = true, flashcardProgress = "正在识别图片文字…") }
            try {
                val ocrAll = performOcr(context, entry)
                if (ocrAll.isBlank() && entry.description.isBlank()) {
                    _uiState.update { it.copy(isGeneratingFlashcards = false, error = "未识别到任何文字") }
                    return@launch
                }
                _uiState.update { it.copy(flashcardProgress = "正在生成闪卡…") }
                val userMessage = buildUserMessage(ocrAll, entry)

                val response = withTimeout(300000) {
                    deepSeekApi.chatCompletion(
                        systemPrompt = apiPreferences.getPrompt(PromptKeys.FLASHCARD) ?: "你是一个学习助教，擅长将学习材料转化为闪卡。请根据提供的OCR识别文字、用户描述、AI总结内容和科目信息，生成一套闪卡。\n\n要求：\n1. 输出纯 JSON，不要任何前缀、后缀或 markdown 标记。直接输出 JSON 对象。\n2. 总共生成卡片的数量，视内容丰富度自行拟定，必须覆盖所有知识点。\n3. 视情况使用以下四种类型（并非每种类型都要出现，而是根据实际确定知识点对应的卡片类型）：\n   - 判断题 (true_false)：陈述一个知识点，判断正误\n   - 单选题 (single_choice)：4 个选项，1 个正确答案\n   - 多选题 (multi_choice)：多个选项、多个正确答案\n   - 记忆卡片 (memory)：正面为概念/术语，背面为详细解释\n4. JSON 格式如下：\n{\n  \"cards\": [\n    {\"type\":\"true_false\",\"question\":\"...\",\"answer\":true,\"explanation\":\"...\"},\n    {\"type\":\"single_choice\",\"question\":\"...\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"answer\":0,\"explanation\":\"...\"},\n    {\"type\":\"multi_choice\",\"question\":\"...\",\"options\":[\"A\",\"B\",\"C\",\"D\",\"E\"],\"answers\":[0,2],\"explanation\":\"...\"},\n    {\"type\":\"memory\",\"front\":\"概念名称\",\"back\":\"详细解释\",\"explanation\":\"扩展知识...\"}\n  ]\n}\n5. 题目覆盖所有知识点，选项应有干扰性但不过分相似。\n6. explanation 必须条理清晰，解释为什么对/错。",
                        userMessage = userMessage,
                        temperature = 0.3
                    )
                }
                val json = Json { ignoreUnknownKeys = true; isLenient = true }
                val jsonText = response.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val set = json.decodeFromString<FlashcardSetJson>(jsonText)
                val cards: List<Flashcard> = set.cards.map { it.toDomain() }
                val sessionId = flashcardRepository.saveSession(entry.id, cards, "")
                AiGenerationManager.complete(AiTaskType.FLASHCARDS, logId)
                _uiState.update {
                    it.copy(
                        isGeneratingFlashcards = false,
                        flashcardProgress = "",
                        generatedSessionId = sessionId,
                        requestGenerateFlashcards = false
                    )
                }
            } catch (e: Exception) {
                AiGenerationManager.fail(AiTaskType.FLASHCARDS, logId, "闪卡生成失败: ${e.message}")
                _uiState.update {
                    it.copy(isGeneratingFlashcards = false, error = "闪卡生成失败: ${e.message}")
                }
            }
        }
    }

    private suspend fun performOcr(context: Context, entry: LogEntry): String {
        if (cachedOcrText != null) return cachedOcrText!!
        val paths = entry.imagePaths
        if (paths.isEmpty()) return ""
        return OcrManager.recognizeWithProgress(context, paths) { current, total ->
            _uiState.update { it.copy(summaryProgress = "正在识别图片文字 $current/$total…") }
        }.also { cachedOcrText = it }
    }

    // ---- Self-Check ----

    fun generateSelfCheck(context: Context) {
        val entry = _uiState.value.entry ?: return
        AiGenerationManager.start(AiTaskType.SELF_CHECK, logId, entry.title)
        val summary = entry.aiSummary.ifBlank { _uiState.value.summaryText }
        if (summary.isBlank()) {
            _uiState.update { it.copy(error = "请先生成 AI 总结") }
            return
        }
        AiGenerationManager.scope.launch {
            AiGenerationManager.updatePhase(AiTaskType.SELF_CHECK, logId, "正在生成自检内容…")
            _uiState.update { it.copy(isGeneratingSelfCheck = true) }
            try {
                val response = withTimeout(300000) {
                    deepSeekApi.chatCompletion(
                        systemPrompt = apiPreferences.getPrompt(PromptKeys.SELF_CHECK) ?: SELF_CHECK_SYSTEM_PROMPT,
                        userMessage = "请为以下学习总结生成自检内容：\n\n$summary",
                        temperature = 0.3
                    )
                }
                val cleaned = response.removePrefix("```html").removePrefix("```").removeSuffix("```").trim()
                if (cleaned.isNotBlank()) {
                    repository.updateSelfCheckContent(logId, cleaned, System.currentTimeMillis())
                    AiGenerationManager.complete(AiTaskType.SELF_CHECK, logId)
                    _uiState.update {
                        it.copy(
                            selfCheckContent = cleaned,
                            isGeneratingSelfCheck = false,
                            selfCheckRevealedSet = emptySet()
                        )
                    }
                } else {
                    AiGenerationManager.fail(AiTaskType.SELF_CHECK, logId, "生成失败，请重试")
                    _uiState.update { it.copy(isGeneratingSelfCheck = false, error = "生成失败，请重试") }
                }
            } catch (e: Exception) {
                AiGenerationManager.fail(AiTaskType.SELF_CHECK, logId, "自检生成失败: ${e.message}")
                _uiState.update { it.copy(isGeneratingSelfCheck = false, error = "自检生成失败: ${e.message}") }
            }
        }
    }

    fun toggleReveal(blankIndex: Int) {
        _uiState.update {
            val newSet = it.selfCheckRevealedSet.toMutableSet()
            if (newSet.contains(blankIndex)) newSet.remove(blankIndex) else newSet.add(blankIndex)
            it.copy(selfCheckRevealedSet = newSet)
        }
    }

    fun resetRevealed() {
        _uiState.update { it.copy(selfCheckRevealedSet = emptySet()) }
    }

    companion object {
        private val SELF_CHECK_SYSTEM_PROMPT = """
你是一个学习自检助手。根据提供的学习总结，将其中的关键知识点用 <blank>知识点</blank> 标签包裹，用于自测记忆效果。

## 应该挖空的内容
- 核心概念和术语（如：协程、ViewBinding、依赖注入）
- 重要的定义和公式名称（如：时间复杂度、牛顿第二定律）
- 关键结论和原理（如：面向对象三大特性、HTTP 无状态）
- 重要的数字、参数、配置值（如：默认超时30秒、线程池大小为核心数×2）

## 不应该挖空的内容
- 普通的连接词、过渡句、辅助描述
- 标题、列表项符号等 Markdown 格式标记
- 已经用 HTML span 高亮的内容（挖掉后 span 标签会丢失）
- 整个句子（挖空后无法根据上下文推断的内容）

## 要求
1. 保持原文的完整意思和 Markdown 格式不变
2. 尽可能覆盖所有应该挖的关键知识点，不要遗漏
3. 挖掉的内容应能根据上下文推断出来（不要挖掉唯一的信息）
4. 清除原文中的所有 HTML 标签，只保留纯 Markdown 格式和文字内容
5. 直接输出处理后的内容，不要任何前言或解释
        """.trimIndent()

        private fun buildSummarySystemPrompt(hasOcr: Boolean, retrievalAugmented: Boolean): String {
            val base = if (hasOcr) """
你是一个学习助手，负责将OCR识别内容、用户描述等学习材料整理成结构化的知识总结。

## 输入说明
- 「用户描述」可能包含课堂字幕片段，也可能包含用户对 AI 的直接指示（如"重点总结第二部分"），请识别并遵循其中的指示。
- 「OCR识别内容」来自图片，可能是课堂讲义、错题、笔记或课件截图。请归纳其中的知识点和考点，而非描述题目或图片本身；识别题目涉及的核心概念，并可延伸关联内容。

## 排版要求
1. 用 Markdown 组织整体结构：每个大章节用 # 一级标题并冠以"一、"、"二、"等中文编号，小节用 ## 二级标题并冠以"1."、"2."等数字编号，细分点用 ### 三级标题；不使用四级及以下标题。不要因为文档开头已经用过 # 就把后续大章节退到 ## —— 只要是大章节，就大胆用 #。列表、表格、**粗体**、*斜体* 等可自由使用。
2. 行内强调使用 HTML <span style="..."> 标签，可自由运用以下 CSS 属性并任意组合：
   color、background-color、font-size、font-weight、font-style、text-decoration。
   只允许 <span> 标签；严禁输出 <script>、<iframe>、<object>、<embed>、<form>、<input> 标签。
3. 高亮底色只使用以下三种颜色，并严格按语义区分：
   - #90CAF9（蓝色）：概念名词、专业术语
   - #FFF176（黄色）：知识点
   - #EF9A9A（红色）：易错点、常见错误
   示例：网络安全中的<span style="background-color: #90CAF9">CIA</span>：<span style="background-color: #FFF176">机密性</span>、<span style="background-color: #FFF176">完整性</span>、<span style="background-color: #FFF176">可用性</span>
4. 对极其重要的知识点，可以用夸张的行内样式突出：特大字号（如 font-size: 1.4em）、特粗（font-weight: 900）、醒目文字颜色（如 color: #D32F2F）、text-shadow 等，可叠加使用。
   示例：<span style="font-size: 1.4em; font-weight: 900; color: #D32F2F; background-color: #FFCDD2">这是极易错的重要知识点！</span>
5. 高亮与夸张样式应克制使用，只标注真正重要的内容。

用户上传的文件可能是课程字幕、课件或笔记。请结合这些材料进行总结。

直接输出总结内容，不要前言或客套话。"""
            else """
你是一个学习助手，负责根据标题、科目和描述将学习内容整理成结构化的知识总结。

## 输入说明
- 「用户描述」可能包含课堂字幕片段，也可能包含用户对 AI 的直接指示（如"重点总结第二部分"），请识别并遵循其中的指示。
- 请根据标题、科目和描述，归纳该学习内容的核心知识点、相关概念和考点。

## 排版要求
1. 用 Markdown 组织整体结构：每个大章节用 # 一级标题并冠以"一、"、"二、"等中文编号，小节用 ## 二级标题并冠以"1."、"2."等数字编号，细分点用 ### 三级标题；不使用四级及以下标题。不要因为文档开头已经用过 # 就把后续大章节退到 ## —— 只要是大章节，就大胆用 #。列表、表格、**粗体**、*斜体* 等可自由使用。
2. 行内强调使用 HTML <span style="..."> 标签，可自由运用以下 CSS 属性并任意组合：
   color、background-color、font-size、font-weight、font-style、text-decoration。
   只允许 <span> 标签；严禁输出 <script>、<iframe>、<object>、<embed>、<form>、<input> 标签。
3. 高亮底色只使用以下三种颜色，并严格按语义区分：
   - #90CAF9（蓝色）：概念名词、专业术语
   - #FFF176（黄色）：知识点
   - #EF9A9A（红色）：易错点、常见错误
   示例：网络安全中的<span style="background-color: #90CAF9">CIA</span>：<span style="background-color: #FFF176">机密性</span>、<span style="background-color: #FFF176">完整性</span>、<span style="background-color: #FFF176">可用性</span>
4. 对极其重要的知识点，可以用夸张的行内样式突出：特大字号（如 font-size: 1.4em）、特粗（font-weight: 900）、醒目文字颜色（如 color: #D32F2F）、text-shadow 等，可叠加使用。
   示例：<span style="font-size: 1.4em; font-weight: 900; color: #D32F2F; background-color: #FFCDD2">这是极易错的重要知识点！</span>
5. 高亮与夸张样式应克制使用，只标注真正重要的内容。

用户上传的文件可能是课程字幕、课件或笔记。请结合这些材料进行总结。

直接输出总结内容，不要前言或客套话。"""
            val ragSuffix = "\n\n请结合补充知识，在总结中既覆盖原始材料的知识点，也融入补充的新知识点，形成完整、有深度的知识总结。"
            return (base + if (retrievalAugmented) ragSuffix else "").trimIndent()
        }
    }

    private fun buildUserMessage(ocrAll: String, entry: LogEntry): String = buildString {
        if (ocrAll.isNotBlank()) {
            append("OCR识别内容：\n")
            append(ocrAll)
            append("\n\n")
        }
        if (entry.attachmentText.isNotBlank()) {
            append("上传文件内容（可能是课程字幕、课件或笔记）：\n")
            append(entry.attachmentText)
            append("\n\n")
        }
        if (entry.description.isNotBlank()) {
            append("用户描述：${entry.description}\n")
        }
        if (entry.subject.isNotBlank()) append("科目：${entry.subject}\n")
        append("标题：${entry.title}\n")
    }
}

// Multi-agent preprocessing models

@kotlinx.serialization.Serializable
data class PreprocessResult(
    val topics: List<PreprocessTopic>
)

@kotlinx.serialization.Serializable
data class PreprocessTopic(
    val title: String,
    val relevance: String = "medium",
    val content: String = ""
)

data class TopicSummary(
    val title: String,
    val summary: String,
    val success: Boolean = true
)
