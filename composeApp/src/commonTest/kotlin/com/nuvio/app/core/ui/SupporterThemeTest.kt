package com.nuvio.app.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SupporterThemeTest {
    private val themes = listOf(
        AppTheme.GOLD to "#FFD45C",
        AppTheme.JADE to "#7BF08D",
        AppTheme.ROSE_GOLD to "#FFB37A",
        AppTheme.ARCTIC_BLUE to "#4DE3FF",
        AppTheme.GRAPHITE to "#F3F5F7",
    )

    @Test
    fun supporterThemeNamesRoundTripForStorage() {
        themes.forEach { (theme, _) ->
            assertEquals(theme, AppTheme.valueOf(theme.name))
        }
    }

    @Test
    fun supporterThemesExposeImportedGradientsAndNativeAccents() {
        themes.forEach { (theme, expectedAccent) ->
            val palette = ThemeColors.getColorPalette(theme)
            assertTrue(palette.accentGradient.size >= 2)
            assertEquals(expectedAccent, palette.nativeAccentHex)
            assertFalse(theme.isEnhanced)
        }
    }
}
