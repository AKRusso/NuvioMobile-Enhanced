package com.nuvio.app.features.livetv

import kotlin.test.Test
import kotlin.test.assertEquals

class LiveTvEpgSourcesTest {
    @Test
    fun cacheFallbackDecisionPrefersFreshThenCache() {
        assertEquals(LiveTvEpgContentChoice.Fresh, chooseLiveTvEpgContent(true, true))
        assertEquals(LiveTvEpgContentChoice.Cached, chooseLiveTvEpgContent(false, true))
        assertEquals(LiveTvEpgContentChoice.Failed, chooseLiveTvEpgContent(false, false))
    }
}
