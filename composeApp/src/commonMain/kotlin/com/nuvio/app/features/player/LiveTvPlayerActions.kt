package com.nuvio.app.features.player

import com.nuvio.app.features.livetv.LiveTvChannel

internal fun PlayerScreenRuntime.switchToLiveTvChannel(channel: LiveTvChannel) {
    com.nuvio.app.features.livetv.LiveTvRepository.recordRecentChannel(channel)
    if (channel.streamUrl == activeSourceUrl) {
        showLiveTvChannelsPanel = false
        controlsVisible = true
        return
    }

    activeSourceUrl = channel.streamUrl
    activeSourceAudioUrl = null
    activeSourceHeaders = sanitizePlaybackHeaders(channel.headers)
    activeSourceResponseHeaders = emptyMap()
    activeStreamType = null
    activeSourceIdentityKey = "live-tv:${channel.streamUrl}"
    activeStreamTitle = channel.name
    activeStreamSubtitle = null
    activeProviderName = "M3U"
    activeProviderAddonId = "live-tv"
    activeVideoId = null
    activeInitialPositionMs = 0L
    activeInitialProgressFraction = null
    args = args.copy(
        title = channel.name,
        logo = channel.logoUrl,
        streamTitle = channel.name,
        streamSubtitle = null,
        sourceUrl = channel.streamUrl,
        sourceHeaders = channel.headers,
    )
    showLiveTvChannelsPanel = false
    controlsVisible = true
}
