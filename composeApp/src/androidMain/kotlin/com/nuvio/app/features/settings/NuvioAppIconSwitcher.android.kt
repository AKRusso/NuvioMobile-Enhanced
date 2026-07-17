package com.nuvio.app.features.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import co.touchlab.kermit.Logger
import kotlin.system.exitProcess

internal actual object NuvioAppIconSwitcher {
    private val log = Logger.withTag("NuvioAppIconSwitcher")
    private const val aliasPackageName = "com.nuvio.enhanced"
    private const val defaultAlias = "$aliasPackageName.IconDefault"

    private val aliases = mapOf(
        NuvioAppIconOption.Default.id to defaultAlias,
        NuvioAppIconOption.Enhanced.id to "$aliasPackageName.IconEnhanced",
        NuvioAppIconOption.Monochrome.id to "$aliasPackageName.IconMonochrome",
        NuvioAppIconOption.Neon.id to "$aliasPackageName.IconNeon",
        NuvioAppIconOption.Gear.id to "$aliasPackageName.IconGear",
        NuvioAppIconOption.Chrome.id to "$aliasPackageName.IconChrome",
        NuvioAppIconOption.Aurora.id to "$aliasPackageName.IconAurora",
        NuvioAppIconOption.Emerald.id to "$aliasPackageName.IconEmerald",
    )

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    actual fun apply(iconId: String): Boolean {
        val context = appContext ?: return false
        val targetAlias = aliases[iconId] ?: defaultAlias
        return runCatching {
            val packageManager = context.packageManager
            val targetComponent = ComponentName(context.packageName, targetAlias)
            val knownComponents = aliases.values.map { alias ->
                ComponentName(context.packageName, alias)
            }
            val flags = PackageManager.DONT_KILL_APP or synchronousPackageManagerFlag()
            val matchFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PackageManager.MATCH_DISABLED_COMPONENTS
            } else {
                PackageManager.GET_DISABLED_COMPONENTS
            }

            packageManager.getActivityInfo(targetComponent, matchFlags)

            packageManager.setComponentEnabledSetting(
                targetComponent,
                if (targetComponent.className == defaultAlias) {
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                },
                flags,
            )

            knownComponents
                .filterNot { it == targetComponent }
                .forEach { component ->
                    packageManager.setComponentEnabledSetting(
                        component,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        flags,
                    )
                }
        }.onFailure { error ->
            log.w(error) { "Failed to apply app icon id=$iconId alias=$targetAlias" }
        }.isSuccess
    }

    actual fun closeAfterApply() {
        Handler(Looper.getMainLooper()).postDelayed(
            { exitProcess(0) },
            900L,
        )
    }

    private fun synchronousPackageManagerFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PackageManager.SYNCHRONOUS
        } else {
            0
        }
}
