package com.sunjk.sunjktool.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow


object PromptKeys {
    const val SELF_CHECK = "self_check"
    const val FLASHCARD = "flashcard"
    const val GAP_ANALYSIS = "gap_analysis"
    const val KNOWLEDGE_RETRIEVAL = "knowledge_retrieval"
    const val QUESTION_SPLIT = "question_split"
    const val QUESTION_ANALYSIS = "question_analysis"
}
class ApiPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---- DeepSeek ----
    fun getDeepSeekKey(): String = prefs.getString(KEY_DEEPSEEK, "") ?: ""
    fun setDeepSeekKey(key: String) = prefs.edit().putString(KEY_DEEPSEEK, key).apply()

    /** Returns the DeepSeek model id used for AI features: [MODEL_V4_PRO] or [MODEL_V4_FLASH] */
    fun getDeepSeekModel(): String = prefs.getString(KEY_DEEPSEEK_MODEL, MODEL_V4_FLASH) ?: MODEL_V4_FLASH
    fun setDeepSeekModel(model: String) = prefs.edit().putString(KEY_DEEPSEEK_MODEL, model).apply()

    /** Returns the DeepSeek API base URL (e.g. https://api.deepseek.com ). */
    fun getDeepSeekBaseUrl(): String = prefs.getString(KEY_DEEPSEEK_BASE_URL, DEFAULT_DEEPSEEK_BASE_URL) ?: DEFAULT_DEEPSEEK_BASE_URL
    fun setDeepSeekBaseUrl(url: String) = prefs.edit().putString(KEY_DEEPSEEK_BASE_URL, url.trim().trimEnd('/')).apply()


    // ---- Tablet / Read-only ----
    fun isTabletMode(): Boolean = prefs.getBoolean(KEY_TABLET_MODE, false)
    fun setTabletMode(enabled: Boolean) = prefs.edit().putBoolean(KEY_TABLET_MODE, enabled).apply()

    fun isReadOnlySync(): Boolean = prefs.getBoolean(KEY_READ_ONLY_SYNC, false)
    fun setReadOnlySync(enabled: Boolean) = prefs.edit().putBoolean(KEY_READ_ONLY_SYNC, enabled).apply()

    fun isSummaryOutlineExpanded(): Boolean = prefs.getBoolean(KEY_SUMMARY_OUTLINE_EXPANDED, true)
    fun setSummaryOutlineExpanded(expanded: Boolean) = prefs.edit().putBoolean(KEY_SUMMARY_OUTLINE_EXPANDED, expanded).apply()

    fun isSummaryOutlineOnLeft(): Boolean = prefs.getBoolean(KEY_SUMMARY_OUTLINE_ON_LEFT, false)
    fun setSummaryOutlineOnLeft(onLeft: Boolean) = prefs.edit().putBoolean(KEY_SUMMARY_OUTLINE_ON_LEFT, onLeft).apply()

    // ---- AI Prompt Overrides ----
    fun getPrompt(key: String): String? = prefs.getString("prompt_$key", null)
    fun setPrompt(key: String, value: String) = prefs.edit().putString("prompt_$key", value).apply()
    fun resetPrompt(key: String) = prefs.edit().remove("prompt_$key").apply()
    fun resetAllPrompts() {
        prefs.edit()
            .remove("prompt_${PromptKeys.SELF_CHECK}")
            .remove("prompt_${PromptKeys.FLASHCARD}")
            .remove("prompt_${PromptKeys.GAP_ANALYSIS}")
            .remove("prompt_${PromptKeys.KNOWLEDGE_RETRIEVAL}")
            .remove("prompt_${PromptKeys.QUESTION_SPLIT}")
            .remove("prompt_${PromptKeys.QUESTION_ANALYSIS}")
            .apply()
    }

    // ---- Qwen (通义千问) ----
    fun getQwenKey(): String = prefs.getString(KEY_QWEN, "") ?: ""
    fun setQwenKey(key: String) = prefs.edit().putString(KEY_QWEN, key).apply()

    fun getQwenBaseUrl(): String = prefs.getString(KEY_QWEN_BASE_URL, DEFAULT_QWEN_BASE_URL) ?: DEFAULT_QWEN_BASE_URL
    fun setQwenBaseUrl(url: String) = prefs.edit().putString(KEY_QWEN_BASE_URL, url.trim().trimEnd('/')).apply()

    fun getQwenModel(): String = prefs.getString(KEY_QWEN_MODEL, MODEL_QWEN_FLASH) ?: MODEL_QWEN_FLASH
    fun setQwenModel(model: String) = prefs.edit().putString(KEY_QWEN_MODEL, model).apply()

    // ---- 各 AI 功能所选模型（存 AiModelOption.id）----
    fun getAiModelFor(feature: String): String =
        prefs.getString("ai_model_$feature", AiModelOption.DEEPSEEK_FLASH.id) ?: AiModelOption.DEEPSEEK_FLASH.id
    fun setAiModelFor(feature: String, optionId: String) =
        prefs.edit().putString("ai_model_$feature", optionId).apply()

    // ---- 全局默认 AI 模型（各功能执行前选择的初始默认值）----
    fun getDefaultAiModelOption(): String =
        prefs.getString(KEY_DEFAULT_AI_MODEL, AiModelOption.DEEPSEEK_FLASH.id) ?: AiModelOption.DEEPSEEK_FLASH.id
    fun setDefaultAiModelOption(optionId: String) =
        prefs.edit().putString(KEY_DEFAULT_AI_MODEL, optionId).apply()

    // ---- QWeather ----
    fun getQWeatherKey(): String = prefs.getString(KEY_QWEATHER, "") ?: ""
    fun setQWeatherKey(key: String) = prefs.edit().putString(KEY_QWEATHER, key).apply()

    // ---- TickTick (滴答清单) ----
    fun getTickTickToken(): String = prefs.getString(KEY_TICKTICK_TOKEN, "") ?: ""
    fun setTickTickToken(token: String) = prefs.edit().putString(KEY_TICKTICK_TOKEN, token).apply()

    fun getTickTickCsrfToken(): String = prefs.getString(KEY_TICKTICK_CSRF, "") ?: ""
    fun setTickTickCsrfToken(csrf: String) = prefs.edit().putString(KEY_TICKTICK_CSRF, csrf).apply()

    fun getTickTickCompletedMode(): String = prefs.getString(KEY_TICKTICK_COMPLETED_MODE, COMPLETED_MODE_ALL) ?: COMPLETED_MODE_ALL
    fun setTickTickCompletedMode(mode: String) = prefs.edit().putString(KEY_TICKTICK_COMPLETED_MODE, mode).apply()

    fun getTickTickUsername(): String = prefs.getString(KEY_TICKTICK_USERNAME, "") ?: ""
    fun setTickTickUsername(username: String) = prefs.edit().putString(KEY_TICKTICK_USERNAME, username).apply()

    fun getTickTickPassword(): String = prefs.getString(KEY_TICKTICK_PASSWORD, "") ?: ""
    fun setTickTickPassword(password: String) = prefs.edit().putString(KEY_TICKTICK_PASSWORD, password).apply()

    // ---- Animation ----
    fun isAnimationEnabled(): Boolean = prefs.getBoolean(KEY_ANIMATION_ENABLED, true)
    fun setAnimationEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_ANIMATION_ENABLED, enabled).apply()

    // ---- Question Bank Auto Save ----
    fun isQuestionBankAutoSave(): Boolean = prefs.getBoolean(KEY_QUESTION_BANK_AUTO_SAVE, false)
    fun setQuestionBankAutoSave(enabled: Boolean) = prefs.edit().putBoolean(KEY_QUESTION_BANK_AUTO_SAVE, enabled).apply()

    // ---- Multi-Agent Summary ----
    fun isMultiAgentParallel(): Boolean = prefs.getBoolean(KEY_MULTI_AGENT_PARALLEL, false)
    fun setMultiAgentParallel(enabled: Boolean) = prefs.edit().putBoolean(KEY_MULTI_AGENT_PARALLEL, enabled).apply()

    fun getSummaryPreprocessModel(): String = prefs.getString(KEY_SUMMARY_PREPROCESS_MODEL, MODEL_V4_FLASH) ?: MODEL_V4_FLASH
    fun setSummaryPreprocessModel(model: String) = prefs.edit().putString(KEY_SUMMARY_PREPROCESS_MODEL, model).apply()

    fun getSummaryAgentModel(): String = prefs.getString(KEY_SUMMARY_AGENT_MODEL, MODEL_V4_PRO) ?: MODEL_V4_PRO
    fun setSummaryAgentModel(model: String) = prefs.edit().putString(KEY_SUMMARY_AGENT_MODEL, model).apply()

    fun getSummaryIntegrateModel(): String = prefs.getString(KEY_SUMMARY_INTEGRATE_MODEL, MODEL_V4_PRO) ?: MODEL_V4_PRO
    fun setSummaryIntegrateModel(model: String) = prefs.edit().putString(KEY_SUMMARY_INTEGRATE_MODEL, model).apply()

    // ---- Onboarding（首次启动引导） ----
    fun isOnboarded(): Boolean = prefs.getBoolean(KEY_ONBOARDED, false)
    fun setOnboarded(onboarded: Boolean) = prefs.edit().putBoolean(KEY_ONBOARDED, onboarded).apply()

    // ---- Theme Mode ----
    /** Returns "system", "light", or "dark" */
    fun getThemeMode(): String = prefs.getString(KEY_THEME_MODE, THEME_MODE_SYSTEM) ?: THEME_MODE_SYSTEM
    fun setThemeMode(mode: String) = prefs.edit().putString(KEY_THEME_MODE, mode).apply()

    /** Reactive flow that emits the current theme mode whenever it changes. */
    val themeModeFlow: Flow<String> = callbackFlow {
        trySend(getThemeMode())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_THEME_MODE) {
                trySend(prefs.getString(KEY_THEME_MODE, THEME_MODE_SYSTEM) ?: THEME_MODE_SYSTEM)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    companion object {
        private const val PREFS_NAME = "api_prefs"
        private const val KEY_DEEPSEEK = "deepseek_key"
        private const val KEY_DEEPSEEK_MODEL = "deepseek_model"
        private const val KEY_DEEPSEEK_BASE_URL = "deepseek_base_url"
        private const val KEY_QWEATHER = "qweather_key"
        private const val KEY_TICKTICK_TOKEN = "ticktick_token"
        private const val KEY_TICKTICK_CSRF = "ticktick_csrf"
        private const val KEY_TICKTICK_COMPLETED_MODE = "ticktick_completed_mode"

        // 已完成任务显示模式
        const val COMPLETED_MODE_ALL = "all"     // 展示全部已完成
        const val COMPLETED_MODE_NONE = "none"   // 不展示已完成
        const val COMPLETED_MODE_TODAY = "today" // 只展示今天的已完成
        private const val KEY_TICKTICK_USERNAME = "ticktick_username"
        private const val KEY_TICKTICK_PASSWORD = "ticktick_password"
        private const val KEY_ANIMATION_ENABLED = "animations_enabled"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ONBOARDED = "onboarded"

        const val MODEL_V4_PRO = "deepseek-v4-pro"
        const val MODEL_V4_FLASH = "deepseek-v4-flash"

        const val DEFAULT_DEEPSEEK_BASE_URL = "https://api.deepseek.com"

        const val MODEL_QWEN_FLASH = "qwen3.5-flash"
        const val DEFAULT_QWEN_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode"
        private const val KEY_QWEN = "qwen_key"
        private const val KEY_QWEN_BASE_URL = "qwen_base_url"
        private const val KEY_QWEN_MODEL = "qwen_model"
        private const val KEY_DEFAULT_AI_MODEL = "default_ai_model"


        const val KEY_TABLET_MODE = "tablet_mode"
        const val KEY_READ_ONLY_SYNC = "read_only_sync"
        const val KEY_SUMMARY_OUTLINE_EXPANDED = "summary_outline_expanded"
        const val KEY_SUMMARY_OUTLINE_ON_LEFT = "summary_outline_on_left"

        const val KEY_QUESTION_BANK_AUTO_SAVE = "qb_auto_save"
        const val KEY_MULTI_AGENT_PARALLEL = "multi_agent_parallel"
        const val KEY_SUMMARY_PREPROCESS_MODEL = "summary_preprocess_model"
        const val KEY_SUMMARY_AGENT_MODEL = "summary_agent_model"
        const val KEY_SUMMARY_INTEGRATE_MODEL = "summary_integrate_model"

        const val THEME_MODE_SYSTEM = "system"
        const val THEME_MODE_LIGHT = "light"
        const val THEME_MODE_DARK = "dark"
    }
}

/** AI 服务商。接口规范一致（OpenAI 兼容 /v1/chat/completions），仅 base_url / api_key / model 不同。 */
enum class AiProvider(val id: String, val displayName: String) {
    DEEPSEEK("deepseek", "DeepSeek"),
    QWEN("qwen", "通义千问")
}

/** 可选 AI 模型：统一入口，供各 AI 功能在执行前选择。 */
enum class AiModelOption(
    val id: String,
    val provider: AiProvider,
    val model: String,
    val label: String,
    val desc: String
) {
    DEEPSEEK_FLASH("deepseek_flash", AiProvider.DEEPSEEK, ApiPreferences.MODEL_V4_FLASH, "V4 Flash", "DeepSeek，速度快、成本低"),
    DEEPSEEK_PRO("deepseek_pro", AiProvider.DEEPSEEK, ApiPreferences.MODEL_V4_PRO, "V4 Pro", "DeepSeek，能力强、费用较高"),
    QWEN_FLASH("qwen_flash", AiProvider.QWEN, ApiPreferences.MODEL_QWEN_FLASH, "Qwen 3.5 Flash", "通义千问，响应快");

    companion object {
        /** 兼容旧存储（纯 model id，如 deepseek-v4-flash）与新存储（option id）。 */
        fun fromId(raw: String): AiModelOption =
            entries.firstOrNull { it.id == raw || it.model == raw } ?: DEEPSEEK_FLASH
    }
}
