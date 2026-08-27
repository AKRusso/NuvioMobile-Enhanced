package com.nuvio.app.core.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TvLayoutProfileTest {
    @Test
    fun `normal tablets never use tv layout`() {
        assertFalse(
            shouldEnableTvLayoutProfile(
                isTelevision = false,
                isWideLandscapeWindow = false,
                isSamsungDesktopMode = false,
                builtInSmallestWidthDp = 800,
            ),
        )
        assertFalse(
            shouldEnableTvLayoutProfile(
                isTelevision = false,
                isWideLandscapeWindow = true,
                isSamsungDesktopMode = false,
                builtInSmallestWidthDp = 600,
            ),
        )
        assertFalse(
            shouldEnableTvLayoutProfile(
                isTelevision = false,
                isWideLandscapeWindow = true,
                isSamsungDesktopMode = false,
                builtInSmallestWidthDp = 800,
            ),
        )
    }

    @Test
    fun `samsung tablets never use tv layout even in dex`() {
        assertFalse(
            shouldEnableTvLayoutProfile(
                isTelevision = false,
                isWideLandscapeWindow = false,
                isSamsungDesktopMode = true,
                builtInSmallestWidthDp = 800,
            ),
        )
        assertFalse(
            shouldEnableTvLayoutProfile(
                isTelevision = false,
                isWideLandscapeWindow = true,
                isSamsungDesktopMode = true,
                builtInSmallestWidthDp = 600,
            ),
        )
        assertFalse(
            shouldEnableTvLayoutProfile(
                isTelevision = false,
                isWideLandscapeWindow = true,
                isSamsungDesktopMode = true,
                builtInSmallestWidthDp = 800,
            ),
        )
    }

    @Test
    fun `resizing a dex window never changes a physical tablet into tv layout`() {
        listOf(false, true).forEach { isWideLandscapeWindow ->
            assertFalse(
                shouldEnableTvLayoutProfile(
                    isTelevision = false,
                    isWideLandscapeWindow = isWideLandscapeWindow,
                    isSamsungDesktopMode = true,
                    builtInSmallestWidthDp = 720,
                ),
            )
        }
    }

    @Test
    fun `phone dex uses tv layout only in a wide landscape window`() {
        assertTrue(
            shouldEnableTvLayoutProfile(
                isTelevision = false,
                isWideLandscapeWindow = true,
                isSamsungDesktopMode = true,
                builtInSmallestWidthDp = 599,
            ),
        )
        assertFalse(
            shouldEnableTvLayoutProfile(
                isTelevision = false,
                isWideLandscapeWindow = false,
                isSamsungDesktopMode = true,
                builtInSmallestWidthDp = 599,
            ),
        )
    }

    @Test
    fun `normal phones never use tv layout`() {
        assertFalse(
            shouldEnableTvLayoutProfile(
                isTelevision = false,
                isWideLandscapeWindow = true,
                isSamsungDesktopMode = false,
                builtInSmallestWidthDp = 411,
            ),
        )
    }

    @Test
    fun `unknown physical device size defaults to non tv layout`() {
        assertFalse(
            shouldEnableTvLayoutProfile(
                isTelevision = false,
                isWideLandscapeWindow = true,
                isSamsungDesktopMode = true,
                builtInSmallestWidthDp = null,
            ),
        )
    }

    @Test
    fun `android tv always uses tv layout`() {
        assertTrue(
            shouldEnableTvLayoutProfile(
                isTelevision = true,
                isWideLandscapeWindow = false,
                isSamsungDesktopMode = false,
                builtInSmallestWidthDp = null,
            ),
        )
    }
}
