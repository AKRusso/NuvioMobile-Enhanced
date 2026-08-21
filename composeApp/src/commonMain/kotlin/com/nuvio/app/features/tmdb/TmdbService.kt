package com.nuvio.app.features.tmdb

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.PosterShape
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object TmdbService {
    private val log = Logger.withTag("TmdbService")
    private val json = Json { ignoreUnknownKeys = true }
    private val imdbToTmdbCache = linkedMapOf<String, String>()
    private val tmdbToImdbCache = linkedMapOf<String, String>()
    private val cacheMutex = Mutex()

    suspend fun ensureTmdbId(videoId: String, mediaType: String): String? {
        val apiKey = currentApiKey() ?: return null

        val normalized = videoId
            .removePrefix("tmdb:")
            .removePrefix("movie:")
            .removePrefix("series:")
            .substringBefore(':')
            .substringBefore('/')
            .trim()

        if (normalized.isBlank()) return null
        if (normalized.all(Char::isDigit)) return normalized
        if (!normalized.startsWith("tt", ignoreCase = true)) return null

        return imdbToTmdb(imdbId = normalized, mediaType = mediaType, apiKey = apiKey)
    }

    suspend fun tmdbToImdb(tmdbId: Int, mediaType: String): String? {
        val apiKey = currentApiKey() ?: return null

        val cacheKey = "$tmdbId:${normalizeMediaType(mediaType)}"
        cacheMutex.withLock {
            tmdbToImdbCache[cacheKey]?.let { return it }
        }

        val endpoint = when (normalizeMediaType(mediaType)) {
            "tv" -> "tv/$tmdbId/external_ids"
            else -> "movie/$tmdbId/external_ids"
        }
        val body = fetch<TmdbExternalIdsResponse>(endpoint = endpoint, apiKey = apiKey) ?: return null
        val imdbId = body.imdbId?.trim()?.takeIf(String::isNotBlank) ?: return null

        cacheMutex.withLock {
            tmdbToImdbCache[cacheKey] = imdbId
            imdbToTmdbCache["$imdbId:${normalizeMediaType(mediaType)}"] = tmdbId.toString()
        }
        return imdbId
    }

    suspend fun getEpisodeRatings(
        tmdbId: Int,
        seasons: Collection<Int>,
    ): Map<Pair<Int, Int>, Double> {
        val apiKey = currentApiKey() ?: return emptyMap()
        // TMDB uses season 0 for specials, which do not belong in the regular episode matrix.
        val seasonNumbers = seasons.filter { it > 0 }.distinct().sorted()
        if (tmdbId <= 0 || seasonNumbers.isEmpty()) return emptyMap()

        return buildMap {
            seasonNumbers.forEach { season ->
                val details = fetch<TmdbSeasonRatingsResponse>(
                    endpoint = "tv/$tmdbId/season/$season",
                    apiKey = apiKey,
                ) ?: return@forEach
                details.episodes.forEach { episode ->
                    val number = episode.episodeNumber ?: return@forEach
                    val rating = episode.voteAverage?.takeIf { it > 0.0 } ?: return@forEach
                    put(season to number, rating)
                }
            }
        }
    }

    suspend fun search(query: String, limit: Int = 24): List<MetaPreview> {
        val apiKey = currentApiKey() ?: return emptyList()
        val normalizedQuery = query.trim().takeIf { it.isNotBlank() } ?: return emptyList()
        val language = TmdbSettingsRepository.snapshot().language.takeIf { it.isNotBlank() } ?: "en"
        val body = fetch<TmdbSearchResponse>(
            endpoint = "search/multi",
            apiKey = apiKey,
            query = mapOf(
                "query" to normalizedQuery,
                "language" to language,
                "include_adult" to "false",
            ),
        ) ?: return emptyList()

        return body.results
            .asSequence()
            .filter { result -> result.mediaType == "movie" || result.mediaType == "tv" }
            .mapNotNull { result -> result.toMetaPreview() }
            .take(limit)
            .toList()
    }

    suspend fun searchPeople(query: String, limit: Int = 12): List<TmdbPersonSearchResult> {
        val apiKey = currentApiKey() ?: return emptyList()
        val normalizedQuery = query.trim().takeIf { it.length >= 2 } ?: return emptyList()
        val language = TmdbSettingsRepository.snapshot().language.takeIf { it.isNotBlank() } ?: "en"
        val body = fetch<TmdbPersonSearchResponse>(
            endpoint = "search/person",
            apiKey = apiKey,
            query = mapOf(
                "query" to normalizedQuery,
                "language" to language,
                "include_adult" to "false",
            ),
        ) ?: return emptyList()

        return body.results
            .asSequence()
            .mapNotNull { result ->
                val mapped = result.toPersonSearchResult() ?: return@mapNotNull null
                val score = personSearchScore(normalizedQuery, mapped.name)
                if (score <= 0) return@mapNotNull null
                scoredPerson(mapped, score)
            }
            .sortedWith(
                compareByDescending<ScoredPerson> { it.score }
                    .thenByDescending { it.person.popularity },
            )
            .map { it.person }
            .take(limit)
            .toList()
    }

    private suspend fun imdbToTmdb(imdbId: String, mediaType: String, apiKey: String): String? {
        val normalizedType = normalizeMediaType(mediaType)
        val cacheKey = "$imdbId:$normalizedType"
        cacheMutex.withLock {
            imdbToTmdbCache[cacheKey]?.let { return it }
        }

        val body = fetch<TmdbFindResponse>(
            endpoint = "find/$imdbId",
            apiKey = apiKey,
            query = mapOf("external_source" to "imdb_id"),
        ) ?: return null

        val resultId = when (normalizedType) {
            "movie" -> body.movieResults.firstOrNull()?.id
            "tv" -> body.tvResults.firstOrNull()?.id
            else -> body.movieResults.firstOrNull()?.id ?: body.tvResults.firstOrNull()?.id
        }?.takeIf { it > 0 }?.toString()

        if (resultId != null) {
            cacheMutex.withLock {
                imdbToTmdbCache[cacheKey] = resultId
                tmdbToImdbCache["$resultId:$normalizedType"] = imdbId
            }
        } else {
            log.d { "No TMDB ID found for $imdbId ($normalizedType)" }
        }

        return resultId
    }

    private suspend inline fun <reified T> fetch(
        endpoint: String,
        apiKey: String,
        query: Map<String, String> = emptyMap(),
    ): T? {
        val url = buildTmdbUrl(endpoint = endpoint, apiKey = apiKey, query = query)
        return runCatching {
            json.decodeFromString<T>(httpGetText(url))
        }.onFailure { error ->
            log.w { "TMDB request failed for $endpoint: ${error.message}" }
        }.getOrNull()
    }

    private fun currentApiKey(): String? =
        TmdbSettingsRepository.snapshot().apiKey.trim().takeIf(String::isNotBlank)

    internal fun normalizeMediaType(mediaType: String): String =
        when (mediaType.trim().lowercase()) {
            "movie", "film" -> "movie"
            "tv", "series", "show", "tvshow", "dizi" -> "tv"
            else -> mediaType.trim().lowercase()
        }
}

internal fun buildTmdbUrl(
    endpoint: String,
    apiKey: String,
    query: Map<String, String> = emptyMap(),
): String {
    val params = linkedMapOf("api_key" to apiKey)
    query.forEach { (key, value) ->
        if (value.isNotBlank()) {
            params[key] = value
        }
    }
    return buildString {
        append("https://api.themoviedb.org/3/")
        append(endpoint.removePrefix("/"))
        if (params.isNotEmpty()) {
            append("?")
            append(params.entries.joinToString("&") { (key, value) ->
                "${key.encodeURLParameter()}=${value.encodeURLParameter()}"
            })
        }
    }
}

private fun buildTmdbImageUrl(path: String?, size: String): String? {
    val clean = path?.trim()?.takeIf { it.isNotBlank() } ?: return null
    if (clean.startsWith("http://") || clean.startsWith("https://")) return clean
    return "https://image.tmdb.org/t/p/$size/${clean.removePrefix("/")}"
}

@Serializable
private data class TmdbFindResponse(
    @SerialName("movie_results") val movieResults: List<TmdbExternalResult> = emptyList(),
    @SerialName("tv_results") val tvResults: List<TmdbExternalResult> = emptyList(),
)

@Serializable
private data class TmdbExternalResult(
    val id: Int,
)

@Serializable
private data class TmdbExternalIdsResponse(
    @SerialName("imdb_id") val imdbId: String? = null,
)

@Serializable
private data class TmdbSeasonRatingsResponse(
    val episodes: List<TmdbEpisodeRatingResponse> = emptyList(),
)

@Serializable
private data class TmdbEpisodeRatingResponse(
    @SerialName("episode_number") val episodeNumber: Int? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
)

@Serializable
private data class TmdbSearchResponse(
    val results: List<TmdbSearchResult> = emptyList(),
)

@Serializable
private data class TmdbSearchResult(
    val id: Int,
    @SerialName("media_type") val mediaType: String? = null,
    val title: String? = null,
    val name: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    val overview: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
) {
    fun toMetaPreview(): MetaPreview? {
        val normalizedType = when (mediaType) {
            "tv" -> "series"
            "movie" -> "movie"
            else -> return null
        }
        val displayName = title?.takeIf { it.isNotBlank() }
            ?: name?.takeIf { it.isNotBlank() }
            ?: originalTitle?.takeIf { it.isNotBlank() }
            ?: originalName?.takeIf { it.isNotBlank() }
            ?: return null
        val release = when (mediaType) {
            "tv" -> firstAirDate
            else -> releaseDate
        }?.takeIf { it.isNotBlank() }
        val rating = voteAverage
            ?.takeIf { score -> score > 0.0 }
            ?.let { score -> ((score * 10).toInt() / 10.0).toString() }
        return MetaPreview(
            id = "tmdb:$id",
            type = normalizedType,
            name = displayName,
            poster = buildTmdbImageUrl(posterPath, "w500"),
            banner = buildTmdbImageUrl(backdropPath, "w1280"),
            posterShape = PosterShape.Poster,
            description = overview?.takeIf { it.isNotBlank() },
            releaseInfo = release?.take(4),
            rawReleaseDate = release,
            voteCount = voteCount,
            imdbRating = rating,
        )
    }
}

data class TmdbPersonSearchResult(
    val id: Int,
    val name: String,
    val photo: String?,
    val knownForDepartment: String?,
    val knownFor: List<String>,
    val popularity: Double,
)

private data class ScoredPerson(
    val person: TmdbPersonSearchResult,
    val score: Int,
)

private fun scoredPerson(
    person: TmdbPersonSearchResult,
    score: Int,
): ScoredPerson = ScoredPerson(person = person, score = score)

@Serializable
private data class TmdbPersonSearchResponse(
    val results: List<TmdbPersonSearchApiResult> = emptyList(),
)

@Serializable
private data class TmdbPersonSearchApiResult(
    val id: Int,
    val name: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("known_for_department") val knownForDepartment: String? = null,
    @SerialName("known_for") val knownFor: List<TmdbPersonKnownForResult> = emptyList(),
    val popularity: Double? = null,
) {
    fun toPersonSearchResult(): TmdbPersonSearchResult? {
        val displayName = name?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return TmdbPersonSearchResult(
            id = id,
            name = displayName,
            photo = buildTmdbImageUrl(profilePath, "w342"),
            knownForDepartment = knownForDepartment?.trim()?.takeIf { it.isNotBlank() },
            knownFor = knownFor
                .mapNotNull { item ->
                    item.title?.trim()?.takeIf { it.isNotBlank() }
                        ?: item.name?.trim()?.takeIf { it.isNotBlank() }
                }
                .distinct()
                .take(3),
            popularity = popularity ?: 0.0,
        )
    }
}

@Serializable
private data class TmdbPersonKnownForResult(
    val title: String? = null,
    val name: String? = null,
)

private fun personSearchScore(
    query: String,
    candidateName: String,
): Int {
    val normalizedQuery = query.searchComparable()
    val normalizedName = candidateName.searchComparable()
    if (normalizedQuery.isBlank() || normalizedName.isBlank()) return 0

    if (normalizedName == normalizedQuery) return 10_000
    if (normalizedName.startsWith(normalizedQuery)) return 9_300

    val queryTokens = normalizedQuery.split(' ').filter(String::isNotBlank)
    val nameTokens = normalizedName.split(' ').filter(String::isNotBlank)
    if (queryTokens.isNotEmpty() && queryTokens.all { queryToken ->
            nameTokens.any { nameToken -> nameToken.startsWith(queryToken) }
        }
    ) {
        return 8_900
    }

    if (queryTokens.isNotEmpty() && queryTokens.all { token -> normalizedName.contains(token) }) {
        return 8_200
    }

    val compactQuery = normalizedQuery.replace(" ", "")
    val compactName = normalizedName.replace(" ", "")
    if (compactName.startsWith(compactQuery)) return 8_000
    val distance = levenshteinDistance(compactQuery, compactName)
    val maxLength = maxOf(compactQuery.length, compactName.length).coerceAtLeast(1)
    val tolerance = when {
        compactQuery.length <= 4 -> 1
        compactQuery.length <= 8 -> 2
        else -> 3
    }
    return if (distance <= tolerance || distance.toDouble() / maxLength.toDouble() <= 0.28) {
        7_400 - (distance * 140)
    } else {
        0
    }
}

private fun String.searchComparable(): String =
    lowercase()
        .map { char ->
            when (char) {
                'ç' -> 'c'
                'ğ' -> 'g'
                'ı', 'i', 'İ' -> 'i'
                'ö' -> 'o'
                'ş' -> 's'
                'ü' -> 'u'
                else -> char
            }
        }
        .joinToString("")
        .map { char -> if (char.isLetterOrDigit()) char else ' ' }
        .joinToString("")
        .trim()
        .replace(Regex("\\s+"), " ")

private fun levenshteinDistance(
    left: String,
    right: String,
): Int {
    if (left == right) return 0
    if (left.isEmpty()) return right.length
    if (right.isEmpty()) return left.length

    var previous = IntArray(right.length + 1) { it }
    var current = IntArray(right.length + 1)

    for (i in left.indices) {
        current[0] = i + 1
        for (j in right.indices) {
            val cost = if (left[i] == right[j]) 0 else 1
            current[j + 1] = minOf(
                current[j] + 1,
                previous[j + 1] + 1,
                previous[j] + cost,
            )
        }
        val swap = previous
        previous = current
        current = swap
    }
    return previous[right.length]
}
