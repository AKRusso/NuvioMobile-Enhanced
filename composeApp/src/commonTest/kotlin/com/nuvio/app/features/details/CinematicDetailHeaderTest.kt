package com.nuvio.app.features.details

import androidx.compose.ui.graphics.Color
import com.nuvio.app.features.details.components.analyzeCinematicLogoPixels
import com.nuvio.app.features.details.components.cinematicDetailHeaderData
import com.nuvio.app.features.details.components.cinematicLogoLayoutMetrics
import com.nuvio.app.features.details.components.cinematicTrailerPlayWhenReady
import com.nuvio.app.features.details.components.detailContrastRatio
import com.nuvio.app.features.details.components.detailHeaderMetadataData
import com.nuvio.app.features.details.components.readableDetailContentColor
import com.nuvio.app.features.details.components.shouldAdaptBrandLogoColor
import com.nuvio.app.features.details.components.shouldShowCinematicHeaderSettings
import com.nuvio.app.features.settings.CinematicHeaderContentMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CinematicDetailHeaderTest {
    @Test
    fun `movie header prefers a production company with branding`() {
        val meta = MetaDetails(
            id = "movie",
            type = "movie",
            name = "Movie",
            releaseInfo = "2026",
            runtime = "125 min",
            productionCompanies = listOf(
                MetaCompany(name = "No Logo"),
                MetaCompany(name = "Studio", logo = "https://example.com/studio.png", tmdbId = 10),
            ),
        )

        val data = cinematicDetailHeaderData(meta)

        assertFalse(data.isSeriesLike)
        assertEquals(listOf("Studio", "No Logo"), data.brands.map(MetaCompany::name))
        assertEquals("company", data.brandKind)
        assertNull(data.seasonCount)
        assertNull(data.episodeCount)
        assertTrue(!data.runtime.isNullOrBlank())
    }

    @Test
    fun `series header prefers network and excludes specials from counts`() {
        val meta = MetaDetails(
            id = "series",
            type = "series",
            name = "Series",
            networks = listOf(MetaCompany(name = "Network", logo = "https://example.com/network.png")),
            productionCompanies = listOf(MetaCompany(name = "Studio")),
            videos = listOf(
                MetaVideo(id = "special", title = "Special", season = 0, episode = 1),
                MetaVideo(id = "s1e1", title = "Episode 1", season = 1, episode = 1),
                MetaVideo(id = "s1e2", title = "Episode 2", season = 1, episode = 2),
            ),
        )

        val data = cinematicDetailHeaderData(meta)

        assertTrue(data.isSeriesLike)
        assertEquals(listOf("Network"), data.brands.map(MetaCompany::name))
        assertEquals("network", data.brandKind)
        assertEquals(1, data.seasonCount)
        assertEquals(2, data.episodeCount)
        assertNull(data.runtime)
    }

    @Test
    fun `tv type uses network branding`() {
        val data = cinematicDetailHeaderData(
            MetaDetails(
                id = "tv",
                type = "tv",
                name = "TV Show",
                networks = listOf(
                    MetaCompany(name = "Netflix", logo = "https://example.com/netflix.png"),
                    MetaCompany(name = "HBO", logo = "https://example.com/hbo.png"),
                ),
                productionCompanies = listOf(MetaCompany(name = "Studio")),
            ),
        )

        assertTrue(data.isSeriesLike)
        assertEquals(listOf("Netflix", "HBO"), data.brands.map(MetaCompany::name))
        assertEquals("network", data.brandKind)
    }

    @Test
    fun `cinematic trailer follows autoplay manual override and visibility`() {
        assertTrue(cinematicTrailerPlayWhenReady(null, autoPlay = true, playbackAllowed = true))
        assertFalse(cinematicTrailerPlayWhenReady(false, autoPlay = true, playbackAllowed = true))
        assertTrue(cinematicTrailerPlayWhenReady(true, autoPlay = false, playbackAllowed = true))
        assertFalse(cinematicTrailerPlayWhenReady(true, autoPlay = false, playbackAllowed = false))
    }

    @Test
    fun `header content chooses the higher contrast foreground`() {
        assertEquals(Color(0xFFF5F7F8), readableDetailContentColor(Color(0xFF101214)))
        assertEquals(Color(0xFF111111), readableDetailContentColor(Color(0xFFF2EEE8)))
        assertTrue(detailContrastRatio(readableDetailContentColor(Color.Black), Color.Black) > 15f)
    }

    @Test
    fun `brand logos preserve readable colors and adapt colors that merge into the background`() {
        assertFalse(shouldAdaptBrandLogoColor(Color(0xFFE50914), Color.Black))
        assertTrue(shouldAdaptBrandLogoColor(Color(0xFF111111), Color.Black))
        assertTrue(shouldAdaptBrandLogoColor(Color.White, Color(0xFFF2EEE8)))
        assertFalse(shouldAdaptBrandLogoColor(Color.Black, Color.White))
        assertFalse(shouldAdaptBrandLogoColor(Color.White, Color.White, hasOpaqueBackground = true))
    }

    @Test
    fun `logo layout keeps up to four items in one row and scrolls after that`() {
        val single = cinematicLogoLayoutMetrics(1, 282f, isTablet = false, squareItems = false)
        val four = cinematicLogoLayoutMetrics(4, 282f, isTablet = false, squareItems = false)
        val five = cinematicLogoLayoutMetrics(5, 282f, isTablet = false, squareItems = false)
        val fiveProviders = cinematicLogoLayoutMetrics(5, 282f, isTablet = false, squareItems = true)

        assertEquals(120f, single.itemWidthDp)
        assertFalse(single.scrollable)
        assertEquals(64.5f, four.itemWidthDp)
        assertFalse(four.scrollable)
        assertEquals(72f, five.itemWidthDp)
        assertTrue(five.scrollable)
        assertTrue(fiveProviders.scrollable)
    }

    @Test
    fun `hidden cinematic content removes the detail settings gear`() {
        assertTrue(shouldShowCinematicHeaderSettings(CinematicHeaderContentMode.Productions))
        assertTrue(shouldShowCinematicHeaderSettings(CinematicHeaderContentMode.WhereToWatch))
        assertFalse(shouldShowCinematicHeaderSettings(CinematicHeaderContentMode.Hidden))
    }

    @Test
    fun `logo analysis crops transparent margins and detects opaque backgrounds`() {
        val cropped = analyzeCinematicLogoPixels(width = 10, height = 10) { x, y ->
            if (x in 2..7 && y in 4..5) Color.Red else Color.Transparent
        }
        val opaque = analyzeCinematicLogoPixels(width = 3, height = 3) { _, _ -> Color.White }

        assertEquals(1, cropped?.sourceX)
        assertEquals(3, cropped?.sourceY)
        assertEquals(8, cropped?.sourceWidth)
        assertEquals(4, cropped?.sourceHeight)
        assertEquals(Color.Red, cropped?.averageVisibleColor)
        assertEquals(0.12f, cropped?.opaqueCoverage ?: 0f, 0.001f)
        assertFalse(cropped?.hasOpaqueBackground == true)
        assertTrue(opaque?.hasOpaqueBackground == true)
    }

    @Test
    fun `semantic metadata keeps age rating and suppresses duplicate raw imdb`() {
        val meta = MetaDetails(
            id = "rated",
            type = "movie",
            name = "Rated",
            ageRating = " TV-MA ",
            imdbRating = "8.4",
            externalRatings = listOf(MetaExternalRating(source = "imdb", value = 8.4)),
        )

        val metadata = detailHeaderMetadataData(meta)
        val cinematicData = cinematicDetailHeaderData(meta)

        assertEquals("TV-MA", metadata.ageRating)
        assertNull(metadata.rawImdbRating)
        assertEquals("TV-MA", cinematicData.ageRating)
        assertNull(cinematicData.imdbRating)
    }

    @Test
    fun `semantic metadata exposes valid raw imdb when no external imdb exists`() {
        val metadata = detailHeaderMetadataData(
            MetaDetails(
                id = "raw-rating",
                type = "movie",
                name = "Raw Rating",
                imdbRating = " 7.9 ",
            ),
        )

        assertEquals("7.9", metadata.rawImdbRating)
    }

    @Test
    fun `detail trailer sound is off by default`() {
        assertFalse(MetaScreenSettingsUiState().heroTrailerSoundEnabled)
    }
}
