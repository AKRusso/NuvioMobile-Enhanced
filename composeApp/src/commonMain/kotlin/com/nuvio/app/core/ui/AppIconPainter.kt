package com.nuvio.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

enum class AppIconResource {
    DiscordMark,
    GithubMark,
    PlayerPlay,
    PlayerPause,
    PlayerAspectRatio,
    PlayerSubtitles,
    PlayerAudioFilled,
    LibraryAddPlus,
}

@Composable
expect fun appIconPainter(icon: AppIconResource): Painter
