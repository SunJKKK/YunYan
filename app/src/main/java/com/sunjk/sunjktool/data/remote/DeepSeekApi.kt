package com.sunjk.sunjktool.data.remote

import com.sunjk.sunjktool.data.local.ApiPreferences
import com.sunjk.sunjktool.data.local.AiProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class DeepSeekApi(
    private val client: HttpClient,
    private val apiPreferences: ApiPreferences
) {

    companion object {
        private const val BALANCE_PATH = "/user/balance"
        private const val CHAT_PATH = "/v1/chat/completions"
        private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }
    }

    private val apiKey: String get() = apiPreferences.getDeepSeekKey()
    private val model: String get() = apiPreferences.getDeepSeekModel()
    private val baseUrl: String get() = normalizeBaseUrl(apiPreferences.getDeepSeekBaseUrl().ifBlank { ApiPreferences.DEFAULT_DEEPSEEK_BASE_URL })

    private fun apiKeyFor(provider: AiProvider): String = when (provider) {
        AiProvider.DEEPSEEK -> apiPreferences.getDeepSeekKey()
        AiProvider.QWEN -> apiPreferences.getQwenKey()
    }

    private fun modelFor(provider: AiProvider): String = when (provider) {
        AiProvider.DEEPSEEK -> apiPreferences.getDeepSeekModel()
        AiProvider.QWEN -> apiPreferences.getQwenModel()
    }

    private fun baseUrlFor(provider: AiProvider): String = when (provider) {
        AiProvider.DEEPSEEK -> normalizeBaseUrl(apiPreferences.getDeepSeekBaseUrl().ifBlank { ApiPreferences.DEFAULT_DEEPSEEK_BASE_URL })
        AiProvider.QWEN -> normalizeBaseUrl(apiPreferences.getQwenBaseUrl().ifBlank { ApiPreferences.DEFAULT_QWEN_BASE_URL })
    }

    /** OpenAI 兼容 base 地址约定不含结尾 /v1（路径统一拼 /v1/chat/completions），兜底去掉历史配置里多余的 /v1 */
    private fun normalizeBaseUrl(url: String): String = url.trim().trimEnd('/').removeSuffix("/v1")

    suspend fun getBalance(): DeepSeekBalanceResponse =
        client.get("$baseUrl$BALANCE_PATH") {
            header("Authorization", "Bearer $apiKey")
            header("Accept", "application/json")
        }.body()

    suspend fun chatCompletion(
        systemPrompt: String,
        userMessage: String,
        temperature: Double = 0.9,
        modelOverride: String? = null,
        provider: AiProvider = AiProvider.DEEPSEEK,
        enableSearch: Boolean = false
    ): String {
        val key = apiKeyFor(provider)
        val base = baseUrlFor(provider)
        val response = client.post("$base$CHAT_PATH") {
            header("Authorization", "Bearer $key")
            contentType(ContentType.Application.Json)
            setBody(ChatCompletionRequest(
                model = modelOverride ?: modelFor(provider),
                messages = listOf(
                    ChatMessage("system", systemPrompt),
                    ChatMessage("user", userMessage)
                ),
                temperature = temperature,
                enableSearch = enableSearch && provider == AiProvider.QWEN
            ))
        }
        if (!response.status.isSuccess()) {
            throw RuntimeException("AI 服务错误 HTTP ${response.status.value}: ${response.bodyAsText().take(300)}")
        }
        val parsed: ChatCompletionResponse = response.body()
        return parsed.choices.firstOrNull()?.message?.content?.trim() ?: ""
    }

    suspend fun chatCompletionStream(
        systemPrompt: String,
        userMessage: String,
        temperature: Double = 0.9,
        modelOverride: String? = null,
        provider: AiProvider = AiProvider.DEEPSEEK,
        enableSearch: Boolean = false
    ): Flow<String> = flow {
        val key = apiKeyFor(provider)
        val base = baseUrlFor(provider)
        val response = client.post("$base$CHAT_PATH") {
            header("Authorization", "Bearer $key")
            contentType(ContentType.Application.Json)
            setBody(ChatCompletionRequest(
                model = modelOverride ?: modelFor(provider),
                messages = listOf(
                    ChatMessage("system", systemPrompt),
                    ChatMessage("user", userMessage)
                ),
                temperature = temperature,
                stream = true,
                enableSearch = enableSearch && provider == AiProvider.QWEN
            ))
        }
        if (!response.status.isSuccess()) {
            throw RuntimeException("AI 服务错误 HTTP ${response.status.value}: ${response.bodyAsText().take(300)}")
        }
        val channel = response.bodyAsChannel()
        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: continue
            if (line.startsWith("data: ") && line != "data: [DONE]") {
                val json = line.removePrefix("data: ")
                var chunk: ChatCompletionChunk? = null
                try {
                    chunk = jsonParser.decodeFromString<ChatCompletionChunk>(json)
                } catch (_: Exception) { /* skip malformed chunks */ }
                val c = chunk ?: continue
                c.error?.let { err ->
                    throw RuntimeException("AI 服务错误: ${err.message.ifBlank { err.code }}")
                }
                c.choices.firstOrNull()?.delta?.content?.let { token ->
                    if (token.isNotEmpty()) emit(token)
                }
            }
        }
    }
}

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.9,
    val stream: Boolean = false,
    val enableSearch: Boolean = false
)

@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
data class ChatCompletionResponse(val choices: List<ChatChoice> = emptyList())

@Serializable
data class ChatChoice(val message: ChatMessage? = null)

@Serializable
data class ChatCompletionChunk(
    val choices: List<ChunkChoice> = emptyList(),
    val error: ApiError? = null
)

@Serializable
data class ChunkChoice(val delta: DeltaContent = DeltaContent())

@Serializable
data class DeltaContent(val content: String = "")

@Serializable
data class ApiError(val message: String = "", val code: String = "")
