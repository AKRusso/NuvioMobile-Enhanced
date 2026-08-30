package com.nuvio.app.features.anime

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.simkl.SimklAuthRepository
import com.nuvio.app.features.simkl.SimklConnectionMode
import com.nuvio.app.features.simkl.SimklEditableEntry
import com.nuvio.app.features.simkl.SimklEditorRepository
import com.nuvio.app.features.simkl.SimklSearchResult
import com.nuvio.app.features.simkl.SimklSyncRepository
import com.nuvio.app.features.simkl.simklPosterUrl
import com.nuvio.app.features.tracking.TrackingEpisode
import com.nuvio.app.features.tracking.TrackingListStatus
import com.nuvio.app.features.tracking.TrackingMediaReference
import com.nuvio.app.features.tracking.TrackingRefreshIntent
import com.nuvio.app.features.tracking.buildTrackingMediaReference
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_back
import nuvio.composeapp.generated.resources.action_cancel
import nuvio.composeapp.generated.resources.action_delete
import nuvio.composeapp.generated.resources.action_ok
import nuvio.composeapp.generated.resources.anime_tracking_advanced
import nuvio.composeapp.generated.resources.anime_tracking_advanced_audio
import nuvio.composeapp.generated.resources.anime_tracking_advanced_characters
import nuvio.composeapp.generated.resources.anime_tracking_advanced_enjoyment
import nuvio.composeapp.generated.resources.anime_tracking_advanced_story
import nuvio.composeapp.generated.resources.anime_tracking_advanced_visuals
import nuvio.composeapp.generated.resources.anime_tracking_automatic_mapping
import nuvio.composeapp.generated.resources.anime_tracking_change
import nuvio.composeapp.generated.resources.anime_tracking_clear
import nuvio.composeapp.generated.resources.anime_tracking_dates_notes
import nuvio.composeapp.generated.resources.anime_tracking_delete_confirmation
import nuvio.composeapp.generated.resources.anime_tracking_delete_title
import nuvio.composeapp.generated.resources.anime_tracking_editor_title
import nuvio.composeapp.generated.resources.anime_tracking_finish_date
import nuvio.composeapp.generated.resources.anime_tracking_favourite
import nuvio.composeapp.generated.resources.anime_tracking_hidden
import nuvio.composeapp.generated.resources.anime_tracking_link_title
import nuvio.composeapp.generated.resources.anime_tracking_mal_priority
import nuvio.composeapp.generated.resources.anime_tracking_mal_rewatch_value
import nuvio.composeapp.generated.resources.anime_tracking_option_high
import nuvio.composeapp.generated.resources.anime_tracking_option_low
import nuvio.composeapp.generated.resources.anime_tracking_option_medium
import nuvio.composeapp.generated.resources.anime_tracking_option_none
import nuvio.composeapp.generated.resources.anime_tracking_option_very_high
import nuvio.composeapp.generated.resources.anime_tracking_option_very_low
import nuvio.composeapp.generated.resources.anime_tracking_memo
import nuvio.composeapp.generated.resources.anime_tracking_memo_hint
import nuvio.composeapp.generated.resources.anime_tracking_no_accounts
import nuvio.composeapp.generated.resources.anime_tracking_not_linked
import nuvio.composeapp.generated.resources.anime_tracking_notes
import nuvio.composeapp.generated.resources.anime_tracking_open
import nuvio.composeapp.generated.resources.anime_tracking_private
import nuvio.composeapp.generated.resources.anime_tracking_progress
import nuvio.composeapp.generated.resources.anime_tracking_provider_section
import nuvio.composeapp.generated.resources.anime_tracking_public_memo
import nuvio.composeapp.generated.resources.anime_tracking_save_progress
import nuvio.composeapp.generated.resources.anime_tracking_score
import nuvio.composeapp.generated.resources.anime_tracking_search_hint
import nuvio.composeapp.generated.resources.anime_tracking_season_all
import nuvio.composeapp.generated.resources.anime_tracking_season_label
import nuvio.composeapp.generated.resources.anime_tracking_start_date
import nuvio.composeapp.generated.resources.anime_tracking_status
import nuvio.composeapp.generated.resources.anime_tracking_status_completed
import nuvio.composeapp.generated.resources.anime_tracking_status_dropped
import nuvio.composeapp.generated.resources.anime_tracking_status_on_hold
import nuvio.composeapp.generated.resources.anime_tracking_status_plan
import nuvio.composeapp.generated.resources.anime_tracking_status_rewatching
import nuvio.composeapp.generated.resources.anime_tracking_status_watching
import nuvio.composeapp.generated.resources.anime_tracking_total_rewatches
import nuvio.composeapp.generated.resources.anime_tracking_unrated
import nuvio.composeapp.generated.resources.anime_tracking_untrack
import nuvio.composeapp.generated.resources.anime_tracking_update_failed
import org.jetbrains.compose.resources.stringResource
import kotlin.math.max
import kotlin.math.roundToInt

private val OledSheetBg = Color(0xFF05060A)
private val OledCardBg = Color(0xFF0F111A)
private val OledCardBorder = Color(0xFF1E2235)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFA1A5B7)
private val AniListColor = Color(0xFF02A9FF)
private val MyAnimeListColor = Color(0xFF2E51A2)
private val SimklColor = Color(0xFF00C755)

private data class ProviderEditorState(
    val loading: Boolean = true,
    val resolvedId: Int? = null,
    val entry: AnimeTrackingEntry? = null,
    val status: AnimeTrackingUserStatus = AnimeTrackingUserStatus.PLAN_TO_WATCH,
    val progress: Int = 0,
    val score: Double = 0.0,
    val favourite: Boolean = false,
    val isPrivate: Boolean = false,
    val hidden: Boolean = false,
    val advancedScores: AnimeTrackingAdvancedScores = AnimeTrackingAdvancedScores(0.0, 0.0, 0.0, 0.0, 0.0),
    val priority: Int = 0,
    val rewatchValue: Int = 0,
    val dirty: Boolean = false,
    val error: Boolean = false,
    val loadFailed: Boolean = false,
)

private data class SharedEditorState(
    val startDate: AnimeTrackingDate? = null,
    val finishDate: AnimeTrackingDate? = null,
    val repeat: Int = 0,
    val notes: String = "",
    val startDateTouched: Boolean = false,
    val finishDateTouched: Boolean = false,
    val dirty: Boolean = false,
)

private data class SimklEditorState(
    val loading: Boolean = true,
    val media: TrackingMediaReference? = null,
    val entry: SimklEditableEntry? = null,
    val status: TrackingListStatus = TrackingListStatus.PLAN_TO_WATCH,
    val progress: Int = 0,
    val score: Int = 0,
    val memo: String = "",
    val memoIsPrivate: Boolean = true,
    val dirty: Boolean = false,
    val error: Boolean = false,
)

private enum class EditorProvider(val displayName: String) {
    ANILIST("AniList"),
    MAL("MyAnimeList"),
    SIMKL("Simkl"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeTrackingEditorSheet(
    meta: MetaDetails,
    onDismiss: () -> Unit,
    initialSeason: Int? = null,
    initialEpisode: Int? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val aniListAuth by AniListTrackingRepository.uiState.collectAsStateWithLifecycle()
    val malAuth by MyAnimeListTrackingRepository.uiState.collectAsStateWithLifecycle()
    val simklAuth by remember {
        SimklAuthRepository.ensureLoaded()
        SimklAuthRepository.uiState
    }.collectAsStateWithLifecycle()
    val settings by AnimeTrackingSettingsRepository.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val seasons = remember(meta.videos) { meta.videos.mapNotNull { it.season }.distinct().sorted() }
    val providerStates = remember(meta.id) { mutableStateMapOf<AnimeTrackingProvider, ProviderEditorState>() }
    var selectedSeason by remember(meta.id, initialSeason) { mutableStateOf(initialSeason) }
    val selectedEpisode = initialEpisode.takeIf { selectedSeason == initialSeason }
    val media = remember(meta.id, meta.type, meta.name, meta.releaseInfo, selectedSeason, selectedEpisode) {
        buildEditorMedia(meta, selectedSeason, selectedEpisode)
    }
    val connected = listOfNotNull(
        AnimeTrackingProvider.ANILIST.takeIf { aniListAuth.connected },
        AnimeTrackingProvider.MY_ANIME_LIST.takeIf { malAuth.connected },
    )
    val simklConnected = simklAuth.mode == SimklConnectionMode.CONNECTED
    var sharedState by remember(media.stableKey) { mutableStateOf(SharedEditorState()) }
    var simklState by remember(media.stableKey) { mutableStateOf(SimklEditorState()) }
    var searchingProvider by remember(meta.id) { mutableStateOf<EditorProvider?>(null) }
    var deletingProvider by remember(meta.id) { mutableStateOf<EditorProvider?>(null) }
    var refreshKey by remember(meta.id) { mutableIntStateOf(0) }
    var saving by remember(meta.id) { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showFinishDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(media, settings, connected, simklConnected, refreshKey) {
        val loadedEntries = mutableMapOf<AnimeTrackingProvider, AnimeTrackingEntry?>()
        connected.forEach { provider ->
            providerStates[provider] = ProviderEditorState()
            val repository = provider.repository()
            val resolvedId = repository.resolvedMediaId(media)
            val entry = resolvedId?.let { repository.loadEntry(media) }
            loadedEntries[provider] = entry
            providerStates[provider] = ProviderEditorState(
                loading = false,
                resolvedId = resolvedId,
                entry = entry,
                status = entry?.status ?: AnimeTrackingUserStatus.PLAN_TO_WATCH,
                progress = entry?.progress ?: 0,
                score = entry?.score ?: 0.0,
                favourite = entry?.isFavourite == true,
                isPrivate = entry?.isPrivate == true,
                hidden = entry?.hiddenFromStatusLists == true,
                advancedScores = entry?.advancedScores ?: AnimeTrackingAdvancedScores(0.0, 0.0, 0.0, 0.0, 0.0),
                priority = entry?.priority ?: 0,
                rewatchValue = entry?.rewatchValue ?: 0,
                loadFailed = resolvedId != null && entry == null,
            )
        }
        val sharedEntry = loadedEntries[AnimeTrackingProvider.ANILIST]
            ?: loadedEntries[AnimeTrackingProvider.MY_ANIME_LIST]
        sharedState = SharedEditorState(
            startDate = sharedEntry?.startDate,
            finishDate = sharedEntry?.finishDate,
            repeat = sharedEntry?.repeat ?: 0,
            notes = sharedEntry?.notes.orEmpty(),
        )

        if (simklConnected) {
            simklState = SimklEditorState()
            runCatching { SimklSyncRepository.refresh(TrackingRefreshIntent.USER_INITIATED) }
            val mapping = AnimeTrackingSettingsRepository.simklMapping(
                meta.type, meta.id, selectedSeason, selectedEpisode,
            )
            val selectedMedia = when {
                mapping?.second == true -> null
                mapping?.first != null -> media.withSimklId(mapping.first!!)
                else -> runCatching {
                    SimklEditorRepository.lookupAnime(media.ids, media.title, media.year)
                }.getOrNull()?.let { media.withSimklId(it.id, simklPosterUrl(it.poster)) }
            }
            val entry = selectedMedia?.let { runCatching { SimklEditorRepository.loadEntry(it) }.getOrNull() }
            simklState = SimklEditorState(
                loading = false,
                media = selectedMedia,
                entry = entry,
                status = entry?.status ?: TrackingListStatus.PLAN_TO_WATCH,
                progress = entry?.progress ?: 0,
                score = entry?.score ?: 0,
                memo = entry?.memo.orEmpty(),
                memoIsPrivate = entry?.memoIsPrivate ?: true,
                error = selectedMedia != null && entry == null,
            )
        } else {
            simklState = SimklEditorState(loading = false)
        }
    }

    if (showStartDatePicker) {
        TrackingDatePickerDialog(
            initialDate = sharedState.startDate,
            onDismiss = { showStartDatePicker = false },
            onSelected = { date ->
                sharedState = sharedState.copy(startDate = date, startDateTouched = true, dirty = true)
                showStartDatePicker = false
            },
        )
    }
    if (showFinishDatePicker) {
        TrackingDatePickerDialog(
            initialDate = sharedState.finishDate,
            onDismiss = { showFinishDatePicker = false },
            onSelected = { date ->
                sharedState = sharedState.copy(finishDate = date, finishDateTouched = true, dirty = true)
                showFinishDatePicker = false
            },
        )
    }

    deletingProvider?.let { provider ->
        AlertDialog(
            onDismissRequest = { deletingProvider = null },
            title = { Text(stringResource(Res.string.anime_tracking_delete_title, provider.displayName)) },
            text = { Text(stringResource(Res.string.anime_tracking_delete_confirmation, provider.displayName)) },
            confirmButton = {
                TextButton(onClick = {
                    deletingProvider = null
                    scope.launch {
                        val success = when (provider) {
                            EditorProvider.ANILIST, EditorProvider.MAL -> {
                                val animeProvider = provider.toAnimeProvider()
                                val state = providerStates[animeProvider] ?: return@launch
                                animeProvider.repository().deleteEntry(media, state.entry?.listEntryId)
                            }
                            EditorProvider.SIMKL -> simklState.media?.let {
                                runCatching { SimklEditorRepository.deleteEntry(it, true).isComplete }.getOrDefault(false)
                            } ?: false
                        }
                        if (success) refreshKey++ else when (provider) {
                            EditorProvider.SIMKL -> simklState = simklState.copy(error = true)
                            else -> providerStates[provider.toAnimeProvider()] =
                                providerStates[provider.toAnimeProvider()]!!.copy(error = true)
                        }
                    }
                }) { Text(stringResource(Res.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingProvider = null }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }

    NuvioModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        fullHeight = true,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.96f).background(OledSheetBg)
                .padding(horizontal = 18.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SheetHeader(
                title = searchingProvider?.let {
                    stringResource(Res.string.anime_tracking_link_title, it.displayName)
                } ?: stringResource(Res.string.anime_tracking_editor_title),
                onClose = onDismiss,
            )
            if (searchingProvider != null) {
                TrackingSearch(
                    provider = searchingProvider!!,
                    initialQuery = meta.name,
                    modifier = Modifier.weight(1f),
                    onBack = { searchingProvider = null },
                    onSelected = { choice ->
                        when (searchingProvider!!) {
                            EditorProvider.ANILIST, EditorProvider.MAL -> AnimeTrackingSettingsRepository.setMapping(
                                provider = searchingProvider!!.toAnimeProvider(),
                                contentType = meta.type,
                                contentId = meta.id,
                                season = selectedSeason,
                                episode = selectedEpisode,
                                providerMediaId = choice.id.toInt(),
                            )
                            EditorProvider.SIMKL -> AnimeTrackingSettingsRepository.setSimklMapping(
                                meta.type, meta.id, selectedSeason, selectedEpisode, choice.id,
                            )
                        }
                        searchingProvider = null
                        refreshKey++
                    },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item { HeroCard(meta) }
                    if (seasons.isNotEmpty()) item {
                        SeasonPicker(seasons, selectedSeason) { selectedSeason = it }
                    }
                    if (connected.isEmpty() && !simklConnected) item {
                        ProCard { Text(stringResource(Res.string.anime_tracking_no_accounts), color = TextSecondary) }
                    }
                    items(connected, key = { "link-${it.name}" }) { provider ->
                        val state = providerStates[provider] ?: ProviderEditorState()
                        ProviderLinkCard(
                            label = provider.displayName,
                            brandColor = provider.brandColor(),
                            loading = state.loading,
                            linked = state.resolvedId != null,
                            tracked = state.entry?.isTracked == true,
                            title = state.entry?.title ?: meta.name,
                            image = state.entry?.imageUrl ?: meta.poster,
                            error = state.loadFailed,
                            onOpen = { state.resolvedId?.let { uriHandler.openUri(provider.repository().sourceUrl(it)) } },
                            onChange = { searchingProvider = provider.toEditorProvider() },
                            onUntrack = {
                                AnimeTrackingSettingsRepository.setMapping(
                                    provider, meta.type, meta.id, selectedSeason, selectedEpisode, null, true,
                                )
                                refreshKey++
                            },
                            onDelete = { deletingProvider = provider.toEditorProvider() },
                        )
                    }
                    if (simklConnected) item(key = "link-simkl") {
                        ProviderLinkCard(
                            label = "Simkl",
                            brandColor = SimklColor,
                            loading = simklState.loading,
                            linked = simklState.media != null,
                            tracked = simklState.entry?.isTracked == true,
                            title = simklState.media?.title ?: meta.name,
                            image = simklState.media?.posterUrl ?: meta.poster,
                            error = simklState.error && simklState.entry == null,
                            onOpen = { simklState.media?.ids?.simkl?.let { uriHandler.openUri("https://simkl.com/anime/$it") } },
                            onChange = { searchingProvider = EditorProvider.SIMKL },
                            onUntrack = {
                                AnimeTrackingSettingsRepository.setSimklMapping(
                                    meta.type, meta.id, selectedSeason, selectedEpisode, null, true,
                                )
                                refreshKey++
                            },
                            onDelete = { deletingProvider = EditorProvider.SIMKL },
                        )
                    }
                    items(connected, key = { "editor-${it.name}" }) { provider ->
                        ProviderTrackingEditor(
                            provider = provider,
                            state = providerStates[provider] ?: ProviderEditorState(),
                            onStateChange = { providerStates[provider] = it },
                            automaticResolutionDisabled = AnimeTrackingSettingsRepository.mapping(
                                provider, meta.type, meta.id, selectedSeason, selectedEpisode,
                            )?.automaticResolutionDisabled(provider) == true,
                            onUseAutomatic = {
                                AnimeTrackingSettingsRepository.setMapping(
                                    provider, meta.type, meta.id, selectedSeason, selectedEpisode, null, false,
                                )
                                refreshKey++
                            },
                        )
                    }
                    if (simklConnected && simklState.media != null && simklState.entry != null) item(key = "editor-simkl") {
                        SimklTrackingEditor(simklState) { simklState = it }
                    }
                    if (connected.any { providerStates[it]?.resolvedId != null }) item {
                        SharedDatesAndNotes(
                            state = sharedState,
                            onStateChange = { sharedState = it },
                            onStartDate = { showStartDatePicker = true },
                            onFinishDate = { showFinishDatePicker = true },
                        )
                    }
                    items(connected, key = { "advanced-${it.name}" }) { provider ->
                        val state = providerStates[provider] ?: ProviderEditorState()
                        if (state.resolvedId != null && !state.loadFailed) {
                            ProviderAdvancedEditor(provider, state) { providerStates[provider] = it }
                        }
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }

                Button(
                    onClick = {
                        saving = true
                        scope.launch {
                            var allSucceeded = true
                            connected.forEach { provider ->
                                val state = providerStates[provider] ?: return@forEach
                                if (state.resolvedId != null && !state.loadFailed && (state.dirty || sharedState.dirty)) {
                                    val update = AnimeTrackingEntryUpdate(
                                        status = state.status,
                                        progress = state.progress,
                                        score = state.score,
                                        startDate = sharedState.startDate.takeIf { sharedState.startDateTouched },
                                        finishDate = sharedState.finishDate.takeIf { sharedState.finishDateTouched },
                                        clearStartDate = sharedState.startDateTouched && sharedState.startDate == null,
                                        clearFinishDate = sharedState.finishDateTouched && sharedState.finishDate == null,
                                        repeat = sharedState.repeat,
                                        notes = sharedState.notes,
                                        isPrivate = state.isPrivate.takeIf { provider == AnimeTrackingProvider.ANILIST },
                                        hiddenFromStatusLists = state.hidden.takeIf { provider == AnimeTrackingProvider.ANILIST },
                                        advancedScores = state.advancedScores.takeIf { provider == AnimeTrackingProvider.ANILIST },
                                        isFavourite = state.favourite.takeIf { provider == AnimeTrackingProvider.ANILIST },
                                        priority = state.priority.takeIf { provider == AnimeTrackingProvider.MY_ANIME_LIST },
                                        rewatchValue = state.rewatchValue.takeIf { provider == AnimeTrackingProvider.MY_ANIME_LIST },
                                    )
                                    val success = provider.repository().saveEntry(media, update)
                                    providerStates[provider] = state.copy(error = !success, dirty = !success)
                                    if (!success) allSucceeded = false
                                }
                            }
                            if (simklConnected && simklState.dirty) {
                                val entry = simklState.entry
                                val success = entry != null && runCatching {
                                    SimklEditorRepository.saveEntry(
                                        entry = entry,
                                        status = simklState.status,
                                        progress = simklState.progress,
                                        score = simklState.score.takeIf { it > 0 },
                                        memo = simklState.memo,
                                        memoIsPrivate = simklState.memoIsPrivate,
                                    ).isComplete
                                }.getOrDefault(false)
                                simklState = simklState.copy(error = !success, dirty = !success)
                                if (!success) allSucceeded = false
                            }
                            saving = false
                            if (allSucceeded) onDismiss()
                        }
                    },
                    enabled = !saving && (
                        sharedState.dirty || connected.any { providerStates[it]?.dirty == true } || simklState.dirty
                    ),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text(stringResource(Res.string.anime_tracking_save_progress), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(title: String, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(title, color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = TextPrimary) }
    }
}

@Composable
private fun HeroCard(meta: MetaDetails) {
    ProCard(contentPadding = 14) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = meta.poster,
                contentDescription = null,
                modifier = Modifier.size(56.dp, 82.dp).clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(meta.name, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 2)
                Text(stringResource(Res.string.anime_tracking_editor_title), color = AniListColor, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SeasonPicker(seasons: List<Int>, selected: Int?, onSelected: (Int?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AssistChip(onClick = { onSelected(null) }, label = { Text(stringResource(Res.string.anime_tracking_season_all)) })
        seasons.forEach { season ->
            AssistChip(
                onClick = { onSelected(season) },
                label = { Text(stringResource(Res.string.anime_tracking_season_label, season)) },
                leadingIcon = if (selected == season) ({ Text("+") }) else null,
            )
        }
    }
}

@Composable
private fun ProviderLinkCard(
    label: String,
    brandColor: Color,
    loading: Boolean,
    linked: Boolean,
    tracked: Boolean,
    title: String,
    image: String?,
    error: Boolean,
    onOpen: () -> Unit,
    onChange: () -> Unit,
    onUntrack: () -> Unit,
    onDelete: () -> Unit,
) {
    ProCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(label, color = brandColor, fontWeight = FontWeight.Bold)
            when {
                loading -> Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = brandColor)
                }
                !linked -> {
                    Text(stringResource(Res.string.anime_tracking_not_linked), color = TextSecondary)
                    Button(onClick = onChange, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.anime_tracking_change)) }
                }
                else -> {
                    if (error) {
                        Text(stringResource(Res.string.anime_tracking_update_failed), color = MaterialTheme.colorScheme.error)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = image,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp, 72.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Text(title, Modifier.weight(1f), color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpen, Modifier.weight(1f), border = BorderStroke(1.dp, OledCardBorder)) {
                            Text(stringResource(Res.string.anime_tracking_open))
                        }
                        TextButton(onClick = onChange, Modifier.weight(1f)) { Text(stringResource(Res.string.anime_tracking_change)) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onUntrack, Modifier.weight(1f)) { Text(stringResource(Res.string.anime_tracking_untrack)) }
                        TextButton(onClick = onDelete, enabled = tracked, modifier = Modifier.weight(1f)) {
                            Text(stringResource(Res.string.action_delete), color = if (tracked) MaterialTheme.colorScheme.error else TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderTrackingEditor(
    provider: AnimeTrackingProvider,
    state: ProviderEditorState,
    onStateChange: (ProviderEditorState) -> Unit,
    automaticResolutionDisabled: Boolean,
    onUseAutomatic: () -> Unit,
) {
    if (state.loading || state.loadFailed) return
    if (state.resolvedId == null) {
        if (automaticResolutionDisabled) OutlinedButton(onClick = onUseAutomatic, Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.anime_tracking_automatic_mapping))
        }
        return
    }
    val statuses = if (provider == AnimeTrackingProvider.ANILIST) AnimeTrackingUserStatus.entries
    else AnimeTrackingUserStatus.entries.filterNot { it == AnimeTrackingUserStatus.REWATCHING }
    val brand = provider.brandColor()
    ProCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.anime_tracking_provider_section, provider.displayName),
                    Modifier.weight(1f), color = brand, fontWeight = FontWeight.Bold,
                )
                if (provider == AnimeTrackingProvider.ANILIST) {
                    IconButton(onClick = { onStateChange(state.copy(favourite = !state.favourite, dirty = true, error = false)) }) {
                        Icon(
                            if (state.favourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            stringResource(Res.string.anime_tracking_favourite),
                            tint = if (state.favourite) Color(0xFFFF5A7A) else TextSecondary,
                        )
                    }
                }
            }
            StatusPicker(state.status, statuses, brand) { status ->
                val progress = if (status == AnimeTrackingUserStatus.COMPLETED) {
                    state.entry?.totalEpisodes ?: state.progress
                } else state.progress
                onStateChange(state.copy(status = status, progress = progress, dirty = true, error = false))
            }
            NumberSlider(
                label = stringResource(Res.string.anime_tracking_progress),
                value = state.progress,
                maximum = max(state.entry?.totalEpisodes ?: 0, max(50, state.progress + 20)),
                suffix = " / ${state.entry?.totalEpisodes?.takeIf { it > 0 } ?: "?"}",
                color = brand,
            ) { onStateChange(state.copy(progress = it, dirty = true, error = false)) }
            ScoreSlider(
                state.score,
                brand,
                integerOnly = provider == AnimeTrackingProvider.MY_ANIME_LIST,
            ) { onStateChange(state.copy(score = it, dirty = true, error = false)) }
            if (state.error) Text(stringResource(Res.string.anime_tracking_update_failed), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SimklTrackingEditor(state: SimklEditorState, onStateChange: (SimklEditorState) -> Unit) {
    ProCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(Res.string.anime_tracking_provider_section, "Simkl"), color = SimklColor, fontWeight = FontWeight.Bold)
            TrackingStatusPicker(state.status, SimklColor) {
                onStateChange(state.copy(status = it, dirty = true, error = false))
            }
            NumberSlider(
                stringResource(Res.string.anime_tracking_progress),
                state.progress,
                max(state.entry?.totalEpisodes ?: 0, max(50, state.progress + 20)),
                " / ${state.entry?.totalEpisodes?.takeIf { it > 0 } ?: "?"}",
                SimklColor,
            ) { onStateChange(state.copy(progress = it, dirty = true, error = false)) }
            ScoreSlider(state.score.toDouble(), SimklColor, integerOnly = true) {
                onStateChange(state.copy(score = it.roundToInt(), dirty = true, error = false))
            }
            Text(stringResource(Res.string.anime_tracking_memo), color = TextSecondary)
            OutlinedTextField(
                value = state.memo,
                onValueChange = { if (it.length <= 140) onStateChange(state.copy(memo = it, dirty = true, error = false)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.anime_tracking_memo_hint)) },
                supportingText = { Text("${state.memo.length} / 140") },
                maxLines = 3,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(if (state.memoIsPrivate) Res.string.anime_tracking_private else Res.string.anime_tracking_public_memo),
                    Modifier.weight(1f), color = TextPrimary,
                )
                Switch(
                    checked = state.memoIsPrivate,
                    onCheckedChange = { onStateChange(state.copy(memoIsPrivate = it, dirty = true, error = false)) },
                )
            }
            if (state.error) Text(stringResource(Res.string.anime_tracking_update_failed), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SharedDatesAndNotes(
    state: SharedEditorState,
    onStateChange: (SharedEditorState) -> Unit,
    onStartDate: () -> Unit,
    onFinishDate: () -> Unit,
) {
    ProCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(Res.string.anime_tracking_dates_notes), color = TextPrimary, fontWeight = FontWeight.Bold)
            DateField(
                label = stringResource(Res.string.anime_tracking_start_date),
                date = state.startDate,
                onClick = onStartDate,
                onClear = { onStateChange(state.copy(startDate = null, startDateTouched = true, dirty = true)) },
            )
            DateField(
                label = stringResource(Res.string.anime_tracking_finish_date),
                date = state.finishDate,
                onClick = onFinishDate,
                onClear = { onStateChange(state.copy(finishDate = null, finishDateTouched = true, dirty = true)) },
            )
            CounterRow(stringResource(Res.string.anime_tracking_total_rewatches), state.repeat) {
                onStateChange(state.copy(repeat = it, dirty = true))
            }
            OutlinedTextField(
                value = state.notes,
                onValueChange = { onStateChange(state.copy(notes = it, dirty = true)) },
                label = { Text(stringResource(Res.string.anime_tracking_notes)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
            )
        }
    }
}

@Composable
private fun ProviderAdvancedEditor(
    provider: AnimeTrackingProvider,
    state: ProviderEditorState,
    onStateChange: (ProviderEditorState) -> Unit,
) {
    var expanded by remember(provider) { mutableStateOf(false) }
    ProCard(modifier = Modifier.clickable { expanded = !expanded }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.anime_tracking_advanced, provider.displayName),
                    Modifier.weight(1f), color = TextPrimary, fontWeight = FontWeight.Bold,
                )
                Icon(Icons.Default.ArrowDropDown, null, tint = TextSecondary)
            }
            if (expanded && provider == AnimeTrackingProvider.ANILIST) {
                ToggleRow(stringResource(Res.string.anime_tracking_private), state.isPrivate) {
                    onStateChange(state.copy(isPrivate = it, dirty = true, error = false))
                }
                ToggleRow(stringResource(Res.string.anime_tracking_hidden), state.hidden) {
                    onStateChange(state.copy(hidden = it, dirty = true, error = false))
                }
                AdvancedScore(stringResource(Res.string.anime_tracking_advanced_story), state.advancedScores.story) {
                    onStateChange(state.copy(advancedScores = state.advancedScores.copy(story = it), dirty = true))
                }
                AdvancedScore(stringResource(Res.string.anime_tracking_advanced_characters), state.advancedScores.characters) {
                    onStateChange(state.copy(advancedScores = state.advancedScores.copy(characters = it), dirty = true))
                }
                AdvancedScore(stringResource(Res.string.anime_tracking_advanced_visuals), state.advancedScores.visuals) {
                    onStateChange(state.copy(advancedScores = state.advancedScores.copy(visuals = it), dirty = true))
                }
                AdvancedScore(stringResource(Res.string.anime_tracking_advanced_audio), state.advancedScores.audio) {
                    onStateChange(state.copy(advancedScores = state.advancedScores.copy(audio = it), dirty = true))
                }
                AdvancedScore(stringResource(Res.string.anime_tracking_advanced_enjoyment), state.advancedScores.enjoyment) {
                    onStateChange(state.copy(advancedScores = state.advancedScores.copy(enjoyment = it), dirty = true))
                }
            }
            if (expanded && provider == AnimeTrackingProvider.MY_ANIME_LIST) {
                ChoiceChips(
                    stringResource(Res.string.anime_tracking_mal_priority),
                    listOf(
                        stringResource(Res.string.anime_tracking_option_low),
                        stringResource(Res.string.anime_tracking_option_medium),
                        stringResource(Res.string.anime_tracking_option_high),
                    ),
                    state.priority,
                ) { onStateChange(state.copy(priority = it, dirty = true, error = false)) }
                ChoiceChips(
                    stringResource(Res.string.anime_tracking_mal_rewatch_value),
                    listOf(
                        stringResource(Res.string.anime_tracking_option_none),
                        stringResource(Res.string.anime_tracking_option_very_low),
                        stringResource(Res.string.anime_tracking_option_low),
                        stringResource(Res.string.anime_tracking_option_medium),
                        stringResource(Res.string.anime_tracking_option_high),
                        stringResource(Res.string.anime_tracking_option_very_high),
                    ),
                    state.rewatchValue,
                ) { onStateChange(state.copy(rewatchValue = it, dirty = true, error = false)) }
            }
        }
    }
}

@Composable
private fun StatusPicker(
    value: AnimeTrackingUserStatus,
    options: List<AnimeTrackingUserStatus>,
    color: Color,
    onSelected: (AnimeTrackingUserStatus) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Text(stringResource(Res.string.anime_tracking_status), color = TextSecondary)
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, OledCardBorder)) {
            Text(value.label(), Modifier.weight(1f), color = color)
            Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(expanded, { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option.label()) }, onClick = { onSelected(option); expanded = false })
            }
        }
    }
}

@Composable
private fun TrackingStatusPicker(value: TrackingListStatus, color: Color, onSelected: (TrackingListStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Text(stringResource(Res.string.anime_tracking_status), color = TextSecondary)
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, OledCardBorder)) {
            Text(value.label(), Modifier.weight(1f), color = color)
            Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(expanded, { expanded = false }) {
            TrackingListStatus.entries.forEach { option ->
                DropdownMenuItem(text = { Text(option.label()) }, onClick = { onSelected(option); expanded = false })
            }
        }
    }
}

@Composable
private fun NumberSlider(label: String, value: Int, maximum: Int, suffix: String, color: Color, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = TextSecondary)
        Text("$value$suffix", color = color, fontWeight = FontWeight.Bold)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange((value - 1).coerceAtLeast(0)) }) { Icon(Icons.Rounded.Remove, null, tint = TextPrimary) }
        Slider(
            value = value.coerceIn(0, maximum).toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 0f..maximum.coerceAtLeast(1).toFloat(),
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color),
        )
        IconButton(onClick = { onChange((value + 1).coerceAtMost(maximum)) }) { Icon(Icons.Rounded.Add, null, tint = TextPrimary) }
    }
}

@Composable
private fun ScoreSlider(value: Double, color: Color, integerOnly: Boolean = false, onChange: (Double) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(Res.string.anime_tracking_score), Modifier.weight(1f), color = TextSecondary)
        Text(
            if (value <= 0.0) stringResource(Res.string.anime_tracking_unrated)
            else if (integerOnly) "${value.roundToInt()} / 10" else "${(value * 10).roundToInt() / 10.0} / 10",
            color = color, fontWeight = FontWeight.Bold,
        )
    }
    Slider(
        value = value.toFloat().coerceIn(0f, 10f),
        onValueChange = { onChange(if (integerOnly) it.roundToInt().toDouble() else it.toDouble()) },
        valueRange = 0f..10f,
        steps = if (integerOnly) 9 else 99,
        colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color),
    )
}

@Composable
private fun AdvancedScore(label: String, value: Double, onChange: (Double) -> Unit) {
    Text("$label: ${if (value == 0.0) stringResource(Res.string.anime_tracking_unrated) else (value * 10).roundToInt() / 10.0}", color = TextSecondary)
    Slider(value.toFloat(), { onChange(it.toDouble()) }, valueRange = 0f..10f, steps = 99)
}

@Composable
private fun DateField(label: String, date: AnimeTrackingDate?, onClick: () -> Unit, onClear: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onClick, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, OledCardBorder)) {
            Icon(Icons.Default.DateRange, null, tint = AniListColor)
            Spacer(Modifier.width(8.dp))
            Text(date?.displayValue() ?: label, maxLines = 1)
        }
        if (date != null) TextButton(onClick = onClear) { Text(stringResource(Res.string.anime_tracking_clear)) }
    }
}

@Composable
private fun CounterRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = TextSecondary)
        IconButton(onClick = { onChange((value - 1).coerceAtLeast(0)) }) { Icon(Icons.Rounded.Remove, null, tint = TextPrimary) }
        Text(value.toString(), color = TextPrimary, fontWeight = FontWeight.Bold)
        IconButton(onClick = { onChange(value + 1) }) { Icon(Icons.Rounded.Add, null, tint = TextPrimary) }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = TextPrimary)
        Switch(checked, onCheckedChange)
    }
}

@Composable
private fun ChoiceChips(label: String, choices: List<String>, selected: Int, onSelected: (Int) -> Unit) {
    Text(label, color = TextSecondary)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEachIndexed { index, choice ->
            FilterChip(selected = selected == index, onClick = { onSelected(index) }, label = { Text(choice) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackingDatePickerDialog(
    initialDate: AnimeTrackingDate?,
    onDismiss: () -> Unit,
    onSelected: (AnimeTrackingDate) -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialDate?.epochMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
                    onSelected(AnimeTrackingDate(date.year, date.month.ordinal + 1, date.day))
                }
            }) { Text(stringResource(Res.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    ) { DatePicker(state) }
}

private data class SearchChoice(val id: Long, val title: String, val image: String?, val details: String)

@Composable
private fun TrackingSearch(
    provider: EditorProvider,
    initialQuery: String,
    modifier: Modifier,
    onBack: () -> Unit,
    onSelected: (SearchChoice) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var query by remember(provider) { mutableStateOf(initialQuery) }
    var results by remember(provider) { mutableStateOf<List<SearchChoice>>(emptyList()) }
    var loading by remember(provider) { mutableStateOf(false) }
    var requestKey by remember(provider) { mutableIntStateOf(0) }

    fun search() {
        if (query.isBlank()) return
        val request = ++requestKey
        loading = true
        scope.launch {
            val found = runCatching {
                when (provider) {
                    EditorProvider.ANILIST, EditorProvider.MAL -> provider.toAnimeProvider().repository().searchAnime(query).map {
                        SearchChoice(it.id.toLong(), it.title, it.imageUrl, listOfNotNull(it.year, it.format, it.episodes?.let { count -> "$count ep." }).joinToString(" - "))
                    }
                    EditorProvider.SIMKL -> SimklEditorRepository.searchAnime(query).map(SimklSearchResult::toSearchChoice)
                }
            }.getOrDefault(emptyList())
            if (request == requestKey) { results = found; loading = false }
        }
    }
    LaunchedEffect(provider) { search() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(Res.string.anime_tracking_search_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { search() }),
            trailingIcon = {
                TextButton(onClick = { search() }, enabled = !loading) { Text(stringResource(Res.string.action_ok)) }
            },
        )
        if (loading) Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results, key = { it.id }) { result ->
                ProCard(modifier = Modifier.clickable { onSelected(result) }, contentPadding = 10) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(result.image, null, Modifier.size(52.dp, 74.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        Column(Modifier.weight(1f)) {
                            Text(result.title, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 2)
                            Text(result.details, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.action_back)) }
    }
}

@Composable
private fun ProCard(modifier: Modifier = Modifier, contentPadding: Int = 16, content: @Composable () -> Unit) {
    Box(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(OledCardBg)
            .border(1.dp, OledCardBorder, RoundedCornerShape(16.dp)).padding(contentPadding.dp),
    ) { content() }
}

private fun buildEditorMedia(meta: MetaDetails, season: Int?, episode: Int?): TrackingMediaReference =
    buildTrackingMediaReference(
        contentType = meta.type,
        parentMetaId = meta.id,
        title = meta.name,
        releaseInfo = meta.releaseInfo,
        seasonNumber = season,
        episodeNumber = episode ?: 1.takeIf { season != null },
    ).let { reference ->
        if (season != null && reference.episode == null) reference.copy(episode = TrackingEpisode(season, 1)) else reference
    }.copy(posterUrl = meta.poster)

private fun TrackingMediaReference.withSimklId(id: Long, poster: String? = posterUrl): TrackingMediaReference =
    copy(ids = ids.copy(simkl = id), posterUrl = poster ?: posterUrl)

private fun SimklSearchResult.toSearchChoice() = SearchChoice(
    id = id,
    title = title,
    image = simklPosterUrl(poster),
    details = listOfNotNull(year?.toString(), format, totalEpisodes?.let { "$it ep." }).joinToString(" - "),
)

private fun AnimeTrackingProvider.repository(): AnimeTrackingRepository = when (this) {
    AnimeTrackingProvider.ANILIST -> AniListTrackingRepository
    AnimeTrackingProvider.MY_ANIME_LIST -> MyAnimeListTrackingRepository
}

private fun AnimeTrackingProvider.brandColor() = when (this) {
    AnimeTrackingProvider.ANILIST -> AniListColor
    AnimeTrackingProvider.MY_ANIME_LIST -> MyAnimeListColor
}

private fun AnimeTrackingProvider.toEditorProvider() = when (this) {
    AnimeTrackingProvider.ANILIST -> EditorProvider.ANILIST
    AnimeTrackingProvider.MY_ANIME_LIST -> EditorProvider.MAL
}

private fun EditorProvider.toAnimeProvider() = when (this) {
    EditorProvider.ANILIST -> AnimeTrackingProvider.ANILIST
    EditorProvider.MAL -> AnimeTrackingProvider.MY_ANIME_LIST
    EditorProvider.SIMKL -> error("Simkl is not an AnimeTrackingProvider")
}

private fun AnimeTrackingDate.displayValue(): String = listOfNotNull(
    year?.toString()?.padStart(4, '0'), month?.toString()?.padStart(2, '0'), day?.toString()?.padStart(2, '0'),
).joinToString("-")

private fun AnimeTrackingDate.epochMillis(): Long? = runCatching {
    LocalDate(requireNotNull(year), requireNotNull(month), requireNotNull(day)).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
}.getOrNull()

@Composable
private fun AnimeTrackingUserStatus.label(): String = when (this) {
    AnimeTrackingUserStatus.WATCHING -> stringResource(Res.string.anime_tracking_status_watching)
    AnimeTrackingUserStatus.COMPLETED -> stringResource(Res.string.anime_tracking_status_completed)
    AnimeTrackingUserStatus.ON_HOLD -> stringResource(Res.string.anime_tracking_status_on_hold)
    AnimeTrackingUserStatus.DROPPED -> stringResource(Res.string.anime_tracking_status_dropped)
    AnimeTrackingUserStatus.PLAN_TO_WATCH -> stringResource(Res.string.anime_tracking_status_plan)
    AnimeTrackingUserStatus.REWATCHING -> stringResource(Res.string.anime_tracking_status_rewatching)
}

@Composable
private fun TrackingListStatus.label(): String = when (this) {
    TrackingListStatus.WATCHING -> stringResource(Res.string.anime_tracking_status_watching)
    TrackingListStatus.PLAN_TO_WATCH -> stringResource(Res.string.anime_tracking_status_plan)
    TrackingListStatus.ON_HOLD -> stringResource(Res.string.anime_tracking_status_on_hold)
    TrackingListStatus.COMPLETED -> stringResource(Res.string.anime_tracking_status_completed)
    TrackingListStatus.DROPPED -> stringResource(Res.string.anime_tracking_status_dropped)
}
