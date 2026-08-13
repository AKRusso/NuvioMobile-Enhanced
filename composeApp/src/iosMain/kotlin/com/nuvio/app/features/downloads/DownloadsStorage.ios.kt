package com.nuvio.app.features.downloads

import com.nuvio.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

internal actual object DownloadsStorage {
    private const val payloadKey = "downloads_payload"
    private const val externalFolderUriKey = "external_folder_uri"

    actual fun loadPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = ProfileScopedKey.of(payloadKey))
    }

    actual fun loadExternalFolderUri(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(externalFolderUriKey))

    actual fun saveExternalFolderUri(uri: String?) {
        val key = ProfileScopedKey.of(externalFolderUriKey)
        if (uri.isNullOrBlank()) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(key)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(uri, forKey = key)
        }
    }
}
