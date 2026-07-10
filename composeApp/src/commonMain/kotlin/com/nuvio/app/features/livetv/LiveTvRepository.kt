package com.nuvio.app.features.livetv

import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.addons.httpGetTextWithHeaders
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object LiveTvRepository {
    private val mutableUiState = MutableStateFlow(LiveTvUiState())
    val uiState = mutableUiState.asStateFlow()
    private val epgScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var initialized = false

    fun ensureLoaded() {
        if (initialized) return
        initialized = true
        mutableUiState.value = mutableUiState.value.copy(
            sourceType = LiveTvStorage.loadSourceType(),
            sourceUrl = LiveTvStorage.loadSourceUrl().orEmpty(),
            stalkerSettings = LiveTvStorage.loadStalkerSettings(),
            favoriteUrls = LiveTvStorage.loadFavoriteUrls(),
            recentChannel = LiveTvStorage.loadRecentChannel(),
        )
    }

    fun onProfileChanged() {
        initialized = false
        mutableUiState.value = LiveTvUiState()
        ensureLoaded()
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
            LiveTvStorage.saveLocalPlaylistData("")
            LiveTvStorage.saveSourceType(LiveTvSourceType.M3u)
            mutableUiState.value = LiveTvUiState(
                sourceType = LiveTvSourceType.M3u,
                sourceUrl = normalizedUrl,
                stalkerSettings = mutableUiState.value.stalkerSettings,
                channels = channels,
                favoriteUrls = mutableUiState.value.favoriteUrls,
                recentChannel = mutableUiState.value.recentChannel,
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

    suspend fun loadLocalPlaylist(fileName: String, playlistData: String): Result<List<LiveTvChannel>> {
        val trimmedData = playlistData.trim()
        val displayName = fileName.trim().ifBlank { "Local M3U playlist" }
        if (trimmedData.isBlank()) {
            val error = IllegalArgumentException("Seçilen M3U dosyası boş.")
            mutableUiState.value = mutableUiState.value.copy(errorMessage = error.message)
            return Result.failure(error)
        }

        mutableUiState.value = mutableUiState.value.copy(
            sourceType = LiveTvSourceType.M3u,
            sourceUrl = displayName,
            isLoading = true,
            errorMessage = null,
        )

        return runCatching {
            val playlist = withContext(Dispatchers.Default) {
                parseM3uPlaylistData(trimmedData)
            }
            val channels = playlist.channels
            require(channels.isNotEmpty()) { "Bu M3U dosyasında oynatılabilir kanal bulunamadı." }
            LiveTvStorage.saveSourceUrl(displayName)
            LiveTvStorage.saveLocalPlaylistData(trimmedData)
            LiveTvStorage.saveSourceType(LiveTvSourceType.M3u)
            mutableUiState.value = LiveTvUiState(
                sourceType = LiveTvSourceType.M3u,
                sourceUrl = displayName,
                stalkerSettings = mutableUiState.value.stalkerSettings,
                channels = channels,
                favoriteUrls = mutableUiState.value.favoriteUrls,
                recentChannel = mutableUiState.value.recentChannel,
                isLoaded = true,
            )
            channels
        }.onFailure { error ->
            mutableUiState.value = mutableUiState.value.copy(
                isLoading = false,
                isLoaded = mutableUiState.value.channels.isNotEmpty(),
                errorMessage = error.message ?: "M3U dosyası yüklenemedi.",
            )
        }
    }

    suspend fun loadStoredLocalPlaylist(): Result<List<LiveTvChannel>> {
        val playlistData = LiveTvStorage.loadLocalPlaylistData().orEmpty()
        if (playlistData.isBlank()) {
            return Result.failure(IllegalStateException("Kayıtlı M3U dosyası bulunamadı."))
        }
        return loadLocalPlaylist(
            fileName = LiveTvStorage.loadSourceUrl().orEmpty().ifBlank { "Local M3U playlist" },
            playlistData = playlistData,
        )
    }

    suspend fun loadStalker(settings: LiveTvStalkerSettings): Result<List<LiveTvChannel>> {
        val normalizedSettings = settings.normalized()
        if (!normalizedSettings.isConfigured) {
            val error = IllegalArgumentException("Portal URL ve MAC adresi zorunludur.")
            mutableUiState.value = mutableUiState.value.copy(errorMessage = error.message)
            return Result.failure(error)
        }
        if (!normalizedSettings.portalUrl.startsWith("http://") && !normalizedSettings.portalUrl.startsWith("https://")) {
            val error = IllegalArgumentException("Geçerli bir HTTP veya HTTPS portal bağlantısı girin.")
            mutableUiState.value = mutableUiState.value.copy(errorMessage = error.message)
            return Result.failure(error)
        }

        mutableUiState.value = mutableUiState.value.copy(
            sourceType = LiveTvSourceType.Stalker,
            sourceUrl = normalizedSettings.portalUrl,
            stalkerSettings = normalizedSettings,
            isLoading = true,
            errorMessage = null,
        )

        return runCatching {
            val channels = withContext(Dispatchers.Default) {
                fetchStalkerChannels(normalizedSettings)
            }
            require(channels.isNotEmpty()) { "Bu Stalker Portal içinde oynatılabilir kanal bulunamadı." }
            LiveTvStorage.saveLocalPlaylistData("")
            LiveTvStorage.saveSourceType(LiveTvSourceType.Stalker)
            LiveTvStorage.saveStalkerSettings(normalizedSettings)
            mutableUiState.value = LiveTvUiState(
                sourceType = LiveTvSourceType.Stalker,
                sourceUrl = normalizedSettings.portalUrl,
                stalkerSettings = normalizedSettings,
                channels = channels,
                favoriteUrls = mutableUiState.value.favoriteUrls,
                recentChannel = mutableUiState.value.recentChannel,
                isLoaded = true,
            )
            channels
        }.onFailure { error ->
            mutableUiState.value = mutableUiState.value.copy(
                isLoading = false,
                isLoaded = mutableUiState.value.channels.isNotEmpty(),
                errorMessage = error.message ?: "Stalker Portal yüklenemedi.",
            )
        }
    }

    suspend fun prepareForPlayback(channel: LiveTvChannel): LiveTvChannel =
        if (mutableUiState.value.sourceType == LiveTvSourceType.Stalker && !channel.stalkerCommand.isNullOrBlank()) {
            resolveStalkerPlaybackChannel(channel)
        } else {
            channel
        }

    fun disconnect() {
        LiveTvStorage.saveSourceUrl("")
        LiveTvStorage.saveLocalPlaylistData("")
        LiveTvStorage.saveSourceType(LiveTvSourceType.M3u)
        LiveTvRepositoryStalker.clearSession()
        mutableUiState.value = LiveTvUiState(
            sourceType = LiveTvSourceType.M3u,
            stalkerSettings = LiveTvStorage.loadStalkerSettings(),
            favoriteUrls = mutableUiState.value.favoriteUrls,
            recentChannel = mutableUiState.value.recentChannel,
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

    fun recordRecentChannel(channel: LiveTvChannel) {
        val recentChannel = LiveTvRecentChannel(
            streamUrl = channel.streamUrl,
            name = channel.name,
            logoUrl = channel.logoUrl,
            group = channel.group,
            tvgId = channel.tvgId,
        )
        LiveTvStorage.saveRecentChannel(recentChannel)
        mutableUiState.value = mutableUiState.value.copy(recentChannel = recentChannel)
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
    fun loadSourceType(): LiveTvSourceType
    fun saveSourceType(type: LiveTvSourceType)
    fun loadSourceUrl(): String?
    fun saveSourceUrl(url: String)
    fun loadLocalPlaylistData(): String?
    fun saveLocalPlaylistData(data: String)
    fun loadStalkerSettings(): LiveTvStalkerSettings
    fun saveStalkerSettings(settings: LiveTvStalkerSettings)
    fun loadFavoriteUrls(): Set<String>
    fun saveFavoriteUrls(urls: Set<String>)
    fun loadRecentChannel(): LiveTvRecentChannel?
    fun saveRecentChannel(channel: LiveTvRecentChannel?)
}

private data class StalkerSession(
    val settings: LiveTvStalkerSettings,
    val token: String,
)

private suspend fun fetchStalkerChannels(settings: LiveTvStalkerSettings): List<LiveTvChannel> {
    val session = LiveTvRepositoryStalker.session(settings)
    val genres = LiveTvRepositoryStalker.getGenres(session)
    return LiveTvRepositoryStalker.getChannels(session, genres)
}

private suspend fun resolveStalkerPlaybackChannel(channel: LiveTvChannel): LiveTvChannel {
    val settings = LiveTvRepository.uiState.value.stalkerSettings.normalized()
    if (!settings.isConfigured) return channel
    val session = LiveTvRepositoryStalker.session(settings)
    val resolvedUrl = LiveTvRepositoryStalker.createLink(session, channel.stalkerCommand.orEmpty())
        ?: channel.streamUrl
    return channel.copy(
        streamUrl = resolvedUrl,
        headers = channel.headers + LiveTvRepositoryStalker.playbackHeaders(session),
    )
}

private object LiveTvRepositoryStalker {
    private var cachedSession: StalkerSession? = null

    fun clearSession() {
        cachedSession = null
    }

    suspend fun session(settings: LiveTvStalkerSettings): StalkerSession {
        cachedSession
            ?.takeIf { it.settings == settings && it.token.isNotBlank() }
            ?.let { return it }

        val token = request(settings, type = "stb", action = "handshake")
            .stalkerJs()
            .stringValue("token")
            .orEmpty()
            .trim()
        require(token.isNotBlank()) { "Stalker Portal token alınamadı." }
        return StalkerSession(settings = settings, token = token).also {
            cachedSession = it
        }
    }

    suspend fun getGenres(session: StalkerSession): Map<String, String> {
        val data = request(session.settings, session.token, type = "itv", action = "get_genres")
            .stalkerJs()
            .arrayValue("data")
        return data.associateNotNull { element ->
            val obj = element as? JsonObject ?: return@associateNotNull null
            val id = obj.stringValue("id") ?: obj.stringValue("alias") ?: return@associateNotNull null
            val title = obj.stringValue("title") ?: obj.stringValue("name") ?: return@associateNotNull null
            id to title
        }
    }

    suspend fun getChannels(
        session: StalkerSession,
        genres: Map<String, String>,
    ): List<LiveTvChannel> {
        val channels = mutableListOf<LiveTvChannel>()
        repeat(20) { pageIndex ->
            val page = pageIndex + 1
            val data = request(
                settings = session.settings,
                token = session.token,
                type = "itv",
                action = "get_ordered_list",
                extraParameters = mapOf("p" to page.toString()),
            ).stalkerJs().arrayValue("data")
            if (data.isEmpty()) return@repeat
            data.forEachIndexed { index, element ->
                val obj = element as? JsonObject ?: return@forEachIndexed
                val name = obj.stringValue("name")
                    ?: obj.stringValue("title")
                    ?: return@forEachIndexed
                val command = obj.stringValue("cmd")
                    ?: obj.stringValue("mc_cmd")
                    ?: obj.stringValue("url")
                    ?: return@forEachIndexed
                val streamUrl = command.toStalkerPlayableUrl()
                if (streamUrl.isBlank()) return@forEachIndexed
                val genreId = obj.stringValue("tv_genre_id") ?: obj.stringValue("genre_id")
                channels += LiveTvChannel(
                    id = obj.stringValue("id") ?: "stalker-${page}-$index-${streamUrl.hashCode()}",
                    name = name,
                    streamUrl = streamUrl,
                    tvgId = obj.stringValue("xmltv_id") ?: obj.stringValue("tvg_id"),
                    logoUrl = obj.stringValue("logo") ?: obj.stringValue("logo_url"),
                    group = genreId?.let(genres::get).orEmpty(),
                    headers = playbackHeaders(session),
                    stalkerCommand = command,
                )
            }
        }
        return channels.distinctBy { it.id.ifBlank { it.streamUrl } }
    }

    suspend fun createLink(session: StalkerSession, command: String): String? {
        val data = request(
            settings = session.settings,
            token = session.token,
            type = "itv",
            action = "create_link",
            extraParameters = mapOf("cmd" to command),
        ).stalkerJs()
        return (data.stringValue("cmd") ?: data.stringValue("url") ?: data.stringValue("stream_url"))
            ?.toStalkerPlayableUrl()
            ?.takeIf { it.isNotBlank() }
    }

    fun playbackHeaders(session: StalkerSession): Map<String, String> =
        baseHeaders(session.settings) + mapOf(
            "Authorization" to "Bearer ${session.token}",
        )

    private suspend fun request(
        settings: LiveTvStalkerSettings,
        token: String? = null,
        type: String,
        action: String,
        extraParameters: Map<String, String> = emptyMap(),
    ): JsonObject {
        val parameters = buildMap {
            put("type", type)
            put("action", action)
            put("JsHttpRequest", "1-xml")
            if (!token.isNullOrBlank()) put("token", token)
            if (settings.username.isNotBlank()) put("login", settings.username)
            if (settings.password.isNotBlank()) put("password", settings.password)
            putAll(extraParameters)
        }
        val url = settings.portalEndpoint() + parameters.entries.joinToString(
            separator = "&",
            prefix = if (settings.portalEndpoint().contains("?")) "&" else "?",
        ) { (key, value) ->
            "${key.encodeURLParameter()}=${value.encodeURLParameter()}"
        }
        val payload = httpGetTextWithHeaders(url, baseHeaders(settings) + tokenHeader(token))
        return stalkerJson.parseToJsonElement(payload).jsonObject
    }

    private fun baseHeaders(settings: LiveTvStalkerSettings): Map<String, String> =
        mapOf(
            "User-Agent" to "Mozilla/5.0 (QtEmbedded; U; Linux; MAG254; en) AppleWebKit/533.3 (KHTML, like Gecko) MAG200 stbapp ver: 4 rev: 2721 Mobile Safari/533.3",
            "X-User-Agent" to "Model: MAG254; Link: Ethernet",
            "Referer" to settings.portalBaseUrl(),
            "Cookie" to "mac=${settings.macAddress}; stb_lang=en; timezone=Europe%2FIstanbul",
        )

    private fun tokenHeader(token: String?): Map<String, String> =
        if (token.isNullOrBlank()) emptyMap() else mapOf("Authorization" to "Bearer $token")
}

private val stalkerJson = Json { ignoreUnknownKeys = true; isLenient = true }

private fun LiveTvStalkerSettings.normalized(): LiveTvStalkerSettings =
    copy(
        portalUrl = portalUrl.trim().trimEnd('/'),
        macAddress = macAddress.trim().uppercase(),
        username = username.trim(),
        password = password.trim(),
    )

private fun LiveTvStalkerSettings.portalEndpoint(): String {
    val normalized = portalUrl.trim().trimEnd('/')
    return when {
        normalized.endsWith("portal.php", ignoreCase = true) -> normalized
        normalized.contains("portal.php?", ignoreCase = true) -> normalized
        else -> "$normalized/portal.php"
    }
}

private fun LiveTvStalkerSettings.portalBaseUrl(): String =
    portalUrl.trim().substringBefore("/portal.php").trimEnd('/') + "/c/"

private fun String.toStalkerPlayableUrl(): String =
    trim()
        .removePrefix("ffmpeg ")
        .removePrefix("auto ")
        .substringBefore(' ')
        .trim()

private fun JsonObject.stalkerJs(): JsonObject =
    (this["js"] as? JsonObject) ?: this

private fun JsonObject.stringValue(name: String): String? =
    (this[name] as? JsonPrimitive)?.jsonPrimitive?.contentOrNull?.trim()?.takeIf(String::isNotBlank)

private fun JsonObject.arrayValue(name: String): List<JsonElement> =
    (this[name] as? JsonArray)?.toList().orEmpty()

private inline fun <K, V> Iterable<JsonElement>.associateNotNull(transform: (JsonElement) -> Pair<K, V>?): Map<K, V> =
    mapNotNull(transform).toMap()

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
        channels = channels
            .distinctBy { it.streamUrl }
            .filterNot { isLikelyCategoryHeading(it.name) },
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

internal fun isLikelyCategoryHeading(name: String): Boolean {
    val normalized = name.trim()
    return normalized.length >= 8 && Regex("""^\s*#+\s*.+\s*#+\s*$""").matches(normalized)
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
