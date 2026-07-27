package com.nuvio.app.features.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import co.touchlab.kermit.Logger

internal actual object NuvioAppIconSwitcher {
    private val log = Logger.withTag("NuvioAppIconSwitcher")
    private const val aliasPackageName = "com.nuvio.enhanced"
    private const val defaultAlias = "$aliasPackageName.IconDefault"

    private val legacyAliases = listOf(
        "$aliasPackageName.IconEnhanced",
        "$aliasPackageName.IconMonochrome",
        "$aliasPackageName.IconNeon",
        "$aliasPackageName.IconGear",
        "$aliasPackageName.IconChrome",
        "$aliasPackageName.IconAurora",
        "$aliasPackageName.IconEmerald",
    )

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    actual fun restoreDefault() {
        val context = appContext ?: return
        runCatching {
            val packageManager = context.packageManager
            val flags = PackageManager.DONT_KILL_APP or synchronousPackageManagerFlag()

            packageManager.setComponentEnabledSetting(
                ComponentName(context.packageName, defaultAlias),
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                flags,
            )

            legacyAliases.forEach { alias ->
                ComponentName(context.packageName, alias).let { component ->
                    packageManager.setComponentEnabledSetting(
                        component,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        flags,
                    )
                }
            }
        }.onFailure { error ->
            log.w(error) { "Failed to restore the default app icon" }
        }
    }

    private fun synchronousPackageManagerFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PackageManager.SYNCHRONOUS
        } else {
            0
        }
}
