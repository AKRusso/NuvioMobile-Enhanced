package com.nuvio.app.features.downloads

import android.content.Context
import android.content.SharedPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object DownloadsStorage {
    private const val preferencesName = "nuvio_downloads"
    private const val payloadKey = "downloads_payload"
    private const val externalFolderUriKey = "external_folder_uri"
    private const val episodeDownloadSettingsKey = "episode_download_settings"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadPayload(): String? =
        preferences?.getString(ProfileScopedKey.of(payloadKey), null)

    actual fun savePayload(payload: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(payloadKey), payload)
            ?.apply()
    }

    actual fun loadExternalFolderUri(): String? =
        preferences?.getString(ProfileScopedKey.of(externalFolderUriKey), null)

    actual fun saveExternalFolderUri(uri: String?) {
        preferences
            ?.edit()
            ?.let { editor ->
                if (uri.isNullOrBlank()) {
                    editor.remove(ProfileScopedKey.of(externalFolderUriKey))
                } else {
                    editor.putString(ProfileScopedKey.of(externalFolderUriKey), uri)
                }
                editor.apply()
            }
    }

    actual fun loadEpisodeDownloadSettings(): String? =
        preferences?.getString(ProfileScopedKey.of(episodeDownloadSettingsKey), null)

    actual fun saveEpisodeDownloadSettings(payload: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(episodeDownloadSettingsKey), payload)
            ?.apply()
    }
}
