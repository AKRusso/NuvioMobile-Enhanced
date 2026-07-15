package com.nuvio.app.features.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

internal actual object NuvioAppIconSwitcher {
    private const val packageName = "com.nuvio.enhanced"
    private const val defaultAlias = "$packageName.IconDefault"

    private val aliases = mapOf(
        NuvioAppIconOption.Default.id to defaultAlias,
        NuvioAppIconOption.Neon.id to "$packageName.IconNeon",
        NuvioAppIconOption.Gear.id to "$packageName.IconGear",
        NuvioAppIconOption.Chrome.id to "$packageName.IconChrome",
        NuvioAppIconOption.Aurora.id to "$packageName.IconAurora",
        NuvioAppIconOption.Emerald.id to "$packageName.IconEmerald",
    )

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    actual fun apply(iconId: String): Boolean {
        val context = appContext ?: return false
        val targetAlias = aliases[iconId] ?: defaultAlias
        return runCatching {
            aliases.values.forEach { alias ->
                context.packageManager.setComponentEnabledSetting(
                    ComponentName(context, alias),
                    if (alias == targetAlias) {
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    } else {
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    },
                    PackageManager.DONT_KILL_APP,
                )
            }
        }.isSuccess
    }
}
