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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

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
    animationStyle: ThemeAnimationStyle = ThemeAnimationStyle.FLOW,
): AnimatedThemeVisuals? {
    if (!theme.isEnhanced || animationStyle == ThemeAnimationStyle.STILL) return null

    val colors = remember(theme, customFirst, customSecond, animationStyle) {
        stylePalette(
            ThemeColors.animatedColors(theme, customFirst, customSecond)
                .map { it.copy(alpha = 1f) },
            animationStyle,
        )
    }
    if (colors.isEmpty()) return null

    val transition = rememberInfiniteTransition(label = "theme_animation")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = animationStyle.durationMillis,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "theme_animation_phase",
    )

    val actionColors = when (animationStyle) {
        ThemeAnimationStyle.SHIMMER -> shimmerColors(colors, phase, samples = 14, alpha = 1f)
        else -> phasedColors(colors, phase, samples = 14, alpha = 1f)
    }
    val chipColors = phasedColors(colors, phase + 0.12f, samples = 10, alpha = animationStyle.chipAlpha)
    val lineColors = phasedColors(colors, phase + 0.26f, samples = 10, alpha = animationStyle.lineAlpha)
    val (start, end) = gradientOffsets(animationStyle, phase)
    val accent = sampleLoop(colors, phase)
    val accentStrong = lerp(accent, sampleLoop(colors, phase + 0.34f), 0.62f)

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
            colors = actionColors.map { it.copy(alpha = animationStyle.softAlpha) },
            start = start,
            end = end,
        ),
    )
}

private val ThemeAnimationStyle.durationMillis: Int
    get() = when (this) {
        ThemeAnimationStyle.FLOW -> 6_400
        ThemeAnimationStyle.SHIMMER -> 3_600
        ThemeAnimationStyle.WAVE -> 5_200
        ThemeAnimationStyle.VIVID_WAVE -> 4_200
        ThemeAnimationStyle.STILL -> 1_000
    }

private val ThemeAnimationStyle.chipAlpha: Float
    get() = when (this) {
        ThemeAnimationStyle.VIVID_WAVE -> 0.30f
        ThemeAnimationStyle.SHIMMER -> 0.20f
        else -> 0.24f
    }

private val ThemeAnimationStyle.lineAlpha: Float
    get() = when (this) {
        ThemeAnimationStyle.VIVID_WAVE -> 0.84f
        ThemeAnimationStyle.SHIMMER -> 0.76f
        else -> 0.78f
    }

private val ThemeAnimationStyle.softAlpha: Float
    get() = when (this) {
        ThemeAnimationStyle.VIVID_WAVE -> 0.13f
        ThemeAnimationStyle.WAVE -> 0.12f
        else -> 0.10f
    }

private fun stylePalette(
    colors: List<Color>,
    style: ThemeAnimationStyle,
): List<Color> {
    if (colors.isEmpty()) return colors
    return when (style) {
        ThemeAnimationStyle.VIVID_WAVE -> colors.map { boostContrast(it, 1.36f) }
        ThemeAnimationStyle.WAVE -> colors.flatMap { color ->
            listOf(
                boostContrast(color, 1.16f),
                lerp(color, Color(0xFF00D9FF), 0.18f),
            )
        }
        ThemeAnimationStyle.SHIMMER -> colors.map { boostContrast(it, 1.12f) }
        ThemeAnimationStyle.FLOW,
        ThemeAnimationStyle.STILL,
        -> colors.map { boostContrast(it, 1.08f) }
    }
}

private fun gradientOffsets(
    style: ThemeAnimationStyle,
    phase: Float,
): Pair<Offset, Offset> {
    val wave = normalizedPhase(phase) * TwoPi
    return when (style) {
        ThemeAnimationStyle.SHIMMER -> {
            val drift = sin(wave) * 160f
            Offset(-760f + drift, -280f) to Offset(920f + drift, 520f)
        }
        ThemeAnimationStyle.WAVE -> {
            Offset(
                x = -640f + sin(wave) * 520f,
                y = -460f + cos(wave * 2f) * 190f,
            ) to Offset(
                x = 980f + cos(wave) * 460f,
                y = 620f + sin(wave * 2f) * 240f,
            )
        }
        ThemeAnimationStyle.VIVID_WAVE -> {
            Offset(
                x = -820f + sin(wave * 2f) * 620f,
                y = -540f + cos(wave) * 340f,
            ) to Offset(
                x = 1_080f + cos(wave * 2f) * 560f,
                y = 720f + sin(wave) * 360f,
            )
        }
        ThemeAnimationStyle.FLOW,
        ThemeAnimationStyle.STILL,
        -> {
            Offset(
                x = -620f + cos(wave) * 260f,
                y = -360f + sin(wave) * 220f,
            ) to Offset(
                x = 940f - cos(wave) * 260f,
                y = 520f - sin(wave) * 220f,
            )
        }
    }
}

private fun phasedColors(
    colors: List<Color>,
    phase: Float,
    samples: Int,
    alpha: Float,
): List<Color> =
    List(samples.coerceAtLeast(4)) { index ->
        val position = normalizedPhase(phase + index.toFloat() / samples.toFloat())
        sampleLoop(colors, position).copy(alpha = alpha)
    }

private fun shimmerColors(
    colors: List<Color>,
    phase: Float,
    samples: Int,
    alpha: Float,
): List<Color> =
    List(samples.coerceAtLeast(6)) { index ->
        val track = index.toFloat() / (samples - 1).coerceAtLeast(1).toFloat()
        val base = sampleLoop(colors, normalizedPhase(track + phase))
        val highlight = (1f - circularDistance(track, normalizedPhase(phase)) / 0.18f)
            .coerceIn(0f, 1f)
        lerp(base, Color.White, highlight * 0.24f).copy(alpha = alpha)
    }

private fun sampleLoop(colors: List<Color>, phase: Float): Color {
    if (colors.isEmpty()) return Color.Transparent
    if (colors.size == 1) return colors.first()

    val position = normalizedPhase(phase) * colors.size
    val index = floor(position).toInt().coerceIn(0, colors.lastIndex)
    val nextIndex = (index + 1) % colors.size
    val fraction = smoothStep(position - index)
    return lerp(colors[index], colors[nextIndex], fraction)
}

private fun boostContrast(color: Color, amount: Float): Color =
    Color(
        red = ((color.red - 0.5f) * amount + 0.5f).coerceIn(0f, 1f),
        green = ((color.green - 0.5f) * amount + 0.5f).coerceIn(0f, 1f),
        blue = ((color.blue - 0.5f) * amount + 0.5f).coerceIn(0f, 1f),
        alpha = color.alpha,
    )

private fun normalizedPhase(value: Float): Float {
    val wrapped = value - floor(value)
    return if (wrapped < 0f) wrapped + 1f else wrapped
}

private fun smoothStep(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun circularDistance(first: Float, second: Float): Float {
    val distance = abs(normalizedPhase(first) - normalizedPhase(second))
    return kotlin.math.min(distance, 1f - distance)
}

@Composable
internal fun rememberAnimatedAccentBrush(
    previewTheme: AppTheme? = null,
    customFirst: ThemeAccentColor = ThemeAccentColor.PINK,
    customSecond: ThemeAccentColor = ThemeAccentColor.CYAN,
    animationStyle: ThemeAnimationStyle = ThemeAnimationStyle.FLOW,
): Brush? = if (previewTheme != null) {
    rememberAnimatedThemeVisuals(previewTheme, customFirst, customSecond, animationStyle)?.brush
} else {
    currentAnimatedThemeVisuals?.brush
}

@Composable
internal fun rememberAnimatedChipBrush(): Brush? = currentAnimatedThemeVisuals?.chipBrush

@Composable
internal fun rememberAnimatedLineBrush(): Brush? = currentAnimatedThemeVisuals?.lineBrush

@Composable
internal fun rememberAnimatedSoftBrush(): Brush? = currentAnimatedThemeVisuals?.softBrush

private const val TwoPi = 6.2831855f
