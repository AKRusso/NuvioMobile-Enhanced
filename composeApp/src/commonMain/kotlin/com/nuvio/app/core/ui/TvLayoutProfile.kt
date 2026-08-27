package com.nuvio.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

data class TvLayoutProfile(
    val enabled: Boolean = false,
    val uiScale: Float = 1f,
)

val LocalTvLayoutProfile = staticCompositionLocalOf { TvLayoutProfile() }

@Composable
expect fun isTvLayoutProfileEnabled(): Boolean

internal fun shouldEnableTvLayoutProfile(
    isTelevision: Boolean,
    isWideLandscapeWindow: Boolean,
    isSamsungDesktopMode: Boolean,
    builtInSmallestWidthDp: Int?,
): Boolean = isTelevision || (
    isWideLandscapeWindow &&
        isSamsungDesktopMode &&
        builtInSmallestWidthDp != null &&
        builtInSmallestWidthDp < 600
    )
