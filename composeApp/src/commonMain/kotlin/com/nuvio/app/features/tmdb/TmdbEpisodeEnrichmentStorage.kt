package com.nuvio.app.features.tmdb

internal expect object TmdbEpisodeEnrichmentStorage {
    fun load(cacheKey: String): String?
    fun save(cacheKey: String, payload: String)
}
