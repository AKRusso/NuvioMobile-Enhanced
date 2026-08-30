package com.nuvio.app.core.auth

internal expect object AuthStorage {
    fun loadAnonymousUserId(): String?
    fun saveAnonymousUserId(userId: String)
    fun clearAnonymousUserId()
    fun loadLocalDataOwnerUserId(): String?
    fun saveLocalDataOwnerUserId(userId: String)
    fun clearLocalDataOwnerUserId()
}
