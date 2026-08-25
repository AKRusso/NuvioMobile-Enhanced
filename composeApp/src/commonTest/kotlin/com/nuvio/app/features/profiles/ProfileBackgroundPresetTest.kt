package com.nuvio.app.features.profiles

import com.nuvio.app.core.ui.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileBackgroundPresetTest {
    @Test
    fun `supporter themes map to their matching automatic backgrounds`() {
        assertEquals(ProfileBackgroundPreset.GOLD, ProfileBackgroundPreset.fromTheme(AppTheme.GOLD))
        assertEquals(ProfileBackgroundPreset.JADE, ProfileBackgroundPreset.fromTheme(AppTheme.JADE))
        assertEquals(ProfileBackgroundPreset.ROSE_GOLD, ProfileBackgroundPreset.fromTheme(AppTheme.ROSE_GOLD))
        assertEquals(ProfileBackgroundPreset.ARCTIC_BLUE, ProfileBackgroundPreset.fromTheme(AppTheme.ARCTIC_BLUE))
        assertEquals(ProfileBackgroundPreset.GRAPHITE, ProfileBackgroundPreset.fromTheme(AppTheme.GRAPHITE))
        assertNull(ProfileBackgroundPreset.fromTheme(AppTheme.OCEAN))
    }

    @Test
    fun `explicit background overrides automatic theme background`() {
        val explicitPreset = NuvioProfile(
            profileIndex = 1,
            backgroundUrl = ProfileBackgroundPreset.JADE.storedValue,
        )
        val customImage = NuvioProfile(
            profileIndex = 1,
            backgroundUrl = "https://example.com/background.jpg",
        )
        val automatic = NuvioProfile(profileIndex = 1)

        assertEquals(
            ProfileBackgroundPreset.JADE,
            effectiveProfileBackgroundPreset(explicitPreset, AppTheme.GOLD),
        )
        assertNull(effectiveProfileBackgroundPreset(customImage, AppTheme.GOLD))
        assertEquals(
            ProfileBackgroundPreset.GOLD,
            effectiveProfileBackgroundPreset(automatic, AppTheme.GOLD),
        )
    }
}
