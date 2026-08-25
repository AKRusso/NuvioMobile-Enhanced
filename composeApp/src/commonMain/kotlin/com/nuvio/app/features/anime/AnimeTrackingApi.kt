package com.nuvio.app.features.anime

import com.nuvio.app.features.addons.httpPostJsonWithHeaders
import com.nuvio.app.features.addons.httpRequestRaw
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
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

    suspend fun saveAniListProgress(accessToken: String, mediaId: Int, progress: Int): Boolean {
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
        ) ?: return false
        return runCatching {
            val root = json.parseToJsonElement(response).jsonObject
            root["errors"] == null && root["data"] !is JsonNull && root["data"] != null
        }.getOrDefault(false)
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

    suspend fun saveMalProgress(accessToken: String, animeId: Int, progress: Int): Boolean = requestCatching {
        val body = buildString {
            append("num_watched_episodes=$progress")
            if (progress > 0) append("&status=watching")
        }
        httpRequestRaw(
            "PATCH",
            "$MAL_API/anime/$animeId/my_list_status",
            mapOf(
                "Authorization" to "Bearer $accessToken",
                "Content-Type" to "application/x-www-form-urlencoded",
                "Accept" to "application/json",
            ),
            body,
        ).status in 200..299
    } ?: false

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

internal data class AnimeTrackingViewer(val id: Int?, val name: String?)

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
