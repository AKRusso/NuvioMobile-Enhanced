package com.nuvio.app.features.library

internal expect object LibraryStorage {
    fun loadPayload(profileId: Int): String?
    fun savePayload(profileId: Int, payload: String)
    fun loadReleaseSupportPayload(profileId: Int, cacheKey: String): String?
    fun saveReleaseSupportPayload(profileId: Int, cacheKey: String, payload: String)
}
