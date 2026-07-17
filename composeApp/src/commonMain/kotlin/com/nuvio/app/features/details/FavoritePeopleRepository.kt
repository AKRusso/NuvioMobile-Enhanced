package com.nuvio.app.features.details

import com.nuvio.app.features.profiles.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class FavoritePerson(
    val tmdbId: Int,
    val name: String,
    val photo: String? = null,
    val knownFor: String? = null,
    val addedAtEpochMs: Long,
)

data class FavoritePeopleUiState(
    val people: List<FavoritePerson> = emptyList(),
    val pinnedPersonIds: List<Int> = emptyList(),
    val isLoaded: Boolean = false,
) {
    val displayPeople: List<FavoritePerson>
        get() {
            if (people.isEmpty() || pinnedPersonIds.isEmpty()) return people
            val peopleById = people.associateBy(FavoritePerson::tmdbId)
            val pinned = pinnedPersonIds.mapNotNull(peopleById::get)
            val pinnedIds = pinned.mapTo(mutableSetOf(), FavoritePerson::tmdbId)
            return pinned + people.filterNot { person -> person.tmdbId in pinnedIds }
        }
}

@Serializable
private data class StoredFavoritePeoplePayload(
    val people: List<FavoritePerson> = emptyList(),
    val pinnedPersonIds: List<Int> = emptyList(),
)

object FavoritePeopleRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _uiState = MutableStateFlow(FavoritePeopleUiState())
    val uiState: StateFlow<FavoritePeopleUiState> = _uiState.asStateFlow()

    private var currentProfileId: Int = ProfileRepository.activeProfileId
    private var hasLoaded = false
    private var peopleById = linkedMapOf<Int, FavoritePerson>()
    private var pinnedPersonIds = emptyList<Int>()

    fun ensureLoaded() {
        val activeProfileId = ProfileRepository.activeProfileId
        if (hasLoaded && currentProfileId == activeProfileId) return
        loadFromDisk(activeProfileId)
    }

    fun onProfileChanged(profileId: Int) {
        loadFromDisk(profileId)
    }

    fun isFavorite(tmdbId: Int): Boolean {
        ensureLoaded()
        return peopleById.containsKey(tmdbId)
    }

    fun toggle(person: PersonDetail) {
        ensureLoaded()
        if (peopleById.remove(person.tmdbId) == null) {
            peopleById[person.tmdbId] = person.toFavoritePerson()
        } else {
            pinnedPersonIds = pinnedPersonIds.filterNot { id -> id == person.tmdbId }
        }
        publish()
        persist()
    }

    fun remove(tmdbId: Int) {
        ensureLoaded()
        if (peopleById.remove(tmdbId) == null) return
        pinnedPersonIds = pinnedPersonIds.filterNot { id -> id == tmdbId }
        publish()
        persist()
    }

    fun togglePinned(tmdbId: Int) {
        ensureLoaded()
        if (!peopleById.containsKey(tmdbId)) return
        pinnedPersonIds = if (tmdbId in pinnedPersonIds) {
            pinnedPersonIds.filterNot { id -> id == tmdbId }
        } else {
            listOf(tmdbId) + pinnedPersonIds
        }.distinct().filter(peopleById::containsKey)
        publish()
        persist()
    }

    private fun loadFromDisk(profileId: Int) {
        currentProfileId = profileId
        hasLoaded = true
        peopleById.clear()
        pinnedPersonIds = emptyList()

        val payload = FavoritePeopleStorage.loadPayload(profileId).orEmpty().trim()
        if (payload.isNotEmpty()) {
            val decoded = runCatching {
                json.decodeFromString<StoredFavoritePeoplePayload>(payload)
            }.getOrDefault(StoredFavoritePeoplePayload())

            decoded.people
                .filter { it.tmdbId > 0 && it.name.isNotBlank() }
                .sortedByDescending(FavoritePerson::addedAtEpochMs)
                .forEach { person ->
                    peopleById[person.tmdbId] = person
                }
            pinnedPersonIds = decoded.pinnedPersonIds
                .distinct()
                .filter(peopleById::containsKey)
        }

        publish()
    }

    private fun persist() {
        val payload = StoredFavoritePeoplePayload(
            people = peopleById.values.sortedByDescending(FavoritePerson::addedAtEpochMs),
            pinnedPersonIds = pinnedPersonIds.filter(peopleById::containsKey),
        )
        FavoritePeopleStorage.savePayload(currentProfileId, json.encodeToString(payload))
    }

    private fun publish() {
        val sortedPeople = peopleById.values.sortedByDescending(FavoritePerson::addedAtEpochMs)
        pinnedPersonIds = pinnedPersonIds.distinct().filter(peopleById::containsKey)
        _uiState.value = FavoritePeopleUiState(
            people = sortedPeople,
            pinnedPersonIds = pinnedPersonIds,
            isLoaded = hasLoaded,
        )
    }
}

private fun PersonDetail.toFavoritePerson(): FavoritePerson =
    FavoritePerson(
        tmdbId = tmdbId,
        name = name.trim(),
        photo = profilePhoto?.trim()?.takeIf { it.isNotBlank() },
        knownFor = knownFor?.trim()?.takeIf { it.isNotBlank() },
        addedAtEpochMs = currentTimeMillis(),
    )

internal expect fun currentTimeMillis(): Long
