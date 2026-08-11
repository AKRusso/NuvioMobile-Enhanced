package com.nuvio.app.features.profiles

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfilePayloadTest {
    @Test
    fun `edited profile payload preserves background and primary plugin choice`() {
        val payload = NuvioProfile(
            profileIndex = 2,
            name = "Secondary",
            backgroundUrl = "https://example.com/background.jpg",
            usesPrimaryAddons = false,
            usesPrimaryPlugins = true,
        ).toProfilePushPayload().copy(name = "Edited")

        assertEquals("Edited", payload.name)
        assertEquals("https://example.com/background.jpg", payload.backgroundUrl)
        assertTrue(payload.usesPrimaryPlugins)
    }

    @Test
    fun `upstream payload keeps plugin choice without sending enhanced background`() {
        val payload = ProfilePushPayload(
            profileIndex = 2,
            name = "Secondary",
            avatarColorHex = "#123456",
            usesPrimaryPlugins = true,
            backgroundUrl = "https://example.com/background.jpg",
        ).toUpstreamProfilePushPayload()

        val encoded = Json.encodeToString(payload)

        assertTrue(encoded.contains("\"uses_primary_plugins\":true"))
        assertFalse(encoded.contains("background_url"))
    }

    @Test
    fun `background merge preserves local values and honors explicit clear`() {
        val remote = listOf(NuvioProfile(profileIndex = 2))
        val localBackgrounds = mapOf(2 to "https://example.com/background.jpg")

        val preserved = mergeProfileBackgrounds(remote, localBackgrounds)
        val cleared = mergeProfileBackgrounds(
            remoteProfiles = remote,
            localBackgrounds = localBackgrounds,
            backgroundOverrides = mapOf(2 to null),
        )

        assertEquals("https://example.com/background.jpg", preserved.single().backgroundUrl)
        assertNull(cleared.single().backgroundUrl)
    }
}
