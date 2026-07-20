package com.nuvio.app.features.tmdb

import com.nuvio.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

internal actual object TmdbPreviewArtworkStorage {
    private const val cacheKeyPrefix = "preview_artwork_"

    actual fun load(cacheKey: String): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(storageKey(cacheKey))

    actual fun save(cacheKey: String, payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = storageKey(cacheKey))
    }

    private fun storageKey(cacheKey: String): String =
        ProfileScopedKey.of("$cacheKeyPrefix${cacheKey.hashCode()}")
}
