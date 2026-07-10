package com.nuvio.app.features.streams

internal expect object StreamSourcePreferencesStorage {
    fun loadPinnedSourcesPayload(): String?
    fun savePinnedSourcesPayload(payload: String)
    fun loadLegacyPinnedSourceId(): String?
    fun loadLegacyPinnedSourceName(): String?
    fun clearPinnedSources()
}
