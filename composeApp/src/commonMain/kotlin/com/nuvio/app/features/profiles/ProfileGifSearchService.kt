package com.nuvio.app.features.profiles

import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.addons.httpGetTextWithHeaders
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
            val provider = GifSearchConfig.PROVIDER.trim().lowercase()
            val apiKey = GifSearchConfig.API_KEY.trim()
            val endpoint = GifSearchConfig.SEARCH_ENDPOINT.trim()
            val cleanQuery = query.trim()

            if (cleanQuery.isBlank()) return@runCatching emptyList()

            when (provider.ifBlank { DefaultGifProvider }) {
                "duckduckgo", "ddg" -> searchDuckDuckGo(cleanQuery, endpoint, limit)
                "proxy", "nuvio" -> searchProxy(cleanQuery, endpoint, limit)
                "tenor" -> searchTenor(cleanQuery, endpoint, apiKey, limit)
                else -> searchGiphy(cleanQuery, endpoint, apiKey, limit)
            }
        }
    }

    private suspend fun searchDuckDuckGo(
        query: String,
        endpoint: String,
        limit: Int,
    ): List<ProfileGifSearchItem> {
        val searchQuery = if (query.contains("gif", ignoreCase = true)) query else "$query gif"
        val searchPageUrl = DuckDuckGoSearchPageEndpoint.withQueryParameters(
            "q" to searchQuery,
            "iax" to "images",
            "ia" to "images",
        )
        val headers = mapOf(
            "User-Agent" to DuckDuckGoUserAgent,
            "Accept" to "application/json,text/html;q=0.9,*/*;q=0.8",
            "Referer" to "https://duckduckgo.com/",
        )
        val searchPage = httpGetTextWithHeaders(searchPageUrl, headers)
        val vqd = extractDuckDuckGoVqd(searchPage) ?: throw ProfileGifSearchNotConfiguredException()
        val payload = httpGetTextWithHeaders(
            (endpoint.ifBlank { DuckDuckGoImageSearchEndpoint }).withQueryParameters(
                "l" to "wt-wt",
                "o" to "json",
                "q" to searchQuery,
                "vqd" to vqd,
                "f" to "type:gif",
                "p" to "1",
            ),
            headers + ("Referer" to searchPageUrl),
        )
        return decodeDuckDuckGoItems(payload, limit)
    }

    private suspend fun searchProxy(query: String, endpoint: String, limit: Int): List<ProfileGifSearchItem> {
        if (endpoint.isBlank()) throw ProfileGifSearchNotConfiguredException()
        val payload = httpGetText(
            endpoint.withQueryParameters(
                "q" to query,
                "query" to query,
                "limit" to limit.coerceIn(1, 50).toString(),
            )
        )
        return decodeProxyItems(payload).ifEmpty {
            runCatching { decodeGiphyItems(payload) }.getOrDefault(emptyList())
        }
    }

    private suspend fun searchGiphy(
        query: String,
        endpoint: String,
        apiKey: String,
        limit: Int,
    ): List<ProfileGifSearchItem> {
        if (apiKey.isBlank()) throw ProfileGifSearchNotConfiguredException()
        val payload = httpGetTextWithProviderKeyGuard(
            (endpoint.ifBlank { GiphySearchEndpoint }).withQueryParameters(
                "api_key" to apiKey,
                "q" to query,
                "limit" to limit.coerceIn(1, 50).toString(),
                "rating" to "pg-13",
                "lang" to "en",
            )
        )
        return decodeGiphyItems(payload)
    }

    private suspend fun searchTenor(
        query: String,
        endpoint: String,
        apiKey: String,
        limit: Int,
    ): List<ProfileGifSearchItem> {
        if (apiKey.isBlank() || apiKey in UnsupportedTenorApiKeys) {
            throw ProfileGifSearchNotConfiguredException()
        }
        val searchEndpoint = endpoint.ifBlank { TenorV2SearchEndpoint }
        val payload = httpGetTextWithProviderKeyGuard(
            searchEndpoint.withQueryParameters(
                "q" to query,
                "key" to apiKey,
                "limit" to limit.coerceIn(1, 50).toString(),
                "media_filter" to if (searchEndpoint.contains("/v1/", ignoreCase = true)) {
                    "minimal"
                } else {
                    "gif,tinygif,nanogif"
                },
                "client_key" to "nuvio_enhanced",
                "contentfilter" to "medium",
            )
        )
        return decodeTenorItems(payload)
    }

    private suspend fun httpGetTextWithProviderKeyGuard(url: String): String =
        try {
            httpGetText(url)
        } catch (throwable: Throwable) {
            val message = throwable.message.orEmpty()
            if (
                message.contains("HTTP 400") ||
                message.contains("HTTP 401") ||
                message.contains("HTTP 403")
            ) {
                throw ProfileGifSearchNotConfiguredException()
            }
            throw throwable
        }

    private fun decodeProxyItems(payload: String): List<ProfileGifSearchItem> =
        runCatching {
            json.decodeFromString<ProxyGifSearchResponse>(payload).items()
        }.getOrElse {
            runCatching {
                json.decodeFromString<List<ProxyGifItem>>(payload).toProfileItems()
            }.getOrDefault(emptyList())
        }

    private fun decodeGiphyItems(payload: String): List<ProfileGifSearchItem> =
        json.decodeFromString<GiphySearchResponse>(payload).data.mapNotNull { result ->
            val previewUrl = result.images.fixedWidthSmall?.url
                ?: result.images.previewGif?.url
                ?: result.images.downsizedSmall?.url
                ?: result.images.downsized?.url
                ?: result.images.original?.url
            val gifUrl = result.images.original?.url
                ?: result.images.downsized?.url
                ?: result.images.fixedWidth?.url
                ?: previewUrl
            if (previewUrl.isNullOrBlank() || gifUrl.isNullOrBlank()) {
                null
            } else {
                ProfileGifSearchItem(
                    id = result.id.ifBlank { gifUrl },
                    title = result.title?.takeIf { it.isNotBlank() } ?: "GIF",
                    previewUrl = previewUrl,
                    gifUrl = gifUrl,
                )
            }
        }

    private fun decodeTenorItems(payload: String): List<ProfileGifSearchItem> =
        json.decodeFromString<TenorSearchResponse>(payload).results.mapNotNull { result ->
            val v1Media = result.media.firstOrNull().orEmpty()
            val previewUrl = result.mediaFormats["tinygif"]?.url
                ?: result.mediaFormats["nanogif"]?.url
                ?: result.mediaFormats["gif"]?.url
                ?: v1Media["tinygif"]?.url
                ?: v1Media["gif"]?.url
            val gifUrl = result.mediaFormats["gif"]?.url
                ?: result.mediaFormats["tinygif"]?.url
                ?: v1Media["gif"]?.url
                ?: previewUrl
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

    private fun decodeDuckDuckGoItems(payload: String, limit: Int): List<ProfileGifSearchItem> =
        json.decodeFromString<DuckDuckGoImageSearchResponse>(payload).results
            .asSequence()
            .mapNotNull { result ->
                val gifUrl = result.image?.takeIf { it.isGifUrl() }
                    ?: result.url?.takeIf { it.isGifUrl() }
                val previewUrl = result.thumbnail?.takeIf { it.isNotBlank() } ?: gifUrl
                if (previewUrl.isNullOrBlank() || gifUrl.isNullOrBlank()) {
                    null
                } else {
                    ProfileGifSearchItem(
                        id = result.imageToken?.takeIf { it.isNotBlank() } ?: gifUrl,
                        title = result.title?.takeIf { it.isNotBlank() } ?: "GIF",
                        previewUrl = previewUrl,
                        gifUrl = gifUrl,
                    )
                }
            }
            .distinctBy { it.gifUrl }
            .take(limit.coerceIn(1, 50))
            .toList()
}

@Serializable
private data class ProxyGifSearchResponse(
    val results: List<ProxyGifItem> = emptyList(),
    val data: List<ProxyGifItem> = emptyList(),
) {
    fun items(): List<ProfileGifSearchItem> =
        (results.ifEmpty { data }).toProfileItems()
}

@Serializable
private data class ProxyGifItem(
    val id: String = "",
    val title: String? = null,
    val previewUrl: String? = null,
    val gifUrl: String? = null,
    val url: String? = null,
)

private fun List<ProxyGifItem>.toProfileItems(): List<ProfileGifSearchItem> =
    mapNotNull { item ->
        val gifUrl = item.gifUrl ?: item.url
        val previewUrl = item.previewUrl ?: gifUrl
        if (previewUrl.isNullOrBlank() || gifUrl.isNullOrBlank()) {
            null
        } else {
            ProfileGifSearchItem(
                id = item.id.ifBlank { gifUrl },
                title = item.title?.takeIf { it.isNotBlank() } ?: "GIF",
                previewUrl = previewUrl,
                gifUrl = gifUrl,
            )
        }
    }

@Serializable
private data class GiphySearchResponse(
    val data: List<GiphyResult> = emptyList(),
)

@Serializable
private data class GiphyResult(
    val id: String = "",
    val title: String? = null,
    val images: GiphyImages = GiphyImages(),
)

@Serializable
private data class GiphyImages(
    @SerialName("fixed_width") val fixedWidth: GiphyImage? = null,
    @SerialName("fixed_width_small") val fixedWidthSmall: GiphyImage? = null,
    @SerialName("preview_gif") val previewGif: GiphyImage? = null,
    @SerialName("downsized_small") val downsizedSmall: GiphyImage? = null,
    val downsized: GiphyImage? = null,
    val original: GiphyImage? = null,
)

@Serializable
private data class GiphyImage(
    val url: String? = null,
)

@Serializable
private data class TenorSearchResponse(
    val results: List<TenorResult> = emptyList(),
)

@Serializable
private data class TenorResult(
    val id: String = "",
    val title: String? = null,
    @SerialName("content_description") val contentDescription: String? = null,
    val media: List<Map<String, TenorMedia>> = emptyList(),
    @SerialName("media_formats") val mediaFormats: Map<String, TenorMedia> = emptyMap(),
)

@Serializable
private data class TenorMedia(
    val url: String? = null,
)

@Serializable
private data class DuckDuckGoImageSearchResponse(
    val results: List<DuckDuckGoImageResult> = emptyList(),
)

@Serializable
private data class DuckDuckGoImageResult(
    val title: String? = null,
    val image: String? = null,
    val thumbnail: String? = null,
    val url: String? = null,
    @SerialName("image_token") val imageToken: String? = null,
)

private const val DefaultGifProvider = "duckduckgo"
private const val DuckDuckGoSearchPageEndpoint = "https://duckduckgo.com/"
private const val DuckDuckGoImageSearchEndpoint = "https://duckduckgo.com/i.js"
private const val DuckDuckGoUserAgent =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"
private const val GiphySearchEndpoint = "https://api.giphy.com/v1/gifs/search"
private const val TenorV2SearchEndpoint = "https://tenor.googleapis.com/v2/search"

private val UnsupportedTenorApiKeys = setOf(
    "LIVDSRZULELA",
)

private val DuckDuckGoVqdPatterns = listOf(
    Regex("""vqd=['"]([^'"]+)['"]"""),
    Regex("""vqd=([^&"'\\]+)"""),
)

private fun extractDuckDuckGoVqd(payload: String): String? =
    DuckDuckGoVqdPatterns.firstNotNullOfOrNull { pattern ->
        pattern.find(payload)?.groupValues?.getOrNull(1)
    }?.takeIf { it.isNotBlank() }

private fun String.isGifUrl(): Boolean =
    contains(".gif", ignoreCase = true) && startsWith("http", ignoreCase = true)

private fun String.withQueryParameters(vararg parameters: Pair<String, String>): String {
    val separator = if (contains("?")) "&" else "?"
    return buildString {
        append(this@withQueryParameters.trimEnd('&', '?'))
        append(separator)
        append(
            parameters.joinToString("&") { (key, value) ->
                "${key.encodeURLParameter()}=${value.encodeURLParameter()}"
            }
        )
    }
}
