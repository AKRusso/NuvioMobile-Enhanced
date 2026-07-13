package com.nuvio.app.features.streams

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class StreamSourcePreferencesUiState(
    val pinnedSources: List<PinnedStreamSource> = emptyList(),
)

@Serializable
data class PinnedStreamSource(
    val id: String,
    val name: String,
)

object StreamSourcePreferencesRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val _uiState = MutableStateFlow(StreamSourcePreferencesUiState())

    val uiState: StateFlow<StreamSourcePreferencesUiState> = _uiState.asStateFlow()

    private var hasLoaded = false
    private var pinnedSources: List<PinnedStreamSource> = emptyList()

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        hasLoaded = false
        loadFromDisk()
    }

    fun clearLocalState() {
        hasLoaded = false
        pinnedSources = emptyList()
        _uiState.value = StreamSourcePreferencesUiState()
        StreamSourcePreferencesStorage.clearPinnedSources()
    }

    fun pinSource(sourceId: String, sourceName: String) {
        ensureLoaded()
        val normalizedId = sourceId.trim()
        val normalizedName = sourceName.trim()
        if (normalizedId.isBlank() || normalizedName.isBlank()) return
        if (pinnedSources.any { it.id == normalizedId }) return

        pinnedSources = (pinnedSources + PinnedStreamSource(normalizedId, normalizedName)).normalized()
        publish()
        savePinnedSources()
    }

    fun unpinSource(sourceId: String) {
        ensureLoaded()
        val normalizedId = sourceId.trim()
        if (normalizedId.isBlank()) return
        val updated = pinnedSources.filterNot { it.id == normalizedId }
        if (updated.size == pinnedSources.size) return

        pinnedSources = updated
        publish()
        savePinnedSources()
    }

    fun clearPinnedSources() {
        ensureLoaded()
        if (pinnedSources.isEmpty()) return
        pinnedSources = emptyList()
        publish()
        StreamSourcePreferencesStorage.clearPinnedSources()
    }

    private fun loadFromDisk() {
        hasLoaded = true
        val payload = StreamSourcePreferencesStorage.loadPinnedSourcesPayload()
        pinnedSources = parsePinnedSources(payload)
            .ifEmpty { legacyPinnedSource() }
            .normalized()
        if (pinnedSources.isNotEmpty() && payload.isNullOrBlank()) {
            savePinnedSources()
        }
        publish()
    }

    private fun publish() {
        _uiState.value = StreamSourcePreferencesUiState(pinnedSources = pinnedSources)
    }

    private fun savePinnedSources() {
        if (pinnedSources.isEmpty()) {
            StreamSourcePreferencesStorage.clearPinnedSources()
        } else {
            StreamSourcePreferencesStorage.savePinnedSourcesPayload(json.encodeToString(pinnedSources))
        }
    }

    private fun parsePinnedSources(payload: String?): List<PinnedStreamSource> {
        if (payload.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<PinnedStreamSource>>(payload)
        } catch (_: SerializationException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }

    private fun legacyPinnedSource(): List<PinnedStreamSource> {
        val id = StreamSourcePreferencesStorage.loadLegacyPinnedSourceId()?.trim()?.takeIf { it.isNotBlank() }
            ?: return emptyList()
        val name = StreamSourcePreferencesStorage.loadLegacyPinnedSourceName()?.trim()?.takeIf { it.isNotBlank() }
            ?: return emptyList()
        return listOf(PinnedStreamSource(id = id, name = name))
    }

    private fun List<PinnedStreamSource>.normalized(): List<PinnedStreamSource> {
        val seen = mutableSetOf<String>()
        return mapNotNull { source ->
            val id = source.id.trim()
            val name = source.name.trim()
            if (id.isBlank() || name.isBlank() || !seen.add(id)) {
                null
            } else {
                PinnedStreamSource(id = id, name = name)
            }
        }
    }
}
