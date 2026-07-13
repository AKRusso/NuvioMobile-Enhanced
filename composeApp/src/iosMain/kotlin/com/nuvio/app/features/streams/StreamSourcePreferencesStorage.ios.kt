package com.nuvio.app.features.streams

import com.nuvio.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

internal actual object StreamSourcePreferencesStorage {
    private const val pinnedSourcesKey = "pinned_sources"
    private const val pinnedSourceIdKey = "pinned_source_id"
    private const val pinnedSourceNameKey = "pinned_source_name"

    actual fun loadPinnedSourcesPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(pinnedSourcesKey))

    actual fun savePinnedSourcesPayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = ProfileScopedKey.of(pinnedSourcesKey))
        NSUserDefaults.standardUserDefaults.removeObjectForKey(ProfileScopedKey.of(pinnedSourceIdKey))
        NSUserDefaults.standardUserDefaults.removeObjectForKey(ProfileScopedKey.of(pinnedSourceNameKey))
    }

    actual fun loadLegacyPinnedSourceId(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(pinnedSourceIdKey))

    actual fun loadLegacyPinnedSourceName(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(pinnedSourceNameKey))

    actual fun clearPinnedSources() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(ProfileScopedKey.of(pinnedSourcesKey))
        NSUserDefaults.standardUserDefaults.removeObjectForKey(ProfileScopedKey.of(pinnedSourceIdKey))
        NSUserDefaults.standardUserDefaults.removeObjectForKey(ProfileScopedKey.of(pinnedSourceNameKey))
    }
}
