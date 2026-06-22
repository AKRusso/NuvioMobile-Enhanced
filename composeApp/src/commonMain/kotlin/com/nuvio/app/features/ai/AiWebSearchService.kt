package com.nuvio.app.features.ai

import com.nuvio.app.features.addons.httpRequestRaw
import com.nuvio.app.features.details.MetaDetails
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal data class AiWebSearchContext(
    val promptContext: String,
    val sources: List<AiWebSource>,
)

internal object AiWebSearchService {
    private val json = Json { ignoreUnknownKeys = true }
    private val memoryCache = linkedMapOf<String, AiWebSearchContext>()

    suspend fun search(
        apiKey: String,
        meta: MetaDetails,
        question: String,
    ): AiWebSearchContext? {
        if (apiKey.isBlank() || !shouldSearchWeb(question)) return null
        val cacheKey = "${meta.id}|${question.trim().lowercase()}"
        memoryCache[cacheKey]?.let { return it }

        val query = buildString {
            append('"')
            append(meta.name)
            append("\" ")
            meta.releaseInfo?.takeIf(String::isNotBlank)?.let {
                append(it)
                append(' ')
            }
            append(meta.type)
            append(' ')
            append(question.trim().take(MAX_QUESTION_LENGTH))
        }
        val body = buildJsonObject {
            put("api_key", apiKey)
            put("query", query)
            put("search_depth", "basic")
            put("topic", "general")
            put("max_results", MAX_SEARCH_RESULTS)
            put("include_answer", false)
            put("include_raw_content", false)
        }
        val response = httpRequestRaw(
            method = "POST",
            url = "https://api.tavily.com/search",
            headers = mapOf("Content-Type" to "application/json"),
            body = body.toString(),
        )
        if (response.status !in 200..299) return null

        val results = runCatching {
            json.parseToJsonElement(response.body)
                .jsonObject["results"]
                ?.jsonArray
                .orEmpty()
                .mapNotNull { element ->
                    val item = element.jsonObject
                    val title = item["title"]?.jsonPrimitive?.content?.trim().orEmpty()
                    val url = item["url"]?.jsonPrimitive?.content?.trim().orEmpty()
                    val content = item["content"]?.jsonPrimitive?.content?.trim().orEmpty()
                    if (title.isBlank() || url.isBlank() || content.isBlank()) {
                        null
                    } else {
                        SearchResult(title = title, url = url, content = content)
                    }
                }
        }.getOrNull().orEmpty()
        if (results.isEmpty()) return null

        val context = AiWebSearchContext(
            promptContext = buildString {
                results.forEachIndexed { index, result ->
                    appendLine("[${index + 1}] ${result.title}")
                    appendLine("URL: ${result.url}")
                    appendLine(result.content.take(MAX_RESULT_CONTENT_LENGTH))
                    appendLine()
                }
            }.trim(),
            sources = results.map { result ->
                AiWebSource(title = result.title, url = result.url)
            },
        )
        if (memoryCache.size >= MAX_CACHE_ENTRIES) {
            memoryCache.remove(memoryCache.keys.first())
        }
        memoryCache[cacheKey] = context
        return context
    }

    private fun shouldSearchWeb(question: String): Boolean {
        val normalized = question.trim().lowercase()
        if (normalized.length < 8) return false
        return localOnlyQuestionMarkers.none(normalized::contains)
    }
}

private data class SearchResult(
    val title: String,
    val url: String,
    val content: String,
)

private val localOnlyQuestionMarkers = listOf(
    "izlemeye değer",
    "worth watching",
    "benzer yapım",
    "similar title",
    "türü ne",
    "hangi tür",
    "what genre",
    "süresi ne",
    "how long",
    "runtime",
    "puanı ne",
    "rating",
    "özetle",
    "summarize",
    "izlemeden önce",
    "before watching",
)

private const val MAX_SEARCH_RESULTS = 5
private const val MAX_RESULT_CONTENT_LENGTH = 900
private const val MAX_QUESTION_LENGTH = 350
private const val MAX_CACHE_ENTRIES = 40
