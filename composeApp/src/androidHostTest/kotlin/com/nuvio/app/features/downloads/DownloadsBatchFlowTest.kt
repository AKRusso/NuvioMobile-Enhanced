package com.nuvio.app.features.downloads

import com.nuvio.app.features.streams.StreamClientResolve
import com.nuvio.app.features.streams.StreamClientResolveParsed
import com.nuvio.app.features.streams.StreamClientResolveRaw
import com.nuvio.app.features.streams.StreamClientResolveStream
import com.nuvio.app.features.streams.StreamItem
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadsQualityAndKeyTest {
    @Test
    fun qualityKeyPrefersParsedResolution() {
        val stream = StreamItem(
            name = "Big.Buck.Bunny.1080p.WEB-DL.mkv",
            title = "Big Buck Bunny",
            description = "1080p HDR",
            addonName = "Test Addon",
            addonId = "addon:test",
            clientResolve = StreamClientResolve(
                stream = StreamClientResolveStream(
                    raw = StreamClientResolveRaw(
                        parsed = StreamClientResolveParsed(resolution = "1080p"),
                    ),
                ),
            ),
        )

        assertEquals("1080p", stream.downloadQualityKey())
        assertEquals("1080p", stream.downloadQualityLabel())
    }

    @Test
    fun qualityKeyFallsBackToTextResolution() {
        val stream = StreamItem(
            name = "Show.S01E02.720p.HDTV.mkv",
            addonName = "Test Addon",
            addonId = "addon:test",
        )

        assertEquals("720p", stream.downloadQualityKey())
    }

    @Test
    fun qualityKeyStripsEpisodeCodeFromFallback() {
        val stream = StreamItem(
            name = "Show S01E02",
            addonName = "Test Addon",
            addonId = "addon:test",
        )

        assertEquals("show", stream.downloadQualityKey())
    }

    @Test
    fun qualityLabelFallsBackToStreamLabel() {
        val stream = StreamItem(
            name = "Show 2160p Remux",
            addonName = "Test Addon",
            addonId = "addon:test",
        )

        assertEquals("Show 2160p Remux", stream.downloadQualityLabel())
    }

    @Test
    fun logicalContentKeyDistinguishesEpisodesAndMovies() {
        assertEquals("show|2|3", downloadLogicalContentKey("show", 2, 3))
        assertEquals("show|movie", downloadLogicalContentKey("show", null, null))
        assertEquals("show|movie", downloadLogicalContentKey("show", 0, null))
    }
}

class DownloadsPendingGroupingTest {
    private fun pending(
        id: String,
        season: Int,
        episode: Int,
        episodeTitle: String,
    ) = PendingEpisodeDownload(
        id = id,
        contentType = "series",
        parentMetaId = "show",
        parentMetaType = "series",
        videoId = "video-$id",
        title = "Show",
        seasonNumber = season,
        episodeNumber = episode,
        episodeTitle = episodeTitle,
        providerName = "Provider",
        providerAddonId = "addon:provider",
        qualityKey = "1080p",
        createdAtEpochMs = 1_000L,
        nextAttemptAtEpochMs = 1_100L,
        expiresAtEpochMs = 1_000L + 24 * 60 * 60 * 1_000L,
    )

    @Test
    fun pendingSearchesGroupByShowAndSortBySeasonThenEpisode() {
        val episodes = listOf(
            pending("b", season = 2, episode = 1, episodeTitle = "Later"),
            pending("a", season = 1, episode = 2, episodeTitle = "Second"),
            pending("c", season = 1, episode = 1, episodeTitle = "First"),
        )

        val groups = episodes.groupByShow()

        assertEquals(1, groups.size)
        val group = groups.first()
        assertEquals("show", group.parentMetaId)
        assertEquals(listOf("c", "a", "b"), group.searches.map { it.id })
    }

    @Test
    fun groupsAreOrderedAlphabeticallyByTitle() {
        val zeta = PendingEpisodeDownload(
            id = "z",
            contentType = "series",
            parentMetaId = "zeta",
            parentMetaType = "series",
            videoId = "video-z",
            title = "Zeta Show",
            seasonNumber = 1,
            episodeNumber = 1,
            providerName = "Provider",
            providerAddonId = "addon:provider",
            qualityKey = "1080p",
            createdAtEpochMs = 1_000L,
            nextAttemptAtEpochMs = 1_100L,
            expiresAtEpochMs = 1_000L + 24 * 60 * 60 * 1_000L,
        )
        val alpha = zeta.copy(
            id = "a",
            parentMetaId = "alpha",
            videoId = "video-a",
            title = "Alpha Show",
        )

        val groups = listOf(zeta, alpha).groupByShow()

        assertEquals(listOf("alpha", "zeta"), groups.map { it.parentMetaId })
    }
}
