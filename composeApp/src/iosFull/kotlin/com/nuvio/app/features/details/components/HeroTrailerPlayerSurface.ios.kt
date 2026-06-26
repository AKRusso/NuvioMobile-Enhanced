package com.nuvio.app.features.details.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitViewController
import com.nuvio.app.features.player.NuvioPlayerBridgeFactory
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun HeroTrailerPlayerSurface(
    sourceUrl: String,
    sourceAudioUrl: String?,
    playWhenReady: Boolean,
    muted: Boolean,
    modifier: Modifier,
    onReady: () -> Unit,
    onEnded: () -> Unit,
    onError: () -> Unit,
) {
    val latestOnReady = rememberUpdatedState(onReady)
    val latestOnError = rememberUpdatedState(onError)
    val bridge = remember { NuvioPlayerBridgeFactory.create() }

    if (bridge == null) {
        LaunchedEffect(Unit) {
            latestOnError.value()
        }
        return
    }

    val viewController = remember(bridge) {
        bridge.createPlayerViewController()
    }

    LaunchedEffect(sourceUrl, sourceAudioUrl) {
        runCatching {
            bridge.loadFileWithAudio(
                videoUrl = sourceUrl,
                audioUrl = sourceAudioUrl,
                headersJson = null,
                subtitlesJson = null,
            )
            latestOnReady.value()
        }.getOrElse {
            latestOnError.value()
        }
    }

    LaunchedEffect(bridge, playWhenReady) {
        if (playWhenReady) {
            bridge.play()
        } else {
            bridge.pause()
        }
    }

    LaunchedEffect(bridge, muted) {
        bridge.setMuted(muted)
    }

    DisposableEffect(bridge) {
        onDispose {
            bridge.destroy()
        }
    }

    Box(modifier = modifier) {
        UIKitViewController(
            factory = { viewController },
            modifier = Modifier.fillMaxSize(),
            interactive = false,
        )
    }
}
