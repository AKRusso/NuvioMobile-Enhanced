package com.nuvio.app.core.ui

import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.Display
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt

@Composable
actual fun isTvLayoutProfileEnabled(): Boolean {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val isWideLandscapeWindow = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
        configuration.screenWidthDp >= 840 && configuration.screenHeightDp >= 480
    // The current configuration follows the DeX/freeform window. Display 0 remains the
    // built-in panel, so its metrics preserve the physical phone/tablet classification.
    val builtInSmallestWidthDp = remember(
        context,
        configuration.orientation,
        configuration.screenWidthDp,
        configuration.screenHeightDp,
    ) {
        context.builtInSmallestWidthDp()
    }

    return shouldEnableTvLayoutProfile(
        isTelevision = context.isTelevisionEnvironment(configuration),
        isWideLandscapeWindow = isWideLandscapeWindow,
        isSamsungDesktopMode = configuration.isSamsungDesktopMode(),
        builtInSmallestWidthDp = builtInSmallestWidthDp,
    )
}

private fun Configuration.isSamsungDesktopMode(): Boolean = runCatching {
    val field = javaClass.fields.firstOrNull { it.name == "semDesktopModeEnabled" } ?: return@runCatching false
    when (val value = field.get(this)) {
        is Boolean -> value
        is Int -> value == SamsungDesktopModeEnabled
        else -> false
    }
}.getOrDefault(false)

@Suppress("DEPRECATION")
private fun Context.builtInSmallestWidthDp(): Int? = runCatching {
    val displayManager = getSystemService(DisplayManager::class.java) ?: return@runCatching null
    val builtInDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY) ?: return@runCatching null
    val metrics = DisplayMetrics()
    builtInDisplay.getRealMetrics(metrics)
    val density = metrics.density.takeIf { it > 0f } ?: return@runCatching null
    val smallestPixels = minOf(metrics.widthPixels, metrics.heightPixels).takeIf { it > 0 }
        ?: return@runCatching null
    (smallestPixels / density).roundToInt()
}.getOrNull()

private const val SamsungDesktopModeEnabled = 1
