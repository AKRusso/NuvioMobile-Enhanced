package com.nuvio.app.features.search

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SearchHistoryRepository {
    private const val MAX_RECENT_SEARCHES = 10

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _uiState = MutableStateFlow<List<String>>(emptyList())
    val uiState: StateFlow<List<String>> = _uiState.asStateFlow()

    private var hasLoaded = false
    private var recentSearches: List<String> = emptyList()

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun recordSearch(query: String) {
        ensureLoaded()
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < 2) return

        val updatedSearches = applySearchHistoryEntry(
            current = recentSearches,
            query = normalizedQuery,
            limit = MAX_RECENT_SEARCHES,
        )
        if (updatedSearches == recentSearches) return

        recentSearches = updatedSearches
        publish()
        persist()
    }

    fun removeSearch(query: String) {
        ensureLoaded()
        val updatedSearches = recentSearches.filterNot { it == query }
        if (updatedSearches == recentSearches) return

        recentSearches = updatedSearches
        publish()
        persist()
    }

    fun clearAll() {
        ensureLoaded()
        if (recentSearches.isEmpty()) return

        recentSearches = emptyList()
        publish()
        persist()
    }

    private fun loadFromDisk() {
        hasLoaded = true
        val payload = SearchHistoryStorage.loadPayload().orEmpty().trim()
        recentSearches = if (payload.isEmpty()) {
            emptyList()
        } else {
            runCatching {
                json.decodeFromString<List<String>>(payload)
            }.getOrDefault(emptyList())
                .map { it.trim() }
                .filter { it.length >= 2 }
                .distinct()
                .take(MAX_RECENT_SEARCHES)
        }
        publish()
    }

    private fun publish() {
        _uiState.value = recentSearches
    }

    private fun persist() {
        SearchHistoryStorage.savePayload(json.encodeToString(recentSearches))
    }
}

internal fun applySearchHistoryEntry(
    current: List<String>,
    query: String,
    limit: Int,
): List<String> =
    buildList {
        add(query)
        current.forEach { existing ->
            if (existing != query && size < limit) {
                add(existing)
            }
        }
    }
