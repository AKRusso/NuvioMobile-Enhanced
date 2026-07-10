package com.nuvio.app.features.cloudstream

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CloudStreamRepositoryParserTest {
    @Test
    fun parsesRepositoryAndResolvesMultiplePluginLists() {
        val manifest = CloudStreamRepositoryParser.parseRepository(
            rawUrl = "https://example.com/repos/repo.json",
            payload = """
                {
                  "name": "Example",
                  "manifestVersion": 1,
                  "pluginLists": ["plugins.json", "../shared/plugins.json"],
                  "futureField": true
                }
            """.trimIndent(),
        )

        assertEquals("Example", manifest.name)
        assertEquals(
            listOf(
                "https://example.com/repos/plugins.json",
                "https://example.com/shared/plugins.json",
            ),
            manifest.pluginListUrls,
        )
    }

    @Test
    fun normalizesGithubRepositoryAndCloudStreamScheme() {
        val expected = "https://raw.githubusercontent.com/Kraptor123/cs-kraptor/master/repo.json"
        assertEquals(expected, normalizeCloudStreamRepositoryUrl("https://github.com/Kraptor123/cs-kraptor"))
        assertEquals(expected, normalizeCloudStreamRepositoryUrl("cloudstreamrepo://github.com/Kraptor123/cs-kraptor"))
    }

    @Test
    fun resolvesAbsoluteRootAndRelativeUrls() {
        val base = "https://example.com/a/b/repo.json"
        assertEquals("https://cdn.example/plugin.cs3", resolveCloudStreamUrl(base, "https://cdn.example/plugin.cs3"))
        assertEquals("https://example.com/plugins.json", resolveCloudStreamUrl(base, "/plugins.json"))
        assertEquals("https://example.com/a/plugins.json", resolveCloudStreamUrl(base, "../plugins.json"))
    }

    @Test
    fun parsesPluginsAndIgnoresUnknownFields() {
        val plugins = CloudStreamRepositoryParser.parsePluginList(
            repositoryManifestUrl = "https://example.com/repo.json",
            pluginListUrl = "https://example.com/lists/plugins.json",
            payload = """
                [{
                  "url": "../builds/KickTR.cs3",
                  "status": 1,
                  "version": 16,
                  "name": "KickTR",
                  "internalName": "KickTR",
                  "authors": ["A", "A", "B"],
                  "language": "tr",
                  "tvTypes": ["Live", "FutureType"],
                  "apiVersion": 1,
                  "fileHash": "sha256-60c0dc079c6929347fbf7d6e273d14507712775f4631d15e430065d50d374f6a",
                  "unknown": {"nested": true}
                }]
            """.trimIndent(),
        )

        val plugin = plugins.single()
        assertEquals("https://example.com/builds/KickTR.cs3", plugin.packageUrl)
        assertEquals(listOf("A", "B"), plugin.authors)
        assertEquals(listOf(CloudStreamTvType.Live, CloudStreamTvType.Other), plugin.tvTypes)
        assertEquals(CloudStreamPluginStatus.Ok, plugin.status)
        assertTrue(plugin.fileHash != null)
    }

    @Test
    fun skipsMalformedPluginWithoutBreakingValidEntries() {
        val plugins = CloudStreamRepositoryParser.parsePluginList(
            repositoryManifestUrl = "https://example.com/repo.json",
            pluginListUrl = "https://example.com/plugins.json",
            payload = """
                [
                  {"name":"Missing identity"},
                  {"url":"Good.cs3","internalName":"Good","name":"Good","version":1,"status":2}
                ]
            """.trimIndent(),
        )

        assertEquals(1, plugins.size)
        assertEquals(CloudStreamPluginStatus.Slow, plugins.single().status)
    }

    @Test
    fun mergesDuplicatePluginsUsingHighestVersion() {
        fun plugin(version: Int) = CloudStreamRepositoryParser.parsePluginList(
            repositoryManifestUrl = "https://example.com/repo.json",
            pluginListUrl = "https://example.com/plugins.json",
            payload = """[{"url":"A.cs3","internalName":"A","name":"A","version":$version,"status":1}]""",
        ).single()

        val merged = CloudStreamRepositoryParser.mergePluginLists(listOf(listOf(plugin(1)), listOf(plugin(3))))
        assertEquals(3, merged.single().version)
    }

    @Test
    fun pluginIdentityIncludesRepository() {
        val first = CloudStreamPluginId("https://one.example/repo.json", "Same")
        val second = CloudStreamPluginId("https://two.example/repo.json", "Same")
        assertNotEquals(first.value, second.value)
        assertNotEquals(first.storageKey, second.storageKey)
    }

    @Test
    fun handlesStatusAndVersionTransitions() {
        assertFalse(CloudStreamPluginStatus.fromWireValue(0).canInstall)
        assertTrue(CloudStreamPluginStatus.fromWireValue(1).canInstall)
        assertEquals(CloudStreamPluginStatus.Unknown, CloudStreamPluginStatus.fromWireValue(99))
    }

    @Test
    fun verifiesSha256AndRejectsMalformedHash() {
        val bytes = "abc".encodeToByteArray()
        val hash = CloudStreamFileHash.parse("sha256-ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
        assertTrue(hash?.matches(bytes) == true)
        assertFalse(hash?.matches("abd".encodeToByteArray()) == true)
        assertNull(CloudStreamFileHash.parse("md5-deadbeef"))
    }

    @Test
    fun sortsEpisodesWithMissingNumbersLast() {
        val sorted = sortCloudStreamEpisodes(
            listOf(
                CloudStreamEpisode("unknown", "Special"),
                CloudStreamEpisode("s2e1", "Episode 1", season = 2, episode = 1),
                CloudStreamEpisode("s1e2", "Episode 2", season = 1, episode = 2),
                CloudStreamEpisode("s1e1", "Episode 1", season = 1, episode = 1),
            ),
        )
        assertEquals(listOf("s1e1", "s1e2", "s2e1", "unknown"), sorted.map { it.data })
    }

    @Test
    fun cloudStreamRouteRoundTripsOpaqueProviderData() {
        val provider = "https://example.com/repo.json#Provider"
        val data = "https://service.example/watch/a:b?token=public"
        val route = cloudStreamRouteId(provider, data)
        assertEquals(CloudStreamRouteData(provider, data), parseCloudStreamRouteId(route))
        assertNull(parseCloudStreamRouteId("cloudstream:broken"))
    }
}
