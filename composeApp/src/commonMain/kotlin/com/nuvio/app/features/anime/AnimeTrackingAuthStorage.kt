package com.nuvio.app.features.anime

internal expect object AnimeTrackingAuthStorage {
    fun loadMetadata(provider: AnimeTrackingProvider): String?
    fun saveMetadata(provider: AnimeTrackingProvider, payload: String?)
    fun loadSettings(): String?
    fun saveSettings(payload: String?)
    fun loadSecret(provider: AnimeTrackingProvider, key: String): String?
    fun saveSecret(provider: AnimeTrackingProvider, key: String, value: String?)
    fun removeProfile(profileId: Int)
}
