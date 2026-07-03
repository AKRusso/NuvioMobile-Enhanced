package com.nuvio.app.core.network

import android.content.Context
import android.content.SharedPreferences

actual object DnsOverHttpsSettingsStorage {
    private const val preferencesName = "nuvio_dns_over_https_settings"
    private const val providerKey = "dns_over_https_provider"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadProviderId(): String? =
        preferences?.getString(providerKey, null)

    actual fun saveProviderId(providerId: String) {
        preferences
            ?.edit()
            ?.putString(providerKey, providerId)
            ?.apply()
    }
}
