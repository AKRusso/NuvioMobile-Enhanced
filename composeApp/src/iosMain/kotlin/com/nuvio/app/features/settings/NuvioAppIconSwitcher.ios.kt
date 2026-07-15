package com.nuvio.app.features.settings

import com.nuvio.app.features.settings.iosappicon.NuvioAppIconSetAlternateIconName
import com.nuvio.app.features.settings.iosappicon.NuvioAppIconSupportsAlternateIcons
import kotlinx.cinterop.ExperimentalForeignApi
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

internal actual object NuvioAppIconSwitcher {
    private val alternateIconNames = mapOf(
        NuvioAppIconOption.Neon.id to "IconNeon",
        NuvioAppIconOption.Gear.id to "IconGear",
        NuvioAppIconOption.Chrome.id to "IconChrome",
        NuvioAppIconOption.Aurora.id to "IconAurora",
        NuvioAppIconOption.Emerald.id to "IconEmerald",
    )

    @OptIn(ExperimentalForeignApi::class)
    actual fun apply(iconId: String): Boolean {
        if (NuvioAppIconSupportsAlternateIcons() != 1) return false
        val iconName = alternateIconNames[iconId]
        dispatch_async(dispatch_get_main_queue()) {
            NuvioAppIconSetAlternateIconName(iconName)
        }
        return true
    }
}
