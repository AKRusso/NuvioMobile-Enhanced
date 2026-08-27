package com.nuvio.app.core.ui

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

internal fun Context.isTelevisionEnvironment(configuration: Configuration): Boolean {
    val uiModeManager = getSystemService(UiModeManager::class.java)
    val isTelevisionUiMode = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
        configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION
    val packageManager = packageManager
    return isTelevisionUiMode ||
        packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
        packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
}
