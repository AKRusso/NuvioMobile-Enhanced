package com.nuvio.app.features.library

import platform.Foundation.NSUserDefaults

actual object LibraryStorage {
    private fun payloadKey(profileId: Int) = "library_payload_$profileId"
    private fun releaseSupportPayloadKey(profileId: Int, cacheKey: String) =
        "library_release_support_${profileId}_${cacheKey.hashCode()}"

    actual fun loadPayload(profileId: Int): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(payloadKey(profileId))

    actual fun savePayload(profileId: Int, payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = payloadKey(profileId))
    }

    actual fun loadReleaseSupportPayload(profileId: Int, cacheKey: String): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(releaseSupportPayloadKey(profileId, cacheKey))

    actual fun saveReleaseSupportPayload(profileId: Int, cacheKey: String, payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = releaseSupportPayloadKey(profileId, cacheKey))
    }
}
