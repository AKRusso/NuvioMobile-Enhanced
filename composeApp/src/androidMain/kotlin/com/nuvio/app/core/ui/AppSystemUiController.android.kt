package com.nuvio.app.core.ui

import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

internal actual object AppSystemUiController {
    private var activity: Activity? = null
    private var statusBarVisible: Boolean = true

    fun bind(activity: Activity) {
        this.activity = activity
        apply()
    }

    fun unbind(activity: Activity) {
        if (this.activity === activity) {
            this.activity = null
        }
    }

    actual fun setStatusBarVisible(visible: Boolean) {
        statusBarVisible = visible
        apply()
    }

    private fun apply() {
        val window = activity?.window ?: return
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (statusBarVisible) {
            controller.show(WindowInsetsCompat.Type.statusBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
