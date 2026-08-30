package com.nuvio.app.features.home

import com.nuvio.app.features.addons.AddonCatalog
import com.nuvio.app.features.addons.AddonExtraProperty
import com.nuvio.app.features.addons.AddonManifest
import com.nuvio.app.features.addons.AddonResource
import com.nuvio.app.features.addons.ManagedAddon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class HomeCatalogDefinitionTest {
    private val definition = HomeCatalogDefinition(
        key = "addon:movie:popular",
        defaultTitle = "Popular - Movie",
        catalogName = "Popular",
        addonName = "Addon",
        manifestUrl = "https://example.com/manifest.json",
        type = "movie",
        catalogId = "popular",
        supportsPagination = true,
        descriptorSignature = "signature",
    )

    @Test
    fun `shows the type suffix by default`() {
        assertEquals("Popular - Movie", definition.titleFor(showCatalogType = true))
    }

    @Test
    fun `omits the type suffix when disabled`() {
        assertEquals("Popular", definition.titleFor(showCatalogType = false))
    }

    @Test
    fun `descriptor signature is deterministic and bounded`() {
        val fixture = descriptorFixture()

        val first = fixture.signature()
        val second = fixture.signature()

        assertEquals(first, second)
        assertEquals("ce3d0a24e3c3f98b", first)
        assertTrue(first.matches(Regex("[0-9a-f]{1,16}")))
    }

    @Test
    fun `descriptor signature changes with significant fields and options`() {
        val fixture = descriptorFixture()

        assertNotEquals(
            fixture.signature(),
            fixture.copy(manifest = fixture.manifest.copy(description = "Changed")).signature(),
        )
        assertNotEquals(
            fixture.signature(),
            fixture.copy(
                catalog = fixture.catalog.copy(
                    extra = fixture.catalog.extra.map { it.copy(options = it.options + "year") },
                ),
            ).signature(),
        )
    }

    @Test
    fun `descriptor signature preserves option order`() {
        val fixture = descriptorFixture()
        val reversed = fixture.copy(
            catalog = fixture.catalog.copy(
                extra = fixture.catalog.extra.map { it.copy(options = it.options.reversed()) },
            ),
        )

        assertNotEquals(fixture.signature(), reversed.signature())
    }

    @Test
    fun `descriptor signature resists delimiter collisions`() {
        val fixture = descriptorFixture()
        val left = fixture.copy(manifest = fixture.manifest.copy(types = listOf("movie,series", "tv")))
        val right = fixture.copy(manifest = fixture.manifest.copy(types = listOf("movie", "series,tv")))

        assertNotEquals(left.signature(), right.signature())
    }

    @Test
    fun `descriptor signature ignores transient addon state`() {
        val fixture = descriptorFixture()
        val transientUpdate = fixture.copy(
            addon = fixture.addon.copy(isRefreshing = true, errorMessage = "Temporary failure"),
        )

        assertEquals(fixture.signature(), transientUpdate.signature())
    }

    @Test
    fun `descriptor signature remains bounded for oversized manifests`() {
        val fixture = descriptorFixture()
        val oversized = fixture.copy(
            manifest = fixture.manifest.copy(
                resources = List(20_000) { index ->
                    AddonResource(
                        name = "resource-$index",
                        types = listOf("movie", "series"),
                        idPrefixes = listOf("tt", "tmdb"),
                    )
                },
            ),
        )

        assertTrue(oversized.signature().length <= 16)
    }
}

private data class DescriptorFixture(
    val addon: ManagedAddon,
    val manifest: AddonManifest,
    val catalog: AddonCatalog,
) {
    fun signature(): String = buildHomeCatalogDescriptorSignature(addon, manifest, catalog)
}

private fun descriptorFixture(): DescriptorFixture {
    val catalog = AddonCatalog(
        type = "movie",
        id = "popular",
        name = "Popular",
        extra = listOf(
            AddonExtraProperty(
                name = "genre",
                options = listOf("action", "drama"),
                optionsLimit = 1,
            ),
        ),
    )
    val manifest = AddonManifest(
        id = "example",
        name = "Example Addon",
        description = "Example catalogs",
        version = "1.0.0",
        logoUrl = "https://example.com/logo.png",
        resources = listOf(
            AddonResource(
                name = "catalog",
                types = listOf("movie"),
                idPrefixes = listOf("tt"),
            ),
        ),
        types = listOf("movie", "series"),
        idPrefixes = listOf("tt", "tmdb"),
        catalogs = listOf(catalog),
        transportUrl = "https://example.com/",
    )
    val addon = ManagedAddon(
        manifestUrl = "https://example.com/manifest.json",
        manifest = manifest,
        userSetName = "My Addon",
    )
    return DescriptorFixture(addon, manifest, catalog)
}
