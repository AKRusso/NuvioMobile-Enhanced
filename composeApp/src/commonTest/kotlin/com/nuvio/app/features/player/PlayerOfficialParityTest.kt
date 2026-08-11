package com.nuvio.app.features.player

import com.nuvio.app.features.streams.StreamAutoPlayMode
import com.nuvio.app.features.streams.StreamBehaviorHints
import com.nuvio.app.features.streams.StreamItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerOfficialParityTest {
    @Test
    fun `manual next episode autoplay without fallback requires binge group`() {
        assertTrue(
            shouldRestrictNextEpisodeAutoPlayToBingeGroup(
                mode = StreamAutoPlayMode.MANUAL,
                autoPlayNextEpisodeEnabled = true,
                fallbackEnabled = false,
                preferBingeGroup = true,
            ),
        )
    }

    @Test
    fun `manual next episode autoplay with fallback can use another stream`() {
        assertFalse(
            shouldRestrictNextEpisodeAutoPlayToBingeGroup(
                mode = StreamAutoPlayMode.MANUAL,
                autoPlayNextEpisodeEnabled = true,
                fallbackEnabled = true,
                preferBingeGroup = true,
            ),
        )
    }

    @Test
    fun `manual binge group preference remains binge only without episode autoplay`() {
        assertTrue(
            shouldRestrictNextEpisodeAutoPlayToBingeGroup(
                mode = StreamAutoPlayMode.MANUAL,
                autoPlayNextEpisodeEnabled = false,
                fallbackEnabled = true,
                preferBingeGroup = true,
            ),
        )
    }

    @Test
    fun `current provider cannot bypass disabled binge group fallback`() {
        val currentProviderStream = StreamItem(
            addonName = "Current provider",
            addonId = "current-provider",
            url = "https://example.com/video.m3u8",
            behaviorHints = StreamBehaviorHints(bingeGroup = "other-group"),
        )

        assertEquals(
            emptyList(),
            currentProviderNextEpisodeCandidates(
                streams = listOf(currentProviderStream),
                currentProviderAddonId = "current-provider",
                currentProviderName = null,
                preferredBingeGroup = "wanted-group",
                bingeGroupOnly = true,
            ),
        )
        assertEquals(
            listOf(currentProviderStream),
            currentProviderNextEpisodeCandidates(
                streams = listOf(currentProviderStream),
                currentProviderAddonId = "current-provider",
                currentProviderName = null,
                preferredBingeGroup = "wanted-group",
                bingeGroupOnly = false,
            ),
        )
    }

    @Test
    fun `picture in picture hides regular and quiet controls`() {
        assertFalse(
            shouldShowPlayerControlsShell(
                isInPip = true,
                controlsVisible = true,
                showParentalGuide = false,
                playerControlsLocked = false,
                showQuietDeviceStatusOverlay = false,
            ),
        )
        assertFalse(
            shouldShowPlayerControlsShell(
                isInPip = true,
                controlsVisible = false,
                showParentalGuide = false,
                playerControlsLocked = false,
                showQuietDeviceStatusOverlay = true,
            ),
        )
    }
}
