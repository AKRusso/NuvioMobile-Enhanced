package com.nuvio.app.core.auth

import android.content.Context
import android.content.SharedPreferences

actual object AuthStorage {
    private const val PREFS_NAME = "nuvio_auth"
    private const val KEY_ANONYMOUS_USER_ID = "anonymous_user_id"
    private const val KEY_LOCAL_DATA_OWNER_USER_ID = "local_data_owner_user_id"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun loadAnonymousUserId(): String? =
        preferences?.getString(KEY_ANONYMOUS_USER_ID, null)

    actual fun saveAnonymousUserId(userId: String) {
        preferences?.edit()?.putString(KEY_ANONYMOUS_USER_ID, userId)?.apply()
    }

    actual fun clearAnonymousUserId() {
        preferences?.edit()?.remove(KEY_ANONYMOUS_USER_ID)?.apply()
    }

    actual fun loadLocalDataOwnerUserId(): String? =
        preferences?.getString(KEY_LOCAL_DATA_OWNER_USER_ID, null)

    actual fun saveLocalDataOwnerUserId(userId: String) {
        preferences?.edit()?.putString(KEY_LOCAL_DATA_OWNER_USER_ID, userId)?.apply()
    }

    actual fun clearLocalDataOwnerUserId() {
        preferences?.edit()?.remove(KEY_LOCAL_DATA_OWNER_USER_ID)?.apply()
    }
}
