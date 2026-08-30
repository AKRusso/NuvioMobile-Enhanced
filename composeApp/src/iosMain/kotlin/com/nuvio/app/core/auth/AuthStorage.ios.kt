package com.nuvio.app.core.auth

import platform.Foundation.NSUserDefaults

actual object AuthStorage {
    private const val KEY_ANONYMOUS_USER_ID = "anonymous_user_id"
    private const val KEY_LOCAL_DATA_OWNER_USER_ID = "local_data_owner_user_id"

    actual fun loadAnonymousUserId(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_ANONYMOUS_USER_ID)

    actual fun saveAnonymousUserId(userId: String) {
        NSUserDefaults.standardUserDefaults.setObject(userId, forKey = KEY_ANONYMOUS_USER_ID)
    }

    actual fun clearAnonymousUserId() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY_ANONYMOUS_USER_ID)
    }

    actual fun loadLocalDataOwnerUserId(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_LOCAL_DATA_OWNER_USER_ID)

    actual fun saveLocalDataOwnerUserId(userId: String) {
        NSUserDefaults.standardUserDefaults.setObject(userId, forKey = KEY_LOCAL_DATA_OWNER_USER_ID)
    }

    actual fun clearLocalDataOwnerUserId() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY_LOCAL_DATA_OWNER_USER_ID)
    }
}
