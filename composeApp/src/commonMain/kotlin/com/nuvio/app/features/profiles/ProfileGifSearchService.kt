package com.nuvio.app.features.profiles

import com.nuvio.app.features.addons.httpGetText
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal data class ProfileGifSearchItem(
    val id: String,
    val title: String,
    val previewUrl: String,
    val gifUrl: String,
)

internal class ProfileGifSearchNotConfiguredException : IllegalStateException()

internal object ProfileGifSearchService {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun search(query: String, limit: Int = 24): Result<List<ProfileGifSearchItem>> = withContext(Dispatchers.Default) {
        runCatching {
            val apiKey = GifSearchConfig.TENOR_API_KEY.trim()
            val endpoint = GifSearchConfig.TENOR_SEARCH_ENDPOINT.trim().ifBlank {
                "https://g.tenor.com/v1/search"
            }
            val cleanQuery = query.trim()

            if (apiKey.isBlank()) {
                throw ProfileGifSearchNotConfiguredException()
            }
            if (cleanQuery.isBlank()) return@runCatching emptyList()

            val url = buildString {
                append(endpoint.trimEnd('/'))
                append("?q=")
                append(cleanQuery.encodeURLParameter())
                append("&key=")
                append(apiKey.encodeURLParameter())
                append("&limit=")
                append(limit.coerceIn(1, 50))
                append("&media_filter=minimal")
                append("&contentfilter=medium")
            }

            val response = json.decodeFromString<TenorV1SearchResponse>(httpGetText(url))
            response.results.mapNotNull { result ->
                val media = result.media.firstOrNull().orEmpty()
                val previewUrl = media["tinygif"]?.url ?: media["gif"]?.url
                val gifUrl = media["gif"]?.url ?: previewUrl
                if (previewUrl.isNullOrBlank() || gifUrl.isNullOrBlank()) {
                    null
                } else {
                    ProfileGifSearchItem(
                        id = result.id.ifBlank { gifUrl },
                        title = result.contentDescription?.takeIf { it.isNotBlank() }
                            ?: result.title?.takeIf { it.isNotBlank() }
                            ?: "GIF",
                        previewUrl = previewUrl,
                        gifUrl = gifUrl,
                    )
                }
            }
        }
    }
}

@Serializable
private data class TenorV1SearchResponse(
    val results: List<TenorV1Result> = emptyList(),
)

@Serializable
private data class TenorV1Result(
    val id: String = "",
    val title: String? = null,
    @SerialName("content_description") val contentDescription: String? = null,
    val media: List<Map<String, TenorV1Media>> = emptyList(),
)

@Serializable
private data class TenorV1Media(
    val url: String? = null,
)
