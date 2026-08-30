package com.nuvio.app.features.anime

import com.nuvio.app.features.tracking.TrackingProfileStore
import com.nuvio.app.features.tracking.TrackingProviderId
import com.nuvio.app.features.tracking.TrackingProviderRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AnimeTrackingProviderPreferences(
    val automaticScrobble: Boolean = true,
    val explicitWatchedSync: Boolean = true,
)

@Serializable
data class AnimeTrackingSettings(
    val aniList: AnimeTrackingProviderPreferences = AnimeTrackingProviderPreferences(),
    val myAnimeList: AnimeTrackingProviderPreferences = AnimeTrackingProviderPreferences(),
    val mappings: Map<String, AnimeTrackingManualMapping> = emptyMap(),
) {
    fun forProvider(provider: AnimeTrackingProvider): AnimeTrackingProviderPreferences = when (provider) {
        AnimeTrackingProvider.ANILIST -> aniList
        AnimeTrackingProvider.MY_ANIME_LIST -> myAnimeList
    }
}

@Serializable
data class AnimeTrackingManualMapping(
    val aniListId: Int? = null,
    val aniListAutomaticResolutionDisabled: Boolean = false,
    val myAnimeListId: Int? = null,
    val myAnimeListAutomaticResolutionDisabled: Boolean = false,
    val simklId: Long? = null,
    val simklAutomaticResolutionDisabled: Boolean = false,
) {
    fun id(provider: AnimeTrackingProvider): Int? = when (provider) {
        AnimeTrackingProvider.ANILIST -> aniListId
        AnimeTrackingProvider.MY_ANIME_LIST -> myAnimeListId
    }

    fun automaticResolutionDisabled(provider: AnimeTrackingProvider): Boolean = when (provider) {
        AnimeTrackingProvider.ANILIST -> aniListAutomaticResolutionDisabled
        AnimeTrackingProvider.MY_ANIME_LIST -> myAnimeListAutomaticResolutionDisabled
    }
}

object AnimeTrackingSettingsRepository : TrackingProfileStore {
    override val providerId: TrackingProviderId = TrackingProviderId.ANILIST
    private val json = Json { ignoreUnknownKeys = true }
    private val _state = MutableStateFlow(AnimeTrackingSettings())
    val state: StateFlow<AnimeTrackingSettings> = _state.asStateFlow()
    private var loaded = false

    init {
        TrackingProviderRegistry.registerProfileStore(this)
    }

    fun ensureLoaded() {
        if (loaded) return
        loaded = true
        _state.value = AnimeTrackingAuthStorage.loadSettings()
            ?.let { payload -> runCatching { json.decodeFromString<AnimeTrackingSettings>(payload) }.getOrNull() }
            ?: AnimeTrackingSettings()
    }

    fun preferences(provider: AnimeTrackingProvider): AnimeTrackingProviderPreferences {
        ensureLoaded()
        return _state.value.forProvider(provider)
    }

    fun setAutomaticScrobble(provider: AnimeTrackingProvider, enabled: Boolean) {
        update(provider) { it.copy(automaticScrobble = enabled) }
    }

    fun setExplicitWatchedSync(provider: AnimeTrackingProvider, enabled: Boolean) {
        update(provider) { it.copy(explicitWatchedSync = enabled) }
    }

    fun mapping(
        provider: AnimeTrackingProvider,
        contentType: String,
        contentId: String,
        season: Int?,
        episode: Int?,
    ): AnimeTrackingManualMapping? {
        ensureLoaded()
        return mappingKeys(contentType, contentId, season, episode)
            .mapNotNull { key -> _state.value.mappings[key] }
            .firstOrNull { mapping ->
                mapping.id(provider) != null || mapping.automaticResolutionDisabled(provider)
            }
    }

    fun setMapping(
        provider: AnimeTrackingProvider,
        contentType: String,
        contentId: String,
        season: Int?,
        episode: Int?,
        providerMediaId: Int?,
        disableAutomaticResolution: Boolean = providerMediaId == null,
    ) {
        ensureLoaded()
        val key = mappingKey(contentType, contentId, season, episode)
        val current = _state.value
        val existing = current.mappings[key] ?: AnimeTrackingManualMapping()
        val mapping = when (provider) {
            AnimeTrackingProvider.ANILIST -> existing.copy(
                aniListId = providerMediaId,
                aniListAutomaticResolutionDisabled = disableAutomaticResolution,
            )
            AnimeTrackingProvider.MY_ANIME_LIST -> existing.copy(
                myAnimeListId = providerMediaId,
                myAnimeListAutomaticResolutionDisabled = disableAutomaticResolution,
            )
        }
        val updatedMappings = current.mappings.toMutableMap().apply {
            if (mapping == AnimeTrackingManualMapping()) remove(key) else put(key, mapping)
        }
        persist(current.copy(mappings = updatedMappings))
    }

    fun simklMapping(
        contentType: String,
        contentId: String,
        season: Int?,
        episode: Int?,
    ): Pair<Long?, Boolean>? {
        ensureLoaded()
        return mappingKeys(contentType, contentId, season, episode)
            .mapNotNull { key -> _state.value.mappings[key] }
            .firstOrNull { mapping -> mapping.simklId != null || mapping.simklAutomaticResolutionDisabled }
            ?.let { mapping -> mapping.simklId to mapping.simklAutomaticResolutionDisabled }
    }

    fun setSimklMapping(
        contentType: String,
        contentId: String,
        season: Int?,
        episode: Int?,
        simklId: Long?,
        disableAutomaticResolution: Boolean = simklId == null,
    ) {
        ensureLoaded()
        val key = mappingKey(contentType, contentId, season, episode)
        val current = _state.value
        val mapping = (current.mappings[key] ?: AnimeTrackingManualMapping()).copy(
            simklId = simklId,
            simklAutomaticResolutionDisabled = disableAutomaticResolution,
        )
        val updatedMappings = current.mappings.toMutableMap().apply {
            if (mapping == AnimeTrackingManualMapping()) remove(key) else put(key, mapping)
        }
        persist(current.copy(mappings = updatedMappings))
    }

    override fun onProfileChanged() {
        loaded = false
        ensureLoaded()
    }

    override fun clearLocalState() {
        AnimeTrackingAuthStorage.saveSettings(null)
        loaded = false
        _state.value = AnimeTrackingSettings()
    }

    override fun removeStoredProfile(profileId: Int) {
        AnimeTrackingAuthStorage.removeProfile(profileId)
    }

    private fun update(
        provider: AnimeTrackingProvider,
        transform: (AnimeTrackingProviderPreferences) -> AnimeTrackingProviderPreferences,
    ) {
        ensureLoaded()
        val current = _state.value
        val updated = when (provider) {
            AnimeTrackingProvider.ANILIST -> current.copy(aniList = transform(current.aniList))
            AnimeTrackingProvider.MY_ANIME_LIST -> current.copy(myAnimeList = transform(current.myAnimeList))
        }
        persist(updated)
    }

    private fun persist(settings: AnimeTrackingSettings) {
        _state.value = settings
        AnimeTrackingAuthStorage.saveSettings(json.encodeToString(settings))
    }

    private fun mappingKeys(
        contentType: String,
        contentId: String,
        season: Int?,
        episode: Int?,
    ): List<String> = buildList {
        if (season != null && episode != null) add(mappingKey(contentType, contentId, season, episode))
        if (season != null) add(mappingKey(contentType, contentId, season, null))
        add(mappingKey(contentType, contentId, null, null))
    }

    private fun mappingKey(
        contentType: String,
        contentId: String,
        season: Int?,
        episode: Int?,
    ): String = listOf(
        contentType.trim().lowercase(),
        contentId.trim(),
        season?.toString().orEmpty(),
        episode?.toString().orEmpty(),
    ).joinToString("|")
}
