package com.nuvio.app.features.livetv

import kotlin.test.Test
import kotlin.test.assertEquals

class LiveTvPlaylistParserTest {
    @Test
    fun parsesChannelMetadataAndHeaders() {
        val channels = parseM3uPlaylist(
            """
            #EXTM3U
            #EXTINF:-1 tvg-name="TRT 1" tvg-logo="https://img.test/trt.png" group-title="Ulusal",TRT 1 HD
            #EXTVLCOPT:http-user-agent=Nuvio
            https://stream.test/trt.m3u8|Referer=https://example.test
            """.trimIndent(),
        )

        assertEquals(1, channels.size)
        assertEquals("TRT 1 HD", channels.first().name)
        assertEquals("Ulusal", channels.first().group)
        assertEquals("https://img.test/trt.png", channels.first().logoUrl)
        assertEquals("Nuvio", channels.first().headers["User-Agent"])
        assertEquals("https://example.test", channels.first().headers["Referer"])
    }

    @Test
    fun removesDuplicateStreamUrls() {
        val channels = parseM3uPlaylist(
            """
            #EXTM3U
            #EXTINF:-1,Channel One
            https://stream.test/live.m3u8
            #EXTINF:-1,Channel One Duplicate
            https://stream.test/live.m3u8
            """.trimIndent(),
        )

        assertEquals(1, channels.size)
        assertEquals("Channel One", channels.first().name)
    }
}
