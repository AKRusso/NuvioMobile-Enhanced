package com.nuvio.app.features.ai

import com.nuvio.app.features.addons.httpRequestRaw
import com.nuvio.app.features.details.MetaDetails
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

object AiAssistantService {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun chat(
        settings: AiAssistantSettings,
        meta: MetaDetails,
        messages: List<AiChatMessage>,
    ): AiAssistantReply {
        require(settings.isReady) { "AI assistant is not configured." }
        require(messages.isNotEmpty()) { "Message cannot be empty." }

        val webContext = if (settings.webSearchEnabled) {
            runCatching {
                AiWebSearchService.search(
                    apiKey = settings.tavilyApiKey,
                    meta = meta,
                    question = messages.last().text,
                )
            }.getOrNull()
        } else {
            null
        }
        val providers = buildList {
            if (settings.provider.isConfigured(settings)) {
                add(settings.provider)
            }
            AiProvider.entries
                .filter { it != settings.provider && it.isConfigured(settings) }
                .forEach(::add)
        }.distinct()
        require(providers.isNotEmpty()) { "AI assistant has no configured provider." }

        var lastError: Throwable? = null
        providers.forEachIndexed { index, provider ->
            try {
                val answer = requestProvider(
                    provider = provider,
                    settings = settings,
                    meta = meta,
                    messages = messages.takeLast(MAX_HISTORY_MESSAGES),
                    webContext = webContext,
                )
                return AiAssistantReply(
                    answer = answer,
                    sources = webContext?.sources.orEmpty(),
                )
            } catch (error: AiServiceException) {
                lastError = error
                val hasFallback = index < providers.lastIndex
                if (!error.retryable || !hasFallback) throw error
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                lastError = error
                if (index >= providers.lastIndex) throw error
            }
        }
        throw lastError ?: IllegalStateException("AI service is unavailable.")
    }

    private suspend fun requestProvider(
        provider: AiProvider,
        settings: AiAssistantSettings,
        meta: MetaDetails,
        messages: List<AiChatMessage>,
        webContext: AiWebSearchContext?,
    ): String = when (provider) {
        AiProvider.CEREBRAS -> chatWithOpenAiCompatible(
            endpoint = "https://api.cerebras.ai/v1/chat/completions",
            apiKey = settings.cerebrasApiKey,
            model = settings.cerebrasModel,
            providerName = "Cerebras",
            meta = meta,
            messages = messages,
            webContext = webContext,
        )
        AiProvider.GROQ -> chatWithOpenAiCompatible(
            endpoint = "https://api.groq.com/openai/v1/chat/completions",
            apiKey = settings.groqApiKey,
            model = settings.groqModel,
            providerName = "Groq",
            meta = meta,
            messages = messages,
            webContext = webContext,
        )
        AiProvider.GEMINI -> chatWithGemini(settings, meta, messages, webContext)
        AiProvider.OPENROUTER -> chatWithOpenAiCompatible(
            endpoint = "https://openrouter.ai/api/v1/chat/completions",
            apiKey = settings.openRouterApiKey,
            model = settings.openRouterModel,
            providerName = "OpenRouter",
            meta = meta,
            messages = messages,
            webContext = webContext,
            extraHeaders = mapOf(
                "HTTP-Referer" to "https://github.com/AKRusso/NuvioMobile-Enhanced",
                "X-Title" to "Nuvio Mobile Enhanced",
            ),
        )
    }

    private suspend fun chatWithGemini(
        settings: AiAssistantSettings,
        meta: MetaDetails,
        messages: List<AiChatMessage>,
        webContext: AiWebSearchContext?,
    ): String {
        val body = buildJsonObject {
            put("systemInstruction", buildJsonObject {
                put("parts", buildJsonArray {
                    add(buildJsonObject { put("text", systemPrompt(meta, webContext)) })
                })
            })
            put("contents", buildJsonArray {
                messages.forEach { message ->
                    add(buildJsonObject {
                        put("role", if (message.role == AiChatRole.USER) "user" else "model")
                        put("parts", buildJsonArray {
                            add(buildJsonObject { put("text", message.text) })
                        })
                    })
                }
            })
            put("generationConfig", buildJsonObject {
                put("temperature", RESPONSE_TEMPERATURE)
                put("maxOutputTokens", MAX_OUTPUT_TOKENS)
            })
        }
        val response = httpRequestRaw(
            method = "POST",
            url = "https://generativelanguage.googleapis.com/v1beta/models/${settings.geminiModel}:generateContent",
            headers = mapOf(
                "Content-Type" to "application/json",
                "x-goog-api-key" to settings.geminiApiKey,
            ),
            body = body.toString(),
        )
        ensureSuccess(response.status, response.body)
        val root = json.parseToJsonElement(response.body).jsonObject
        return root["candidates"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("content")
            ?.jsonObject
            ?.get("parts")
            ?.jsonArray
            ?.joinToString("\n") { part ->
                part.jsonObject["text"]?.jsonPrimitive?.contentOrNull.orEmpty()
            }
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: throw AiServiceException("Gemini returned an empty response.", retryable = true)
    }

    private suspend fun chatWithOpenAiCompatible(
        endpoint: String,
        apiKey: String,
        model: String,
        providerName: String,
        meta: MetaDetails,
        messages: List<AiChatMessage>,
        webContext: AiWebSearchContext?,
        extraHeaders: Map<String, String> = emptyMap(),
    ): String {
        require(apiKey.isNotBlank()) { "$providerName API key is missing." }
        val body = buildJsonObject {
            put("model", model)
            put("messages", buildJsonArray {
                add(openAiMessage("system", systemPrompt(meta, webContext)))
                messages.forEach { message ->
                    add(
                        openAiMessage(
                            role = if (message.role == AiChatRole.USER) "user" else "assistant",
                            text = message.text,
                        ),
                    )
                }
            })
            put("temperature", RESPONSE_TEMPERATURE)
            put("max_tokens", MAX_OUTPUT_TOKENS)
        }
        val response = httpRequestRaw(
            method = "POST",
            url = endpoint,
            headers = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer $apiKey",
            ) + extraHeaders,
            body = body.toString(),
        )
        ensureSuccess(response.status, response.body)
        val root = json.parseToJsonElement(response.body).jsonObject
        return root["choices"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("message")
            ?.jsonObject
            ?.get("content")
            ?.jsonPrimitive
            ?.contentOrNull
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: throw AiServiceException("$providerName returned an empty response.", retryable = true)
    }

    private fun openAiMessage(role: String, text: String): JsonObject = buildJsonObject {
        put("role", role)
        put("content", text)
    }

    private fun ensureSuccess(status: Int, body: String) {
        if (status in 200..299) return
        val apiMessage = runCatching {
            val error = json.parseToJsonElement(body).jsonObject["error"]
            when (error) {
                is JsonObject -> error["message"]?.jsonPrimitive?.contentOrNull
                else -> error?.jsonPrimitive?.contentOrNull
            }
        }.getOrNull()
        throw AiServiceException(
            message = apiMessage ?: "AI service returned error $status.",
            retryable = status == 408 || status == 429 || status >= 500,
        )
    }

    private fun systemPrompt(
        meta: MetaDetails,
        webContext: AiWebSearchContext?,
    ): String = buildString {
        appendLine("You are the content assistant inside the Nuvio app.")
        appendLine("Reply in the same language as the user. Keep the answer concise and natural.")
        appendLine("Treat only CONTENT DATA and WEB SOURCES below as verified facts about this title.")
        appendLine("Never invent plot points, characters, cast, production details, ratings, or events.")
        appendLine("If the requested fact is absent from both sections, say you could not find reliable information.")
        appendLine("When WEB SOURCES are present, cite factual claims with [1], [2], and so on.")
        appendLine("If sources conflict, state the uncertainty instead of choosing silently.")
        appendLine("You may give clearly labeled subjective recommendations based only on the supplied genres and summary.")
        appendLine("Do not reveal spoilers unless the user explicitly asks for spoilers.")
        appendLine("Do not pretend that you watched the title or accessed external sources.")
        appendLine()
        appendLine("CONTENT DATA")
        appendLine("Title: ${meta.name}")
        appendLine("Type: ${meta.type}")
        meta.releaseInfo?.let { appendLine("Release: $it") }
        meta.genres.takeIf { it.isNotEmpty() }?.let { appendLine("Genres: ${it.joinToString()}") }
        meta.runtime?.let { appendLine("Runtime: $it") }
        meta.imdbRating?.let { appendLine("IMDb rating: $it") }
        meta.director.takeIf { it.isNotEmpty() }?.let { appendLine("Directors: ${it.joinToString()}") }
        meta.writer.takeIf { it.isNotEmpty() }?.let { appendLine("Writers: ${it.joinToString()}") }
        meta.cast.take(10).takeIf { it.isNotEmpty() }?.let { cast ->
            appendLine("Cast: ${cast.joinToString { it.name }}")
        }
        meta.description?.takeIf(String::isNotBlank)?.let { appendLine("Summary: $it") }
        appendLine("END CONTENT DATA")
        webContext?.let {
            appendLine()
            appendLine("WEB SOURCES")
            appendLine(it.promptContext)
            appendLine("END WEB SOURCES")
        }
    }
}

private fun AiProvider.isConfigured(settings: AiAssistantSettings): Boolean =
    when (this) {
        AiProvider.CEREBRAS -> settings.cerebrasApiKey.isNotBlank() && settings.cerebrasModel.isNotBlank()
        AiProvider.GROQ -> settings.groqApiKey.isNotBlank() && settings.groqModel.isNotBlank()
        AiProvider.GEMINI -> settings.geminiApiKey.isNotBlank() && settings.geminiModel.isNotBlank()
        AiProvider.OPENROUTER -> settings.openRouterApiKey.isNotBlank() && settings.openRouterModel.isNotBlank()
    }

private class AiServiceException(
    message: String,
    val retryable: Boolean,
) : IllegalStateException(message)

private const val MAX_HISTORY_MESSAGES = 8
private const val MAX_OUTPUT_TOKENS = 500
private const val RESPONSE_TEMPERATURE = 0.2
