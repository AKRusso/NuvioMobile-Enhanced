package com.nuvio.app.features.settings

import com.nuvio.app.core.ui.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

class AppIconOptionTest {
    @Test
    fun primaryIconUsesPlatformDefault() {
        assertEquals(null, AppIconOption.ORIGINAL.platformName)
        assertEquals(AppIconOption.ORIGINAL, AppIconOption.fromPlatformName(null))
    }

    @Test
    fun alternateIconNamesRoundTrip() {
        AppIconOption.entries.drop(1).forEach { icon ->
            assertEquals(icon, AppIconOption.fromPlatformName(icon.platformName))
        }
    }

    @Test
    fun shortlistedCatalogueContainsSixIcons() {
        assertEquals(6, AppIconOption.entries.size)
    }

    @Test
    fun unknownIconFallsBackToOriginal() {
        assertEquals(AppIconOption.ORIGINAL, AppIconOption.fromPlatformName("UnknownIcon"))
    }

    @Test
    fun supporterThemesUseMatchingWordmarks() {
        assertEquals(AppIconOption.COPPER.wordmarkResource, AppTheme.GOLD.wordmarkResource(AppIconOption.ORIGINAL))
        assertEquals(AppIconOption.EMERALD.wordmarkResource, AppTheme.JADE.wordmarkResource(AppIconOption.ORIGINAL))
        assertEquals(AppIconOption.ROSE_GOLD.wordmarkResource, AppTheme.ROSE_GOLD.wordmarkResource(AppIconOption.ORIGINAL))
        assertEquals(AppIconOption.ARCTIC_BLUE.wordmarkResource, AppTheme.ARCTIC_BLUE.wordmarkResource(AppIconOption.ORIGINAL))
        assertEquals(AppIconOption.GRAPHITE.wordmarkResource, AppTheme.GRAPHITE.wordmarkResource(AppIconOption.ORIGINAL))
    }
}
