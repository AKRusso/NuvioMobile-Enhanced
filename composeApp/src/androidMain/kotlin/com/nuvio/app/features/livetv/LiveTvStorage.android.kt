package com.nuvio.app.features.livetv

import android.content.Context
import android.content.SharedPreferences

actual object LiveTvStorage {
    private const val preferencesName = "nuvio_live_tv"
    private const val sourceUrlKey = "m3u_source_url"
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

    actual fun loadSourceUrl(): String? =
        preferences?.getString(sourceUrlKey, null)

    actual fun saveSourceUrl(url: String) {
        preferences?.edit()?.apply {
            if (url.isBlank()) remove(sourceUrlKey) else putString(sourceUrlKey, url)
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
