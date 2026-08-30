package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerSubtitleMatchingTest {
    @Test
    fun `normalizes ISO aliases and regional codes`() {
        assertEquals("en", SubtitleLanguageMatching.normalizeLanguageCode("ENG"))
        assertEquals("fr", SubtitleLanguageMatching.normalizeLanguageCode("fre"))
        assertEquals("pt-br", SubtitleLanguageMatching.normalizeLanguageCode("pt_BR"))
        assertEquals("es-419", SubtitleLanguageMatching.normalizeLanguageCode("spl"))
        assertEquals("zh-tw", SubtitleLanguageMatching.normalizeLanguageCode("zht"))
    }

    @Test
    fun `normalizes regional language names`() {
        assertEquals("pt-br", SubtitleLanguageMatching.normalizeLanguageCode("Portuguese (Brazilian)"))
        assertEquals("pt", SubtitleLanguageMatching.normalizeLanguageCode("Portuguese Portugal"))
        assertEquals("es-419", SubtitleLanguageMatching.normalizeLanguageCode("Spanish Latinoamerica"))
        assertEquals("es", SubtitleLanguageMatching.normalizeLanguageCode("Spanish Spain"))
    }

    @Test
    fun `detects Portuguese and Spanish variants from track metadata`() {
        assertEquals("pt-br", SubtitleLanguageMatching.detectTrackLanguageVariant("por", "Portuguese (BR)", null))
        assertEquals("pt", SubtitleLanguageMatching.detectTrackLanguageVariant("pt", "European Portuguese", null))
        assertEquals("es-419", SubtitleLanguageMatching.detectTrackLanguageVariant("spa", "Español Latino", null))
        assertEquals("es", SubtitleLanguageMatching.detectTrackLanguageVariant("es", "Spanish Castilian", null))
    }

    @Test
    fun `regional matching does not collapse explicit Portuguese or Spanish variants`() {
        assertTrue(SubtitleLanguageMatching.matchesLanguageCode("por", "pt"))
        assertFalse(SubtitleLanguageMatching.matchesLanguageCode("pt-br", "pt"))
        assertTrue(SubtitleLanguageMatching.matchesLanguageCode("spl", "es-419"))
        assertFalse(SubtitleLanguageMatching.matchesLanguageCode("es-419", "es"))
    }
}
