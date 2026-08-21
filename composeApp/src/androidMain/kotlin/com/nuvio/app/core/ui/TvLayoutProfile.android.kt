package com.nuvio.app.core.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
actual fun isTvLayoutProfileEnabled(): Boolean {
    val configuration = LocalConfiguration.current
    val isWideLandscapeWindow = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
        configuration.screenWidthDp >= 840 && configuration.screenHeightDp >= 480
    if (!isWideLandscapeWindow) return false

    // DeX exposes a Samsung desktop-mode flag on supported devices. The fallback covers
    // phone-class devices placed in a wide external/freeform window without affecting tablets.
    return configuration.isSamsungDesktopMode() || configuration.smallestScreenWidthDp < 600
}

private fun Configuration.isSamsungDesktopMode(): Boolean = runCatching {
    val field = javaClass.fields.firstOrNull { it.name == "semDesktopModeEnabled" } ?: return@runCatching false
    when (val value = field.get(this)) {
        is Boolean -> value
        is Int -> value != 0
        else -> false
    }
}.getOrDefault(false)
