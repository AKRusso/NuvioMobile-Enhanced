package com.nuvio.app.features.anime

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimeTrackingRepositoryTest {
    @Test
    fun acceptsOnlyTheConfiguredCallbackEndpoint() {
        val redirect = "nuvioenhanced://auth/anilist"

        assertTrue(isExpectedAnimeAuthCallback("$redirect?code=123&state=abc", redirect))
        assertTrue(isExpectedAnimeAuthCallback("$redirect/#access_token=token", redirect))
        assertFalse(isExpectedAnimeAuthCallback("nuvioenhanced://auth/anilist-evil?code=123", redirect))
        assertFalse(isExpectedAnimeAuthCallback("nuvioenhanced://attacker/anilist?code=123", redirect))
        assertFalse(isExpectedAnimeAuthCallback("https://auth/anilist?code=123", redirect))
    }
}
