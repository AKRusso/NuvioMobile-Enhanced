package com.nuvio.app.core.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun isKeyboardNavigationAvailable(): Boolean {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    return shouldStartInKeyboardInputMode(
        hasHardwareKeyboard = configuration.keyboard != Configuration.KEYBOARD_NOKEYS,
        hasDpadNavigation = configuration.navigation == Configuration.NAVIGATION_DPAD,
        isTelevision = context.isTelevisionEnvironment(configuration),
    )
}
