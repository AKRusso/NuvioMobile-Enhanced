package com.nuvio.app.features.settings

import com.nuvio.app.features.settings.iosappicon.NuvioAppIconSetAlternateIconName
import com.nuvio.app.features.settings.iosappicon.NuvioAppIconSupportsAlternateIcons
import kotlinx.cinterop.ExperimentalForeignApi
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

internal actual object NuvioAppIconSwitcher {
    @OptIn(ExperimentalForeignApi::class)
    actual fun restoreDefault() {
        if (NuvioAppIconSupportsAlternateIcons() != 1) return
        dispatch_async(dispatch_get_main_queue()) {
            NuvioAppIconSetAlternateIconName(null)
        }
    }
}
