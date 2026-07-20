package com.nuvio.app.features.tmdb

internal expect object TmdbPreviewArtworkStorage {
    fun load(cacheKey: String): String?
    fun save(cacheKey: String, payload: String)
}
