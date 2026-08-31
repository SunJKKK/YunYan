package com.sunjk.sunjktool.feature.learninglog.flashcard

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.model.FlashcardSetJson
import com.sunjk.sunjktool.data.local.ApiPreferences
import com.sunjk.sunjktool.data.local.PromptKeys
import com.sunjk.sunjktool.data.remote.DeepSeekApi
import com.sunjk.sunjktool.data.sync.SyncTrigger
import com.sunjk.sunjktool.di.AiGenerationManager
import com.sunjk.sunjktool.di.AiTaskStatus
import com.sunjk.sunjktool.di.AiTaskType
import com.sunjk.sunjktool.domain.model.AnswerRecord
import com.sunjk.sunjktool.domain.model.Flashcard
import com.sunjk.sunjktool.domain.model.FlashcardSession
import com.sunjk.sunjktool.domain.model.LogEntry
import com.sunjk.sunjktool.domain.model.toDomain
import com.sunjk.sunjktool.domain.repository.FlashcardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json

data class SessionSummary(
    val id: Long,
    val cardCount: Int,
    val correctCount: Int,
    val answeredCount: Int,
    val isComplete: Boolean,
    val style: String = "",
    val createdDate: Long,
    val index: Int = 0
)

data class WrongAnswer(
    val sessionId: Long,
    val sessionIndex: Int,
    val cardIndex: Int,
    val card: Flashcard
)

@Immutable
data class FlashcardHubUiState(
    val isLoading: Boolean = true,
    val sessions: List<FlashcardSession> = emptyList(),
    val error: String? = null,
    val navigateToSessionId: Long? = null,
    val deleteConfirmId: Long? = null,
    val previewSession: FlashcardSession? = null,
    val isGenerating: Boolean = false,
    val generationProgress: String = "",
    val generationPhase: String = "idle",  // idle / ocr / gap / retrieval / design
    val showStyleDialog: Boolean = false,
    val customStyle: String = "",
    val customCardCount: String = "",
    val useCustomCount: Boolean = false,
    val enableTrueFalse: Boolean = true,
    val enableSingleChoice: Boolean = true,
    val enableMultiChoice: Boolean = true,
    val enableMemory: Boolean = true,
    val includeOcr: Boolean = true,
    val includeDescription: Boolean = true,
    val includeAiSummary: Boolean = true
)

class FlashcardHubViewModel(
    private val repository: FlashcardRepository,
    private val deepSeekApi: DeepSeekApi,
    private val logRepository: com.sunjk.sunjktool.domain.repository.LogRepository,
    private val apiPreferences: ApiPreferences,
    private val logEntryId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashcardHubUiState())
    val uiState: StateFlow<FlashcardHubUiState> = _uiState.asStateFlow()

    private var cachedOcrText: String? = null

    init {
        viewModelScope.launch {
            repository.getAllSessions(logEntryId).collect { sessions ->
                _uiState.update { it.copy(sessions = sessions, isLoading = false) }
            }
        }
        viewModelScope.launch {
            AiGenerationManager.tasks.collect { tasks ->
                val task = tasks.firstOrNull { it.taskId == AiGenerationManager.taskIdFor(AiTaskType.FLASHCARDS, logEntryId) }
                _uiState.update {
                    it.copy(
                        isGenerating = task?.status == AiTaskStatus.RUNNING,
                        generationProgress = task?.phase ?: "",
                        generationPhase = if (task?.status == AiTaskStatus.RUNNING) task.phase else it.generationPhase
                    )
                }
            }
        }
    }

    val sessionSummaries: List<SessionSummary>
        get() {
            val total = _uiState.value.sessions.size
            return _uiState.value.sessions.mapIndexed { idx, s ->
                val answered = s.answers.size
                SessionSummary(
                    id = s.id,
                    cardCount = s.cards.size,
                    correctCount = s.answers.values.count { it.isCorrect },
                    answeredCount = answered,
                    isComplete = answered == s.cards.size,
                    style = s.style.ifBlank { "默认" },
                    createdDate = s.createdDate,
                    index = total - idx  // oldest=1, newest=N
                )
            }
        }

    val wrongAnswers: List<WrongAnswer>
        get() {
            val total = _uiState.value.sessions.size
            val result = mutableListOf<WrongAnswer>()
            _uiState.value.sessions.forEachIndexed { sIdx, session ->
                session.answers.forEach { (cardIdx, ans) ->
                    if (!ans.isCorrect) {
                        val card = session.cards.getOrNull(cardIdx) ?: return@forEach
                        if (card is Flashcard.Memory) return@forEach
                        result.add(WrongAnswer(session.id, total - sIdx, cardIdx, card))
                    }
                }
            }
            return result
        }

    fun showStyleDialog() {
        _uiState.update { it.copy(showStyleDialog = true, customStyle = "", customCardCount = "", useCustomCount = false) }
    }

    fun dismissStyleDialog() {
        _uiState.update { it.copy(showStyleDialog = false) }
    }

    fun updateCustomStyle(style: String) {
        _uiState.update { it.copy(customStyle = style) }
    }

    fun updateCustomCardCount(count: String) {
        _uiState.update { it.copy(customCardCount = count) }
    }

    fun setUseCustomCount(use: Boolean) {
        _uiState.update { it.copy(useCustomCount = use) }
    }

    fun toggleTrueFalse() { _uiState.update { it.copy(enableTrueFalse = !it.enableTrueFalse) } }
    fun toggleSingleChoice() { _uiState.update { it.copy(enableSingleChoice = !it.enableSingleChoice) } }
    fun toggleMultiChoice() { _uiState.update { it.copy(enableMultiChoice = !it.enableMultiChoice) } }
    fun toggleMemory() { _uiState.update { it.copy(enableMemory = !it.enableMemory) } }
    fun toggleIncludeOcr() { _uiState.update { it.copy(includeOcr = !it.includeOcr) } }
    fun toggleIncludeDescription() { _uiState.update { it.copy(includeDescription = !it.includeDescription) } }
    fun toggleIncludeAiSummary() { _uiState.update { it.copy(includeAiSummary = !it.includeAiSummary) } }

    fun generateFlashcards(context: Context, style: String, cardCount: Int = 0) {
        AiGenerationManager.start(AiTaskType.FLASHCARDS, logEntryId, "AI 闪卡")
        AiGenerationManager.updatePhase(AiTaskType.FLASHCARDS, logEntryId, "正在识别图片文字…")
        AiGenerationManager.scope.launch {
            _uiState.update { it.copy(showStyleDialog = false, isGenerating = true, generationProgress = "正在识别图片文字…", generationPhase = "ocr") }
            try {
                val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }
                val entry = logRepository.getAllEntries().first().find { it.id == logEntryId } ?: return@launch
                val ocrAll = if (uiState.value.includeOcr) performOcr(context, entry) else ""
                val userMessage = buildUserMessage(
                    ocrAll, entry,
                    includeDescription = uiState.value.includeDescription,
                    includeAiSummary = uiState.value.includeAiSummary
                )

                val countInstruction = if (cardCount > 0) "总共生成恰好 $cardCount 张卡片。" else "卡片数量由你根据内容量自行决定，不要自行限制数量——如果内容知识点多，就多出题，要全面覆盖所有知识点。宁可多一些也不要遗漏。"

                val isRetrievalAugmented = style.trim() == "检索增强"

                // Retrieval-augmented: 3-stage pipeline
                val supplementaryContext: String
                if (isRetrievalAugmented) {
                    // Stage 1: Gap analysis
                    _uiState.update { it.copy(generationProgress = "正在分析知识缺口…", generationPhase = "gap") }
                    val gapResult = withTimeout(300000) {
                        deepSeekApi.chatCompletion(
                            systemPrompt = apiPreferences.getPrompt(PromptKeys.GAP_ANALYSIS) ?: "你是学习分析专家。仔细分析用户的学习材料，找出其中的知识缺口和可以深入扩展的方向。\n\n输出纯 JSON，格式：{\"gaps\":[{\"topic\":\"知识点名\",\"description\":\"缺口说明\",\"importance\":\"high|medium|low\"}],\"extensions\":[{\"topic\":\"知识点名\",\"direction\":\"可扩展方向\"}],\"missingDetails\":[\"缺失的细节1\",\"缺失的细节2\"]}\n\n分析要点：\n1. 是否有前置知识未覆盖？\n2. 是否有重要的关联概念未提及？\n3. 是否有容易混淆的相似概念需要区分？\n4. 是否可以深入挖掘某个知识点的原理或应用？\n5. 是否有实际案例或练习题可以补充？",
                            userMessage = "请分析以下学习材料中的知识缺口：\n\n$userMessage",
                            temperature = 0.4
                        )
                    }
                    val gapText = gapResult.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                    val gapAnalysis = jsonParser.decodeFromString<com.sunjk.sunjktool.data.model.GapAnalysisResult>(gapText)

                    // Stage 2: Knowledge retrieval (using model internal knowledge)
                    _uiState.update { it.copy(generationProgress = "正在检索补充知识…", generationPhase = "retrieval") }
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
                    supplementaryContext = buildString {
                        append("\n\n--- 补充知识（基于缺口分析检索）---\n")
                        knowledgeSupplement.supplements.forEach { s ->
                            append("【${s.topic}】${s.content}\n")
                            if (s.keyPoints.isNotEmpty()) {
                                append("要点：${s.keyPoints.joinToString("；")}\n")
                            }
                        }
                    }
                } else {
                    supplementaryContext = ""
                }

                // Stage 3: Flashcard design (or single-stage for non-retrieval styles)
                _uiState.update { it.copy(generationProgress = "正在设计闪卡…", generationPhase = "design") }

                val styleInstruction = when (style.trim()) {
                    "核心" -> "聚焦于材料中最核心、最重要的知识点，每道题都应直接考察关键概念。"
                    "易错" -> "聚焦于容易混淆、容易出错的细节知识点，设计具有迷惑性的干扰项。"
                    "混淆" -> "设计容易混淆的相似概念对比题，考察辨别相似知识点的能力。"
                    "详解" -> "每道题考查一个知识点极其全面的解释，可包含原理、派生、关联等深度解析。"
                    "拓展" -> "在材料知识点基础上向外延伸，引入同类型的相关知识点，帮助知识体系扩展。"
                    "检索增强" -> "核心目标是填补学习材料中的知识空白。请结合缺口分析和补充知识，设计能够弥补这些空白的高质量闪卡。题目应既覆盖原始材料的核心知识点，也覆盖补充知识中引入的新内容，形成完整的知识体系。"
                    else -> "生成风格要求：$style"
                }
                val retrievalInstruction = if (isRetrievalAugmented) "请结合上述补充知识，在闪卡中覆盖原材料的核心知识和补充的新知识点，确保知识体系的完整性。" else ""

                val response = withTimeout(300000) {
                    deepSeekApi.chatCompletion(
                        systemPrompt = apiPreferences.getPrompt(PromptKeys.FLASHCARD) ?: "你是一个学习助教，擅长将学习材料转化为有趣的闪卡。请根据提供的OCR识别文字、用户描述和科目信息，生成一套闪卡。\n\n要求：\n1. 输出纯 JSON，不要任何前缀、后缀或 markdown 标记。直接输出 JSON 对象。\n2. ${countInstruction}\n3. ${buildTypeInstruction(_uiState.value.enableTrueFalse, _uiState.value.enableSingleChoice, _uiState.value.enableMultiChoice, _uiState.value.enableMemory)}\n4. JSON 格式：{\"cards\":[{\"type\":\"true_false\",\"question\":\"...\",\"answer\":true,\"explanation\":\"...\",\"knowledgePoint\":\"知识点\"},{\"type\":\"single_choice\",\"question\":\"...\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"answer\":0,\"explanation\":\"...\",\"knowledgePoint\":\"知识点\"},{\"type\":\"multi_choice\",\"question\":\"...\",\"options\":[\"A\",\"B\",\"C\",\"D\",\"E\"],\"answers\":[0,2],\"explanation\":\"...\",\"knowledgePoint\":\"知识点名\"},{\"type\":\"memory\",\"front\":\"...\",\"back\":\"...\",\"explanation\":\"...\",\"knowledgePoint\":\"知识点名\"}]}\n5. 题目覆盖核心知识点，选项应有干扰性但不过分相似，explanation 必须条理清晰。\n6. 每张卡片必须包含 \"knowledgePoint\" 字段，值为该题考查的核心知识点名称（3-8个字，如\"协程取消\"\"Flow冷热流\"）。\n7. ${styleInstruction}\n8. ${retrievalInstruction}",
                        userMessage = userMessage + supplementaryContext,
                        temperature = 0.3
                    )
                }
                val jsonText = response.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val set = jsonParser.decodeFromString<FlashcardSetJson>(jsonText)
                val cards: List<Flashcard> = set.cards.map { it.toDomain() }
                val sessionId = repository.saveSession(logEntryId, cards, style.trim())
                SyncTrigger.requestAutoSync()
                SyncTrigger.bumpEntity("flashcard_sessions")
                AiGenerationManager.complete(AiTaskType.FLASHCARDS, logEntryId)
                _uiState.update { it.copy(isGenerating = false, generationProgress = "", generationPhase = "idle", navigateToSessionId = sessionId) }
            } catch (e: Exception) {
                AiGenerationManager.fail(AiTaskType.FLASHCARDS, logEntryId, "闪卡生成失败: ${e.message}")
                val phase = _uiState.value.generationPhase
                val phaseDesc = when (phase) {
                    "ocr" -> "图片识别"
                    "gap" -> "缺口分析"
                    "retrieval" -> "知识检索"
                    "design" -> "闪卡设计"
                    else -> "生成"
                }
                _uiState.update { it.copy(isGenerating = false, generationProgress = "", generationPhase = "idle", error = "$phaseDesc 失败: ${e.message}") }
            }
        }
    }

    fun clearNavigation() {
        _uiState.update { it.copy(navigateToSessionId = null, deleteConfirmId = null) }
    }

    fun onSessionClick(sessionId: Long) {
        _uiState.update { it.copy(navigateToSessionId = sessionId) }
    }

    fun showPreview(sessionId: Long) {
        val session = _uiState.value.sessions.find { it.id == sessionId }
        _uiState.update { it.copy(previewSession = session) }
    }

    fun dismissPreview() {
        _uiState.update { it.copy(previewSession = null) }
    }

    fun requestDelete(sessionId: Long) {
        _uiState.update { it.copy(deleteConfirmId = sessionId) }
    }

    fun confirmDelete() {
        val id = _uiState.value.deleteConfirmId ?: return
        viewModelScope.launch {
            repository.deleteSession(id)
            SyncTrigger.requestAutoSync()
            SyncTrigger.bumpEntity("flashcard_sessions")
            _uiState.update { it.copy(deleteConfirmId = null) }
        }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(deleteConfirmId = null) }
    }

    private suspend fun performOcr(context: Context, entry: LogEntry): String {
        if (cachedOcrText != null) return cachedOcrText!!
        return com.sunjk.sunjktool.util.ocr.OcrManager.recognize(context, entry.imagePaths)
            .also { cachedOcrText = it }
    }

    private fun buildTypeInstruction(
        enableTrueFalse: Boolean, enableSingleChoice: Boolean,
        enableMultiChoice: Boolean, enableMemory: Boolean
    ): String {
        val types = buildList {
            if (enableTrueFalse) add("判断题")
            if (enableSingleChoice) add("单选题")
            if (enableMultiChoice) add("多选题")
            if (enableMemory) add("记忆卡片")
        }
        if (types.isEmpty()) return "使用判断题、单选题、多选题、记忆卡片四种类型。"
        return "仅使用以下题型：${types.joinToString("、")}。每种题型根据内容需要灵活出现，并非每种都必须出现。"
    }

    private fun buildUserMessage(
        ocrAll: String, entry: LogEntry,
        includeDescription: Boolean = true,
        includeAiSummary: Boolean = true
    ): String = buildString {
        if (ocrAll.isNotBlank()) {
            append("OCR识别内容：\n")
            append(ocrAll)
            append("\n\n")
        }
        if (includeDescription && entry.description.isNotBlank())
            append("用户描述：${entry.description}\n")
        if (entry.subject.isNotBlank()) append("科目：${entry.subject}\n")
        if (includeAiSummary && entry.aiSummary.isNotBlank())
            append("AI总结：${entry.aiSummary}\n")
        append("标题：${entry.title}\n")
    }
}
