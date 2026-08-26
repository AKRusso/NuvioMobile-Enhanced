package com.nuvio.app.features.profiles

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
}
