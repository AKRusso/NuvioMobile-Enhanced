package com.nuvio.app.features.streams

import android.content.Context
import android.content.SharedPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object StreamSourcePreferencesStorage {
    private const val preferencesName = "nuvio_stream_source_preferences"
    private const val pinnedSourcesKey = "pinned_sources"
    private const val pinnedSourceIdKey = "pinned_source_id"
    private const val pinnedSourceNameKey = "pinned_source_name"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadPinnedSourcesPayload(): String? =
        preferences?.getString(ProfileScopedKey.of(pinnedSourcesKey), null)

    actual fun savePinnedSourcesPayload(payload: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(pinnedSourcesKey), payload)
            ?.remove(ProfileScopedKey.of(pinnedSourceIdKey))
            ?.remove(ProfileScopedKey.of(pinnedSourceNameKey))
            ?.apply()
    }

    actual fun loadLegacyPinnedSourceId(): String? =
        preferences?.getString(ProfileScopedKey.of(pinnedSourceIdKey), null)

    actual fun loadLegacyPinnedSourceName(): String? =
        preferences?.getString(ProfileScopedKey.of(pinnedSourceNameKey), null)

    actual fun clearPinnedSources() {
        preferences
            ?.edit()
            ?.remove(ProfileScopedKey.of(pinnedSourcesKey))
            ?.remove(ProfileScopedKey.of(pinnedSourceIdKey))
            ?.remove(ProfileScopedKey.of(pinnedSourceNameKey))
            ?.apply()
    }
}
