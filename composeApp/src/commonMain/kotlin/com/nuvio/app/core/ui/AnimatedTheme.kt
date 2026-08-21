package com.nuvio.app.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.floor
import kotlin.math.pow

internal data class AnimatedThemeVisuals(
    val accent: Color,
    val accentStrong: Color,
    val brush: Brush,
    val chipBrush: Brush,
    val lineBrush: Brush,
    val selectionBrush: Brush,
    val softBrush: Brush,
)

internal val LocalAnimatedThemeVisuals = staticCompositionLocalOf<AnimatedThemeVisuals?> { null }

internal val currentAnimatedThemeVisuals: AnimatedThemeVisuals?
    @Composable
    @ReadOnlyComposable
    get() = LocalAnimatedThemeVisuals.current

@Composable
internal fun rememberAnimatedThemeVisuals(
    theme: AppTheme,
    customFirst: Color = ThemeAccentColor.PINK.color,
    customSecond: Color = ThemeAccentColor.CYAN.color,
): AnimatedThemeVisuals? {
    if (!theme.isEnhanced) return null

    val colors = remember(theme, customFirst, customSecond) {
        stylePalette(
            ThemeColors.animatedColors(theme, customFirst, customSecond)
                .map { it.copy(alpha = 1f) },
            theme,
        )
    }
    if (colors.isEmpty()) return null

    val phase = 0f
    val actionColors = colors
    val chipColors = actionColors.map { it.copy(alpha = StaticChipAlpha) }
    val lineColors = actionColors
    val selectionColors = highlightColors(colors, phase, samples = 24, strength = 0.72f)
    val accent = sampleLoop(colors, phase)
    val accentStrong = smoothColorLerp(accent, sampleLoop(colors, phase + 0.34f), 0.62f)

    return AnimatedThemeVisuals(
        accent = accent,
        accentStrong = accentStrong,
        brush = themeBrush(actionColors),
        chipBrush = themeBrush(chipColors),
        lineBrush = themeBrush(lineColors),
        selectionBrush = Brush.horizontalGradient(selectionColors),
        softBrush = themeBrush(
            actionColors.map { it.copy(alpha = StaticSoftAlpha) },
        ),
    )
}

private const val StaticChipAlpha = 0.22f
private const val StaticSoftAlpha = 0.08f

private fun stylePalette(
    colors: List<Color>,
    theme: AppTheme,
): List<Color> {
    if (colors.isEmpty()) return colors
    val enhanced = colors.map { enhanceColor(it, saturation = 1.10f, contrast = 1.04f) }
    return if (theme == AppTheme.CUSTOM) {
        enhanced.map(Color::toAccessibleAccent)
    } else {
        enhanced
    }
}

private fun themeBrush(colors: List<Color>): Brush =
    Brush.horizontalGradient(smoothGradientPalette(colors, steps = 8))

private fun smoothGradientPalette(
    colors: List<Color>,
    steps: Int,
    closed: Boolean = false,
): List<Color> {
    val palette = colors.ifEmpty { return listOf(Color.Transparent) }
    if (palette.size == 1) return palette
    val segmentCount = if (closed) palette.size else palette.lastIndex
    return buildList {
        repeat(segmentCount) { segment ->
            val start = palette[segment]
            val end = palette[(segment + 1) % palette.size]
            repeat(steps.coerceAtLeast(2)) { step ->
                val fraction = step.toFloat() / steps.coerceAtLeast(2).toFloat()
                add(smoothColorLerp(start, end, smoothStep(fraction)))
            }
        }
        add(if (closed) palette.first() else palette.last())
    }
}

private fun highlightColors(
    colors: List<Color>,
    phase: Float,
    samples: Int,
    strength: Float,
): List<Color> =
    List(samples.coerceAtLeast(8)) { index ->
        val track = index.toFloat() / (samples - 1).coerceAtLeast(1).toFloat()
        val base = sampleLoop(colors, normalizedPhase(track + phase))
        val highlight = (1f - circularDistance(track, normalizedPhase(phase)) / 0.10f)
            .coerceIn(0f, 1f)
        smoothColorLerp(base, Color.White, highlight * strength)
    }

private fun sampleLoop(colors: List<Color>, phase: Float): Color {
    if (colors.isEmpty()) return Color.Transparent
    if (colors.size == 1) return colors.first()

    val position = normalizedPhase(phase) * colors.size
    val index = floor(position).toInt().coerceIn(0, colors.lastIndex)
    val nextIndex = (index + 1) % colors.size
    val fraction = smoothStep(position - index)
    return smoothColorLerp(colors[index], colors[nextIndex], fraction)
}

private fun smoothStep(value: Float): Float {
    val clamped = value.coerceIn(0f, 1f)
    return clamped * clamped * (3f - 2f * clamped)
}

private fun smoothColorLerp(start: Color, end: Color, fraction: Float): Color {
    val amount = fraction.coerceIn(0f, 1f)
    fun channel(first: Float, second: Float): Float {
        val firstLinear = first.coerceIn(0f, 1f).pow(2.2f)
        val secondLinear = second.coerceIn(0f, 1f).pow(2.2f)
        return (firstLinear + (secondLinear - firstLinear) * amount)
            .coerceIn(0f, 1f)
            .pow(1f / 2.2f)
    }
    return Color(
        red = channel(start.red, end.red),
        green = channel(start.green, end.green),
        blue = channel(start.blue, end.blue),
        alpha = start.alpha + (end.alpha - start.alpha) * amount,
    )
}

private fun enhanceColor(
    color: Color,
    saturation: Float,
    contrast: Float,
): Color {
    val gray = color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f
    fun channel(value: Float): Float {
        val saturated = gray + (value - gray) * saturation
        return ((saturated - 0.5f) * contrast + 0.5f).coerceIn(0f, 1f)
    }
    return Color(
        red = channel(color.red),
        green = channel(color.green),
        blue = channel(color.blue),
        alpha = color.alpha,
    )
}

private fun normalizedPhase(value: Float): Float {
    val wrapped = value - floor(value)
    return if (wrapped < 0f) wrapped + 1f else wrapped
}

private fun circularDistance(first: Float, second: Float): Float {
    val distance = kotlin.math.abs(normalizedPhase(first) - normalizedPhase(second))
    return kotlin.math.min(distance, 1f - distance)
}

@Composable
internal fun rememberAnimatedAccentBrush(
    previewTheme: AppTheme? = null,
    customFirst: Color = ThemeAccentColor.PINK.color,
    customSecond: Color = ThemeAccentColor.CYAN.color,
): Brush? = if (previewTheme != null) {
    rememberAnimatedThemeVisuals(previewTheme, customFirst, customSecond)?.brush
} else {
    currentAnimatedThemeVisuals?.brush
}

@Composable
internal fun rememberAnimatedChipBrush(): Brush? = currentAnimatedThemeVisuals?.chipBrush

@Composable
internal fun rememberAnimatedLineBrush(): Brush? = currentAnimatedThemeVisuals?.lineBrush

@Composable
internal fun rememberAnimatedSelectionBrush(): Brush? = currentAnimatedThemeVisuals?.selectionBrush

@Composable
internal fun rememberAnimatedSoftBrush(): Brush? = currentAnimatedThemeVisuals?.softBrush

/**
 * An always-animated gradient brush (plus a glow color) used for the "Automatic"
 * download button. Unlike [rememberAnimatedAccentBrush], whose gradient is static, this
 * cycles through the theme palette over time for a smooth, subtle RGB flow. Returns null
 * for non-enhanced themes, which should fall back to the standard accent styling.
 */
internal data class AutomaticActionBrush(
    val brush: Brush,
    val glowColor: Color,
)

@Composable
internal fun rememberAutomaticActionBrush(): AutomaticActionBrush? {
    val theme = LocalAppTheme.current
    if (!theme.isEnhanced) return null
    val palette = remember(theme) {
        stylePalette(
            ThemeColors.animatedColors(theme).map { it.copy(alpha = 1f) },
            theme,
        )
    }
    if (palette.size <= 1) return null

    val transition = rememberInfiniteTransition(label = "automaticActionBrush")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "automaticActionBrushPhase",
    )

    val glowColor = sampleLoop(palette, phase)
    val brush = Brush.linearGradient(
        listOf(
            sampleLoop(palette, phase),
            sampleLoop(palette, normalizedPhase(phase + 0.33f)),
            sampleLoop(palette, normalizedPhase(phase + 0.66f)),
        ),
    )
    return AutomaticActionBrush(brush = brush, glowColor = glowColor)
}
