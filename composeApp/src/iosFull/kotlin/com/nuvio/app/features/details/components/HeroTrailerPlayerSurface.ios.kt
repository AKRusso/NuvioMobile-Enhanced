package com.nuvio.app.features.details.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitViewController
import co.touchlab.kermit.Logger
import com.nuvio.app.features.player.NuvioPlayerBridgeFactory
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val HERO_TRAILER_IOS_TAG = "HeroTrailerIos"

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
    seekRequest: HeroTrailerSeekRequest?,
    onPlaybackStateChanged: (HeroTrailerPlaybackSnapshot) -> Unit,
) {
    val latestOnReady = rememberUpdatedState(onReady)
    val latestOnEnded = rememberUpdatedState(onEnded)
    val latestOnError = rememberUpdatedState(onError)
    val latestOnPlaybackStateChanged = rememberUpdatedState(onPlaybackStateChanged)
    val bridge = remember {
        NuvioPlayerBridgeFactory.create()
    }

    if (bridge == null) {
        LaunchedEffect(Unit) {
            latestOnError.value()
        }
        return
    }

    LaunchedEffect(bridge, sourceUrl, sourceAudioUrl) {
        runCatching {
            bridge.setEmbeddedPreviewMode(true)
            bridge.setResizeMode(1)
            bridge.setMuted(muted)
            bridge.loadFileWithAudio(
                videoUrl = sourceUrl,
                audioUrl = sourceAudioUrl,
                headersJson = null,
                subtitlesJson = null,
            )
            if (playWhenReady) {
                bridge.play()
            } else {
                bridge.pause()
            }
        }.onFailure { error ->
            Logger.w(HERO_TRAILER_IOS_TAG, error) { "Failed to load iOS hero trailer preview" }
            latestOnError.value()
        }
    }

    LaunchedEffect(bridge, playWhenReady) {
        runCatching {
            if (playWhenReady) {
                bridge.play()
            } else {
                bridge.pause()
            }
        }.onFailure { error ->
            Logger.w(HERO_TRAILER_IOS_TAG, error) { "Failed to update iOS hero trailer playback state" }
            latestOnError.value()
        }
    }

    LaunchedEffect(bridge, muted) {
        runCatching {
            bridge.setMuted(muted)
        }.onFailure { error ->
            Logger.w(HERO_TRAILER_IOS_TAG, error) { "Failed to update iOS hero trailer mute state" }
        }
    }


    LaunchedEffect(bridge, seekRequest?.id) {
        val request = seekRequest ?: return@LaunchedEffect
        runCatching {
            bridge.seekTo(request.positionMs.coerceAtLeast(0L))
            if (playWhenReady) bridge.play()
        }.onFailure { error ->
            Logger.w(HERO_TRAILER_IOS_TAG, error) { "Failed to seek iOS hero trailer preview" }
            latestOnError.value()
        }
    }

    LaunchedEffect(bridge, sourceUrl, sourceAudioUrl) {
        var readyReported = false
        var endedReported = false
        var lastErrorMessage: String? = null
        while (isActive) {
            val errorMessage = runCatching { bridge.getErrorMessage().ifBlank { null } }.getOrNull()
            if (errorMessage != null && errorMessage != lastErrorMessage) {
                lastErrorMessage = errorMessage
                latestOnError.value()
                return@LaunchedEffect
            }
            if (!readyReported) {
                val hasVideo = runCatching { bridge.getVideoWidth() > 0 && bridge.getVideoHeight() > 0 }
                    .getOrDefault(false)
                val isPlaying = runCatching { bridge.getIsPlaying() }.getOrDefault(false)
                if (hasVideo || isPlaying) {
                    readyReported = true
                    latestOnReady.value()
                }
            }
            if (!endedReported && runCatching { bridge.getIsEnded() }.getOrDefault(false)) {
                endedReported = true
                latestOnEnded.value()
            } else if (endedReported && !runCatching { bridge.getIsEnded() }.getOrDefault(false)) {
                endedReported = false
            }
            latestOnPlaybackStateChanged.value(
                HeroTrailerPlaybackSnapshot(
                    positionMs = runCatching { bridge.getPositionMs() }.getOrDefault(0L).coerceAtLeast(0L),
                    durationMs = runCatching { bridge.getDurationMs() }.getOrDefault(0L).coerceAtLeast(0L),
                    isPlaying = runCatching { bridge.getIsPlaying() }.getOrDefault(false),
                ),
            )
            delay(250L)
        }
    }

    DisposableEffect(bridge) {
        onDispose {
            runCatching {
                bridge.pause()
                bridge.setEmbeddedPreviewMode(false)
                bridge.destroy()
            }.onFailure { error ->
                Logger.w(HERO_TRAILER_IOS_TAG, error) { "Failed to dispose iOS hero trailer preview" }
            }
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
    ) {
        UIKitViewController(
            factory = { bridge.createPlayerViewController() },
            modifier = Modifier.fillMaxSize(),
            interactive = false,
        )
    }
}
