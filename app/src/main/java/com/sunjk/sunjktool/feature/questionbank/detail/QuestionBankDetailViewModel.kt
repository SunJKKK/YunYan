package com.sunjk.sunjktool.feature.questionbank.detail

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.local.ApiPreferences
import com.sunjk.sunjktool.data.local.PromptKeys
import com.sunjk.sunjktool.data.remote.DeepSeekApi
import com.sunjk.sunjktool.data.sync.SyncTrigger
import com.sunjk.sunjktool.domain.model.Question
import com.sunjk.sunjktool.domain.model.QuestionBankCategory
import com.sunjk.sunjktool.domain.model.SplitQuestionItem
import com.sunjk.sunjktool.domain.repository.LogRepository
import com.sunjk.sunjktool.domain.repository.QuestionBankRepository
import com.sunjk.sunjktool.util.ocr.OcrManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
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
    val isSavingQuestions: Boolean = false
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
    private val apiPreferences: ApiPreferences,
    private val categoryId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestionBankDetailUiState())
    val uiState: StateFlow<QuestionBankDetailUiState> = _uiState.asStateFlow()

    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
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
                    _uiState.update { it.copy(questions = questions) }
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
        _uiState.update { it.copy(isCreatingQuestion = true) }
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
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingAnalysis = true, generationProgress = "正在识别图片文字…", generationPhase = "ocr", newQuestionError = null) }
            try {
                // Phase 1: OCR
                val ocrText = if (state.newQuestionImages.isNotEmpty()) {
                    OcrManager.recognize(context, state.newQuestionImages)
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
                val splitResult = withTimeout(300000) {
                    deepSeekApi.chatCompletion(
                        systemPrompt = apiPreferences.getPrompt(PromptKeys.QUESTION_SPLIT) ?: "你是一位题目识别专家。用户提供了一段文本，其中可能包含多道题目。请识别出每道独立的题目，并将它们拆分出来。\n\n规则：\n1. 按题号（如 1. / ① / (1) / 一、等）、空行分隔、语义边界来识别题目\n2. 合并跨页或跨段的同一道题\n3. 忽略非题目的杂文（如页码、水印、无关说明）\n4. 保留每道题的完整题干文本\n5. 如果文本中只有一道题，也正常拆分\n\n输出纯 JSON，格式：{\"questions\":[{\"index\":0,\"content\":\"题干内容…\"},{\"index\":1,\"content\":\"题干内容…\"}]}",
                        userMessage = "请识别并拆分以下文本中的题目：\n\n$fullText",
                        temperature = 0.3
                    )
                }
                val splitText = splitResult.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val splitParsed = jsonParser.decodeFromString<SplitQuestionsResult>(splitText)
                val splitItems = splitParsed.questions.map { SplitQuestionItem(it.index, it.content) }

                if (splitItems.isEmpty()) {
                    _uiState.update { it.copy(isGeneratingAnalysis = false, newQuestionError = "未能识别到题目，请检查输入内容") }
                    return@launch
                }

                _uiState.update { it.copy(splitQuestions = splitItems, isGeneratingAnalysis = false, generationPhase = "idle", generationProgress = "") }

                if (apiPreferences.isQuestionBankAutoSave()) {
                    // Skip split review and analysis preview, go straight to save
                    generateAnalyses(context)
                } else {
                    _uiState.update { it.copy(showSplitReview = true) }
                }

            } catch (e: Exception) {
                val phaseDesc = when (_uiState.value.generationPhase) {
                    "ocr" -> "图片识别"
                    "split" -> "题目拆分"
                    else -> "处理"
                }
                _uiState.update { it.copy(isGeneratingAnalysis = false, generationPhase = "idle", newQuestionError = "$phaseDesc 失败: ${e.message}") }
            }
        }
    }

    fun generateAnalyses(context: Context) {
        val state = _uiState.value
        if (state.splitQuestions.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingAnalysis = true, generationProgress = "正在检索相关知识…", generationPhase = "retrieval", showSplitReview = false, newQuestionError = null) }
            try {
                // Phase 3: Knowledge retrieval
                val allEntries = logRepository.getAllEntries().first()
                val summaryContext = allEntries
                    .filter { it.aiSummary.isNotBlank() }
                    .take(20)
                    .joinToString("\n\n---\n\n") { "学习记录「${it.title}」（${it.subject}）：\n${it.aiSummary}" }

                val breadcrumbs = repository.getBreadcrumbs(categoryId)
                val categoryPath = breadcrumbs.joinToString(" / ") { it.second }

                // Build the full text from split questions (with any user edits)
                val finalQuestions = state.splitQuestions.map { item ->
                    val editedContent = state.editedSplitContent[item.index]
                    SplitQuestionItem(item.index, editedContent ?: item.content)
                }

                val questionsText = finalQuestions.joinToString("\n\n") { "第${it.index + 1}题：${it.content}" }

                // Phase 4: Batch generate structured analyses
                _uiState.update { it.copy(generationProgress = "正在生成解析（共${finalQuestions.size}题）…", generationPhase = "generation") }

                val analysisResponse = withTimeout(300000) {
                    deepSeekApi.chatCompletion(
                        systemPrompt = buildString {
                            append("你是一位学习助教，擅长对各类题目进行深入解析。\n\n")
                            append("## 输出格式\n")
                            append("每道题分两段，用标记分隔：\n\n")
                            append("<<<CARD_0>>>\n")
                            append("{\"type\":\"single_choice\",\"options\":[\"A. 选项A\",\"B. 选项B\",\"C. 选项C\",\"D. 选项D\"],\"answer\":0,\"knowledgePoint\":\"知识点\"}\n")
                            append("<<<BODY_0>>>\n")
                            append("（解析正文，自由组织）\n")
                            append("<<<CARD_1>>>\n")
                            append("...\n\n")
                            append("JSON 字段：type(single_choice/multi_choice/true_false/open)、options(选项数组，open 题为[])、answer(单选填索引数字，多选填 [0,2]，判断填 true/false，open 题填 -1)、knowledgePoint(核心知识点3-10字)\n\n")
                            append("## 排版\n")
                            append("自由使用 Markdown（标题、列表、表格、粗体斜体等）和 HTML <span style=\"...\"> 标签（color、background-color、font-size、font-weight、font-style、text-decoration），可叠加使用。\n")
                            append("三种底色按语义使用：<span style=\"background-color:#90CAF9\">#90CAF9 概念术语</span>、<span style=\"background-color:#FFF176\">#FFF176 知识点</span>、<span style=\"background-color:#EF9A9A\">#EF9A9A 易错点</span>\n")
                            append("重要内容可用夸张样式突出：特大字号、特粗、醒目颜色等。\n")
                            append("只允许 <span>，禁止 <script>/<iframe>/<object>/<embed>/<form>/<input>。\n")
                            append("中文，条理清晰，深入透彻。\n\n")
                            append("## 规则\n")
                            append("- <<<CARD_N>>> 和 <<<BODY_N>>> 独占一行，N 从 0 递增\n")
                            append("- JSON 紧跟 <<<CARD_N>>>（可跳过空行）\n")
                            append("- 正文紧跟 <<<BODY_N>>>，到下一个 <<<CARD_ 或文末为止\n")
                            if (categoryPath.isNotBlank()) {
                                append("\n题目所属分类：「$categoryPath」，请结合该领域专业知识。\n")
                            }
                            if (summaryContext.isNotBlank()) {
                                append("相关知识背景（可参考）：\n$summaryContext\n")
                            }
                        },
                        userMessage = buildString {
                            append("请为以下题目生成解析：\n\n$questionsText")
                            if (categoryPath.isNotBlank()) {
                                append("\n\n所属分类：$categoryPath")
                            }
                            val hint = state.analysisStyleHint.trim()
                            if (hint.isNotBlank()) {
                                append("\n\n【用户解析偏好】请按以下风格偏好组织解析内容：$hint")
                            }
                        },
                        temperature = 0.3
                    )
                }

                // Parse two-section card response
                val analysesMap = parseTwoSectionCards(analysisResponse, finalQuestions.size)

                if (analysesMap.isEmpty() && finalQuestions.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            isGeneratingAnalysis = false,
                            generationPhase = "idle",
                            showSplitReview = true,
                            newQuestionError = "解析生成失败：AI 返回格式异常，请重试"
                        )
                    }
                    return@launch
                }

                if (apiPreferences.isQuestionBankAutoSave()) {
                    // Auto-save: skip preview, save directly
                    _uiState.update {
                        it.copy(
                            isGeneratingAnalysis = false,
                            generationPhase = "idle",
                            generationProgress = "",
                            generatedAnalyses = analysesMap,
                            splitQuestions = finalQuestions
                        )
                    }
                    saveQuestions()
                } else {
                    _uiState.update {
                        it.copy(
                            isGeneratingAnalysis = false,
                            generationPhase = "idle",
                            generationProgress = "",
                            generatedAnalyses = analysesMap,
                            splitQuestions = finalQuestions
                        )
                    }
                }

            } catch (e: Exception) {
                val phaseDesc = when (_uiState.value.generationPhase) {
                    "retrieval" -> "知识检索"
                    "generation" -> "解析生成"
                    else -> "生成"
                }
                _uiState.update { it.copy(isGeneratingAnalysis = false, generationPhase = "idle", newQuestionError = "$phaseDesc 失败: ${e.message}") }
            }
        }
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
