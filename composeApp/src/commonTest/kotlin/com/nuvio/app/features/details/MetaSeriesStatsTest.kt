package com.nuvio.app.features.details

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MetaSeriesStatsTest {

    @Test
    fun `series stats exclude specials and duplicate episodes`() {
        val meta = series(
            MetaVideo(id = "s1e1", title = "Episode 1", season = 1, episode = 1),
            MetaVideo(id = "s1e1-copy", title = "Episode 1 copy", season = 1, episode = 1),
            MetaVideo(id = "s1e2", title = "Episode 2", season = 1, episode = 2),
            MetaVideo(id = "special", title = "Special", season = 0, episode = 1),
        )

        assertEquals(MetaSeriesStats(seasonCount = 1, episodeCount = 2), meta.mainSeriesStats())
    }

    @Test
    fun `series stats exclude invalid season and episode coordinates`() {
        val meta = series(
            MetaVideo(id = "regular", title = "Regular", season = 2, episode = 3),
            MetaVideo(id = "seasonless", title = "Seasonless", season = null, episode = 1),
            MetaVideo(id = "negative", title = "Negative", season = -1, episode = 1),
            MetaVideo(id = "no-episode", title = "No episode", season = 2, episode = null),
            MetaVideo(id = "episode-zero", title = "Episode zero", season = 2, episode = 0),
        )

        assertEquals(MetaSeriesStats(seasonCount = 1, episodeCount = 1), meta.mainSeriesStats())
    }

    @Test
    fun `series containing only specials has no main stats`() {
        val meta = series(
            MetaVideo(id = "special-1", title = "Special 1", season = 0, episode = 1),
            MetaVideo(id = "special-2", title = "Special 2", season = null, episode = 2),
        )

        assertNull(meta.mainSeriesStats())
    }

    private fun series(vararg videos: MetaVideo) = MetaDetails(
        id = "show",
        type = "series",
        name = "Show",
        videos = videos.toList(),
    )
}
