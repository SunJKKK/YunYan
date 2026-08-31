package com.sunjk.sunjktool.data.remote

import com.sunjk.sunjktool.data.local.ApiPreferences
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
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
    private val baseUrl: String get() = apiPreferences.getDeepSeekBaseUrl().ifBlank { ApiPreferences.DEFAULT_DEEPSEEK_BASE_URL }

    suspend fun getBalance(): DeepSeekBalanceResponse =
        client.get("$baseUrl$BALANCE_PATH") {
            header("Authorization", "Bearer $apiKey")
            header("Accept", "application/json")
        }.body()

    suspend fun chatCompletion(
        systemPrompt: String,
        userMessage: String,
        temperature: Double = 0.9,
        modelOverride: String? = null
    ): String {
        val response: ChatCompletionResponse = client.post("$baseUrl$CHAT_PATH") {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(ChatCompletionRequest(
                model = modelOverride ?: model,
                messages = listOf(
                    ChatMessage("system", systemPrompt),
                    ChatMessage("user", userMessage)
                ),
                temperature = temperature
            ))
        }.body()
        return response.choices.firstOrNull()?.message?.content?.trim() ?: ""
    }

    suspend fun chatCompletionStream(
        systemPrompt: String,
        userMessage: String,
        temperature: Double = 0.9,
        modelOverride: String? = null
    ): Flow<String> = flow {
        client.post("$baseUrl$CHAT_PATH") {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(ChatCompletionRequest(
                model = modelOverride ?: model,
                messages = listOf(
                    ChatMessage("system", systemPrompt),
                    ChatMessage("user", userMessage)
                ),
                temperature = temperature,
                stream = true
            ))
        }.apply {
            val channel = bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: continue
                if (line.startsWith("data: ") && line != "data: [DONE]") {
                    try {
                        val json = line.removePrefix("data: ")
                        val chunk = jsonParser.decodeFromString<ChatCompletionChunk>(json)
                        chunk.choices.firstOrNull()?.delta?.content?.let { token ->
                            if (token.isNotEmpty()) emit(token)
                        }
                    } catch (_: Exception) { /* skip malformed chunks */ }
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
    val stream: Boolean = false
)

@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
data class ChatCompletionResponse(val choices: List<ChatChoice> = emptyList())

@Serializable
data class ChatChoice(val message: ChatMessage? = null)

@Serializable
data class ChatCompletionChunk(val choices: List<ChunkChoice> = emptyList())

@Serializable
data class ChunkChoice(val delta: DeltaContent = DeltaContent())

@Serializable
data class DeltaContent(val content: String = "")
