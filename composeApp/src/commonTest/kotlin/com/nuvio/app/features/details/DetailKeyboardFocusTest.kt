package com.nuvio.app.features.details

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test
    fun productionSectionAvailabilityFollowsCinematicHeaderWithoutChangingSettings() {
        assertTrue(
            detailProductionSectionVisible(
                hasProductionData = true,
                cinematicDetailHeaderEnabled = false,
            ),
        )
        assertFalse(
            detailProductionSectionVisible(
                hasProductionData = true,
                cinematicDetailHeaderEnabled = true,
            ),
        )
        assertFalse(
            detailProductionSectionVisible(
                hasProductionData = false,
                cinematicDetailHeaderEnabled = false,
            ),
        )
    }

    @Test
    fun cinematicHeaderRemovesOnlyItsBackdropBridge() {
        assertFalse(
            shouldRenderDetailBackdropBridge(
                backgroundMode = MetaScreenBackgroundMode.Cinematic,
                cinematicDetailHeaderEnabled = true,
            ),
        )
        assertFalse(
            shouldRenderDetailBackdropBridge(
                backgroundMode = MetaScreenBackgroundMode.DominantColor,
                cinematicDetailHeaderEnabled = true,
            ),
        )
        assertTrue(
            shouldRenderDetailBackdropBridge(
                backgroundMode = MetaScreenBackgroundMode.Cinematic,
                cinematicDetailHeaderEnabled = false,
            ),
        )
        assertFalse(
            shouldRenderDetailBackdropBridge(
                backgroundMode = MetaScreenBackgroundMode.Normal,
                cinematicDetailHeaderEnabled = false,
            ),
        )
    }

    @Test
    fun cinematicHeaderUsesDescriptionOnlyAndDisablesNuvioRead() {
        assertTrue(shouldUseDescriptionOnlyDetailOverview(cinematicDetailHeaderEnabled = true))
        assertFalse(shouldUseDescriptionOnlyDetailOverview(cinematicDetailHeaderEnabled = false))
        assertFalse(
            shouldEnableDetailNuvioRead(
                nuvioReadEnabled = true,
                cinematicDetailHeaderEnabled = true,
            ),
        )
        assertTrue(
            shouldEnableDetailNuvioRead(
                nuvioReadEnabled = true,
                cinematicDetailHeaderEnabled = false,
            ),
        )
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
