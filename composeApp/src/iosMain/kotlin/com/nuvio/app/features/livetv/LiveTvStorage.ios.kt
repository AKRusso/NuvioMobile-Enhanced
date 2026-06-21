package com.nuvio.app.features.livetv

import platform.Foundation.NSUserDefaults

actual object LiveTvStorage {
    private const val sourceUrlKey = "live_tv_m3u_source_url"
    private const val favoriteUrlsKey = "live_tv_favorite_channel_urls"

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
}
