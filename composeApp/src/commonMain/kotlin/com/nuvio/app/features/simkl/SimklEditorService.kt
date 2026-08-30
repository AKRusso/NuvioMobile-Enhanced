package com.nuvio.app.features.simkl

import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.tracking.TrackingExternalIds
import com.nuvio.app.features.tracking.TrackingHistoryItem
import com.nuvio.app.features.tracking.TrackingListStatus
import com.nuvio.app.features.tracking.TrackingMediaReference
import com.nuvio.app.features.tracking.TrackingMutationResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

enum class SimklSearchType(val apiValue: String) {
    ANIME("anime"),
    TV("tv"),
    MOVIE("movie"),
}

data class SimklSearchResult(
    val id: Long,
    val title: String,
    val year: Int? = null,
    val poster: String? = null,
    val type: SimklSearchType,
    val format: String? = null,
    val totalEpisodes: Int? = null,
    val ids: Map<String, JsonElement> = emptyMap(),
)

data class SimklEpisodeCoordinate(
    val flatNumber: Int,
    val tvdbSeason: Int? = null,
    val tvdbEpisode: Int? = null,
    val simklEpisodeId: Long? = null,
)

data class SimklEditableEntry(
    val media: TrackingMediaReference,
    val status: TrackingListStatus? = null,
    val progress: Int = 0,
    val score: Int? = null,
    val memo: String? = null,
    val memoIsPrivate: Boolean = true,
    val totalEpisodes: Int? = null,
    val isTracked: Boolean = false,
)

internal class SimklEditorService(
    private val client: SimklApiClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun search(
        query: String,
        type: SimklSearchType = SimklSearchType.ANIME,
        page: Int = 1,
        limit: Int = 20,
    ): List<SimklSearchResult> {
        val normalized = query.trim()
        if (normalized.isEmpty()) return emptyList()
        val body = client.execute(
            SimklApiRequest(
                method = SimklHttpMethod.GET,
                path = "/search/${type.apiValue}",
                query = mapOf(
                    "q" to normalized,
                    "page" to page.coerceAtLeast(1).toString(),
                    "limit" to limit.coerceIn(1, 50).toString(),
                    "extended" to "full",
                ),
                requiresAuthentication = false,
            ),
        ).body
        return json.decodeFromString<List<SimklTextSearchDto>>(body).mapNotNull { it.toResult(type) }
    }

    suspend fun lookup(
        ids: TrackingExternalIds,
        title: String? = null,
        year: Int? = null,
        type: SimklSearchType? = null,
    ): SimklSearchResult? {
        val query = ids.toLookupQuery(type).toMutableMap().apply {
            title?.trim()?.takeIf(String::isNotEmpty)?.let { put("title", it) }
            year?.let { put("year", it.toString()) }
        }
        require(query.isNotEmpty()) { "Simkl lookup requires an ID or title" }
        val body = client.execute(
            SimklApiRequest(
                method = SimklHttpMethod.GET,
                path = "/search/id",
                query = query,
                requiresAuthentication = false,
            ),
        ).body
        return json.decodeFromString<List<SimklIdSearchDto>>(body).firstOrNull()?.toResult()
    }

    suspend fun animeEpisodeCoordinates(simklId: Long): List<SimklEpisodeCoordinate> {
        require(simklId > 0L) { "Simkl ID must be positive" }
        return client.execute(
            SimklApiRequest(
                method = SimklHttpMethod.GET,
                path = "/anime/episodes/$simklId",
                requiresAuthentication = false,
            ),
        ).body.let { body ->
            json.decodeFromString<List<SimklAnimeEpisodeDto>>(body).mapNotNull { episode ->
                episode.episode?.let { flat ->
                    SimklEpisodeCoordinate(
                        flatNumber = flat,
                        tvdbSeason = episode.tvdb?.season,
                        tvdbEpisode = episode.tvdb?.episode,
                        simklEpisodeId = episode.ids?.simklId.longValue(),
                    )
                }
            }
        }
    }

    private fun TrackingExternalIds.toLookupQuery(type: SimklSearchType?): Map<String, String> = buildMap {
        simkl?.let { put("simkl", it.toString()) }
        imdb?.trim()?.takeIf(String::isNotEmpty)?.let { put("imdb", it) }
        tmdb?.let { put("tmdb", it.toString()) }
        tvdb?.trim()?.takeIf(String::isNotEmpty)?.let { put("tvdb", it) }
        mal?.let { put("mal", it.toString()) }
        anidb?.let { put("anidb", it.toString()) }
        anilist?.let { put("anilist", it.toString()) }
        kitsu?.let { put("kitsu", it.toString()) }
        if (tmdb != null && type != null) put("type", if (type == SimklSearchType.MOVIE) "movie" else "show")
    }
}

object SimklEditorRepository {
    private val searchService by lazy { SimklEditorService(SimklApi.client) }

    suspend fun search(
        query: String,
        type: SimklSearchType,
        page: Int = 1,
        limit: Int = 20,
    ): List<SimklSearchResult> = searchService.search(query, type, page, limit)

    suspend fun lookup(
        ids: TrackingExternalIds,
        title: String? = null,
        year: Int? = null,
        type: SimklSearchType? = null,
    ): SimklSearchResult? = searchService.lookup(ids, title, year, type)

    suspend fun searchAnime(query: String, limit: Int = 20): List<SimklSearchResult> =
        search(query, SimklSearchType.ANIME, limit = limit)

    suspend fun lookupAnime(
        ids: TrackingExternalIds,
        title: String? = null,
        year: Int? = null,
    ): SimklSearchResult? = lookup(ids, title, year, SimklSearchType.ANIME)

    suspend fun animeEpisodeCoordinates(simklId: Long): List<SimklEpisodeCoordinate> =
        searchService.animeEpisodeCoordinates(simklId)

    suspend fun loadEntry(media: TrackingMediaReference): SimklEditableEntry {
        SimklSyncRepository.ensureLoaded()
        val entry = SimklSyncRepository.state.value.snapshot.entries.firstOrNull { candidate ->
            candidate.media?.matchesTarget(media.toSimklMedia()) == true
        }
        return SimklEditableEntry(
            media = media,
            status = entry?.status?.toTrackingStatus(),
            progress = entry?.watchedEpisodesCount ?: 0,
            score = entry?.userRating,
            memo = entry?.memo?.text,
            memoIsPrivate = entry?.memo?.isPrivate ?: true,
            totalEpisodes = entry?.totalEpisodesCount?.takeIf { it > 0 },
            isTracked = entry != null,
        )
    }

    suspend fun saveEntry(
        entry: SimklEditableEntry,
        status: TrackingListStatus,
        progress: Int,
        score: Int?,
        memo: String?,
        memoIsPrivate: Boolean,
    ): TrackingMutationResult {
        require(progress >= 0) { "Simkl progress must not be negative" }
        val profileId = ProfileRepository.activeProfileId
        val targetProgress = entry.totalEpisodes?.let(progress::coerceAtMost) ?: progress
        var result = TrackingMutationResult(attemptedCount = 0)
        if (targetProgress > entry.progress) {
            result = SimklMutationRepository.addToHistory(
                profileId = profileId,
                items = ((entry.progress + 1)..targetProgress).map { number ->
                    TrackingHistoryItem(entry.media.withFlatEpisode(number))
                },
            )
        } else if (targetProgress < entry.progress) {
            result = SimklMutationRepository.removeFromHistory(
                profileId = profileId,
                items = ((targetProgress + 1)..entry.progress).map(entry.media::withFlatEpisode),
            )
        }
        val metadataResult = SimklMutationRepository.updateEntry(
            profileId = profileId,
            item = entry.media.copy(episode = null),
            status = status,
            score = score,
            memo = SimklMemo(memo, memoIsPrivate),
        )
        return if (result.attemptedCount == 0) metadataResult else TrackingMutationResult(
            attemptedCount = result.attemptedCount + metadataResult.attemptedCount,
            notFoundCount = result.notFoundCount + metadataResult.notFoundCount,
            resolutions = result.resolutions + metadataResult.resolutions,
        )
    }

    suspend fun deleteEntry(
        media: TrackingMediaReference,
        destructiveDeleteConfirmed: Boolean,
    ): TrackingMutationResult {
        requireSimklDestructiveDeleteConfirmed(destructiveDeleteConfirmed)
        return SimklMutationRepository.deleteEntry(
            profileId = ProfileRepository.activeProfileId,
            item = media.copy(episode = null),
            destructiveDeleteConfirmed = true,
        )
    }
}

private fun TrackingMediaReference.withFlatEpisode(number: Int): TrackingMediaReference = copy(
    episode = com.nuvio.app.features.tracking.TrackingEpisode(number = number),
)

private fun SimklListStatus.toTrackingStatus(): TrackingListStatus = when (this) {
    SimklListStatus.WATCHING -> TrackingListStatus.WATCHING
    SimklListStatus.PLAN_TO_WATCH -> TrackingListStatus.PLAN_TO_WATCH
    SimklListStatus.ON_HOLD -> TrackingListStatus.ON_HOLD
    SimklListStatus.COMPLETED -> TrackingListStatus.COMPLETED
    SimklListStatus.DROPPED -> TrackingListStatus.DROPPED
}

@Serializable
private data class SimklSearchIdsDto(
    @SerialName("simkl_id") val simklId: JsonElement? = null,
    val simkl: JsonElement? = null,
    val slug: JsonElement? = null,
    val imdb: JsonElement? = null,
    val tmdb: JsonElement? = null,
    val tvdb: JsonElement? = null,
    val mal: JsonElement? = null,
    val anidb: JsonElement? = null,
    val anilist: JsonElement? = null,
    val kitsu: JsonElement? = null,
)

@Serializable
private data class SimklTextSearchDto(
    val title: String? = null,
    val year: Int? = null,
    val poster: String? = null,
    val type: String? = null,
    @SerialName("ep_count") val episodeCount: Int? = null,
    @SerialName("total_episodes") val totalEpisodes: Int? = null,
    val ids: SimklSearchIdsDto = SimklSearchIdsDto(),
) {
    fun toResult(searchType: SimklSearchType): SimklSearchResult? {
        val id = ids.simklId.longValue() ?: ids.simkl.longValue() ?: return null
        return SimklSearchResult(
            id = id,
            title = title ?: return null,
            year = year,
            poster = poster,
            type = searchType,
            format = type,
            totalEpisodes = episodeCount ?: totalEpisodes,
            ids = ids.toMap(),
        )
    }
}

@Serializable
private data class SimklIdSearchDto(
    val type: String? = null,
    val title: String? = null,
    val year: Int? = null,
    val poster: String? = null,
    @SerialName("anime_type") val animeType: String? = null,
    @SerialName("total_episodes") val totalEpisodes: Int? = null,
    val ids: SimklSearchIdsDto = SimklSearchIdsDto(),
) {
    fun toResult(): SimklSearchResult? {
        val id = ids.simkl.longValue() ?: ids.simklId.longValue() ?: return null
        val searchType = when (type?.lowercase()) {
            "anime" -> SimklSearchType.ANIME
            "tv", "show" -> SimklSearchType.TV
            "movie", "movies" -> SimklSearchType.MOVIE
            else -> return null
        }
        return SimklSearchResult(
            id,
            title ?: return null,
            year,
            poster,
            searchType,
            animeType,
            totalEpisodes,
            ids.toMap(),
        )
    }
}

private fun SimklSearchIdsDto.toMap(): Map<String, JsonElement> = buildMap {
    (simkl ?: simklId)?.let { put("simkl", it) }
    slug?.let { put("slug", it) }
    imdb?.let { put("imdb", it) }
    tmdb?.let { put("tmdb", it) }
    tvdb?.let { put("tvdb", it) }
    mal?.let { put("mal", it) }
    anidb?.let { put("anidb", it) }
    anilist?.let { put("anilist", it) }
    kitsu?.let { put("kitsu", it) }
}

private fun JsonElement?.longValue(): Long? =
    (this as? JsonPrimitive)?.contentOrNull?.trim()?.toLongOrNull()

@Serializable
private data class SimklAnimeEpisodeDto(
    val episode: Int? = null,
    val tvdb: SimklEpisodeMapping? = null,
    val ids: SimklSearchIdsDto? = null,
)
