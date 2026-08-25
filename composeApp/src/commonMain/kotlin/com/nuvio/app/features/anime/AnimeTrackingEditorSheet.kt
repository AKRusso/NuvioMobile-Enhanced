package com.nuvio.app.features.anime

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.tracking.TrackingEpisode
import com.nuvio.app.features.tracking.buildTrackingMediaReference
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_back
import nuvio.composeapp.generated.resources.action_save
import nuvio.composeapp.generated.resources.anime_tracking_automatic_mapping
import nuvio.composeapp.generated.resources.anime_tracking_change_mapping
import nuvio.composeapp.generated.resources.anime_tracking_editor_title
import nuvio.composeapp.generated.resources.anime_tracking_link_title
import nuvio.composeapp.generated.resources.anime_tracking_no_accounts
import nuvio.composeapp.generated.resources.anime_tracking_progress
import nuvio.composeapp.generated.resources.anime_tracking_remove_mapping
import nuvio.composeapp.generated.resources.anime_tracking_search_hint
import nuvio.composeapp.generated.resources.anime_tracking_season_all
import nuvio.composeapp.generated.resources.anime_tracking_season_label
import nuvio.composeapp.generated.resources.anime_tracking_update_failed
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeTrackingEditorSheet(
    meta: MetaDetails,
    onDismiss: () -> Unit,
    initialSeason: Int? = null,
    initialEpisode: Int? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val aniListState by AniListTrackingRepository.uiState.collectAsStateWithLifecycle()
    val malState by MyAnimeListTrackingRepository.uiState.collectAsStateWithLifecycle()
    val settings by AnimeTrackingSettingsRepository.state.collectAsStateWithLifecycle()
    val seasons = remember(meta.videos) { meta.videos.mapNotNull { it.season }.distinct().sorted() }
    var selectedSeason by remember(meta.id, initialSeason) { mutableStateOf(initialSeason) }
    var searchingProvider by remember(meta.id) { mutableStateOf<AnimeTrackingProvider?>(null) }

    NuvioModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = if (searchingProvider == null) {
                    stringResource(Res.string.anime_tracking_editor_title)
                } else {
                    stringResource(Res.string.anime_tracking_link_title, searchingProvider!!.displayName)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = meta.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (searchingProvider != null) {
                AnimeTrackingSearch(
                    provider = searchingProvider!!,
                    initialQuery = meta.name,
                    onBack = { searchingProvider = null },
                    onSelected = { result ->
                        AnimeTrackingSettingsRepository.setMapping(
                            provider = searchingProvider!!,
                            contentType = meta.type,
                            contentId = meta.id,
                            season = selectedSeason,
                            episode = initialEpisode.takeIf { selectedSeason == initialSeason },
                            providerMediaId = result.id,
                        )
                        searchingProvider = null
                    },
                )
            } else {
                if (seasons.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AssistChip(
                            onClick = { selectedSeason = null },
                            label = { Text(stringResource(Res.string.anime_tracking_season_all)) },
                            leadingIcon = if (selectedSeason == null) ({ Text("✓") }) else null,
                        )
                        seasons.forEach { season ->
                            AssistChip(
                                onClick = { selectedSeason = season },
                                label = { Text(stringResource(Res.string.anime_tracking_season_label, season)) },
                                leadingIcon = if (selectedSeason == season) ({ Text("✓") }) else null,
                            )
                        }
                    }
                }

                val connected = listOfNotNull(
                    AnimeTrackingProvider.ANILIST.takeIf { aniListState.connected },
                    AnimeTrackingProvider.MY_ANIME_LIST.takeIf { malState.connected },
                )
                if (connected.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.anime_tracking_no_accounts),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                connected.forEach { provider ->
                    AnimeTrackingProviderEditor(
                        provider = provider,
                        meta = meta,
                        season = selectedSeason,
                        episode = initialEpisode.takeIf { selectedSeason == initialSeason },
                        settings = settings,
                        onSearch = { searchingProvider = provider },
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimeTrackingProviderEditor(
    provider: AnimeTrackingProvider,
    meta: MetaDetails,
    season: Int?,
    episode: Int?,
    settings: AnimeTrackingSettings,
    onSearch: () -> Unit,
) {
    val repository = provider.repository()
    val media = remember(meta.id, season, episode) {
        buildTrackingMediaReference(
            contentType = meta.type,
            parentMetaId = meta.id,
            title = meta.name,
            releaseInfo = meta.releaseInfo,
            seasonNumber = season,
            episodeNumber = episode ?: 1.takeIf { season != null },
        ).let { reference ->
            if (season != null && reference.episode == null) {
                reference.copy(episode = TrackingEpisode(season = season, number = 1))
            } else {
                reference
            }
        }
    }
    val scope = rememberCoroutineScope()
    var resolvedId by remember(media, settings) { mutableStateOf<Int?>(null) }
    var progress by remember(media, settings) { mutableStateOf(0) }
    var loading by remember(media, settings) { mutableStateOf(true) }
    var saving by remember(media, settings) { mutableStateOf(false) }
    var error by remember(media, settings) { mutableStateOf<String?>(null) }

    LaunchedEffect(media, settings) {
        loading = true
        error = null
        resolvedId = repository.resolvedMediaId(media)
        progress = if (resolvedId != null) repository.loadProgress(media) ?: 0 else 0
        loading = false
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(provider.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (loading) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            }
            if (!loading && resolvedId == null) {
                Button(onClick = onSearch, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.anime_tracking_change_mapping))
                }
            } else if (!loading) {
                Text(
                    text = "ID ${resolvedId.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stringResource(Res.string.anime_tracking_progress), style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { progress = (progress - 1).coerceAtLeast(0) }) {
                        Icon(Icons.Rounded.Remove, contentDescription = null)
                    }
                    OutlinedTextField(
                        value = progress.toString(),
                        onValueChange = { value -> value.toIntOrNull()?.let { progress = it.coerceAtLeast(0) } },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {}),
                    )
                    IconButton(onClick = { progress += 1 }) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                    }
                }
                Button(
                    onClick = {
                        saving = true
                        error = null
                        scope.launch {
                            if (!repository.saveProgress(media, progress)) {
                                error = "update_failed"
                            }
                            saving = false
                        }
                    },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (saving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(Res.string.action_save))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onSearch) {
                        Text(stringResource(Res.string.anime_tracking_change_mapping))
                    }
                    TextButton(
                        onClick = {
                            AnimeTrackingSettingsRepository.setMapping(
                                provider = provider,
                                contentType = meta.type,
                                contentId = meta.id,
                                season = season,
                                episode = episode,
                                providerMediaId = null,
                                disableAutomaticResolution = true,
                            )
                        },
                    ) {
                        Text(stringResource(Res.string.anime_tracking_remove_mapping))
                    }
                }
            }
            error?.let {
                Text(
                    text = stringResource(Res.string.anime_tracking_update_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            val mapping = AnimeTrackingSettingsRepository.mapping(provider, meta.type, meta.id, season, episode)
            if (mapping?.automaticResolutionDisabled(provider) == true) {
                OutlinedButton(
                    onClick = {
                        AnimeTrackingSettingsRepository.setMapping(
                            provider = provider,
                            contentType = meta.type,
                            contentId = meta.id,
                            season = season,
                            episode = episode,
                            providerMediaId = null,
                            disableAutomaticResolution = false,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.anime_tracking_automatic_mapping))
                }
            }
        }
    }
}

@Composable
private fun AnimeTrackingSearch(
    provider: AnimeTrackingProvider,
    initialQuery: String,
    onBack: () -> Unit,
    onSelected: (AnimeTrackingSearchResult) -> Unit,
) {
    val repository = provider.repository()
    val scope = rememberCoroutineScope()
    var query by remember(provider, initialQuery) { mutableStateOf(initialQuery) }
    var results by remember(provider) { mutableStateOf<List<AnimeTrackingSearchResult>>(emptyList()) }
    var loading by remember(provider) { mutableStateOf(false) }

    fun search() {
        if (query.isBlank() || loading) return
        loading = true
        scope.launch {
            results = repository.searchAnime(query)
            loading = false
        }
    }

    LaunchedEffect(provider) { search() }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(Res.string.anime_tracking_search_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { search() }),
        trailingIcon = {
            TextButton(onClick = { search() }, enabled = !loading) { Text("OK") }
        },
    )
    if (loading) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            CircularProgressIndicator()
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(results, key = { result -> result.id }) { result ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelected(result) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = result.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(width = 52.dp, height = 74.dp),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(result.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                    Text(
                        listOfNotNull(result.year?.toString(), result.format, result.episodes?.let { "$it ep." })
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.action_back))
    }
}

private fun AnimeTrackingProvider.repository(): AnimeTrackingRepository = when (this) {
    AnimeTrackingProvider.ANILIST -> AniListTrackingRepository
    AnimeTrackingProvider.MY_ANIME_LIST -> MyAnimeListTrackingRepository
}

private fun Int?.orEmpty(): String = this?.toString().orEmpty()
