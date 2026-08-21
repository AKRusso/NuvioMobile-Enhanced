package com.nuvio.app.features.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.network.NetworkCondition
import com.nuvio.app.core.network.NetworkStatusRepository
import com.nuvio.app.core.ui.LocalNuvioBottomNavigationOverlayPadding
import com.nuvio.app.core.ui.LocalTvLayoutProfile
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioNetworkOfflineCard
import com.nuvio.app.core.ui.nuvioSafeBottomPadding
import com.nuvio.app.core.ui.rememberHeroStretchState
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.addons.enabledAddons
import com.nuvio.app.features.cloud.CloudLibraryContentType
import com.nuvio.app.features.cloud.CloudLibraryRepository
import com.nuvio.app.features.cloud.CloudLibraryUiState
import com.nuvio.app.features.cloud.findPlaybackTargetForProgress
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.details.SeriesPrimaryAction
import com.nuvio.app.features.details.seriesPrimaryAction
import com.nuvio.app.features.home.components.HomeCatalogRowSection
import com.nuvio.app.features.home.components.HomeContinueWatchingSection
import com.nuvio.app.features.home.components.HomeEmptyStateCard
import com.nuvio.app.features.home.components.HomeHeroReservedSpace
import com.nuvio.app.features.home.components.HomeHeroSection
import com.nuvio.app.features.home.components.HomeSmartShelfComposerSection
import com.nuvio.app.features.home.components.HomeSkeletonHero
import com.nuvio.app.features.home.components.HomeSkeletonRow
import com.nuvio.app.features.tmdb.TmdbSettingsRepository
import com.nuvio.app.features.tracking.TrackingSettingsRepository
import com.nuvio.app.features.tracking.WatchProgressSource
import com.nuvio.app.features.watched.WatchedItem
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.watched.episodePlaybackId
import com.nuvio.app.features.watched.watchedItemKey
import com.nuvio.app.features.watchprogress.CachedInProgressItem
import com.nuvio.app.features.watchprogress.CachedNextUpItem
import com.nuvio.app.features.watchprogress.ContinueWatchingEnrichmentCache
import com.nuvio.app.features.watchprogress.CurrentDateProvider
import com.nuvio.app.features.watchprogress.ContinueWatchingPreferencesRepository
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import com.nuvio.app.features.watchprogress.ContinueWatchingSortMode
import com.nuvio.app.features.watchprogress.isMalformedNextUpSeedContentId
import com.nuvio.app.features.watchprogress.isSeriesTypeForContinueWatching
import com.nuvio.app.features.watchprogress.nextUpDismissKey
import com.nuvio.app.features.watchprogress.parseReleaseDateToEpochMs
import com.nuvio.app.features.watchprogress.resolvedProgressKey
import com.nuvio.app.features.watchprogress.shouldTreatAsInProgressForContinueWatching
import com.nuvio.app.features.watchprogress.shouldUseAsCompletedSeedForContinueWatching
import com.nuvio.app.features.watchprogress.WatchProgressClock
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import com.nuvio.app.features.watchprogress.WatchProgressSourceCoordinator
import com.nuvio.app.features.watchprogress.buildContinueWatchingEpisodeSubtitle
import com.nuvio.app.features.watchprogress.continueWatchingEntries
import com.nuvio.app.features.watchprogress.toContinueWatchingItem
import com.nuvio.app.features.watchprogress.toUpNextContinueWatchingItem
import com.nuvio.app.features.watching.application.WatchingState
import com.nuvio.app.features.watching.domain.WatchingContentRef
import com.nuvio.app.features.watching.domain.isReleasedBy
import com.nuvio.app.features.collection.CollectionRepository
import com.nuvio.app.features.cloudstream.CloudStreamRepository
import com.nuvio.app.features.library.LibraryRepository
import com.nuvio.app.features.library.toLibraryItem
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.settings.NuvioEnhancedSettingsRepository
import com.nuvio.app.features.home.components.HomeCollectionRowSection
import com.nuvio.app.features.watchprogress.ContinueWatchingSectionStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.yield
import com.nuvio.app.features.home.components.ContinueWatchingLayout
import com.nuvio.app.features.home.components.ContinueWatchingDataSourceKey
import com.nuvio.app.features.home.components.continueWatchingLandscapeCardHeight
import com.nuvio.app.features.home.components.homeSectionHorizontalPaddingForWidth
import com.nuvio.app.features.home.components.rememberContinueWatchingLayout
import kotlinx.coroutines.CancellationException
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    animateCollectionGifs: Boolean = true,
    scrollToTopRequests: Flow<Unit> = emptyFlow(),
    onCatalogClick: ((HomeCatalogSection) -> Unit)? = null,
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
    onContinueWatchingClick: ((ContinueWatchingItem) -> Unit)? = null,
    onContinueWatchingLongPress: ((ContinueWatchingItem) -> Unit)? = null,
    onFolderClick: ((collectionId: String, folderId: String) -> Unit)? = null,
    onFirstCatalogRendered: (() -> Unit)? = null,
) {
    LaunchedEffect(Unit) {
        AddonRepository.initialize()
        CloudStreamRepository.initialize()
        CollectionRepository.initialize()
        ContinueWatchingPreferencesRepository.ensureLoaded()
        WatchedRepository.ensureLoaded()
        WatchProgressRepository.ensureLoaded()
    }

    LaunchedEffect(Unit) {
        WatchProgressSourceCoordinator.ensureStarted()
    }

    val addonsUiState by AddonRepository.uiState.collectAsStateWithLifecycle()
    val cloudStreamUiState by CloudStreamRepository.uiState.collectAsStateWithLifecycle()
    val homeUiState by HomeRepository.uiState.collectAsStateWithLifecycle()
    val homeSettingsUiState by remember {
        HomeCatalogSettingsRepository.snapshot()
        HomeCatalogSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val nuvioEnhancedSettings by remember {
        NuvioEnhancedSettingsRepository.ensureLoaded()
        NuvioEnhancedSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val libraryUiState by remember {
        LibraryRepository.ensureLoaded()
        LibraryRepository.uiState
    }.collectAsStateWithLifecycle()
    val homeListState = rememberLazyListState()
    val continueWatchingListState = rememberLazyListState()
    val upcomingListState = rememberLazyListState()
    val collections by CollectionRepository.collections.collectAsStateWithLifecycle()
    val continueWatchingPreferences by ContinueWatchingPreferencesRepository.uiState.collectAsStateWithLifecycle()
    val watchedUiState by WatchedRepository.uiState.collectAsStateWithLifecycle()
    val fullyWatchedSeriesKeys by WatchedRepository.fullyWatchedSeriesKeys.collectAsStateWithLifecycle()
    val watchProgressUiState by WatchProgressRepository.uiState.collectAsStateWithLifecycle()
    val effectiveWatchProgressSource = watchProgressUiState.source
    val cloudLibraryUiState by CloudLibraryRepository.uiState.collectAsStateWithLifecycle()
    val tmdbSettingsUiState by remember {
        TmdbSettingsRepository.ensureLoaded()
        TmdbSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val networkStatusUiState by NetworkStatusRepository.uiState.collectAsStateWithLifecycle()
    val trackingSettingsUiState by remember {
        TrackingSettingsRepository.ensureLoaded()
        TrackingSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    var observedOfflineState by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var manualRefreshRequested by remember { mutableStateOf(false) }

    LaunchedEffect(scrollToTopRequests) {
        scrollToTopRequests.collect {
            homeListState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(networkStatusUiState.condition) {
        when (networkStatusUiState.condition) {
            NetworkCondition.NoInternet,
            NetworkCondition.ServersUnreachable,
            -> {
                observedOfflineState = true
            }

            NetworkCondition.Online -> {
                if (observedOfflineState) {
                    observedOfflineState = false
                    HomeRepository.refresh(addonsUiState.addons.enabledAddons(), force = true)
                }
            }

            NetworkCondition.Unknown,
            NetworkCondition.Checking,
            -> Unit
        }
    }

    val progressProviderOwnsCompletedHistory = remember(effectiveWatchProgressSource) {
        WatchProgressRepository.activeProviderOwnsCompletedHistoryProjection()
    }
    val continueWatchingCutoffEpochMs = remember(
        effectiveWatchProgressSource,
        trackingSettingsUiState.continueWatchingDaysCap,
    ) {
        WatchProgressRepository.activeProviderContinueWatchingCutoffEpochMs(
            daysCap = trackingSettingsUiState.continueWatchingDaysCap,
            nowEpochMs = WatchProgressClock.nowEpochMs(),
        )
    }
    val nextUpWatchedItems = remember(watchedUiState.items, progressProviderOwnsCompletedHistory) {
        if (progressProviderOwnsCompletedHistory) emptyList() else watchedUiState.items
    }

    val effectiveWatchProgressEntries = remember(
        watchProgressUiState.entries,
        watchProgressUiState.hiddenContentIds,
        continueWatchingCutoffEpochMs,
    ) {
        val visibleProviderEntries = watchProgressUiState.entries.filterNot { entry ->
            entry.parentMetaId in watchProgressUiState.hiddenContentIds ||
                WatchProgressRepository.isDroppedShow(entry.parentMetaId)
        }
        filterEntriesForContinueWatchingWindow(
            entries = visibleProviderEntries,
            cutoffEpochMs = continueWatchingCutoffEpochMs,
        )
    }

    val allNextUpSeedCandidates = remember(
        watchProgressUiState.entries,
        watchProgressUiState.hiddenContentIds,
        nextUpWatchedItems,
        progressProviderOwnsCompletedHistory,
        continueWatchingPreferences.upNextFromFurthestEpisode,
    ) {
        buildHomeNextUpSeedCandidates(
            progressEntries = watchProgressUiState.entries,
            watchedItems = nextUpWatchedItems,
            providerOwnsCompletedHistory = progressProviderOwnsCompletedHistory,
            preferFurthestEpisode = continueWatchingPreferences.upNextFromFurthestEpisode,
            nowEpochMs = WatchProgressClock.nowEpochMs(),
            shouldUseProgressSeed = WatchProgressRepository::shouldUseAsNextUpSeed,
            isContentHidden = { contentId ->
                contentId in watchProgressUiState.hiddenContentIds ||
                    WatchProgressRepository.isDroppedShow(contentId)
            },
        )
    }

    val recentNextUpSeedCandidates = remember(
        allNextUpSeedCandidates,
        continueWatchingCutoffEpochMs,
    ) {
        filterHomeNextUpCandidatesForContinueWatchingWindow(
            candidates = allNextUpSeedCandidates,
            cutoffEpochMs = continueWatchingCutoffEpochMs,
        )
    }

    val activeNextUpSeedContentIds = remember(allNextUpSeedCandidates) {
        allNextUpSeedCandidates.mapTo(mutableSetOf()) { candidate -> candidate.content.id }
    }

    val currentNextUpSeedByContentId = remember(allNextUpSeedCandidates) {
        allNextUpSeedCandidates.associate { candidate ->
            candidate.content.id to (candidate.seasonNumber to candidate.episodeNumber)
        }.toMap()
    }

    val visibleContinueWatchingEntries = remember(effectiveWatchProgressEntries) {
        effectiveWatchProgressEntries.continueWatchingEntries(limit = HomeContinueWatchingMaxRecentProgressItems)
    }

    val watchProgressSeedKey = remember(watchProgressUiState.entries) {
        watchProgressUiState.entries.map { entry ->
            Triple(entry.parentMetaId, entry.seasonNumber, entry.episodeNumber)
        }
    }

    LaunchedEffect(visibleContinueWatchingEntries) {
        if (visibleContinueWatchingEntries.any(WatchProgressEntry::isCloudLibraryProgressEntry)) {
            CloudLibraryRepository.ensureLoaded()
        }
    }

    val latestCompletedAtBySeries = remember(allNextUpSeedCandidates) {
        allNextUpSeedCandidates
            .groupBy { candidate -> candidate.content.id }
            .mapValues { (_, candidates) -> candidates.maxOfOrNull { candidate -> candidate.markedAtEpochMs } ?: Long.MIN_VALUE }
    }

    val nextUpSuppressedSeriesIds = remember(visibleContinueWatchingEntries, latestCompletedAtBySeries) {
        visibleContinueWatchingEntries
            .asSequence()
            .filter { entry -> entry.parentMetaType.isSeriesTypeForContinueWatching() }
            .filter { entry ->
                shouldTreatAsActiveInProgressForNextUpSuppression(
                    progress = entry,
                    latestCompletedAt = latestCompletedAtBySeries[entry.parentMetaId],
                )
            }
            .map { entry -> entry.parentMetaId }
            .filter(String::isNotBlank)
            .toSet()
    }

    val completedSeriesCandidates = remember(recentNextUpSeedCandidates, nextUpSuppressedSeriesIds) {
        recentNextUpSeedCandidates.filter { candidate ->
            candidate.content.id !in nextUpSuppressedSeriesIds
        }
    }
    val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
    val activeProfileId = profileState.activeProfile?.profileIndex ?: 1
    val cwCacheGeneration by ContinueWatchingEnrichmentCache.generation.collectAsStateWithLifecycle()
    var hasUserScrolledContinueWatching by remember(activeProfileId, effectiveWatchProgressSource) {
        mutableStateOf(false)
    }
    var hasUserScrolledUpcoming by remember(activeProfileId, effectiveWatchProgressSource) {
        mutableStateOf(false)
    }

    LaunchedEffect(activeProfileId, effectiveWatchProgressSource, continueWatchingListState) {
        snapshotFlow { continueWatchingListState.isScrollInProgress }.collect { isScrolling ->
            if (isScrolling) hasUserScrolledContinueWatching = true
        }
    }

    LaunchedEffect(activeProfileId, effectiveWatchProgressSource, upcomingListState) {
        snapshotFlow { upcomingListState.isScrollInProgress }.collect { isScrolling ->
            if (isScrolling) hasUserScrolledUpcoming = true
        }
    }

    var nextUpItemsBySeries by remember(activeProfileId, effectiveWatchProgressSource) {
        mutableStateOf<Map<String, Pair<Long, ContinueWatchingItem>>>(emptyMap())
    }
    var processedNextUpContentIds by remember(activeProfileId, effectiveWatchProgressSource) {
        mutableStateOf<Set<String>>(emptySet())
    }

    LaunchedEffect(activeProfileId, effectiveWatchProgressSource, cwCacheGeneration) {
        nextUpItemsBySeries = emptyMap()
        processedNextUpContentIds = emptySet()
    }

    val cachedSnapshots = remember(activeProfileId, effectiveWatchProgressSource, cwCacheGeneration) {
        ContinueWatchingEnrichmentCache.getSnapshots(
            profileId = activeProfileId,
            source = effectiveWatchProgressSource,
        )
    }
    val shouldValidateMissingNextUpSeeds = remember(
        watchProgressUiState.hasLoadedRemoteProgress,
        watchedUiState.isLoaded,
        watchedUiState.hasLoadedRemoteItems,
        progressProviderOwnsCompletedHistory,
    ) {
        isHomeNextUpSeedSourceLoaded(
            providerOwnsCompletedHistory = progressProviderOwnsCompletedHistory,
            hasLoadedRemoteProgress = watchProgressUiState.hasLoadedRemoteProgress,
            hasLoadedWatchedItems = watchedUiState.isLoaded,
            hasLoadedRemoteWatchedItems = watchedUiState.hasLoadedRemoteItems,
        )
    }
    val cachedNextUpItems = remember(
        cachedSnapshots.first,
        continueWatchingPreferences.dismissedNextUpKeys,
        activeNextUpSeedContentIds,
        currentNextUpSeedByContentId,
        progressProviderOwnsCompletedHistory,
        watchProgressUiState.hasLoadedRemoteProgress,
        shouldValidateMissingNextUpSeeds,
        processedNextUpContentIds,
        nextUpItemsBySeries,
        continueWatchingPreferences.showUnairedNextUp,
        watchedUiState.isLoaded,
        watchProgressUiState.hiddenContentIds,
    ) {
        cachedSnapshots.first.mapNotNull { cached ->
            if (
                shouldValidateMissingNextUpSeeds &&
                cached.contentId !in activeNextUpSeedContentIds
            ) {
                return@mapNotNull null
            }
            val currentSeed = currentNextUpSeedByContentId[cached.contentId]
            if (currentSeed != null) {
                val (currentSeason, currentEpisode) = currentSeed
                if (
                    hasHomeNextUpSeedChangedFromCache(
                        currentSeason = currentSeason,
                        currentEpisode = currentEpisode,
                        cachedSeason = cached.seedSeason,
                        cachedEpisode = cached.seedEpisode,
                    )
                ) {
                    return@mapNotNull null
                }
            }
            if (
                progressProviderOwnsCompletedHistory &&
                watchProgressUiState.hasLoadedRemoteProgress &&
                cached.contentId in processedNextUpContentIds &&
                cached.contentId !in nextUpItemsBySeries.keys
            ) {
                return@mapNotNull null
            }
            if (nextUpDismissKey(cached.contentId, cached.seedSeason, cached.seedEpisode) in continueWatchingPreferences.dismissedNextUpKeys) {
                return@mapNotNull null
            }
            if (!cachedNextUpHasAired(cached) && !continueWatchingPreferences.showUnairedNextUp) {
                return@mapNotNull null
            }
            if (
                cached.contentId in watchProgressUiState.hiddenContentIds ||
                WatchProgressRepository.isDroppedShow(cached.contentId)
            ) {
                return@mapNotNull null
            }
            val item = cached.toContinueWatchingItem() ?: return@mapNotNull null
            val sortTimestamp = if (item.isReleaseAlert) {
                com.nuvio.app.features.watchprogress.parseReleaseDateToEpochMs(item.released) ?: cached.lastWatched
            } else {
                cached.lastWatched
            }
            cached.contentId to (sortTimestamp to item)
        }.toMap()
    }
    val cachedInProgressItems = remember(
        cachedSnapshots.second,
        effectiveWatchProgressSource,
        watchProgressUiState.hiddenContentIds,
    ) {
        buildMap {
            cachedSnapshots.second.forEach { cached ->
                if (
                    cached.contentId in watchProgressUiState.hiddenContentIds ||
                    WatchProgressRepository.isDroppedShow(cached.contentId)
                ) {
                    return@forEach
                }
                if (cached.contentType.equals("live-tv", ignoreCase = true)) {
                    return@forEach
                }
                put(cached.resolvedProgressKey(), cached.toContinueWatchingItem())
                putContinueWatchingFallbackAliases(
                    videoId = cached.videoId,
                    contentId = cached.contentId,
                    contentType = cached.contentType,
                    item = cached.toContinueWatchingItem(),
                )
            }
        }
    }
    var resolvedInProgressItems by remember(activeProfileId, effectiveWatchProgressSource) {
        mutableStateOf<Map<String, ContinueWatchingItem>>(emptyMap())
    }

    LaunchedEffect(
        activeProfileId,
        effectiveWatchProgressSource,
        visibleContinueWatchingEntries,
    ) {
        if (visibleContinueWatchingEntries.isEmpty()) {
            resolvedInProgressItems = emptyMap()
            return@LaunchedEffect
        }

        val visibleKeys = visibleContinueWatchingEntries
            .flatMap { entry -> entry.continueWatchingFallbackKeys() }
            .toSet()
        val prunedResolvedItems = resolvedInProgressItems.filterKeys { key -> key in visibleKeys }
        if (prunedResolvedItems.size != resolvedInProgressItems.size) {
            resolvedInProgressItems = prunedResolvedItems
        }

        val candidates = visibleContinueWatchingEntries
            .asSequence()
            .filter(WatchProgressEntry::needsContinueWatchingMetadataResolution)
            .filterNot { entry ->
                entry.continueWatchingFallbackKeys().any { key -> key in prunedResolvedItems }
            }
            .take(HomeContinueWatchingMaxRecentProgressItems)
            .toList()
        if (candidates.isEmpty()) return@LaunchedEffect

        val resolved = withContext(Dispatchers.Default) {
            val semaphore = Semaphore(4)
            candidates.map { entry ->
                async {
                    semaphore.withPermit {
                        resolveContinueWatchingEntryMetadata(entry)?.let { item ->
                            entry.continueWatchingFallbackKeys().associateWith { item }
                        }.orEmpty()
                    }
                }
            }.awaitAll().fold(mutableMapOf<String, ContinueWatchingItem>()) { acc, itemMap ->
                acc.apply { putAll(itemMap) }
            }
        }

        if (resolved.isNotEmpty()) {
            resolvedInProgressItems = prunedResolvedItems + resolved
            saveContinueWatchingSnapshots(
                profileId = activeProfileId,
                source = effectiveWatchProgressSource,
                cacheGeneration = cwCacheGeneration,
                nextUpItemsBySeries = nextUpItemsBySeries,
                visibleContinueWatchingEntries = visibleContinueWatchingEntries,
                todayIsoDate = CurrentDateProvider.todayIsoDate(),
                seedLastWatchedMap = emptyMap(),
            )
        }
    }

    val effectivNextUpItems = remember(
        nextUpItemsBySeries,
        cachedNextUpItems,
        continueWatchingPreferences.dismissedNextUpKeys,
        activeNextUpSeedContentIds,
        currentNextUpSeedByContentId,
        shouldValidateMissingNextUpSeeds,
    ) {
        val liveNextUpItems = filterNextUpItemsByCurrentSeeds(
            nextUpItemsBySeries = nextUpItemsBySeries,
            activeSeedContentIds = activeNextUpSeedContentIds,
            currentSeedByContentId = currentNextUpSeedByContentId,
            shouldDropItemsWithoutActiveSeed = shouldValidateMissingNextUpSeeds,
        ).filterValues { (_, item) ->
            nextUpDismissKey(
                item.parentMetaId,
                item.nextUpSeedSeasonNumber,
                item.nextUpSeedEpisodeNumber,
            ) !in continueWatchingPreferences.dismissedNextUpKeys
        }
        mergeHomeNextUpItemsWithCache(
            resolvedItems = liveNextUpItems,
            cachedItems = cachedNextUpItems,
            conclusivelyProcessedContentIds = processedNextUpContentIds,
        )
    }

    val allContinueWatchingItems = remember(
        visibleContinueWatchingEntries,
        cachedInProgressItems,
        resolvedInProgressItems,
        effectivNextUpItems,
        nextUpSuppressedSeriesIds,
        continueWatchingPreferences.sortMode,
        cloudLibraryUiState,
    ) {
        buildHomeContinueWatchingItems(
            visibleEntries = visibleContinueWatchingEntries,
            cachedInProgressByVideoId = cachedInProgressItems + resolvedInProgressItems,
            nextUpItemsBySeries = effectivNextUpItems,
            nextUpSuppressedSeriesIds = nextUpSuppressedSeriesIds,
            sortMode = continueWatchingPreferences.sortMode,
            todayIsoDate = CurrentDateProvider.todayIsoDate(),
            cloudLibraryUiState = cloudLibraryUiState,
        )
    }
    val (continueWatchingItems, upcomingItems) = remember(
        allContinueWatchingItems,
        continueWatchingPreferences.sortMode,
    ) {
        splitUpcomingItems(
            items = allContinueWatchingItems,
            mode = continueWatchingPreferences.sortMode,
        )
    }
    val hasContinueWatchingRows = continueWatchingItems.isNotEmpty() || upcomingItems.isNotEmpty()
    LaunchedEffect(
        activeProfileId,
        effectiveWatchProgressSource,
        continueWatchingItems.isNotEmpty(),
        hasUserScrolledContinueWatching,
    ) {
        if (!hasUserScrolledContinueWatching && continueWatchingItems.isNotEmpty()) {
            snapshotFlow {
                continueWatchingListState.firstVisibleItemIndex to
                    continueWatchingListState.firstVisibleItemScrollOffset
            }.collect { (index, offset) ->
                if (
                    !hasUserScrolledContinueWatching &&
                    !continueWatchingListState.isScrollInProgress &&
                    (index != 0 || offset != 0)
                ) {
                    continueWatchingListState.scrollToItem(0)
                }
            }
        }
    }
    LaunchedEffect(
        activeProfileId,
        effectiveWatchProgressSource,
        upcomingItems.isNotEmpty(),
        hasUserScrolledUpcoming,
    ) {
        if (!hasUserScrolledUpcoming && upcomingItems.isNotEmpty()) {
            snapshotFlow {
                upcomingListState.firstVisibleItemIndex to upcomingListState.firstVisibleItemScrollOffset
            }.collect { (index, offset) ->
                if (
                    !hasUserScrolledUpcoming &&
                    !upcomingListState.isScrollInProgress &&
                    (index != 0 || offset != 0)
                ) {
                    upcomingListState.scrollToItem(0)
                }
            }
        }
    }
    val enabledAddons = remember(addonsUiState.addons) {
        addonsUiState.addons.enabledAddons()
    }
    val isRefreshingEnabledAddons = remember(enabledAddons) {
        enabledAddons.any { addon -> addon.isRefreshing }
    }
    LaunchedEffect(homeUiState.isLoading, isRefreshingEnabledAddons) {
        if (!homeUiState.isLoading && !isRefreshingEnabledAddons) {
            manualRefreshRequested = false
        }
    }
    val availableManifests = remember(enabledAddons) {
        enabledAddons.mapNotNull { addon -> addon.manifest }
    }

    val metaProviderKey = remember(availableManifests) {
        availableManifests
            .filter { manifest -> manifest.resources.any { resource -> resource.name == "meta" } }
            .map { manifest -> manifest.transportUrl }
            .sorted()
    }
    val metaProviderReadinessKey = remember(enabledAddons) {
        enabledAddons
            .sortedBy { addon -> addon.manifestUrl }
            .joinToString(separator = "|") { addon ->
                "${addon.manifestUrl}:${addon.manifest != null}:${addon.isRefreshing}:${addon.errorMessage.orEmpty()}"
            }
    }
    var nextUpResolutionRetryAttempt by remember(
        activeProfileId,
        effectiveWatchProgressSource,
        completedSeriesCandidates,
        metaProviderKey,
        metaProviderReadinessKey,
        networkStatusUiState.condition,
        continueWatchingPreferences.showUnairedNextUp,
        continueWatchingPreferences.upNextFromFurthestEpisode,
        continueWatchingPreferences.dismissedNextUpKeys,
        cwCacheGeneration,
    ) {
        mutableStateOf(0)
    }

    val catalogRefreshKey = remember(enabledAddons, cloudStreamUiState.registryRevision) {
        buildHomeCatalogRefreshSignature(enabledAddons) +
            "cloudstream:${cloudStreamUiState.registryRevision}:${cloudStreamUiState.plugins.count { it.isRunnable }}"
    }

    LaunchedEffect(catalogRefreshKey) {
        if (catalogRefreshKey.isEmpty()) return@LaunchedEffect
        HomeCatalogSettingsRepository.syncCatalogs(enabledAddons)
        HomeRepository.refresh(enabledAddons)
    }

    LaunchedEffect(
        tmdbSettingsUiState.enabled,
        tmdbSettingsUiState.hasApiKey,
        tmdbSettingsUiState.language,
        tmdbSettingsUiState.useArtwork,
    ) {
        HomeRepository.applyCurrentSettings()
    }

    LaunchedEffect(collections) {
        HomeCatalogSettingsRepository.syncCollections(collections)
    }

    LaunchedEffect(
        completedSeriesCandidates,
        metaProviderKey,
        metaProviderReadinessKey,
        networkStatusUiState.condition,
        nextUpResolutionRetryAttempt,
        continueWatchingPreferences.showUnairedNextUp,
        continueWatchingPreferences.upNextFromFurthestEpisode,
        continueWatchingPreferences.dismissedNextUpKeys,
        isRefreshingEnabledAddons,
        watchProgressSeedKey,
        visibleContinueWatchingEntries,
        nextUpWatchedItems,
        watchedUiState.isLoaded,
        watchedUiState.hasLoadedRemoteItems,
        watchProgressUiState.hasLoadedRemoteProgress,
        shouldValidateMissingNextUpSeeds,
        activeProfileId,
        effectiveWatchProgressSource,
        cwCacheGeneration,
    ) {
        // Do not wait for remote history when playback has already recorded a local completion.
        // The later remote validation still removes stale results once both snapshots are ready.
        if (!shouldValidateMissingNextUpSeeds && completedSeriesCandidates.isEmpty()) {
            return@LaunchedEffect
        }

        if (completedSeriesCandidates.isEmpty()) {
            nextUpItemsBySeries = emptyMap()
            processedNextUpContentIds = emptySet()
            saveContinueWatchingSnapshots(
                profileId = activeProfileId,
                source = effectiveWatchProgressSource,
                cacheGeneration = cwCacheGeneration,
                nextUpItemsBySeries = emptyMap(),
                visibleContinueWatchingEntries = visibleContinueWatchingEntries,
                todayIsoDate = CurrentDateProvider.todayIsoDate(),
                seedLastWatchedMap = emptyMap(),
            )
            return@LaunchedEffect
        }

        val retainedRetryItems = if (nextUpResolutionRetryAttempt > 0) nextUpItemsBySeries else emptyMap()
        val retainedRetryProcessedIds = if (nextUpResolutionRetryAttempt > 0) {
            processedNextUpContentIds
        } else {
            emptySet()
        }
        withContext(Dispatchers.Default) {
            val cachedResolvedNextUpItems = completedSeriesCandidates.mapNotNull { candidate ->
                val cached = retainedRetryItems[candidate.content.id]
                    ?: cachedNextUpItems[candidate.content.id]
                    ?: return@mapNotNull null
                val item = cached.second
                if (
                    item.nextUpSeedSeasonNumber != candidate.seasonNumber ||
                    item.nextUpSeedEpisodeNumber != candidate.episodeNumber
                ) {
                    return@mapNotNull null
                }
                if (!hasUsableHomeNextUpMetadata(item)) return@mapNotNull null
                candidate.content.id to cached
            }.toMap()
            val candidatesToResolve = completedSeriesCandidates.filter { candidate ->
                candidate.content.id !in cachedResolvedNextUpItems &&
                    candidate.content.id !in retainedRetryProcessedIds
            }
            val resolutionPlan = planHomeNextUpResolutionCandidates(candidatesToResolve)
            val resolutionCandidates = resolutionPlan.initialCandidates
            val deferredResolutionCandidates = resolutionPlan.deferredCandidates
            val seedLastWatchedMap = completedSeriesCandidates.associate { it.content.id to it.markedAtEpochMs }
            if (candidatesToResolve.isEmpty()) {
                val conclusiveContentIds = cachedResolvedNextUpItems.keys + retainedRetryProcessedIds
                val cachedResults = mergeHomeNextUpItemsWithCache(
                    resolvedItems = cachedResolvedNextUpItems,
                    cachedItems = cachedNextUpItems,
                    conclusivelyProcessedContentIds = conclusiveContentIds,
                )
                withContext(Dispatchers.Main) {
                    nextUpItemsBySeries = cachedResults
                    processedNextUpContentIds = conclusiveContentIds
                }
                saveContinueWatchingSnapshots(
                    profileId = activeProfileId,
                    source = effectiveWatchProgressSource,
                    cacheGeneration = cwCacheGeneration,
                    nextUpItemsBySeries = cachedResults,
                    visibleContinueWatchingEntries = visibleContinueWatchingEntries,
                    todayIsoDate = CurrentDateProvider.todayIsoDate(),
                    seedLastWatchedMap = seedLastWatchedMap,
                )
                return@withContext
            }

            val todayIsoDate = CurrentDateProvider.todayIsoDate()
            val semaphore = Semaphore(NEXT_UP_RESOLUTION_CONCURRENCY)
            val freshResults = mutableMapOf<String, Pair<Long, ContinueWatchingItem>>()
            val processedFreshContentIds = retainedRetryProcessedIds.toMutableSet()
            val attemptedFreshContentIds = mutableSetOf<String>()
            val candidateBatches = resolutionCandidates.chunked(NEXT_UP_RESOLUTION_BATCH_SIZE)

            for (batch in candidateBatches) {
                if (cachedResolvedNextUpItems.size + freshResults.size >= HomeContinueWatchingMaxRecentProgressItems) {
                    break
                }
                attemptedFreshContentIds += batch.map { candidate -> candidate.content.id }
                val batchResults = kotlinx.coroutines.channels.Channel<
                    Pair<CompletedSeriesCandidate, HomeNextUpResolutionAttempt>,
                >(batch.size)
                batch.forEach { completedEntry ->
                    launch {
                        val result = try {
                            semaphore.withPermit {
                                completedEntry to resolveHomeNextUpCandidate(
                                    completedEntry = completedEntry,
                                    watchProgressEntries = watchProgressUiState.entries,
                                    watchedItems = nextUpWatchedItems,
                                    cachedFallbackItem = cachedNextUpItems[completedEntry.content.id]?.second,
                                    todayIsoDate = todayIsoDate,
                                    preferFurthestEpisode = continueWatchingPreferences.upNextFromFurthestEpisode,
                                    showUnairedNextUp = continueWatchingPreferences.showUnairedNextUp,
                                    dismissedNextUpKeys = continueWatchingPreferences.dismissedNextUpKeys,
                                    providerOwnsCompletedHistory = progressProviderOwnsCompletedHistory,
                                )
                            }
                        } catch (error: Throwable) {
                            if (error is CancellationException) throw error
                            completedEntry to HomeNextUpResolutionAttempt.transientFailure()
                        }
                        batchResults.send(result)
                    }
                }
                repeat(batch.size) {
                    val (candidate, attempt) = batchResults.receive()
                    if (attempt.isConclusive) processedFreshContentIds += candidate.content.id

                    val resolvedBeforeCandidate = freshResults.size
                    attempt.resolved?.let { (contentId, item) ->
                        if (cachedResolvedNextUpItems.size + freshResults.size < HomeContinueWatchingMaxRecentProgressItems) {
                            freshResults[contentId] = item
                        }
                    }
                    if (freshResults.size > resolvedBeforeCandidate || attempt.isConclusive) {
                        val conclusiveContentIds = cachedResolvedNextUpItems.keys + processedFreshContentIds
                        val progressiveResults = mergeHomeNextUpItemsWithCache(
                            resolvedItems = cachedResolvedNextUpItems + freshResults,
                            cachedItems = cachedNextUpItems,
                            conclusivelyProcessedContentIds = conclusiveContentIds,
                        )
                        withContext(Dispatchers.Main) {
                            nextUpItemsBySeries = progressiveResults
                            processedNextUpContentIds = conclusiveContentIds
                        }
                    }
                }
                batchResults.close()
            }

            val conclusiveContentIds = cachedResolvedNextUpItems.keys + processedFreshContentIds
            val results = mergeHomeNextUpItemsWithCache(
                resolvedItems = cachedResolvedNextUpItems + freshResults,
                cachedItems = cachedNextUpItems,
                conclusivelyProcessedContentIds = conclusiveContentIds,
            )
            withContext(Dispatchers.Main) {
                nextUpItemsBySeries = results
                processedNextUpContentIds = conclusiveContentIds
            }

            saveContinueWatchingSnapshots(
                profileId = activeProfileId,
                source = effectiveWatchProgressSource,
                cacheGeneration = cwCacheGeneration,
                nextUpItemsBySeries = results,
                visibleContinueWatchingEntries = visibleContinueWatchingEntries,
                todayIsoDate = todayIsoDate,
                seedLastWatchedMap = seedLastWatchedMap,
            )

            val deferredCandidateBatches = deferredResolutionCandidates.chunked(NEXT_UP_RESOLUTION_BATCH_SIZE)
            for (batch in deferredCandidateBatches) {
                if (cachedResolvedNextUpItems.size + freshResults.size >= HomeContinueWatchingMaxRecentProgressItems) {
                    break
                }
                attemptedFreshContentIds += batch.map { candidate -> candidate.content.id }
                val batchResults = kotlinx.coroutines.channels.Channel<
                    Pair<CompletedSeriesCandidate, HomeNextUpResolutionAttempt>,
                >(batch.size)
                batch.forEach { completedEntry ->
                    launch {
                        val result = try {
                            semaphore.withPermit {
                                completedEntry to resolveHomeNextUpCandidate(
                                    completedEntry = completedEntry,
                                    watchProgressEntries = watchProgressUiState.entries,
                                    watchedItems = nextUpWatchedItems,
                                    cachedFallbackItem = cachedNextUpItems[completedEntry.content.id]?.second,
                                    todayIsoDate = todayIsoDate,
                                    preferFurthestEpisode = continueWatchingPreferences.upNextFromFurthestEpisode,
                                    showUnairedNextUp = continueWatchingPreferences.showUnairedNextUp,
                                    dismissedNextUpKeys = continueWatchingPreferences.dismissedNextUpKeys,
                                    providerOwnsCompletedHistory = progressProviderOwnsCompletedHistory,
                                )
                            }
                        } catch (error: Throwable) {
                            if (error is CancellationException) throw error
                            completedEntry to HomeNextUpResolutionAttempt.transientFailure()
                        }
                        batchResults.send(result)
                    }
                }
                repeat(batch.size) {
                    val (candidate, attempt) = batchResults.receive()
                    if (attempt.isConclusive) processedFreshContentIds += candidate.content.id

                    val resolvedBeforeCandidate = freshResults.size
                    attempt.resolved?.let { (contentId, item) ->
                        if (cachedResolvedNextUpItems.size + freshResults.size < HomeContinueWatchingMaxRecentProgressItems) {
                            freshResults[contentId] = item
                        }
                    }
                    if (freshResults.size > resolvedBeforeCandidate || attempt.isConclusive) {
                        val deferredConclusiveContentIds = cachedResolvedNextUpItems.keys + processedFreshContentIds
                        val progressiveResults = mergeHomeNextUpItemsWithCache(
                            resolvedItems = cachedResolvedNextUpItems + freshResults,
                            cachedItems = cachedNextUpItems,
                            conclusivelyProcessedContentIds = deferredConclusiveContentIds,
                        )
                        withContext(Dispatchers.Main) {
                            nextUpItemsBySeries = progressiveResults
                            processedNextUpContentIds = deferredConclusiveContentIds
                        }
                        saveContinueWatchingSnapshots(
                            profileId = activeProfileId,
                            source = effectiveWatchProgressSource,
                            cacheGeneration = cwCacheGeneration,
                            nextUpItemsBySeries = progressiveResults,
                            visibleContinueWatchingEntries = visibleContinueWatchingEntries,
                            todayIsoDate = todayIsoDate,
                            seedLastWatchedMap = seedLastWatchedMap,
                        )
                    }
                    yield()
                }
                batchResults.close()
            }

            val transientContentIds = attemptedFreshContentIds
                .asSequence()
                .filterNot { contentId -> contentId in processedFreshContentIds }
                .toList()
            if (
                transientContentIds.isNotEmpty() &&
                nextUpResolutionRetryAttempt < MAX_NEXT_UP_RESOLUTION_RETRIES &&
                networkStatusUiState.condition == NetworkCondition.Online
            ) {
                val retryDelayMs = NEXT_UP_RESOLUTION_RETRY_BASE_DELAY_MS *
                    (1L shl nextUpResolutionRetryAttempt)
                delay(retryDelayMs)
                withContext(Dispatchers.Main) {
                    nextUpResolutionRetryAttempt += 1
                }
            }
        }
    }

    val hasActiveAddons = enabledAddons.any { it.manifest != null }
    val showHeroSlot = homeSettingsUiState.heroEnabled
    val isResolvingHeroSources = enabledAddons.any { it.isRefreshing } || homeUiState.isLoading
    val showHeroSkeleton = showHeroSlot &&
        homeUiState.heroItems.isEmpty() &&
        isResolvingHeroSources
    var firstCatalogReported by remember { mutableStateOf(false) }

    val visibleCollections = remember(collections) {
        visibleCollectionsWithUniqueIds(collections)
    }
    val collectionsMap = remember(visibleCollections) {
        visibleCollections.associateBy { "collection_${it.id}" }
    }
    val sectionsMap = remember(homeUiState.sections) {
        homeUiState.sections.associateBy(HomeCatalogSection::key)
    }
    val enabledHomeItems = remember(homeSettingsUiState.items) {
        homeSettingsUiState.items.filter { it.enabled }
    }
    val visibleSeriesPosterTargets = remember(enabledHomeItems, sectionsMap) {
        enabledHomeItems
            .filterNot { it.isCollection }
            .mapNotNull { settingsItem -> sectionsMap[settingsItem.key] }
            .flatMap { section -> section.items.take(HOME_CATALOG_PREVIEW_LIMIT) }
            .filter { item -> item.type.isHomeSeriesLikeType() }
            .distinctBy { item -> watchedItemKey(item.type, item.id) }
    }
    LaunchedEffect(
        visibleSeriesPosterTargets,
        watchedUiState.items,
        watchProgressUiState.entries,
    ) {
        reconcileVisibleSeriesPosterBadges(
            items = visibleSeriesPosterTargets,
            watchedItems = watchedUiState.items,
            progressEntries = watchProgressUiState.entries,
        )
    }
    val hasRenderableCollectionRows = remember(enabledHomeItems, collectionsMap) {
        enabledHomeItems.any { item ->
            item.isCollection && collectionsMap[item.key] != null
        }
    }
    val smartShelvesEnabled = nuvioEnhancedSettings.enhancedHomeFeaturesEnabled &&
        nuvioEnhancedSettings.smartShelvesEnabled &&
        !nuvioEnhancedSettings.quietHomeModeEnabled
    val smartShelves = remember(
        smartShelvesEnabled,
        continueWatchingItems,
        libraryUiState.items,
        homeUiState.sections,
    ) {
        if (!smartShelvesEnabled) {
            emptyList()
        } else {
            buildHomeSmartShelves(
                continueWatchingItems = continueWatchingItems,
                libraryItems = libraryUiState.items,
                catalogSections = homeUiState.sections,
            )
        }
    }
    val hasPremiumHomeRows = smartShelves.isNotEmpty()
    val hasInitialHomeContent = homeUiState.sections.isNotEmpty() ||
        homeUiState.heroItems.isNotEmpty() ||
        hasContinueWatchingRows ||
        hasRenderableCollectionRows ||
        hasPremiumHomeRows
    val initialHomeReady = hasInitialHomeContent ||
        (
            !homeUiState.isLoading &&
                !isRefreshingEnabledAddons &&
                (!hasActiveAddons || homeUiState.errorMessage != null)
            )
    LaunchedEffect(initialHomeReady, onFirstCatalogRendered) {
        if (firstCatalogReported || !initialHomeReady) return@LaunchedEffect
        firstCatalogReported = true
        onFirstCatalogRendered?.invoke()
    }
    val hapticFeedback = LocalHapticFeedback.current
    var heroRefreshHapticArmed by remember { mutableStateOf(true) }
    var heroRefreshPullProgress by remember { mutableStateOf(0f) }
    val isHomeRefreshInProgress = homeUiState.isLoading || isRefreshingEnabledAddons || manualRefreshRequested
    LaunchedEffect(isHomeRefreshInProgress) {
        if (isHomeRefreshInProgress) {
            heroRefreshPullProgress = 0f
        }
    }
    LaunchedEffect(
        heroRefreshPullProgress,
        nuvioEnhancedSettings.heroRefreshHapticsEnabled,
    ) {
        if (!nuvioEnhancedSettings.heroRefreshHapticsEnabled) {
            heroRefreshHapticArmed = true
            return@LaunchedEffect
        }
        when {
            heroRefreshPullProgress >= HOME_HERO_REFRESH_HAPTIC_THRESHOLD &&
                heroRefreshHapticArmed -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                heroRefreshHapticArmed = false
            }
            heroRefreshPullProgress < HOME_HERO_REFRESH_HAPTIC_RESET_THRESHOLD -> {
                heroRefreshHapticArmed = true
            }
        }
    }
    val heroRefreshVisualProgress by animateFloatAsState(
        targetValue = heroRefreshPullProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 150),
        label = "homeHeroRefreshVisual",
    )
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val viewportHeight = maxHeight
        val tvLayout = LocalTvLayoutProfile.current.enabled
        val homeSectionPadding = if (tvLayout) 0.dp else homeSectionHorizontalPaddingForWidth(maxWidth.value)
        val continueWatchingLayout = rememberContinueWatchingLayout(
            maxWidthDp = maxWidth.value,
            visualScale = if (tvLayout) 1.85f else 1f,
        )
        val posterCardStyle = rememberPosterCardStyleUiState()
        val continueWatchingCardHeight = remember(posterCardStyle.widthDp) {
            continueWatchingLandscapeCardHeight(posterCardStyle.widthDp)
        }
        val heroToContinueWatchingGap = if (nuvioEnhancedSettings.streamingShowcaseHeroEnabled) {
            HOME_STREAMING_SHOWCASE_HERO_TO_CONTINUE_WATCHING_GAP
        } else {
            HOME_HERO_TO_CONTINUE_WATCHING_GAP
        }
        val nativeBottomNavigationOverlayHeight =
            if (LocalNuvioBottomNavigationOverlayPadding.current > 0.dp) {
                nuvioSafeBottomPadding()
            } else {
                0.dp
            }
        val mobileHeroBelowSectionHeightHint = remember(
            maxWidth.value,
            continueWatchingPreferences.isVisible,
            continueWatchingPreferences.style,
            hasContinueWatchingRows,
            continueWatchingLayout,
            continueWatchingCardHeight,
            nativeBottomNavigationOverlayHeight,
            hasPremiumHomeRows,
        ) {
            heroMobileBelowSectionHeightHint(
                maxWidthDp = maxWidth.value,
                hasPremiumHomeRows = hasPremiumHomeRows,
                continueWatchingVisible = continueWatchingPreferences.isVisible,
                hasContinueWatchingItems = hasContinueWatchingRows,
                continueWatchingStyle = continueWatchingPreferences.style,
                continueWatchingLayout = continueWatchingLayout,
                continueWatchingCardHeight = continueWatchingCardHeight,
                bottomNavigationOverlayHeight = nativeBottomNavigationOverlayHeight,
            )
        }
        val heroStretchState = rememberHeroStretchState(homeListState)
        val heroStretchModifier = if (showHeroSlot) {
            Modifier.nestedScroll(heroStretchState.nestedScrollConnection)
        } else {
            Modifier
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .homePremiumHeroRefresh(
                    enabled = showHeroSlot,
                    isRefreshing = isHomeRefreshInProgress,
                    isAtTop = {
                        homeListState.firstVisibleItemIndex == 0 &&
                            homeListState.firstVisibleItemScrollOffset == 0
                    },
                    onPullProgressChange = { progress ->
                        heroRefreshPullProgress = progress
                    },
                    onRefresh = {
                        coroutineScope.launch {
                            manualRefreshRequested = true
                            HomeRepository.refresh(addonsUiState.addons.enabledAddons(), force = true)
                        }
                    },
                ),
        ) {
            NuvioScreen(
                modifier = Modifier.fillMaxSize().then(heroStretchModifier),
                horizontalPadding = 0.dp,
                topPadding = if (showHeroSlot) 0.dp else null,
                backgroundColor = Color.Transparent,
                listState = homeListState,
            ) {
                if (showHeroSlot) {
                    item {
                        when {
                            showHeroSkeleton -> HomeSkeletonHero(
                                modifier = Modifier,
                                viewportHeight = viewportHeight,
                                mobileBelowSectionHeightHint = mobileHeroBelowSectionHeightHint,
                                posterArtHeroEnabled = nuvioEnhancedSettings.posterArtHeroEnabled,
                                streamingShowcaseHeroEnabled = nuvioEnhancedSettings.streamingShowcaseHeroEnabled,
                            )

                            homeUiState.heroItems.isNotEmpty() -> HomeHeroSection(
                                items = homeUiState.heroItems,
                                modifier = Modifier,
                                viewportHeight = viewportHeight,
                                mobileBelowSectionHeightHint = mobileHeroBelowSectionHeightHint,
                                listState = homeListState,
                                autoScrollEnabled = !nuvioEnhancedSettings.originalNuvioHeroBannerEnabled &&
                                    homeSettingsUiState.heroAutoScrollEnabled &&
                                    heroRefreshVisualProgress <= 0.01f &&
                                    !isHomeRefreshInProgress,
                                motionPreviewEnabled = !nuvioEnhancedSettings.originalNuvioHeroBannerEnabled &&
                                    homeSettingsUiState.heroMotionPreviewEnabled,
                                heroDisplayMode = nuvioEnhancedSettings.heroDisplayMode,
                                heroArtworkSource = nuvioEnhancedSettings.heroArtworkSource,
                                posterArtHeroEnabled = nuvioEnhancedSettings.posterArtHeroEnabled,
                                streamingShowcaseHeroEnabled = nuvioEnhancedSettings.streamingShowcaseHeroEnabled,
                                streamingShowcaseVideoPreviewEnabled =
                                    nuvioEnhancedSettings.streamingShowcaseVideoPreviewEnabled,
                                streamingShowcaseVideoPreviewSoundEnabled =
                                    nuvioEnhancedSettings.streamingShowcaseVideoPreviewSoundEnabled,
                                compactMetadata = nuvioEnhancedSettings.compactHeroMetadata,
                                showRatings = nuvioEnhancedSettings.showHeroRatings,
                                ratingsAboveMetadata = nuvioEnhancedSettings.ratingsAboveMetadata,
                                showOverview = nuvioEnhancedSettings.streamingShowcaseHeroEnabled ||
                                    (
                                        nuvioEnhancedSettings.showHeroOverview &&
                                        !nuvioEnhancedSettings.quietHomeModeEnabled
                                    ),
                                showDetailsButton = nuvioEnhancedSettings.showHeroDetailsButton,
                                originalNuvioHeroBannerEnabled = nuvioEnhancedSettings.originalNuvioHeroBannerEnabled,
                                metadataRefreshKey = tmdbSettingsUiState.language,
                                refreshPullProgress = heroRefreshVisualProgress,
                                stretchPx = { heroStretchState.stretchPx },
                                onItemClick = onPosterClick,
                                onPlayClick = onPosterClick,
                                onSaveClick = { item ->
                                    coroutineScope.launch {
                                        LibraryRepository.toggleSaved(item.toLibraryItem(savedAtEpochMs = 0L))
                                    }
                                },
                                onStreamingShowcaseVideoPreviewSoundChange = { enabled ->
                                    NuvioEnhancedSettingsRepository.setStreamingShowcaseVideoPreviewSoundEnabled(enabled)
                                },
                                isSaved = { item ->
                                    libraryUiState.items.any { saved ->
                                        saved.id == item.id && saved.type == item.type
                                    } || LibraryRepository.isSaved(item.id, item.type)
                                },
                            )

                            else -> HomeHeroReservedSpace(
                                modifier = Modifier,
                                viewportHeight = viewportHeight,
                                mobileBelowSectionHeightHint = mobileHeroBelowSectionHeightHint,
                                posterArtHeroEnabled = nuvioEnhancedSettings.posterArtHeroEnabled,
                                streamingShowcaseHeroEnabled = nuvioEnhancedSettings.streamingShowcaseHeroEnabled,
                            )
                        }
                    }
                }

                if (continueWatchingPreferences.isVisible && continueWatchingItems.isNotEmpty()) {
                    item(key = HOME_CONTINUE_WATCHING_SECTION_KEY) {
                        HomeContinueWatchingSection(
                            items = continueWatchingItems,
                            dataSourceKey = ContinueWatchingDataSourceKey(
                                profileId = activeProfileId,
                                source = effectiveWatchProgressSource,
                            ),
                            style = continueWatchingPreferences.style,
                            useEpisodeThumbnails = continueWatchingPreferences.useEpisodeThumbnails,
                            blurNextUp = continueWatchingPreferences.blurNextUp,
                            showReadyBadge = nuvioEnhancedSettings.showContinueWatchingReadyBadge,
                            modifier = Modifier
                                .padding(
                                    top = heroToContinueWatchingGap,
                                    bottom = 12.dp,
                                ),
                            sectionPadding = homeSectionPadding,
                            layout = continueWatchingLayout,
                            listState = continueWatchingListState,
                            onItemClick = onContinueWatchingClick,
                            onItemLongPress = onContinueWatchingLongPress,
                        )
                    }
                }

                if (continueWatchingPreferences.isVisible && upcomingItems.isNotEmpty()) {
                    item(key = HOME_UPCOMING_SECTION_KEY) {
                        HomeContinueWatchingSection(
                            items = upcomingItems,
                            dataSourceKey = ContinueWatchingDataSourceKey(
                                profileId = activeProfileId,
                                source = effectiveWatchProgressSource,
                            ),
                            style = continueWatchingPreferences.style,
                            title = stringResource(Res.string.upcoming_section_title),
                            useEpisodeThumbnails = continueWatchingPreferences.useEpisodeThumbnails,
                            blurNextUp = continueWatchingPreferences.blurNextUp,
                            showReadyBadge = nuvioEnhancedSettings.showContinueWatchingReadyBadge,
                            modifier = Modifier.padding(
                                top = if (continueWatchingItems.isEmpty()) heroToContinueWatchingGap else 0.dp,
                                bottom = 12.dp,
                            ),
                            sectionPadding = homeSectionPadding,
                            layout = continueWatchingLayout,
                            listState = upcomingListState,
                            onItemClick = onContinueWatchingClick,
                            onItemLongPress = onContinueWatchingLongPress,
                        )
                    }
                }

                if (smartShelves.isNotEmpty()) {
                    item(key = HOME_SMART_SHELVES_SECTION_KEY) {
                        HomeSmartShelfComposerSection(
                            shelves = smartShelves,
                            modifier = Modifier.padding(bottom = 12.dp),
                            sectionPadding = homeSectionPadding,
                            onPosterClick = onPosterClick,
                        )
                    }
                }

                when {
                    !hasActiveAddons && !hasRenderableCollectionRows && !hasPremiumHomeRows -> {
                        item {
                            HomeEmptyStateCard(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                title = stringResource(Res.string.compose_search_empty_no_active_addons_title),
                                message = stringResource(Res.string.home_empty_no_active_addons_message),
                            )
                        }
                    }

                    homeUiState.isLoading && homeUiState.sections.isEmpty() && !hasRenderableCollectionRows && !hasPremiumHomeRows -> {
                        items(3) {
                            HomeSkeletonRow(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                showHeaderAccent = !homeSettingsUiState.hideCatalogUnderline,
                            )
                        }
                    }

                    homeUiState.sections.isEmpty() && homeUiState.heroItems.isEmpty() &&
                        (!continueWatchingPreferences.isVisible || !hasContinueWatchingRows) &&
                        !hasRenderableCollectionRows &&
                        !hasPremiumHomeRows -> {
                        item {
                            if (networkStatusUiState.isOfflineLike) {
                                NuvioNetworkOfflineCard(
                                    condition = networkStatusUiState.condition,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    onRetry = {
                                        NetworkStatusRepository.requestRefresh(force = true)
                                        HomeRepository.refresh(addonsUiState.addons.enabledAddons(), force = true)
                                    },
                                )
                            } else {
                                HomeEmptyStateCard(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    title = stringResource(Res.string.home_empty_no_rows_title),
                                    message = homeUiState.errorMessage
                                        ?: stringResource(Res.string.home_empty_no_rows_message),
                                )
                            }
                        }
                    }

                    else -> {
                        enabledHomeItems.forEach { settingsItem ->
                            if (settingsItem.isCollection) {
                                val collection = collectionsMap[settingsItem.key]
                                if (collection != null) {
                                    item(key = settingsItem.key) {
                                        HomeCollectionRowSection(
                                            collection = collection,
                                            modifier = Modifier.padding(bottom = 12.dp),
                                            sectionPadding = homeSectionPadding,
                                            animateGifsProvider = { animateCollectionGifs },
                                            onFolderClick = onFolderClick,
                                        )
                                    }
                                }
                            } else {
                                val section = sectionsMap[settingsItem.key]
                                if (section != null && section.items.isNotEmpty()) {
                                    item(key = settingsItem.key) {
                                        HomeCatalogRowSection(
                                            section = section,
                                            entries = section.items.take(HOME_CATALOG_PREVIEW_LIMIT),
                                            modifier = Modifier.padding(bottom = 12.dp),
                                            sectionPadding = homeSectionPadding,
                                            onViewAllClick = if (section.canOpenCatalog(HOME_CATALOG_PREVIEW_LIMIT)) {
                                                onCatalogClick?.let { { it(section) } }
                                            } else {
                                                null
                                            },
                                            watchedKeys = watchedUiState.watchedKeys,
                                            fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                                            onPosterClick = onPosterClick,
                                            onPosterLongClick = onPosterLongClick,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val HOME_CATALOG_PREVIEW_LIMIT = 18
private const val HOME_CONTINUE_WATCHING_SECTION_KEY = "home_continue_watching"
private const val HOME_UPCOMING_SECTION_KEY = "home_upcoming"
private const val HOME_SMART_SHELVES_SECTION_KEY = "home_smart_shelves"
private const val HOME_HERO_REFRESH_HAPTIC_THRESHOLD = 1f
private const val HOME_HERO_REFRESH_HAPTIC_RESET_THRESHOLD = 0.22f
private val HOME_PREMIUM_REFRESH_TRIGGER_DISTANCE = 96.dp
private val HOME_PREMIUM_REFRESH_MAX_PULL_DISTANCE = 168.dp
private val HOME_STREAMING_SHOWCASE_HERO_TO_CONTINUE_WATCHING_GAP = 0.dp
private val HOME_HERO_TO_CONTINUE_WATCHING_GAP = 32.dp
internal const val HomeContinueWatchingMaxRecentProgressItems = 300
internal const val HomeNextUpInitialResolutionLimit = 32
private const val NEXT_UP_RESOLUTION_CONCURRENCY = 4
private const val MAX_NEXT_UP_RESOLUTION_RETRIES = 3
private const val NEXT_UP_RESOLUTION_RETRY_BASE_DELAY_MS = 1_500L

private fun Modifier.homePremiumHeroRefresh(
    enabled: Boolean,
    isRefreshing: Boolean,
    isAtTop: () -> Boolean,
    onPullProgressChange: (Float) -> Unit,
    onRefresh: () -> Unit,
): Modifier {
    if (!enabled) return this

    return pointerInput(enabled, isRefreshing) {
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial,
            )
            if (isRefreshing) {
                onPullProgressChange(0f)
                if (isAtTop()) {
                    var totalDx = 0f
                    var totalDy = 0f
                    var consumingRefreshPull = false

                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break

                        val delta = change.position - change.previousPosition
                        totalDx += delta.x
                        totalDy += delta.y

                        if (!consumingRefreshPull) {
                            val horizontalDrag =
                                abs(totalDx) > viewConfiguration.touchSlop && abs(totalDx) > abs(totalDy)
                            val downwardDrag =
                                totalDy > viewConfiguration.touchSlop && totalDy > abs(totalDx)
                            val upwardDrag = totalDy < -viewConfiguration.touchSlop

                            when {
                                horizontalDrag || upwardDrag || !isAtTop() -> break
                                downwardDrag -> consumingRefreshPull = true
                                else -> continue
                            }
                        }

                        if (consumingRefreshPull && delta.y > 0f && !isAtTop()) break
                    }
                }
                onPullProgressChange(0f)
                return@awaitEachGesture
            }
            if (!isAtTop()) {
                onPullProgressChange(0f)
                return@awaitEachGesture
            }

            val triggerDistancePx = HOME_PREMIUM_REFRESH_TRIGGER_DISTANCE.toPx()
            val maxPullDistancePx = HOME_PREMIUM_REFRESH_MAX_PULL_DISTANCE.toPx()
            var totalDx = 0f
            var totalDy = 0f
            var pullDistancePx = 0f
            var dragging = false
            var refreshRequested = false

            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                if (!change.pressed) {
                    if (dragging && pullDistancePx >= triggerDistancePx && !refreshRequested) {
                        refreshRequested = true
                        onRefresh()
                    }
                    onPullProgressChange(0f)
                    break
                }

                val delta = change.position - change.previousPosition
                totalDx += delta.x
                totalDy += delta.y

                if (!dragging) {
                    val horizontalDrag =
                        abs(totalDx) > viewConfiguration.touchSlop && abs(totalDx) > abs(totalDy)
                    val downwardDrag =
                        totalDy > viewConfiguration.touchSlop && totalDy > abs(totalDx)
                    val upwardDrag = totalDy < -viewConfiguration.touchSlop

                    when {
                        horizontalDrag || upwardDrag || !isAtTop() -> {
                            onPullProgressChange(0f)
                            break
                        }
                        downwardDrag -> {
                            dragging = true
                        }
                        else -> continue
                    }
                }

                val resistedDelta = if (delta.y > 0f) {
                    val resistance = 1f - (pullDistancePx / maxPullDistancePx).coerceIn(0f, 0.82f)
                    delta.y * (0.62f * resistance)
                } else {
                    delta.y * 0.9f
                }
                pullDistancePx = (pullDistancePx + resistedDelta).coerceIn(0f, maxPullDistancePx)
                onPullProgressChange((pullDistancePx / triggerDistancePx).coerceIn(0f, 1f))
            }
            onPullProgressChange(0f)
        }
    }
}

private const val NEXT_UP_RESOLUTION_BATCH_SIZE = NEXT_UP_RESOLUTION_CONCURRENCY

private suspend fun reconcileVisibleSeriesPosterBadges(
    items: List<MetaPreview>,
    watchedItems: List<WatchedItem>,
    progressEntries: List<WatchProgressEntry>,
) {
    if (items.isEmpty()) return
    val watchedKeys = watchedItems.mapTo(linkedSetOf()) { item ->
        watchedItemKey(item.type, item.id, item.season, item.episode)
    }
    val touchedSeriesIds = buildSet {
        watchedItems.forEach { item ->
            if (item.type.isHomeSeriesLikeType() && item.season != null && item.episode != null) {
                add(item.id)
            }
        }
        progressEntries.forEach { entry ->
            if (entry.parentMetaType.isHomeSeriesLikeType() && entry.isEpisode && entry.isEffectivelyCompleted) {
                add(entry.parentMetaId)
            }
        }
    }
    if (touchedSeriesIds.isEmpty()) return
    val todayIsoDate = CurrentDateProvider.todayIsoDate()
    withContext(Dispatchers.Default) {
        items
            .filter { item -> item.id in touchedSeriesIds }
            .forEach { item ->
                val meta = runCatching {
                    MetaDetailsRepository.fetch(type = item.type, id = item.id)
                }.getOrNull() ?: return@forEach
                WatchedRepository.reconcileFullyWatchedSeriesState(
                    meta = meta,
                    todayIsoDate = todayIsoDate,
                    isEpisodeWatched = { episode ->
                        watchedItemKey(meta.type, meta.id, episode.season, episode.episode) in watchedKeys
                    },
                    isEpisodeCompleted = { episode ->
                        val playbackId = meta.episodePlaybackId(episode)
                        progressEntries.any { entry ->
                            entry.videoId == playbackId && entry.isEffectivelyCompleted
                        }
                    },
                )
            }
    }
}

private fun String.isHomeSeriesLikeType(): Boolean =
    trim().lowercase() in setOf("series", "show", "tv", "tvshow")

internal data class HomeNextUpResolutionPlan(
    val initialCandidates: List<CompletedSeriesCandidate>,
    val deferredCandidates: List<CompletedSeriesCandidate>,
)

internal fun planHomeNextUpResolutionCandidates(
    candidates: List<CompletedSeriesCandidate>,
): HomeNextUpResolutionPlan =
    HomeNextUpResolutionPlan(
        initialCandidates = candidates.take(HomeNextUpInitialResolutionLimit),
        deferredCandidates = candidates.drop(HomeNextUpInitialResolutionLimit),
    )

internal fun filterEntriesForContinueWatchingWindow(
    entries: List<WatchProgressEntry>,
    cutoffEpochMs: Long?,
): List<WatchProgressEntry> = cutoffEpochMs
    ?.let { cutoff -> entries.filter { entry -> entry.lastUpdatedEpochMs >= cutoff } }
    ?: entries

internal fun filterHomeNextUpCandidatesForContinueWatchingWindow(
    candidates: List<CompletedSeriesCandidate>,
    cutoffEpochMs: Long?,
): List<CompletedSeriesCandidate> = cutoffEpochMs
    ?.let { cutoff -> candidates.filter { candidate -> candidate.markedAtEpochMs >= cutoff } }
    ?: candidates

internal fun buildHomeNextUpSeedCandidates(
    progressEntries: List<WatchProgressEntry>,
    watchedItems: List<WatchedItem>,
    providerOwnsCompletedHistory: Boolean,
    preferFurthestEpisode: Boolean,
    nowEpochMs: Long,
    shouldUseProgressSeed: (WatchProgressEntry, Long) -> Boolean = { entry, _ ->
        entry.shouldUseAsCompletedSeedForContinueWatching()
    },
    isContentHidden: (String) -> Boolean = { false },
): List<CompletedSeriesCandidate> {
    val progressSeeds = progressEntries
        .asSequence()
        .filterNot { entry -> isContentHidden(entry.parentMetaId) }
        .filter { entry -> entry.parentMetaType.isSeriesTypeForContinueWatching() }
        .filter { entry -> entry.seasonNumber != null && entry.episodeNumber != null && entry.seasonNumber != 0 }
        .filter { entry -> !isMalformedNextUpSeedContentId(entry.parentMetaId) }
        .filter { entry -> shouldUseProgressSeed(entry, nowEpochMs) }
        .toList()
    val watchedSeeds = if (providerOwnsCompletedHistory) {
        emptyList()
    } else {
        watchedItems.filter { item ->
            !isContentHidden(item.id) &&
                item.type.isSeriesTypeForContinueWatching() &&
                item.season != null &&
                item.episode != null &&
                item.season != 0 &&
                !isMalformedNextUpSeedContentId(item.id)
        }
    }

    return WatchingState.latestCompletedBySeries(
        progressEntries = progressSeeds,
        watchedItems = watchedSeeds,
        preferFurthestEpisode = preferFurthestEpisode,
    ).mapNotNull { (content, completed) ->
        if (!content.type.isSeriesTypeForContinueWatching()) return@mapNotNull null
        if (completed.seasonNumber == 0) return@mapNotNull null
        if (isMalformedNextUpSeedContentId(content.id)) return@mapNotNull null
        CompletedSeriesCandidate(
            content = content,
            seasonNumber = completed.seasonNumber,
            episodeNumber = completed.episodeNumber,
            markedAtEpochMs = completed.markedAtEpochMs,
        )
    }.sortedWith(
        compareByDescending<CompletedSeriesCandidate> { candidate -> candidate.markedAtEpochMs }
            .thenByDescending { candidate -> candidate.seasonNumber }
            .thenByDescending { candidate -> candidate.episodeNumber },
    )
}

internal fun filterNextUpItemsByCurrentSeeds(
    nextUpItemsBySeries: Map<String, Pair<Long, ContinueWatchingItem>>,
    activeSeedContentIds: Set<String>,
    currentSeedByContentId: Map<String, Pair<Int, Int>>,
    shouldDropItemsWithoutActiveSeed: Boolean,
): Map<String, Pair<Long, ContinueWatchingItem>> =
    nextUpItemsBySeries.filter { (contentId, pair) ->
        if (shouldDropItemsWithoutActiveSeed && contentId !in activeSeedContentIds) {
            return@filter false
        }
        val item = pair.second
        val currentSeed = currentSeedByContentId[contentId] ?: return@filter true
        item.nextUpSeedSeasonNumber == currentSeed.first &&
            item.nextUpSeedEpisodeNumber == currentSeed.second
    }

internal fun isHomeNextUpSeedSourceLoaded(
    providerOwnsCompletedHistory: Boolean,
    hasLoadedRemoteProgress: Boolean,
    hasLoadedWatchedItems: Boolean,
    hasLoadedRemoteWatchedItems: Boolean,
): Boolean = hasLoadedRemoteProgress && (
    providerOwnsCompletedHistory || (hasLoadedWatchedItems && hasLoadedRemoteWatchedItems)
)

internal fun cachedNextUpHasAired(
    cached: CachedNextUpItem,
    nowEpochMs: Long = WatchProgressClock.nowEpochMs(),
): Boolean =
    parseReleaseDateToEpochMs(cached.released)
        ?.let { releaseEpochMs -> nowEpochMs >= releaseEpochMs }
        ?: cached.hasAired

internal fun hasHomeNextUpSeedChangedFromCache(
    currentSeason: Int,
    currentEpisode: Int,
    cachedSeason: Int?,
    cachedEpisode: Int?,
): Boolean {
    if (cachedSeason == null || cachedEpisode == null) return false
    return currentSeason != cachedSeason || currentEpisode != cachedEpisode
}

internal fun hasUsableHomeNextUpMetadata(item: ContinueWatchingItem): Boolean {
    val hasResolvedTitle = item.title.isNotBlank() &&
        !item.title.equals(item.parentMetaId, ignoreCase = true)
    val hasArtwork = listOf(
        item.imageUrl,
        item.poster,
        item.background,
        item.episodeThumbnail,
    ).any { value -> !value.isNullOrBlank() }
    return hasResolvedTitle && hasArtwork
}

internal fun mergeHomeNextUpItemsWithCache(
    resolvedItems: Map<String, Pair<Long, ContinueWatchingItem>>,
    cachedItems: Map<String, Pair<Long, ContinueWatchingItem>>,
    conclusivelyProcessedContentIds: Set<String>,
): Map<String, Pair<Long, ContinueWatchingItem>> {
    val retainedCachedItems = cachedItems.filterKeys { contentId ->
        contentId !in conclusivelyProcessedContentIds || contentId in resolvedItems
    }
    val resolvedItemsWithCacheFallback = resolvedItems.mapValues { (contentId, pair) ->
        pair.first to pair.second.withFallbackMetadata(cachedItems[contentId]?.second)
    }
    return retainedCachedItems + resolvedItemsWithCacheFallback
}

internal enum class HomeNextUpCandidateMetadataOutcome {
    Ready,
    Dismissed,
    Transient,
}

internal data class HomeNextUpCandidateMetadataDecision(
    val item: ContinueWatchingItem,
    val outcome: HomeNextUpCandidateMetadataOutcome,
)

internal fun classifyHomeNextUpCandidateMetadata(
    freshItem: ContinueWatchingItem,
    cachedFallbackItem: ContinueWatchingItem?,
    dismissedNextUpKeys: Set<String>,
): HomeNextUpCandidateMetadataDecision {
    val mergedItem = freshItem.withFallbackMetadata(cachedFallbackItem)
    val dismissKey = nextUpDismissKey(
        mergedItem.parentMetaId,
        mergedItem.nextUpSeedSeasonNumber,
        mergedItem.nextUpSeedEpisodeNumber,
    )
    val outcome = when {
        dismissKey in dismissedNextUpKeys -> HomeNextUpCandidateMetadataOutcome.Dismissed
        hasUsableHomeNextUpMetadata(mergedItem) -> HomeNextUpCandidateMetadataOutcome.Ready
        else -> HomeNextUpCandidateMetadataOutcome.Transient
    }
    return HomeNextUpCandidateMetadataDecision(item = mergedItem, outcome = outcome)
}

private suspend fun resolveHomeNextUpCandidate(
    completedEntry: CompletedSeriesCandidate,
    watchProgressEntries: List<WatchProgressEntry>,
    watchedItems: List<WatchedItem>,
    cachedFallbackItem: ContinueWatchingItem?,
    todayIsoDate: String,
    preferFurthestEpisode: Boolean,
    showUnairedNextUp: Boolean,
    dismissedNextUpKeys: Set<String>,
    providerOwnsCompletedHistory: Boolean,
): HomeNextUpResolutionAttempt {
    val contentId = completedEntry.content.id
    val meta = try {
        MetaDetailsRepository.fetch(
            type = completedEntry.content.type,
            id = contentId,
        )
    } catch (error: Throwable) {
        if (error is CancellationException) throw error
        null
    }
    if (meta == null) return HomeNextUpResolutionAttempt.transientFailure()

    val resolvedProgressEntries = WatchProgressRepository.prepareNextUpProgressEntries(
        entries = watchProgressEntries,
        contentId = contentId,
    )
    val resolvedWatchedItems = watchedItems
    val resolvedWatchedKeys = resolvedWatchedItems.mapTo(linkedSetOf()) { item ->
        watchedItemKey(item.type, item.id, item.season, item.episode)
    }

    if (!providerOwnsCompletedHistory) {
        WatchedRepository.reconcileFullyWatchedSeriesState(
            meta = meta,
            todayIsoDate = todayIsoDate,
            isEpisodeWatched = { episode ->
                watchedItemKey(meta.type, meta.id, episode.season, episode.episode) in resolvedWatchedKeys
            },
            isEpisodeCompleted = { episode ->
                val playbackId = meta.episodePlaybackId(episode)
                resolvedProgressEntries.any { entry ->
                    entry.videoId == playbackId && entry.isEffectivelyCompleted
                }
            },
        )
    }

    val action = meta.seriesPrimaryAction(
        content = completedEntry.content,
        entries = resolvedProgressEntries,
        watchedItems = resolvedWatchedItems,
        todayIsoDate = todayIsoDate,
        preferFurthestEpisode = preferFurthestEpisode,
        showUnairedNextUp = showUnairedNextUp,
    )
    if (action == null) return HomeNextUpResolutionAttempt.conclusiveNone()
    if (action.resumePositionMs != null) return HomeNextUpResolutionAttempt.conclusiveNone()

    val nextEpisode = meta.videoForSeriesAction(action)
    if (nextEpisode == null) return HomeNextUpResolutionAttempt.conclusiveNone()
    val metadataDecision = classifyHomeNextUpCandidateMetadata(
        freshItem = completedEntry.toContinueWatchingSeed(meta)
            .toUpNextContinueWatchingItem(nextEpisode),
        cachedFallbackItem = cachedFallbackItem,
        dismissedNextUpKeys = dismissedNextUpKeys,
    )
    val item = metadataDecision.item
    when (metadataDecision.outcome) {
        HomeNextUpCandidateMetadataOutcome.Dismissed -> return HomeNextUpResolutionAttempt.conclusiveNone()
        HomeNextUpCandidateMetadataOutcome.Transient -> return HomeNextUpResolutionAttempt.transientFailure()
        HomeNextUpCandidateMetadataOutcome.Ready -> Unit
    }

    val sortTimestamp = if (item.isReleaseAlert) {
        com.nuvio.app.features.watchprogress.parseReleaseDateToEpochMs(item.released) ?: completedEntry.markedAtEpochMs
    } else {
        completedEntry.markedAtEpochMs
    }
    return HomeNextUpResolutionAttempt.success(contentId to (sortTimestamp to item))
}

private fun MetaDetails.videoForSeriesAction(action: SeriesPrimaryAction): MetaVideo? {
    if (action.seasonNumber != null && action.episodeNumber != null) {
        videos.firstOrNull { video ->
            video.season == action.seasonNumber &&
                video.episode == action.episodeNumber
        }?.let { return it }
    }
    return videos.firstOrNull { video ->
        com.nuvio.app.features.watchprogress.buildPlaybackVideoId(
            parentMetaId = id,
            seasonNumber = video.season,
            episodeNumber = video.episode,
            fallbackVideoId = video.id,
        ) == action.videoId || video.id == action.videoId
    }
}

private fun shouldTreatAsActiveInProgressForNextUpSuppression(
    progress: WatchProgressEntry,
    latestCompletedAt: Long?,
): Boolean {
    if (!progress.shouldTreatAsInProgressForContinueWatching()) return false
    if (latestCompletedAt == null || latestCompletedAt == Long.MIN_VALUE) return true
    return progress.lastUpdatedEpochMs >= latestCompletedAt
}

private fun heroMobileBelowSectionHeightHint(
    maxWidthDp: Float,
    hasPremiumHomeRows: Boolean,
    continueWatchingVisible: Boolean,
    hasContinueWatchingItems: Boolean,
    continueWatchingStyle: ContinueWatchingSectionStyle,
    continueWatchingLayout: ContinueWatchingLayout,
    continueWatchingCardHeight: Dp,
    bottomNavigationOverlayHeight: Dp,
): Dp? {
    if (maxWidthDp >= 600f) return null
    if (hasPremiumHomeRows) {
        return 300.dp + bottomNavigationOverlayHeight
    }
    if (!continueWatchingVisible || !hasContinueWatchingItems) return null

    val sectionHeight = when (continueWatchingStyle) {
        ContinueWatchingSectionStyle.Card -> continueWatchingCardHeight + 56.dp
        ContinueWatchingSectionStyle.Wide -> continueWatchingLayout.wideCardHeight + 56.dp
        ContinueWatchingSectionStyle.Poster ->
            continueWatchingLayout.posterCardHeight + continueWatchingLayout.posterTitleBlockHeight + 70.dp
    }
    return sectionHeight + bottomNavigationOverlayHeight
}

internal fun buildHomeContinueWatchingItems(
    visibleEntries: List<WatchProgressEntry>,
    cachedInProgressByVideoId: Map<String, ContinueWatchingItem> = emptyMap(),
    nextUpItemsBySeries: Map<String, Pair<Long, ContinueWatchingItem>>,
    nextUpSuppressedSeriesIds: Set<String>? = null,
    sortMode: ContinueWatchingSortMode = ContinueWatchingSortMode.DEFAULT,
    todayIsoDate: String = "",
    cloudLibraryUiState: CloudLibraryUiState? = null,
): List<ContinueWatchingItem> {
    val suppressedSeriesIds = nextUpSuppressedSeriesIds
        ?: visibleEntries
            .asSequence()
            .filter { entry -> entry.parentMetaType.isSeriesTypeForContinueWatching() }
            .map { entry -> entry.parentMetaId }
            .filter(String::isNotBlank)
            .toSet()

    val candidates = buildList {
        addAll(
            visibleEntries.map { entry ->
                val liveItem = entry.toContinueWatchingItem()
                val cachedFallback = cachedInProgressByVideoId.fallbackFor(entry)
                HomeContinueWatchingCandidate(
                    lastUpdatedEpochMs = entry.lastUpdatedEpochMs,
                    item = liveItem
                        .withFallbackMetadata(cachedFallback)
                        .withCloudLibraryMetadata(cloudLibraryUiState),
                    isProgressEntry = true,
                )
            },
        )
        addAll(
            nextUpItemsBySeries.values.mapNotNull { (lastUpdatedEpochMs, item) ->
                if (item.parentMetaId in suppressedSeriesIds) return@mapNotNull null
                HomeContinueWatchingCandidate(
                    lastUpdatedEpochMs = lastUpdatedEpochMs,
                    item = item,
                    isProgressEntry = false,
                )
            },
        )
    }

    // Deduplicate by series/content id first (order-stable)
    val seen = mutableSetOf<String>()
    val deduplicated = candidates
        .sortedWith(
            compareByDescending<HomeContinueWatchingCandidate> { it.lastUpdatedEpochMs }
                .thenByDescending { it.isProgressEntry },
        )
        .filter { candidate -> candidate.item.shouldDisplayInContinueWatching() }
        .filter { candidate ->
            val key = candidate.item.parentMetaId.ifBlank { candidate.item.videoId }
            seen.add(key)
        }

    return when (sortMode) {
        ContinueWatchingSortMode.DEFAULT,
        ContinueWatchingSortMode.SPLIT_UPCOMING,
        -> deduplicated.map(HomeContinueWatchingCandidate::item)
        ContinueWatchingSortMode.STREAMING_STYLE -> applyStreamingStyleSort(deduplicated, todayIsoDate)
    }
}

internal fun splitUpcomingItems(
    items: List<ContinueWatchingItem>,
    mode: ContinueWatchingSortMode,
    nowEpochMs: Long = WatchProgressClock.nowEpochMs(),
): Pair<List<ContinueWatchingItem>, List<ContinueWatchingItem>> {
    if (mode != ContinueWatchingSortMode.SPLIT_UPCOMING) return items to emptyList()

    val (upcoming, main) = items.partition { item ->
        item.isNextUp && parseReleaseDateToEpochMs(item.released)
            ?.let { releaseEpochMs -> releaseEpochMs > nowEpochMs } == true
    }
    val sortedUpcoming = upcoming.sortedWith { first, second ->
        val firstRelease = parseReleaseDateToEpochMs(first.released)
        val secondRelease = parseReleaseDateToEpochMs(second.released)
        when {
            firstRelease == null && secondRelease == null -> 0
            firstRelease == null -> 1
            secondRelease == null -> -1
            else -> firstRelease.compareTo(secondRelease)
        }
    }
    return main to sortedUpcoming
}

private fun applyStreamingStyleSort(
    candidates: List<HomeContinueWatchingCandidate>,
    todayIsoDate: String,
): List<ContinueWatchingItem> {
    val (released, unreleased) = candidates.partition { candidate ->
        val item = candidate.item
        if (!item.isNextUp) {
            true // in-progress items are always "released"
        } else {
            val itemReleased = item.released
            if (itemReleased.isNullOrBlank() || todayIsoDate.isBlank()) {
                true // no date info → treat as released
            } else {
                isReleasedBy(todayIsoDate = todayIsoDate, releasedDate = itemReleased)
            }
        }
    }

    // Released: most recently watched first (already sorted by dedup pass)
    val sortedReleased = released.map(HomeContinueWatchingCandidate::item)

    // Unaired: soonest air date first; unknown dates go to the end
    val sortedUnreleased = unreleased
        .sortedWith { a, b ->
            val dateA = a.item.released?.takeIf { it.isNotBlank() }
            val dateB = b.item.released?.takeIf { it.isNotBlank() }
            when {
                dateA == null && dateB == null -> 0
                dateA == null -> 1
                dateB == null -> -1
                else -> dateA.compareTo(dateB)
            }
        }
        .map(HomeContinueWatchingCandidate::item)

    return sortedReleased + sortedUnreleased
}

internal data class CompletedSeriesCandidate(
    val content: WatchingContentRef,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val markedAtEpochMs: Long,
)

private data class HomeContinueWatchingCandidate(
    val lastUpdatedEpochMs: Long,
    val item: ContinueWatchingItem,
    val isProgressEntry: Boolean,
)

private data class HomeNextUpResolutionAttempt(
    val resolved: Pair<String, Pair<Long, ContinueWatchingItem>>?,
    val isConclusive: Boolean,
) {
    companion object {
        fun success(
            resolved: Pair<String, Pair<Long, ContinueWatchingItem>>,
        ): HomeNextUpResolutionAttempt = HomeNextUpResolutionAttempt(resolved, isConclusive = true)

        fun conclusiveNone(): HomeNextUpResolutionAttempt =
            HomeNextUpResolutionAttempt(resolved = null, isConclusive = true)

        fun transientFailure(): HomeNextUpResolutionAttempt =
            HomeNextUpResolutionAttempt(resolved = null, isConclusive = false)
    }
}

private fun saveContinueWatchingSnapshots(
    profileId: Int,
    source: WatchProgressSource,
    cacheGeneration: Int,
    nextUpItemsBySeries: Map<String, Pair<Long, ContinueWatchingItem>>,
    visibleContinueWatchingEntries: List<WatchProgressEntry>,
    todayIsoDate: String,
    seedLastWatchedMap: Map<String, Long>,
) {
    val nextUpCache = nextUpItemsBySeries.mapNotNull { (contentId, pair) ->
        val item = pair.second
        CachedNextUpItem(
            contentId = contentId,
            contentType = item.parentMetaType,
            name = item.title,
            poster = item.poster,
            backdrop = item.background,
            logo = item.logo,
            videoId = item.videoId,
            season = item.seasonNumber,
            episode = item.episodeNumber,
            episodeTitle = item.episodeTitle,
            episodeThumbnail = item.episodeThumbnail,
            pauseDescription = item.pauseDescription,
            released = item.released,
            hasAired = item.released?.let { released ->
                isReleasedBy(todayIsoDate = todayIsoDate, releasedDate = released)
            } ?: true,
            lastWatched = seedLastWatchedMap[contentId] ?: pair.first,
            sortTimestamp = pair.first,
            seedSeason = item.nextUpSeedSeasonNumber,
            seedEpisode = item.nextUpSeedEpisodeNumber,
            isReleaseAlert = item.isReleaseAlert,
            isNewSeasonRelease = item.isNewSeasonRelease,
        )
    }
    val inProgressCache = buildHomeInProgressCacheSnapshot(
        visibleEntries = visibleContinueWatchingEntries,
        cachedEntries = ContinueWatchingEnrichmentCache.getInProgressSnapshot(
            profileId = profileId,
            source = source,
        ),
        resolvedItemsByProgressKey = visibleContinueWatchingEntries.mapNotNull { entry ->
            entry.peekResolvedContinueWatchingItem()?.let { item -> entry.resolvedProgressKey() to item }
        }.toMap(),
    )
    ContinueWatchingEnrichmentCache.saveSnapshots(
        profileId = profileId,
        source = source,
        generation = cacheGeneration,
        nextUp = nextUpCache,
        inProgress = inProgressCache,
    )
}

internal fun buildHomeInProgressCacheSnapshot(
    visibleEntries: List<WatchProgressEntry>,
    cachedEntries: List<CachedInProgressItem>,
    resolvedItemsByProgressKey: Map<String, ContinueWatchingItem> = emptyMap(),
): List<CachedInProgressItem> {
    val cachedByProgressKey = cachedEntries.associateBy(CachedInProgressItem::resolvedProgressKey)
    return visibleEntries.map { entry ->
        val progressKey = entry.resolvedProgressKey()
        val fallbackItem = resolvedItemsByProgressKey[progressKey]
            ?.withFallbackMetadata(cachedByProgressKey[progressKey]?.toContinueWatchingItem())
            ?: cachedByProgressKey[progressKey]?.toContinueWatchingItem()
        val item = entry
            .toContinueWatchingItem()
            .withFallbackMetadata(fallbackItem)
        CachedInProgressItem(
            contentId = entry.parentMetaId,
            contentType = entry.contentType,
            name = item.title,
            poster = item.poster,
            backdrop = item.background,
            logo = item.logo,
            videoId = entry.videoId,
            season = entry.seasonNumber,
            episode = entry.episodeNumber,
            episodeTitle = item.episodeTitle,
            episodeThumbnail = item.episodeThumbnail,
            pauseDescription = item.pauseDescription,
            position = entry.lastPositionMs,
            duration = entry.durationMs,
            lastWatched = entry.lastUpdatedEpochMs,
            progressPercent = entry.progressPercent,
            progressKey = progressKey,
        )
    }
}

private fun CompletedSeriesCandidate.toContinueWatchingSeed(meta: com.nuvio.app.features.details.MetaDetails) =
    WatchProgressEntry(
        contentType = content.type,
        parentMetaId = content.id,
        parentMetaType = content.type,
        videoId = "${content.id}:${seasonNumber}:${episodeNumber}",
        title = meta.name,
        logo = meta.logo,
        poster = meta.poster,
        background = meta.background,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        lastPositionMs = 0L,
        durationMs = 0L,
        lastUpdatedEpochMs = markedAtEpochMs,
        isCompleted = true,
    )

private fun ContinueWatchingItem.shouldDisplayInContinueWatching(): Boolean =
    isNextUp || progressFraction < 0.995f

private fun CachedNextUpItem.toContinueWatchingItem(): ContinueWatchingItem? {
    val alertState = com.nuvio.app.features.watchprogress.calculateReleaseAlertState(
        seedLastUpdatedEpochMs = lastWatched,
        seedSeasonNumber = seedSeason,
        nextSeasonNumber = season,
        releasedIso = released,
    )
    val resolvedPoster = poster.nonBlankOrNull()
    val resolvedBackdrop = backdrop.nonBlankOrNull()
    val resolvedEpisodeThumbnail = episodeThumbnail.nonBlankOrNull()
    return ContinueWatchingItem(
        parentMetaId = contentId,
        parentMetaType = contentType,
        videoId = videoId,
        title = name,
        subtitle = buildContinueWatchingEpisodeSubtitle(
            seasonNumber = season,
            episodeNumber = episode,
            episodeTitle = episodeTitle,
        ),
        imageUrl = resolvedEpisodeThumbnail ?: resolvedBackdrop ?: resolvedPoster,
        logo = logo.nonBlankOrNull(),
        poster = resolvedPoster,
        background = resolvedBackdrop,
        seasonNumber = season,
        episodeNumber = episode,
        episodeTitle = episodeTitle.nonBlankOrNull(),
        episodeThumbnail = resolvedEpisodeThumbnail,
        pauseDescription = pauseDescription.nonBlankOrNull(),
        released = released.nonBlankOrNull(),
        isNextUp = true,
        nextUpSeedSeasonNumber = seedSeason,
        nextUpSeedEpisodeNumber = seedEpisode,
        resumePositionMs = 0L,
        resumeProgressFraction = null,
        durationMs = 0L,
        progressFraction = 0f,
        isReleaseAlert = alertState.isReleaseAlert,
        isNewSeasonRelease = alertState.isNewSeasonRelease,
    )
}

private fun CachedInProgressItem.toContinueWatchingItem(): ContinueWatchingItem {
    val explicitResumeProgressFraction = progressPercent
        ?.takeIf { duration <= 0L && it > 0f }
        ?.let { (it / 100f).coerceIn(0f, 1f) }
    val normalizedProgressFraction = progressPercent
        ?.let { (it / 100f).coerceIn(0f, 1f) }
        ?: if (duration > 0L) {
            (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    val resolvedPoster = poster.nonBlankOrNull()
    val resolvedBackdrop = backdrop.nonBlankOrNull()
    val resolvedEpisodeThumbnail = episodeThumbnail.nonBlankOrNull()

    return ContinueWatchingItem(
        parentMetaId = contentId,
        parentMetaType = contentType,
        videoId = videoId,
        title = name,
        subtitle = buildContinueWatchingEpisodeSubtitle(
            seasonNumber = season,
            episodeNumber = episode,
            episodeTitle = episodeTitle,
        ),
        imageUrl = resolvedEpisodeThumbnail ?: resolvedBackdrop ?: resolvedPoster,
        logo = logo.nonBlankOrNull(),
        poster = resolvedPoster,
        background = resolvedBackdrop,
        seasonNumber = season,
        episodeNumber = episode,
        episodeTitle = episodeTitle.nonBlankOrNull(),
        episodeThumbnail = resolvedEpisodeThumbnail,
        pauseDescription = pauseDescription.nonBlankOrNull(),
        isNextUp = false,
        nextUpSeedSeasonNumber = null,
        nextUpSeedEpisodeNumber = null,
        resumePositionMs = if (explicitResumeProgressFraction != null) 0L else position,
        resumeProgressFraction = explicitResumeProgressFraction,
        durationMs = duration,
        progressFraction = normalizedProgressFraction,
    )
}

private fun ContinueWatchingItem.withFallbackMetadata(
    fallback: ContinueWatchingItem?,
): ContinueWatchingItem {
    val nonBlankFallbackTitle = fallback?.title?.takeIf { it.isNotBlank() }
    val fallbackTitle = nonBlankFallbackTitle
        ?.takeUnless { fallback.hasPlaceholderProgressTitle() }

    return copy(
        title = when {
            title.isBlank() && nonBlankFallbackTitle != null -> nonBlankFallbackTitle
            hasPlaceholderProgressTitle() && fallbackTitle != null -> fallbackTitle
            else -> title
        },
        subtitle = when {
            subtitle.isBlank() -> fallback?.subtitle?.takeIf { it.isNotBlank() }.orEmpty()
            fallback?.subtitle.isNullOrBlank() -> subtitle
            else -> fallback.subtitle
        },
        imageUrl = imageUrl.orNonBlank(fallback?.imageUrl),
        logo = logo.orNonBlank(fallback?.logo),
        poster = poster.orNonBlank(fallback?.poster),
        background = background.orNonBlank(fallback?.background),
        episodeTitle = episodeTitle.orNonBlank(fallback?.episodeTitle),
        episodeThumbnail = episodeThumbnail.orNonBlank(fallback?.episodeThumbnail),
        pauseDescription = pauseDescription.orNonBlank(fallback?.pauseDescription),
        released = released.orNonBlank(fallback?.released),
    )
}

private fun String?.nonBlankOrNull(): String? = this?.takeIf { it.isNotBlank() }

private fun String?.orNonBlank(fallback: String?): String? =
    nonBlankOrNull() ?: fallback.nonBlankOrNull()

private fun ContinueWatchingItem.withCloudLibraryMetadata(
    cloudLibraryUiState: CloudLibraryUiState?,
): ContinueWatchingItem {
    if (!isCloudLibraryContinueWatchingItem() || cloudLibraryUiState == null) return this
    val target = cloudLibraryUiState.findPlaybackTargetForProgress(
        contentId = parentMetaId,
        videoId = videoId,
    ) ?: return this
    val fileName = target.file.name.trim().takeIf { it.isNotBlank() }
        ?: target.item.name.trim().takeIf { it.isNotBlank() }
        ?: return this
    return copy(
        title = fileName,
        pauseDescription = pauseDescription
            ?: target.item.name.takeIf { itemName -> itemName.isNotBlank() && itemName != fileName },
    )
}

private fun ContinueWatchingItem.hasPlaceholderCloudTitle(): Boolean {
    if (!isCloudLibraryContinueWatchingItem()) return false
    val normalizedTitle = title.trim()
    return normalizedTitle.equals(parentMetaId, ignoreCase = true) ||
        normalizedTitle.equals(videoId, ignoreCase = true)
}

private fun ContinueWatchingItem.hasPlaceholderProgressTitle(): Boolean {
    val normalizedTitle = title.trim()
    if (normalizedTitle.isEmpty()) return true
    return hasPlaceholderCloudTitle() ||
        normalizedTitle.isRawMetadataTitle() ||
        normalizedTitle.equals(parentMetaId.trim(), ignoreCase = true) ||
        normalizedTitle.equals(videoId.trim(), ignoreCase = true)
}

private fun String.isRawMetadataTitle(): Boolean {
    val value = trim()
    if (value.isEmpty()) return true
    val lower = value.lowercase()
    return Regex("^tt\\d{5,}$").matches(lower) ||
        Regex("^(imdb|tmdb|trakt)[:_-]?\\d+$").matches(lower) ||
        Regex("^\\d{6,}$").matches(lower)
}

private fun WatchProgressEntry.continueWatchingFallbackKeys(): List<String> =
    buildList {
        resolvedProgressKey().takeIf(String::isNotBlank)?.let(::add)
        videoId.trim().takeIf(String::isNotBlank)?.let(::add)
        parentMetaId.trim().takeIf(String::isNotBlank)?.let { contentId ->
            add(contentId)
            parentMetaType.trim().lowercase().takeIf(String::isNotBlank)?.let { type ->
                add("$type:$contentId")
            }
            if (isEpisode || parentMetaType.isSeriesTypeForContinueWatching()) {
                add("series:$contentId")
            }
        }
    }.distinct()

private fun ContinueWatchingItem.continueWatchingFallbackKeys(): List<String> =
    buildList {
        videoId.trim().takeIf(String::isNotBlank)?.let(::add)
        parentMetaId.trim().takeIf(String::isNotBlank)?.let { contentId ->
            add(contentId)
            parentMetaType.trim().lowercase().takeIf(String::isNotBlank)?.let { type ->
                add("$type:$contentId")
            }
            if (parentMetaType.isSeriesTypeForContinueWatching()) {
                add("series:$contentId")
            }
        }
    }.distinct()

private fun MutableMap<String, ContinueWatchingItem>.putContinueWatchingFallbackAliases(
    videoId: String,
    contentId: String,
    contentType: String,
    item: ContinueWatchingItem,
) {
    buildList {
        videoId.trim().takeIf(String::isNotBlank)?.let(::add)
        contentId.trim().takeIf(String::isNotBlank)?.let { id ->
            add(id)
            contentType.trim().lowercase().takeIf(String::isNotBlank)?.let { type ->
                add("$type:$id")
            }
            if (contentType.isSeriesTypeForContinueWatching()) {
                add("series:$id")
            }
        }
    }.distinct().forEach { key -> put(key, item) }
}

private fun Map<String, ContinueWatchingItem>.fallbackFor(entry: WatchProgressEntry): ContinueWatchingItem? =
    entry.continueWatchingFallbackKeys().firstNotNullOfOrNull { key -> this[key] }

private fun WatchProgressEntry.needsContinueWatchingMetadataResolution(): Boolean {
    if (isCloudLibraryProgressEntry()) return false
    if (parentMetaId.isBlank() || isMalformedNextUpSeedContentId(parentMetaId)) return false
    val item = toContinueWatchingItem()
    return item.hasPlaceholderProgressTitle() ||
        item.imageUrl == null ||
        item.logo == null && item.poster == null && item.background == null
}

private suspend fun resolveContinueWatchingEntryMetadata(entry: WatchProgressEntry): ContinueWatchingItem? {
    if (!entry.needsContinueWatchingMetadataResolution()) return null
    val meta = MetaDetailsRepository.fetch(type = entry.parentMetaType, id = entry.parentMetaId)
        ?: return null
    return entry.toContinueWatchingItem().withMetaDetailsMetadata(meta)
}

private fun WatchProgressEntry.peekResolvedContinueWatchingItem(): ContinueWatchingItem? =
    MetaDetailsRepository.peek(type = parentMetaType, id = parentMetaId)
        ?.let { meta -> toContinueWatchingItem().withMetaDetailsMetadata(meta) }

private fun ContinueWatchingItem.withMetaDetailsMetadata(meta: MetaDetails): ContinueWatchingItem {
    val matchingEpisode = meta.videos.firstOrNull { video ->
        video.season == seasonNumber && video.episode == episodeNumber
    }
    val resolvedTitle = meta.name.takeIf { it.isNotBlank() } ?: title
    val resolvedEpisodeTitle = episodeTitle ?: matchingEpisode?.title
    val resolvedEpisodeThumbnail = episodeThumbnail ?: matchingEpisode?.thumbnail
    val resolvedBackground = background ?: meta.background
    val resolvedPoster = poster ?: meta.poster

    return copy(
        title = when {
            hasPlaceholderProgressTitle() -> resolvedTitle
            else -> title
        },
        subtitle = subtitle.ifBlank {
            buildContinueWatchingEpisodeSubtitle(
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                episodeTitle = resolvedEpisodeTitle,
            )
        },
        imageUrl = imageUrl ?: resolvedEpisodeThumbnail ?: resolvedBackground ?: resolvedPoster,
        logo = logo ?: meta.logo,
        poster = resolvedPoster,
        background = resolvedBackground,
        episodeTitle = resolvedEpisodeTitle,
        episodeThumbnail = resolvedEpisodeThumbnail,
        pauseDescription = pauseDescription ?: matchingEpisode?.overview ?: meta.description,
        released = released ?: matchingEpisode?.released,
    )
}

private fun ContinueWatchingItem.isCloudLibraryContinueWatchingItem(): Boolean =
    parentMetaType.equals(CloudLibraryContentType, ignoreCase = true)

private fun WatchProgressEntry.isCloudLibraryProgressEntry(): Boolean =
    contentType.equals(CloudLibraryContentType, ignoreCase = true) ||
        parentMetaType.equals(CloudLibraryContentType, ignoreCase = true)
