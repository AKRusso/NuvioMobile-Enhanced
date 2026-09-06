package com.nuvio.app.features.details

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MetaDetailsParserTest {

    @Test
    fun `parse rejects null meta object without json object cast crash`() {
        assertFailsWith<IllegalStateException> {
            MetaDetailsParser.parse("""{"meta":null}""")
        }
    }

    @Test
    fun `parse accepts bare meta object response`() {
        val result = MetaDetailsParser.parse(
            """
            {
              "id": "mal:62516",
              "type": "series",
              "name": "The Fragrant Flower Blooms with Dignity"
            }
            """.trimIndent(),
        )

        assertEquals("mal:62516", result.id)
        assertEquals("series", result.type)
        assertEquals("The Fragrant Flower Blooms with Dignity", result.name)
    }

    @Test
    fun `parse preserves explicit video availability`() {
        val result = MetaDetailsParser.parse(
            """
            {
              "meta": {
                "id": "mal:52991",
                "type": "series",
                "name": "Show",
                "videos": [
                  {
                    "id": "show:3:1",
                    "title": "Episode 1",
                    "season": 3,
                    "episode": 1,
                    "released": null,
                    "available": false
                  },
                  {
                    "id": "show:1:1",
                    "title": "Episode 1",
                    "season": 1,
                    "episode": 1
                  }
                ]
              }
            }
            """.trimIndent(),
        )

        assertFalse(result.videos[0].available)
        assertTrue(result.videos[1].available)
    }

    @Test
    fun `parse reads defaultVideoId from behavior hints`() {
        val result = MetaDetailsParser.parse(
            """
            {
              "meta": {
                "id": "show",
                "type": "series",
                "name": "Show",
                "behaviorHints": {
                  "defaultVideoId": "show:1:2"
                }
              }
            }
            """.trimIndent(),
        )

        assertEquals("show:1:2", result.defaultVideoId)
    }

    @Test
    fun `parse reads positive per-episode addon ratings`() {
        val result = MetaDetailsParser.parse(
            """
            {
              "meta": {
                "id": "show",
                "type": "series",
                "name": "Show",
                "videos": [
                  { "id": "show:1:1", "title": "Episode 1", "rating": "8.7" },
                  { "id": "show:1:2", "title": "Episode 2", "rating": 7.5 }
                ]
              }
            }
            """.trimIndent(),
        )

        assertEquals(8.7, result.videos[0].rating)
        assertEquals(7.5, result.videos[1].rating)
    }

    @Test
    fun `parse ignores invalid per-episode addon ratings`() {
        val result = MetaDetailsParser.parse(
            """
            {
              "meta": {
                "id": "show",
                "type": "series",
                "name": "Show",
                "videos": [
                  { "id": "show:1:1", "title": "Episode 1", "rating": "unknown" },
                  { "id": "show:1:2", "title": "Episode 2", "rating": 0 }
                ]
              }
            }
            """.trimIndent(),
        )

        assertEquals(null, result.videos[0].rating)
        assertEquals(null, result.videos[1].rating)
    }

    @Test
    fun `parse preserves addon production and network logos`() {
        val result = MetaDetailsParser.parse(
            """
            {
              "meta": {
                "id": "show",
                "type": "series",
                "name": "Show",
                "production_companies": [
                  { "id": 420, "name": "Studio", "logo_path": "/studio.png" }
                ],
                "networks": [
                  { "tmdbId": 213, "name": "Netflix", "logo": "https://example.com/netflix.png" }
                ]
              }
            }
            """.trimIndent(),
        )

        assertEquals("https://image.tmdb.org/t/p/w300/studio.png", result.productionCompanies.single().logo)
        assertEquals(420, result.productionCompanies.single().tmdbId)
        assertEquals("Netflix", result.networks.single().name)
        assertEquals("https://example.com/netflix.png", result.networks.single().logo)
        assertEquals(213, result.networks.single().tmdbId)
    }
}
