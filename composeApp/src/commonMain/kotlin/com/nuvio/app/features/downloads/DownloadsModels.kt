package com.nuvio.app.features.downloads

import com.nuvio.app.core.i18n.localizedByteUnit
import com.nuvio.app.features.details.MetaCompany
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaExternalRating
import com.nuvio.app.features.details.MetaPerson
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.streams.StreamSubtitle
import kotlinx.serialization.Serializable
import kotlinx.coroutines.runBlocking
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.downloads_enqueue_missing_url
import nuvio.composeapp.generated.resources.downloads_enqueue_replaced
import nuvio.composeapp.generated.resources.downloads_enqueue_started
import nuvio.composeapp.generated.resources.downloads_enqueue_unsupported_format
import org.jetbrains.compose.resources.getString

@Serializable
enum class DownloadStatus {
    Downloading,
    Paused,
    Completed,
    Failed,
}

@Serializable
data class DownloadItem(
    val id: String,
    val contentType: String,
    val parentMetaId: String,
    val parentMetaType: String,
    val videoId: String,
    val title: String,
    val logo: String? = null,
    val poster: String? = null,
    val background: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTitle: String? = null,
    val episodeThumbnail: String? = null,
    val episodeOverview: String? = null,
    val detailsSnapshot: DownloadDetailsSnapshot? = null,
    val streamTitle: String,
    val streamSubtitle: String? = null,
    val providerName: String,
    val providerAddonId: String? = null,
    val externalSubtitles: List<StreamSubtitle> = emptyList(),
    val sourceUrl: String,
    val sourceHeaders: Map<String, String> = emptyMap(),
    val sourceResponseHeaders: Map<String, String> = emptyMap(),
    val localFileUri: String? = null,
    val fileName: String,
    val status: DownloadStatus,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val downloadSpeedBytesPerSecond: Long = 0L,
    val errorMessage: String? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
) {
    val isEpisode: Boolean
        get() = seasonNumber != null && episodeNumber != null

    val isPlayable: Boolean
        get() = status == DownloadStatus.Completed && !localFileUri.isNullOrBlank()

    val displaySubtitle: String
        get() = episodeTitle.orEmpty()

    val progressFraction: Float
        get() {
            val total = totalBytes?.takeIf { it > 0L } ?: return 0f
            return (downloadedBytes.toDouble() / total.toDouble())
                .toFloat()
                .coerceIn(0f, 1f)
        }

    val estimatedRemainingSeconds: Long?
        get() {
            if (status != DownloadStatus.Downloading) return null
            val total = totalBytes?.takeIf { it > 0L } ?: return null
            val speed = downloadSpeedBytesPerSecond.takeIf { it > 0L } ?: return null
            val remainingBytes = (total - downloadedBytes).coerceAtLeast(0L)
            if (remainingBytes <= 0L) return 0L
            return (remainingBytes + speed - 1L) / speed
        }

    val logicalContentKey: String
        get() = if (isEpisode) {
            "${parentMetaId.trim()}|${seasonNumber ?: -1}|${episodeNumber ?: -1}"
        } else {
            "${parentMetaId.trim()}|movie"
        }
}

@Serializable
data class DownloadDetailsSnapshot(
    val id: String,
    val type: String,
    val name: String,
    val aliases: List<String> = emptyList(),
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val lastAirDate: String? = null,
    val status: String? = null,
    val imdbRating: String? = null,
    val ageRating: String? = null,
    val runtime: String? = null,
    val externalRatings: List<DownloadExternalRatingSnapshot> = emptyList(),
    val genres: List<String> = emptyList(),
    val director: List<String> = emptyList(),
    val writer: List<String> = emptyList(),
    val cast: List<DownloadPersonSnapshot> = emptyList(),
    val productionCompanies: List<DownloadCompanySnapshot> = emptyList(),
    val networks: List<DownloadCompanySnapshot> = emptyList(),
    val country: String? = null,
    val awards: String? = null,
    val language: String? = null,
    val website: String? = null,
    val hasScheduledVideos: Boolean = false,
    val videos: List<DownloadVideoSnapshot> = emptyList(),
)

@Serializable
data class DownloadExternalRatingSnapshot(
    val source: String,
    val value: Double,
)

@Serializable
data class DownloadPersonSnapshot(
    val name: String,
    val role: String? = null,
    val photo: String? = null,
    val tmdbId: Int? = null,
)

@Serializable
data class DownloadCompanySnapshot(
    val name: String,
    val logo: String? = null,
    val tmdbId: Int? = null,
)

@Serializable
data class DownloadVideoSnapshot(
    val id: String,
    val title: String,
    val released: String? = null,
    val available: Boolean = true,
    val thumbnail: String? = null,
    val seasonPoster: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val overview: String? = null,
    val runtime: Int? = null,
)

data class DownloadsUiState(
    val items: List<DownloadItem> = emptyList(),
) {
    val activeItems: List<DownloadItem>
        get() = items.filter { it.status != DownloadStatus.Completed }

    val completedItems: List<DownloadItem>
        get() = items.filter { it.status == DownloadStatus.Completed }
}

enum class DownloadEnqueueResult {
    Started,
    Replaced,
    MissingUrl,
    UnsupportedFormat;

    fun toastMessage(): String = runBlocking {
        when (this@DownloadEnqueueResult) {
            Started -> getString(Res.string.downloads_enqueue_started)
            Replaced -> getString(Res.string.downloads_enqueue_replaced)
            MissingUrl -> getString(Res.string.downloads_enqueue_missing_url)
            UnsupportedFormat -> getString(Res.string.downloads_enqueue_unsupported_format)
        }
    }
}

internal fun List<DownloadItem>.sortedForSeriesDownloads(): List<DownloadItem> =
    sortedWith(downloadSeriesEpisodeComparator)

internal val downloadSeriesEpisodeComparator: Comparator<DownloadItem> =
    compareBy<DownloadItem> { it.seasonNumber ?: Int.MAX_VALUE }
        .thenBy { it.episodeNumber ?: Int.MAX_VALUE }
        .thenBy { it.episodeTitle?.trim().orEmpty().lowercase() }
        .thenBy { it.title.trim().lowercase() }
        .thenBy { it.id }

internal fun DownloadItem.downloadSizeLabel(): String {
    val downloaded = formatDownloadBytes(downloadedBytes)
    val total = totalBytes?.takeIf { it > 0L }?.let(::formatDownloadBytes)
    return if (total != null) "$downloaded / $total" else downloaded
}

internal fun DownloadItem.downloadSpeedLabel(): String? =
    downloadSpeedBytesPerSecond
        .takeIf { status == DownloadStatus.Downloading && it > 0L }
        ?.let { "⚡ ${formatDownloadBytes(it)}/s" }

internal fun DownloadItem.downloadEtaLabel(): String? =
    estimatedRemainingSeconds
        ?.let { "ETA ${formatDownloadDuration(it)}" }

internal fun DownloadItem.downloadProgressInfoLines(): List<String> =
    listOfNotNull(
        downloadSpeedLabel(),
        downloadEtaLabel(),
        downloadSizeLabel().takeIf { it.isNotBlank() },
    )

internal fun formatDownloadBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 ${localizedByteUnit("B")}"
    val kib = 1024.0
    val mib = kib * 1024.0
    val gib = mib * 1024.0
    val value = bytes.toDouble()
    return when {
        value >= gib -> "${((value / gib) * 10.0).toInt() / 10.0} ${localizedByteUnit("GB")}"
        value >= mib -> "${((value / mib) * 10.0).toInt() / 10.0} ${localizedByteUnit("MB")}"
        value >= kib -> "${((value / kib) * 10.0).toInt() / 10.0} ${localizedByteUnit("KB")}"
        else -> "$bytes ${localizedByteUnit("B")}"
    }
}

private fun formatDownloadDuration(seconds: Long): String {
    val safeSeconds = seconds.coerceAtLeast(0L)
    if (safeSeconds < 60L) return "${safeSeconds}s"
    val minutes = safeSeconds / 60L
    val remainingSeconds = safeSeconds % 60L
    if (minutes < 60L) {
        return if (remainingSeconds > 0L && minutes < 10L) {
            "${minutes}m ${remainingSeconds}s"
        } else {
            "${minutes}m"
        }
    }
    val hours = minutes / 60L
    val remainingMinutes = minutes % 60L
    return if (remainingMinutes > 0L) {
        "${hours}h ${remainingMinutes}m"
    } else {
        "${hours}h"
    }
}

fun MetaDetails.toDownloadDetailsSnapshot(): DownloadDetailsSnapshot =
    DownloadDetailsSnapshot(
        id = id,
        type = type,
        name = name,
        aliases = aliases,
        poster = poster,
        background = background,
        logo = logo,
        description = description,
        releaseInfo = releaseInfo,
        lastAirDate = lastAirDate,
        status = status,
        imdbRating = imdbRating,
        ageRating = ageRating,
        runtime = runtime,
        externalRatings = externalRatings.map { it.toDownloadSnapshot() },
        genres = genres,
        director = director,
        writer = writer,
        cast = cast.map { it.toDownloadSnapshot() },
        productionCompanies = productionCompanies.map { it.toDownloadSnapshot() },
        networks = networks.map { it.toDownloadSnapshot() },
        country = country,
        awards = awards,
        language = language,
        website = website,
        hasScheduledVideos = hasScheduledVideos,
        videos = videos.map { it.toDownloadSnapshot() },
    )

fun DownloadItem.toOfflineMetaDetails(): MetaDetails? {
    val snapshot = detailsSnapshot ?: return null
    return snapshot.toMetaDetails(
        fallbackItem = this,
    )
}

private fun DownloadDetailsSnapshot.toMetaDetails(
    fallbackItem: DownloadItem,
): MetaDetails =
    MetaDetails(
        id = id.ifBlank { fallbackItem.parentMetaId },
        type = type.ifBlank { fallbackItem.parentMetaType.ifBlank { fallbackItem.contentType } },
        name = name.ifBlank { fallbackItem.title },
        aliases = aliases,
        poster = poster ?: fallbackItem.poster,
        background = background ?: fallbackItem.background,
        logo = logo ?: fallbackItem.logo,
        description = description ?: fallbackItem.episodeOverview,
        releaseInfo = releaseInfo,
        lastAirDate = lastAirDate,
        status = status,
        imdbRating = imdbRating,
        ageRating = ageRating,
        runtime = runtime,
        externalRatings = externalRatings.map { it.toMetaExternalRating() },
        genres = genres,
        director = director,
        writer = writer,
        cast = cast.map { it.toMetaPerson() },
        productionCompanies = productionCompanies.map { it.toMetaCompany() },
        networks = networks.map { it.toMetaCompany() },
        country = country,
        awards = awards,
        language = language,
        website = website,
        hasScheduledVideos = hasScheduledVideos,
        videos = videos.map { it.toMetaVideo() },
    )

private fun MetaExternalRating.toDownloadSnapshot(): DownloadExternalRatingSnapshot =
    DownloadExternalRatingSnapshot(
        source = source,
        value = value,
    )

private fun DownloadExternalRatingSnapshot.toMetaExternalRating(): MetaExternalRating =
    MetaExternalRating(
        source = source,
        value = value,
    )

private fun MetaPerson.toDownloadSnapshot(): DownloadPersonSnapshot =
    DownloadPersonSnapshot(
        name = name,
        role = role,
        photo = photo,
        tmdbId = tmdbId,
    )

private fun DownloadPersonSnapshot.toMetaPerson(): MetaPerson =
    MetaPerson(
        name = name,
        role = role,
        photo = photo,
        tmdbId = tmdbId,
    )

private fun MetaCompany.toDownloadSnapshot(): DownloadCompanySnapshot =
    DownloadCompanySnapshot(
        name = name,
        logo = logo,
        tmdbId = tmdbId,
    )

private fun DownloadCompanySnapshot.toMetaCompany(): MetaCompany =
    MetaCompany(
        name = name,
        logo = logo,
        tmdbId = tmdbId,
    )

private fun MetaVideo.toDownloadSnapshot(): DownloadVideoSnapshot =
    DownloadVideoSnapshot(
        id = id,
        title = title,
        released = released,
        available = available,
        thumbnail = thumbnail,
        seasonPoster = seasonPoster,
        season = season,
        episode = episode,
        overview = overview,
        runtime = runtime,
    )

private fun DownloadVideoSnapshot.toMetaVideo(): MetaVideo =
    MetaVideo(
        id = id,
        title = title,
        released = released,
        available = available,
        thumbnail = thumbnail,
        seasonPoster = seasonPoster,
        season = season,
        episode = episode,
        overview = overview,
        runtime = runtime,
    )
