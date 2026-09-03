package com.sunjk.sunjktool.feature.questionbank.detail

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.local.AiModelOption
import com.sunjk.sunjktool.data.local.ApiPreferences
import com.sunjk.sunjktool.data.local.PromptKeys
import com.sunjk.sunjktool.data.remote.DeepSeekApi
import com.sunjk.sunjktool.data.sync.SyncTrigger
import com.sunjk.sunjktool.di.AiGenerationManager
import com.sunjk.sunjktool.di.AiTaskStatus
import com.sunjk.sunjktool.di.AiTaskType
import com.sunjk.sunjktool.domain.model.LogEntry
import com.sunjk.sunjktool.domain.model.Question
import com.sunjk.sunjktool.domain.model.QuestionBankCategory
import com.sunjk.sunjktool.domain.model.SplitQuestionItem
import com.sunjk.sunjktool.domain.repository.LogRepository
import com.sunjk.sunjktool.domain.repository.NotebookRepository
import com.sunjk.sunjktool.domain.repository.QuestionBankRepository
import com.sunjk.sunjktool.util.MarkdownOutlineParser
import com.sunjk.sunjktool.util.MarkdownSection
import com.sunjk.sunjktool.util.SummaryLinkHelper
import com.sunjk.sunjktool.util.SummaryLinkRef
import com.sunjk.sunjktool.util.ocr.OcrManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

@Stable
data class BreadcrumbItem(
    val categoryId: Long,
    val name: String
)

@Immutable
data class QuestionBankDetailUiState(
    val category: QuestionBankCategory? = null,
    val subCategories: List<QuestionBankCategory> = emptyList(),
    val questions: List<Question> = emptyList(),
    val breadcrumbs: List<BreadcrumbItem> = emptyList(),
    val isLoading: Boolean = true,
    val deleteCategoryConfirm: Boolean = false,
    val deleteQuestionId: Long? = null,

    // Expand/collapse
    val expandedQuestionIds: Set<Long> = emptySet(),
    val globalExpandAll: Boolean = false,

    // New question form
    val isCreatingQuestion: Boolean = false,
    val newQuestionContent: String = "",
    val newQuestionImages: List<String> = emptyList(),
    val isGeneratingAnalysis: Boolean = false,
    val generationPhase: String = "idle",  // idle / ocr / split / retrieval / generation
    val generationProgress: String = "",
    val newQuestionError: String? = null,

    // Split review
    val splitQuestions: List<SplitQuestionItem> = emptyList(),
    val showSplitReview: Boolean = false,
    val editedSplitContent: Map<Int, String> = emptyMap(),
    val analysisStyleHint: String = "",  // 用户自定义解析风格

    // Generated analyses
    val generatedAnalyses: Map<Int, String> = emptyMap(),
    val isSavingQuestions: Boolean = false,

    // 题集一键直达：开启后生成进度改用页面进度卡片，而非生成解析对话框
    val questionBankAutoSave: Boolean = false,
    val aiModel: String = AiModelOption.DEEPSEEK_FLASH.id
)

// JSON models for AI responses

@Serializable
data class SplitQuestionsResult(
    val questions: List<SplitQuestionJson>
)

@Serializable
data class SplitQuestionJson(
    val index: Int,
    val content: String
)

class QuestionBankDetailViewModel(
    private val repository: QuestionBankRepository,
    private val deepSeekApi: DeepSeekApi,
    private val logRepository: LogRepository,
    private val notebookRepository: NotebookRepository,
    private val apiPreferences: ApiPreferences,
    private val categoryId: Long,
    private val initialQuestionId: Long? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestionBankDetailUiState())
    val uiState: StateFlow<QuestionBankDetailUiState> = _uiState.asStateFlow()

    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    /** ViewModel 是否仍存活；后台任务据此决定完成后是进预览还是直接入库 */
    private var alive = true

    init {
        loadData()
        observeGenerationTask()
    }

    override fun onCleared() {
        alive = false
        super.onCleared()
    }

    /** 监听应用级生成任务，进入页面时恢复正在进行的解析生成进度 */
    private fun observeGenerationTask() {
        viewModelScope.launch {
            AiGenerationManager.tasks.collect { tasks ->
                val task = tasks.firstOrNull {
                    it.taskId == AiGenerationManager.taskIdFor(AiTaskType.QUESTION_BANK, categoryId)
                }
                if (task != null && task.status == AiTaskStatus.RUNNING && !_uiState.value.isGeneratingAnalysis) {
                    _uiState.update {
                        it.copy(
                            isGeneratingAnalysis = true,
                            generationPhase = task.phaseKey.ifBlank { "generation" },
                            generationProgress = task.phase,
                            questionBankAutoSave = apiPreferences.isQuestionBankAutoSave()
                        )
                    }
                }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(questionBankAutoSave = apiPreferences.isQuestionBankAutoSave()) }
            launch {
                repository.getCategoryById(categoryId).collect { category ->
                    _uiState.update { it.copy(category = category, isLoading = false) }
                }
            }
            launch {
                repository.getChildCategories(categoryId).collect { children ->
                    _uiState.update { it.copy(subCategories = children) }
                }
            }
            launch {
                repository.getQuestionsByCategoryId(categoryId).collect { questions ->
                    val initial = initialQuestionId
                    val expanded = if (initial != null && questions.any { it.id == initial })
                        setOf(initial) else emptySet()
                    _uiState.update { it.copy(questions = questions, expandedQuestionIds = expanded) }
                }
            }
            launch {
                val crumbs = repository.getBreadcrumbs(categoryId)
                    .map { BreadcrumbItem(it.first, it.second) }
                _uiState.update { it.copy(breadcrumbs = crumbs) }
            }
        }
    }

    // ── Expand/Collapse ─────────────────────────────────────────────

    fun toggleQuestion(id: Long) {
        _uiState.update { state ->
            val newSet = if (id in state.expandedQuestionIds)
                state.expandedQuestionIds - id
            else
                state.expandedQuestionIds + id
            state.copy(expandedQuestionIds = newSet)
        }
    }

    fun toggleExpandAll() {
        _uiState.update { it.copy(globalExpandAll = !it.globalExpandAll, expandedQuestionIds = emptySet()) }
    }

    // ── Delete ──────────────────────────────────────────────────────

    fun requestDeleteCategory() {
        _uiState.update { it.copy(deleteCategoryConfirm = true) }
    }

    fun dismissDeleteCategory() {
        _uiState.update { it.copy(deleteCategoryConfirm = false) }
    }

    fun confirmDeleteCategory() {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
            _uiState.update { it.copy(deleteCategoryConfirm = false) }
            SyncTrigger.bumpEntity("question_bank_categories")
            SyncTrigger.requestAutoSync()
        }
    }

    fun requestDeleteQuestion(id: Long) {
        _uiState.update { it.copy(deleteQuestionId = id) }
    }

    fun dismissDeleteQuestion() {
        _uiState.update { it.copy(deleteQuestionId = null) }
    }

    fun confirmDeleteQuestion() {
        val id = _uiState.value.deleteQuestionId ?: return
        viewModelScope.launch {
            repository.deleteQuestion(id)
            _uiState.update { it.copy(deleteQuestionId = null) }
            SyncTrigger.bumpEntity("questions")
            SyncTrigger.requestAutoSync()
        }
    }

    // ── New Question Form ───────────────────────────────────────────

    fun showCreateQuestionForm() {
        _uiState.update { it.copy(isCreatingQuestion = true, aiModel = apiPreferences.getAiModelFor("questionbank")) }
    }

    fun setAiModel(option: AiModelOption) {
        apiPreferences.setAiModelFor("questionbank", option.id)
        _uiState.update { it.copy(aiModel = option.id) }
    }

    fun dismissCreateQuestionForm() {
        _uiState.update {
            it.copy(
                isCreatingQuestion = false,
                newQuestionContent = "",
                newQuestionImages = emptyList(),
                newQuestionError = null,
                splitQuestions = emptyList(),
                showSplitReview = false,
                generatedAnalyses = emptyMap(),
                analysisStyleHint = ""
            )
        }
    }

    fun updateNewQuestionContent(text: String) {
        _uiState.update { it.copy(newQuestionContent = text, newQuestionError = null) }
    }

    fun addNewQuestionImages(paths: List<String>) {
        _uiState.update { it.copy(newQuestionImages = it.newQuestionImages + paths) }
    }

    fun removeNewQuestionImage(index: Int) {
        _uiState.update {
            val newList = it.newQuestionImages.toMutableList().apply { removeAt(index) }
            it.copy(newQuestionImages = newList)
        }
    }

    fun updateSplitContent(index: Int, content: String) {
        _uiState.update {
            val newMap = it.editedSplitContent.toMutableMap().apply { put(index, content) }
            it.copy(editedSplitContent = newMap)
        }
    }

    fun removeSplitQuestion(index: Int) {
        _uiState.update {
            val newList = it.splitQuestions.filter { q -> q.index != index }
            it.copy(splitQuestions = newList)
        }
    }

    fun updateAnalysisStyleHint(hint: String) {
        _uiState.update { it.copy(analysisStyleHint = hint) }
    }

    // ── AI Generation Pipeline ──────────────────────────────────────

    fun startGeneration(context: Context) {
        val state = _uiState.value
        if (state.newQuestionContent.isBlank() && state.newQuestionImages.isEmpty()) {
            _uiState.update { it.copy(newQuestionError = "请输入题目内容或上传图片") }
            return
        }
        val taskTitle = state.category?.name?.let { "题集解析·$it" } ?: "题集解析"
        AiGenerationManager.start(AiTaskType.QUESTION_BANK, categoryId, taskTitle)
        AiGenerationManager.updatePhase(AiTaskType.QUESTION_BANK, categoryId, "正在识别图片文字…", progress = 0.1f, phaseKey = "ocr")
        AiGenerationManager.scope.launch {
            _uiState.update {
                it.copy(
                    isGeneratingAnalysis = true,
                    generationProgress = "正在识别图片文字…",
                    generationPhase = "ocr",
                    newQuestionError = null,
                    questionBankAutoSave = apiPreferences.isQuestionBankAutoSave()
                )
            }
            try {
                // Phase 1: OCR —— 用 applicationContext，后台任务在页面退出后仍安全
                val ocrText = if (state.newQuestionImages.isNotEmpty()) {
                    OcrManager.recognize(context.applicationContext, state.newQuestionImages)
                } else ""

                // Combine user text + OCR text
                val fullText = buildString {
                    if (state.newQuestionContent.isNotBlank()) {
                        append(state.newQuestionContent)
                        if (ocrText.isNotBlank()) append("\n\n")
                    }
                    if (ocrText.isNotBlank()) append(ocrText)
                }

                if (fullText.isBlank()) {
                    _uiState.update { it.copy(isGeneratingAnalysis = false, newQuestionError = "未能识别到任何文字内容") }
                    return@launch
                }

                // Phase 2: Split questions
                _uiState.update { it.copy(generationProgress = "正在识别和拆题…", generationPhase = "split") }
                AiGenerationManager.updatePhase(AiTaskType.QUESTION_BANK, categoryId, "正在识别和拆题…", progress = 0.2f, phaseKey = "split")
                val splitResult = run {
                    deepSeekApi.chatCompletion(
                        systemPrompt = apiPreferences.getPrompt(PromptKeys.QUESTION_SPLIT) ?: "你是一位题目识别专家。用户提供了一段文本，其中可能包含多道题目。请识别出每道独立的题目，并将它们拆分出来。\n\n规则：\n1. 按题号（如 1. / ① / (1) / 一、等）、空行分隔、语义边界来识别题目\n2. 合并跨页或跨段的同一道题\n3. 忽略非题目的杂文（如页码、水印、无关说明）\n4. 保留每道题的完整题干文本\n5. 如果文本中只有一道题，也正常拆分\n\n输出纯 JSON，格式：{\"questions\":[{\"index\":0,\"content\":\"题干内容…\"},{\"index\":1,\"content\":\"题干内容…\"}]}",
                        userMessage = "请识别并拆分以下文本中的题目：\n\n$fullText",
                        temperature = 0.3,
                        modelOverride = AiModelOption.fromId(_uiState.value.aiModel).model,
                        provider = AiModelOption.fromId(_uiState.value.aiModel).provider
                    )
                }
                val splitText = splitResult.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val splitParsed = jsonParser.decodeFromString<SplitQuestionsResult>(splitText)
                val splitItems = splitParsed.questions.map { SplitQuestionItem(it.index, it.content) }

                if (splitItems.isEmpty()) {
                    AiGenerationManager.fail(AiTaskType.QUESTION_BANK, categoryId, "未能识别到题目，请检查输入内容")
                    _uiState.update { it.copy(isGeneratingAnalysis = false, newQuestionError = "未能识别到题目，请检查输入内容") }
                    return@launch
                }

                _uiState.update { it.copy(splitQuestions = splitItems) }

                if (apiPreferences.isQuestionBankAutoSave()) {
                    // 一键直达：保持生成状态，跳过拆分确认和预览，直接进入解析生成（后台续跑）
                    generateAnalyses(context)
                } else {
                    // 非自动保存：拆分完成停住等待用户确认，结束本轮后台任务
                    AiGenerationManager.remove(AiTaskType.QUESTION_BANK, categoryId)
                    _uiState.update {
                        it.copy(
                            isGeneratingAnalysis = false,
                            generationPhase = "idle",
                            generationProgress = "",
                            showSplitReview = true
                        )
                    }
                }

            } catch (e: Exception) {
                val phaseDesc = when (_uiState.value.generationPhase) {
                    "ocr" -> "图片识别"
                    "split" -> "题目拆分"
                    else -> "处理"
                }
                AiGenerationManager.fail(AiTaskType.QUESTION_BANK, categoryId, "$phaseDesc 失败: ${e.message}")
                _uiState.update { it.copy(isGeneratingAnalysis = false, generationPhase = "idle", newQuestionError = "$phaseDesc 失败: ${e.message}") }
            }
        }
    }

    fun generateAnalyses(context: Context) {
        val state = _uiState.value
        if (state.splitQuestions.isEmpty()) return

        // 捕获全部所需数据：任务在应用级作用域执行，页面退出后仍继续
        val finalQuestions = state.splitQuestions.map { item ->
            val editedContent = state.editedSplitContent[item.index]
            SplitQuestionItem(item.index, editedContent ?: item.content)
        }
        val imagePaths = state.newQuestionImages
        val styleHint = state.analysisStyleHint
        val autoSave = apiPreferences.isQuestionBankAutoSave()
        val taskTitle = state.category?.name?.let { "题集解析·$it" } ?: "题集解析"

        _uiState.update { it.copy(isGeneratingAnalysis = true, generationProgress = "正在检索相关知识…", generationPhase = "retrieval", showSplitReview = false, newQuestionError = null) }
        // 自动保存场景由 startGeneration 启动任务，此处复用同一任务保持阶段连续；手动触发时新建
        if (AiGenerationManager.task(AiTaskType.QUESTION_BANK, categoryId) == null) {
            AiGenerationManager.start(AiTaskType.QUESTION_BANK, categoryId, taskTitle)
        }
        AiGenerationManager.updatePhase(AiTaskType.QUESTION_BANK, categoryId, "正在检索相关知识…", progress = 0.25f, phaseKey = "retrieval")

        AiGenerationManager.scope.launch {
            try {
                // ── Phase 3: 知识检索（路径匹配 + 章节切分）──
                _uiState.update { it.copy(generationProgress = "正在分析学习记录大纲…", generationPhase = "retrieval") }
                AiGenerationManager.updatePhase(AiTaskType.QUESTION_BANK, categoryId, "正在分析学习记录大纲…", progress = 0.3f, phaseKey = "retrieval")

                val breadcrumbs = repository.getBreadcrumbs(categoryId)
                val categoryPath = breadcrumbs.joinToString(" / ") { it.second }

                val allEntries = logRepository.getAllEntries().first()
                val matchedEntries = if (categoryPath.isNotBlank()) {
                    val notebooks = notebookRepository.getAll().first()
                    val pathById = buildMap<Long, String> {
                        for (nb in notebooks) {
                            var cur = nb
                            val names = ArrayDeque<String>()
                            var guard = 0
                            while (guard++ < 64) {
                                names.addFirst(cur.name)
                                val pid = cur.parentId ?: break
                                cur = notebooks.find { it.id == pid } ?: break
                            }
                            put(nb.id, names.joinToString(" / "))
                        }
                    }
                    allEntries.filter { entry ->
                        val nbPath = entry.notebookId?.let { pathById[it] } ?: return@filter false
                        (nbPath == categoryPath || nbPath.startsWith("$categoryPath / ")) &&
                            entry.aiSummary.isNotBlank()
                    }
                } else emptyList()

                // 路径无匹配时回退为最近 20 条，保证知识背景不缺席
                val contextEntries = if (matchedEntries.isNotEmpty()) matchedEntries
                else allEntries.filter { it.aiSummary.isNotBlank() }.take(20)

                // 将每篇 AI 总结按标题切分为章节（标题 + 正文），仅保留有标题的章节
                val entrySections = contextEntries.mapNotNull { entry ->
                    val sections = MarkdownOutlineParser.splitSections(entry.aiSummary)
                        .filter { it.heading != null && it.body.isNotBlank() }
                    if (sections.isEmpty()) null else entry to sections
                }

                // ── Phase 4a: 大纲选择轮（一次请求，AI 为每题挑选相关章节）──
                val selections = selectSectionsForQuestions(entrySections, finalQuestions, categoryPath)

                // 方案A：把全部被选中章节的并集作为公共知识背景前缀。
                // 所有题共享同一份 systemPrompt（仅 user 段变化）→ 命中 DeepSeek 上下文硬盘缓存，
                // 第 2 题起的输入费用降至缓存命中价（约为未命中的 1/30~1/50）。
                // 每个章节（任意标题层级，可能是二级/三级标题）分配一个编号 [ref:N]，
                // AI 引用某知识点时插入 [[ref:N]]，程序再替换为精确指向该标题的内部链接。
                val commonRefs = selections.values.flatten()
                    .distinctBy { (entry, section) -> "${entry.id}_${section.heading?.headingId}" }
                val commonLinkRefs = mutableListOf<SummaryLinkRef>()
                val commonContext = buildString {
                    commonRefs.forEach { (entry, section) ->
                        val h = section.heading ?: return@forEach
                        val idx = commonLinkRefs.size
                        commonLinkRefs.add(SummaryLinkRef(entry.id, h.headingId, h.title))
                        if (idx > 0) append("\n\n")
                        append("[ref:$idx] 学习记录「${entry.title}」（${entry.notebookName}）·《${h.title}》：\n${section.body}")
                    }
                }
                val commonSystemPrompt = buildAnalysisSystemPrompt(categoryPath, matchedEntries.isNotEmpty(), commonContext)

                // ── Phase 4b: 逐题独立请求生成解析 + knowledgePoint ──
                val analyses = mutableMapOf<Int, String>()
                val failedIndices = mutableListOf<Int>()
                val total = finalQuestions.size

                finalQuestions.forEachIndexed { i, item ->
                    val progressText = "正在生成解析（第 ${i + 1}/$total 题）…"
                    if (alive) {
                        _uiState.update { it.copy(generationProgress = progressText, generationPhase = "generation") }
                    }
                    AiGenerationManager.updatePhase(
                        AiTaskType.QUESTION_BANK, categoryId, progressText,
                        progress = (i + 1).toFloat() / total,
                        phaseKey = "generation", step = i + 1, stepTotal = total
                    )
                    try {
                        // 本题参考章节指引：放在 user 段，不影响 system 前缀的缓存命中
                        val refGuide = selections[item.index].orEmpty()
                            .distinctBy { (entry, section) -> "${entry.id}_${section.heading?.title}" }
                            .takeIf { it.isNotEmpty() }
                            ?.joinToString("；") { (entry, section) ->
                                "「${section.heading?.title}」（${entry.title}）"
                            }
                        val response = run {
                            deepSeekApi.chatCompletion(
                                systemPrompt = commonSystemPrompt,
                                userMessage = buildString {
                                    append("请为以下题目生成解析：\n\n第${item.index + 1}题：${item.content}")
                                    if (categoryPath.isNotBlank()) {
                                        append("\n\n所属分类：$categoryPath")
                                    }
                                    refGuide?.let {
                                        append("\n\n【本题参考章节】请优先参考以上知识背景中的：$it")
                                    }
                                    if (styleHint.isNotBlank()) {
                                        append("\n\n【用户解析偏好】请按以下风格偏好组织解析内容：$styleHint")
                                    }
                                },
                                temperature = 0.3,
                                modelOverride = AiModelOption.fromId(_uiState.value.aiModel).model,
                                provider = AiModelOption.fromId(_uiState.value.aiModel).provider
                            )
                        }
                        val parsed = parseTwoSectionCards(response, 1)[0]
                        if (parsed.isNullOrBlank()) failedIndices.add(item.index)
                        else analyses[item.index] = SummaryLinkHelper.replaceRefMarkers(parsed, commonLinkRefs)
                    } catch (e: Exception) {
                        android.util.Log.w("QuestionBank", "解析生成失败（第${item.index + 1}题）: ${e.message}")
                        failedIndices.add(item.index)
                    }
                }

                if (analyses.isEmpty()) {
                    if (alive) {
                        if (autoSave) {
                            // 一键直达：失败时回到输入表单重试，不进入拆分确认
                            _uiState.update {
                                it.copy(
                                    isGeneratingAnalysis = false, generationPhase = "idle",
                                    newQuestionError = "解析生成失败，请重试"
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isGeneratingAnalysis = false, generationPhase = "idle",
                                    showSplitReview = true,
                                    newQuestionError = "解析生成失败：AI 返回格式异常，请重试"
                                )
                            }
                        }
                    }
                    AiGenerationManager.fail(AiTaskType.QUESTION_BANK, categoryId, "解析生成失败，请重试")
                    return@launch
                }

                AiGenerationManager.complete(AiTaskType.QUESTION_BANK, categoryId)

                val failedCount = failedIndices.size
                val completedCount = total - failedCount
                if (alive && !autoSave) {
                    // 前台预览，让用户确认后再保存
                    _uiState.update {
                        it.copy(
                            isGeneratingAnalysis = false, generationPhase = "idle",
                            generationProgress = "",
                            generatedAnalyses = analyses,
                            splitQuestions = finalQuestions,
                            newQuestionError = if (failedCount > 0)
                                "有 $failedCount 道题生成失败，已跳过" else null
                        )
                    }
                } else {
                    // 自动保存，或页面已退出 → 直接入库
                    persistAnalyses(finalQuestions, imagePaths, analyses)
                    if (alive) {
                        _uiState.update {
                            it.copy(
                                isGeneratingAnalysis = false, generationPhase = "idle",
                                generationProgress = "",
                                generatedAnalyses = analyses,
                                splitQuestions = finalQuestions,
                                isCreatingQuestion = false,
                                newQuestionContent = "",
                                newQuestionImages = emptyList(),
                                showSplitReview = false,
                                newQuestionError = if (failedCount > 0)
                                    "已完成 $completedCount/${total} 道，$failedCount 道生成失败" else null
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                android.util.Log.w("QuestionBank", "解析生成任务失败", e)
                AiGenerationManager.fail(AiTaskType.QUESTION_BANK, categoryId, "解析生成失败: ${e.message}")
                if (alive) {
                    _uiState.update { it.copy(isGeneratingAnalysis = false, generationPhase = "idle", newQuestionError = "解析生成失败: ${e.message}") }
                }
            }
        }
    }

    // ── 生成流程辅助 ──────────────────────────────────────────────

    /** 构建单题解析的 system prompt：knowledgePoint 规则 + 该题选中的章节正文 */
    private fun buildAnalysisSystemPrompt(
        categoryPath: String,
        hasPathMatch: Boolean,
        kpContext: String
    ): String = buildString {
        append("你是一位学习助教，擅长对各类题目进行深入解析。\n\n")
        append("## 输出格式\n")
        append("题目分两段，用标记分隔：\n\n")
        append("<<<CARD_0>>>\n")
        append("{\"type\":\"single_choice\",\"options\":[\"A. 选项A\",\"B. 选项B\",\"C. 选项C\",\"D. 选项D\"],\"answer\":0,\"knowledgePoint\":\"知识点\"}\n")
        append("<<<BODY_0>>>\n")
        append("（解析正文，自由组织）\n\n")
        append("JSON 字段：type(single_choice/multi_choice/true_false/open)、options(选项数组，open 题为[])、answer(单选填索引数字，多选填 [0,2]，判断填 true/false，open 题填 -1)、knowledgePoint(核心考点，见下方「knowledgePoint 填写规则」)\n\n")
        append("## 排版\n")
        append("自由使用 Markdown（标题、列表、表格、粗体斜体等）和 HTML <span style=\"...\"> 标签（color、background-color、font-size、font-weight、font-style、text-decoration），可叠加使用。\n")
        append("三种底色按语义使用：<span style=\"background-color:#90CAF9\">#90CAF9 概念术语</span>、<span style=\"background-color:#FFF176\">#FFF176 知识点</span>、<span style=\"background-color:#EF9A9A\">#EF9A9A 易错点</span>\n")
        append("重要内容可用夸张样式突出：特大字号、特粗、醒目颜色等。\n")
        append("只允许 <span>，禁止 <script>/<iframe>/<object>/<embed>/<form>/<input>。\n")
        append("中文，条理清晰，深入透彻。\n\n")
        append("## 规则\n")
        append("- <<<CARD_0>>> 和 <<<BODY_0>>> 独占一行\n")
        append("- JSON 紧跟 <<<CARD_0>>>（可跳过空行）\n")
        append("- 正文紧跟 <<<BODY_0>>>，到文末为止\n")
        if (categoryPath.isNotBlank()) {
            append("\n题目所属分类：「$categoryPath」，请结合该领域专业知识。\n")
        }
        if (kpContext.isNotBlank()) {
            append("\n## knowledgePoint 填写规则\n")
            append("knowledgePoint 填写本题的「题型/考点」，而不是题目谈论的「题材/话题」。两者区别：题材是题目的内容主题（如教育方法、经济建设），考点是解这道题所用的方法论（如提问方式、结构类型、公式定理）。\n")
            append("判定依据是下方「学习记录知识背景」中沉淀的知识点体系——先判断本题属于哪类题型/考点，再用该体系中的术语命名 knowledgePoint，保持与学习记录中的叫法一致。\n")
            append("示例：一道片段阅读题，材料讲的是「教育方法」（题材），但根据学习记录中的知识点可知该题属于「后对策类说理结构」（题型），则 knowledgePoint 填「后对策类说理结构」，绝不填「教育方法」。\n")
            append("仅当学习记录知识背景中找不到可对应的考点，且题目本身无法归类到任何方法论时，才退而填写最核心的知识概念。3-10 字。\n")
            append("\n## 引用链接规则\n")
            append("解析正文中，当实际使用了下方「学习记录知识背景」中某条章节的方法、知识点或结论时，在该处插入链接标记 [[ref:N]]（N 为该章节编号，见背景中每条开头的 [ref:N]）。\n")
            append("标记应紧跟在使用该内容的知识点句末，不要单独成行。可引用多条章节；编号务必使用背景中真实出现的 [ref:N]，不要编造。\n")
            append("仅当确实引用了该章节内容时才插入；背景未涵盖或与本题无关时不要插入。\n")
            append("\n相关知识背景（来自笔记本「${if (hasPathMatch) categoryPath else "全部（未找到路径匹配的笔记本）"}」的学习记录）：\n$kpContext\n")
        }
    }

    /**
     * 大纲选择轮：把每篇学习记录 AI 总结的标题大纲（不含正文）连同全部题目发给 AI，
     * 返回每道题需要参考的记录章节。不同题目可以选不同的记录章节。
     */
    private suspend fun selectSectionsForQuestions(
        entrySections: List<Pair<LogEntry, List<MarkdownSection>>>,
        finalQuestions: List<SplitQuestionItem>,
        categoryPath: String
    ): Map<Int, List<Pair<LogEntry, MarkdownSection>>> {
        if (entrySections.isEmpty() || finalQuestions.isEmpty()) return emptyMap()

        val outlineText = buildString {
            entrySections.forEachIndexed { idx, (entry, sections) ->
                append("记录[e=$idx]「${entry.title}」（${entry.notebookName}）：\n")
                sections.forEachIndexed { si, section ->
                    append("  ${si + 1}. ${section.heading?.title}\n")
                }
                append("\n")
            }
        }
        val questionsText = finalQuestions.joinToString("\n\n") { "第${it.index + 1}题：${it.content}" }

        val response = try {
            run {
                deepSeekApi.chatCompletion(
                    systemPrompt = buildString {
                        append("你是检索助手。以下是从笔记本「${if (categoryPath.isNotBlank()) categoryPath else "学习记录"}」中匹配到的学习记录 AI 总结的标题大纲（不含正文）。\n\n")
                        append(outlineText)
                        append("\n任务：为每道题挑选与其最相关的记录章节，这些章节的正文将作为该题解析的知识背景。不同题目可能需要不同记录的章节，请逐题独立判断。\n")
                        append("输出严格 JSON，格式：{\"selections\":[{\"q\":0,\"refs\":[{\"e\":0,\"s\":[1,2]}]}]}\n")
                        append("q=题号（从 0 开始）、e=记录编号、s=章节编号数组（从 1 开始）。可一题选多条记录多个章节；某题无需知识背景时 refs 为 []。只输出 JSON，不要解释。")
                    },
                    userMessage = "题目如下：\n\n$questionsText",
                    temperature = 0.1,
                    modelOverride = AiModelOption.fromId(_uiState.value.aiModel).model,
                    provider = AiModelOption.fromId(_uiState.value.aiModel).provider
                )
            }
        } catch (e: Exception) {
            android.util.Log.w("QuestionBank", "大纲选择失败，回退为全部章节: ${e.message}")
            null
        }

        if (response == null) {
            // 失败兜底：所有题都参考全部章节（等价于旧行为）
            val all = entrySections.flatMap { (entry, sections) -> sections.map { entry to it } }
            return finalQuestions.associate { it.index to all }
        }

        return try {
            val cleaned = response
                .substringAfter("{", missingDelimiterValue = response)
                .let { it.substring(0, it.lastIndexOf("}") + 1) }
            val parsed = jsonParser.decodeFromString<SectionSelectionResult>(cleaned)
            buildMap {
                for (sel in parsed.selections) {
                    val refs = sel.refs.flatMap { ref ->
                        val entry = entrySections.getOrNull(ref.e) ?: return@flatMap emptyList()
                        ref.s.mapNotNull { idx -> entry.second.getOrNull(idx - 1)?.let { entry.first to it } }
                    }
                    put(sel.q, refs)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("QuestionBank", "大纲选择解析失败，回退为全部章节: ${e.message}")
            val all = entrySections.flatMap { (entry, sections) -> sections.map { entry to it } }
            finalQuestions.associate { it.index to all }
        }
    }

    /** 直接入库（自动保存 / 页面已退出时的后台完成路径） */
    private suspend fun persistAnalyses(
        finalQuestions: List<SplitQuestionItem>,
        imagePaths: List<String>,
        analyses: Map<Int, String>
    ) {
        val now = LocalDateTime.now()
        val questionList = finalQuestions.map { item ->
            Question(
                categoryId = categoryId,
                content = item.content,
                imagePaths = imagePaths,
                aiAnalysis = analyses[item.index] ?: "",
                createdDate = now,
                updatedDate = now
            )
        }
        repository.saveQuestions(questionList)
        SyncTrigger.bumpEntity("questions")
        SyncTrigger.requestAutoSync()
    }

    fun saveQuestions() {
        val state = _uiState.value
        val questions = state.splitQuestions
        val analyses = state.generatedAnalyses
        if (questions.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingQuestions = true) }
            try {
                val now = LocalDateTime.now()
                val imagePaths = state.newQuestionImages
                val questionList = questions.map { item ->
                    Question(
                        categoryId = categoryId,
                        content = state.editedSplitContent[item.index] ?: item.content,
                        imagePaths = imagePaths,
                        aiAnalysis = analyses[item.index] ?: "",
                        createdDate = now,
                        updatedDate = now
                    )
                }
                repository.saveQuestions(questionList)
                _uiState.update {
                    it.copy(
                        isSavingQuestions = false,
                        isCreatingQuestion = false,
                        newQuestionContent = "",
                        newQuestionImages = emptyList(),
                        splitQuestions = emptyList(),
                        showSplitReview = false,
                        generatedAnalyses = emptyMap(),
                        editedSplitContent = emptyMap(),
                        newQuestionError = null
                    )
                }
                SyncTrigger.bumpEntity("questions")
                SyncTrigger.requestAutoSync()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSavingQuestions = false, newQuestionError = "保存失败: ${e.message}") }
            }
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    /**
     * Parse two-section format:
     *   <<<CARD_N>>>
     *   {json metadata}
     *   <<<BODY_N>>>
     *   markdown explanation (until next <<<CARD_ or EOF)
     */
    private fun parseTwoSectionCards(response: String, expectedCount: Int): Map<Int, String> {
        val result = mutableMapOf<Int, String>()

        // Match CARD markers with optional whitespace: <<<CARD_0>>> or <<< CARD_0 >>>
        val cardMarkerRegex = Regex("""<<<\s*CARD_(\d+)\s*>>>""")
        val cardMatches = cardMarkerRegex.findAll(response).toList()

        if (cardMatches.isEmpty()) {
            android.util.Log.w("QuestionBank", "No CARD markers found, trying fallback parsers. Response starts: ${response.take(200)}")
            return parseStructuredCards(response, expectedCount)
        }

        android.util.Log.d("QuestionBank", "Found ${cardMatches.size} CARD markers")

        for (i in cardMatches.indices) {
            val cardIdx = cardMatches[i].groupValues[1].toIntOrNull() ?: continue

            // Find first non-empty line after CARD marker that starts with '{'
            var pos = cardMatches[i].range.last + 1
            var metaJson = ""
            while (pos < response.length) {
                val lineEnd = response.indexOf('\n', pos)
                val line = response.substring(pos, if (lineEnd >= 0) lineEnd else response.length).trim()
                pos = if (lineEnd >= 0) lineEnd + 1 else response.length
                if (line.isEmpty()) continue
                if (line.startsWith("{")) {
                    metaJson = line
                    break
                }
                // Not a JSON line — stop scanning
                android.util.Log.w("QuestionBank", "CARD $cardIdx: expected JSON after marker, got: ${line.take(80)}")
                break
            }
            if (metaJson.isEmpty()) {
                android.util.Log.w("QuestionBank", "CARD $cardIdx: no JSON line found after marker")
                continue
            }

            // Find matching BODY marker (lenient: allow whitespace)
            val bodyMarkerRegex = Regex("""<<<\s*BODY_${cardIdx}\s*>>>""")
            val bodyMatch = bodyMarkerRegex.find(response, pos)
            val bodyStart = if (bodyMatch != null) bodyMatch.range.last + 1 else pos
            // Body ends at next CARD marker or EOF
            val nextCard = cardMarkerRegex.find(response, bodyStart)
            val bodyEnd = nextCard?.range?.first ?: response.length
            val body = response.substring(bodyStart, bodyEnd).trim()

            if (body.isEmpty()) {
                android.util.Log.w("QuestionBank", "CARD $cardIdx: body is empty")
            }

            try {
                val meta = jsonParser.decodeFromString<CardMeta>(metaJson)
                val fullCard = StructuredCard(
                    index = cardIdx,
                    type = meta.type,
                    options = meta.options,
                    answer = meta.answer,
                    knowledgePoint = meta.knowledgePoint,
                    explanation = body
                )
                val fullJson = jsonParser.encodeToString(StructuredCard.serializer(), fullCard)
                result[cardIdx] = fullJson
            } catch (e: Exception) {
                android.util.Log.w("QuestionBank", "CARD $cardIdx: JSON parse failed: ${e.message}. Using body as-is.")
                result[cardIdx] = body
            }
        }

        android.util.Log.d("QuestionBank", "Parsed ${result.size} cards out of ${cardMatches.size} markers")
        return result
    }

    /** Fallback: old JSON-line + body format */
    private fun parseStructuredCards(response: String, expectedCount: Int): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        val cardHeaderRegex = Regex("""^\{"index":(\d+),""", RegexOption.MULTILINE)
        val headers = cardHeaderRegex.findAll(response).toList()
        if (headers.isEmpty()) return parseDelimitedAnalyses(response, expectedCount)

        for (i in headers.indices) {
            val headerStart = headers[i].range.first
            val newlineIndex = response.indexOf('\n', headerStart)
            val headerLine = response.substring(headerStart, if (newlineIndex >= 0) newlineIndex else response.length).trim()
            val bodyStart = if (newlineIndex >= 0) newlineIndex + 1 else response.length
            val bodyEnd = if (i + 1 < headers.size) headers[i + 1].range.first else response.length
            val body = response.substring(bodyStart, bodyEnd).trim()
            try {
                val meta = jsonParser.decodeFromString<CardMeta>(headerLine)
                val fullCard = StructuredCard(i, meta.type, meta.options, meta.answer, meta.knowledgePoint, body)
                result[i] = jsonParser.encodeToString(StructuredCard.serializer(), fullCard)
            } catch (_: Exception) {
                result[i.coerceAtMost(expectedCount - 1)] = body
            }
        }
        return result
    }

    /** Fallback: old ---ANALYSIS_N--- delimiter format */
    private fun parseDelimitedAnalyses(response: String, expectedCount: Int): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        val delimiterRegex = Regex("---ANALYSIS_(\\d+)---")
        val parts = response.split(delimiterRegex)
        val indices = delimiterRegex.findAll(response).map { it.groupValues[1].toIntOrNull() ?: -1 }.toList()
        if (indices.isNotEmpty() && parts.size >= indices.size + 1) {
            for (i in indices.indices) {
                val idx = indices[i]
                val content = parts.getOrElse(i + 1) { "" }.trim()
                if (idx >= 0 && content.isNotBlank()) result[idx] = content
            }
        }
        if (result.isNotEmpty()) return result
        val cleaned = response.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return try {
            jsonParser.decodeFromString<AnalysisResult>(cleaned).analyses.associate { it.index to it.analysis }
        } catch (_: Exception) {
            if (expectedCount == 1) mapOf(0 to cleaned) else emptyMap()
        }
    }
}

// ── Structured card models ─────────────────────────────────────────

@Serializable
data class CardMeta(
    val type: String = "open",
    val options: List<String> = emptyList(),
    val answer: kotlinx.serialization.json.JsonElement? = null,
    val knowledgePoint: String = ""
)

@Serializable
data class StructuredCard(
    val index: Int,
    val type: String = "open",
    val options: List<String> = emptyList(),
    val answer: kotlinx.serialization.json.JsonElement? = null,
    val knowledgePoint: String = "",
    val explanation: String = ""
)

@Serializable
private data class AnalysisResult(val analyses: List<AnalysisItem>)

@Serializable
private data class AnalysisItem(val index: Int, val analysis: String)

// ── 大纲选择轮 JSON 模型 ───────────────────────────────────────────

@Serializable
private data class SectionSelectionResult(
    val selections: List<QuestionSectionSelection> = emptyList()
)

@Serializable
private data class QuestionSectionSelection(
    val q: Int,
    val refs: List<SectionRef> = emptyList()
)

@Serializable
private data class SectionRef(
    val e: Int,
    val s: List<Int> = emptyList()
)
