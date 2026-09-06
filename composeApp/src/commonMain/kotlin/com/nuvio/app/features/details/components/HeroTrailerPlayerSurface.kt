package com.nuvio.app.features.details.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class HeroTrailerSeekRequest(
    val id: Int,
    val positionMs: Long,
)

data class HeroTrailerPlaybackSnapshot(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
)

@Composable
expect fun HeroTrailerPlayerSurface(
    sourceUrl: String,
    sourceAudioUrl: String?,
    playWhenReady: Boolean,
    muted: Boolean,
    modifier: Modifier,
    onReady: () -> Unit,
    onEnded: () -> Unit,
    onError: () -> Unit,
    seekRequest: HeroTrailerSeekRequest? = null,
    onPlaybackStateChanged: (HeroTrailerPlaybackSnapshot) -> Unit = {},
)
