package com.nuvio.app.features.livetv

import platform.Foundation.NSUserDefaults

actual object LiveTvStorage {
    private const val sourceUrlKey = "live_tv_m3u_source_url"
    private const val favoriteUrlsKey = "live_tv_favorite_channel_urls"
    private const val recentChannelUrlKey = "live_tv_recent_channel_url"
    private const val recentChannelNameKey = "live_tv_recent_channel_name"
    private const val recentChannelLogoKey = "live_tv_recent_channel_logo"
    private const val recentChannelGroupKey = "live_tv_recent_channel_group"
    private const val recentChannelTvgIdKey = "live_tv_recent_channel_tvg_id"

    actual fun loadSourceUrl(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(sourceUrlKey)

    actual fun saveSourceUrl(url: String) {
        if (url.isBlank()) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(sourceUrlKey)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(url, forKey = sourceUrlKey)
        }
    }

    actual fun loadFavoriteUrls(): Set<String> =
        NSUserDefaults.standardUserDefaults
            .stringForKey(favoriteUrlsKey)
            .orEmpty()
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSet()

    actual fun saveFavoriteUrls(urls: Set<String>) {
        NSUserDefaults.standardUserDefaults.setObject(
            urls.sorted().joinToString("\n"),
            forKey = favoriteUrlsKey,
        )
    }

    actual fun loadRecentChannel(): LiveTvRecentChannel? {
        val defaults = NSUserDefaults.standardUserDefaults
        val streamUrl = defaults.stringForKey(recentChannelUrlKey).orEmpty().trim()
        val name = defaults.stringForKey(recentChannelNameKey).orEmpty().trim()
        if (streamUrl.isBlank() || name.isBlank()) return null
        return LiveTvRecentChannel(
            streamUrl = streamUrl,
            name = name,
            logoUrl = defaults.stringForKey(recentChannelLogoKey)?.takeIf(String::isNotBlank),
            group = defaults.stringForKey(recentChannelGroupKey).orEmpty(),
            tvgId = defaults.stringForKey(recentChannelTvgIdKey)?.takeIf(String::isNotBlank),
        )
    }

    actual fun saveRecentChannel(channel: LiveTvRecentChannel?) {
        val defaults = NSUserDefaults.standardUserDefaults
        if (channel == null) {
            defaults.removeObjectForKey(recentChannelUrlKey)
            defaults.removeObjectForKey(recentChannelNameKey)
            defaults.removeObjectForKey(recentChannelLogoKey)
            defaults.removeObjectForKey(recentChannelGroupKey)
            defaults.removeObjectForKey(recentChannelTvgIdKey)
            return
        }
        defaults.setObject(channel.streamUrl, forKey = recentChannelUrlKey)
        defaults.setObject(channel.name, forKey = recentChannelNameKey)
        if (channel.logoUrl.isNullOrBlank()) defaults.removeObjectForKey(recentChannelLogoKey)
        else defaults.setObject(channel.logoUrl, forKey = recentChannelLogoKey)
        if (channel.group.isBlank()) defaults.removeObjectForKey(recentChannelGroupKey)
        else defaults.setObject(channel.group, forKey = recentChannelGroupKey)
        if (channel.tvgId.isNullOrBlank()) defaults.removeObjectForKey(recentChannelTvgIdKey)
        else defaults.setObject(channel.tvgId, forKey = recentChannelTvgIdKey)
    }
}
