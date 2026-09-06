package com.nuvio.app.features.plugins

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PluginPersistenceTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun metadataStateDoesNotSerializeScraperSourceCode() {
        val sourceCode = "module.exports = " + "x".repeat(4 * 1024 * 1024)
        val stored = PluginsUiState(
            scrapers = listOf(pluginScraper(sourceCode)),
        ).toStoredPluginsState()

        val encoded = json.encodeToString(stored)

        assertNull(stored.scrapers.single().code)
        assertTrue(encoded.length < 2_048)
        assertFalse(sourceCode in encoded)
    }

    @Test
    fun largeCollectionMetadataSizeDoesNotScaleWithSharedSourceCode() {
        val sourceCode = "module.exports = " + "x".repeat(1_100_000)
        val stored = PluginsUiState(
            scrapers = List(61) { index ->
                pluginScraper(sourceCode).copy(
                    id = "scraper-$index",
                    name = "Scraper $index",
                )
            },
        ).toStoredPluginsState()

        val encoded = json.encodeToString(stored)

        assertTrue(stored.scrapers.all { it.code == null })
        assertTrue(encoded.length < 32_000)
        assertFalse(sourceCode in encoded)
    }

    @Test
    fun metadataStatePreservesExcludedQualities() {
        val stored = PluginsUiState(
            excludedQualities = setOf("cam", "480p"),
        ).toStoredPluginsState()

        assertEquals(setOf("cam", "480p"), stored.excludedQualities)
    }

    @Test
    fun cachedScraperCodeRestoresOfflineWithoutMigration() {
        val sourceCode = "offline scraper source"
        val stored = pluginScraper(sourceCode).toStoredPluginScraper()

        val restored = stored.restorePluginScraper { scraperId ->
            if (scraperId == stored.id) sourceCode else null
        }

        assertEquals(sourceCode, restored?.scraper?.code)
        assertFalse(restored?.requiresMigration ?: true)
    }

    @Test
    fun legacyEmbeddedScraperCodeIsPreservedForMigration() {
        val sourceCode = "legacy scraper source"
        val stored = pluginScraper(sourceCode)
            .toStoredPluginScraper()
            .copy(code = sourceCode)

        val restored = stored.restorePluginScraper { null }

        assertEquals(sourceCode, restored?.scraper?.code)
        assertTrue(restored?.requiresMigration == true)
    }

    @Test
    fun cachedCodeTakesPriorityWhileLegacyStateIsStillMarkedForMigration() {
        val stored = pluginScraper("legacy source")
            .toStoredPluginScraper()
            .copy(code = "legacy source")

        val restored = stored.restorePluginScraper { "cached source" }

        assertEquals("cached source", restored?.scraper?.code)
        assertTrue(restored?.requiresMigration == true)
    }

    private fun pluginScraper(code: String): PluginScraper = PluginScraper(
        id = "https://plugins.example/manifest.json:scraper",
        repositoryUrl = "https://plugins.example/manifest.json",
        name = "Scraper",
        description = "",
        version = "1.0.0",
        filename = "scraper.js",
        supportedTypes = listOf("movie", "tv"),
        enabled = true,
        manifestEnabled = true,
        code = code,
    )
}
