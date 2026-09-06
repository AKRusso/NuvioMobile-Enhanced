package com.nuvio.app.features.details

import com.nuvio.app.features.tmdb.TmdbSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MetaDetailsRepositoryTest {
    @Test
    fun `tmdb derived cache changes with language`() {
        val english = TmdbSettings(enabled = true, apiKey = "key", language = "en")
        val french = english.copy(language = "fr")

        assertNotEquals(
            tmdbEnrichmentSettingsFingerprint(english),
            tmdbEnrichmentSettingsFingerprint(french),
        )
    }

    @Test
    fun `tmdb derived cache changes with enrichment modules`() {
        val episodesEnabled = TmdbSettings(enabled = true, apiKey = "key", useEpisodes = true)
        val episodesDisabled = episodesEnabled.copy(useEpisodes = false)

        assertNotEquals(
            tmdbEnrichmentSettingsFingerprint(episodesEnabled),
            tmdbEnrichmentSettingsFingerprint(episodesDisabled),
        )
    }

    @Test
    fun `tmdb enrichment timeout scales for all seasons and remains bounded`() {
        assertEquals(10_000L, tmdbEnrichmentTimeoutMs(seasonCount = 0))
        assertEquals(40_000L, tmdbEnrichmentTimeoutMs(seasonCount = 12))
        assertEquals(60_000L, tmdbEnrichmentTimeoutMs(seasonCount = 100))
    }

    @Test
    fun `metadata request key distinguishes titles and types`() {
        assertEquals("movie:tt123", metaDetailsRequestKey(type = "movie", id = "tt123"))
        assertNotEquals(
            metaDetailsRequestKey(type = "movie", id = "tt123"),
            metaDetailsRequestKey(type = "series", id = "tt123"),
        )
    }
}
