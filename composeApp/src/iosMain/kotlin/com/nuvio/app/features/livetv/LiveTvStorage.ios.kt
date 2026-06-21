package com.nuvio.app.features.livetv

import platform.Foundation.NSUserDefaults

actual object LiveTvStorage {
    private const val sourceUrlKey = "live_tv_m3u_source_url"

    actual fun loadSourceUrl(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(sourceUrlKey)

    actual fun saveSourceUrl(url: String) {
        if (url.isBlank()) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(sourceUrlKey)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(url, forKey = sourceUrlKey)
        }
    }
}
