package com.nuvio.app.core.network

import platform.Foundation.NSUserDefaults

actual object DnsOverHttpsSettingsStorage {
    private const val providerKey = "dns_over_https_provider"

    actual fun loadProviderId(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(providerKey)

    actual fun saveProviderId(providerId: String) {
        NSUserDefaults.standardUserDefaults.setObject(providerId, forKey = providerKey)
    }
}
