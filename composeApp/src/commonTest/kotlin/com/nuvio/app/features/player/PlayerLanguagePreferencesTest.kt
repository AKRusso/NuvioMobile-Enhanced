package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerLanguagePreferencesTest {
    @Test
    fun normalizesCommonCodesAndNames() {
        assertEquals("en", normalizeLanguageCode("en"))
        assertEquals("pt-br", normalizeLanguageCode("pt_BR"))
        assertEquals("fr", normalizeLanguageCode("French SDH"))
    }

    @Test
    fun filtersAddonSubtitlesUsingNormalizedTargets() {
        val settings = PlayerSettingsUiState(
            preferredSubtitleLanguage = "pt-BR",
            subtitleStyle = SubtitleStyleState.DEFAULT.copy(showOnlyPreferredLanguages = true),
        )
        val subtitles = listOf(
            addonSubtitle("portuguese", "por"),
            addonSubtitle("english", "en"),
        )

        val filtered = filterAddonSubtitlesForSettings(subtitles, settings)

        assertEquals(listOf("portuguese"), filtered.map(AddonSubtitle::id))
        assertTrue(languageMatchesPreference("pt-BR", "pt"))
    }

    private fun addonSubtitle(id: String, language: String) = AddonSubtitle(
        id = id,
        url = "https://example.com/$id.srt",
        language = language,
        display = id,
        addonName = "Test",
    )
}
