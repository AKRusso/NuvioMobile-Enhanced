package com.nuvio.app.features.library

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ViewAgenda
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.core.i18n.localizedByteUnit
import com.nuvio.app.core.i18n.localizedMonthName
import com.nuvio.app.core.i18n.localizedSeasonEpisodeCode
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.ui.NuvioBottomSheetDivider
import com.nuvio.app.core.ui.NuvioBottomSheetActionRow
import com.nuvio.app.core.ui.NuvioMediaActionOverlay
import com.nuvio.app.core.ui.PosterLandscapeAspectRatio
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.dismissNuvioBottomSheet
import com.nuvio.app.core.ui.nuvioSafeBottomPadding
import com.nuvio.app.features.watchprogress.CurrentDateProvider
import com.nuvio.app.core.network.NetworkCondition
import com.nuvio.app.core.network.NetworkStatusRepository
import com.nuvio.app.core.ui.DisintegratingContainer
import com.nuvio.app.core.ui.NuvioDropdownChip
import com.nuvio.app.core.ui.NuvioDropdownOption
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.LocalTvLayoutProfile
import com.nuvio.app.core.ui.NuvioNetworkOfflineCard
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.core.ui.NuvioViewAllPillSize
import com.nuvio.app.core.ui.NuvioShelfSection
import com.nuvio.app.core.ui.ScopedDisintegrationTracker
import com.nuvio.app.core.ui.nuvioConsumePointerEvents
import com.nuvio.app.features.cloud.CloudLibraryFile
import com.nuvio.app.features.cloud.CloudLibraryItem
import com.nuvio.app.features.cloud.CloudLibraryItemType
import com.nuvio.app.features.cloud.CloudLibraryRepository
import com.nuvio.app.features.cloud.CloudLibraryUiState
import com.nuvio.app.features.debrid.DebridSettingsRepository
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.downloads.DownloadItem
import com.nuvio.app.features.downloads.DownloadStatus
import com.nuvio.app.features.downloads.DownloadsRepository
import com.nuvio.app.features.downloads.DownloadsUiState
import com.nuvio.app.features.downloads.downloadSizeLabel
import com.nuvio.app.features.downloads.sortedForSeriesDownloads
import com.nuvio.app.features.home.libraryItemKeyForHomeRadar
import com.nuvio.app.features.home.homeRadarDetailsRequestKey
import com.nuvio.app.features.home.components.HomeEmptyStateCard
import com.nuvio.app.features.home.components.HomePosterCard
import com.nuvio.app.features.home.components.HomeSkeletonRow
import com.nuvio.app.features.home.components.posterGridColumnCountForWidth
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.tracking.TrackingRefreshIntent
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.watching.application.WatchingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    scrollToTopRequests: Flow<Unit> = emptyFlow(),
    onPosterClick: ((LibraryItem) -> Unit)? = null,
    onPosterLongClick: ((LibraryItem, LibrarySection) -> Unit)? = null,
    onSectionViewAllClick: ((LibrarySection, LibrarySortOption) -> Unit)? = null,
    onCloudFilePlay: ((CloudLibraryItem, CloudLibraryFile) -> Unit)? = null,
    onConnectCloudClick: (() -> Unit)? = null,
    onDownloadsClick: (() -> Unit)? = null,
    onOpenDownload: ((DownloadItem) -> Unit)? = null,
) {
    val uiState by remember {
        LibraryRepository.ensureLoaded()
        LibraryRepository.uiState
    }.collectAsStateWithLifecycle()
    val cloudUiState by CloudLibraryRepository.uiState.collectAsStateWithLifecycle()
    val cloudSettings by remember {
        DebridSettingsRepository.ensureLoaded()
        DebridSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val watchedUiState by remember {
        WatchedRepository.ensureLoaded()
        WatchedRepository.uiState
    }.collectAsStateWithLifecycle()
    val fullyWatchedSeriesKeys by WatchedRepository.fullyWatchedSeriesKeys.collectAsStateWithLifecycle()
    val displaySettings by remember {
        LibraryDisplaySettingsRepository.ensureLoaded()
        LibraryDisplaySettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val downloadsUiState by remember {
        DownloadsRepository.ensureLoaded()
        DownloadsRepository.uiState
    }.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        DownloadsRepository.removeMissingCompletedDownloads()
    }
    val networkStatusUiState by NetworkStatusRepository.uiState.collectAsStateWithLifecycle()
    var observedOfflineState by remember { mutableStateOf(false) }
    var sourceModeName by rememberSaveable { mutableStateOf(LibraryViewMode.Saved.name) }
    val sourceMode = remember(sourceModeName) {
        runCatching { LibraryViewMode.valueOf(sourceModeName) }.getOrDefault(LibraryViewMode.Saved)
    }
    var downloadsFilterName by rememberSaveable { mutableStateOf(LibraryDownloadsFilter.All.name) }
    val downloadsFilter = remember(downloadsFilterName) {
        runCatching { LibraryDownloadsFilter.valueOf(downloadsFilterName) }
            .getOrDefault(LibraryDownloadsFilter.All)
    }
    var selectedProviderId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTypeName by rememberSaveable { mutableStateOf<String?>(null) }
    var cloudSearchQuery by rememberSaveable { mutableStateOf("") }
    val selectedType = remember(selectedTypeName) {
        selectedTypeName?.let { runCatching { CloudLibraryItemType.valueOf(it) }.getOrNull() }
    }
    var selectedCloudItemKey by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDownloadId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedLibrarySectionKey by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedLibraryType by rememberSaveable { mutableStateOf<String?>(null) }
    var showReleaseCalendar by rememberSaveable { mutableStateOf(false) }
    val releaseCalendarItemsKey = remember(uiState.items) { libraryCalendarItemsCacheKey(uiState.items) }
    val releaseCalendarFallbackEvents = remember(releaseCalendarItemsKey) {
        buildLibraryReleaseCalendarFallbackEvents(uiState.items)
    }
    var releaseCalendarEvents by remember { mutableStateOf(releaseCalendarFallbackEvents) }
    var releaseCalendarLoading by remember { mutableStateOf(false) }
    var releaseCalendarLoadedKey by remember { mutableStateOf<String?>(null) }
    val releaseRadarDetailsRequestKey = remember(uiState.items) { uiState.items.homeRadarDetailsRequestKey() }
    val releaseSupportProfileId = ProfileRepository.activeProfileId
    val releaseSupportCacheKey = remember(releaseCalendarItemsKey, releaseRadarDetailsRequestKey) {
        libraryReleaseSupportCacheKey(
            calendarItemsKey = releaseCalendarItemsKey,
            radarDetailsRequestKey = releaseRadarDetailsRequestKey,
        )
    }
    var releaseRadarDetailsByKey by remember { mutableStateOf<Map<String, MetaDetails>>(emptyMap()) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val isRemoteSource = uiState.sourceMode != LibrarySourceMode.LOCAL
    val effectiveSortOption = effectiveLibrarySortOption(
        selected = displaySettings.sortOption,
        sourceMode = uiState.sourceMode,
    )
    val sortedSections = remember(uiState.sections, displaySettings.sortOption, uiState.sourceMode) {
        sortLibrarySections(
            sections = uiState.sections,
            selected = displaySettings.sortOption,
            sourceMode = uiState.sourceMode,
        )
    }
    val verticalProjection = remember(
        uiState.sections,
        uiState.sourceMode,
        selectedLibrarySectionKey,
        selectedLibraryType,
        displaySettings.sortOption,
    ) {
        buildLibraryVerticalProjection(
            sections = uiState.sections,
            sourceMode = uiState.sourceMode,
            selectedSectionKey = selectedLibrarySectionKey,
            selectedType = selectedLibraryType,
            sortOption = displaySettings.sortOption,
        )
    }
    val enhancedContent = rememberLibraryEnhancedContent(uiState.items)
    val retryLibraryLoad: () -> Unit = {
        NetworkStatusRepository.requestRefresh(force = true)
        coroutineScope.launch {
            LibraryRepository.pullFromServer(
                profileId = ProfileRepository.activeProfileId,
                refreshIntent = TrackingRefreshIntent.USER_INITIATED,
            )
        }
    }

    LaunchedEffect(networkStatusUiState.condition, isRemoteSource) {
        when (networkStatusUiState.condition) {
            NetworkCondition.NoInternet,
            NetworkCondition.ServersUnreachable,
            -> {
                observedOfflineState = true
            }

            NetworkCondition.Online -> {
                if (!observedOfflineState) return@LaunchedEffect
                observedOfflineState = false
                if (isRemoteSource) {
                    coroutineScope.launch {
                        LibraryRepository.pullFromServer(ProfileRepository.activeProfileId)
                    }
                }
            }

            NetworkCondition.Unknown,
            NetworkCondition.Checking,
            -> Unit
        }
    }

    LaunchedEffect(scrollToTopRequests) {
        scrollToTopRequests.collect {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(sourceMode, cloudSettings.cloudLibraryEnabled, cloudSettings.providerApiKeys) {
        if (sourceMode == LibraryViewMode.Cloud) {
            CloudLibraryRepository.ensureLoaded()
            selectedCloudItemKey = null
        }
    }

    LaunchedEffect(releaseSupportProfileId, releaseSupportCacheKey, releaseCalendarFallbackEvents) {
        val cachedDetails = loadLibraryReleaseSupportCache(
            profileId = releaseSupportProfileId,
            cacheKey = releaseSupportCacheKey,
        )?.detailsByKey.orEmpty()
        releaseCalendarEvents = if (cachedDetails.isNotEmpty()) {
            buildLibraryReleaseCalendarEventsFromDetails(uiState.items, cachedDetails)
        } else {
            releaseCalendarFallbackEvents
        }
        releaseCalendarLoading = false
        releaseCalendarLoadedKey = null
    }

    LaunchedEffect(showReleaseCalendar, releaseSupportProfileId, releaseSupportCacheKey) {
        if (!showReleaseCalendar) return@LaunchedEffect
        val itemsSnapshot = uiState.items
        if (releaseCalendarLoadedKey == releaseSupportCacheKey) return@LaunchedEffect
        val cachedPayload = loadLibraryReleaseSupportCache(releaseSupportProfileId, releaseSupportCacheKey)
        val cachedDetails = cachedPayload?.detailsByKey.orEmpty()
        releaseCalendarEvents = if (cachedDetails.isNotEmpty()) {
            buildLibraryReleaseCalendarEventsFromDetails(itemsSnapshot, cachedDetails)
        } else {
            releaseCalendarFallbackEvents
        }
        if (itemsSnapshot.isEmpty()) {
            releaseCalendarLoadedKey = releaseSupportCacheKey
            return@LaunchedEffect
        }
        if (cachedPayload?.isFresh() == true) {
            releaseCalendarLoadedKey = releaseSupportCacheKey
            return@LaunchedEffect
        }
        releaseCalendarLoading = cachedPayload == null
        try {
            val resolvedDetails = resolveLibraryReleaseRadarDetails(itemsSnapshot)
            val nextDetails = if (cachedDetails.isNotEmpty()) cachedDetails + resolvedDetails else resolvedDetails
            if (nextDetails.isNotEmpty()) {
                saveLibraryReleaseSupportCache(releaseSupportProfileId, releaseSupportCacheKey, nextDetails)
            }
            releaseCalendarEvents = buildLibraryReleaseCalendarEventsFromDetails(itemsSnapshot, nextDetails)
            releaseCalendarLoadedKey = releaseSupportCacheKey
        } finally {
            releaseCalendarLoading = false
        }
    }

    LaunchedEffect(sourceMode, releaseSupportProfileId, releaseSupportCacheKey) {
        if (sourceMode == LibraryViewMode.Cloud || releaseRadarDetailsRequestKey.isBlank()) {
            releaseRadarDetailsByKey = emptyMap()
            return@LaunchedEffect
        }
        val cachedPayload = loadLibraryReleaseSupportCache(releaseSupportProfileId, releaseSupportCacheKey)
        val cachedDetails = cachedPayload?.detailsByKey.orEmpty()
        if (cachedDetails.isNotEmpty()) {
            releaseRadarDetailsByKey = cachedDetails
            if (cachedPayload?.isFresh() == true) return@LaunchedEffect
        }
        val resolvedDetails = withContext(Dispatchers.Default) {
            resolveLibraryReleaseRadarDetails(uiState.items)
        }
        val nextDetails = if (cachedDetails.isNotEmpty()) cachedDetails + resolvedDetails else resolvedDetails
        if (nextDetails.isNotEmpty()) {
            releaseRadarDetailsByKey = nextDetails
            saveLibraryReleaseSupportCache(releaseSupportProfileId, releaseSupportCacheKey, nextDetails)
        }
    }

    val disintegration = remember { LibraryDisintegrationHolder() }
    val librarySectionsDisplay = if (
        sourceMode == LibraryViewMode.Saved &&
        displaySettings.layoutMode == LibraryLayoutMode.HORIZONTAL &&
        uiState.isLoaded &&
        sortedSections.isNotEmpty()
    ) {
        disintegration.sync(
            sourceMode = uiState.sourceMode,
            sections = sortedSections,
            previewLimit = LIBRARY_SECTION_PREVIEW_LIMIT,
        )
    } else {
        disintegration.reset()
        emptyList()
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val tvLayout = LocalTvLayoutProfile.current.enabled
        val gridColumns = remember(maxWidth, tvLayout) { posterGridColumnCountForWidth(maxWidth, tvLayout) }

        NuvioScreen(
            modifier = Modifier.fillMaxSize(),
            horizontalPadding = 0.dp,
            listState = listState,
        ) {
            stickyHeader {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(MaterialTheme.colorScheme.background)
                            .nuvioConsumePointerEvents(),
                    )
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        NuvioScreenHeader(
                            title = if (sourceMode == LibraryViewMode.Cloud) {
                                stringResource(Res.string.library_title)
                            } else if (sourceMode == LibraryViewMode.Downloads) {
                                stringResource(Res.string.compose_settings_root_downloads_title)
                            } else {
                                when (uiState.sourceMode) {
                                    LibrarySourceMode.LOCAL -> stringResource(Res.string.library_title)
                                    LibrarySourceMode.TRAKT -> stringResource(Res.string.library_trakt_title)
                                    LibrarySourceMode.SIMKL -> stringResource(Res.string.library_simkl_title)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp),
                            actions = {
                                if (sourceMode == LibraryViewMode.Saved) {
                                    val targetLayout = if (displaySettings.layoutMode == LibraryLayoutMode.HORIZONTAL) {
                                        LibraryLayoutMode.VERTICAL
                                    } else {
                                        LibraryLayoutMode.HORIZONTAL
                                    }
                                    IconButton(
                                        onClick = {
                                            LibraryDisplaySettingsRepository.setLayoutMode(targetLayout)
                                        },
                                    ) {
                                        Crossfade(
                                            targetState = targetLayout,
                                            animationSpec = tween(durationMillis = 140),
                                            label = "libraryLayoutAction",
                                        ) { animatedTargetLayout ->
                                            Icon(
                                                imageVector = if (animatedTargetLayout == LibraryLayoutMode.VERTICAL) {
                                                    Icons.Rounded.GridView
                                                } else {
                                                    Icons.Rounded.ViewAgenda
                                                },
                                                contentDescription = if (animatedTargetLayout == LibraryLayoutMode.VERTICAL) {
                                                    stringResource(Res.string.library_layout_show_vertical)
                                                } else {
                                                    stringResource(Res.string.library_layout_show_horizontal)
                                                },
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    IconButton(onClick = { showReleaseCalendar = true }) {
                                        Icon(
                                            imageVector = Icons.Rounded.CalendarMonth,
                                            contentDescription = stringResource(Res.string.library_calendar_open),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            },
                        )
                        LibrarySourceSwitch(
                            selectedMode = sourceMode,
                            onModeSelected = { mode ->
                                sourceModeName = mode.name
                            },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            if (sourceMode == LibraryViewMode.Cloud) {
                cloudLibraryContent(
                    uiState = cloudUiState,
                    selectedProviderId = selectedProviderId,
                    selectedType = selectedType,
                    selectedCloudItemKey = selectedCloudItemKey,
                    searchQuery = cloudSearchQuery,
                    onSearchQueryChange = {
                        cloudSearchQuery = it
                        selectedCloudItemKey = null
                    },
                    onProviderSelected = {
                        selectedProviderId = it
                        selectedTypeName = null
                        selectedCloudItemKey = null
                    },
                    onTypeSelected = {
                        selectedTypeName = it?.name
                        selectedCloudItemKey = null
                    },
                    onItemSelected = { item ->
                        val playableFiles = item.playableFiles
                        when {
                            playableFiles.size == 1 -> onCloudFilePlay?.invoke(item, playableFiles.first())
                            playableFiles.size > 1 -> selectedCloudItemKey = item.stableKey
                        }
                    },
                    onFileSelected = { item, file -> onCloudFilePlay?.invoke(item, file) },
                    onBackToItems = { selectedCloudItemKey = null },
                    onRefresh = { CloudLibraryRepository.refresh() },
                    onConnectCloudClick = onConnectCloudClick,
                )
            } else if (sourceMode == LibraryViewMode.Downloads) {
                downloadsLibraryContent(
                    uiState = downloadsUiState,
                    showHeaderAccent = true,
                    selectedFilter = downloadsFilter,
                    onFilterSelected = { downloadsFilterName = it.name },
                    onOpenDownload = onOpenDownload,
                    onDownloadsClick = onDownloadsClick,
                    onSelectDownload = { selectedDownloadId = it.id },
                )
            } else {
                when {
                    !uiState.isLoaded || (uiState.isLoading && uiState.sections.isEmpty()) -> {
                        if (displaySettings.layoutMode == LibraryLayoutMode.VERTICAL) {
                            libraryVerticalSkeletonItems(gridColumns)
                        } else {
                            items(3) {
                                HomeSkeletonRow(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                    }

                    !uiState.errorMessage.isNullOrBlank() && uiState.sections.isEmpty() -> {
                        item {
                            if (networkStatusUiState.isOfflineLike) {
                                NuvioNetworkOfflineCard(
                                    condition = networkStatusUiState.condition,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    onRetry = retryLibraryLoad,
                                )
                            } else {
                                HomeEmptyStateCard(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    title = when (uiState.sourceMode) {
                                        LibrarySourceMode.LOCAL -> stringResource(Res.string.library_load_failed)
                                        LibrarySourceMode.TRAKT -> stringResource(Res.string.library_trakt_load_failed)
                                        LibrarySourceMode.SIMKL -> stringResource(Res.string.library_simkl_load_failed)
                                    },
                                    message = uiState.errorMessage.orEmpty(),
                                    actionLabel = stringResource(Res.string.action_retry),
                                    onActionClick = retryLibraryLoad,
                                )
                            }
                        }
                    }

                    uiState.sections.isEmpty() -> {
                        item {
                            if (networkStatusUiState.isOfflineLike && isRemoteSource) {
                                NuvioNetworkOfflineCard(
                                    condition = networkStatusUiState.condition,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    onRetry = retryLibraryLoad,
                                )
                            } else {
                                HomeEmptyStateCard(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    title = when (uiState.sourceMode) {
                                        LibrarySourceMode.LOCAL -> stringResource(Res.string.library_empty_title)
                                        LibrarySourceMode.TRAKT -> stringResource(Res.string.library_trakt_empty_title)
                                        LibrarySourceMode.SIMKL -> stringResource(Res.string.library_simkl_empty_title)
                                    },
                                    message = when (uiState.sourceMode) {
                                        LibrarySourceMode.LOCAL -> stringResource(Res.string.library_empty_message)
                                        LibrarySourceMode.TRAKT -> stringResource(Res.string.library_trakt_empty_message)
                                        LibrarySourceMode.SIMKL -> stringResource(Res.string.library_simkl_empty_message)
                                    },
                                )
                            }
                        }
                    }

                    else -> {
                        item(
                            key = "library-saved-controls:${uiState.sourceMode}:" +
                                "${displaySettings.layoutMode}:$effectiveSortOption",
                        ) {
                            LibrarySavedControls(
                                layoutMode = displaySettings.layoutMode,
                                sourceMode = uiState.sourceMode,
                                sortOption = effectiveSortOption,
                                verticalProjection = verticalProjection,
                                onSectionSelected = { sectionKey ->
                                    selectedLibrarySectionKey = sectionKey
                                    selectedLibraryType = null
                                },
                                onTypeSelected = { type -> selectedLibraryType = type },
                                onSortSelected = LibraryDisplaySettingsRepository::setSortOption,
                                modifier = libraryContentTransitionModifier()
                                    .padding(horizontal = 16.dp),
                            )
                        }
                        when (displaySettings.layoutMode) {
                            LibraryLayoutMode.HORIZONTAL -> librarySections(
                                displaySections = librarySectionsDisplay,
                                watchedKeys = watchedUiState.watchedKeys,
                                fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                                sortOption = effectiveSortOption,
                                onPosterClick = onPosterClick,
                                onSectionViewAllClick = onSectionViewAllClick,
                                onPosterLongClick = onPosterLongClick,
                                onDisintegrated = disintegration::onExited,
                            )
                            LibraryLayoutMode.VERTICAL -> libraryVerticalContent(
                                projection = verticalProjection,
                                columns = gridColumns,
                                watchedKeys = watchedUiState.watchedKeys,
                                fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                                onPosterClick = onPosterClick,
                                onPosterLongClick = onPosterLongClick,
                            )
                        }
                        libraryEnhancedSections(
                            content = enhancedContent,
                            onPosterClick = onPosterClick,
                        )
                    }
                }
            }
        }
    }

    if (showReleaseCalendar) {
        LibraryReleaseCalendarSheet(
            events = releaseCalendarEvents,
            onDismiss = { showReleaseCalendar = false },
            onPosterClick = onPosterClick,
        )
    }

    val selectedDownload = selectedDownloadId?.let { downloadId ->
        downloadsUiState.items.firstOrNull { it.id == downloadId }
    }
    if (selectedDownload != null) {
        LibraryDownloadActionSheet(
            item = selectedDownload,
            onDismiss = { selectedDownloadId = null },
            onPlay = {
                onOpenDownload?.invoke(selectedDownload)
                selectedDownloadId = null
            },
            onRemove = {
                DownloadsRepository.cancelDownload(selectedDownload.id)
                selectedDownloadId = null
            },
        )
    }
}

private fun LazyListScope.downloadsLibraryContent(
    uiState: DownloadsUiState,
    showHeaderAccent: Boolean,
    selectedFilter: LibraryDownloadsFilter,
    onFilterSelected: (LibraryDownloadsFilter) -> Unit,
    onOpenDownload: ((DownloadItem) -> Unit)?,
    onDownloadsClick: (() -> Unit)?,
    onSelectDownload: (DownloadItem) -> Unit,
) {
    // Progress callbacks update updatedAtEpochMs several times per second. Ordering the
    // horizontal rail by that field makes cards jump below the user's finger and looks
    // like progress has moved between series.
    val activeItems = uiState.activeItems.sortedByDescending { it.createdAtEpochMs }
    val completedMovies = uiState.completedItems
        .filterNot(DownloadItem::isEpisode)
        .sortedByDescending { it.updatedAtEpochMs }
    val completedShows = uiState.completedItems
        .filter(DownloadItem::isEpisode)
        .groupBy { it.parentMetaId }
        .mapNotNull { (_, episodes) ->
            episodes.sortedForSeriesDownloads().lastOrNull()?.let { latest ->
                LibraryDownloadShowGroup(latest, episodes)
            }
        }
        .sortedBy { it.representative.title.lowercase() }
    val movieEntries = completedMovies.map { LibraryDownloadDisplayEntry.Movie(it) }
    val showEntries = completedShows.map { LibraryDownloadDisplayEntry.Show(it) }
    val allEntries = (movieEntries + showEntries).sortedByDescending { it.sortEpochMs }
    val visibleEntries = when (selectedFilter) {
        LibraryDownloadsFilter.All -> allEntries
        LibraryDownloadsFilter.Movies -> movieEntries
        LibraryDownloadsFilter.Shows -> showEntries
    }

    if (uiState.items.isEmpty()) {
        item(key = "library-downloads-empty") {
            LibraryDownloadsEmptyState(
                onManageClick = onDownloadsClick,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 26.dp),
            )
        }
        return
    }

    if (activeItems.isNotEmpty()) {
        item(key = "library-downloads-active") {
            NuvioShelfSection(
                title = stringResource(Res.string.downloads_section_downloading),
                entries = activeItems,
                headerHorizontalPadding = 16.dp,
                rowContentPadding = PaddingValues(horizontal = 16.dp),
                showHeaderAccent = showHeaderAccent,
                onViewAllClick = null,
                viewAllPillSize = NuvioViewAllPillSize.Compact,
                key = { item -> item.id },
            ) { item ->
                LibraryActiveDownloadCard(
                    item = item,
                    onClick = {
                        onSelectDownload(item)
                    },
                )
            }
        }
    }

    item(key = "library-downloads-filters") {
        LibraryDownloadsFilterRow(
            selectedFilter = selectedFilter,
            allCount = allEntries.size,
            movieCount = movieEntries.size,
            showCount = showEntries.size,
            onFilterSelected = onFilterSelected,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }

    if (visibleEntries.isNotEmpty()) {
        item(key = "library-downloads-${selectedFilter.name}") {
            NuvioShelfSection(
                title = when (selectedFilter) {
                    LibraryDownloadsFilter.All -> stringResource(Res.string.downloads_section_all)
                    LibraryDownloadsFilter.Movies -> stringResource(Res.string.downloads_section_movies)
                    LibraryDownloadsFilter.Shows -> stringResource(Res.string.downloads_section_shows)
                },
                entries = visibleEntries,
                headerHorizontalPadding = 16.dp,
                rowContentPadding = PaddingValues(horizontal = 16.dp),
                showHeaderAccent = showHeaderAccent,
                onViewAllClick = null,
                viewAllPillSize = NuvioViewAllPillSize.Compact,
                key = { entry -> entry.key },
            ) { group ->
                val representative = group.representative
                val libraryItem = when (group) {
                    is LibraryDownloadDisplayEntry.Movie -> representative.toDownloadedLibraryItem()
                    is LibraryDownloadDisplayEntry.Show -> representative.toDownloadedLibraryItem().copy(
                        releaseInfo = stringResource(Res.string.downloads_episode_count, group.group.episodes.size),
                    )
                }
                HomePosterCard(
                    item = libraryItem.toMetaPreview(),
                    isWatched = false,
                    onClick = {
                        onSelectDownload(representative)
                    },
                )
            }
        }
    } else if (allEntries.isNotEmpty()) {
        item(key = "library-downloads-filter-empty") {
            Text(
                text = stringResource(Res.string.downloads_filter_empty),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LibraryDownloadsFilterRow(
    selectedFilter: LibraryDownloadsFilter,
    allCount: Int,
    movieCount: Int,
    showCount: Int,
    onFilterSelected: (LibraryDownloadsFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LibraryDownloadFilterChip(
            label = stringResource(Res.string.downloads_filter_all),
            value = allCount.toString(),
            selected = selectedFilter == LibraryDownloadsFilter.All,
            onClick = { onFilterSelected(LibraryDownloadsFilter.All) },
        )
        LibraryDownloadFilterChip(
            label = stringResource(Res.string.downloads_section_movies),
            value = movieCount.toString(),
            selected = selectedFilter == LibraryDownloadsFilter.Movies,
            onClick = { onFilterSelected(LibraryDownloadsFilter.Movies) },
        )
        LibraryDownloadFilterChip(
            label = stringResource(Res.string.downloads_section_shows),
            value = showCount.toString(),
            selected = selectedFilter == LibraryDownloadsFilter.Shows,
            onClick = { onFilterSelected(LibraryDownloadsFilter.Shows) },
        )
    }
}

@Composable
private fun LibraryDownloadFilterChip(
    label: String,
    value: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = Modifier
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.74f)
        },
        border = BorderStroke(
            1.dp,
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun LibraryDownloadsEmptyState(
    onManageClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(Res.string.downloads_empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(Res.string.downloads_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (onManageClick != null) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(onClick = onManageClick),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Text(
                    text = stringResource(Res.string.downloads_show_downloads),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun LibraryDownloadActionSheet(
    item: DownloadItem,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
) {
    val artwork = item.detailsSnapshot?.poster?.takeIf { it.isNotBlank() }
        ?: item.poster?.takeIf { it.isNotBlank() }
        ?: item.episodeThumbnail?.takeIf { it.isNotBlank() }
        ?: item.background?.takeIf { it.isNotBlank() }
    val title = item.downloadDisplayTitle()
    val status = when (item.status) {
        DownloadStatus.Downloading -> stringResource(
            Res.string.downloads_status_downloading,
            item.downloadSizeLabel(),
        )
        DownloadStatus.Waiting -> stringResource(Res.string.downloads_status_waiting)
        DownloadStatus.Paused -> stringResource(
            Res.string.downloads_status_paused,
            item.downloadSizeLabel(),
        )
        DownloadStatus.Completed -> stringResource(
            Res.string.downloads_status_completed,
            item.downloadSizeLabel(),
        )
        DownloadStatus.Failed -> item.errorMessage ?: stringResource(Res.string.downloads_status_failed)
    }

    fun dismissAfter(action: () -> Unit) {
        action()
        onDismiss()
    }

    NuvioMediaActionOverlay(
        artworkUrl = artwork,
        contentDescription = title,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 430.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .width(274.dp)
                    .aspectRatio(0.675f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E1E1E)),
                contentAlignment = Alignment.Center,
            ) {
                if (!artwork.isNullOrBlank()) {
                    AsyncImage(
                        model = artwork,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = title.take(1).uppercase(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = title,
                modifier = Modifier.padding(top = 16.dp, start = 20.dp, end = 20.dp),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.66f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            LibraryDownloadInfoCard(item = item)
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .widthIn(max = 360.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF19191F).copy(alpha = 0.97f),
                shadowElevation = 16.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    when (item.status) {
                        DownloadStatus.Downloading -> LibraryDownloadActionRow(
                            icon = Icons.Rounded.Pause,
                            title = stringResource(Res.string.downloads_action_pause),
                            onClick = { dismissAfter { DownloadsRepository.pauseDownload(item.id) } },
                        )
                        DownloadStatus.Waiting -> {
                            // Queued behind an active download; only removal is offered.
                        }
                        DownloadStatus.Paused -> LibraryDownloadActionRow(
                            icon = Icons.Rounded.PlayArrow,
                            title = stringResource(Res.string.downloads_action_resume),
                            onClick = { dismissAfter { DownloadsRepository.resumeDownload(item.id) } },
                        )
                        DownloadStatus.Failed -> LibraryDownloadActionRow(
                            icon = Icons.Rounded.Refresh,
                            title = stringResource(Res.string.action_retry),
                            onClick = { dismissAfter { DownloadsRepository.retryDownload(item.id) } },
                        )
                        DownloadStatus.Completed -> if (item.isPlayable) {
                            LibraryDownloadActionRow(
                                icon = Icons.Rounded.PlayArrow,
                                title = stringResource(Res.string.action_play),
                                onClick = { dismissAfter(onPlay) },
                            )
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f))
                    LibraryDownloadActionRow(
                        icon = Icons.Rounded.Delete,
                        title = stringResource(
                            if (item.status == DownloadStatus.Downloading) {
                                Res.string.downloads_action_cancel_download
                            } else {
                                Res.string.downloads_action_remove_download
                            },
                        ),
                        destructive = true,
                        onClick = { dismissAfter(onRemove) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryDownloadInfoCard(item: DownloadItem) {
    val description = item.downloadMetadataLabel()
    if (description.isBlank()) return
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.86f)
            .widthIn(max = 360.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1D1D23).copy(alpha = 0.94f),
    ) {
        Text(
            text = description,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.68f),
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LibraryDownloadActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val color = if (destructive) Color(0xFFFF6E95) else Color.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun DownloadItem.downloadMetadataLabel(): String = listOfNotNull(
    localizedSeasonEpisodeCode(seasonNumber, episodeNumber),
    providerName.takeIf { it.isNotBlank() },
    streamTitle.takeIf { it.isNotBlank() },
    streamSubtitle?.takeIf { it.isNotBlank() },
).joinToString(" • ")

@Composable
private fun LibraryActiveDownloadCard(
    item: DownloadItem,
    onClick: () -> Unit,
) {
    val artwork = item.detailsSnapshot?.poster?.takeIf { it.isNotBlank() }
        ?: item.poster?.takeIf { it.isNotBlank() }
        ?: item.episodeThumbnail?.takeIf { it.isNotBlank() }
        ?: item.background?.takeIf { it.isNotBlank() }
    val progress = item.progressFraction.coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.675f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (!artwork.isNullOrBlank()) {
                AsyncImage(
                    model = artwork,
                    contentDescription = item.downloadDisplayTitle(),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = item.downloadDisplayTitle().take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(9.dp),
                shape = RoundedCornerShape(999.dp),
                color = Color.Black.copy(alpha = 0.86f),
                contentColor = Color.White,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)),
                shadowElevation = 6.dp,
            ) {
                Text(
                    text = "${(progress * 100f).toInt().coerceIn(0, 100)}%",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp)),
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        )
        Column(
            modifier = Modifier.padding(horizontal = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.downloadParentTitle(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.downloadSecondaryLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private sealed class LibraryDownloadDisplayEntry {
    abstract val key: String
    abstract val representative: DownloadItem
    abstract val sortEpochMs: Long

    data class Movie(
        val download: DownloadItem,
    ) : LibraryDownloadDisplayEntry() {
        override val key: String = "movie-${download.id}"
        override val representative: DownloadItem = download
        override val sortEpochMs: Long = download.updatedAtEpochMs
    }

    data class Show(
        val group: LibraryDownloadShowGroup,
    ) : LibraryDownloadDisplayEntry() {
        override val key: String = "show-${group.representative.parentMetaId}"
        override val representative: DownloadItem = group.representative
        override val sortEpochMs: Long = group.episodes.maxOfOrNull { it.updatedAtEpochMs }
            ?: group.representative.updatedAtEpochMs
    }
}

private enum class LibraryDownloadsFilter {
    All,
    Movies,
    Shows,
}

private fun DownloadItem.downloadDisplayTitle(): String =
    if (isEpisode) {
        episodeTitle?.trim()?.takeIf { it.isNotBlank() } ?: title
    } else {
        title
    }

private fun DownloadItem.downloadParentTitle(): String = title.trim().ifBlank { downloadDisplayTitle() }

private fun DownloadItem.downloadSecondaryLabel(): String =
    if (isEpisode) {
        listOfNotNull(
            localizedSeasonEpisodeCode(seasonNumber, episodeNumber),
            episodeTitle?.trim()?.takeIf { it.isNotBlank() && it != downloadParentTitle() },
        ).joinToString(" • ")
    } else {
        streamTitle.takeIf { it.isNotBlank() && it != downloadParentTitle() }
            ?: providerName
    }

private data class LibraryDownloadShowGroup(
    val representative: DownloadItem,
    val episodes: List<DownloadItem>,
)

private fun DownloadItem.toDownloadedLibraryItem(): LibraryItem {
    val snapshot = detailsSnapshot
    val type = snapshot?.type?.trim()?.takeIf { it.isNotBlank() }
        ?: parentMetaType.trim().ifBlank { contentType.trim() }.ifBlank { "movie" }
    return LibraryItem(
        id = snapshot?.id?.trim()?.takeIf { it.isNotBlank() } ?: parentMetaId.trim().ifBlank { id },
        type = type,
        name = snapshot?.name?.trim()?.takeIf { it.isNotBlank() } ?: title.trim().ifBlank { streamTitle },
        poster = snapshot?.poster ?: poster ?: episodeThumbnail,
        banner = snapshot?.background ?: background,
        logo = snapshot?.logo ?: logo,
        description = snapshot?.description ?: episodeOverview,
        releaseInfo = snapshot?.releaseInfo,
        imdbRating = snapshot?.imdbRating,
        genres = snapshot?.genres.orEmpty(),
        savedAtEpochMs = updatedAtEpochMs.takeIf { it > 0L } ?: createdAtEpochMs,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryReleaseCalendarSheet(
    events: List<LibraryCalendarEvent>,
    onDismiss: () -> Unit,
    onPosterClick: ((LibraryItem) -> Unit)?,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initialMonth = remember(events) { initialLibraryCalendarMonth(events) }
    var visibleMonth by remember(events) { mutableStateOf(initialMonth) }
    val monthEvents = remember(events, visibleMonth) {
        events.filter { it.date.year == visibleMonth.year && it.date.month == visibleMonth.month }
            .sortedBy { it.date.iso }
    }
    val eventsByDate = remember(events) { events.groupBy { it.date.iso } }
    var selectedDateIso by remember(events) { mutableStateOf(monthEvents.firstOrNull()?.date?.iso) }
    val selectedEvents = selectedDateIso?.let(eventsByDate::get).orEmpty()
    NuvioModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.library_calendar_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(Res.string.library_calendar_exact_dates_only), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(Res.string.action_close))
                }
            }
            if (events.isEmpty()) {
                Text(stringResource(Res.string.library_calendar_empty_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(Res.string.library_calendar_empty_message), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { visibleMonth = visibleMonth.previous(); selectedDateIso = null }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(Res.string.library_calendar_previous_month))
                    }
                    Text(visibleMonth.displayTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { visibleMonth = visibleMonth.next(); selectedDateIso = null }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(Res.string.library_calendar_next_month))
                    }
                }
                LibraryCalendarWeekdayHeader()
                LibraryCalendarMonthGrid(
                    month = visibleMonth,
                    eventsByDate = eventsByDate,
                    selectedDateIso = selectedDateIso,
                    onDateSelected = { selectedDateIso = it.iso },
                )
                NuvioBottomSheetDivider()
                val visibleEvents = selectedEvents.ifEmpty { monthEvents }
                Text(
                    text = if (selectedEvents.isNotEmpty()) {
                        stringResource(Res.string.library_calendar_selected_day, displayLibraryCalendarDate(selectedDateIso.orEmpty()))
                    } else {
                        stringResource(Res.string.library_calendar_month_events)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                visibleEvents.forEach { event ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPosterClick?.invoke(event.item) },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .aspectRatio(PosterLandscapeAspectRatio)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                event.artwork?.let { artwork ->
                                    AsyncImage(
                                        model = artwork,
                                        contentDescription = event.displayTitle,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit,
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.displayTitle,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                event.episodeCode?.let { code ->
                                    Text(
                                        text = code,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Text(
                                    formatReleaseDateForDisplay(event.rawReleaseInfo),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryCalendarWeekdayHeader() {
    val labels = listOf(
        Res.string.library_calendar_weekday_mon,
        Res.string.library_calendar_weekday_tue,
        Res.string.library_calendar_weekday_wed,
        Res.string.library_calendar_weekday_thu,
        Res.string.library_calendar_weekday_fri,
        Res.string.library_calendar_weekday_sat,
        Res.string.library_calendar_weekday_sun,
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEach { label ->
            Text(stringResource(label), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LibraryCalendarMonthGrid(
    month: LibraryCalendarMonth,
    eventsByDate: Map<String, List<LibraryCalendarEvent>>,
    selectedDateIso: String?,
    onDateSelected: (LibraryCalendarDate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        libraryCalendarCells(month).chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(Modifier.weight(1f).height(44.dp))
                    } else {
                        val hasEvents = eventsByDate[date.iso].orEmpty().isNotEmpty()
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = hasEvents) { onDateSelected(date) },
                            color = when {
                                selectedDateIso == date.iso -> MaterialTheme.colorScheme.primary
                                hasEvents -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            },
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(date.day.toString(), color = if (selectedDateIso == date.iso) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                if (hasEvents) Text(eventsByDate[date.iso].orEmpty().size.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class LibraryCalendarEvent(
    val date: LibraryCalendarDate,
    val rawReleaseInfo: String,
    val item: LibraryItem,
    val displayTitle: String = item.name,
    val artwork: String? = item.banner ?: item.poster,
    val season: Int? = null,
    val episode: Int? = null,
) {
    val episodeCode: String? = if (season != null && episode != null) {
        localizedSeasonEpisodeCode(season, episode)
    } else {
        null
    }
}

private fun MetaVideo.calendarDisplayTitle(item: LibraryItem): String =
    title.trim().takeIf { it.isNotBlank() } ?: item.name

private fun MetaVideo.calendarArtwork(item: LibraryItem): String? =
    thumbnail?.takeIf { it.isNotBlank() }
        ?: seasonPoster?.takeIf { it.isNotBlank() }
        ?: item.poster?.takeIf { it.isNotBlank() }
        ?: item.banner?.takeIf { it.isNotBlank() }

private fun MetaVideo.toLibraryCalendarEvent(item: LibraryItem): LibraryCalendarEvent? {
    val raw = released?.takeIf { it.isNotBlank() } ?: return null
    val date = parseLibraryCalendarDate(raw) ?: return null
    return LibraryCalendarEvent(
        date = date,
        rawReleaseInfo = raw,
        item = item,
        displayTitle = calendarDisplayTitle(item),
        artwork = calendarArtwork(item),
        season = season,
        episode = episode,
    )
}

private data class LibraryCalendarDate(val year: Int, val month: Int, val day: Int) {
    val iso: String = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

private data class LibraryCalendarMonth(val year: Int, val month: Int) {
    val displayTitle: String = "${localizedMonthName(month)} $year"
    fun previous() = if (month == 1) LibraryCalendarMonth(year - 1, 12) else copy(month = month - 1)
    fun next() = if (month == 12) LibraryCalendarMonth(year + 1, 1) else copy(month = month + 1)
}

private fun libraryCalendarItemsCacheKey(items: List<LibraryItem>): String =
    items.joinToString(separator = "|") { item ->
        "${item.type}:${item.id}:${item.releaseInfo.orEmpty()}"
    }

private fun libraryReleaseSupportCacheKey(
    calendarItemsKey: String,
    radarDetailsRequestKey: String,
): String = "release_support_v2:${calendarItemsKey.hashCode()}:${radarDetailsRequestKey.hashCode()}"

private suspend fun buildLibraryReleaseCalendarEvents(items: List<LibraryItem>): List<LibraryCalendarEvent> {
    val resolvedDetails = resolveLibraryReleaseRadarDetails(items)
    return buildLibraryReleaseCalendarEventsFromDetails(items, resolvedDetails)
}

private fun buildLibraryReleaseCalendarEventsFromDetails(
    items: List<LibraryItem>,
    detailsByKey: Map<String, MetaDetails>,
): List<LibraryCalendarEvent> {
    val fallbackEvents = buildLibraryReleaseCalendarFallbackEvents(items)
    val libraryItemsByRadarKey = items.associateBy(::libraryItemKeyForHomeRadar)
    val episodeEvents = detailsByKey.flatMap { (radarKey, details) ->
        val item = libraryItemsByRadarKey[radarKey] ?: return@flatMap emptyList()
        details.videos.mapNotNull { video -> video.toLibraryCalendarEvent(item) }
    }
    val seriesWithEpisodeEvents = episodeEvents.map { it.item.id to it.item.type.lowercase() }.toSet()
    return (episodeEvents + fallbackEvents.filterNot { event ->
        event.item.isLibrarySeries() && (event.item.id to event.item.type.lowercase()) in seriesWithEpisodeEvents
    }).distinctBy {
        it.date.iso + it.item.type + it.item.id + it.season + it.episode + it.displayTitle
    }
        .sortedBy { it.date.iso }
}

private fun buildLibraryReleaseCalendarFallbackEvents(items: List<LibraryItem>): List<LibraryCalendarEvent> =
    items.asSequence().mapNotNull { item ->
        val raw = item.releaseInfo?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val date = parseLibraryCalendarDate(raw) ?: return@mapNotNull null
        LibraryCalendarEvent(date, raw, item)
    }.sortedBy { it.date.iso }.toList()

private suspend fun resolveLibraryReleaseRadarDetails(items: List<LibraryItem>): Map<String, MetaDetails> =
    coroutineScope {
        val resolved = mutableListOf<Pair<String, MetaDetails>>()
        items.filter(LibraryItem::isLibrarySeries)
            .take(LIBRARY_RELEASE_RADAR_DETAILS_RESOLUTION_LIMIT)
            .chunked(LIBRARY_RELEASE_RADAR_DETAILS_RESOLUTION_CONCURRENCY)
            .forEach { chunk ->
                resolved += chunk.map { item ->
                    async {
                        val details = runCatching { MetaDetailsRepository.fetch(item.type, item.id) }.getOrNull()
                            ?: return@async null
                        libraryItemKeyForHomeRadar(item) to details
                    }
                }.awaitAll().filterNotNull()
            }
        resolved.toMap()
    }

private fun loadLibraryReleaseSupportCache(profileId: Int, cacheKey: String): LibraryReleaseSupportCachePayload? =
    LibraryStorage.loadReleaseSupportPayload(profileId, cacheKey)
        ?.let { payload -> runCatching { libraryReleaseSupportJson.decodeFromString<StoredLibraryReleaseSupportPayload>(payload) }.getOrNull() }
        ?.toCachePayload()

private fun saveLibraryReleaseSupportCache(profileId: Int, cacheKey: String, detailsByKey: Map<String, MetaDetails>) {
    if (detailsByKey.isEmpty()) return
    val payload = StoredLibraryReleaseSupportPayload(
        fetchedAtEpochMs = LibraryClock.nowEpochMs(),
        details = detailsByKey.map { (key, details) -> StoredLibraryReleaseSupportDetail.from(key, details) },
    )
    runCatching { LibraryStorage.saveReleaseSupportPayload(profileId, cacheKey, libraryReleaseSupportJson.encodeToString(payload)) }
}

private data class LibraryReleaseSupportCachePayload(
    val fetchedAtEpochMs: Long,
    val detailsByKey: Map<String, MetaDetails>,
) {
    fun isFresh(nowEpochMs: Long = LibraryClock.nowEpochMs()): Boolean =
        fetchedAtEpochMs > 0L && nowEpochMs - fetchedAtEpochMs <= LIBRARY_RELEASE_SUPPORT_CACHE_TTL_MS && detailsByKey.isNotEmpty()
}

@Serializable
private data class StoredLibraryReleaseSupportPayload(
    val fetchedAtEpochMs: Long,
    val details: List<StoredLibraryReleaseSupportDetail> = emptyList(),
) {
    fun toCachePayload() = LibraryReleaseSupportCachePayload(
        fetchedAtEpochMs,
        details.associate { it.key to it.toMetaDetails() },
    )
}

@Serializable
private data class StoredLibraryReleaseSupportDetail(
    val key: String,
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val imdbRating: String? = null,
    val genres: List<String> = emptyList(),
    val videos: List<StoredLibraryReleaseSupportVideo> = emptyList(),
) {
    fun toMetaDetails() = MetaDetails(
        id, type, name, poster = poster, background = background, logo = logo,
        description = description, releaseInfo = releaseInfo, imdbRating = imdbRating,
        genres = genres, videos = videos.map { it.toMetaVideo() },
    )

    companion object {
        fun from(key: String, details: MetaDetails) = StoredLibraryReleaseSupportDetail(
            key, details.id, details.type, details.name, details.poster, details.background,
            details.logo, details.description, details.releaseInfo, details.imdbRating,
            details.genres, details.videos.map { StoredLibraryReleaseSupportVideo.from(it) },
        )
    }
}

@Serializable
private data class StoredLibraryReleaseSupportVideo(
    val id: String,
    val title: String,
    val released: String? = null,
    val thumbnail: String? = null,
    val seasonPoster: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val overview: String? = null,
    val runtime: Int? = null,
) {
    fun toMetaVideo() = MetaVideo(id, title, released, thumbnail = thumbnail, seasonPoster = seasonPoster, season = season, episode = episode, overview = overview, runtime = runtime)

    companion object {
        fun from(video: MetaVideo) = StoredLibraryReleaseSupportVideo(video.id, video.title, video.released, video.thumbnail, video.seasonPoster, video.season, video.episode, video.overview, video.runtime)
    }
}

private fun parseLibraryCalendarDate(raw: String?): LibraryCalendarDate? {
    val parts = raw?.trim()?.substringBefore('T')?.split('-') ?: return null
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull()?.takeIf { it in 1000..9999 } ?: return null
    val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return null
    val day = parts[2].toIntOrNull()?.takeIf { it in 1..daysInLibraryCalendarMonth(year, month) } ?: return null
    return LibraryCalendarDate(year, month, day)
}

private fun LibraryItem.isLibrarySeries(): Boolean =
    type.equals("series", true) || type.equals("tv", true) || type.equals("show", true) || type.equals("tvshow", true)

private fun initialLibraryCalendarMonth(events: List<LibraryCalendarEvent>): LibraryCalendarMonth {
    val today = CurrentDateProvider.todayIsoDate()
    val target = events.firstOrNull { it.date.iso >= today }?.date ?: events.lastOrNull()?.date
    return LibraryCalendarMonth(target?.year ?: 1970, target?.month ?: 1)
}

private fun displayLibraryCalendarDate(iso: String): String {
    val parts = iso.split('-')
    return if (parts.size == 3) "${parts[2].toIntOrNull() ?: parts[2]} ${localizedMonthName(parts[1].toIntOrNull() ?: 1)} ${parts[0]}" else iso
}

private fun libraryCalendarCells(month: LibraryCalendarMonth): List<LibraryCalendarDate?> {
    val cells = MutableList<LibraryCalendarDate?>(firstLibraryCalendarWeekdayOffset(month.year, month.month)) { null }
    repeat(daysInLibraryCalendarMonth(month.year, month.month)) { cells += LibraryCalendarDate(month.year, month.month, it + 1) }
    while (cells.size < 42) cells += null
    return cells
}

private fun daysInLibraryCalendarMonth(year: Int, month: Int): Int = when (month) {
    2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

private fun firstLibraryCalendarWeekdayOffset(year: Int, month: Int): Int {
    val y = if (month < 3) year - 1 else year
    val m = if (month < 3) month + 12 else month
    return ((1 + (13 * (m + 1)) / 5 + y + y / 4 - y / 100 + y / 400) % 7 + 6) % 7
}

private fun LazyListScope.cloudLibraryContent(
    uiState: CloudLibraryUiState,
    selectedProviderId: String?,
    selectedType: CloudLibraryItemType?,
    selectedCloudItemKey: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onProviderSelected: (String?) -> Unit,
    onTypeSelected: (CloudLibraryItemType?) -> Unit,
    onItemSelected: (CloudLibraryItem) -> Unit,
    onFileSelected: (CloudLibraryItem, CloudLibraryFile) -> Unit,
    onBackToItems: () -> Unit,
    onRefresh: () -> Unit,
    onConnectCloudClick: (() -> Unit)?,
) {
    when {
        !uiState.isLoaded -> {
            cloudLibrarySkeletonItems()
        }

        !uiState.isEnabled -> {
            item {
                HomeEmptyStateCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = stringResource(Res.string.cloud_library_disabled_title),
                    message = stringResource(Res.string.cloud_library_disabled_message),
                    actionLabel = stringResource(Res.string.cloud_library_disabled_action),
                    onActionClick = onConnectCloudClick,
                )
            }
        }

        !uiState.hasConnectedProvider -> {
            item {
                HomeEmptyStateCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = stringResource(Res.string.cloud_library_connect_title),
                    message = stringResource(Res.string.cloud_library_connect_message),
                    actionLabel = stringResource(Res.string.cloud_library_connect_action),
                    onActionClick = onConnectCloudClick,
                )
            }
        }

        else -> {
            val providerItems = uiState.items
                .filter { item -> selectedProviderId == null || item.providerId == selectedProviderId }
            val availableTypes = providerItems
                .map { item -> item.type }
                .distinct()
                .sortedBy { type -> type.ordinal }
            val effectiveSelectedType = selectedType?.takeIf { type -> type in availableTypes }
            val typeFilteredItems = providerItems
                .filter { item -> effectiveSelectedType == null || item.type == effectiveSelectedType }
            // Local filter over the already-loaded library. Matches the item name or any of its
            // file names, since the useful identifier is often in the filename, not the title.
            val trimmedQuery = searchQuery.trim()
            val filteredItems = if (trimmedQuery.isEmpty()) {
                typeFilteredItems
            } else {
                typeFilteredItems.filter { item ->
                    item.name.contains(trimmedQuery, ignoreCase = true) ||
                        item.files.any { file -> file.name.contains(trimmedQuery, ignoreCase = true) }
                }
            }
            val selectedItem = filteredItems.firstOrNull { it.stableKey == selectedCloudItemKey }

            if (selectedItem != null) {
                item {
                    CloudLibraryFilePicker(
                        item = selectedItem,
                        onBack = onBackToItems,
                        onFileSelected = { file -> onFileSelected(selectedItem, file) },
                    )
                }
            } else {
                item {
                    CloudLibraryToolbar(
                        uiState = uiState,
                        selectedProviderId = selectedProviderId,
                        selectedType = effectiveSelectedType,
                        availableTypes = availableTypes,
                        onProviderSelected = onProviderSelected,
                        onTypeSelected = onTypeSelected,
                        onRefresh = onRefresh,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                item(key = "cloud-library-search") {
                    CloudLibrarySearchField(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                uiState.providers
                    .filter { providerState -> selectedProviderId == null || providerState.providerId == selectedProviderId }
                    .filter { providerState -> !providerState.errorMessage.isNullOrBlank() && providerState.items.isEmpty() }
                    .forEach { providerState ->
                        item(key = "cloud-error-${providerState.providerId}") {
                            HomeEmptyStateCard(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                title = stringResource(Res.string.cloud_library_load_failed, providerState.providerName),
                                message = providerState.errorMessage.orEmpty(),
                                actionLabel = stringResource(Res.string.action_retry),
                                onActionClick = onRefresh,
                            )
                        }
                    }

                if (uiState.isRefreshing && filteredItems.isEmpty()) {
                    cloudLibrarySkeletonItems()
                } else if (filteredItems.isEmpty()) {
                    item {
                        HomeEmptyStateCard(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            title = stringResource(Res.string.cloud_library_empty_title),
                            message = stringResource(Res.string.cloud_library_empty_message),
                            actionLabel = stringResource(Res.string.action_retry),
                            onActionClick = onRefresh,
                        )
                    }
                } else {
                    items(
                        items = filteredItems,
                        key = { item -> item.stableKey },
                    ) { item ->
                        CloudLibraryRow(
                            item = item,
                            onClick = { onItemSelected(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudLibrarySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        placeholder = { Text(stringResource(Res.string.cloud_library_search_label)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(Res.string.compose_search_clear),
                    )
                }
            }
        },
    )
}

private fun LazyListScope.cloudLibrarySkeletonItems() {
    item(key = "cloud-library-skeleton-toolbar") {
        CloudLibrarySkeletonToolbar(
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
    items(3) {
        CloudLibrarySkeletonRow()
    }
}

@Composable
private fun LibrarySourceSwitch(
    selectedMode: LibraryViewMode,
    onModeSelected: (LibraryViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LibraryChip(
            label = stringResource(Res.string.library_source_saved),
            selected = selectedMode == LibraryViewMode.Saved,
            onClick = { onModeSelected(LibraryViewMode.Saved) },
        )
        LibraryChip(
            label = stringResource(Res.string.compose_settings_root_downloads_title),
            selected = selectedMode == LibraryViewMode.Downloads,
            onClick = { onModeSelected(LibraryViewMode.Downloads) },
        )
        LibraryChip(
            label = stringResource(Res.string.library_source_cloud),
            selected = selectedMode == LibraryViewMode.Cloud,
            onClick = { onModeSelected(LibraryViewMode.Cloud) },
        )
    }
}

@Composable
private fun CloudLibraryToolbar(
    uiState: CloudLibraryUiState,
    selectedProviderId: String?,
    selectedType: CloudLibraryItemType?,
    availableTypes: List<CloudLibraryItemType>,
    onProviderSelected: (String?) -> Unit,
    onTypeSelected: (CloudLibraryItemType?) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val providerOptions = buildList {
        add(NuvioDropdownOption(key = "", label = stringResource(Res.string.cloud_library_provider_all)))
        addAll(
            uiState.providers.map { provider ->
                NuvioDropdownOption(
                    key = provider.providerId,
                    label = provider.providerName,
                )
            },
        )
    }
    val typeOptions = buildList {
        add(NuvioDropdownOption(key = "", label = stringResource(Res.string.cloud_library_type_all)))
        addAll(
            availableTypes.map { type ->
                NuvioDropdownOption(
                    key = type.name,
                    label = cloudLibraryTypeLabel(type),
                )
            },
        )
    }
    val selectedProviderName = uiState.providers
        .firstOrNull { provider -> provider.providerId == selectedProviderId }
        ?.providerName
        ?: stringResource(Res.string.cloud_library_provider_all)
    val selectedTypeLabel = selectedType?.let { type -> cloudLibraryTypeLabel(type) }
        ?: stringResource(Res.string.cloud_library_type_all)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NuvioDropdownChip(
                    title = stringResource(Res.string.cloud_library_select_provider),
                    label = selectedProviderName,
                    selectedKey = selectedProviderId.orEmpty(),
                    options = providerOptions,
                    enabled = providerOptions.size > 1,
                    onSelected = { option ->
                        onProviderSelected(option.key.ifBlank { null })
                    },
                )
                NuvioDropdownChip(
                    title = stringResource(Res.string.cloud_library_select_type),
                    label = selectedTypeLabel,
                    selectedKey = selectedType?.name.orEmpty(),
                    options = typeOptions,
                    enabled = typeOptions.size > 1,
                    onSelected = { option ->
                        val type = option.key
                            .takeIf { it.isNotBlank() }
                            ?.let(CloudLibraryItemType::valueOf)
                        onTypeSelected(type)
                    },
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(Res.string.cloud_library_refresh),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LibraryChip(
    label: String,
    selected: Boolean,
    loading: Boolean = false,
    error: Boolean = false,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) colorScheme.primaryContainer else colorScheme.surfaceContainerLow,
        border = if (selected) BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.45f)) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (loading) {
                NuvioLoadingIndicator(
                    modifier = Modifier.size(12.dp),
                    color = colorScheme.primary,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    error -> colorScheme.error
                    selected -> colorScheme.onPrimaryContainer
                    else -> colorScheme.onSurfaceVariant
                },
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CloudLibraryRow(
    item: CloudLibraryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playableCount = item.playableFiles.size
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(enabled = playableCount > 0, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = cloudLibrarySubtitle(item),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = cloudLibraryStatusLine(item),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (playableCount > 0) {
                    IconButton(onClick = onClick) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(Res.string.action_play),
                        )
                    }
                }
            }
            item.progressFraction?.takeIf { it in 0f..0.999f }?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CloudLibraryFilePicker(
    item: CloudLibraryItem,
    onBack: () -> Unit,
    onFileSelected: (CloudLibraryFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(Res.string.action_back),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(Res.string.cloud_library_file_picker_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val files = item.playableFiles
            if (files.isEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.cloud_library_no_files_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(Res.string.cloud_library_no_files_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                files.forEach { file ->
                    CloudLibraryFileRow(
                        file = file,
                        onClick = { onFileSelected(file) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudLibraryFileRow(
    file: CloudLibraryFile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.58f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(18.dp),
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = file.sizeBytes?.let { size -> formatCloudBytes(size) }.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(Res.string.cloud_library_play_file),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun cloudLibrarySubtitle(item: CloudLibraryItem): String {
    val fileLine = when (val playableCount = item.playableFiles.size) {
        0 -> stringResource(Res.string.cloud_library_no_playable_files)
        1 -> item.playableFiles.first().name
        else -> stringResource(Res.string.cloud_library_playable_file_count, playableCount)
    }
    return listOf(item.providerName, cloudLibraryTypeLabel(item.type), fileLine).joinToString(" • ")
}

@Composable
private fun cloudLibraryStatusLine(item: CloudLibraryItem): String {
    val fallback = if (item.playableFiles.isEmpty()) {
        stringResource(Res.string.cloud_library_no_playable_files)
    } else {
        stringResource(Res.string.cloud_library_status_ready)
    }
    return listOfNotNull(
        item.status?.toDisplayStatus(),
        item.sizeBytes?.let(::formatCloudBytes),
        item.progressFraction?.let { "${(it * 100f).toInt()}%" },
    ).joinToString(" • ").ifBlank { fallback }
}

@Composable
private fun cloudLibraryTypeLabel(type: CloudLibraryItemType): String =
    when (type) {
        CloudLibraryItemType.Torrent -> stringResource(Res.string.cloud_library_type_torrents)
        CloudLibraryItemType.Usenet -> stringResource(Res.string.cloud_library_type_usenet)
        CloudLibraryItemType.WebDownload -> stringResource(Res.string.cloud_library_type_web)
        CloudLibraryItemType.File -> stringResource(Res.string.cloud_library_type_files)
    }

private fun formatCloudBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 ${localizedByteUnit("B")}"
    val kib = 1024.0
    val mib = kib * 1024.0
    val gib = mib * 1024.0
    val value = bytes.toDouble()
    return when {
        value >= gib -> "${((value / gib) * 10.0).toInt() / 10.0} ${localizedByteUnit("GB")}"
        value >= mib -> "${((value / mib) * 10.0).toInt() / 10.0} ${localizedByteUnit("MB")}"
        value >= kib -> "${((value / kib) * 10.0).toInt() / 10.0} ${localizedByteUnit("KB")}"
        else -> "$bytes ${localizedByteUnit("B")}"
    }
}

private fun String.toDisplayStatus(): String =
    replace('_', ' ')
        .lowercase()
        .replaceFirstChar { it.titlecase() }

@Composable
private fun CloudLibrarySkeletonToolbar(
    modifier: Modifier = Modifier,
) {
    val brush = rememberCloudLibrarySkeletonBrush()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CloudSkeletonBlock(brush = brush, width = 112.dp, height = 36.dp, cornerRadius = 12.dp)
            CloudSkeletonBlock(brush = brush, width = 92.dp, height = 36.dp, cornerRadius = 12.dp)
        }
    }
}

@Composable
private fun CloudLibrarySkeletonRow(
    modifier: Modifier = Modifier,
) {
    val brush = rememberCloudLibrarySkeletonBrush()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    CloudSkeletonBlock(
                        brush = brush,
                        modifier = Modifier.fillMaxWidth(0.74f),
                        height = 18.dp,
                        cornerRadius = 6.dp,
                    )
                    CloudSkeletonBlock(
                        brush = brush,
                        modifier = Modifier.fillMaxWidth(0.9f),
                        height = 14.dp,
                        cornerRadius = 6.dp,
                    )
                    CloudSkeletonBlock(
                        brush = brush,
                        modifier = Modifier.fillMaxWidth(0.52f),
                        height = 12.dp,
                        cornerRadius = 6.dp,
                    )
                }
                CloudSkeletonBlock(brush = brush, width = 48.dp, height = 48.dp, cornerRadius = 24.dp)
            }
        }
    }
}

@Composable
private fun rememberCloudLibrarySkeletonBrush(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
    )
    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f),
    )
}

@Composable
private fun CloudSkeletonBlock(
    brush: Brush,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp,
    cornerRadius: Dp,
) {
    val sizeModifier = if (width != null) {
        modifier.size(width = width, height = height)
    } else {
        modifier.height(height)
    }
    Box(
        modifier = sizeModifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush),
    )
}

private enum class LibraryViewMode {
    Saved,
    Downloads,
    Cloud,
}

private fun LazyListScope.librarySections(
    displaySections: List<LibraryDisplaySection>,
    watchedKeys: Set<String>,
    fullyWatchedSeriesKeys: Set<String>,
    sortOption: LibrarySortOption,
    onPosterClick: ((LibraryItem) -> Unit)?,
    onSectionViewAllClick: ((LibrarySection, LibrarySortOption) -> Unit)?,
    onPosterLongClick: ((LibraryItem, LibrarySection) -> Unit)?,
    onDisintegrated: (String) -> Unit,
) {
    items(
        items = displaySections,
        key = { section -> "library-horizontal:${section.type}" },
    ) { section ->
        NuvioShelfSection(
            title = section.displayTitle,
            entries = section.previewEntries,
            modifier = libraryContentTransitionModifier(),
            headerHorizontalPadding = 16.dp,
            rowContentPadding = PaddingValues(horizontal = 16.dp),
            onViewAllClick = section.source
                ?.takeIf { it.items.size > LIBRARY_SECTION_PREVIEW_LIMIT }
                ?.let { source -> onSectionViewAllClick?.let { { it(source, sortOption) } } },
            viewAllPillSize = NuvioViewAllPillSize.Compact,
            key = { entry -> entry.globalKey },
            animatePlacement = true,
        ) { entry ->
            val item = entry.item
            val posterItem = item.toMetaPreview()
            val entrySource = entry.section
            DisintegratingContainer(
                disintegrating = entry.exiting,
                onDisintegrated = { onDisintegrated(entry.globalKey) },
            ) {
                HomePosterCard(
                    item = posterItem,
                    isWatched = WatchingState.isPosterWatched(
                        watchedKeys = watchedKeys,
                        item = posterItem,
                        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                    ),
                    onClick = if (entry.exiting) null else onPosterClick?.let { { it(item) } },
                    onLongClick = if (entry.exiting || entrySource == null) {
                        null
                    } else {
                        onPosterLongClick?.let { { it(item, entrySource) } }
                    },
                )
            }
        }
    }
}

private const val LIBRARY_SECTION_PREVIEW_LIMIT = 18
private const val LIBRARY_RELEASE_RADAR_DETAILS_RESOLUTION_LIMIT = 24
private const val LIBRARY_RELEASE_RADAR_DETAILS_RESOLUTION_CONCURRENCY = 4
private const val LIBRARY_RELEASE_SUPPORT_CACHE_TTL_MS = 6L * 60L * 60L * 1_000L
private const val LIBRARY_DOWNLOADS_PREVIEW_LIMIT = 18
private val libraryReleaseSupportJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

private data class LibraryDisplayEntry(
    val globalKey: String,
    val item: LibraryItem,
    val section: LibrarySection?,
    val exiting: Boolean,
)

private data class LibraryDisplaySection(
    val source: LibrarySection?,
    val type: String,
    val displayTitle: String,
    val previewEntries: List<LibraryDisplayEntry>,
)

private class LibraryExitingEntry(
    val item: LibraryItem,
    val sectionType: String,
    val sectionTitle: String,
    val index: Int,
)

private fun libraryGlobalKey(sectionType: String, item: LibraryItem): String =
    "$sectionType|${item.type}|${item.id}"

private class LibraryDisintegrationHolder {
    private val tracker = ScopedDisintegrationTracker<LibrarySourceMode, String, LibraryExitingEntry> { entry ->
        libraryGlobalKey(entry.sectionType, entry.item)
    }

    fun onExited(globalKey: String) {
        tracker.onDisintegrated(globalKey)
    }

    fun reset() {
        tracker.reset()
    }

    fun sync(
        sourceMode: LibrarySourceMode,
        sections: List<LibrarySection>,
        previewLimit: Int,
    ): List<LibraryDisplaySection> {
        val current = ArrayList<LibraryExitingEntry>()
        sections.forEach { section ->
            section.items.take(previewLimit).forEachIndexed { index, item ->
                current += LibraryExitingEntry(item, section.type, section.displayTitle, index)
            }
        }
        val exitingBySection = tracker.sync(sourceMode, current)
            .asSequence()
            .filter { entry -> entry.exiting }
            .map { entry -> entry.item }
            .groupBy { entry -> entry.sectionType }
        val seenTypes = HashSet<String>(sections.size)
        val result = ArrayList<LibraryDisplaySection>(sections.size + 1)

        for (section in sections) {
            seenTypes += section.type
            val entries = ArrayList<LibraryDisplayEntry>(previewLimit + 1)
            section.items.take(previewLimit).forEach { item ->
                entries += LibraryDisplayEntry(
                    globalKey = libraryGlobalKey(section.type, item),
                    item = item,
                    section = section,
                    exiting = false,
                )
            }
            exitingBySection[section.type]?.sortedBy { it.index }?.forEach { ex ->
                val key = libraryGlobalKey(section.type, ex.item)
                if (entries.none { it.globalKey == key }) {
                    entries.add(
                        ex.index.coerceIn(0, entries.size),
                        LibraryDisplayEntry(key, ex.item, section, exiting = true),
                    )
                }
            }
            result += LibraryDisplaySection(section, section.type, section.displayTitle, entries)
        }

        for ((type, list) in exitingBySection) {
            if (type in seenTypes) continue
            val sorted = list.sortedBy { it.index }
            val entries = sorted.map { ex ->
                LibraryDisplayEntry(libraryGlobalKey(type, ex.item), ex.item, section = null, exiting = true)
            }
            result += LibraryDisplaySection(null, type, sorted.first().sectionTitle, entries)
        }

        return result
    }
}
