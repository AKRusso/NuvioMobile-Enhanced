package com.nuvio.app.features.livetv

import kotlin.test.Test
import kotlin.test.assertEquals

class LiveTvPlaylistParserTest {
    @Test
    fun parsesChannelMetadataAndHeaders() {
        val playlist = parseM3uPlaylistData(
            """
            #EXTM3U url-tvg="https://epg.test/guide.xml"
            #EXTINF:-1 tvg-id="trt1.tr" tvg-name="TRT 1" tvg-logo="https://img.test/trt.png" group-title="Ulusal",TRT 1 HD
            #EXTVLCOPT:http-user-agent=Nuvio
            https://stream.test/trt.m3u8|Referer=https://example.test
            """.trimIndent(),
        )
        val channels = playlist.channels

        assertEquals(1, channels.size)
        assertEquals(listOf("https://epg.test/guide.xml"), playlist.epgUrls)
        assertEquals("trt1.tr", channels.first().tvgId)
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

    @Test
    fun skipsCategoryHeadingLikeEntries() {
        val channels = parseM3uPlaylist(
            """
            #EXTM3U
            #EXTINF:-1,#### HABER KANALLARI ####
            https://stream.test/haber.m3u8
            #EXTINF:-1,TRT 1 HD
            https://stream.test/trt1.m3u8
            """.trimIndent(),
        )

        assertEquals(1, channels.size)
        assertEquals("TRT 1 HD", channels.first().name)
    }
}
