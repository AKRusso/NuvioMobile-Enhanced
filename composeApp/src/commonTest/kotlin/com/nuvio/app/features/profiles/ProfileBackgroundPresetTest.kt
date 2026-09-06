package com.nuvio.app.features.profiles

import com.nuvio.app.core.ui.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileBackgroundPresetTest {
    @Test
    fun `only explicit preset values resolve to a preset`() {
        val explicitPreset = NuvioProfile(
            profileIndex = 1,
            backgroundUrl = ProfileBackgroundPreset.JADE.storedValue,
        )
        val customImage = NuvioProfile(
            profileIndex = 1,
            backgroundUrl = "https://example.com/background.jpg",
        )
        val automatic = NuvioProfile(profileIndex = 1)

        assertEquals(ProfileBackgroundPreset.JADE, profileBackgroundPreset(explicitPreset))
        assertNull(profileBackgroundPreset(customImage))
        assertNull(profileBackgroundPreset(automatic))
    }

    @Test
    fun `supporter themes map to their profile backgrounds`() {
        assertEquals(ProfileBackgroundPreset.GOLD, ProfileBackgroundPreset.fromTheme(AppTheme.GOLD))
        assertEquals(ProfileBackgroundPreset.JADE, ProfileBackgroundPreset.fromTheme(AppTheme.JADE))
        assertEquals(ProfileBackgroundPreset.ROSE_GOLD, ProfileBackgroundPreset.fromTheme(AppTheme.ROSE_GOLD))
        assertEquals(ProfileBackgroundPreset.ARCTIC_BLUE, ProfileBackgroundPreset.fromTheme(AppTheme.ARCTIC_BLUE))
        assertEquals(ProfileBackgroundPreset.GRAPHITE, ProfileBackgroundPreset.fromTheme(AppTheme.GRAPHITE))
        assertNull(ProfileBackgroundPreset.fromTheme(AppTheme.WHITE))
    }

    @Test
    fun `supporter theme wins presets while custom backgrounds win`() {
        val automatic = NuvioProfile(profileIndex = 1)
        val explicitPreset = NuvioProfile(
            profileIndex = 1,
            backgroundUrl = ProfileBackgroundPreset.JADE.storedValue,
        )
        val customImage = NuvioProfile(
            profileIndex = 1,
            backgroundUrl = "https://example.com/background.jpg",
        )

        assertEquals(ProfileBackgroundPreset.GOLD, effectiveProfileBackgroundPreset(automatic, AppTheme.GOLD))
        assertEquals(ProfileBackgroundPreset.GOLD, effectiveProfileBackgroundPreset(explicitPreset, AppTheme.GOLD))
        assertEquals(ProfileBackgroundPreset.JADE, effectiveProfileBackgroundPreset(explicitPreset, AppTheme.WHITE))
        assertNull(effectiveProfileBackgroundPreset(customImage, AppTheme.GOLD))
    }

    @Test
    fun `effective background changes with supporter theme when there is no custom image`() {
        val profile = NuvioProfile(
            profileIndex = 1,
            backgroundUrl = ProfileBackgroundPreset.JADE.storedValue,
        )

        val gold = effectiveProfileBackground(profile, AppTheme.GOLD)
        val roseGold = effectiveProfileBackground(profile, AppTheme.ROSE_GOLD)

        assertEquals(ProfileBackgroundPreset.GOLD, gold.preset)
        assertEquals(ProfileBackgroundPreset.ROSE_GOLD, roseGold.preset)
        assertNull(gold.customImageUrl)
        assertNull(roseGold.customImageUrl)
    }

    @Test
    fun `custom image remains unchanged across theme changes`() {
        val customUrl = "https://example.com/custom-background.jpg"
        val profile = NuvioProfile(profileIndex = 1, backgroundUrl = customUrl)

        AppTheme.entries.forEach { theme ->
            val effective = effectiveProfileBackground(profile, theme)
            assertNull(effective.preset)
            assertEquals(customUrl, effective.customImageUrl)
        }
    }
}
