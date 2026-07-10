package com.nuvio.app.features.cloudstream

import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.PosterShape
import com.nuvio.app.features.streams.StreamBehaviorHints
import com.nuvio.app.features.streams.StreamItem
import com.nuvio.app.features.streams.StreamProxyHeaders
import com.nuvio.app.features.streams.StreamSubtitle

private const val cloudStreamRoutePrefix = "cloudstream:"

data class CloudStreamRouteData(
    val providerId: String,
    val data: String,
)

fun cloudStreamRouteId(providerId: String, data: String): String =
    "$cloudStreamRoutePrefix${providerId.length}:$providerId$data"

fun parseCloudStreamRouteId(value: String): CloudStreamRouteData? {
    if (!value.startsWith(cloudStreamRoutePrefix)) return null
    val payload = value.removePrefix(cloudStreamRoutePrefix)
    val separator = payload.indexOf(':')
    if (separator <= 0) return null
    val providerLength = payload.substring(0, separator).toIntOrNull() ?: return null
    val content = payload.substring(separator + 1)
    if (providerLength <= 0 || providerLength > content.length) return null
    val providerId = content.substring(0, providerLength)
    val data = content.substring(providerLength)
    if (providerId.isBlank() || data.isBlank()) return null
    return CloudStreamRouteData(providerId = providerId, data = data)
}

fun CloudStreamSearchItem.toMetaPreview(): MetaPreview = MetaPreview(
    id = cloudStreamRouteId(providerId, data),
    type = type.nuvioType,
    name = name,
    poster = posterUrl,
    banner = backgroundUrl,
    posterShape = if (type == CloudStreamTvType.Live) PosterShape.Landscape else PosterShape.Poster,
    description = description,
    releaseInfo = year?.toString(),
)

fun CloudStreamLoadItem.toMetaDetails(): MetaDetails {
    val routeId = cloudStreamRouteId(providerId, data)
    val sortedEpisodes = sortCloudStreamEpisodes(episodes)
    val videos = if (sortedEpisodes.isEmpty()) {
        listOf(
            MetaVideo(
                id = routeId,
                title = if (type == CloudStreamTvType.Live) "Canlı yayın" else name,
                available = true,
                thumbnail = backgroundUrl ?: posterUrl,
            ),
        )
    } else {
        sortedEpisodes.map { episode ->
            MetaVideo(
                id = cloudStreamRouteId(providerId, episode.data),
                title = episode.name,
                available = true,
                thumbnail = episode.posterUrl,
                season = episode.season,
                episode = episode.episode,
                overview = episode.description,
            )
        }
    }
    return MetaDetails(
        id = routeId,
        type = type.nuvioType,
        name = name,
        poster = posterUrl,
        background = backgroundUrl,
        description = description,
        releaseInfo = year?.toString(),
        imdbRating = ratingPercent?.let { rating ->
            val score = rating.coerceIn(0, 100) / 10.0
            if (score % 1.0 == 0.0) score.toInt().toString() else score.toString()
        },
        genres = tags,
        videos = videos,
    )
}

fun CloudStreamPlaybackSource.toStreamItem(
    providerId: String,
    providerName: String,
): StreamItem {
    val requestHeaders = buildMap {
        putAll(headers)
        referer?.takeIf { it.isNotBlank() }?.let { put("Referer", it) }
    }
    val qualityLabel = quality?.takeIf { it > 0 }?.let { "${it}p" }
    return StreamItem(
        name = listOfNotNull(name.takeIf { it.isNotBlank() }, qualityLabel).joinToString(" · "),
        title = name,
        url = url,
        sourceName = name,
        addonName = providerName,
        addonId = "cloudstream:${sha256Hex(providerId.encodeToByteArray()).take(16)}",
        streamType = when {
            isHls -> "hls"
            isDash -> "dash"
            else -> "direct"
        },
        behaviorHints = StreamBehaviorHints(
            notWebReady = requestHeaders.isNotEmpty(),
            proxyHeaders = requestHeaders.takeIf { it.isNotEmpty() }?.let { StreamProxyHeaders(request = it) },
        ),
        externalSubtitles = subtitles.map { subtitle ->
            StreamSubtitle(
                url = subtitle.url,
                language = subtitle.language,
                name = subtitle.name,
                headers = subtitle.headers.takeIf { it.isNotEmpty() },
            )
        },
    )
}

