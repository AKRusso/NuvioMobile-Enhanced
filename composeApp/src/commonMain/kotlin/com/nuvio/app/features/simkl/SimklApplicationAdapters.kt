package com.nuvio.app.features.simkl

import co.touchlab.kermit.Logger
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.tracking.TrackingHistoryItem
import com.nuvio.app.features.tracking.TrackingProviderId
import com.nuvio.app.features.tracking.TrackingProgressProvider
import com.nuvio.app.features.tracking.TrackingProgressSnapshot
import com.nuvio.app.features.tracking.TrackingRefreshIntent
import com.nuvio.app.features.tracking.TrackingScrobbleEvent
import com.nuvio.app.features.tracking.TrackingWatchedProvider
import com.nuvio.app.features.tracking.TrackingWatchedSnapshot
import com.nuvio.app.features.watched.WatchedItem
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object SimklWatchedSyncAdapter : TrackingWatchedProvider {
    override val providerId: TrackingProviderId = TrackingProviderId.SIMKL
    override suspend fun pull(profileId: Int, pageSize: Int): List<WatchedItem> {
        if (profileId != ProfileRepository.activeProfileId) return emptyList()
        SimklSyncRepository.refresh(
            intent = TrackingRefreshIntent.AUTOMATIC,
            origin = SimklRefreshOrigin.WATCHED_ITEMS,
        )
        val snapshot = SimklSyncRepository.state.value.snapshot
        val projection = snapshot.toSimklWatchedProjection()
        SimklWatchDiagnostics.logProjection(
            stage = "items-pull",
            snapshot = snapshot,
            projection = projection,
        )
        return projection.items
    }

    override suspend fun pullFullyWatchedSeriesKeys(profileId: Int): Set<String>? {
        // Simkl "completed" = all episodes watched; Nuvio "completed" = all
        // *available* episodes watched. Let local revalidation decide.
        return null
    }

    override suspend fun pullExtraWatchedKeys(profileId: Int): Set<String> {
        if (profileId != ProfileRepository.activeProfileId) return emptySet()
        SimklSyncRepository.refresh(
            intent = TrackingRefreshIntent.AUTOMATIC,
            origin = SimklRefreshOrigin.WATCHED_ITEMS,
        )
        val snapshot = SimklSyncRepository.state.value.snapshot
        return snapshot.animeAlternateWatchedKeys() + snapshot.movieAlternateWatchedKeys()
    }

    override fun observeExtraWatchedKeys(profileId: Int): kotlinx.coroutines.flow.Flow<Set<String>> =
        observeWatchedSnapshot(profileId)
            .map { snapshot -> snapshot.extraWatchedKeys }
            .distinctUntilChanged()

    override fun observeWatchedSnapshot(profileId: Int): Flow<TrackingWatchedSnapshot> =
        SimklSyncRepository.state
            .filter { state ->
                state.hasLoaded && profileId == ProfileRepository.activeProfileId
            }
            .map { state ->
                SimklAnimeWatchedFallback.clearOptimisticRemovals()
                val projection = state.snapshot.toSimklWatchedProjection()
                TrackingWatchedSnapshot(
                    items = projection.items,
                    fullyWatchedSeriesKeys = projection.fullyWatchedSeriesKeys,
                    extraWatchedKeys = state.snapshot.animeAlternateWatchedKeys() +
                        state.snapshot.movieAlternateWatchedKeys(),
                )
            }
            .distinctUntilChanged()

    override suspend fun push(profileId: Int, items: Collection<WatchedItem>) {
        if (profileId != ProfileRepository.activeProfileId || items.isEmpty()) return
        SimklSyncRepository.ensureLoaded()
        val snapshot = SimklSyncRepository.state.value.snapshot
        val historyItems = items.map { item ->
            TrackingHistoryItem(
                media = snapshot.mediaReference(
                    contentId = item.id,
                    contentType = item.type,
                    title = item.name,
                    releaseInfo = item.releaseInfo,
                    season = item.season,
                    episode = item.episode,
                    videoId = item.videoId,
                ),
                watchedAtEpochMs = item.markedAtEpochMs,
            )
        }
        val result = SimklMutationRepository.addToHistory(profileId = profileId, items = historyItems)
        check(result.isComplete) {
            "Simkl could not match ${result.notFoundCount} of ${result.attemptedCount} watched items"
        }
    }

    override suspend fun delete(profileId: Int, items: Collection<WatchedItem>) {
        if (profileId != ProfileRepository.activeProfileId || items.isEmpty()) return
        val episodeItems = items.filter { item -> item.season != null && item.episode != null }
        if (episodeItems.isEmpty()) return
        // Optimistically mark video IDs as removed so fallback won't show them as watched
        episodeItems.forEach { item -> item.videoId?.let(SimklAnimeWatchedFallback::markOptimisticallyRemoved) }
        SimklSyncRepository.ensureLoaded()
        val snapshot = SimklSyncRepository.state.value.snapshot
        val media = episodeItems.map { item ->
            snapshot.mediaReference(
                contentId = item.id,
                contentType = item.type,
                title = item.name,
                releaseInfo = item.releaseInfo,
                season = item.season,
                episode = item.episode,
                videoId = item.videoId,
            ).let { ref ->
                val enriched = snapshot.enrichMediaReference(ref)
                enriched.resolveAnimeEpisodeForSimkl()
            }
        }
        val result = SimklMutationRepository.removeFromHistory(profileId = profileId, items = media)
        check(result.isComplete) {
            "Simkl could not match ${result.notFoundCount} of ${result.attemptedCount} watched items"
        }
    }
}

data class SimklProgressUiState(
    val entries: List<WatchProgressEntry> = emptyList(),
    val isLoading: Boolean = false,
    val hasLoadedRemoteProgress: Boolean = false,
    val errorMessage: String? = null,
)

object SimklProgressRepository {
    private val log = Logger.withTag("SimklProgress")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(SimklProgressUiState())
    val uiState: StateFlow<SimklProgressUiState> = _uiState.asStateFlow()
    private val optimisticEntries = MutableStateFlow<Map<String, WatchProgressEntry>>(emptyMap())

    init {
        scope.launch {
            SimklSyncRepository.state.collectLatest(::publish)
        }
    }

    fun ensureLoaded() {
        SimklAuthRepository.ensureLoaded()
        SimklSyncRepository.ensureLoaded()
        publish(SimklSyncRepository.state.value)
    }

    fun onProfileChanged() {
        optimisticEntries.value = emptyMap()
        ensureLoaded()
    }

    fun clearLocalState() {
        optimisticEntries.value = emptyMap()
        _uiState.value = SimklProgressUiState()
    }

    suspend fun refresh(intent: TrackingRefreshIntent) {
        SimklSyncRepository.refresh(
            intent = intent,
            origin = SimklRefreshOrigin.PROGRESS,
        )
        publish(SimklSyncRepository.state.value)
    }

    suspend fun removeProgress(entries: Collection<WatchProgressEntry>) {
        applyOptimisticRemoval(entries)
        val sessionIds = entries.mapNotNullTo(linkedSetOf()) { entry ->
            entry.progressKey
                ?.removePrefix(SIMKL_PLAYBACK_PROGRESS_KEY_PREFIX)
                ?.takeIf { entry.progressKey.startsWith(SIMKL_PLAYBACK_PROGRESS_KEY_PREFIX) }
                ?.toLongOrNull()
                ?.takeIf { it > 0L }
        }
        if (sessionIds.isEmpty()) return

        val removed = linkedSetOf<Long>()
        for (sessionId in sessionIds) {
            try {
                SimklApi.client.execute(
                    SimklApiRequest(
                        method = SimklHttpMethod.DELETE,
                        path = "/sync/playback/$sessionId",
                    ),
                )
                removed += sessionId
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val apiError = error as? SimklApiException
                log.w {
                    "Failed to remove Simkl playback: status=${apiError?.status} " +
                        "code=${apiError?.errorCode ?: "transport_failure"}"
                }
            }
        }
        SimklSyncRepository.commitPlaybackRemoval(removed)
    }

    fun applyOptimisticRemoval(entries: Collection<WatchProgressEntry>) {
        val keys = entries.mapTo(mutableSetOf(), WatchProgressEntry::simklProgressIdentityKey)
        if (keys.isEmpty()) return
        optimisticEntries.update { current -> current.filterKeys { key -> key !in keys } }
        publish(SimklSyncRepository.state.value)
    }

    fun applyOptimisticProgress(entry: WatchProgressEntry) {
        val key = entry.simklProgressIdentityKey()
        optimisticEntries.update { current ->
            val existing = current[key]
            if (existing != null && existing.lastUpdatedEpochMs > entry.lastUpdatedEpochMs) {
                current
            } else {
                current + (key to entry)
            }
        }
        publish(SimklSyncRepository.state.value)
    }

    internal fun reconcileTerminalScrobble(result: SimklScrobbleResult, event: TrackingScrobbleEvent) {
        val contentIds = buildSet {
            addAll(result.media.allContentIdsForOptimisticReconciliation())
            event.media.catalog?.contentId?.trim()?.takeIf(String::isNotBlank)?.let(::add)
        }
        val episodeCoordinates = buildSet {
            result.episode?.let { episode -> add(episode.season to episode.number) }
            event.media.episode?.let { episode -> add(episode.season to episode.number) }
        }
        optimisticEntries.update { current ->
            current.filterValues { entry ->
                val sameContent = entry.parentMetaId in contentIds
                val sameEpisode = episodeCoordinates.isEmpty() ||
                    (entry.seasonNumber to entry.episodeNumber) in episodeCoordinates
                !sameContent || !sameEpisode
            }
        }
        publish(SimklSyncRepository.state.value)
    }

    private fun publish(syncState: SimklSyncUiState) {
        _uiState.value = SimklProgressUiState(
            entries = mergeSimklOptimisticProgress(
                providerEntries = syncState.snapshot.toSimklProgressEntries(),
                optimisticEntries = optimisticEntries.value.values,
            ),
            isLoading = syncState.isLoading,
            hasLoadedRemoteProgress = syncState.hasLoaded && syncState.errorMessage == null,
            errorMessage = syncState.errorMessage,
        )
    }
}

object SimklTrackingProgressProvider : TrackingProgressProvider {
    override val providerId: TrackingProviderId = TrackingProviderId.SIMKL
    override val changes: Flow<Unit> = SimklProgressRepository.uiState.map { Unit }

    override fun ensureLoaded() = SimklProgressRepository.ensureLoaded()

    override fun onProfileChanged() = SimklProgressRepository.onProfileChanged()

    override fun clearLocalState() = SimklProgressRepository.clearLocalState()

    override suspend fun refresh(force: Boolean, sourceChanged: Boolean) =
        SimklProgressRepository.refresh(resolveSimklProgressRefreshIntent(force, sourceChanged))

    override fun snapshot(): TrackingProgressSnapshot {
        val state = SimklProgressRepository.uiState.value
        return TrackingProgressSnapshot(
            entries = state.entries,
            hiddenContentIds = SimklSyncRepository.state.value.snapshot
                .hiddenFromContinueWatchingContentIds(),
            hasLoadedRemoteProgress = state.hasLoadedRemoteProgress,
            errorMessage = state.errorMessage,
        )
    }

    override suspend fun removeProgress(entries: Collection<WatchProgressEntry>) =
        SimklProgressRepository.removeProgress(entries)

    override fun applyOptimisticRemoval(entries: Collection<WatchProgressEntry>) =
        SimklProgressRepository.applyOptimisticRemoval(entries)

    override fun applyOptimisticProgress(entry: WatchProgressEntry) =
        SimklProgressRepository.applyOptimisticProgress(entry)

    override fun isHiddenFromProgress(contentId: String): Boolean =
        SimklSyncRepository.state.value.snapshot.isHiddenFromContinueWatching(contentId)

    override fun normalizeParentContentId(parentContentId: String, videoId: String?): String {
        val snapshot = SimklSyncRepository.state.value.snapshot
        val resolvedId = snapshot.resolveCanonicalContentId(parentContentId)
        return resolvedId ?: parentContentId
    }

    override suspend fun refreshEpisodeProgress(contentId: String, forceRefresh: Boolean) =
        SimklProgressRepository.refresh(
            resolveSimklProgressRefreshIntent(force = forceRefresh, sourceChanged = false),
        )
}

internal fun mergeSimklOptimisticProgress(
    providerEntries: Collection<WatchProgressEntry>,
    optimisticEntries: Collection<WatchProgressEntry>,
): List<WatchProgressEntry> = (providerEntries + optimisticEntries)
    .groupBy(WatchProgressEntry::simklProgressIdentityKey)
    .mapNotNull { (_, candidates) -> candidates.maxByOrNull(WatchProgressEntry::lastUpdatedEpochMs) }
    .sortedByDescending(WatchProgressEntry::lastUpdatedEpochMs)

private fun WatchProgressEntry.simklProgressIdentityKey(): String = if (seasonNumber != null && episodeNumber != null) {
    "${parentMetaType.simklProgressTypeKey()}:${parentMetaId.trim()}:$seasonNumber:$episodeNumber"
} else {
    "${parentMetaType.simklProgressTypeKey()}:${parentMetaId.trim()}:${videoId.trim()}"
}

private fun String.simklProgressTypeKey(): String = when (trim().lowercase()) {
    "series", "show", "tv", "tvshow" -> "series"
    "movie", "film" -> "movie"
    else -> trim().lowercase()
}

private fun SimklMedia.allContentIdsForOptimisticReconciliation(): Set<String> = buildSet {
    ids.idValue("imdb")?.let(::add)
    listOf("tmdb", "tvdb", "mal", "anidb", "anilist", "kitsu").forEach { type ->
        ids.idValue(type)?.let { id -> add("$type:$id") }
    }
    ids.simklIdValue()?.let { id -> add("simkl:$id") }
}

private const val SIMKL_PLAYBACK_PROGRESS_KEY_PREFIX = "simkl-playback:"
