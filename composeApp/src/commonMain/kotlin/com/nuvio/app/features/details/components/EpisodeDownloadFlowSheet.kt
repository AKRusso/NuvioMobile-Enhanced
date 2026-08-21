package com.nuvio.app.features.details.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import coil3.compose.AsyncImage
import co.touchlab.kermit.Logger
import com.nuvio.app.core.i18n.localizedSeasonEpisodeCode
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.PlatformBackHandler
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.rememberAutomaticActionBrush
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.details.metaVideoSeasonEpisodeComparator
import com.nuvio.app.features.downloads.BatchDownloadResult
import com.nuvio.app.features.downloads.DownloadPreferredQuality
import com.nuvio.app.features.downloads.DownloadQueueMode
import com.nuvio.app.features.downloads.DownloadSourceOption
import com.nuvio.app.features.downloads.DownloadSourceResolver
import com.nuvio.app.features.downloads.EpisodeAutoSelectStatus
import com.nuvio.app.features.downloads.EpisodeDownloadCoordinator
import com.nuvio.app.features.downloads.EpisodeDownloadSettings
import com.nuvio.app.features.downloads.EpisodeDownloadSettingsStorage
import com.nuvio.app.features.downloads.EpisodeDownloadTarget
import com.nuvio.app.features.downloads.ProviderSearchState
import com.nuvio.app.features.downloads.ProviderSearchStatus
import com.nuvio.app.features.downloads.downloadOptionQualityHeight
import com.nuvio.app.features.downloads.toDownloadOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_back
import nuvio.composeapp.generated.resources.action_cancel
import nuvio.composeapp.generated.resources.action_done
import nuvio.composeapp.generated.resources.downloads_auto_select
import nuvio.composeapp.generated.resources.downloads_auto_select_desc
import nuvio.composeapp.generated.resources.downloads_auto_selecting
import nuvio.composeapp.generated.resources.downloads_automatic
import nuvio.composeapp.generated.resources.downloads_awaiting_source
import nuvio.composeapp.generated.resources.downloads_batch_summary
import nuvio.composeapp.generated.resources.downloads_choose_source
import nuvio.composeapp.generated.resources.downloads_choose_source_message
import nuvio.composeapp.generated.resources.downloads_choose_source_movie_message
import nuvio.composeapp.generated.resources.downloads_clear_selection
import nuvio.composeapp.generated.resources.downloads_continue
import nuvio.composeapp.generated.resources.downloads_download_all_at_once
import nuvio.composeapp.generated.resources.downloads_download_mode
import nuvio.composeapp.generated.resources.downloads_download_one_at_a_time
import nuvio.composeapp.generated.resources.downloads_download_settings
import nuvio.composeapp.generated.resources.downloads_download_started
import nuvio.composeapp.generated.resources.downloads_episode_prompt_message
import nuvio.composeapp.generated.resources.downloads_episode_prompt_title
import nuvio.composeapp.generated.resources.downloads_filter_all
import nuvio.composeapp.generated.resources.downloads_filter_empty
import nuvio.composeapp.generated.resources.downloads_finding_sources
import nuvio.composeapp.generated.resources.downloads_no_compatible_sources
import nuvio.composeapp.generated.resources.downloads_no_compatible_sources_movie
import nuvio.composeapp.generated.resources.downloads_preferred_quality
import nuvio.composeapp.generated.resources.downloads_quality_best
import nuvio.composeapp.generated.resources.downloads_selected_episodes
import nuvio.composeapp.generated.resources.downloads_select_all_season
import nuvio.composeapp.generated.resources.downloads_select_by_me
import nuvio.composeapp.generated.resources.downloads_select_episodes
import nuvio.composeapp.generated.resources.downloads_source_failed
import nuvio.composeapp.generated.resources.downloads_source_found
import nuvio.composeapp.generated.resources.downloads_source_found_one
import nuvio.composeapp.generated.resources.downloads_source_no_sources
import nuvio.composeapp.generated.resources.downloads_source_searching
import nuvio.composeapp.generated.resources.streams_download_file
import org.jetbrains.compose.resources.stringResource

private enum class EpisodeDownloadStep {
    Start,
    Episodes,
    Sources,
    AutoSelecting,
    Complete,
}

private const val SHEET_ANIMATION_MS = 240

@Composable
fun EpisodeDownloadFlowSheet(
    meta: MetaDetails,
    defaultEpisode: MetaVideo?,
    initialEpisodes: List<MetaVideo> = emptyList(),
    showStartPrompt: Boolean = true,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val isMovie = meta.type == "movie"
    val movieVideo = remember(meta) {
        meta.videos.firstOrNull { it.season == null && it.episode == null }
    }
    val candidates = remember(meta.videos) {
        meta.videos
            .filter { it.available && (it.season != null || it.episode != null) }
            .sortedWith(metaVideoSeasonEpisodeComparator)
    }
    var selectedEpisodes by remember(meta.id) {
        mutableStateOf(initialEpisodes.toSet())
    }
    var step by remember(meta.id, showStartPrompt, isMovie) {
        mutableStateOf(
            when {
                isMovie -> EpisodeDownloadStep.Sources
                !showStartPrompt -> EpisodeDownloadStep.Episodes
                else -> EpisodeDownloadStep.Start
            },
        )
    }
    var options by remember(meta.id) { mutableStateOf<List<DownloadSourceOption>>(emptyList()) }
    var selectedOption by remember(meta.id) { mutableStateOf<DownloadSourceOption?>(null) }
    var isLoadingSources by remember(meta.id) { mutableStateOf(false) }
    var providerStatuses by remember(meta.id) { mutableStateOf<List<ProviderSearchStatus>>(emptyList()) }
    var result by remember(meta.id) { mutableStateOf<BatchDownloadResult?>(null) }
    var settings by remember(meta.id) {
        mutableStateOf(EpisodeDownloadSettingsStorage.load())
    }
    var showSettings by remember(meta.id) { mutableStateOf(false) }
    var episodeAutoStatuses by remember(meta.id) { mutableStateOf<List<EpisodeAutoSelectStatus>>(emptyList()) }
    var autoJob by remember(meta.id) { mutableStateOf<Job?>(null) }
    var sourceLoadJob by remember(meta.id) { mutableStateOf<Job?>(null) }
    var manualEnqueueJob by remember(meta.id) { mutableStateOf<Job?>(null) }
    var isEnqueuing by remember(meta.id) { mutableStateOf(false) }

    // Automatic mode is a simple binary that follows the settings toggle: when enabled it
    // applies to any non-empty episode selection, so the buttons/states stay consistent.
    val useAutoSelect = !isMovie && settings.autoSelect && selectedEpisodes.isNotEmpty()
    val log = Logger.withTag("EpisodeDownloadFlow")

    fun target(video: MetaVideo) = EpisodeDownloadTarget(
        videoId = video.id,
        parentMetaId = meta.id,
        parentMetaType = meta.type,
        seasonNumber = video.season,
        episodeNumber = video.episode,
        title = video.title,
        thumbnail = video.thumbnail,
        overview = video.overview,
        embeddedStreams = video.streams,
    )

    fun movieTarget() = EpisodeDownloadTarget(
        videoId = meta.id,
        parentMetaId = meta.id,
        parentMetaType = meta.type,
        seasonNumber = null,
        episodeNumber = null,
        title = meta.name,
        thumbnail = meta.poster,
        overview = meta.description,
        embeddedStreams = movieVideo?.streams.orEmpty(),
    )

    fun loadSources() {
        if (isLoadingSources) return
        val targets = if (isMovie) {
            listOf(movieTarget())
        } else {
            selectedEpisodes.map(::target)
        }
        if (targets.isEmpty()) return
        isLoadingSources = true
        options = emptyList()
        providerStatuses = emptyList()
        selectedOption = null
        showSettings = false
        step = EpisodeDownloadStep.Sources
        log.d { "loadSources: start targets=${targets.size} isMovie=$isMovie" }
        sourceLoadJob = scope.launch {
            try {
                // Resolve the source list for each selected episode independently: if the
                // first episode has no compatible sources, try the next one instead of
                // failing the whole flow. Options stream in progressively via onFound so
                // the user can start picking/downloading while the search continues.
                var runningCount = 0
                for (target in targets) {
                    runningCount++
                    val resolved = try {
                        DownloadSourceResolver.resolveOptions(
                            contentType = meta.type,
                            target = target,
                            onProgress = { statuses -> providerStatuses = statuses },
                            onFound = { found ->
                                val merged = (options + found).toDownloadOptions()
                                options = merged
                                log.d {
                                    "loadSources: progressive options=${merged.size} " +
                                        "(+${found.size} from ${target.title ?: target.videoId}) " +
                                        "running=$runningCount/${targets.size}"
                                }
                            },
                        )
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Throwable) {
                        log.w(error) {
                            "loadSources: source resolution failed for ${target.title ?: target.videoId}"
                        }
                        emptyList()
                    }
                    if (resolved.isNotEmpty()) {
                        options = (options + resolved).toDownloadOptions()
                        break
                    }
                }
                log.d {
                    "loadSources: done targets=${targets.size} options=${options.size} " +
                        "providers=${providerStatuses.size}"
                }
            } finally {
                isLoadingSources = false
                sourceLoadJob = null
            }
        }
    }

    fun startAutoDownload() {
        if (step == EpisodeDownloadStep.AutoSelecting || isEnqueuing) return
        val targets = if (isMovie) {
            listOf(movieTarget())
        } else {
            selectedEpisodes.map(::target)
        }
        if (targets.isEmpty()) return
        showSettings = false
        episodeAutoStatuses = emptyList()
        step = EpisodeDownloadStep.AutoSelecting
        autoJob = scope.launch {
            try {
                result = EpisodeDownloadCoordinator.enqueueAuto(
                    contentType = meta.type,
                    parentMetaId = meta.id,
                    parentMetaType = meta.type,
                    title = meta.name,
                    logo = meta.logo,
                    poster = meta.poster,
                    background = meta.background,
                    targets = targets,
                    settings = settings,
                    onProgress = { statuses -> episodeAutoStatuses = statuses },
                )
                step = EpisodeDownloadStep.Complete
            } catch (error: CancellationException) {
                log.d { "startAutoDownload: auto selection cancelled" }
                throw error
            } catch (error: Throwable) {
                log.w(error) { "startAutoDownload: auto download failed" }
                result = BatchDownloadResult(started = 0, replaced = 0, awaitingSource = targets.size)
                step = EpisodeDownloadStep.Complete
            }
        }
    }

    fun cancelAutoSelecting() {
        autoJob?.cancel()
        autoJob = null
        episodeAutoStatuses = emptyList()
        step = if (isMovie) EpisodeDownloadStep.Sources else EpisodeDownloadStep.Episodes
    }

    // The sheet animates in/out: open slides up, dismiss slides back down before the parent
    // actually removes it from composition.
    var sheetVisible by remember(meta.id) { mutableStateOf(false) }
    var dismissing by remember(meta.id) { mutableStateOf(false) }
    var dismissCompleted by remember(meta.id) { mutableStateOf(false) }

    fun completeDismiss() {
        if (dismissCompleted) return
        dismissCompleted = true
        onDismiss()
    }

    fun dismissSheet() {
        if (dismissing) return
        dismissing = true
        sheetVisible = false
        scope.launch {
            delay(SHEET_ANIMATION_MS.toLong() + 40L)
            completeDismiss()
        }
    }

    LaunchedEffect(meta.id) {
        log.d {
            "sheet: opened metaId=${meta.id} type=${meta.type} isMovie=$isMovie " +
                "showStartPrompt=$showStartPrompt episodes=${initialEpisodes.size}"
        }
        sheetVisible = true
        if (isMovie) loadSources()
    }

    DisposableEffect(meta.id, isMovie) {
        onDispose {
            autoJob?.cancel()
            sourceLoadJob?.cancel()
            manualEnqueueJob?.cancel()
            if (dismissing) completeDismiss()
            log.d { "sheet: dismissed metaId=${meta.id} step=$step options=${options.size}" }
        }
    }

    val sheetClickIntercept = remember { MutableInteractionSource() }

    key(meta.id, isMovie, showStartPrompt) {
        Box(modifier = Modifier.fillMaxSize().zIndex(4f)) {
            AnimatedVisibility(
                visible = sheetVisible,
                enter = fadeIn(animationSpec = tween(SHEET_ANIMATION_MS)),
                exit = fadeOut(animationSpec = tween(SHEET_ANIMATION_MS / 2)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(onClick = ::dismissSheet),
                )
            }
            AnimatedVisibility(
                visible = sheetVisible,
                enter = slideInVertically(
                    animationSpec = tween(SHEET_ANIMATION_MS),
                    initialOffsetY = { it },
                ) + fadeIn(animationSpec = tween(SHEET_ANIMATION_MS)),
                exit = slideOutVertically(
                    animationSpec = tween(SHEET_ANIMATION_MS),
                    targetOffsetY = { it },
                ) + fadeOut(animationSpec = tween(SHEET_ANIMATION_MS / 2)),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .heightIn(max = 700.dp)
                        .navigationBarsPadding()
                        .clickable(
                            interactionSource = sheetClickIntercept,
                            indication = null,
                            onClick = {},
                        ),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.nuvio.colors.surfaceSheet,
                ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp, bottom = 6.dp)
                        .size(width = 54.dp, height = 5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.nuvio.colors.borderDefault),
                )
                if (showSettings && (step == EpisodeDownloadStep.Episodes || step == EpisodeDownloadStep.Sources)) {
                    DownloadSettingsPanel(
                        settings = settings,
                        onSettingsChange = { updated ->
                            settings = updated
                            EpisodeDownloadSettingsStorage.save(updated)
                        },
                    )
                }
                AnimatedContent(
                    targetState = step,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                    label = "episode_download_flow",
                ) { currentStep ->
                    when (currentStep) {
                        EpisodeDownloadStep.Start -> {
                            val downloadTarget = defaultEpisode ?: candidates.firstOrNull()
                            EpisodeDownloadStart(
                                episode = downloadTarget,
                                onDownloadDefault = {
                                    val target = downloadTarget ?: return@EpisodeDownloadStart
                                    selectedEpisodes = setOf(target)
                                    loadSources()
                                },
                                onSelectByMe = { step = EpisodeDownloadStep.Episodes },
                                onDismiss = ::dismissSheet,
                            )
                        }

                        EpisodeDownloadStep.Episodes -> {
                            EpisodeDownloadPicker(
                                episodes = candidates,
                                selectedEpisodes = selectedEpisodes,
                                fallbackArtwork = meta.background ?: meta.poster,
                                onToggle = { episode ->
                                    selectedEpisodes = if (episode in selectedEpisodes) {
                                        selectedEpisodes - episode
                                    } else {
                                        selectedEpisodes + episode
                                    }
                                },
                                onSelectSeason = { season ->
                                    selectedEpisodes = selectedEpisodes + candidates.filter { it.season == season }
                                },
                                onClear = { selectedEpisodes = emptySet() },
                                onContinue = {
                                    if (useAutoSelect) {
                                        startAutoDownload()
                                    } else {
                                        loadSources()
                                    }
                                },
                                onDismiss = ::dismissSheet,
                                useAutoSelect = useAutoSelect,
                                onSettingsToggle = { showSettings = !showSettings },
                            )
                        }

                        EpisodeDownloadStep.Sources -> EpisodeDownloadSourcePicker(
                            options = options,
                            selectedOption = selectedOption,
                            isLoading = isLoadingSources,
                            providerStatuses = providerStatuses,
                            message = stringResource(
                                if (isMovie) {
                                    Res.string.downloads_choose_source_movie_message
                                } else {
                                    Res.string.downloads_choose_source_message
                                },
                            ),
                            emptyMessage = stringResource(
                                if (isMovie) {
                                    Res.string.downloads_no_compatible_sources_movie
                                } else {
                                    Res.string.downloads_no_compatible_sources
                                },
                            ),
                            showBack = !isMovie,
                            useAutoSelect = useAutoSelect,
                            showAutomaticAction = settings.autoSelect,
                            isSubmitting = isEnqueuing,
                            showSettingsButton = !isMovie && selectedEpisodes.isNotEmpty(),
                            onSettingsToggle = { showSettings = !showSettings },
                            onSelect = { option -> if (!isEnqueuing) selectedOption = option },
                            onDownload = {
                                if (isEnqueuing) return@EpisodeDownloadSourcePicker
                                selectedOption?.let { option ->
                                    log.d {
                                        "download: tapped manual option=${option.providerName}/${option.qualityLabel} " +
                                            "episodes=${if (isMovie) 1 else selectedEpisodes.size}"
                                    }
                                    isEnqueuing = true
                                    manualEnqueueJob = scope.launch {
                                        try {
                                            result = EpisodeDownloadCoordinator.enqueue(
                                                contentType = meta.type,
                                                parentMetaId = meta.id,
                                                parentMetaType = meta.type,
                                                title = meta.name,
                                                logo = meta.logo,
                                                poster = meta.poster,
                                                background = meta.background,
                                                targets = if (isMovie) {
                                                    listOf(movieTarget())
                                                } else {
                                                    selectedEpisodes.map(::target)
                                                },
                                                selectedOption = option,
                                                queueMode = settings.downloadMode,
                                            )
                                            step = EpisodeDownloadStep.Complete
                                        } catch (error: CancellationException) {
                                            throw error
                                        } catch (error: Throwable) {
                                            log.w(error) { "download: manual enqueue failed" }
                                            result = BatchDownloadResult(started = 0, replaced = 0, awaitingSource = 0)
                                            step = EpisodeDownloadStep.Complete
                                        } finally {
                                            isEnqueuing = false
                                            manualEnqueueJob = null
                                        }
                                    }
                                }
                            },
                            onAutomaticDownload = { if (!isEnqueuing) startAutoDownload() },
                            onBack = {
                                if (!isMovie) {
                                    sourceLoadJob?.cancel()
                                    sourceLoadJob = null
                                    isLoadingSources = false
                                    selectedOption = null
                                    showSettings = false
                                    step = if (showStartPrompt) EpisodeDownloadStep.Start else EpisodeDownloadStep.Episodes
                                }
                            },
                        )

                        EpisodeDownloadStep.AutoSelecting -> EpisodeDownloadAutoSelecting(
                            statuses = episodeAutoStatuses,
                            onCancel = { cancelAutoSelecting() },
                        )

                        EpisodeDownloadStep.Complete -> EpisodeDownloadComplete(
                            result = result,
                            isMovie = isMovie,
                            onDone = ::dismissSheet,
                        )
                    }
                }
            }
        }
        }
        }
    }
    PlatformBackHandler(enabled = true, onBack = ::dismissSheet)
}
}

@Composable
private fun EpisodeDownloadLoading() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(Res.string.downloads_finding_sources),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProviderSearchPanel(
    statuses: List<ProviderSearchStatus>,
    isLoading: Boolean,
    emptyMessage: String? = null,
) {
    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
        if (isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = stringResource(Res.string.downloads_finding_sources),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Box(modifier = Modifier.weight(1f))
                val resolved = statuses.count { it.state != ProviderSearchState.Searching }
                if (resolved > 0) {
                    Text(
                        text = "$resolved/${statuses.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (!emptyMessage.isNullOrBlank()) {
            Text(
                text = emptyMessage,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            items(statuses, key = { status -> status.providerAddonId }) { status ->
                ProviderStatusRow(status = status)
            }
        }
    }
}

@Composable
private fun ProviderStatusRow(status: ProviderSearchStatus) {
    val stateColor = when (status.state) {
        ProviderSearchState.Searching -> MaterialTheme.colorScheme.primary
        ProviderSearchState.Found -> MaterialTheme.colorScheme.primary
        ProviderSearchState.NoSources -> MaterialTheme.colorScheme.onSurfaceVariant
        ProviderSearchState.Failed -> MaterialTheme.colorScheme.error
    }
    val statusText = when (status.state) {
        ProviderSearchState.Searching -> stringResource(Res.string.downloads_source_searching)
        ProviderSearchState.Found -> if (status.sourceCount == 1) {
            stringResource(Res.string.downloads_source_found_one, status.sourceCount)
        } else {
            stringResource(Res.string.downloads_source_found, status.sourceCount)
        }
        ProviderSearchState.NoSources -> stringResource(Res.string.downloads_source_no_sources)
        ProviderSearchState.Failed -> stringResource(Res.string.downloads_source_failed)
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (status.state) {
                ProviderSearchState.Searching -> CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
                ProviderSearchState.Found -> Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = stateColor,
                    modifier = Modifier.size(18.dp),
                )
                ProviderSearchState.NoSources, ProviderSearchState.Failed -> Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = stateColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = status.providerName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = stateColor,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun EpisodeDownloadStart(
    episode: MetaVideo?,
    onDownloadDefault: () -> Unit,
    onSelectByMe: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = episode?.title.orEmpty().ifBlank { stringResource(Res.string.downloads_select_episodes) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(Res.string.downloads_episode_prompt_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(Res.string.downloads_episode_prompt_message, title),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        episode?.let {
            EpisodeDownloadCard(
                episode = it,
                fallbackArtwork = null,
                selected = true,
                onClick = onDownloadDefault,
            )
        }
        NuvioPrimaryButton(
            text = stringResource(Res.string.streams_download_file),
            enabled = episode != null,
            onClick = onDownloadDefault,
        )
        SecondaryDownloadButton(
            text = stringResource(Res.string.downloads_select_by_me),
            onClick = onSelectByMe,
        )
        SecondaryDownloadButton(
            text = stringResource(Res.string.action_cancel),
            onClick = onDismiss,
        )
    }
}

@Composable
private fun EpisodeDownloadPicker(
    episodes: List<MetaVideo>,
    selectedEpisodes: Set<MetaVideo>,
    fallbackArtwork: String?,
    onToggle: (MetaVideo) -> Unit,
    onSelectSeason: (Int?) -> Unit,
    onClear: () -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
    useAutoSelect: Boolean,
    onSettingsToggle: () -> Unit,
) {
    val grouped = remember(episodes) { episodes.groupBy(MetaVideo::season) }
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.downloads_select_episodes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (episodes.size > 1) {
                    Surface(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onSettingsToggle),
                        shape = RoundedCornerShape(12.dp),
                        color = if (useAutoSelect) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = stringResource(Res.string.downloads_download_settings),
                                tint = if (useAutoSelect) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
            Text(
                text = stringResource(Res.string.downloads_selected_episodes, selectedEpisodes.size),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            grouped.forEach { (season, seasonEpisodes) ->
                item(key = "download-season-$season") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (season == null || season == 0) "Specials" else "Season $season",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(Res.string.downloads_select_all_season),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onSelectSeason(season) },
                        )
                    }
                }
                items(seasonEpisodes, key = { episode -> "download-${episode.id}-${episode.season}-${episode.episode}" }) { episode ->
                    EpisodeDownloadCard(
                        episode = episode,
                        fallbackArtwork = fallbackArtwork,
                        selected = episode in selectedEpisodes,
                        modifier = Modifier.padding(horizontal = 20.dp),
                        onClick = { onToggle(episode) },
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SecondaryDownloadButton(
                text = stringResource(Res.string.downloads_clear_selection),
                modifier = Modifier.weight(0.9f),
                onClick = onClear,
            )
            if (useAutoSelect) {
                AutomaticDownloadButton(
                    text = stringResource(Res.string.downloads_automatic),
                    modifier = Modifier.weight(1.4f),
                    enabled = selectedEpisodes.isNotEmpty(),
                    onClick = onContinue,
                )
            } else {
                NuvioPrimaryButton(
                    text = stringResource(Res.string.downloads_continue),
                    modifier = Modifier.weight(1.4f),
                    enabled = selectedEpisodes.isNotEmpty(),
                    onClick = onContinue,
                )
            }
        }
        SecondaryDownloadButton(
            text = stringResource(Res.string.action_cancel),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            onClick = onDismiss,
        )
    }
}

@Composable
private fun QualityFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun EpisodeDownloadSourcePicker(
    options: List<DownloadSourceOption>,
    selectedOption: DownloadSourceOption?,
    isLoading: Boolean,
    providerStatuses: List<ProviderSearchStatus>,
    message: String,
    emptyMessage: String,
    showBack: Boolean,
    useAutoSelect: Boolean,
    showAutomaticAction: Boolean,
    isSubmitting: Boolean,
    showSettingsButton: Boolean,
    onSettingsToggle: () -> Unit,
    onSelect: (DownloadSourceOption) -> Unit,
    onDownload: () -> Unit,
    onAutomaticDownload: () -> Unit,
    onBack: () -> Unit,
) {
    var qualityFilter by remember { mutableStateOf<Int?>(null) }
    val qualityHeights = remember(options) {
        options.map { it.downloadOptionQualityHeight() }.filter { it > 0 }.distinct().sortedDescending()
    }
    val filteredOptions = remember(options, qualityFilter) {
        if (qualityFilter == null) {
            options
        } else {
            options.filter { it.downloadOptionQualityHeight() == qualityFilter }
        }
    }
    LaunchedEffect(qualityHeights) {
        if (qualityFilter != null && qualityFilter !in qualityHeights) {
            qualityFilter = null
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.downloads_choose_source),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (showSettingsButton) {
                    Surface(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onSettingsToggle),
                        shape = RoundedCornerShape(12.dp),
                        color = if (useAutoSelect) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = stringResource(Res.string.downloads_download_settings),
                                tint = if (useAutoSelect) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (qualityHeights.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QualityFilterChip(
                        label = stringResource(Res.string.downloads_filter_all),
                        selected = qualityFilter == null,
                        onClick = { qualityFilter = null },
                    )
                    qualityHeights.forEach { height ->
                        QualityFilterChip(
                            label = if (height >= 2160) "4K" else "${height}p",
                            selected = qualityFilter == height,
                            onClick = { qualityFilter = height },
                        )
                    }
                }
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
        ) {
            when {
                // Manual mode: options appear progressively while providers are still
                // searching, so the user can pick/download immediately.
                options.isNotEmpty() -> {
                    if (filteredOptions.isEmpty()) {
                        item(key = "filter-empty") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(Res.string.downloads_filter_empty),
                                    modifier = Modifier.padding(horizontal = 32.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        items(
                            filteredOptions,
                            key = { option -> "${option.providerAddonId}:${option.qualityKey}:${option.stream.streamLabel}" },
                        ) { option ->
                            SourceOptionCard(
                                option = option,
                                selected = option == selectedOption,
                                onClick = { if (!isSubmitting) onSelect(option) },
                            )
                        }
                    }
                    if (isLoading) {
                        item(key = "still-searching") {
                            StillSearchingFooter(
                                resolved = options.size,
                                total = providerStatuses.size,
                            )
                        }
                    }
                }
                providerStatuses.isNotEmpty() -> item(key = "provider-panel") {
                    ProviderSearchPanel(
                        statuses = providerStatuses,
                        isLoading = isLoading,
                        emptyMessage = if (!isLoading) emptyMessage else null,
                    )
                }
                isLoading -> item(key = "loading") { EpisodeDownloadLoading() }
                else -> item(key = "empty") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = emptyMessage,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (showBack) {
                SecondaryDownloadButton(
                    text = stringResource(Res.string.action_back),
                    modifier = Modifier.weight(0.8f),
                    enabled = !isSubmitting,
                    onClick = onBack,
                )
            }
            if (showAutomaticAction) {
                AutomaticDownloadButton(
                    text = stringResource(Res.string.downloads_automatic),
                    modifier = Modifier.weight(1f),
                    enabled = !isSubmitting,
                    onClick = onAutomaticDownload,
                )
            }
            if (!useAutoSelect) {
                DownloadPrimaryButton(
                    text = stringResource(Res.string.streams_download_file),
                    modifier = Modifier.weight(1.2f),
                    enabled = selectedOption != null && !isSubmitting,
                    isLoading = isSubmitting,
                    onClick = onDownload,
                )
            }
        }
    }
}

@Composable
private fun EpisodeDownloadComplete(
    result: BatchDownloadResult?,
    isMovie: Boolean,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val nothingFound = result == null ||
            (result.started == 0 && result.replaced == 0 && result.awaitingSource == 0)
        if (nothingFound) {
            Text(
                text = stringResource(Res.string.downloads_source_no_sources),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(
                    if (isMovie) {
                        Res.string.downloads_no_compatible_sources_movie
                    } else {
                        Res.string.downloads_no_compatible_sources
                    },
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(
                    if (result.started == 0 && result.replaced == 0) {
                        Res.string.downloads_awaiting_source
                    } else {
                        Res.string.downloads_download_started
                    },
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            result.let { summary ->
                Text(
                    text = stringResource(
                        Res.string.downloads_batch_summary,
                        summary.started,
                        summary.replaced,
                        summary.awaitingSource,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (summary.awaitingSource > 0) {
                    Text(
                        text = stringResource(Res.string.downloads_awaiting_source),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        NuvioPrimaryButton(text = stringResource(Res.string.action_done), onClick = onDone)
    }
}

@Composable
private fun EpisodeDownloadCard(
    episode: MetaVideo,
    fallbackArtwork: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(180),
        label = "episode_download_selected",
    )
    val artwork = episode.thumbnail ?: episode.seasonPoster ?: fallbackArtwork
    Surface(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(width = 124.dp, height = 70.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                if (!artwork.isNullOrBlank()) {
                    AsyncImage(
                        model = artwork,
                        contentDescription = episode.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = localizedSeasonEpisodeCode(episode.season, episode.episode).orEmpty(),
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
                Text(
                    text = episode.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (selected) {
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(7.dp).size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceOptionCard(
    option: DownloadSourceOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "download_source_selected",
    )
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = color,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Rounded.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(option.providerName, fontWeight = FontWeight.SemiBold)
                Text(option.qualityLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun rememberDownloadPress(): Pair<MutableInteractionSource, Float> {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "download_press_scale",
    )
    return interactionSource to scale
}

@Composable
private fun StillSearchingFooter(
    resolved: Int,
    total: Int,
) {
    val transition = rememberInfiniteTransition(label = "still_searching")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "still_searching_alpha",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .graphicsLayer { this.alpha = alpha },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            strokeWidth = 2.dp,
        )
        Text(
            text = stringResource(Res.string.downloads_finding_sources),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (total > 0) {
            Text(
                text = "$resolved/$total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DownloadPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val (interactionSource, pressScale) = rememberDownloadPress()
    Box(
        modifier = modifier.graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        },
        contentAlignment = Alignment.Center,
    ) {
        NuvioPrimaryButton(
            text = text,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = enabled,
            interactionSource = interactionSource,
        )
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        }
    }
}

@Composable
private fun SecondaryDownloadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val (interactionSource, pressScale) = rememberDownloadPress()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(enabled = enabled, interactionSource = interactionSource, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (enabled) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AutomaticDownloadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val automaticBrush = rememberAutomaticActionBrush()
    val glowColor = automaticBrush?.glowColor
    val containerBrush = automaticBrush?.brush ?: SolidColor(MaterialTheme.colorScheme.primary)
    val contentColor = Color(0xFF111111)
    val shape = RoundedCornerShape(18.dp)
    val (interactionSource, pressScale) = rememberDownloadPress()
    Surface(
        modifier = modifier
            .height(52.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .then(
                if (enabled && glowColor != null) {
                    Modifier.shadow(
                        elevation = 12.dp,
                        shape = shape,
                        ambientColor = glowColor,
                        spotColor = glowColor,
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        shape = shape,
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(containerBrush),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DownloadSettingsPanel(
    settings: EpisodeDownloadSettings,
    onSettingsChange: (EpisodeDownloadSettings) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.downloads_download_settings),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSettingsChange(settings.copy(autoSelect = !settings.autoSelect)) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(Res.string.downloads_auto_select),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(Res.string.downloads_auto_select_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            DownloadToggle(
                enabled = settings.autoSelect,
                onToggle = { onSettingsChange(settings.copy(autoSelect = !settings.autoSelect)) },
            )
        }
        Text(
            text = stringResource(Res.string.downloads_preferred_quality),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DownloadSettingsChip(
                text = stringResource(Res.string.downloads_quality_best),
                selected = settings.preferredQuality == DownloadPreferredQuality.Best,
                onClick = { onSettingsChange(settings.copy(preferredQuality = DownloadPreferredQuality.Best)) },
            )
            DownloadSettingsChip(
                text = "4K",
                selected = settings.preferredQuality == DownloadPreferredQuality.Q2160,
                onClick = { onSettingsChange(settings.copy(preferredQuality = DownloadPreferredQuality.Q2160)) },
            )
            DownloadSettingsChip(
                text = "1080p",
                selected = settings.preferredQuality == DownloadPreferredQuality.Q1080,
                onClick = { onSettingsChange(settings.copy(preferredQuality = DownloadPreferredQuality.Q1080)) },
            )
            DownloadSettingsChip(
                text = "720p",
                selected = settings.preferredQuality == DownloadPreferredQuality.Q720,
                onClick = { onSettingsChange(settings.copy(preferredQuality = DownloadPreferredQuality.Q720)) },
            )
            DownloadSettingsChip(
                text = "480p",
                selected = settings.preferredQuality == DownloadPreferredQuality.Q480,
                onClick = { onSettingsChange(settings.copy(preferredQuality = DownloadPreferredQuality.Q480)) },
            )
            DownloadSettingsChip(
                text = "360p",
                selected = settings.preferredQuality == DownloadPreferredQuality.Q360,
                onClick = { onSettingsChange(settings.copy(preferredQuality = DownloadPreferredQuality.Q360)) },
            )
        }
        Text(
            text = stringResource(Res.string.downloads_download_mode),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DownloadSettingsChip(
                text = stringResource(Res.string.downloads_download_all_at_once),
                selected = settings.downloadMode == DownloadQueueMode.AllAtOnce,
                onClick = { onSettingsChange(settings.copy(downloadMode = DownloadQueueMode.AllAtOnce)) },
            )
            DownloadSettingsChip(
                text = stringResource(Res.string.downloads_download_one_at_a_time),
                selected = settings.downloadMode == DownloadQueueMode.OneAtATime,
                onClick = { onSettingsChange(settings.copy(downloadMode = DownloadQueueMode.OneAtATime)) },
            )
        }
    }
}

@Composable
private fun DownloadToggle(
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val trackColor by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(180),
        label = "download_settings_toggle",
    )
    Surface(
        modifier = Modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(50),
        color = trackColor,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .size(22.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onPrimary),
            )
        }
    }
}

@Composable
private fun DownloadSettingsChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(180),
        label = "download_settings_chip",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(180),
        label = "download_settings_chip_content",
    )
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = containerColor,
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun EpisodeDownloadAutoSelecting(
    statuses: List<EpisodeAutoSelectStatus>,
    onCancel: () -> Unit,
) {
    val anySearching = statuses.any { it.state == ProviderSearchState.Searching }
    val anyFound = statuses.any { it.state == ProviderSearchState.Found }
    val allTerminal = statuses.isNotEmpty() && !anySearching
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.downloads_auto_selecting),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.downloads_auto_select_desc),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (allTerminal && !anyFound) {
            Text(
                text = stringResource(Res.string.downloads_no_compatible_sources),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(
            modifier = Modifier.heightIn(max = 460.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(statuses, key = { status -> status.target.videoId }) { status ->
                EpisodeAutoSelectCard(status = status)
            }
        }
        SecondaryDownloadButton(
            text = stringResource(Res.string.action_cancel),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 8.dp),
            onClick = onCancel,
        )
    }
}

@Composable
private fun EpisodeAutoSelectCard(
    status: EpisodeAutoSelectStatus,
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (status.state) {
            ProviderSearchState.Searching -> MaterialTheme.colorScheme.surfaceContainerHigh
            ProviderSearchState.Found -> MaterialTheme.colorScheme.primaryContainer
            ProviderSearchState.NoSources -> MaterialTheme.colorScheme.surfaceContainerHigh
            ProviderSearchState.Failed -> MaterialTheme.colorScheme.errorContainer
        },
        animationSpec = tween(300),
        label = "episode_auto_select_card",
    )
    val code = localizedSeasonEpisodeCode(status.target.seasonNumber, status.target.episodeNumber).orEmpty()
    val header = buildString {
        if (code.isNotBlank()) append(code).append("  ")
        append(status.target.title.orEmpty())
    }.ifBlank { status.target.videoId }
    val statusLabel = when (status.state) {
        ProviderSearchState.Searching -> stringResource(Res.string.downloads_source_searching)
        ProviderSearchState.Found -> status.qualityLabel.orEmpty().ifBlank { stringResource(Res.string.downloads_source_found_one, 1) }
        ProviderSearchState.NoSources -> stringResource(Res.string.downloads_source_no_sources)
        ProviderSearchState.Failed -> stringResource(Res.string.downloads_source_failed)
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (status.state) {
                ProviderSearchState.Searching -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
                ProviderSearchState.Found -> Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                ProviderSearchState.NoSources -> Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                ProviderSearchState.Failed -> Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = header,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = statusLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
