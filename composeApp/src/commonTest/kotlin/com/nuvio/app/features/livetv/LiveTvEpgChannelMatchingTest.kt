package com.nuvio.app.features.livetv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LiveTvEpgChannelMatchingTest {
    @Test
    fun resolvesExactIdBeforeAliases() {
        val resolved = resolve(xmlChannels("exact" to "Different"), tvgId = "exact", name = "Other")

        assertEquals("exact", resolved.tvgId)
    }

    @Test
    fun resolvesIdCaseInsensitively() {
        val resolved = resolve(xmlChannels("RTP1.pt" to "RTP 1"), tvgId = "rtp1.PT", name = "Other")

        assertEquals("RTP1.pt", resolved.tvgId)
    }

    @Test
    fun resolvesMissingIdFromDisplayName() {
        val resolved = resolve(xmlChannels("provider-news" to "News One"), name = "  NEWS   ONE ")

        assertEquals("provider-news", resolved.tvgId)
    }

    @Test
    fun resolvesConservativeNameWithHdSuffix() {
        val resolved = resolve(xmlChannels("provider-one" to "Canal One"), name = "Canal-One HD")

        assertEquals("provider-one", resolved.tvgId)
    }

    @Test
    fun doesNotResolveAmbiguousAlias() {
        val providerChannels = extractXmlTvChannelAliases(
            xmlChannels("one" to "Shared HD", "two" to "Shared FHD"),
        )
        val resolved = resolveXmlTvChannelIds(listOf(channel(name = "Shared 4K")), providerChannels).single()

        assertNull(resolved.tvgId)
    }

    @Test
    fun decodesXmlEntitiesInIdsAndDisplayNames() {
        val aliases = extractXmlTvChannelAliases(xmlChannels("news&amp;one" to "News &amp; One"))
        val resolved = resolveXmlTvChannelIds(
            listOf(channel(tvgId = "news&one", name = "News & One")),
            aliases,
        ).single()

        assertEquals("news&one", aliases.single().id)
        assertEquals(setOf("News & One"), aliases.single().displayNames)
        assertEquals("news&one", resolved.tvgId)
    }

    @Test
    fun doesNotUseUnsafePartialOrFuzzyMatch() {
        val resolved = resolve(xmlChannels("provider-one" to "News Network"), name = "News Netwrk")

        assertNull(resolved.tvgId)
    }

    private fun resolve(xml: String, tvgId: String? = null, name: String): LiveTvChannel =
        resolveXmlTvChannelIds(
            channels = listOf(channel(tvgId = tvgId, name = name)),
            providerChannels = extractXmlTvChannelAliases(xml),
        ).single()

    private fun channel(tvgId: String? = null, name: String): LiveTvChannel =
        LiveTvChannel(
            id = "playlist-channel",
            name = name,
            streamUrl = "https://stream.test/live.m3u8",
            tvgId = tvgId,
        )

    private fun xmlChannels(vararg channels: Pair<String, String>): String = buildString {
        append("<tv>")
        channels.forEach { (id, name) ->
            append("<channel id=\"").append(id).append("\">")
            append("<display-name>").append(name).append("</display-name>")
            append("</channel>")
        }
        append("<programme channel=\"unused\"></programme></tv>")
    }
}
