package com.nuvio.app.features.livetv

import com.nuvio.app.features.addons.httpGetText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object LiveTvRepository {
    private val mutableUiState = MutableStateFlow(LiveTvUiState())
    val uiState = mutableUiState.asStateFlow()

    private var initialized = false

    fun ensureLoaded() {
        if (initialized) return
        initialized = true
        mutableUiState.value = mutableUiState.value.copy(
            sourceUrl = LiveTvStorage.loadSourceUrl().orEmpty(),
        )
    }

    suspend fun load(sourceUrl: String): Result<List<LiveTvChannel>> {
        val normalizedUrl = sourceUrl.trim()
        if (!normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://")) {
            val error = IllegalArgumentException("Geçerli bir HTTP veya HTTPS M3U bağlantısı girin.")
            mutableUiState.value = mutableUiState.value.copy(errorMessage = error.message)
            return Result.failure(error)
        }

        mutableUiState.value = mutableUiState.value.copy(
            sourceUrl = normalizedUrl,
            isLoading = true,
            errorMessage = null,
        )

        return runCatching {
            val channels = parseM3uPlaylist(httpGetText(normalizedUrl))
            require(channels.isNotEmpty()) { "Bu M3U listesinde oynatılabilir kanal bulunamadı." }
            LiveTvStorage.saveSourceUrl(normalizedUrl)
            mutableUiState.value = LiveTvUiState(
                sourceUrl = normalizedUrl,
                channels = channels,
                isLoaded = true,
            )
            channels
        }.onFailure { error ->
            mutableUiState.value = mutableUiState.value.copy(
                isLoading = false,
                isLoaded = mutableUiState.value.channels.isNotEmpty(),
                errorMessage = error.message ?: "M3U listesi yüklenemedi.",
            )
        }
    }

    fun disconnect() {
        LiveTvStorage.saveSourceUrl("")
        mutableUiState.value = LiveTvUiState()
    }
}

internal expect object LiveTvStorage {
    fun loadSourceUrl(): String?
    fun saveSourceUrl(url: String)
}

internal fun parseM3uPlaylist(content: String): List<LiveTvChannel> {
    val channels = mutableListOf<LiveTvChannel>()
    var metadata: ParsedM3uMetadata? = null
    var pendingHeaders = emptyMap<String, String>()

    content.lineSequence().forEach { rawLine ->
        val line = rawLine.trim().removePrefix("\uFEFF")
        when {
            line.startsWith("#EXTINF", ignoreCase = true) -> {
                metadata = parseExtInf(line)
            }

            line.startsWith("#EXTVLCOPT:http-user-agent=", ignoreCase = true) -> {
                pendingHeaders = pendingHeaders + ("User-Agent" to line.substringAfter('=').trim())
            }

            line.startsWith("#EXTHTTP:", ignoreCase = true) -> {
                pendingHeaders = pendingHeaders + parseExtHttpHeaders(line.substringAfter(':'))
            }

            line.isNotEmpty() && !line.startsWith("#") -> {
                val parsedUrl = parseStreamUrl(line)
                val current = metadata ?: ParsedM3uMetadata(
                    name = "Kanal ${channels.size + 1}",
                    logoUrl = null,
                    group = "",
                )
                channels += LiveTvChannel(
                    id = "${parsedUrl.url}#${channels.size}",
                    name = current.name.ifBlank { "Kanal ${channels.size + 1}" },
                    streamUrl = parsedUrl.url,
                    logoUrl = current.logoUrl,
                    group = current.group,
                    headers = pendingHeaders + parsedUrl.headers,
                )
                metadata = null
                pendingHeaders = emptyMap()
            }
        }
    }

    return channels.distinctBy { it.streamUrl }
}

private data class ParsedM3uMetadata(
    val name: String,
    val logoUrl: String?,
    val group: String,
)

private data class ParsedStreamUrl(
    val url: String,
    val headers: Map<String, String>,
)

private val m3uAttributeRegex = Regex("""([\w-]+)="([^"]*)"""")

private fun parseExtInf(line: String): ParsedM3uMetadata {
    val attributes = m3uAttributeRegex
        .findAll(line.substringBeforeLast(',', line))
        .associate { match -> match.groupValues[1].lowercase() to match.groupValues[2].trim() }
    val displayName = line.substringAfterLast(',', "").trim()
        .ifBlank { attributes["tvg-name"].orEmpty() }

    return ParsedM3uMetadata(
        name = displayName,
        logoUrl = attributes["tvg-logo"]?.takeIf { it.isNotBlank() },
        group = attributes["group-title"].orEmpty(),
    )
}

private fun parseStreamUrl(line: String): ParsedStreamUrl {
    val url = line.substringBefore('|').trim()
    val headers = line.substringAfter('|', "")
        .split('&')
        .mapNotNull { entry ->
            val key = entry.substringBefore('=').trim()
            val value = entry.substringAfter('=', "").trim()
            if (key.isBlank() || value.isBlank()) null else key to value
        }
        .toMap()
    return ParsedStreamUrl(url = url, headers = headers)
}

private fun parseExtHttpHeaders(value: String): Map<String, String> {
    val trimmed = value.trim().removePrefix("{").removeSuffix("}")
    return trimmed.split(',')
        .mapNotNull { entry ->
            val key = entry.substringBefore(':').trim().trim('"')
            val headerValue = entry.substringAfter(':', "").trim().trim('"')
            if (key.isBlank() || headerValue.isBlank()) null else key to headerValue
        }
        .toMap()
}
