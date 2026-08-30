package com.nuvio.app.features.simkl

import com.nuvio.app.features.addons.RawHttpResponse
import com.nuvio.app.features.tracking.TrackingExternalIds
import com.nuvio.app.features.tracking.TrackingListStatus
import com.nuvio.app.features.tracking.TrackingMediaKind
import com.nuvio.app.features.tracking.TrackingMediaReference
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SimklEditorServiceTest {
    @Test
    fun `memo decoding tolerates object string empty object and null`() {
        val json = Json { ignoreUnknownKeys = true }

        val objectMemo = json.decodeFromString<MemoEnvelope>(
            """{"memo":{"text":"note","is_private":"false"}}""",
        ).memo
        val stringMemo = json.decodeFromString<MemoEnvelope>("""{"memo":"legacy note"}""").memo
        val emptyMemo = json.decodeFromString<MemoEnvelope>("""{"memo":{}}""").memo
        val nullMemo = json.decodeFromString<MemoEnvelope>("""{"memo":null}""").memo

        assertEquals(SimklMemo("note", false), objectMemo)
        assertEquals(SimklMemo("legacy note", true), stringMemo)
        assertEquals(SimklMemo(), emptyMemo)
        assertEquals(SimklMemo(), nullMemo)
    }

    @Test
    fun `entry update carries status score and public memo without watch coordinates`() {
        val body = buildSimklEntryUpdateBody(
            item = anime(),
            status = TrackingListStatus.WATCHING,
            score = 9,
            memo = SimklMemo("No spoilers", isPrivate = false),
        ).let(Json::parseToJsonElement).jsonObject
        val item = body.getValue("shows").let { it as kotlinx.serialization.json.JsonArray }.single().jsonObject

        assertEquals("watching", item.getValue("status").jsonPrimitive.content)
        assertEquals(9, item.getValue("rating").jsonPrimitive.content.toInt())
        assertEquals("No spoilers", item.getValue("memo").jsonObject.getValue("text").jsonPrimitive.content)
        assertFalse(item.getValue("memo").jsonObject.getValue("is_private").jsonPrimitive.content.toBoolean())
        assertNull(item["episodes"])
        assertNull(item["seasons"])
        assertNull(item["watched_at"])
    }

    @Test
    fun `entry update validates score and memo limits`() {
        assertFailsWith<IllegalArgumentException> {
            buildSimklEntryUpdateBody(anime(), TrackingListStatus.WATCHING, 0, SimklMemo())
        }
        assertFailsWith<IllegalArgumentException> {
            buildSimklEntryUpdateBody(anime(), TrackingListStatus.WATCHING, null, SimklMemo("x".repeat(141)))
        }
    }

    @Test
    fun `search lookup and episode coordinates use existing unauthenticated client`() = runBlocking {
        val engine = RecordingEngine(
            response(
                """[{"title":"Cowboy Bebop","year":1998,"type":"tv","ep_count":26,"ids":{"simkl_id":"37089","imdb":"tt0213338","mal":1}}]""",
            ),
            response(
                """[{"type":"anime","title":"Attack on Titan","anime_type":"tv","total_episodes":75,"ids":{"simkl":39687,"mal":16498}}]""",
            ),
            response(
                """[{"type":"movie","title":"Inception","year":2010,"ids":{"simkl":472214,"imdb":"tt1375666"}}]""",
            ),
            response(
                """[{"episode":27,"ids":{"simkl_id":9001},"tvdb":{"season":2,"episode":1}}]""",
            ),
        )
        val service = SimklEditorService(client(engine))

        val searched = service.search(" Cowboy Bebop ").single()
        val lookedUp = service.lookup(TrackingExternalIds(mal = 16498), type = SimklSearchType.ANIME)
        val titleLookup = service.lookup(TrackingExternalIds(), title = " Inception ", year = 2010)
        val coordinate = service.animeEpisodeCoordinates(39687).single()

        assertEquals(37089L, searched.id)
        assertEquals(26, searched.totalEpisodes)
        assertEquals("tt0213338", searched.ids.getValue("imdb").jsonPrimitive.content)
        assertEquals("1", searched.ids.getValue("mal").jsonPrimitive.content)
        assertEquals(39687L, lookedUp?.id)
        assertEquals(SimklSearchType.ANIME, lookedUp?.type)
        assertEquals(472214L, titleLookup?.id)
        assertEquals(SimklSearchType.MOVIE, titleLookup?.type)
        assertEquals(SimklEpisodeCoordinate(27, 2, 1, 9001), coordinate)
        assertEquals(
            listOf("/search/anime", "/search/id", "/search/id", "/anime/episodes/39687"),
            engine.paths,
        )
        assertTrue(engine.urls[0].contains("q=Cowboy%20Bebop") || engine.urls[0].contains("q=Cowboy+Bebop"))
        assertTrue(engine.urls[1].contains("mal=16498"))
        assertTrue(engine.urls[2].contains("title=Inception"))
        assertTrue(engine.urls[2].contains("year=2010"))
        assertTrue(engine.headers.all { "Authorization" !in it })
    }

    @Test
    fun `destructive delete requires explicit confirmation before request`() = runBlocking {
        val engine = RecordingEngine(response("{}"))

        assertFailsWith<IllegalArgumentException> {
            requireSimklDestructiveDeleteConfirmed(false)
        }
        assertTrue(engine.paths.isEmpty())
    }

    private fun anime() = TrackingMediaReference(
        kind = TrackingMediaKind.ANIME,
        title = "Attack on Titan",
        year = 2013,
        ids = TrackingExternalIds(simkl = 39687, mal = 16498),
    )

    private fun client(engine: RecordingEngine) = SimklApiClient(
        engine = engine,
        accessToken = { null },
        onUnauthorized = {},
        nowEpochMs = { 0L },
        sleep = {},
        retryJitterMs = { 0L },
    )

    private class RecordingEngine(vararg responses: RawHttpResponse) : SimklHttpEngine {
        private val queued = responses.toMutableList()
        val paths = mutableListOf<String>()
        val urls = mutableListOf<String>()
        val headers = mutableListOf<Map<String, String>>()

        override suspend fun execute(
            method: String,
            url: String,
            headers: Map<String, String>,
            body: String,
        ): RawHttpResponse {
            urls += url
            paths += url.substringAfter("api.simkl.com").substringBefore('?')
            this.headers += headers
            return queued.removeAt(0)
        }
    }

    private companion object {
        fun response(body: String) = RawHttpResponse(
            status = 200,
            statusText = "",
            url = "https://api.simkl.com/test",
            body = body,
            headers = emptyMap(),
        )
    }
}

@kotlinx.serialization.Serializable
private data class MemoEnvelope(val memo: SimklMemo = SimklMemo())
