package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubtitleStyleStateTest {
    @Test
    fun customFontDisablesLibassToKeepTheSelectedTypeface() {
        val style = SubtitleStyleState(
            fontFamily = SubtitleFontFamily.Custom,
            customFontPath = "/private/fonts/custom.ttf",
        )

        assertTrue(style.usesCustomFont())
        assertFalse(style.shouldUseLibass(libassEnabled = true))
    }

    @Test
    fun emptyCustomFontPathDoesNotDisableLibass() {
        val style = SubtitleStyleState(fontFamily = SubtitleFontFamily.Custom)

        assertFalse(style.usesCustomFont())
        assertTrue(style.shouldUseLibass(libassEnabled = true))
    }
}
