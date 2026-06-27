package com.nuvio.app.features.livetv

import platform.Foundation.NSUserDefaults

actual object LiveTvStorage {
    private const val sourceTypeKey = "live_tv_source_type"
    private const val sourceUrlKey = "live_tv_m3u_source_url"
    private const val stalkerPortalUrlKey = "live_tv_stalker_portal_url"
    private const val stalkerMacAddressKey = "live_tv_stalker_mac_address"
    private const val stalkerUsernameKey = "live_tv_stalker_username"
    private const val stalkerPasswordKey = "live_tv_stalker_password"
    private const val favoriteUrlsKey = "live_tv_favorite_channel_urls"
    private const val recentChannelUrlKey = "live_tv_recent_channel_url"
    private const val recentChannelNameKey = "live_tv_recent_channel_name"
    private const val recentChannelLogoKey = "live_tv_recent_channel_logo"
    private const val recentChannelGroupKey = "live_tv_recent_channel_group"
    private const val recentChannelTvgIdKey = "live_tv_recent_channel_tvg_id"

    actual fun loadSourceType(): LiveTvSourceType =
        when (NSUserDefaults.standardUserDefaults.stringForKey(sourceTypeKey)) {
            LiveTvSourceType.Stalker.name -> LiveTvSourceType.Stalker
            else -> LiveTvSourceType.M3u
        }

    actual fun saveSourceType(type: LiveTvSourceType) {
        NSUserDefaults.standardUserDefaults.setObject(type.name, forKey = sourceTypeKey)
    }

    actual fun loadSourceUrl(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(sourceUrlKey)

    actual fun saveSourceUrl(url: String) {
        if (url.isBlank()) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(sourceUrlKey)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(url, forKey = sourceUrlKey)
        }
    }

    actual fun loadStalkerSettings(): LiveTvStalkerSettings {
        val defaults = NSUserDefaults.standardUserDefaults
        return LiveTvStalkerSettings(
            portalUrl = defaults.stringForKey(stalkerPortalUrlKey).orEmpty(),
            macAddress = defaults.stringForKey(stalkerMacAddressKey).orEmpty(),
            username = defaults.stringForKey(stalkerUsernameKey).orEmpty(),
            password = defaults.stringForKey(stalkerPasswordKey).orEmpty(),
        )
    }

    actual fun saveStalkerSettings(settings: LiveTvStalkerSettings) {
        val defaults = NSUserDefaults.standardUserDefaults
        defaults.setObject(settings.portalUrl, forKey = stalkerPortalUrlKey)
        defaults.setObject(settings.macAddress, forKey = stalkerMacAddressKey)
        defaults.setObject(settings.username, forKey = stalkerUsernameKey)
        defaults.setObject(settings.password, forKey = stalkerPasswordKey)
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
