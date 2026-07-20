package com.nuvio.app.features.library

import android.content.Context
import android.content.SharedPreferences

actual object LibraryStorage {
    private const val preferencesName = "nuvio_library"
    private fun payloadKey(profileId: Int) = "library_payload_$profileId"
    private fun releaseSupportPayloadKey(profileId: Int, cacheKey: String) =
        "library_release_support_${profileId}_${cacheKey.hashCode()}"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadPayload(profileId: Int): String? =
        preferences?.getString(payloadKey(profileId), null)

    actual fun savePayload(profileId: Int, payload: String) {
        preferences
            ?.edit()
            ?.putString(payloadKey(profileId), payload)
            ?.apply()
    }

    actual fun loadReleaseSupportPayload(profileId: Int, cacheKey: String): String? =
        preferences?.getString(releaseSupportPayloadKey(profileId, cacheKey), null)

    actual fun saveReleaseSupportPayload(profileId: Int, cacheKey: String, payload: String) {
        preferences
            ?.edit()
            ?.putString(releaseSupportPayloadKey(profileId, cacheKey), payload)
            ?.apply()
    }
}
