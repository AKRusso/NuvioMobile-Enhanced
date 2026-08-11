package com.nuvio.app.features.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import kotlin.system.exitProcess

internal actual object AppIconPlatform {
    actual val requiresCloseConfirmation: Boolean = true

    private const val launcherPackage = "com.nuvio.app.launcher"
    private val launcherComponents = AppIconOption.entries.map { option ->
        option.platformName to "$launcherPackage.${option.platformName ?: "AppIconDefault"}"
    }
    private val legacyLauncherClasses = listOf(
        "com.nuvio.enhanced.IconDefault",
        "com.nuvio.enhanced.IconEnhanced",
        "com.nuvio.enhanced.IconMonochrome",
        "com.nuvio.enhanced.IconNeon",
        "com.nuvio.enhanced.IconGear",
        "com.nuvio.enhanced.IconChrome",
        "com.nuvio.enhanced.IconAurora",
        "com.nuvio.enhanced.IconEmerald",
    )

    private var context: Context? = null

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        this.context = appContext
        reconcileLegacyLaunchers(appContext)
    }

    actual fun currentIconName(): String? {
        val appContext = context ?: return null
        return currentIconName(appContext)
    }

    fun currentLauncherIconResource(context: Context): Int {
        val option = AppIconOption.fromPlatformName(currentIconName(context))
        val resourceName = if (option == AppIconOption.ORIGINAL) {
            "ic_launcher_alt_enhanced"
        } else {
            "ic_launcher_${option.key}"
        }
        return context.resources
            .getIdentifier(resourceName, "mipmap", context.packageName)
            .takeIf { it != 0 }
            ?: com.nuvio.app.R.mipmap.ic_launcher
    }

    fun currentLauncherComponent(context: Context): ComponentName {
        val currentName = currentIconName(context)
        val className = launcherComponents.first { it.first == currentName }.second
        return component(context, className)
    }

    private fun currentIconName(context: Context): String? {
        val packageManager = context.packageManager
        val explicitlyEnabled = launcherComponents.firstOrNull { (_, className) ->
            packageManager.getComponentEnabledSetting(component(context, className)) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        return explicitlyEnabled?.first
    }

    internal fun reconcileLegacyLaunchers() {
        context?.let(::reconcileLegacyLaunchers)
    }

    private fun reconcileLegacyLaunchers(context: Context) {
        val packageManager = context.packageManager
        val selectedClass = launcherComponents.firstOrNull { (_, className) ->
            packageManager.getComponentEnabledSetting(component(context, className)) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }?.second ?: launcherComponents.first { it.first == null }.second
        applyComponentStates(context, selectedClass)
    }

    actual suspend fun activateIcon(name: String?): Boolean {
        val appContext = context ?: return false
        val selectedClass = launcherComponents.firstOrNull { it.first == name }?.second ?: return false
        val changed = applyComponentStates(appContext, selectedClass)
        if (changed) {
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }
        return false
    }

    private fun applyComponentStates(context: Context, selectedClass: String): Boolean = runCatching {
        val packageManager = context.packageManager
        val allClasses = launcherComponents.map { it.second } + legacyLauncherClasses
        val flags = PackageManager.DONT_KILL_APP or synchronousPackageManagerFlag()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.setComponentEnabledSettings(
                allClasses.map { className ->
                    PackageManager.ComponentEnabledSetting(
                        component(context, className),
                        if (className == selectedClass) {
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        } else {
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        },
                        flags,
                    )
                },
            )
        } else {
            packageManager.setComponentEnabledSetting(
                component(context, selectedClass),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                flags,
            )
            allClasses.filterNot { it == selectedClass }.forEach { className ->
                packageManager.setComponentEnabledSetting(
                    component(context, className),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    flags,
                )
            }
        }
    }.isSuccess

    private fun component(context: Context, className: String): ComponentName =
        ComponentName(context.packageName, className)

    private fun synchronousPackageManagerFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PackageManager.SYNCHRONOUS
        } else {
            0
        }
}
