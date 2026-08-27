package com.nuvio.app.features.details

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    @Test
    fun actionSectionIndexUsesItsTabGroupPosition() {
        val settings = MetaScreenSettingsUiState(
            tabLayout = true,
            items = listOf(
                section(MetaScreenSectionKey.OVERVIEW, order = 0, tabGroup = 1),
                section(MetaScreenSectionKey.ACTIONS, order = 1, tabGroup = 1),
                section(MetaScreenSectionKey.CAST, order = 2),
            ),
        )

        assertEquals(
            expected = 1,
            actual = detailActionSectionLazyListIndex(settings) { true },
        )
    }

    @Test
    fun disabledActionSectionHasNoListIndex() {
        val settings = MetaScreenSettingsUiState(
            items = listOf(
                section(MetaScreenSectionKey.ACTIONS, order = 0, enabled = false),
                section(MetaScreenSectionKey.OVERVIEW, order = 1),
            ),
        )

        assertNull(detailActionSectionLazyListIndex(settings) { true })
    }

    private fun section(
        key: MetaScreenSectionKey,
        order: Int,
        enabled: Boolean = true,
        tabGroup: Int? = null,
    ) = MetaScreenSectionItem(
        key = key,
        title = key.name,
        description = "",
        enabled = enabled,
        order = order,
        tabGroup = tabGroup,
    )
}
