package com.nuvio.app.features.livetv

import com.nuvio.app.features.addons.httpGetText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object LiveTvRepository {
    private val mutableUiState = MutableStateFlow(LiveTvUiState())
    val uiState = mutableUiState.asStateFlow()
    private val epgScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var initialized = false

    fun ensureLoaded() {
        if (initialized) return
        initialized = true
        mutableUiState.value = mutableUiState.value.copy(
            sourceUrl = LiveTvStorage.loadSourceUrl().orEmpty(),
            favoriteUrls = LiveTvStorage.loadFavoriteUrls(),
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
            val playlist = withContext(Dispatchers.Default) {
                parseM3uPlaylistData(httpGetText(normalizedUrl))
            }
            val channels = playlist.channels
            require(channels.isNotEmpty()) { "Bu M3U listesinde oynatılabilir kanal bulunamadı." }
            LiveTvStorage.saveSourceUrl(normalizedUrl)
            mutableUiState.value = LiveTvUiState(
                sourceUrl = normalizedUrl,
                channels = channels,
                favoriteUrls = mutableUiState.value.favoriteUrls,
                isEpgLoading = playlist.epgUrls.isNotEmpty(),
                isLoaded = true,
            )
            loadEpgInBackground(normalizedUrl, playlist.epgUrls)
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
        mutableUiState.value = LiveTvUiState(
            favoriteUrls = mutableUiState.value.favoriteUrls,
        )
    }

    fun toggleFavorite(channel: LiveTvChannel) {
        val favorites = mutableUiState.value.favoriteUrls.toMutableSet()
        if (!favorites.add(channel.streamUrl)) {
            favorites.remove(channel.streamUrl)
        }
        LiveTvStorage.saveFavoriteUrls(favorites)
        mutableUiState.value = mutableUiState.value.copy(favoriteUrls = favorites)
    }

    private fun loadEpgInBackground(sourceUrl: String, epgUrls: List<String>) {
        if (epgUrls.isEmpty()) return
        epgScope.launch {
            val programmes = epgUrls
                .mapNotNull { epgUrl ->
                    runCatching {
                        parseCurrentXmlTvProgrammes(httpGetText(epgUrl))
                    }.getOrNull()
                }
                .fold(emptyMap<String, LiveTvProgramme>()) { merged, entries -> merged + entries }
            if (mutableUiState.value.sourceUrl == sourceUrl) {
                mutableUiState.value = mutableUiState.value.copy(
                    currentProgrammes = programmes,
                    isEpgLoading = false,
                )
            }
        }
    }
}

internal expect object LiveTvStorage {
    fun loadSourceUrl(): String?
    fun saveSourceUrl(url: String)
    fun loadFavoriteUrls(): Set<String>
    fun saveFavoriteUrls(urls: Set<String>)
}

internal fun parseM3uPlaylist(content: String): List<LiveTvChannel> =
    parseM3uPlaylistData(content).channels

internal data class ParsedM3uPlaylist(
    val channels: List<LiveTvChannel>,
    val epgUrls: List<String>,
)

internal fun parseM3uPlaylistData(content: String): ParsedM3uPlaylist {
    val channels = mutableListOf<LiveTvChannel>()
    val epgUrls = linkedSetOf<String>()
    var metadata: ParsedM3uMetadata? = null
    var pendingHeaders = emptyMap<String, String>()

    content.lineSequence().forEach { rawLine ->
        val line = rawLine.trim().removePrefix("\uFEFF")
        when {
            line.startsWith("#EXTM3U", ignoreCase = true) -> {
                val attributes = parseM3uAttributes(line)
                listOfNotNull(attributes["url-tvg"], attributes["x-tvg-url"])
                    .flatMap { it.split(',', ';') }
                    .map(String::trim)
                    .filter { it.startsWith("http://") || it.startsWith("https://") }
                    .forEach(epgUrls::add)
            }

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
                    tvgId = null,
                    logoUrl = null,
                    group = "",
                )
                channels += LiveTvChannel(
                    id = "${parsedUrl.url}#${channels.size}",
                    name = current.name.ifBlank { "Kanal ${channels.size + 1}" },
                    streamUrl = parsedUrl.url,
                    tvgId = current.tvgId,
                    logoUrl = current.logoUrl,
                    group = current.group,
                    headers = pendingHeaders + parsedUrl.headers,
                )
                metadata = null
                pendingHeaders = emptyMap()
            }
        }
    }

    return ParsedM3uPlaylist(
        channels = channels.distinctBy { it.streamUrl },
        epgUrls = epgUrls.toList(),
    )
}

private data class ParsedM3uMetadata(
    val name: String,
    val tvgId: String?,
    val logoUrl: String?,
    val group: String,
)

private data class ParsedStreamUrl(
    val url: String,
    val headers: Map<String, String>,
)

private val m3uAttributeRegex = Regex("""([\w-]+)="([^"]*)"""")

private fun parseExtInf(line: String): ParsedM3uMetadata {
    val attributes = parseM3uAttributes(line.substringBeforeLast(',', line))
    val displayName = line.substringAfterLast(',', "").trim()
        .ifBlank { attributes["tvg-name"].orEmpty() }

    return ParsedM3uMetadata(
        name = displayName,
        tvgId = attributes["tvg-id"]?.takeIf(String::isNotBlank),
        logoUrl = attributes["tvg-logo"]?.takeIf { it.isNotBlank() },
        group = attributes["group-title"].orEmpty(),
    )
}

private fun parseM3uAttributes(line: String): Map<String, String> =
    m3uAttributeRegex
        .findAll(line)
        .associate { match -> match.groupValues[1].lowercase() to match.groupValues[2].trim() }

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

internal expect object LiveTvClock {
    fun nowEpochMs(): Long
    fun parseXmlTvTimestamp(value: String): Long?
}

private val xmlTvProgrammeRegex = Regex(
    """<programme\b([^>]*)>([\s\S]*?)</programme>""",
    RegexOption.IGNORE_CASE,
)
private val xmlTvTitleRegex = Regex(
    """<title\b[^>]*>([\s\S]*?)</title>""",
    RegexOption.IGNORE_CASE,
)
private val xmlAttributeRegex = Regex("""([\w-]+)="([^"]*)"""")

internal fun parseCurrentXmlTvProgrammes(
    content: String,
    nowEpochMs: Long = LiveTvClock.nowEpochMs(),
): Map<String, LiveTvProgramme> {
    val programmes = mutableMapOf<String, LiveTvProgramme>()
    xmlTvProgrammeRegex.findAll(content).forEach { match ->
        val attributes = xmlAttributeRegex.findAll(match.groupValues[1])
            .associate { attribute -> attribute.groupValues[1].lowercase() to attribute.groupValues[2] }
        val channelId = attributes["channel"]?.trim()?.takeIf(String::isNotBlank) ?: return@forEach
        val rawStart = attributes["start"].orEmpty()
        val rawStop = attributes["stop"].orEmpty()
        val startEpochMs = LiveTvClock.parseXmlTvTimestamp(rawStart) ?: return@forEach
        val stopEpochMs = LiveTvClock.parseXmlTvTimestamp(rawStop) ?: return@forEach
        if (nowEpochMs !in startEpochMs until stopEpochMs) return@forEach
        val title = xmlTvTitleRegex.find(match.groupValues[2])
            ?.groupValues
            ?.get(1)
            ?.decodeXmlEntities()
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: return@forEach
        programmes[channelId] = LiveTvProgramme(
            title = title,
            startEpochMs = startEpochMs,
            stopEpochMs = stopEpochMs,
            timeLabel = "${rawStart.xmlTvTimePart()} - ${rawStop.xmlTvTimePart()}",
        )
    }
    return programmes
}

private fun String.xmlTvTimePart(): String {
    val digits = takeWhile(Char::isDigit)
    return if (digits.length >= 12) "${digits.substring(8, 10)}:${digits.substring(10, 12)}" else ""
}

private fun String.decodeXmlEntities(): String =
    replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
