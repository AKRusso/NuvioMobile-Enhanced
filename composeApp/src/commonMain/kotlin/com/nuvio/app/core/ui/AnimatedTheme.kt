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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositeShader
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.lerp
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

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
    animationStyle: ThemeAnimationStyle = ThemeAnimationStyle.FLOW,
): AnimatedThemeVisuals? {
    if (!theme.isEnhanced) return null

    val colors = remember(theme, customFirst, customSecond, animationStyle) {
        stylePalette(
            ThemeColors.animatedColors(theme, customFirst, customSecond)
                .map { it.copy(alpha = 1f) },
            animationStyle,
        )
    }
    if (colors.isEmpty()) return null

    val phase = if (animationStyle == ThemeAnimationStyle.STILL) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "theme_animation")
        val animatedPhase by transition.animateFloat(
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
        animatedPhase
    }

    val actionColors = when (animationStyle) {
        ThemeAnimationStyle.VIVID_WAVE -> phasedColors(colors, phase, samples = 18, alpha = 1f)
        else -> colors
    }
    val chipColors = actionColors.map { it.copy(alpha = animationStyle.chipAlpha) }
    val lineColors = actionColors
    val selectionColors = highlightColors(colors, phase, samples = 24, strength = 0.72f)
    val accent = sampleLoop(colors, phase)
    val accentStrong = lerp(accent, sampleLoop(colors, phase + 0.34f), 0.62f)

    return AnimatedThemeVisuals(
        accent = accent,
        accentStrong = accentStrong,
        brush = themeBrush(animationStyle, actionColors, phase),
        chipBrush = themeBrush(animationStyle, chipColors, phase),
        lineBrush = ImmiscibleLineBrush(lineColors, phase),
        selectionBrush = Brush.horizontalGradient(selectionColors),
        softBrush = themeBrush(
            animationStyle,
            actionColors.map { it.copy(alpha = animationStyle.softAlpha) },
            phase,
        ),
    )
}

private val ThemeAnimationStyle.durationMillis: Int
    get() = when (this) {
        ThemeAnimationStyle.FLOW -> 14_000
        ThemeAnimationStyle.SHIMMER -> 12_000
        ThemeAnimationStyle.WAVE -> 18_000
        ThemeAnimationStyle.VIVID_WAVE -> 15_000
        ThemeAnimationStyle.STILL -> 1_000
    }

private val ThemeAnimationStyle.chipAlpha: Float
    get() = when (this) {
        ThemeAnimationStyle.VIVID_WAVE -> 0.26f
        ThemeAnimationStyle.SHIMMER -> 0.20f
        else -> 0.22f
    }

private val ThemeAnimationStyle.softAlpha: Float
    get() = when (this) {
        ThemeAnimationStyle.VIVID_WAVE -> 0.11f
        ThemeAnimationStyle.WAVE -> 0.09f
        else -> 0.08f
    }

private fun stylePalette(
    colors: List<Color>,
    style: ThemeAnimationStyle,
): List<Color> {
    if (colors.isEmpty()) return colors
    return when (style) {
        ThemeAnimationStyle.VIVID_WAVE -> colors.map { boostContrast(it, 1.18f) }
        ThemeAnimationStyle.WAVE -> colors.map { boostContrast(it, 1.06f) }
        ThemeAnimationStyle.SHIMMER -> colors.map { boostContrast(it, 1.08f) }
        ThemeAnimationStyle.FLOW,
        ThemeAnimationStyle.STILL,
        -> colors
    }
}

private fun themeBrush(
    style: ThemeAnimationStyle,
    colors: List<Color>,
    phase: Float,
): Brush = when (style) {
    ThemeAnimationStyle.FLOW -> FlowMotionBrush(colors, phase)
    ThemeAnimationStyle.STILL -> Brush.horizontalGradient(colors)
    ThemeAnimationStyle.SHIMMER -> ShimmerMotionBrush(colors, phase)
    ThemeAnimationStyle.WAVE -> MeshMotionBrush(colors, phase)
    ThemeAnimationStyle.VIVID_WAVE -> Brush.sweepGradient(colors + colors.first())
}

private class FlowMotionBrush(
    private val colors: List<Color>,
    private val phase: Float,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val palette = loopPalette(colors)
        val width = size.width.coerceAtLeast(1f)
        val height = size.height.coerceAtLeast(1f)
        val period = max(width * 1.35f, height * 4.5f)
        val shift = normalizedPhase(phase) * period
        return LinearGradientShader(
            from = Offset(-period + shift, -height * 0.35f),
            to = Offset(shift, height * 1.35f),
            colors = palette,
            tileMode = TileMode.Repeated,
        )
    }
}

private class ShimmerMotionBrush(
    private val colors: List<Color>,
    private val phase: Float,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val palette = colors.ifEmpty { listOf(Color.Transparent) }
        val width = size.width.coerceAtLeast(1f)
        val height = size.height.coerceAtLeast(1f)
        val base = LinearGradientShader(
            from = Offset.Zero,
            to = Offset(width, height),
            colors = palette,
        )
        val progress = normalizedPhase(phase)
        val center = Offset(
            x = width * (-0.28f + progress * 1.56f),
            y = height * (0.5f + sin(progress * TwoPi) * 0.28f),
        )
        val glowColor = sampleLoop(palette, progress + 0.38f)
        val glow = RadialGradientShader(
            center = center,
            radius = max(width * 0.34f, height * 2.8f),
            colors = listOf(
                glowColor,
                glowColor.copy(alpha = glowColor.alpha * 0.72f),
                glowColor.copy(alpha = 0f),
            ),
            colorStops = listOf(0f, 0.36f, 1f),
        )
        return CompositeShader(base, glow, BlendMode.SrcOver)
    }
}

private class MeshMotionBrush(
    private val colors: List<Color>,
    private val phase: Float,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val palette = colors.ifEmpty { listOf(Color.Transparent) }
        val width = size.width.coerceAtLeast(1f)
        val height = size.height.coerceAtLeast(1f)
        val angle = normalizedPhase(phase) * TwoPi
        var shader = LinearGradientShader(
            from = Offset.Zero,
            to = Offset(width, height),
            colors = listOf(palette[0], palette[1 % palette.size], palette[2 % palette.size]),
        )
        val radius = max(width * 0.44f, height * 2.4f)
        val centers = listOf(
            Offset(width * (0.16f + sin(angle) * 0.24f), height * (0.20f + cos(angle) * 0.24f)),
            Offset(width * (0.82f + cos(angle) * 0.22f), height * (0.24f + sin(angle) * 0.26f)),
            Offset(width * (0.24f + cos(angle + 2.1f) * 0.25f), height * (0.78f + sin(angle) * 0.22f)),
            Offset(width * (0.78f + sin(angle + 1.4f) * 0.24f), height * (0.76f + cos(angle) * 0.24f)),
        )
        centers.forEachIndexed { index, center ->
            val color = palette[(index + 2) % palette.size]
            val blob = RadialGradientShader(
                center = center,
                radius = radius,
                colors = listOf(color, color.copy(alpha = 0f)),
                colorStops = listOf(0f, 1f),
            )
            shader = CompositeShader(shader, blob, BlendMode.SrcOver)
        }
        return shader
    }
}

private class ImmiscibleLineBrush(
    private val colors: List<Color>,
    private val phase: Float,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val palette = colors.ifEmpty { listOf(Color.Transparent) }
        val width = size.width.coerceAtLeast(1f)
        val height = size.height.coerceAtLeast(1f)
        val first = palette.first()
        val second = palette[(palette.size / 2).coerceAtMost(palette.lastIndex)]
        val third = palette.last()
        val angle = normalizedPhase(phase) * TwoPi
        var shader = LinearGradientShader(
            from = Offset.Zero,
            to = Offset(width, 0f),
            colors = listOf(first, first),
        )
        val drops = listOf(
            Triple(second, 0.24f + sin(angle) * 0.16f, 0.92f),
            Triple(second, 0.67f + cos(angle * 0.84f) * 0.18f, 1.35f),
            Triple(third, 0.48f + sin(angle + 2.2f) * 0.20f, 0.72f),
        )
        drops.forEachIndexed { index, (color, centerX, scale) ->
            val center = Offset(
                x = width * centerX,
                y = height * (0.5f + sin(angle + index * 1.7f) * 0.22f),
            )
            val drop = RadialGradientShader(
                center = center,
                radius = max(height * 1.7f, width * 0.16f) * scale,
                colors = listOf(color, color, color.copy(alpha = 0f), color.copy(alpha = 0f)),
                colorStops = listOf(0f, 0.76f, 0.80f, 1f),
            )
            shader = CompositeShader(shader, drop, BlendMode.SrcOver)
        }
        return shader
    }
}

private fun loopPalette(colors: List<Color>): List<Color> {
    val palette = colors.ifEmpty { listOf(Color.Transparent) }
    return if (palette.first() == palette.last()) palette else palette + palette.first()
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
        lerp(base, Color.White, highlight * strength)
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

private fun sampleLoop(colors: List<Color>, phase: Float): Color {
    if (colors.isEmpty()) return Color.Transparent
    if (colors.size == 1) return colors.first()

    val position = normalizedPhase(phase) * colors.size
    val index = floor(position).toInt().coerceIn(0, colors.lastIndex)
    val nextIndex = (index + 1) % colors.size
    val fraction = position - index
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

private fun circularDistance(first: Float, second: Float): Float {
    val distance = kotlin.math.abs(normalizedPhase(first) - normalizedPhase(second))
    return kotlin.math.min(distance, 1f - distance)
}

@Composable
internal fun rememberAnimatedAccentBrush(
    previewTheme: AppTheme? = null,
    customFirst: Color = ThemeAccentColor.PINK.color,
    customSecond: Color = ThemeAccentColor.CYAN.color,
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
internal fun rememberAnimatedSelectionBrush(): Brush? = currentAnimatedThemeVisuals?.selectionBrush

@Composable
internal fun rememberAnimatedSoftBrush(): Brush? = currentAnimatedThemeVisuals?.softBrush

private const val TwoPi = 6.2831855f
