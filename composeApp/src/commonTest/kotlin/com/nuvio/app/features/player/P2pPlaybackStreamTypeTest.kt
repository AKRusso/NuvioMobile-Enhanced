package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class P2pPlaybackStreamTypeTest {
    @Test
    fun matroskaFilenameDoesNotForceAPlaybackEngine() {
        assertNull(
            p2pPlaybackStreamType(streamType = null, filename = "Episode.01.1080p.mkv"),
        )
    }

    @Test
    fun explicitStreamTypeTakesPrecedenceOverFilename() {
        assertEquals(
            "hls",
            p2pPlaybackStreamType(streamType = "HLS", filename = "Episode.01.mkv"),
        )
    }

    @Test
    fun unknownContainerKeepsAnUnknownTypeUnset() {
        assertNull(p2pPlaybackStreamType(streamType = null, filename = "Episode.01"))
    }

    @Test
    fun directStreamTypeDoesNotForceAPlaybackEngine() {
        assertNull(p2pPlaybackStreamType(streamType = "direct", filename = "Episode.01.mkv"))
    }
}
