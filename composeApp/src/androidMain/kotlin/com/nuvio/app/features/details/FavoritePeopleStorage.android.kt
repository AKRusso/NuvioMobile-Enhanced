package com.nuvio.app.features.details

import android.content.Context
import android.content.SharedPreferences

internal actual object FavoritePeopleStorage {
    private const val preferencesName = "nuvio_favorite_people"
    private fun payloadKey(profileId: Int) = "favorite_people_payload_$profileId"

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
}

internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()
