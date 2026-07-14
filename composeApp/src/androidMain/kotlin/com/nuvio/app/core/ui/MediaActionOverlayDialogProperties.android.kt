package com.nuvio.app.core.ui

import androidx.compose.ui.window.DialogProperties

internal actual fun nuvioMediaActionOverlayDialogProperties(): DialogProperties =
    DialogProperties(
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false,
    )
