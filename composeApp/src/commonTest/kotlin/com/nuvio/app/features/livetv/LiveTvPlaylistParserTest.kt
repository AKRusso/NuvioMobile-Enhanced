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
            #EXTVLCOPT:http-referrer=https://playlist-referrer.test/
            https://stream.test/trt.m3u8
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
        assertEquals("https://playlist-referrer.test/", channels.first().headers["Referer"])
        assertEquals("hls", channels.first().streamType)
    }

    @Test
    fun parsesMultipleCompressedEpgUrls() {
        val playlist = parseM3uPlaylistData(
            """
            #EXTM3U url-tvg="https://epg.test/main.xml.xz,https://epg.test/extra.xml.xz"
            #EXTINF:-1 tvg-id="RTP1.pt",RTP 1
            https://stream.test/rtp1.m3u8
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                "https://epg.test/main.xml.xz",
                "https://epg.test/extra.xml.xz",
            ),
            playlist.epgUrls,
        )
    }

    @Test
    fun parsesRealWorldXTvgUrlHeader() {
        val playlist = parseM3uPlaylistData(
            """
            #EXTM3U x-tvg-url="https://worker-9dd4.onrender.com/guide.xml.gz"
            #EXTINF:-1,Channel
            https://stream.test/live.m3u8
            """.trimIndent(),
        )

        assertEquals(listOf("https://worker-9dd4.onrender.com/guide.xml.gz"), playlist.epgUrls)
    }

    @Test
    fun parsesHeaderAliasesCaseInsensitivelyAndDeduplicatesUrls() {
        val playlist = parseM3uPlaylistData(
            """
            #eXtM3u X-TVG-URL=https://epg.test/a.xml; HTTPS://epg.test/b.xml URL-TVG='https://epg.test/a.xml' tvg-url="https://epg.test/c.xml?token=a%20b HTTPS://epg.test/b.xml ftp://epg.test/no.xml https://"
            #ExTiNf:-1,Channel
            https://stream.test/live.m3u8
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                "https://epg.test/a.xml",
                "HTTPS://epg.test/b.xml",
                "https://epg.test/c.xml?token=a%20b",
            ),
            playlist.epgUrls,
        )
    }

    @Test
    fun handlesBomCrLfQuotedCommasAndDirectivesBeforeStream() {
        val playlist = parseM3uPlaylistData(
            "\uFEFF#EXTM3U TVG-URL=https://epg.test/guide.xml\r\n" +
                "#EXTINF:-1 TVG-ID=channel.id TVG-NAME=Fallback TVG-LOGO=https://img.test/logo.png " +
                "GROUP-TITLE=\"News, World\" CATCHUP=append CATCHUP-DAYS=7 " +
                "CATCHUP-SOURCE=\"https://archive.test/{utc}?name=a%20b\",\"Channel, One\"\r\n" +
                "#EXTGRP:News\r\n#KODIPROP:inputstream.adaptive.manifest_type=hls\r\n\r\n" +
                "https://stream.test/live.m3u8\r\n",
        )

        val channel = playlist.channels.single()
        assertEquals(listOf("https://epg.test/guide.xml"), playlist.epgUrls)
        assertEquals("channel.id", channel.tvgId)
        assertEquals("Channel, One", channel.name)
        assertEquals("News, World", channel.group)
        assertEquals("https://img.test/logo.png", channel.logoUrl)
    }

    @Test
    fun ignoresMalformedRecordsAndKeepsIdsStableAcrossOrdering() {
        val first = parseM3uPlaylist(
            """
            #EXTM3U
            #EXTINF:-1,Broken
            not a stream URL
            #EXTINF:-1,One
            https://stream.test/one.m3u8
            #EXTINF:-1,Two
            https://stream.test/two.m3u8
            """.trimIndent(),
        )
        val reordered = parseM3uPlaylist(
            """
            #EXTM3U
            #EXTINF:-1,Two
            https://stream.test/two.m3u8
            #EXTINF:-1,One
            https://stream.test/one.m3u8
            """.trimIndent(),
        )

        assertEquals(2, first.size)
        assertEquals(
            first.associate { it.streamUrl to it.id },
            reordered.associate { it.streamUrl to it.id },
        )
    }

    @Test
    fun parsesInlineStreamHeaders() {
        val channels = parseM3uPlaylist(
            """
            #EXTM3U
            #EXTINF:-1,Header Channel
            https://stream.test/header.m3u8|Referer=https://example.test
            """.trimIndent(),
        )

        assertEquals(1, channels.size)
        assertEquals("https://example.test", channels.first().headers["Referer"])
    }

    @Test
    fun marksMatroskaStreamsFromPlaylist() {
        val channels = parseM3uPlaylist(
            """
            #EXTM3U
            #EXTINF:-1,Movie Stream
            #EXTVLCOPT:http-referrer=https://example.test/
            https://stream.test/movie.mkv
            """.trimIndent(),
        )

        assertEquals(1, channels.size)
        assertEquals("matroska", channels.first().streamType)
        assertEquals("https://example.test/", channels.first().headers["Referer"])
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

    @Test
    fun retainsCurrentAndFutureXmlTvProgrammesForFavoriteChannel() {
        val schedule = parseXmlTvProgrammeSchedule(
            content = """
                <tv>
                    <programme start="20240101000000 +0000" stop="20240101003000 +0000" channel="one">
                        <title>Past</title>
                    </programme>
                    <programme start="20240101003000 +0000" stop="20240101010000 +0000" channel="one">
                        <title>Current &amp; Live</title>
                    </programme>
                    <programme start="20240101010000 +0000" stop="20240101020000 +0000" channel="one">
                        <title>Future</title>
                    </programme>
                </tv>
            """.trimIndent(),
            nowEpochMs = 1704069900000L,
            relevantChannelIds = setOf("one"),
            retainedScheduleChannelIds = setOf("one"),
        )

        assertEquals(
            listOf("Current & Live", "Future"),
            schedule["one"]?.map(LiveTvProgramme::title),
        )
        assertEquals("00:30 - 01:00", schedule["one"]?.first()?.timeLabel)

        val current = currentXmlTvProgrammes(schedule, nowEpochMs = 1704069900000L)
        assertEquals("Current & Live", current["one"]?.title)
    }

    @Test
    fun retainsAvailableXmlTvProgrammeDetails() {
        val schedule = parseXmlTvProgrammeSchedule(
            content = """
                <tv>
                    <programme start="20240101003000 +0000" stop="20240101010000 +0000" channel="one">
                        <title>Current &amp; Live</title>
                        <sub-title>Episode title</sub-title>
                        <desc><![CDATA[Full provider description.]]></desc>
                        <category>News</category>
                        <category>Live</category>
                        <episode-num system="xmltv_ns">0.1.</episode-num>
                        <icon src="https://images.test/programme.jpg" />
                        <rating system="TV-PG"><value>12</value></rating>
                        <credits>
                            <director>Director One</director>
                            <actor>Actor One</actor>
                        </credits>
                        <date>20240101</date>
                        <country>Portugal</country>
                        <language>pt</language>
                        <new />
                        <premiere />
                    </programme>
                </tv>
            """.trimIndent(),
            nowEpochMs = 1704069900000L,
            relevantChannelIds = setOf("one"),
            retainedScheduleChannelIds = setOf("one"),
        )

        val programme = schedule.getValue("one").single()
        assertEquals("Episode title", programme.subtitle)
        assertEquals("Full provider description.", programme.description)
        assertEquals(listOf("News", "Live"), programme.categories)
        assertEquals("S01E02", programme.episode)
        assertEquals("https://images.test/programme.jpg", programme.iconUrl)
        assertEquals("12 · TV-PG", programme.rating)
        assertEquals(listOf("director", "actor"), programme.credits.map(LiveTvProgrammeCredit::role))
        assertEquals("2024-01-01", programme.date)
        assertEquals("Portugal", programme.country)
        assertEquals("pt", programme.language)
        assertEquals(true, programme.isNew)
        assertEquals(true, programme.isPremiere)
    }

    @Test
    fun retainsCurrentForAllRelevantChannelsButFutureOnlyForFavorites() {
        val schedule = parseXmlTvProgrammeSchedule(
            content = """
                <tv>
                    <programme start="20240101003000 +0000" stop="20240101010000 +0000" channel="favorite">
                        <title>Favorite Current</title>
                    </programme>
                    <programme start="20240101010000 +0000" stop="20240101020000 +0000" channel="favorite">
                        <title>Favorite Future</title>
                    </programme>
                    <programme start="20240101003000 +0000" stop="20240101010000 +0000" channel="other">
                        <title>Other Current</title>
                    </programme>
                    <programme start="20240101010000 +0000" stop="20240101020000 +0000" channel="other">
                        <title>Other Future</title>
                    </programme>
                    <programme start="20240101003000 +0000" stop="20240101010000 +0000" channel="provider-only">
                        <title>Provider Only</title>
                    </programme>
                </tv>
            """.trimIndent(),
            nowEpochMs = 1704069900000L,
            relevantChannelIds = setOf("favorite", "other"),
            retainedScheduleChannelIds = setOf("favorite"),
        )

        assertEquals(
            listOf("Favorite Current", "Favorite Future"),
            schedule["favorite"]?.map(LiveTvProgramme::title),
        )
        assertEquals(listOf("Other Current"), schedule["other"]?.map(LiveTvProgramme::title))
        assertEquals(null, schedule["provider-only"])
    }

    @Test
    fun dropsProviderProgrammesWhenNoChannelIdsAreRelevant() {
        val schedule = parseXmlTvProgrammeSchedule(
            content = """
                <tv>
                    <programme start="20240101003000 +0000" stop="20240101010000 +0000" channel="provider-one">
                        <title>Provider Current</title>
                    </programme>
                    <programme start="20240101010000 +0000" stop="20240101020000 +0000" channel="provider-one">
                        <title>Provider Future</title>
                    </programme>
                </tv>
            """.trimIndent(),
            nowEpochMs = 1704069900000L,
            relevantChannelIds = emptySet(),
            retainedScheduleChannelIds = emptySet(),
        )

        assertEquals(emptyMap(), schedule)
    }

    @Test
    fun mergesAndDeduplicatesXmlTvSchedules() {
        val first = LiveTvProgramme(
            title = "First",
            startEpochMs = 1000L,
            stopEpochMs = 2000L,
            timeLabel = "00:00 - 00:30",
        )
        val second = LiveTvProgramme(
            title = "Second",
            startEpochMs = 2000L,
            stopEpochMs = 3000L,
            timeLabel = "00:30 - 01:00",
        )

        val merged = mergeXmlTvProgrammeSchedules(
            listOf(
                mapOf("one" to listOf(second, first)),
                mapOf("one" to listOf(first)),
            ),
        )

        assertEquals(listOf(first, second), merged["one"])
    }
}
