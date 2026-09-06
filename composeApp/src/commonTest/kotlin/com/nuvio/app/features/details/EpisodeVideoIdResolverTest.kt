package com.nuvio.app.features.details

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EpisodeVideoIdResolverTest {
    @Test
    fun `resolves the requested episode from cached metadata`() {
        val meta = MetaDetails(
            id = "series",
            type = "series",
            name = "Series",
            videos = listOf(
                MetaVideo(id = "s1e1", title = "First", season = 1, episode = 1),
                MetaVideo(id = "s2e3", title = "Target", season = 2, episode = 3),
            ),
        )

        assertEquals("s2e3", resolveCachedEpisodeVideoId(meta, season = 2, episode = 3))
    }

    @Test
    fun `does not resolve missing or blank episode ids`() {
        val meta = MetaDetails(
            id = "series",
            type = "series",
            name = "Series",
            videos = listOf(MetaVideo(id = "", title = "Episode", season = 1, episode = 1)),
        )

        assertNull(resolveCachedEpisodeVideoId(meta, season = 1, episode = 1))
        assertNull(resolveCachedEpisodeVideoId(meta, season = 1, episode = 2))
        assertNull(resolveCachedEpisodeVideoId(null, season = 1, episode = 1))
    }

    @Test
    fun `episode list identity ignores visual metadata changes`() {
        val episodes = listOf(
            MetaVideo(id = "s1e1", title = "Original", thumbnail = "old", season = 1, episode = 1),
        )
        val enrichedEpisodes = listOf(
            episodes.single().copy(title = "Enriched", thumbnail = "new", overview = "Description"),
        )

        assertEquals(episodeListIdentity(episodes), episodeListIdentity(enrichedEpisodes))
    }
}
