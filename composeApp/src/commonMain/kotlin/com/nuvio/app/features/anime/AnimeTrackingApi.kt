package com.nuvio.app.features.anime

import com.nuvio.app.features.addons.httpPostJsonWithHeaders
import com.nuvio.app.features.addons.httpRequestRaw
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object AnimeTrackingApi {
    private const val ANILIST_API = "https://graphql.anilist.co"
    private const val MAL_API = "https://api.myanimelist.net/v2"
    private const val MAL_TOKEN_URL = "https://myanimelist.net/v1/oauth2/token"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun aniListViewer(accessToken: String): AnimeTrackingViewer? {
        val response = aniListQuery(accessToken, "query { Viewer { id name } }") ?: return null
        return runCatching {
            val viewer = json.parseToJsonElement(response).jsonObject["data"]
                ?.jsonObject?.get("Viewer")?.jsonObject ?: return null
            AnimeTrackingViewer(
                id = viewer["id"]?.jsonPrimitive?.intOrNull,
                name = viewer["name"]?.jsonPrimitive?.contentOrNull,
            )
        }.getOrNull()
    }

    suspend fun aniListMalId(mediaId: Int): Int? {
        val variables = buildJsonObject { put("id", JsonPrimitive(mediaId)) }
        val response = aniListQuery(null, "query (${ '$' }id: Int) { Media(id: ${ '$' }id) { idMal } }", variables)
            ?: return null
        return runCatching {
            json.parseToJsonElement(response).jsonObject["data"]?.jsonObject
                ?.get("Media")?.jsonObject?.get("idMal")?.jsonPrimitive?.intOrNull
        }.getOrNull()
    }

    suspend fun searchAniList(query: String): List<AnimeTrackingSearchResult> {
        val variables = buildJsonObject { put("search", JsonPrimitive(query)) }
        val response = aniListQuery(
            null,
            "query (${ '$' }search: String) { Page(page: 1, perPage: 15) { " +
                "media(search: ${ '$' }search, type: ANIME) { id title { userPreferred romaji english } " +
                "coverImage { large } format status startDate { year } episodes } } }",
            variables,
        ) ?: return emptyList()
        return runCatching {
            json.parseToJsonElement(response).jsonObject["data"]?.jsonObject
                ?.get("Page")?.jsonObject?.get("media")?.jsonArray
                ?.mapNotNull { element ->
                    val media = element.jsonObject
                    val id = media["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
                    val title = media["title"]?.jsonObject
                    AnimeTrackingSearchResult(
                        id = id,
                        title = title?.get("userPreferred")?.jsonPrimitive?.contentOrNull
                            ?: title?.get("romaji")?.jsonPrimitive?.contentOrNull
                            ?: title?.get("english")?.jsonPrimitive?.contentOrNull
                            ?: return@mapNotNull null,
                        imageUrl = media["coverImage"]?.jsonObject?.get("large")?.jsonPrimitive?.contentOrNull,
                        year = media["startDate"]?.jsonObject?.get("year")?.jsonPrimitive?.intOrNull,
                        format = media["format"]?.jsonPrimitive?.contentOrNull,
                        status = media["status"]?.jsonPrimitive?.contentOrNull,
                        episodes = media["episodes"]?.jsonPrimitive?.intOrNull,
                    )
                }.orEmpty()
        }.getOrDefault(emptyList())
    }

    suspend fun aniListProgress(accessToken: String, mediaId: Int): Int? {
        val variables = buildJsonObject { put("mediaId", JsonPrimitive(mediaId)) }
        val response = aniListQuery(
            accessToken,
            "query (${ '$' }mediaId: Int) { MediaList(mediaId: ${ '$' }mediaId, type: ANIME) { progress } }",
            variables,
        ) ?: return null
        return runCatching {
            json.parseToJsonElement(response).jsonObject["data"]?.jsonObject
                ?.get("MediaList")?.jsonObject?.get("progress")?.jsonPrimitive?.intOrNull
        }.getOrNull()
    }

    suspend fun aniListEntry(accessToken: String, mediaId: Int): AnimeTrackingEntry? {
        val variables = buildJsonObject { put("mediaId", JsonPrimitive(mediaId)) }
        val response = aniListQuery(
            accessToken,
            "query (${ '$' }mediaId: Int) { Media(id: ${ '$' }mediaId, type: ANIME) { " +
                "id title { userPreferred romaji english } coverImage { large } episodes isFavourite " +
                "mediaListEntry { id status progress score(format: POINT_10_DECIMAL) repeat notes private " +
                "hiddenFromStatusLists startedAt { year month day } completedAt { year month day } advancedScores } } }",
            variables,
        ) ?: return null
        return runCatching {
            val media = json.parseToJsonElement(response).jsonObject["data"]?.jsonObject
                ?.get("Media") as? JsonObject ?: return null
            val listEntry = media["mediaListEntry"] as? JsonObject
            val title = media["title"]?.jsonObject
            AnimeTrackingEntry(
                mediaId = media["id"]?.jsonPrimitive?.intOrNull ?: mediaId,
                listEntryId = listEntry?.get("id")?.jsonPrimitive?.intOrNull,
                title = title?.get("userPreferred")?.jsonPrimitive?.contentOrNull
                    ?: title?.get("romaji")?.jsonPrimitive?.contentOrNull
                    ?: title?.get("english")?.jsonPrimitive?.contentOrNull
                    ?: "Anime #$mediaId",
                imageUrl = media["coverImage"]?.jsonObject?.get("large")?.jsonPrimitive?.contentOrNull,
                totalEpisodes = media["episodes"]?.jsonPrimitive?.intOrNull,
                status = AnimeTrackingUserStatus.fromAniList(listEntry?.get("status")?.jsonPrimitive?.contentOrNull),
                progress = listEntry?.get("progress")?.jsonPrimitive?.intOrNull ?: 0,
                score = listEntry?.get("score")?.jsonPrimitive?.doubleOrNull ?: 0.0,
                startDate = parseAniListDate(listEntry?.get("startedAt") as? JsonObject),
                finishDate = parseAniListDate(listEntry?.get("completedAt") as? JsonObject),
                repeat = listEntry?.get("repeat")?.jsonPrimitive?.intOrNull ?: 0,
                notes = listEntry?.get("notes")?.jsonPrimitive?.contentOrNull,
                isPrivate = listEntry?.get("private")?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
                hiddenFromStatusLists = listEntry?.get("hiddenFromStatusLists")?.jsonPrimitive?.contentOrNull
                    ?.toBooleanStrictOrNull() ?: false,
                advancedScores = parseAniListAdvancedScores(listEntry?.get("advancedScores")),
                isFavourite = media["isFavourite"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
                isTracked = listEntry != null,
            )
        }.getOrNull()
    }

    suspend fun saveAniListEntry(
        accessToken: String,
        mediaId: Int,
        update: AnimeTrackingEntryUpdate,
    ): AnimeTrackingMutationStatus {
        val currentFavourite = update.isFavourite?.let { aniListEntry(accessToken, mediaId)?.isFavourite }
        if (update.isFavourite != null && currentFavourite == null) return AnimeTrackingMutationStatus.FAILED
        val variables = buildAniListEntryVariables(mediaId, update)
        val response = aniListQuery(
            accessToken,
            "mutation (${ '$' }mediaId: Int, ${ '$' }status: MediaListStatus, ${ '$' }progress: Int, " +
                "${ '$' }score: Float, ${ '$' }repeat: Int, ${ '$' }private: Boolean, ${ '$' }notes: String, " +
                "${ '$' }hiddenFromStatusLists: Boolean, ${ '$' }startedAt: FuzzyDateInput, " +
                "${ '$' }completedAt: FuzzyDateInput, ${ '$' }advancedScores: [Float]) { " +
                "SaveMediaListEntry(mediaId: ${ '$' }mediaId, status: ${ '$' }status, progress: ${ '$' }progress, " +
                "score: ${ '$' }score, repeat: ${ '$' }repeat, private: ${ '$' }private, notes: ${ '$' }notes, " +
                "hiddenFromStatusLists: ${ '$' }hiddenFromStatusLists, startedAt: ${ '$' }startedAt, " +
                "completedAt: ${ '$' }completedAt, advancedScores: ${ '$' }advancedScores) { id } }",
            variables,
        ) ?: return retryAniListWithoutAdvancedScores(accessToken, mediaId, update)
        val status = aniListMutationStatus(response)
        if (status != AnimeTrackingMutationStatus.SUCCESS) {
            return if (status == AnimeTrackingMutationStatus.FAILED) {
                retryAniListWithoutAdvancedScores(accessToken, mediaId, update)
            } else {
                status
            }
        }
        return if (update.isFavourite != null && update.isFavourite != currentFavourite) {
            toggleAniListFavourite(accessToken, mediaId)
        } else {
            AnimeTrackingMutationStatus.SUCCESS
        }
    }

    private suspend fun retryAniListWithoutAdvancedScores(
        accessToken: String,
        mediaId: Int,
        update: AnimeTrackingEntryUpdate,
    ): AnimeTrackingMutationStatus = if (update.advancedScores != null) {
        saveAniListEntry(accessToken, mediaId, update.copy(advancedScores = null))
    } else {
        AnimeTrackingMutationStatus.FAILED
    }

    suspend fun deleteAniListEntry(accessToken: String, listEntryId: Int): AnimeTrackingMutationStatus {
        val variables = buildJsonObject { put("id", JsonPrimitive(listEntryId)) }
        val response = aniListQuery(
            accessToken,
            "mutation (${ '$' }id: Int) { DeleteMediaListEntry(id: ${ '$' }id) { deleted } }",
            variables,
        ) ?: return AnimeTrackingMutationStatus.FAILED
        return aniListMutationStatus(response)
    }

    suspend fun toggleAniListFavourite(accessToken: String, mediaId: Int): AnimeTrackingMutationStatus {
        val variables = buildJsonObject { put("animeId", JsonPrimitive(mediaId)) }
        val response = aniListQuery(
            accessToken,
            "mutation (${ '$' }animeId: Int) { ToggleFavourite(animeId: ${ '$' }animeId) { anime { nodes { id } } } }",
            variables,
        ) ?: return AnimeTrackingMutationStatus.FAILED
        return aniListMutationStatus(response)
    }

    suspend fun saveAniListProgress(
        accessToken: String,
        mediaId: Int,
        progress: Int,
    ): AnimeTrackingMutationStatus {
        val variables = buildJsonObject {
            put("mediaId", JsonPrimitive(mediaId))
            put("progress", JsonPrimitive(progress))
            if (progress > 0) put("status", JsonPrimitive("CURRENT"))
        }
        val response = aniListQuery(
            accessToken,
            "mutation (${ '$' }mediaId: Int, ${ '$' }progress: Int, ${ '$' }status: MediaListStatus) { " +
                "SaveMediaListEntry(mediaId: ${ '$' }mediaId, progress: ${ '$' }progress, status: ${ '$' }status) { id } }",
            variables,
        ) ?: return AnimeTrackingMutationStatus.FAILED
        return runCatching {
            val root = json.parseToJsonElement(response).jsonObject
            val errorText = root["errors"]?.toString().orEmpty()
            when {
                errorText.contains("invalid token", ignoreCase = true) ||
                    errorText.contains("unauthenticated", ignoreCase = true) ||
                    errorText.contains("unauthorized", ignoreCase = true) -> AnimeTrackingMutationStatus.UNAUTHORIZED
                root["errors"] == null && root["data"] !is JsonNull && root["data"] != null -> {
                    AnimeTrackingMutationStatus.SUCCESS
                }
                else -> AnimeTrackingMutationStatus.FAILED
            }
        }.getOrDefault(AnimeTrackingMutationStatus.FAILED)
    }

    suspend fun malViewer(accessToken: String): AnimeTrackingViewer? = requestCatching {
        val response = httpRequestRaw(
            "GET",
            "$MAL_API/users/@me",
            mapOf("Authorization" to "Bearer $accessToken", "Accept" to "application/json"),
            "",
        )
        json.decodeFromString<MalViewerResponse>(response.body).let { AnimeTrackingViewer(it.id, it.name) }
    }

    suspend fun searchMal(accessToken: String, query: String): List<AnimeTrackingSearchResult> = requestCatching {
        val response = httpRequestRaw(
            "GET",
            "$MAL_API/anime?q=${query.encodeURLParameter()}&limit=15&fields=id,title,main_picture,start_date,status,num_episodes,media_type",
            mapOf("Authorization" to "Bearer $accessToken", "Accept" to "application/json"),
            "",
        )
        json.parseToJsonElement(response.body).jsonObject["data"]?.jsonArray
            ?.mapNotNull { element ->
                val node = element.jsonObject["node"]?.jsonObject ?: return@mapNotNull null
                AnimeTrackingSearchResult(
                    id = node["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null,
                    title = node["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
                    imageUrl = node["main_picture"]?.jsonObject?.get("large")?.jsonPrimitive?.contentOrNull,
                    year = node["start_date"]?.jsonPrimitive?.contentOrNull?.take(4)?.toIntOrNull(),
                    format = node["media_type"]?.jsonPrimitive?.contentOrNull,
                    status = node["status"]?.jsonPrimitive?.contentOrNull,
                    episodes = node["num_episodes"]?.jsonPrimitive?.intOrNull,
                )
            }.orEmpty()
    } ?: emptyList()

    suspend fun malProgress(accessToken: String, animeId: Int): Int? = requestCatching {
        val response = httpRequestRaw(
            "GET",
            "$MAL_API/anime/$animeId?fields=my_list_status",
            mapOf("Authorization" to "Bearer $accessToken", "Accept" to "application/json"),
            "",
        )
        json.parseToJsonElement(response.body).jsonObject["my_list_status"]?.jsonObject
            ?.get("num_episodes_watched")?.jsonPrimitive?.intOrNull
    }

    suspend fun malEntry(accessToken: String, animeId: Int): AnimeTrackingEntry? = requestCatching {
        val response = httpRequestRaw(
            "GET",
            "$MAL_API/anime/$animeId?fields=id,title,main_picture,num_episodes," +
                "my_list_status%7Bstatus,score,num_episodes_watched,is_rewatching,start_date,finish_date," +
                "priority,num_times_rewatched,rewatch_value,comments%7D",
            mapOf("Authorization" to "Bearer $accessToken", "Accept" to "application/json"),
            "",
        )
        val anime = json.parseToJsonElement(response.body).jsonObject
        val listEntry = anime["my_list_status"] as? JsonObject
        AnimeTrackingEntry(
            mediaId = anime["id"]?.jsonPrimitive?.intOrNull ?: animeId,
            title = anime["title"]?.jsonPrimitive?.contentOrNull ?: "Anime #$animeId",
            imageUrl = anime["main_picture"]?.jsonObject?.get("large")?.jsonPrimitive?.contentOrNull,
            totalEpisodes = anime["num_episodes"]?.jsonPrimitive?.intOrNull,
            status = if (listEntry?.get("is_rewatching")?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() == true) {
                AnimeTrackingUserStatus.REWATCHING
            } else {
                AnimeTrackingUserStatus.fromMal(listEntry?.get("status")?.jsonPrimitive?.contentOrNull)
            },
            progress = listEntry?.get("num_episodes_watched")?.jsonPrimitive?.intOrNull ?: 0,
            score = listEntry?.get("score")?.jsonPrimitive?.doubleOrNull ?: 0.0,
            startDate = parseMalDate(listEntry?.get("start_date")?.jsonPrimitive?.contentOrNull),
            finishDate = parseMalDate(listEntry?.get("finish_date")?.jsonPrimitive?.contentOrNull),
            repeat = listEntry?.get("num_times_rewatched")?.jsonPrimitive?.intOrNull ?: 0,
            notes = listEntry?.get("comments")?.jsonPrimitive?.contentOrNull,
            priority = listEntry?.get("priority")?.jsonPrimitive?.intOrNull,
            rewatchValue = listEntry?.get("rewatch_value")?.jsonPrimitive?.intOrNull,
            isTracked = listEntry != null,
        )
    }

    suspend fun exchangeMalCode(code: String, verifier: String): MalTokenResponse? = malTokenRequest(
        "client_id=${AnimeTrackingConfig.MAL_CLIENT_ID.encodeURLParameter()}" +
            "&code=${code.encodeURLParameter()}" +
            "&code_verifier=${verifier.encodeURLParameter()}" +
            "&grant_type=authorization_code",
    )

    suspend fun refreshMalToken(refreshToken: String): MalTokenResponse? = malTokenRequest(
        "client_id=${AnimeTrackingConfig.MAL_CLIENT_ID.encodeURLParameter()}" +
            "&refresh_token=${refreshToken.encodeURLParameter()}" +
            "&grant_type=refresh_token",
    )

    suspend fun saveMalProgress(
        accessToken: String,
        animeId: Int,
        progress: Int,
    ): AnimeTrackingMutationStatus = requestCatching {
        val body = buildString {
            append("num_watched_episodes=$progress")
            if (progress > 0) append("&status=watching")
        }
        val status = httpRequestRaw(
            "PATCH",
            "$MAL_API/anime/$animeId/my_list_status",
            mapOf(
                "Authorization" to "Bearer $accessToken",
                "Content-Type" to "application/x-www-form-urlencoded",
                "Accept" to "application/json",
            ),
            body,
        ).status
        when {
            status in 200..299 -> AnimeTrackingMutationStatus.SUCCESS
            status == 401 || status == 403 -> AnimeTrackingMutationStatus.UNAUTHORIZED
            else -> AnimeTrackingMutationStatus.FAILED
        }
    } ?: AnimeTrackingMutationStatus.FAILED

    suspend fun saveMalEntry(
        accessToken: String,
        animeId: Int,
        update: AnimeTrackingEntryUpdate,
    ): AnimeTrackingMutationStatus = requestCatching {
        val body = buildMalEntryPayload(update)
        val responseStatus = httpRequestRaw(
            "PATCH",
            "$MAL_API/anime/$animeId/my_list_status",
            mapOf(
                "Authorization" to "Bearer $accessToken",
                "Content-Type" to "application/x-www-form-urlencoded",
                "Accept" to "application/json",
            ),
            body,
        ).status
        mutationStatus(responseStatus)
    } ?: AnimeTrackingMutationStatus.FAILED

    suspend fun deleteMalEntry(accessToken: String, animeId: Int): AnimeTrackingMutationStatus = requestCatching {
        val status = httpRequestRaw(
            "DELETE",
            "$MAL_API/anime/$animeId/my_list_status",
            mapOf("Authorization" to "Bearer $accessToken", "Accept" to "application/json"),
            "",
        ).status
        mutationStatus(status)
    } ?: AnimeTrackingMutationStatus.FAILED

    private suspend fun aniListQuery(
        accessToken: String?,
        query: String,
        variables: JsonObject? = null,
    ): String? = requestCatching {
        val body = buildJsonObject {
            put("query", JsonPrimitive(query))
            variables?.let { put("variables", it) }
        }.toString()
        val headers = mutableMapOf("Content-Type" to "application/json", "Accept" to "application/json")
        accessToken?.takeIf(String::isNotBlank)?.let { headers["Authorization"] = "Bearer $it" }
        httpPostJsonWithHeaders(ANILIST_API, body, headers)
    }

    private fun aniListMutationStatus(response: String): AnimeTrackingMutationStatus = runCatching {
        val root = json.parseToJsonElement(response).jsonObject
        val errorText = root["errors"]?.toString().orEmpty()
        when {
            errorText.contains("invalid token", ignoreCase = true) ||
                errorText.contains("unauthenticated", ignoreCase = true) ||
                errorText.contains("unauthorized", ignoreCase = true) -> AnimeTrackingMutationStatus.UNAUTHORIZED
            root["errors"] == null && root["data"] !is JsonNull && root["data"] != null ->
                AnimeTrackingMutationStatus.SUCCESS
            else -> AnimeTrackingMutationStatus.FAILED
        }
    }.getOrDefault(AnimeTrackingMutationStatus.FAILED)

    private fun mutationStatus(status: Int): AnimeTrackingMutationStatus = when {
        status in 200..299 -> AnimeTrackingMutationStatus.SUCCESS
        status == 401 || status == 403 -> AnimeTrackingMutationStatus.UNAUTHORIZED
        else -> AnimeTrackingMutationStatus.FAILED
    }

    private suspend fun malTokenRequest(body: String): MalTokenResponse? = requestCatching {
        val response = httpRequestRaw(
            "POST",
            MAL_TOKEN_URL,
            mapOf("Content-Type" to "application/x-www-form-urlencoded", "Accept" to "application/json"),
            body,
        )
        json.decodeFromString<MalTokenResponse>(response.body)
    }

    private suspend inline fun <T> requestCatching(block: () -> T): T? = try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Throwable) {
        null
    }
}

internal fun buildAniListEntryVariables(mediaId: Int, update: AnimeTrackingEntryUpdate): JsonObject = buildJsonObject {
    put("mediaId", JsonPrimitive(mediaId))
    put("status", JsonPrimitive(update.status.aniListValue))
    put("progress", JsonPrimitive(update.progress.coerceAtLeast(0)))
    put("score", JsonPrimitive(update.score.coerceIn(0.0, 10.0)))
    update.repeat?.let { put("repeat", JsonPrimitive(it.coerceAtLeast(0))) }
    update.isPrivate?.let { put("private", JsonPrimitive(it)) }
    update.notes?.let { put("notes", JsonPrimitive(it)) }
    update.hiddenFromStatusLists?.let { put("hiddenFromStatusLists", JsonPrimitive(it)) }
    putAnimeTrackingDate("startedAt", update.startDate, update.clearStartDate)
    putAnimeTrackingDate("completedAt", update.finishDate, update.clearFinishDate)
    update.advancedScores?.let { scores ->
        val values = scores.values
        put(
            "advancedScores",
            if (values.all { it == 0.0 }) JsonNull else JsonArray(values.map(::JsonPrimitive)),
        )
    }
}

internal fun buildMalEntryPayload(update: AnimeTrackingEntryUpdate): String = buildList {
    add("status=${update.status.malValue.encodeURLParameter()}")
    add("is_rewatching=${update.status == AnimeTrackingUserStatus.REWATCHING}")
    add("num_watched_episodes=${update.progress.coerceAtLeast(0)}")
    add("score=${update.score.toInt().coerceIn(0, 10)}")
    when {
        update.startDate != null -> add("start_date=${update.startDate.toMalDate()}")
        update.clearStartDate -> add("start_date=")
    }
    when {
        update.finishDate != null -> add("finish_date=${update.finishDate.toMalDate()}")
        update.clearFinishDate -> add("finish_date=")
    }
    update.repeat?.let { add("num_times_rewatched=${it.coerceAtLeast(0)}") }
    update.notes?.let { add("comments=${it.encodeURLParameter()}") }
    update.priority?.let { add("priority=${it.coerceIn(0, 2)}") }
    update.rewatchValue?.let { add("rewatch_value=${it.coerceIn(0, 5)}") }
}.joinToString("&")

private fun kotlinx.serialization.json.JsonObjectBuilder.putAnimeTrackingDate(
    key: String,
    date: AnimeTrackingDate?,
    clear: Boolean,
) {
    when {
        date != null -> put(key, buildJsonObject {
            date.year?.let { put("year", JsonPrimitive(it)) }
            date.month?.let { put("month", JsonPrimitive(it)) }
            date.day?.let { put("day", JsonPrimitive(it)) }
        })
        clear -> put(key, buildJsonObject {
            put("year", JsonNull)
            put("month", JsonNull)
            put("day", JsonNull)
        })
    }
}

private fun parseAniListDate(value: JsonObject?): AnimeTrackingDate? {
    val date = AnimeTrackingDate(
        year = value?.get("year")?.jsonPrimitive?.intOrNull,
        month = value?.get("month")?.jsonPrimitive?.intOrNull,
        day = value?.get("day")?.jsonPrimitive?.intOrNull,
    )
    return date.takeUnless { it.year == null && it.month == null && it.day == null }
}

internal fun parseMalDate(value: String?): AnimeTrackingDate? {
    val parts = value?.split('-') ?: return null
    if (parts.size != 3) return null
    return AnimeTrackingDate(parts[0].toIntOrNull(), parts[1].toIntOrNull(), parts[2].toIntOrNull())
        .takeIf { it.year != null && it.month != null && it.day != null }
}

private fun parseAniListAdvancedScores(value: kotlinx.serialization.json.JsonElement?): AnimeTrackingAdvancedScores? {
    val values = when (value) {
        is JsonArray -> value.mapNotNull { it.jsonPrimitive.doubleOrNull }
        is JsonObject -> value.values.mapNotNull { it.jsonPrimitive.doubleOrNull }
        else -> emptyList()
    }
    return values.takeIf { it.size >= 5 }?.let {
        AnimeTrackingAdvancedScores(it[0], it[1], it[2], it[3], it[4])
    }
}

internal data class AnimeTrackingViewer(val id: Int?, val name: String?)

internal enum class AnimeTrackingMutationStatus {
    SUCCESS,
    FAILED,
    UNAUTHORIZED,
}

internal enum class AnimeTrackingUserStatus(
    val aniListValue: String,
    val malValue: String,
) {
    WATCHING("CURRENT", "watching"),
    COMPLETED("COMPLETED", "completed"),
    ON_HOLD("PAUSED", "on_hold"),
    DROPPED("DROPPED", "dropped"),
    PLAN_TO_WATCH("PLANNING", "plan_to_watch"),
    REWATCHING("REPEATING", "watching");

    companion object {
        fun fromAniList(value: String?): AnimeTrackingUserStatus? = entries.firstOrNull {
            it.aniListValue.equals(value, ignoreCase = true)
        }

        fun fromMal(value: String?): AnimeTrackingUserStatus? = entries.firstOrNull {
            it.malValue.equals(value, ignoreCase = true)
        }
    }
}

internal data class AnimeTrackingEntry(
    val mediaId: Int,
    val listEntryId: Int? = null,
    val title: String,
    val imageUrl: String? = null,
    val totalEpisodes: Int? = null,
    val status: AnimeTrackingUserStatus? = null,
    val progress: Int = 0,
    val score: Double = 0.0,
    val startDate: AnimeTrackingDate? = null,
    val finishDate: AnimeTrackingDate? = null,
    val repeat: Int = 0,
    val notes: String? = null,
    val isPrivate: Boolean = false,
    val hiddenFromStatusLists: Boolean = false,
    val advancedScores: AnimeTrackingAdvancedScores? = null,
    val isFavourite: Boolean = false,
    val priority: Int? = null,
    val rewatchValue: Int? = null,
    val isTracked: Boolean = false,
)

internal data class AnimeTrackingEntryUpdate(
    val status: AnimeTrackingUserStatus,
    val progress: Int,
    val score: Double,
    val startDate: AnimeTrackingDate? = null,
    val finishDate: AnimeTrackingDate? = null,
    val clearStartDate: Boolean = false,
    val clearFinishDate: Boolean = false,
    val repeat: Int? = null,
    val notes: String? = null,
    val isPrivate: Boolean? = null,
    val hiddenFromStatusLists: Boolean? = null,
    val advancedScores: AnimeTrackingAdvancedScores? = null,
    val isFavourite: Boolean? = null,
    val priority: Int? = null,
    val rewatchValue: Int? = null,
)

internal data class AnimeTrackingDate(
    val year: Int?,
    val month: Int?,
    val day: Int?,
) {
    internal fun toMalDate(): String = listOf(year, month, day).mapIndexed { index, value ->
        requireNotNull(value) { "MAL dates require year, month, and day" }
        if (index == 0) value.toString().padStart(4, '0') else value.toString().padStart(2, '0')
    }.joinToString("-")
}

internal data class AnimeTrackingAdvancedScores(
    val story: Double,
    val characters: Double,
    val visuals: Double,
    val audio: Double,
    val enjoyment: Double,
) {
    internal val values: List<Double> get() = listOf(story, characters, visuals, audio, enjoyment)
}

data class AnimeTrackingSearchResult(
    val id: Int,
    val title: String,
    val imageUrl: String? = null,
    val year: Int? = null,
    val format: String? = null,
    val status: String? = null,
    val episodes: Int? = null,
)

@Serializable
internal data class MalTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long,
)

@Serializable
private data class MalViewerResponse(val id: Int? = null, val name: String? = null)
