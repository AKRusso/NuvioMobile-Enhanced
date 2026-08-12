package com.nuvio.app.features.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.nuvio
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.addon_title
import nuvio.composeapp.generated.resources.action_play
import nuvio.composeapp.generated.resources.compose_player_built_in
import nuvio.composeapp.generated.resources.compose_action_pause
import nuvio.composeapp.generated.resources.compose_player_auto_sync
import nuvio.composeapp.generated.resources.compose_player_capture_line
import nuvio.composeapp.generated.resources.compose_player_fetch_subtitles
import nuvio.composeapp.generated.resources.compose_player_languages
import nuvio.composeapp.generated.resources.compose_player_loading_lines
import nuvio.composeapp.generated.resources.compose_player_none
import nuvio.composeapp.generated.resources.compose_player_no_subtitle_lines_found
import nuvio.composeapp.generated.resources.compose_player_reload
import nuvio.composeapp.generated.resources.compose_player_reset
import nuvio.composeapp.generated.resources.compose_player_select_addon_subtitle_first
import nuvio.composeapp.generated.resources.compose_player_style
import nuvio.composeapp.generated.resources.compose_player_subtitle_delay
import nuvio.composeapp.generated.resources.compose_player_subtitles
import nuvio.composeapp.generated.resources.compose_player_sync_short
import nuvio.composeapp.generated.resources.settings_playback_option_forced
import nuvio.composeapp.generated.resources.subtitle_language_unknown
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NuvioSubtitleModal(
    visible: Boolean,
    activeTab: SubtitleTab,
    subtitleTracks: List<SubtitleTrack>,
    selectedSubtitleIndex: Int,
    addonSubtitles: List<AddonSubtitle>,
    selectedAddonSubtitleId: String?,
    isLoadingAddonSubtitles: Boolean,
    preferredSubtitleLanguage: String,
    secondaryPreferredSubtitleLanguage: String?,
    subtitleStyle: SubtitleStyleState,
    subtitleDelayMs: Int,
    selectedAddonSubtitle: AddonSubtitle?,
    subtitleAutoSyncState: SubtitleAutoSyncUiState,
    onBuiltInTrackSelected: (Int) -> Unit,
    onAddonSubtitleSelected: (AddonSubtitle) -> Unit,
    onFetchAddonSubtitles: () -> Unit,
    onStyleChanged: (SubtitleStyleState) -> Unit,
    onSubtitleDelayChanged: (Int) -> Unit,
    onSubtitleDelayReset: () -> Unit,
    onAutoSyncCapture: () -> Unit,
    onAutoSyncCueSelected: (SubtitleSyncCue) -> Unit,
    onAutoSyncReload: () -> Unit,
    onTogglePlayback: () -> Unit,
    currentPlaybackPositionMs: Long,
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val effectiveSelectedAddonSubtitle = selectedAddonSubtitle ?: addonSubtitles.firstOrNull { subtitle ->
        subtitle.id == selectedAddonSubtitleId || subtitle.url == selectedAddonSubtitleId
    }
    val playbackLanguageKey = selectedSubtitleLanguageKey(
        subtitleTracks = subtitleTracks,
        selectedSubtitleIndex = selectedSubtitleIndex,
        selectedAddonSubtitle = effectiveSelectedAddonSubtitle,
    )
    val playbackOptionId = selectedSubtitleOptionId(
        subtitleTracks = subtitleTracks,
        selectedSubtitleIndex = selectedSubtitleIndex,
        selectedAddonSubtitle = effectiveSelectedAddonSubtitle,
    )
    val languageItems = remember(
        subtitleTracks,
        addonSubtitles,
        preferredSubtitleLanguage,
        secondaryPreferredSubtitleLanguage,
        subtitleStyle.showOnlyPreferredLanguages,
        playbackLanguageKey,
    ) {
        buildSubtitleLanguageItems(
            subtitleTracks = subtitleTracks,
            addonSubtitles = addonSubtitles,
            preferredLanguage = preferredSubtitleLanguage,
            secondaryPreferredLanguage = secondaryPreferredSubtitleLanguage,
            showOnlyPreferredLanguages = subtitleStyle.showOnlyPreferredLanguages,
            selectedLanguageKey = playbackLanguageKey,
        )
    }
    var activeLanguageKey by remember(visible) {
        mutableStateOf(
            playbackLanguageKey.takeIf { key -> languageItems.any { it.key == key } }
                ?: languageItems.firstOrNull { it.key != SubtitleOffLanguageKey }?.key
                ?: SubtitleOffLanguageKey,
        )
    }
    var pendingOptionId by remember(visible) { mutableStateOf<String?>(playbackOptionId) }
    val options = remember(activeLanguageKey, subtitleTracks, addonSubtitles) {
        buildSubtitleSelectionOptions(activeLanguageKey, subtitleTracks, addonSubtitles)
    }
    val selectedOptionId = pendingOptionId ?: playbackOptionId
    val styleVisible = activeLanguageKey != SubtitleOffLanguageKey &&
        selectedOptionId != null && options.any { it.id == selectedOptionId }

    LaunchedEffect(languageItems) {
        if (languageItems.none { it.key == activeLanguageKey }) {
            activeLanguageKey = playbackLanguageKey.takeIf { key -> languageItems.any { it.key == key } }
                ?: languageItems.firstOrNull { it.key != SubtitleOffLanguageKey }?.key
                ?: SubtitleOffLanguageKey
        }
    }

    LaunchedEffect(playbackLanguageKey, playbackOptionId) {
        if (playbackOptionId != null || playbackLanguageKey == SubtitleOffLanguageKey) {
            activeLanguageKey = playbackLanguageKey
            pendingOptionId = playbackOptionId
        }
    }

    PlayerOverlayScaffold(
        visible = visible,
        onDismiss = onDismiss,
        modifier = modifier,
        contentPadding = if (activeTab == SubtitleTab.Sync) {
            PaddingValues(start = 28.dp, end = 28.dp, top = 18.dp, bottom = 18.dp)
        } else {
            PaddingValues(start = 52.dp, end = 52.dp, top = 36.dp, bottom = 76.dp)
        },
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val availableWidth = maxWidth
            val railMaxHeight = (maxHeight - 72.dp).coerceAtLeast(120.dp)

            if (activeTab == SubtitleTab.Sync) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.compose_player_sync_short),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    NuvioSubtitleSyncContent(
                        availableWidth = availableWidth,
                        subtitleDelayMs = subtitleDelayMs,
                        selectedAddonSubtitle = effectiveSelectedAddonSubtitle,
                        subtitleAutoSyncState = subtitleAutoSyncState,
                        currentPlaybackPositionMs = currentPlaybackPositionMs,
                        isCompact = railMaxHeight < 420.dp,
                        isPlaying = isPlaying,
                        onSubtitleDelayChanged = onSubtitleDelayChanged,
                        onSubtitleDelayReset = onSubtitleDelayReset,
                        onAutoSyncCapture = onAutoSyncCapture,
                        onAutoSyncCueSelected = onAutoSyncCueSelected,
                        onAutoSyncReload = onAutoSyncReload,
                        onTogglePlayback = onTogglePlayback,
                        railMaxHeight = railMaxHeight,
                    )
                }
            } else {
                Column(
                    modifier = Modifier.align(Alignment.BottomStart),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                Text(
                    text = stringResource(Res.string.compose_player_subtitles),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    SubtitleRail(
                        title = stringResource(Res.string.compose_player_languages),
                        width = 200.dp,
                    ) {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = railMaxHeight),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            items(languageItems, key = { it.key }) { item ->
                                SubtitleLanguageRow(
                                    item = item,
                                    selected = item.key == activeLanguageKey,
                                    onClick = {
                                        activeLanguageKey = item.key
                                        val availableOptions = buildSubtitleSelectionOptions(
                                            item.key,
                                            subtitleTracks,
                                            addonSubtitles,
                                        )
                                        pendingOptionId = playbackOptionId?.takeIf { id ->
                                            availableOptions.any { it.id == id }
                                        }
                                        if (item.key == SubtitleOffLanguageKey) {
                                            onBuiltInTrackSelected(-1)
                                        }
                                    },
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = activeLanguageKey != SubtitleOffLanguageKey,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        SubtitleRail(
                            title = stringResource(Res.string.compose_player_subtitles),
                            width = 300.dp,
                        ) {
                            when {
                                options.isEmpty() && isLoadingAddonSubtitles -> {
                                    PlayerModalLoading(modifier = Modifier.padding(vertical = 24.dp))
                                }

                                options.isEmpty() -> {
                                    SubtitleRailEmptyState(
                                        text = stringResource(Res.string.compose_player_fetch_subtitles),
                                        onClick = onFetchAddonSubtitles,
                                    )
                                }

                                else -> {
                                    LazyColumn(
                                        modifier = Modifier.heightIn(max = railMaxHeight),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp),
                                    ) {
                                        items(options, key = { it.id }) { option ->
                                            SubtitleOptionRow(
                                                option = option,
                                                selected = option.id == selectedOptionId,
                                                onClick = {
                                                    pendingOptionId = option.id
                                                    when (option) {
                                                        is SubtitleSelectionOption.BuiltIn -> {
                                                            onBuiltInTrackSelected(option.track.index)
                                                        }

                                                        is SubtitleSelectionOption.Addon -> {
                                                            onAddonSubtitleSelected(option.subtitle)
                                                        }
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = styleVisible,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        SubtitleRail(
                            title = stringResource(Res.string.compose_player_style),
                            width = 280.dp,
                        ) {
                            Column(
                                modifier = Modifier
                                    .heightIn(max = railMaxHeight)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                SubtitleStylePanel(
                                    style = subtitleStyle,
                                    isCompact = railMaxHeight < 420.dp,
                                    onStyleChanged = onStyleChanged,
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

@Composable
private fun NuvioSubtitleSyncContent(
    availableWidth: Dp,
    selectedAddonSubtitle: AddonSubtitle?,
    subtitleAutoSyncState: SubtitleAutoSyncUiState,
    currentPlaybackPositionMs: Long,
    subtitleDelayMs: Int,
    isCompact: Boolean,
    isPlaying: Boolean,
    onSubtitleDelayChanged: (Int) -> Unit,
    onSubtitleDelayReset: () -> Unit,
    onAutoSyncCapture: () -> Unit,
    onAutoSyncCueSelected: (SubtitleSyncCue) -> Unit,
    onAutoSyncReload: () -> Unit,
    onTogglePlayback: () -> Unit,
    railMaxHeight: Dp,
) {
    val tokens = MaterialTheme.nuvio
    val sortedCues = remember(subtitleAutoSyncState.cues) {
        subtitleAutoSyncState.cues.sortedBy(SubtitleSyncCue::startTimeMs)
    }
    val subtitlePositionMs = (currentPlaybackPositionMs - subtitleDelayMs).coerceAtLeast(0L)
    // Keep the last spoken line highlighted during the gap before the next cue.
    val activeCue = activeSubtitleSyncCue(sortedCues, subtitlePositionMs)
        ?: sortedCues.lastOrNull { it.startTimeMs <= subtitlePositionMs }
    val activeCueIndex = sortedCues.indexOf(activeCue)
    val cueListState = rememberLazyListState()
    var followActiveCue by remember { mutableStateOf(true) }
    var autoScrollInProgress by remember { mutableStateOf(false) }

    LaunchedEffect(cueListState) {
        snapshotFlow { cueListState.isScrollInProgress }.collect { isScrolling ->
            if (isScrolling && !autoScrollInProgress) {
                followActiveCue = false
            } else if (!isScrolling && !autoScrollInProgress && !followActiveCue) {
                delay(900)
                val activeItem = cueListState.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == activeCueIndex }
                val viewportCenter = (
                    cueListState.layoutInfo.viewportStartOffset +
                        cueListState.layoutInfo.viewportEndOffset
                    ) / 2
                val itemCenter = activeItem?.let { it.offset + it.size / 2 }
                if (itemCenter != null && kotlin.math.abs(itemCenter - viewportCenter) <= 96) {
                    followActiveCue = true
                }
            }
        }
    }

    LaunchedEffect(sortedCues) {
        followActiveCue = true
    }

    LaunchedEffect(activeCueIndex, sortedCues.size, followActiveCue) {
        if (!followActiveCue || activeCueIndex < 0) return@LaunchedEffect
        autoScrollInProgress = true
        try {
            cueListState.animateSubtitleCueToCenter(activeCueIndex)
        } finally {
            autoScrollInProgress = false
        }
    }

    val useSideBySideLayout = availableWidth >= 720.dp
    val contentModifier = Modifier.fillMaxWidth()
    val contentSpacing = if (useSideBySideLayout) 16.dp else 12.dp

    if (useSideBySideLayout) {
        Row(
            modifier = contentModifier,
            horizontalArrangement = Arrangement.spacedBy(contentSpacing),
            verticalAlignment = Alignment.Top,
        ) {
            SubtitleSyncCueList(
                modifier = Modifier.weight(1f),
                isCompact = isCompact,
                sortedCues = sortedCues,
                activeCueIndex = activeCueIndex,
                cueListState = cueListState,
                selectedAddonSubtitle = selectedAddonSubtitle,
                subtitleAutoSyncState = subtitleAutoSyncState,
                onAutoSyncCueSelected = onAutoSyncCueSelected,
                onManualScrollStarted = { followActiveCue = false },
                tokens = tokens,
                railMaxHeight = railMaxHeight,
            )
            SubtitleSyncControls(
                modifier = Modifier.width(if (isCompact) 320.dp else 360.dp),
                isCompact = isCompact,
                selectedAddonSubtitle = selectedAddonSubtitle,
                subtitleAutoSyncState = subtitleAutoSyncState,
                subtitleDelayMs = subtitleDelayMs,
                isPlaying = isPlaying,
                sortedCues = sortedCues,
                onSubtitleDelayChanged = onSubtitleDelayChanged,
                onSubtitleDelayReset = onSubtitleDelayReset,
                onAutoSyncCapture = onAutoSyncCapture,
                onAutoSyncReload = onAutoSyncReload,
                onTogglePlayback = onTogglePlayback,
                tokens = tokens,
            )
        }
    } else {
        Column(
            modifier = contentModifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
        ) {
            SubtitleSyncCueList(
                modifier = Modifier.fillMaxWidth(),
                isCompact = isCompact,
                sortedCues = sortedCues,
                activeCueIndex = activeCueIndex,
                cueListState = cueListState,
                selectedAddonSubtitle = selectedAddonSubtitle,
                subtitleAutoSyncState = subtitleAutoSyncState,
                onAutoSyncCueSelected = onAutoSyncCueSelected,
                onManualScrollStarted = { followActiveCue = false },
                tokens = tokens,
                railMaxHeight = (railMaxHeight * 0.52f).coerceAtLeast(180.dp),
            )
            SubtitleSyncControls(
                modifier = Modifier.fillMaxWidth(),
                isCompact = isCompact,
                selectedAddonSubtitle = selectedAddonSubtitle,
                subtitleAutoSyncState = subtitleAutoSyncState,
                subtitleDelayMs = subtitleDelayMs,
                isPlaying = isPlaying,
                sortedCues = sortedCues,
                onSubtitleDelayChanged = onSubtitleDelayChanged,
                onSubtitleDelayReset = onSubtitleDelayReset,
                onAutoSyncCapture = onAutoSyncCapture,
                onAutoSyncReload = onAutoSyncReload,
                onTogglePlayback = onTogglePlayback,
                tokens = tokens,
            )
        }
    }
}

@Composable
private fun SubtitleSyncCueList(
    modifier: Modifier,
    isCompact: Boolean,
    sortedCues: List<SubtitleSyncCue>,
    activeCueIndex: Int,
    cueListState: androidx.compose.foundation.lazy.LazyListState,
    selectedAddonSubtitle: AddonSubtitle?,
    subtitleAutoSyncState: SubtitleAutoSyncUiState,
    onAutoSyncCueSelected: (SubtitleSyncCue) -> Unit,
    onManualScrollStarted: () -> Unit,
    tokens: com.nuvio.app.core.ui.NuvioThemeTokens,
    railMaxHeight: Dp,
) {
    Column(modifier = modifier) {
            Text(
                text = stringResource(Res.string.compose_player_subtitles),
                color = tokens.colors.textMuted,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            when {
                    selectedAddonSubtitle == null -> {
                        Text(
                            text = stringResource(Res.string.compose_player_select_addon_subtitle_first),
                            color = tokens.colors.textMuted,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 220.dp, max = railMaxHeight)
                                .padding(24.dp),
                        )
                    }

                    subtitleAutoSyncState.isLoading && sortedCues.isEmpty() -> {
                        Text(
                            text = stringResource(Res.string.compose_player_loading_lines),
                            color = tokens.colors.textMuted,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 220.dp, max = railMaxHeight)
                                .padding(24.dp),
                        )
                    }

                    sortedCues.isEmpty() -> {
                        Text(
                            text = subtitleAutoSyncState.errorMessage
                                ?: stringResource(Res.string.compose_player_no_subtitle_lines_found),
                            color = if (subtitleAutoSyncState.errorMessage != null) {
                                tokens.colors.danger
                            } else {
                                tokens.colors.textMuted
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 220.dp, max = railMaxHeight)
                                .padding(24.dp),
                        )
                    }

                    else -> {
                        LazyColumn(
                            state = cueListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 260.dp, max = railMaxHeight)
                                .subtitleSyncManualScroll(onManualScrollStarted),
                            contentPadding = PaddingValues(vertical = railMaxHeight / 2),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            itemsIndexed(sortedCues) { index, cue ->
                                val isActive = index == activeCueIndex
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isActive) tokens.colors.accent.copy(alpha = 0.22f)
                                            else Color.Transparent,
                                        )
                                        .border(
                                            1.dp,
                                            if (isActive) tokens.colors.accent.copy(alpha = 0.72f)
                                            else Color.Transparent,
                                            RoundedCornerShape(14.dp),
                                        )
                                        .clickable { onAutoSyncCueSelected(cue) }
                                         .padding(
                                             horizontal = if (isCompact) 10.dp else 14.dp,
                                             vertical = if (isActive) {
                                                 if (isCompact) 10.dp else 14.dp
                                             } else {
                                                 if (isCompact) 7.dp else 10.dp
                                             },
                                         ),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Text(
                                        text = nuvioFormatCueTimestamp(cue.startTimeMs),
                                        color = if (isActive) tokens.colors.accent else tokens.colors.textMuted,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                         modifier = Modifier.padding(top = 5.dp),
                                    )
                                    Text(
                                        text = cue.text,
                                        color = if (isActive) tokens.colors.textPrimary else tokens.colors.textSecondary,
                                         fontSize = if (isActive) {
                                             if (isCompact) 22.sp else 26.sp
                                         } else {
                                             if (isCompact) 17.sp else 20.sp
                                         },
                                         lineHeight = if (isActive) {
                                             if (isCompact) 27.sp else 32.sp
                                         } else {
                                             if (isCompact) 22.sp else 26.sp
                                         },
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }
                }
    }
}

@Composable
private fun SubtitleSyncControls(
    modifier: Modifier,
    isCompact: Boolean,
    selectedAddonSubtitle: AddonSubtitle?,
    subtitleAutoSyncState: SubtitleAutoSyncUiState,
    subtitleDelayMs: Int,
    isPlaying: Boolean,
    sortedCues: List<SubtitleSyncCue>,
    onSubtitleDelayChanged: (Int) -> Unit,
    onSubtitleDelayReset: () -> Unit,
    onAutoSyncCapture: () -> Unit,
    onAutoSyncReload: () -> Unit,
    onTogglePlayback: () -> Unit,
    tokens: com.nuvio.app.core.ui.NuvioThemeTokens,
) {
    Column(
        modifier = modifier
                .clip(RoundedCornerShape(18.dp))
                .background(tokens.colors.surfaceCard.copy(alpha = 0.84f))
                .border(1.dp, tokens.colors.borderSubtle, RoundedCornerShape(18.dp))
                .padding(if (isCompact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.CenterFocusStrong,
                    contentDescription = null,
                    tint = tokens.colors.accent,
                    modifier = Modifier.size(if (isCompact) 16.dp else 18.dp),
                )
                Text(
                    text = stringResource(Res.string.compose_player_auto_sync),
                    color = tokens.colors.textPrimary,
                    style = if (isCompact) {
                        MaterialTheme.typography.labelLarge
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text = selectedAddonSubtitle?.display
                    ?: stringResource(Res.string.compose_player_select_addon_subtitle_first),
                color = tokens.colors.textMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = stringResource(Res.string.compose_player_subtitle_delay),
                color = tokens.colors.textSecondary,
                style = if (isCompact) {
                    MaterialTheme.typography.labelLarge
                } else {
                    MaterialTheme.typography.titleMedium
                },
            )
            NuvioSyncStepper(
                isCompact = isCompact,
                value = nuvioFormatSubtitleDelay(subtitleDelayMs),
                onMinus = {
                    onSubtitleDelayChanged(
                        (subtitleDelayMs - SUBTITLE_DELAY_STEP_MS).coerceAtLeast(SUBTITLE_DELAY_MIN_MS),
                    )
                },
                onPlus = {
                    onSubtitleDelayChanged(
                        (subtitleDelayMs + SUBTITLE_DELAY_STEP_MS).coerceAtMost(SUBTITLE_DELAY_MAX_MS),
                    )
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NuvioSyncActionButton(
                    modifier = Modifier.weight(1f),
                    isCompact = isCompact,
                    text = if (isPlaying) stringResource(Res.string.compose_action_pause)
                    else stringResource(Res.string.action_play),
                    icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    enabled = selectedAddonSubtitle != null,
                    selected = isPlaying,
                    onClick = onTogglePlayback,
                )
                NuvioSyncActionButton(
                    modifier = Modifier.weight(1f),
                    isCompact = isCompact,
                    text = stringResource(Res.string.compose_player_reload),
                    icon = Icons.Rounded.Refresh,
                    enabled = selectedAddonSubtitle != null,
                    onClick = onAutoSyncReload,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NuvioSyncActionButton(
                    modifier = Modifier.weight(1f),
                    isCompact = isCompact,
                    text = stringResource(Res.string.compose_player_capture_line),
                    icon = Icons.Rounded.CenterFocusStrong,
                    enabled = selectedAddonSubtitle != null,
                    onClick = onAutoSyncCapture,
                )
                NuvioSyncActionButton(
                    modifier = Modifier.weight(1f),
                    isCompact = isCompact,
                    text = stringResource(Res.string.compose_player_reset),
                    enabled = true,
                    onClick = onSubtitleDelayReset,
                )
            }

            if (subtitleAutoSyncState.isLoading && sortedCues.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.compose_player_loading_lines),
                    color = tokens.colors.textMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            subtitleAutoSyncState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = tokens.colors.danger,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

@Composable
private fun NuvioSyncActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
) {
    val tokens = MaterialTheme.nuvio
    Row(
        modifier = modifier
            .heightIn(min = if (isCompact) 40.dp else 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    selected -> tokens.colors.accent
                    enabled -> tokens.colors.surfaceElevated
                    else -> tokens.colors.overlayDisabled
                },
            )
            .border(1.dp, tokens.colors.borderSubtle, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = if (isCompact) 8.dp else 12.dp,
                vertical = if (isCompact) 8.dp else 11.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = if (selected) tokens.colors.onAccent else tokens.colors.textSecondary,
                modifier = Modifier.size(if (isCompact) 16.dp else 20.dp),
            )
        }
        Text(
            text = text,
            color = when {
                selected -> tokens.colors.onAccent
                enabled -> tokens.colors.textPrimary
                else -> tokens.colors.textDisabled
            },
            style = if (isCompact) {
                MaterialTheme.typography.labelMedium
            } else {
                MaterialTheme.typography.labelLarge
            },
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun NuvioSyncStepper(
    isCompact: Boolean,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NuvioSyncStepperButton(
            isCompact = isCompact,
            icon = Icons.Rounded.KeyboardArrowDown,
            onClick = onMinus,
        )
        Text(
            text = value,
            color = tokens.colors.textPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontSize = if (isCompact) 18.sp else 22.sp,
            fontWeight = FontWeight.Bold,
        )
        NuvioSyncStepperButton(
            isCompact = isCompact,
            icon = Icons.Rounded.KeyboardArrowUp,
            onClick = onPlus,
        )
    }
}

@Composable
private fun NuvioSyncStepperButton(
    isCompact: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Box(
        modifier = Modifier
            .size(if (isCompact) 38.dp else 50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tokens.colors.accent.copy(alpha = 0.18f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tokens.colors.accent,
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun nuvioFormatSubtitleDelay(delayMs: Int): String {
    val sign = if (delayMs >= 0) "+" else "-"
    val absoluteMs = kotlin.math.abs(delayMs)
    return "$sign${absoluteMs / 1000}.${(absoluteMs % 1000).toString().padStart(3, '0')}s"
}

private fun nuvioFormatCueTimestamp(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000L).coerceAtLeast(0L)
    return "${totalSeconds / 60L}:${(totalSeconds % 60L).toString().padStart(2, '0')}"
}

@Composable
private fun SubtitleRail(
    title: String,
    width: Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MaterialTheme.nuvio

    Column(
        modifier = Modifier.width(width),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            color = tokens.colors.textMuted,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        content()
    }
}

@Composable
private fun SubtitleLanguageRow(
    item: SubtitleLanguageItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val label = when (item.key) {
        SubtitleOffLanguageKey -> stringResource(Res.string.compose_player_none)
        SubtitleUnknownLanguageKey -> stringResource(Res.string.subtitle_language_unknown)
        else -> languageLabelForCode(item.key)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) tokens.colors.accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f, fill = false),
            color = if (selected) tokens.colors.onAccent else Color.White,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.count > 0) {
            Text(
                text = item.count.toString(),
                modifier = Modifier
                    .padding(start = 6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (selected) Color.White.copy(alpha = 0.18f)
                        else tokens.colors.accent.copy(alpha = 0.85f),
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                color = tokens.colors.onAccent,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun SubtitleOptionRow(
    option: SubtitleSelectionOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val sourceLabel: String
    val title: String
    val metadata: String?

    when (option) {
        is SubtitleSelectionOption.BuiltIn -> {
            sourceLabel = stringResource(Res.string.compose_player_built_in)
            title = localizedTrackDisplayName(
                option.track.label,
                option.track.language,
                option.track.index,
            )
            metadata = if (option.track.isForced) {
                stringResource(Res.string.settings_playback_option_forced)
            } else {
                null
            }
        }

        is SubtitleSelectionOption.Addon -> {
            sourceLabel = option.subtitle.addonName ?: stringResource(Res.string.addon_title)
            title = languageLabelForCode(option.subtitle.language)
            metadata = option.subtitle.display.takeIf { it.isNotBlank() && it != title }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) tokens.colors.accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SubtitleSourceChip(label = sourceLabel, selected = selected)
            Text(
                text = title,
                color = if (selected) tokens.colors.onAccent else Color.White,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            metadata?.let {
                Text(
                    text = it,
                    color = if (selected) tokens.colors.onAccent.copy(alpha = 0.72f) else tokens.colors.textMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = tokens.colors.onAccent,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(20.dp),
            )
        }
    }
}

@Composable
private fun SubtitleSourceChip(
    label: String,
    selected: Boolean,
) {
    val tokens = MaterialTheme.nuvio
    val shape = RoundedCornerShape(999.dp)

    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                if (selected) tokens.colors.onAccent.copy(alpha = 0.14f)
                else Color.White.copy(alpha = 0.08f),
            )
            .then(
                if (selected) {
                    Modifier.border(1.dp, tokens.colors.onAccent.copy(alpha = 0.22f), shape)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            color = if (selected) tokens.colors.onAccent.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.78f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SubtitleRailEmptyState(
    text: String,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.CloudDownload,
            contentDescription = null,
            tint = tokens.colors.textMuted,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            color = tokens.colors.textMuted,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
