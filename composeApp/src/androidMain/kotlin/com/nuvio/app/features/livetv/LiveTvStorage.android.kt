package com.nuvio.app.features.livetv

import android.content.Context
import android.content.SharedPreferences

actual object LiveTvStorage {
    private const val preferencesName = "nuvio_live_tv"
    private const val sourceUrlKey = "m3u_source_url"

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
}
