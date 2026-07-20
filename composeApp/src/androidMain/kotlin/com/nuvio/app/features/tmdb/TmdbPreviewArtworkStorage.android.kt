package com.nuvio.app.features.tmdb

import android.content.Context
import android.content.SharedPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object TmdbPreviewArtworkStorage {
    private const val preferencesName = "nuvio_tmdb_preview_artwork"
    private const val cacheKeyPrefix = "preview_artwork_"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun load(cacheKey: String): String? =
        preferences?.getString(storageKey(cacheKey), null)

    actual fun save(cacheKey: String, payload: String) {
        preferences
            ?.edit()
            ?.putString(storageKey(cacheKey), payload)
            ?.apply()
    }

    private fun storageKey(cacheKey: String): String =
        ProfileScopedKey.of("$cacheKeyPrefix${cacheKey.hashCode()}")
}
