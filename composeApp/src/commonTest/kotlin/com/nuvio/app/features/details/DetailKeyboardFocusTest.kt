package com.nuvio.app.features.details

import kotlin.test.Test
import kotlin.test.assertEquals

class DetailKeyboardFocusTest {
    @Test
    fun actionSectionIndexFollowsCustomSectionOrder() {
        val settings = MetaScreenSettingsUiState(
            items = listOf(
                section(MetaScreenSectionKey.OVERVIEW, order = 0),
                section(MetaScreenSectionKey.CAST, order = 1),
                section(MetaScreenSectionKey.ACTIONS, order = 2),
            ),
        )

        assertEquals(
            expected = 3,
            actual = detailActionSectionLazyListIndex(settings) { true },
        )
    }

    @Test
    fun actionSectionIndexSkipsHiddenAndEmptySections() {
        val settings = MetaScreenSettingsUiState(
            items = listOf(
                section(MetaScreenSectionKey.CAST, order = 0, enabled = false),
                section(MetaScreenSectionKey.COMMENTS, order = 1),
                section(MetaScreenSectionKey.ACTIONS, order = 2),
            ),
        )

        assertEquals(
            expected = 1,
            actual = detailActionSectionLazyListIndex(settings) { key ->
                key != MetaScreenSectionKey.COMMENTS
            },
        )
    }

    private fun section(
        key: MetaScreenSectionKey,
        order: Int,
        enabled: Boolean = true,
    ) = MetaScreenSectionItem(
        key = key,
        title = key.name,
        description = "",
        enabled = enabled,
        order = order,
    )
}
