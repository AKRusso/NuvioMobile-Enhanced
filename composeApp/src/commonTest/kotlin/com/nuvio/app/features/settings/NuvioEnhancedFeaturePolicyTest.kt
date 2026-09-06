package com.nuvio.app.features.settings

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NuvioEnhancedFeaturePolicyTest {
    @Test
    fun `nuvio read is optional by default`() {
        assertFalse(NuvioEnhancedSettingsUiState().nuvioReadEnabled)
    }

    @Test
    fun `cinematic detail header is optional by default`() {
        assertFalse(NuvioEnhancedSettingsUiState().cinematicDetailHeaderEnabled)
        assertEquals(
            CinematicHeaderContentMode.Productions,
            NuvioEnhancedSettingsUiState().cinematicHeaderContentMode,
        )
    }

    @Test
    fun `tracking badge does not keep enhanced settings marked as new`() {
        val seenEnhancedFeatures = NuvioEnhancedFeature.entries
            .filter(NuvioEnhancedFeature::showInEnhancedSettings)
            .mapTo(mutableSetOf(), NuvioEnhancedFeature::id)
        val state = NuvioEnhancedSettingsUiState(seenFeatureIds = seenEnhancedFeatures)

        assertTrue(state.isNew(NuvioEnhancedFeature.AnimeTracking))
        assertFalse(state.hasNewFeatures)
    }

    @Test
    fun `unseen nuvio read marks enhanced settings as new`() {
        val seenFeatures = NuvioEnhancedFeature.entries
            .mapTo(mutableSetOf(), NuvioEnhancedFeature::id)
            .apply { remove(NuvioEnhancedFeature.NuvioRead.id) }
        val state = NuvioEnhancedSettingsUiState(seenFeatureIds = seenFeatures)

        assertTrue(state.isNew(NuvioEnhancedFeature.NuvioRead))
        assertTrue(state.hasNewFeatures)
    }
}
