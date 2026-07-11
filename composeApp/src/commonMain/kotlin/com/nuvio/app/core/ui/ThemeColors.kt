package com.nuvio.app.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.theme_color_blue
import nuvio.composeapp.generated.resources.theme_color_cyan
import nuvio.composeapp.generated.resources.theme_color_green
import nuvio.composeapp.generated.resources.theme_color_pink
import nuvio.composeapp.generated.resources.theme_color_purple
import nuvio.composeapp.generated.resources.theme_color_red
import nuvio.composeapp.generated.resources.theme_color_white
import nuvio.composeapp.generated.resources.theme_color_yellow
import org.jetbrains.compose.resources.StringResource

enum class ThemeAccentColor(
    val color: Color,
    val labelRes: StringResource,
) {
    PINK(Color(0xFFFF5F9E), Res.string.theme_color_pink),
    PURPLE(Color(0xFF9B5CFF), Res.string.theme_color_purple),
    BLUE(Color(0xFF397CFF), Res.string.theme_color_blue),
    CYAN(Color(0xFF35D6E8), Res.string.theme_color_cyan),
    GREEN(Color(0xFF57D67A), Res.string.theme_color_green),
    YELLOW(Color(0xFFFFD447), Res.string.theme_color_yellow),
    RED(Color(0xFFFF5263), Res.string.theme_color_red),
    WHITE(Color(0xFFF4F7FF), Res.string.theme_color_white),
}

data class ThemeColorPalette(
    val secondary: Color,
    val secondaryVariant: Color,
    val nativeAccentHex: String,
    val onSecondary: Color = Color.White,
    val onSecondaryVariant: Color = Color.White,
    val focusRing: Color,
    val focusBackground: Color,
    val background: Color = Color(0xFF0D0D0D),
    val backgroundElevated: Color = Color(0xFF1A1A1A),
    val backgroundCard: Color = Color(0xFF242424),
)

object ThemeColors {

    val Crimson = ThemeColorPalette(
        secondary = Color(0xFFE53935),
        secondaryVariant = Color(0xFFC62828),
        nativeAccentHex = "#E53935",
        focusRing = Color(0xFFFF5252),
        focusBackground = Color(0xFF3D1A1A),
        background = Color(0xFF0D0D0D),
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF241A1A),
    )

    val Ocean = ThemeColorPalette(
        secondary = Color(0xFF1E88E5),
        secondaryVariant = Color(0xFF1565C0),
        nativeAccentHex = "#1E88E5",
        focusRing = Color(0xFF42A5F5),
        focusBackground = Color(0xFF1A2D3D),
        background = Color(0xFF0D0D0F),
        backgroundElevated = Color(0xFF1A1A1E),
        backgroundCard = Color(0xFF1A1F24),
    )

    val Violet = ThemeColorPalette(
        secondary = Color(0xFF8E24AA),
        secondaryVariant = Color(0xFF6A1B9A),
        nativeAccentHex = "#8E24AA",
        focusRing = Color(0xFFAB47BC),
        focusBackground = Color(0xFF2D1A3D),
        background = Color(0xFF0D0D0F),
        backgroundElevated = Color(0xFF1A1A1E),
        backgroundCard = Color(0xFF1F1A24),
    )

    val Emerald = ThemeColorPalette(
        secondary = Color(0xFF43A047),
        secondaryVariant = Color(0xFF2E7D32),
        nativeAccentHex = "#43A047",
        focusRing = Color(0xFF66BB6A),
        focusBackground = Color(0xFF1A3D1E),
        background = Color(0xFF0D0D0D),
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF1A241A),
    )

    val Amber = ThemeColorPalette(
        secondary = Color(0xFFFB8C00),
        secondaryVariant = Color(0xFFEF6C00),
        nativeAccentHex = "#FB8C00",
        focusRing = Color(0xFFFFA726),
        focusBackground = Color(0xFF3D2D1A),
        background = Color(0xFF0F0D0D),
        backgroundElevated = Color(0xFF1E1A1A),
        backgroundCard = Color(0xFF24201A),
    )

    val Rose = ThemeColorPalette(
        secondary = Color(0xFFD81B60),
        secondaryVariant = Color(0xFFC2185B),
        nativeAccentHex = "#D81B60",
        focusRing = Color(0xFFEC407A),
        focusBackground = Color(0xFF3D1A2D),
        background = Color(0xFF0D0D0D),
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF241A1F),
    )

    val Aurora = ThemeColorPalette(
        secondary = Color(0xFFFF5F9E),
        secondaryVariant = Color(0xFF8B5CF6),
        nativeAccentHex = "#FF5F9E",
        focusRing = Color(0xFF67D5FF),
        focusBackground = Color(0xFF2A1838),
        background = Color(0xFF0E0C12),
        backgroundElevated = Color(0xFF1A1720),
        backgroundCard = Color(0xFF231D2A),
    )

    val Prism = ThemeColorPalette(
        secondary = Color(0xFF4CC9FF),
        secondaryVariant = Color(0xFFFF4FB3),
        nativeAccentHex = "#4CC9FF",
        onSecondary = Color(0xFF101318),
        focusRing = Color(0xFFFFFF66),
        focusBackground = Color(0xFF172B38),
        background = Color(0xFF0B0E12),
        backgroundElevated = Color(0xFF171B20),
        backgroundCard = Color(0xFF1D2429),
    )

    val Nebula = ThemeColorPalette(
        secondary = Color(0xFF9D68FF),
        secondaryVariant = Color(0xFF38D9D0),
        nativeAccentHex = "#9D68FF",
        focusRing = Color(0xFFFF62B0),
        focusBackground = Color(0xFF291B3B),
        background = Color(0xFF0E0B13),
        backgroundElevated = Color(0xFF1B1721),
        backgroundCard = Color(0xFF251D2D),
    )

    val Opal = ThemeColorPalette(
        secondary = Color(0xFF57E0C1),
        secondaryVariant = Color(0xFFFF7FAE),
        nativeAccentHex = "#57E0C1",
        onSecondary = Color(0xFF101318),
        focusRing = Color(0xFFC7A8FF),
        focusBackground = Color(0xFF19342F),
        background = Color(0xFF0B100F),
        backgroundElevated = Color(0xFF171D1C),
        backgroundCard = Color(0xFF1C2724),
    )

    val Custom = ThemeColorPalette(
        secondary = ThemeAccentColor.PINK.color,
        secondaryVariant = ThemeAccentColor.CYAN.color,
        nativeAccentHex = "#FF5F9E",
        focusRing = ThemeAccentColor.CYAN.color,
        focusBackground = Color(0xFF2A1838),
        background = Color(0xFF0E0C12),
        backgroundElevated = Color(0xFF1A1720),
        backgroundCard = Color(0xFF231D2A),
    )

    val White = ThemeColorPalette(
        secondary = Color(0xFFF5F5F5),
        secondaryVariant = Color(0xFFE0E0E0),
        nativeAccentHex = "#F5F5F5",
        onSecondary = Color(0xFF111111),
        onSecondaryVariant = Color(0xFF111111),
        focusRing = Color(0xFFFFFFFF),
        focusBackground = Color(0xFF303030),
        background = Color(0xFF0D0D0D),
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF222222),
    )

    fun getColorPalette(
        theme: AppTheme,
        customFirst: ThemeAccentColor = ThemeAccentColor.PINK,
        customSecond: ThemeAccentColor = ThemeAccentColor.CYAN,
    ): ThemeColorPalette = when (theme) {
        AppTheme.CRIMSON -> Crimson
        AppTheme.OCEAN -> Ocean
        AppTheme.VIOLET -> Violet
        AppTheme.EMERALD -> Emerald
        AppTheme.AMBER -> Amber
        AppTheme.ROSE -> Rose
        AppTheme.AURORA -> Aurora
        AppTheme.PRISM -> Prism
        AppTheme.NEBULA -> Nebula
        AppTheme.OPAL -> Opal
        AppTheme.CUSTOM -> Custom.copy(
            secondary = customFirst.color,
            secondaryVariant = customSecond.color,
            nativeAccentHex = customFirst.color.toAccentHex(),
            onSecondary = customFirst.color.contentColor(),
            onSecondaryVariant = customSecond.color.contentColor(),
            focusRing = customSecond.color,
        )
        AppTheme.WHITE -> White
    }

    fun animatedColors(
        theme: AppTheme,
        customFirst: ThemeAccentColor = ThemeAccentColor.PINK,
        customSecond: ThemeAccentColor = ThemeAccentColor.CYAN,
    ): List<Color> = when (theme) {
        AppTheme.AURORA -> listOf(
            Color(0xFFFF2F92),
            Color(0xFF7C4DFF),
            Color(0xFF00E5FF),
            Color(0xFFFFB000),
        )
        AppTheme.PRISM -> listOf(
            Color(0xFF00D6FF),
            Color(0xFFFF2F92),
            Color(0xFFFFFF4A),
            Color(0xFF00D66B),
        )
        AppTheme.NEBULA -> listOf(
            Color(0xFF8B5CFF),
            Color(0xFFFF3D9A),
            Color(0xFF00D4FF),
            Color(0xFFFFB86B),
        )
        AppTheme.OPAL -> listOf(
            Color(0xFF00FFC6),
            Color(0xFFFF4FA3),
            Color(0xFF7C6DFF),
            Color(0xFFFFE16B),
        )
        AppTheme.CUSTOM -> listOf(
            customFirst.color,
            customSecond.color,
            customFirst.color.copy(alpha = 0.82f),
            customSecond.color.copy(alpha = 0.82f),
        )
        else -> emptyList()
    }
}

private fun Color.contentColor(): Color =
    if (luminance() > 0.55f) Color(0xFF101318) else Color.White

private fun Color.toAccentHex(): String {
    fun channel(value: Float): String =
        (value.coerceIn(0f, 1f) * 255f).toInt().toString(16).padStart(2, '0').uppercase()
    return "#${channel(red)}${channel(green)}${channel(blue)}"
}
