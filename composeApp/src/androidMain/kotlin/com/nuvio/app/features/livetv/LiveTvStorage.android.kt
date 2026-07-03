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

    private fun resolvedProfileId(): Int = resolveLiveTvStorageProfileId()

    private fun scopedKey(baseKey: String, profileId: Int = resolvedProfileId()): String = "${baseKey}_$profileId"

    private fun SharedPreferences.getScopedString(baseKey: String): String? {
        val profileId = resolvedProfileId()
        return getString(scopedKey(baseKey, profileId), null)
            ?: if (profileId == 1) getString(baseKey, null) else null
    }

    private fun SharedPreferences.Editor.putScopedString(baseKey: String, value: String?) {
        val profileId = resolvedProfileId()
        val profileKey = scopedKey(baseKey, profileId)
        if (value.isNullOrBlank()) {
            remove(profileKey)
            if (profileId == 1) remove(baseKey)
        } else {
            putString(profileKey, value)
            if (profileId == 1) putString(baseKey, value)
        }
    }

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadSourceType(): LiveTvSourceType =
        when (preferences?.getScopedString(sourceTypeKey) ?: LiveTvSourceType.M3u.name) {
            LiveTvSourceType.Stalker.name -> LiveTvSourceType.Stalker
            else -> LiveTvSourceType.M3u
        }

    actual fun saveSourceType(type: LiveTvSourceType) {
        preferences?.edit()?.apply {
            putScopedString(sourceTypeKey, type.name)
        }?.apply()
    }

    actual fun loadSourceUrl(): String? =
        preferences?.getScopedString(sourceUrlKey)

    actual fun saveSourceUrl(url: String) {
        preferences?.edit()?.apply {
            putScopedString(sourceUrlKey, url)
        }?.apply()
    }

    actual fun loadStalkerSettings(): LiveTvStalkerSettings =
        LiveTvStalkerSettings(
            portalUrl = preferences?.getScopedString(stalkerPortalUrlKey).orEmpty(),
            macAddress = preferences?.getScopedString(stalkerMacAddressKey).orEmpty(),
            username = preferences?.getScopedString(stalkerUsernameKey).orEmpty(),
            password = preferences?.getScopedString(stalkerPasswordKey).orEmpty(),
        )

    actual fun saveStalkerSettings(settings: LiveTvStalkerSettings) {
        preferences?.edit()?.apply {
            putScopedString(stalkerPortalUrlKey, settings.portalUrl)
            putScopedString(stalkerMacAddressKey, settings.macAddress)
            putScopedString(stalkerUsernameKey, settings.username)
            putScopedString(stalkerPasswordKey, settings.password)
        }?.apply()
    }

    actual fun loadFavoriteUrls(): Set<String> =
        preferences?.getScopedString(favoriteUrlsKey)
            ?.lineSequence()
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?.toSet()
            .orEmpty()

    actual fun saveFavoriteUrls(urls: Set<String>) {
        preferences?.edit()?.apply {
            putScopedString(favoriteUrlsKey, urls.sorted().joinToString("\n"))
        }?.apply()
    }

    actual fun loadRecentChannel(): LiveTvRecentChannel? {
        val prefs = preferences ?: return null
        val streamUrl = prefs.getScopedString(recentChannelUrlKey).orEmpty().trim()
        val name = prefs.getScopedString(recentChannelNameKey).orEmpty().trim()
        if (streamUrl.isBlank() || name.isBlank()) return null
        return LiveTvRecentChannel(
            streamUrl = streamUrl,
            name = name,
            logoUrl = prefs.getScopedString(recentChannelLogoKey)?.takeIf(String::isNotBlank),
            group = prefs.getScopedString(recentChannelGroupKey).orEmpty(),
            tvgId = prefs.getScopedString(recentChannelTvgIdKey)?.takeIf(String::isNotBlank),
        )
    }

    actual fun saveRecentChannel(channel: LiveTvRecentChannel?) {
        preferences?.edit()?.apply {
            putScopedString(recentChannelUrlKey, channel?.streamUrl)
            putScopedString(recentChannelNameKey, channel?.name)
            putScopedString(recentChannelLogoKey, channel?.logoUrl)
            putScopedString(recentChannelGroupKey, channel?.group)
            putScopedString(recentChannelTvgIdKey, channel?.tvgId)
        }?.apply()
    }
}
