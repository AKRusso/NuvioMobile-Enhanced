package com.nuvio.app.features.cloudstream

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CloudStreamNuvioAdapterTest {
    @Test
    fun convertsSearchAndDetailsWithoutLosingOpaqueProviderData() {
        val providerId = "https://example.com/repo.json#SeriesProvider"
        val search = CloudStreamSearchItem(
            providerId = providerId,
            data = "provider:data:42",
            name = "Example Series",
            type = CloudStreamTvType.TvSeries,
            posterUrl = "https://cdn.example/poster.jpg",
            year = 2026,
        )
        val preview = search.toMetaPreview()
        assertEquals(CloudStreamRouteData(providerId, "provider:data:42"), parseCloudStreamRouteId(preview.id))
        assertEquals("series", preview.type)

        val details = CloudStreamLoadItem(
            providerId = providerId,
            data = search.data,
            name = search.name,
            type = search.type,
            ratingPercent = 87,
            tags = listOf("Drama"),
            episodes = listOf(
                CloudStreamEpisode("episode:2", "Second", season = 1, episode = 2),
                CloudStreamEpisode("episode:1", "First", season = 1, episode = 1),
            ),
        ).toMetaDetails()

        assertEquals("8.7", details.imdbRating)
        assertEquals(listOf(1, 2), details.videos.mapNotNull { it.episode })
        assertEquals(
            CloudStreamRouteData(providerId, "episode:1"),
            parseCloudStreamRouteId(details.videos.first().id),
        )
    }

    @Test
    fun convertsExtractorLinkHeadersAndSubtitlesForPlayer() {
        val stream = CloudStreamPlaybackSource(
            name = "Provider CDN",
            url = "https://video.example/master.m3u8",
            quality = 1080,
            referer = "https://provider.example/watch/42",
            headers = mapOf(
                "User-Agent" to "Nuvio Test",
                "Cookie" to "session=opaque",
            ),
            subtitles = listOf(
                CloudStreamSubtitle(
                    url = "https://sub.example/tr.vtt",
                    language = "tr",
                    name = "Türkçe",
                    headers = mapOf("Authorization" to "opaque"),
                ),
            ),
            isHls = true,
        ).toStreamItem(
            providerId = "https://example.com/repo.json#Provider",
            providerName = "Provider",
        )

        assertEquals("hls", stream.streamType)
        assertEquals("Provider CDN · 1080p", stream.name)
        assertEquals("https://provider.example/watch/42", stream.behaviorHints.proxyHeaders?.request?.get("Referer"))
        assertEquals("session=opaque", stream.behaviorHints.proxyHeaders?.request?.get("Cookie"))
        assertTrue(stream.behaviorHints.notWebReady)
        assertEquals("tr", stream.externalSubtitles.single().language)
        assertEquals("opaque", stream.externalSubtitles.single().headers?.get("Authorization"))
    }

    @Test
    fun installActionsAndCompatibilityAreExplicit() {
        val kick = metadata(
            repository = "https://raw.githubusercontent.com/Kraptor123/cs-kraptor/master/repo.json",
            internalName = "KickTR",
            version = 2,
        )
        val installed = CloudStreamInstalledPlugin(
            metadata = kick,
            installedVersion = 1,
            enabled = true,
            verified = true,
            compatibility = CloudStreamCompatibilityResolver.resolve(kick),
            installedAtEpochMs = 1L,
        )

        assertEquals(CloudStreamInstallAction.Install, kick.installAction(null))
        assertEquals(CloudStreamInstallAction.Update, kick.installAction(installed))
        assertTrue(CloudStreamCompatibilityResolver.resolve(kick).isRunnable)

        val unsupported = metadata("https://example.com/repo.json", "DexOnly", 1)
        assertFalse(CloudStreamCompatibilityResolver.resolve(unsupported).isRunnable)
        assertEquals(
            CloudStreamRuntimeKind.UnsupportedAndroidDex,
            CloudStreamCompatibilityResolver.resolve(unsupported).runtimeKind,
        )
    }

    private fun metadata(repository: String, internalName: String, version: Int) = CloudStreamPluginMetadata(
        id = CloudStreamPluginId(repository, internalName),
        repositoryManifestUrl = repository,
        packageUrl = "https://example.com/$internalName.cs3",
        status = CloudStreamPluginStatus.Ok,
        version = version,
        name = internalName,
        internalName = internalName,
        authors = emptyList(),
        description = null,
        fileSize = null,
        projectUrl = null,
        language = null,
        tvTypes = emptyList(),
        rawTvTypes = emptyList(),
        iconUrl = null,
        apiVersion = 1,
        fileHash = null,
    )
}
