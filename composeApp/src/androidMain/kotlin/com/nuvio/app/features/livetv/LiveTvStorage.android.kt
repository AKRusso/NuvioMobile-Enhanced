package com.nuvio.app.features.livetv

import android.content.Context
import android.content.SharedPreferences

actual object LiveTvStorage {
    private const val preferencesName = "nuvio_live_tv"
    private const val sourceTypeKey = "source_type"
    private const val sourceUrlKey = "m3u_source_url"
    private const val stalkerPortalUrlKey = "stalker_portal_url"
    private const val stalkerMacAddressKey = "stalker_mac_address"
    private const val stalkerUsernameKey = "stalker_username"
    private const val stalkerPasswordKey = "stalker_password"
    private const val favoriteUrlsKey = "favorite_channel_urls"
    private const val recentChannelUrlKey = "recent_channel_url"
    private const val recentChannelNameKey = "recent_channel_name"
    private const val recentChannelLogoKey = "recent_channel_logo"
    private const val recentChannelGroupKey = "recent_channel_group"
    private const val recentChannelTvgIdKey = "recent_channel_tvg_id"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadSourceType(): LiveTvSourceType =
        when (preferences?.getString(sourceTypeKey, LiveTvSourceType.M3u.name)) {
            LiveTvSourceType.Stalker.name -> LiveTvSourceType.Stalker
            else -> LiveTvSourceType.M3u
        }

    actual fun saveSourceType(type: LiveTvSourceType) {
        preferences?.edit()?.putString(sourceTypeKey, type.name)?.apply()
    }

    actual fun loadSourceUrl(): String? =
        preferences?.getString(sourceUrlKey, null)

    actual fun saveSourceUrl(url: String) {
        preferences?.edit()?.apply {
            if (url.isBlank()) remove(sourceUrlKey) else putString(sourceUrlKey, url)
        }?.apply()
    }

    actual fun loadStalkerSettings(): LiveTvStalkerSettings =
        LiveTvStalkerSettings(
            portalUrl = preferences?.getString(stalkerPortalUrlKey, null).orEmpty(),
            macAddress = preferences?.getString(stalkerMacAddressKey, null).orEmpty(),
            username = preferences?.getString(stalkerUsernameKey, null).orEmpty(),
            password = preferences?.getString(stalkerPasswordKey, null).orEmpty(),
        )

    actual fun saveStalkerSettings(settings: LiveTvStalkerSettings) {
        preferences?.edit()?.apply {
            putString(stalkerPortalUrlKey, settings.portalUrl)
            putString(stalkerMacAddressKey, settings.macAddress)
            putString(stalkerUsernameKey, settings.username)
            putString(stalkerPasswordKey, settings.password)
        }?.apply()
    }

    actual fun loadFavoriteUrls(): Set<String> =
        preferences?.getStringSet(favoriteUrlsKey, emptySet()).orEmpty()

    actual fun saveFavoriteUrls(urls: Set<String>) {
        preferences?.edit()?.putStringSet(favoriteUrlsKey, urls)?.apply()
    }

    actual fun loadRecentChannel(): LiveTvRecentChannel? {
        val prefs = preferences ?: return null
        val streamUrl = prefs.getString(recentChannelUrlKey, null).orEmpty().trim()
        val name = prefs.getString(recentChannelNameKey, null).orEmpty().trim()
        if (streamUrl.isBlank() || name.isBlank()) return null
        return LiveTvRecentChannel(
            streamUrl = streamUrl,
            name = name,
            logoUrl = prefs.getString(recentChannelLogoKey, null)?.takeIf(String::isNotBlank),
            group = prefs.getString(recentChannelGroupKey, null).orEmpty(),
            tvgId = prefs.getString(recentChannelTvgIdKey, null)?.takeIf(String::isNotBlank),
        )
    }

    actual fun saveRecentChannel(channel: LiveTvRecentChannel?) {
        preferences?.edit()?.apply {
            if (channel == null) {
                remove(recentChannelUrlKey)
                remove(recentChannelNameKey)
                remove(recentChannelLogoKey)
                remove(recentChannelGroupKey)
                remove(recentChannelTvgIdKey)
            } else {
                putString(recentChannelUrlKey, channel.streamUrl)
                putString(recentChannelNameKey, channel.name)
                if (channel.logoUrl.isNullOrBlank()) remove(recentChannelLogoKey) else putString(recentChannelLogoKey, channel.logoUrl)
                if (channel.group.isBlank()) remove(recentChannelGroupKey) else putString(recentChannelGroupKey, channel.group)
                if (channel.tvgId.isNullOrBlank()) remove(recentChannelTvgIdKey) else putString(recentChannelTvgIdKey, channel.tvgId)
            }
        }?.apply()
    }
}
