package com.sunjk.sunjktool.feature.learninglog.detail

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.local.AiModelOption
import com.sunjk.sunjktool.data.local.AiProvider
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
import com.sunjk.sunjktool.domain.repository.QuestionBankRepository
import com.sunjk.sunjktool.domain.repository.ReviewNoteRepository
import com.sunjk.sunjktool.util.ReviewHelper
import com.sunjk.sunjktool.util.SummaryLinkHelper
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
    val summaryRetrievalModel: String = AiModelOption.DEEPSEEK_FLASH.id,
    val flashcardModel: String = AiModelOption.DEEPSEEK_FLASH.id,
    val selfCheckModel: String = AiModelOption.DEEPSEEK_FLASH.id,
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
    val selfCheckRevealedSet: Set<Int> = emptySet(),
    // 反向关联：headingId -> 引用该章节的题目数（由题集解析正文扫描而来）
    val referenceCounts: Map<String, Int> = emptyMap()
)


class LogDetailViewModel(
    private val repository: LogRepository,
    private val reviewHelper: ReviewHelper,
    private val deepSeekApi: DeepSeekApi,
    private val flashcardRepository: FlashcardRepository,
    private val reviewNoteRepository: ReviewNoteRepository,

    private val apiPreferences: ApiPreferences,
    private val logId: Long,
    private val questionBankRepository: QuestionBankRepository
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
        // 反向关联：扫描全部题目解析正文，统计当前学习记录的每个章节被多少道题引用
        viewModelScope.launch {
            questionBankRepository.getAllQuestions().collect { allQuestions ->
                val counts = mutableMapOf<String, Int>()
                for (q in allQuestions) {
                    SummaryLinkHelper.extractInternalLinks(q.aiAnalysis).forEach { link ->
                        if (link.logEntryId == logId) {
                            counts[link.headingId] = (counts[link.headingId] ?: 0) + 1
                        }
                    }
                }
                _uiState.update { it.copy(referenceCounts = counts) }
            }
        }
        viewModelScope.launch {
            AiGenerationManager.tasks.collect { tasks ->
                val summary = tasks.firstOrNull { it.taskId == AiGenerationManager.taskIdFor(AiTaskType.SUMMARY, logId) }
                val selfCheck = tasks.firstOrNull { it.taskId == AiGenerationManager.taskIdFor(AiTaskType.SELF_CHECK, logId) }
                val flashcards = tasks.firstOrNull { it.taskId == AiGenerationManager.taskIdFor(AiTaskType.FLASHCARDS, logId) }
                _uiState.update {
                    val runningSummary = summary?.status == AiTaskStatus.RUNNING
                    it.copy(
                        isGeneratingSummary = runningSummary,
                        summaryProgress = summary?.phase ?: "",
                        summaryGenerationPhase = if (runningSummary) summary.phaseKey.ifBlank { it.summaryGenerationPhase } else it.summaryGenerationPhase,
                        summaryGenerationMode = if (runningSummary && summary.mode.isNotBlank()) summary.mode else it.summaryGenerationMode,
                        summaryRetrievalAugmented = if (runningSummary && summary.mode.isNotBlank()) summary.mode == "rag" else it.summaryRetrievalAugmented,
                        summaryMultiAgentTotal = if (runningSummary && summary.stepTotal > 0) summary.stepTotal else it.summaryMultiAgentTotal,
                        summaryMultiAgentCurrent = if (runningSummary) summary.step else it.summaryMultiAgentCurrent,
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
                summaryIntegrateModel = apiPreferences.getSummaryIntegrateModel(),
                summaryRetrievalModel = apiPreferences.getAiModelFor("summary_retrieval")
            )
        }
    }

    fun dismissSummaryModeDialog() {
        _uiState.update { it.copy(showSummaryModeDialog = false) }
    }

    fun setSummaryGenerationMode(mode: String) {
        _uiState.update { it.copy(summaryGenerationMode = mode) }
    }

    fun setSummaryPreprocessModel(option: AiModelOption) {
        _uiState.update { it.copy(summaryPreprocessModel = option.id) }
    }

    fun setSummaryAgentModel(option: AiModelOption) {
        _uiState.update { it.copy(summaryAgentModel = option.id) }
    }

    fun setSummaryIntegrateModel(option: AiModelOption) {
        _uiState.update { it.copy(summaryIntegrateModel = option.id) }
    }

    fun setSummaryRetrievalModel(option: AiModelOption) {
        _uiState.update { it.copy(summaryRetrievalModel = option.id) }
    }

    fun setFlashcardModel(option: AiModelOption) {
        _uiState.update { it.copy(flashcardModel = option.id) }
    }

    fun setSelfCheckModel(option: AiModelOption) {
        _uiState.update { it.copy(selfCheckModel = option.id) }
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
        apiPreferences.setAiModelFor("summary_retrieval", s.summaryRetrievalModel)

        AiGenerationManager.start(AiTaskType.SUMMARY, logId, s.entry?.title ?: "AI 总结", mode = s.summaryGenerationMode)
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
                val summaryOption = AiModelOption.fromId(_uiState.value.summaryAgentModel)

                // Stream tokens from DeepSeek
                _uiState.update { it.copy(summaryProgress = "正在生成总结…", summaryGenerationPhase = "summary") }
                AiGenerationManager.updatePhase(AiTaskType.SUMMARY, logId, "正在生成总结…", progress = 0.7f, phaseKey = "summary")
                val fullSummary = StringBuilder()
                _uiState.update { it.copy(summaryText = "", summaryExpanded = true) }
                run {
                    deepSeekApi.chatCompletionStream(
                        systemPrompt = buildSummarySystemPrompt(
                            hasOcr = ocrAll.isNotBlank(),
                            retrievalAugmented = false
                        ),
                        userMessage = userMessage,
                        temperature = 0.3,
                        modelOverride = summaryOption.model,
                        provider = summaryOption.provider
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
                _uiState.update { it.copy(isGeneratingSummary = false, summaryGenerationPhase = "idle", error = "总结失败: ${e.message}", summaryText = "生成失败：${e.message}") }
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
                val summaryOption = AiModelOption.fromId(_uiState.value.summaryAgentModel)
                val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

                // Stage 1: Gap analysis
                _uiState.update { it.copy(summaryProgress = "正在分析知识缺口…", summaryGenerationPhase = "gap") }
                AiGenerationManager.updatePhase(AiTaskType.SUMMARY, logId, "正在分析知识缺口…", progress = 0.35f, phaseKey = "gap")
                val gapResult = run {
                    deepSeekApi.chatCompletion(
                        systemPrompt = apiPreferences.getPrompt(PromptKeys.GAP_ANALYSIS) ?: "你是学习分析专家。仔细分析用户的学习材料，找出其中的知识缺口和可以深入扩展的方向。\n\n输出纯 JSON，格式：{\"gaps\":[{\"topic\":\"知识点名\",\"description\":\"缺口说明\",\"importance\":\"high|medium|low\"}],\"extensions\":[{\"topic\":\"知识点名\",\"direction\":\"可扩展方向\"}],\"missingDetails\":[\"缺失的细节1\",\"缺失的细节2\"]}\n\n分析要点：\n1. 是否有前置知识未覆盖？\n2. 是否有重要的关联概念未提及？\n3. 是否有容易混淆的相似概念需要区分？\n4. 是否可以深入挖掘某个知识点的原理或应用？\n5. 是否有实际案例或练习题可以补充？",
                        userMessage = "请分析以下学习材料中的知识缺口：\n\n$userMessage",
                        temperature = 0.4,
                        modelOverride = summaryOption.model,
                        provider = summaryOption.provider
                    )
                }
                val gapText = gapResult.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val gapAnalysis = jsonParser.decodeFromString<com.sunjk.sunjktool.data.model.GapAnalysisResult>(gapText)

                // Stage 2: Knowledge retrieval
                _uiState.update { it.copy(summaryProgress = "正在检索补充知识…", summaryGenerationPhase = "retrieval") }
                AiGenerationManager.updatePhase(AiTaskType.SUMMARY, logId, "正在检索补充知识…", progress = 0.5f, phaseKey = "retrieval")
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
                val retrievalOption = AiModelOption.fromId(_uiState.value.summaryRetrievalModel)
                val webSearchEnabled = retrievalOption.provider == AiProvider.QWEN
                val retrievalSystemPrompt = apiPreferences.getPrompt(PromptKeys.KNOWLEDGE_RETRIEVAL) ?: if (webSearchEnabled) {
                    "你是知识检索专家。基于缺口分析结果，为每个缺口和扩展方向提供补充知识。已开启联网搜索，请优先采用联网检索到的最新信息，确保内容准确、有深度，必要时标注来源。\n\n输出纯 JSON，格式：{\"supplements\":[{\"topic\":\"知识点名\",\"content\":\"补充的核心知识内容\",\"keyPoints\":[\"要点1\",\"要点2\"]}]}\n\n要求：\n1. 内容准确、有深度，不仅仅是表面定义\n2. 覆盖缺口分析中的所有重要缺口\n3. 对每个扩展方向提供有实质性内容的知识补充"
                } else {
                    "你是知识检索专家。基于缺口分析结果，为每个缺口和扩展方向提供补充知识。请使用你的训练知识来填补这些空白。\n\n输出纯 JSON，格式：{\"supplements\":[{\"topic\":\"知识点名\",\"content\":\"补充的核心知识内容\",\"keyPoints\":[\"要点1\",\"要点2\"]}]}\n\n要求：\n1. 内容准确、有深度，不仅仅是表面定义\n2. 覆盖缺口分析中的所有重要缺口\n3. 对每个扩展方向提供有实质性内容的知识补充"
                }
                val knowledgeResult = run {
                    deepSeekApi.chatCompletion(
                        systemPrompt = retrievalSystemPrompt,
                        userMessage = "请根据以下缺口分析，提供补充知识：\n\n$gapSummary",
                        temperature = 0.3,
                        modelOverride = retrievalOption.model,
                        provider = retrievalOption.provider,
                        enableSearch = webSearchEnabled
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
                AiGenerationManager.updatePhase(AiTaskType.SUMMARY, logId, "正在生成总结…", progress = 0.8f, phaseKey = "summary")
                val fullSummary = StringBuilder()
                _uiState.update { it.copy(summaryText = "", summaryExpanded = true) }
                run {
                    deepSeekApi.chatCompletionStream(
                        systemPrompt = buildSummarySystemPrompt(hasOcr = ocrAll.isNotBlank(), retrievalAugmented = true),
                        userMessage = userMessage + supplementaryContext,
                        temperature = 0.3,
                        modelOverride = summaryOption.model,
                        provider = summaryOption.provider
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
                _uiState.update { it.copy(isGeneratingSummary = false, summaryGenerationPhase = "idle", error = "总结失败: ${e.message}", summaryText = "生成失败：${e.message}") }
            }
        }
    }

    private fun generateMultiAgentSummary(context: Context) {
        val entry = _uiState.value.entry ?: return
        val s = _uiState.value
        val preprocessOption = AiModelOption.fromId(s.summaryPreprocessModel)
        val agentOption = AiModelOption.fromId(s.summaryAgentModel)
        val integrateOption = AiModelOption.fromId(s.summaryIntegrateModel)
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

                // Phase 1: Preprocessing — AI 只看全文输出归并区间，本地按区间拼装（AI 不再复制原文）
                _uiState.update { it.copy(summaryProgress = "正在分析内容结构…", summaryGenerationPhase = "preprocess", summaryMultiAgentStep = "预处理") }
                AiGenerationManager.updatePhase(AiTaskType.SUMMARY, logId, "正在分析内容结构…", progress = 0.12f, phaseKey = "preprocess")

                // 本地先把全文切成带编号的候选单元（纯机械，不依赖段落/标题/来源）
                val units = splitIntoCandidateUnits(userMessage)
                if (units.isEmpty()) {
                    _uiState.update { it.copy(isGeneratingSummary = false, error = "未能从内容中切分出有效单元，请重试") }
                    return@launch
                }
                _uiState.update { it.copy(summaryProgress = "本地切分完成 ${units.size} 个单元…", summaryGenerationPhase = "chunking") }
                AiGenerationManager.updatePhase(AiTaskType.SUMMARY, logId, "本地切分完成 ${units.size} 个单元…", progress = 0.15f, phaseKey = "chunking")
                val unitListing = units.joinToString("\n") { "[${it.id}] ${it.text}" }
                AiGenerationManager.updatePhase(AiTaskType.SUMMARY, logId, "AI 主题归并中…", progress = 0.18f, phaseKey = "preprocess")

                val chunkStrategyPrompt = when (s.summaryChunkStrategy) {
                    "chapter" -> "主题粒度较大：优先把讲同一大节的相邻单元归并为一个大主题（对应原文的章/大节级别）。"
                    "section" -> "主题粒度较细：对应原文的节/小节级别，相邻的小知识点尽量独立成主题。"
                    else -> "AI自行判断最合理的归并粒度，平衡主题数量和每个主题的内容量。"
                }

                val preprocessResult = run {
                    deepSeekApi.chatCompletion(
                        systemPrompt = buildString {
                            append("你是一位文本分析专家。用户提供了学习材料，已按句子拆分为带编号的单元（每行 [编号] 内容）。这些单元可能来自不同来源（如OCR讲义、字幕、笔记）且已混合在一起，请忽略来源差异，只依据内容语义判断。\n\n")
                            append("处理步骤：\n\n")
                            append("第一步：语义归并\n")
                            append("- 识别讲同一知识点的单元，归并为同一主题\n")
                            append("- 一个主题可以由多个不连续单元组成（例如某些单元在开头、另有一些在中部），只要讲同一知识点就归并到一起\n")
                            append("- 不要遗漏任何单元；每个单元必须恰好归入一个主题，难以独立成主题的短单元归入语义最接近的主题\n\n")
                            append("第二步：主题命名与重要性\n")
                            append("- 为每个主题命名（用原章节名或概括），并给出重要性 high/medium/low\n\n")
                            append("第三步：归并粒度\n")
                            append("- $chunkStrategyPrompt\n")
                            append("- 主题按原文出现顺序排列，不要按主题打乱重排\n\n")
                            append("输出纯 JSON（不要输出任何单元原文，只用编号区间引用）：\n")
                            append("{\"topics\":[{\"title\":\"主题名\",\"relevance\":\"high|medium|low\",\"unitRanges\":[[1,4],[7,9]]}]}\n")
                            append("unitRanges 是闭区间数组，表示该主题包含的单元编号范围；一个主题可包含多个不连续区间。")
                        },
                        userMessage = "请对以下带编号的学习材料单元进行主题归并：\n\n$unitListing",
                        temperature = 0.3,
                        modelOverride = preprocessOption.model,
                        provider = preprocessOption.provider
                    )
                }
                val preText = preprocessResult.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val preData = jsonParser.decodeFromString<PreprocessPlan>(preText)
                _uiState.update { it.copy(summaryProgress = "本地按区间组块 ${preData.topics.size} 个主题…", summaryGenerationPhase = "preprocess") }
                AiGenerationManager.updatePhase(AiTaskType.SUMMARY, logId, "本地按区间组块 ${preData.topics.size} 个主题…", progress = 0.22f, phaseKey = "preprocess")
                // 本地按区间把单元原文拼成块（原文只在这一遍被引用，AI 不再复制全文）
                val topics = preData.topics.map { pt ->
                    PreprocessTopic(pt.title, pt.relevance, assembleTopicContent(units, pt))
                }

                if (topics.isEmpty()) {
                    _uiState.update { it.copy(isGeneratingSummary = false, error = "预处理未能识别出主题，请尝试其他模式") }
                    return@launch
                }

                // Phase 2: Per-topic summaries
                val topicSummaries = mutableListOf<TopicSummary>()
                val parallel = apiPreferences.isMultiAgentParallel()
                _uiState.update { it.copy(summaryMultiAgentTotal = topics.size, summaryMultiAgentCurrent = 0, summaryGenerationPhase = "topic_summary", summaryMultiAgentStep = "主题总结") }
                AiGenerationManager.updatePhase(AiTaskType.SUMMARY, logId, "主题总结 0/${topics.size}", progress = 0.3f, phaseKey = "topic_summary", step = 0, stepTotal = topics.size)

                if (parallel) {
                    // Parallel execution
                    _uiState.update { it.copy(summaryProgress = "正在并行总结 ${topics.size} 个主题…") }
                    val results = coroutineScope {
                        topics.map { topic ->
                            async {
                                try {
                                    val summary = generateTopicSummary(topic, userMessage, agentOption)
                                    TopicSummary(topic.title, summary, true)
                                } catch (e: Exception) {
                                    TopicSummary(topic.title, "[总结失败: ${e.message}]", false)
                                }
                            }
                        }.map { deferred ->
                            val result = deferred.await()
                            _uiState.update { it.copy(summaryMultiAgentCurrent = it.summaryMultiAgentCurrent + 1) }
                            val done = _uiState.value.summaryMultiAgentCurrent
                            AiGenerationManager.updatePhase(AiTaskType.SUMMARY, logId, "主题总结 $done/${topics.size}", progress = 0.3f + 0.4f * done / topics.size, phaseKey = "topic_summary", step = done, stepTotal = topics.size)
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
                            val summary = run {
                                generateTopicSummary(topic, userMessage, agentOption)
                            }
                            topicSummaries.add(TopicSummary(topic.title, summary, true))
                        } catch (e: Exception) {
                            topicSummaries.add(TopicSummary(topic.title, "[总结失败: ${e.message}]", false))
                        }
                        _uiState.update { it.copy(summaryMultiAgentCurrent = idx + 1) }
                        AiGenerationManager.updatePhase(AiTaskType.SUMMARY, logId, "主题总结 ${idx + 1}/${topics.size}", progress = 0.3f + 0.4f * (idx + 1) / topics.size, phaseKey = "topic_summary", step = idx + 1, stepTotal = topics.size)
                    }
                }

                if (topicSummaries.isEmpty()) {
                    _uiState.update { it.copy(isGeneratingSummary = false, error = "所有主题总结均失败，请重试") }
                    return@launch
                }

                // Phase 3: Integration — stream final summary
                _uiState.update { it.copy(summaryProgress = "正在整合生成最终总结…", summaryGenerationPhase = "integrate", summaryMultiAgentStep = "整合生成") }
                AiGenerationManager.updatePhase(AiTaskType.SUMMARY, logId, "正在整合生成最终总结…", progress = 0.75f, phaseKey = "integrate")
                val topicsText = topicSummaries.joinToString("\n\n---\n\n") { ts ->
                    "【${ts.title}】\n${ts.summary}"
                }

                val fullSummary = StringBuilder()
                _uiState.update { it.copy(summaryText = "", summaryExpanded = true) }
                run {
                    deepSeekApi.chatCompletionStream(
                        systemPrompt = buildString {
                            append("你是一位学习总结专家。以下是对学习材料各主题的分段总结，请整合为一份完整、连贯的最终总结。\n\n")
                            append("要求：\n1. 消除重复内容\n2. 统一行文风格\n3. 补充跨主题的关联和逻辑关系\n4. 确保整体结构流畅\n")
                            append("5. 使用与 AI 总结一致的排版：Markdown 标题、列表、表格、**粗体**、*斜体*\n")
                            append("6. 使用 <span style=\"...\"> 标签：color、background-color、font-size、font-weight、font-style、text-decoration\n")
                            append("   三色：<span style=\"background-color:#90CAF9\">概念</span> <span style=\"background-color:#FFF176\">知识点</span> <span style=\"background-color:#EF9A9A\">易错点</span>\n")
                            append("7. 重要内容可用夸张样式：特大字号、特粗、醒目颜色\n")
                            append("只允许 <span>，禁止 <script>/<iframe>/<object>/<embed>/<form>/<input>。中文，条理清晰。\n")
                            if (entry.notebookName.isNotBlank()) append("分类：${entry.notebookName}\n")
                            append("标题：${entry.title}")
                        },
                        userMessage = "请整合以下各主题总结为一份完整总结：\n\n$topicsText",
                        temperature = 0.3,
                        modelOverride = integrateOption.model,
                        provider = integrateOption.provider
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
                _uiState.update { it.copy(isGeneratingSummary = false, summaryGenerationPhase = "idle", error = "总结失败: ${e.message}", summaryText = "生成失败：${e.message}") }
            }
        }
    }

    private suspend fun generateTopicSummary(topic: PreprocessTopic, userMessage: String, option: AiModelOption): String {
        return run {
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
                modelOverride = option.model,
                provider = option.provider
            )
        }
    }

    /**
     * 本地把全文切成带编号的候选单元（纯机械，不依赖段落/标题/来源）。
     * 规则：换行/句号族标点拆句 → 超长句按字符硬切（带重叠防断义）→ 相邻短句盲合控制单元总数。
     * 宁碎勿粗：AI 对单元的引用是原子性的，单元过粗会把不同主题锁死在同一单元内。
     */
    private fun splitIntoCandidateUnits(text: String): List<TextUnit> {
        val sentences = text
            .replace("\r", "\n")
            .split(Regex("(?<=[。！？；.!?;\\n])"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (sentences.isEmpty()) return emptyList()

        // 超长句按字符硬切，带重叠避免断义
        val pieces = mutableListOf<String>()
        for (s in sentences) {
            if (s.length <= UNIT_MAX_CHARS) { pieces.add(s); continue }
            var start = 0
            while (start < s.length) {
                val end = minOf(start + UNIT_MAX_CHARS, s.length)
                pieces.add(s.substring(start, end))
                if (end == s.length) break
                start = end - UNIT_OVERLAP
            }
        }

        // 相邻短句盲合（控制单元总数，不涉语义判断）
        val merged = mutableListOf<String>()
        val buffer = StringBuilder()
        for (p in pieces) {
            if (buffer.isNotEmpty() && buffer.length + p.length > UNIT_MAX_CHARS) {
                merged.add(buffer.toString())
                buffer.setLength(0)
            }
            if (buffer.isNotEmpty()) buffer.append('\n')
            buffer.append(p)
        }
        if (buffer.isNotEmpty()) merged.add(buffer.toString())

        return merged.mapIndexed { index, t -> TextUnit(index + 1, t.trim()) }
    }

    /** 按 unitRanges 把单元原文拼成块；越界编号静默忽略 */
    private fun assembleTopicContent(units: List<TextUnit>, topic: PlanTopic): String {
        val idSet = units.mapTo(HashSet()) { it.id }
        val selected = topic.unitRanges
            .flatMap { r -> if (r.size >= 2) (r[0]..r[1]).toList() else r }
            .filter { it in idSet }
            .distinct()
            .sorted()
        return units.filter { it.id in selected }.joinToString("\n") { it.text }
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
        AiGenerationManager.updatePhase(AiTaskType.FLASHCARDS, logId, "正在识别图片文字…", phaseKey = "ocr")
        AiGenerationManager.scope.launch {
            _uiState.update { it.copy(isGeneratingFlashcards = true, flashcardProgress = "正在识别图片文字…") }
            try {
                val ocrAll = performOcr(context, entry, AiTaskType.FLASHCARDS)
                if (ocrAll.isBlank() && entry.description.isBlank()) {
                    _uiState.update { it.copy(isGeneratingFlashcards = false, error = "未识别到任何文字") }
                    return@launch
                }
                _uiState.update { it.copy(flashcardProgress = "正在生成闪卡…") }
                AiGenerationManager.updatePhase(AiTaskType.FLASHCARDS, logId, "正在生成闪卡…", progress = 0.8f, phaseKey = "design")
                val userMessage = buildUserMessage(ocrAll, entry)
                val flashcardOption = AiModelOption.fromId(_uiState.value.flashcardModel)

                val response = run {
                    deepSeekApi.chatCompletion(
                        systemPrompt = apiPreferences.getPrompt(PromptKeys.FLASHCARD) ?: "你是一个学习助教，擅长将学习材料转化为闪卡。请根据提供的OCR识别文字、用户描述、AI总结内容和科目信息，生成一套闪卡。\n\n要求：\n1. 输出纯 JSON，不要任何前缀、后缀或 markdown 标记。直接输出 JSON 对象。\n2. 总共生成卡片的数量，视内容丰富度自行拟定，必须覆盖所有知识点。\n3. 视情况使用以下四种类型（并非每种类型都要出现，而是根据实际确定知识点对应的卡片类型）：\n   - 判断题 (true_false)：陈述一个知识点，判断正误\n   - 单选题 (single_choice)：4 个选项，1 个正确答案\n   - 多选题 (multi_choice)：多个选项、多个正确答案\n   - 记忆卡片 (memory)：正面为概念/术语，背面为详细解释\n4. JSON 格式如下：\n{\n  \"cards\": [\n    {\"type\":\"true_false\",\"question\":\"...\",\"answer\":true,\"explanation\":\"...\"},\n    {\"type\":\"single_choice\",\"question\":\"...\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"answer\":0,\"explanation\":\"...\"},\n    {\"type\":\"multi_choice\",\"question\":\"...\",\"options\":[\"A\",\"B\",\"C\",\"D\",\"E\"],\"answers\":[0,2],\"explanation\":\"...\"},\n    {\"type\":\"memory\",\"front\":\"概念名称\",\"back\":\"详细解释\",\"explanation\":\"扩展知识...\"}\n  ]\n}\n5. 题目覆盖所有知识点，选项应有干扰性但不过分相似。\n6. explanation 必须条理清晰，解释为什么对/错。",
                        userMessage = userMessage,
                        temperature = 0.3,
                        modelOverride = flashcardOption.model,
                        provider = flashcardOption.provider
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

    private suspend fun performOcr(context: Context, entry: LogEntry, taskType: AiTaskType = AiTaskType.SUMMARY): String {
        if (cachedOcrText != null) return cachedOcrText!!
        val paths = entry.imagePaths
        if (paths.isEmpty()) return ""
        return OcrManager.recognizeWithProgress(context, paths) { current, total ->
            _uiState.update { it.copy(summaryProgress = "正在识别图片文字 $current/$total…") }
            AiGenerationManager.updatePhase(taskType, logId, "正在识别图片文字 $current/$total…", progress = current.toFloat() / total, phaseKey = "ocr")
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
            AiGenerationManager.updatePhase(AiTaskType.SELF_CHECK, logId, "正在生成自检内容…", progress = 0.5f, phaseKey = "generate")
            _uiState.update { it.copy(isGeneratingSelfCheck = true) }
            try {
                val selfCheckOption = AiModelOption.fromId(_uiState.value.selfCheckModel)
                val response = run {
                    deepSeekApi.chatCompletion(
                        systemPrompt = apiPreferences.getPrompt(PromptKeys.SELF_CHECK) ?: SELF_CHECK_SYSTEM_PROMPT,
                        userMessage = "请为以下学习总结生成自检内容：\n\n$summary",
                        temperature = 0.3,
                        modelOverride = selfCheckOption.model,
                        provider = selfCheckOption.provider
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
        // 本地切分单元参数：宁碎勿粗，AI 按编号原子引用
        private const val UNIT_MAX_CHARS = 300
        private const val UNIT_OVERLAP = 40

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
你是一个学习助手，负责根据标题、分类和描述将学习内容整理成结构化的知识总结。

## 输入说明
- 「用户描述」可能包含课堂字幕片段，也可能包含用户对 AI 的直接指示（如"重点总结第二部分"），请识别并遵循其中的指示。
- 请根据标题、分类和描述，归纳该学习内容的核心知识点、相关概念和考点。

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
        if (entry.notebookName.isNotBlank()) append("分类：${entry.notebookName}\n")
        append("标题：${entry.title}\n")
    }
}

// Multi-agent preprocessing models

/** 本地切分出的候选单元：AI 归并时的唯一原子引用单位（不依赖段落/标题/来源） */
data class TextUnit(val id: Int, val text: String)

/** 预处理 AI 输出的主题归并方案（AI 只输出编号区间，不复制原文） */
@kotlinx.serialization.Serializable
data class PreprocessPlan(
    val topics: List<PlanTopic>
)

@kotlinx.serialization.Serializable
data class PlanTopic(
    val title: String,
    val relevance: String = "medium",
    /** 闭区间数组，可多个不连续区间，如 [[1,4],[7,9]] */
    val unitRanges: List<List<Int>> = emptyList()
)

/** 主题总结阶段的块结构（由本地按 unitRanges 拼装而来，content = 块原文） */
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
