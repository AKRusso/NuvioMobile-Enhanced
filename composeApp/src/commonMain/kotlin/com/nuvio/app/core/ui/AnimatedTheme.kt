package com.nuvio.app.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random

internal data class AnimatedThemeVisuals(
    val accent: Color,
    val accentStrong: Color,
    val brush: Brush,
    val chipBrush: Brush,
    val lineBrush: Brush,
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
    customFirst: ThemeAccentColor = ThemeAccentColor.PINK,
    customSecond: ThemeAccentColor = ThemeAccentColor.CYAN,
): AnimatedThemeVisuals? {
    if (!theme.isEnhanced) return null

    val colors = ThemeColors.animatedColors(theme, customFirst, customSecond)
        .map { it.copy(alpha = 1f) }
    if (colors.isEmpty()) return null

    val first = colors.first()
    val red = androidx.compose.runtime.remember(colors) { Animatable(first.red) }
    val green = androidx.compose.runtime.remember(colors) { Animatable(first.green) }
    val blue = androidx.compose.runtime.remember(colors) { Animatable(first.blue) }
    val startX = androidx.compose.runtime.remember(colors) { Animatable(-420f) }
    val startY = androidx.compose.runtime.remember(colors) { Animatable(-180f) }
    val endX = androidx.compose.runtime.remember(colors) { Animatable(980f) }
    val endY = androidx.compose.runtime.remember(colors) { Animatable(360f) }

    LaunchedEffect(colors) {
        while (true) {
            val target = colors.random()
            val colorDuration = Random.nextInt(11_000, 19_001)
            val motionDuration = Random.nextInt(13_000, 22_001)
            coroutineScope {
                launch { red.animateTo(target.red, tween(colorDuration, easing = FastOutSlowInEasing)) }
                launch { green.animateTo(target.green, tween(colorDuration, easing = FastOutSlowInEasing)) }
                launch { blue.animateTo(target.blue, tween(colorDuration, easing = FastOutSlowInEasing)) }
                launch {
                    startX.animateTo(Random.nextFloat() * 1_800f - 900f, tween(motionDuration, easing = FastOutSlowInEasing))
                }
                launch {
                    startY.animateTo(Random.nextFloat() * 1_800f - 900f, tween(motionDuration, easing = FastOutSlowInEasing))
                }
                launch {
                    endX.animateTo(Random.nextFloat() * 1_600f - 200f, tween(motionDuration, easing = FastOutSlowInEasing))
                }
                launch {
                    endY.animateTo(Random.nextFloat() * 1_600f - 200f, tween(motionDuration, easing = FastOutSlowInEasing))
                }
            }
        }
    }

    val accent = Color(red.value, green.value, blue.value)
    val anchors = highContrastAnchors(accent, colors)
    val accentStrong = lerp(accent, anchors[1], 0.72f)
    val start = Offset(startX.value - 540f, startY.value - 420f)
    val end = Offset(endX.value + 540f, endY.value + 420f)
    val actionColors = smoothGradientColors(anchors, stepsPerSegment = 8)
    val chipColors = smoothGradientColors(anchors, stepsPerSegment = 6)
        .mapIndexed { index, color ->
            val alpha = when (index % 3) {
                0 -> 0.24f
                1 -> 0.18f
                else -> 0.14f
            }
            color.copy(alpha = alpha)
        }
    val lineColors = smoothGradientColors(
        listOf(accent, anchors[2], anchors[1], accent),
        stepsPerSegment = 7,
    )
        .map { it.copy(alpha = 0.94f) }

    return AnimatedThemeVisuals(
        accent = accent,
        accentStrong = accentStrong,
        brush = Brush.linearGradient(
            colors = actionColors,
            start = start,
            end = end,
        ),
        chipBrush = Brush.linearGradient(
            colors = chipColors,
            start = start,
            end = end,
        ),
        lineBrush = Brush.linearGradient(
            colors = lineColors,
            start = start,
            end = end,
        ),
        softBrush = Brush.linearGradient(
            colors = actionColors.map { it.copy(alpha = 0.12f) },
            start = start,
            end = end,
        ),
    )
}

private fun highContrastAnchors(accent: Color, colors: List<Color>): List<Color> {
    val first = colors.maxByOrNull { colorDistance(it, accent) } ?: colors.first()
    val second = colors
        .filterNot { it == first }
        .maxByOrNull { colorDistance(it, first) + colorDistance(it, accent) * 0.6f }
        ?: colors.last()
    val third = colors
        .filterNot { it == first || it == second }
        .maxByOrNull { colorDistance(it, first) + colorDistance(it, second) }
        ?: colors.first()

    return listOf(
        first,
        accent,
        second,
        third,
        first,
    )
}

private fun smoothGradientColors(
    anchors: List<Color>,
    stepsPerSegment: Int,
): List<Color> {
    if (anchors.size < 2) return anchors

    val steps = stepsPerSegment.coerceAtLeast(2)
    val result = mutableListOf<Color>()
    anchors.zipWithNext().forEach { (from, to) ->
        repeat(steps) { index ->
            val fraction = smoothStep(index.toFloat() / steps.toFloat())
            result += lerp(from, to, fraction)
        }
    }
    result += anchors.last()
    return result
}

private fun smoothStep(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun colorDistance(first: Color, second: Color): Float {
    val red = first.red - second.red
    val green = first.green - second.green
    val blue = first.blue - second.blue
    return red * red + green * green + blue * blue
}

@Composable
internal fun rememberAnimatedAccentBrush(
    previewTheme: AppTheme? = null,
    customFirst: ThemeAccentColor = ThemeAccentColor.PINK,
    customSecond: ThemeAccentColor = ThemeAccentColor.CYAN,
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
internal fun rememberAnimatedSoftBrush(): Brush? = currentAnimatedThemeVisuals?.softBrush
