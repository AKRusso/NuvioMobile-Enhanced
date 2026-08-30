package com.nuvio.app.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer

internal data class ActionAccentStyle(
    val brush: Brush,
    val contentColor: Color,
)

@Composable
internal fun rememberActionAccentStyle(enabled: Boolean = true): ActionAccentStyle {
    if (!enabled) {
        return ActionAccentStyle(
            brush = SolidColor(MaterialTheme.colorScheme.surfaceVariant),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    val animatedBrush = rememberAnimatedAccentBrush()
    val appTheme = MaterialTheme.appTheme
    val palette = remember(appTheme) { ThemeColors.getColorPalette(appTheme) }
    val tokens = MaterialTheme.nuvio
    return ActionAccentStyle(
        brush = animatedBrush ?: if (palette.accentGradient.size >= 2) {
            Brush.linearGradient(palette.accentGradient)
        } else {
            SolidColor(tokens.colors.accent)
        },
        contentColor = tokens.colors.onAccent,
    )
}

fun ThemeColorPalette.accentBrush(): Brush =
    if (accentGradient.size >= 2) Brush.linearGradient(accentGradient)
    else SolidColor(accentGradient.firstOrNull() ?: secondary)

fun Modifier.gradientMask(brush: Brush): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithCache {
            onDrawWithContent {
                drawContent()
                drawRect(brush = brush, blendMode = BlendMode.SrcIn)
            }
        }
