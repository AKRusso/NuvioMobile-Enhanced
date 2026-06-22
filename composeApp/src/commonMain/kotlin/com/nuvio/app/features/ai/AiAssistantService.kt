package com.nuvio.app.features.ai

import com.nuvio.app.features.addons.httpRequestRaw
import com.nuvio.app.features.details.MetaDetails
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
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
    ): String {
        require(settings.isReady) { "AI assistant is not configured." }
        require(messages.isNotEmpty()) { "Message cannot be empty." }

        return when (settings.provider) {
            AiProvider.GEMINI -> chatWithGemini(settings, meta, messages)
            AiProvider.OPENROUTER -> chatWithOpenRouter(settings, meta, messages)
        }
    }

    private suspend fun chatWithGemini(
        settings: AiAssistantSettings,
        meta: MetaDetails,
        messages: List<AiChatMessage>,
    ): String {
        val body = buildJsonObject {
            put("systemInstruction", buildJsonObject {
                put("parts", buildJsonArray {
                    add(buildJsonObject { put("text", systemPrompt(meta)) })
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
                put("temperature", 0.65)
                put("maxOutputTokens", 700)
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
            ?.joinToString("\n") { part -> part.jsonObject["text"]?.jsonPrimitive?.contentOrNull.orEmpty() }
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("Gemini boş bir yanıt döndürdü.")
    }

    private suspend fun chatWithOpenRouter(
        settings: AiAssistantSettings,
        meta: MetaDetails,
        messages: List<AiChatMessage>,
    ): String {
        val body = buildJsonObject {
            put("model", settings.openRouterModel)
            put("messages", buildJsonArray {
                add(openRouterMessage("system", systemPrompt(meta)))
                messages.forEach { message ->
                    add(
                        openRouterMessage(
                            role = if (message.role == AiChatRole.USER) "user" else "assistant",
                            text = message.text,
                        ),
                    )
                }
            })
            put("temperature", 0.65)
            put("max_tokens", 700)
        }
        val response = httpRequestRaw(
            method = "POST",
            url = "https://openrouter.ai/api/v1/chat/completions",
            headers = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer ${settings.openRouterApiKey}",
                "HTTP-Referer" to "https://github.com/yesnt10/NuvioMobile-Enhanced",
                "X-Title" to "Nuvio Mobile Enhanced",
            ),
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
            ?: throw IllegalStateException("OpenRouter boş bir yanıt döndürdü.")
    }

    private fun openRouterMessage(role: String, text: String): JsonObject = buildJsonObject {
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
        throw IllegalStateException(apiMessage ?: "AI servisi hata döndürdü ($status).")
    }

    private fun systemPrompt(meta: MetaDetails): String = buildString {
        appendLine("Sen Nuvio uygulamasındaki içerik asistanısın.")
        appendLine("Kullanıcının dilinde, kısa ve doğal yanıt ver.")
        appendLine("Kullanıcı açıkça istemedikçe spoiler verme. Emin olmadığın bilgileri uydurma.")
        appendLine("Yalnızca aşağıdaki içerik ve genel sinema bilgisi bağlamında yardımcı ol.")
        appendLine()
        appendLine("İçerik: ${meta.name}")
        appendLine("Tür: ${meta.type}")
        meta.releaseInfo?.let { appendLine("Yayın: $it") }
        meta.genres.takeIf { it.isNotEmpty() }?.let { appendLine("Türler: ${it.joinToString()}") }
        meta.runtime?.let { appendLine("Süre: $it") }
        meta.imdbRating?.let { appendLine("IMDb: $it") }
        meta.director.takeIf { it.isNotEmpty() }?.let { appendLine("Yönetmen: ${it.joinToString()}") }
        meta.writer.takeIf { it.isNotEmpty() }?.let { appendLine("Yazar: ${it.joinToString()}") }
        meta.cast.take(10).takeIf { it.isNotEmpty() }?.let { cast ->
            appendLine("Oyuncular: ${cast.joinToString { it.name }}")
        }
        meta.description?.takeIf(String::isNotBlank)?.let { appendLine("Özet: $it") }
    }
}

