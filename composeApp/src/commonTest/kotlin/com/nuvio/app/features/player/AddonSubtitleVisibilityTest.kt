package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AddonSubtitleVisibilityTest {

    @Test
    fun selectedFilteredSubtitleIsPrependedFromUnfilteredEpisodeResults() {
        val selected = subtitle("selected", "es")
        val visible = subtitle("visible", "en")

        val result = ensureSelectedAddonSubtitleVisible(
            filteredSubtitles = listOf(visible),
            unfilteredSubtitles = listOf(visible, selected),
            selectedId = selected.selectionKey,
        )

        assertEquals(listOf(selected, visible), result)
        assertSame(selected, resolveSelectedAddonSubtitle(listOf(visible, selected), selected.selectionKey))
    }

    @Test
    fun selectedSubtitleIsNotPrependedTwiceWhenAlreadyVisible() {
        val selected = subtitle("selected", "en")
        val visible = listOf(selected, subtitle("other", "en"))

        val result = ensureSelectedAddonSubtitleVisible(
            filteredSubtitles = visible,
            unfilteredSubtitles = visible,
            selectedId = selected.selectionKey,
        )

        assertSame(visible, result)
        assertEquals(1, result.count { it.selectionKey == selected.selectionKey })
    }

    private fun subtitle(id: String, language: String) = AddonSubtitle(
        id = id,
        url = "https://example.com/$id.srt",
        language = language,
        display = id,
        addonName = "Addon",
    )
}
