package com.nuvio.app.features.settings

import androidx.compose.ui.graphics.Color
import com.nuvio.app.core.ui.AppTheme
import com.nuvio.app.core.ui.ThemeColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MemberBrandWordmarkTest {
    @Test
    fun supporterThemesUseTheirOwnAccentPalettes() {
        val gold = memberBrandWordmarkColors(AppTheme.GOLD)
        val jade = memberBrandWordmarkColors(AppTheme.JADE)

        assertEquals(ThemeColors.Gold.accentGradient.first(), gold.first())
        assertEquals(ThemeColors.Jade.accentGradient.first(), jade.first())
        assertEquals(gold.first(), gold.last())
        assertEquals(jade.first(), jade.last())
        assertNotEquals(gold, jade)
    }

    @Test
    fun enhancedAndCustomThemesUseTheirProvidedAnimatedColors() {
        val selectedColors = listOf(Color.Red, Color.Green, Color.Blue)

        assertEquals(
            selectedColors + selectedColors.first(),
            memberBrandWordmarkColors(AppTheme.CUSTOM, selectedColors),
        )
    }

    @Test
    fun singleAccentThemesStillProduceAnAnimatedGradient() {
        val colors = memberBrandWordmarkColors(AppTheme.CRIMSON)

        assertEquals(colors.first(), colors.last())
        assertEquals(4, colors.size)
        assertNotEquals(colors[0], colors[1])
    }
}
