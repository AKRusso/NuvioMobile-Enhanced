package com.nuvio.app.features.downloads

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.i18n.localizedSeasonEpisodeCode
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.NuvioPrimaryButton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_cancel
import nuvio.composeapp.generated.resources.action_done
import nuvio.composeapp.generated.resources.downloads_awaiting_source
import nuvio.composeapp.generated.resources.downloads_batch_summary
import nuvio.composeapp.generated.resources.downloads_choose_source
import nuvio.composeapp.generated.resources.downloads_continue_searching
import nuvio.composeapp.generated.resources.downloads_finding_sources
import nuvio.composeapp.generated.resources.downloads_no_compatible_sources
import nuvio.composeapp.generated.resources.downloads_pending_choice_message
import nuvio.composeapp.generated.resources.downloads_pending_choice_title
import nuvio.composeapp.generated.resources.downloads_pending_provider_quality
import nuvio.composeapp.generated.resources.streams_download_file
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingSourceChoiceSheet(
    group: PendingShowGroup,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var options by remember(group.parentMetaId) { mutableStateOf<List<DownloadSourceOption>>(emptyList()) }
    var selectedOption by remember(group.parentMetaId) { mutableStateOf<DownloadSourceOption?>(null) }
    var isLoadingOptions by remember(group.parentMetaId) { mutableStateOf(true) }
    var isSubmitting by remember(group.parentMetaId) { mutableStateOf(false) }
    var isComplete by remember(group.parentMetaId) { mutableStateOf(false) }
    var result by remember(group.parentMetaId) { mutableStateOf<BatchDownloadResult?>(null) }

    val firstSearch = group.searches.firstOrNull()

    fun target(search: PendingEpisodeDownload) = EpisodeDownloadTarget(
        videoId = search.videoId,
        parentMetaId = search.parentMetaId,
        parentMetaType = search.parentMetaType,
        seasonNumber = search.seasonNumber,
        episodeNumber = search.episodeNumber,
        title = search.episodeTitle,
        thumbnail = search.episodeThumbnail,
        overview = search.episodeOverview,
        searchTitle = search.title,
    )

    LaunchedEffect(group.parentMetaId) {
        val first = firstSearch ?: return@LaunchedEffect
        try {
            val firstOptions = DownloadSourceResolver.options(group.contentType, target(first))
            val compatibleSourceKeys = coroutineScope {
                group.searches.drop(1).map { search ->
                    async {
                        DownloadSourceResolver.options(group.contentType, target(search))
                            .mapTo(mutableSetOf()) { option -> option.providerAddonId to option.qualityKey }
                    }
                }.fold(
                    firstOptions.mapTo(mutableSetOf()) { option -> option.providerAddonId to option.qualityKey },
                ) { availableForAll, deferred ->
                    availableForAll.apply { retainAll(deferred.await()) }
                }
            }
            // The selected first-episode stream is reused for that episode. Keep only
            // provider/quality pairs that are available for every waiting episode.
            options = firstOptions.filter { option ->
                (option.providerAddonId to option.qualityKey) in compatibleSourceKeys
            }
        } finally {
            isLoadingOptions = false
        }
    }

    NuvioModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        fullHeight = true,
    ) {
        if (isComplete) {
            PendingChoiceComplete(
                result = result,
                onDone = onDismiss,
            )
            return@NuvioModalBottomSheet
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(Res.string.downloads_pending_choice_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(Res.string.downloads_pending_choice_message, group.searches.size),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "pending-show-title") {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                items(group.searches, key = { it.id }) { search ->
                    PendingEpisodeRow(search = search)
                }

                item(key = "pending-source-label") {
                    Text(
                        text = stringResource(Res.string.downloads_choose_source),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }

                if (isLoadingOptions) {
                    item(key = "pending-sources-loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(Res.string.downloads_finding_sources),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else if (options.isEmpty()) {
                    item(key = "pending-sources-empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(Res.string.downloads_no_compatible_sources),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    items(
                        items = options,
                        key = { option -> "${option.providerAddonId}:${option.qualityKey}:${option.stream.streamLabel}" },
                    ) { option ->
                        PendingSourceOptionCard(
                            option = option,
                            selected = option == selectedOption,
                            onClick = { selectedOption = option },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PendingSecondaryButton(
                    text = stringResource(Res.string.downloads_continue_searching),
                    modifier = Modifier.weight(0.9f),
                    enabled = !isSubmitting,
                    onClick = {
                        isSubmitting = true
                        scope.launch {
                            try {
                                coroutineScope {
                                    group.searches.map { search ->
                                        async { EpisodeDownloadCoordinator.retryPending(search.id) }
                                    }.forEach { it.await() }
                                }
                            } finally {
                                isSubmitting = false
                                onDismiss()
                            }
                        }
                    },
                )
                NuvioPrimaryButton(
                    text = stringResource(Res.string.streams_download_file),
                    modifier = Modifier.weight(1.1f),
                    enabled = selectedOption != null && !isSubmitting,
                    onClick = {
                        val option = selectedOption ?: return@NuvioPrimaryButton
                        isSubmitting = true
                        scope.launch {
                            try {
                                result = EpisodeDownloadCoordinator.enqueue(
                                    contentType = group.contentType,
                                    parentMetaId = group.parentMetaId,
                                    parentMetaType = group.parentMetaType,
                                    title = group.title,
                                    logo = group.logo,
                                    poster = group.poster,
                                    background = group.background,
                                    targets = group.searches.map(::target),
                                    selectedOption = option,
                                    removeResolvedPending = true,
                                )
                                isComplete = true
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                )
            }

            PendingSecondaryButton(
                text = stringResource(Res.string.action_cancel),
                enabled = !isSubmitting,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun PendingEpisodeRow(search: PendingEpisodeDownload) {
    val episodeCode = localizedSeasonEpisodeCode(search.seasonNumber, search.episodeNumber)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = listOfNotNull(episodeCode, search.episodeTitle)
                        .joinToString(" - ")
                        .ifBlank { search.episodeTitle ?: search.title },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        Res.string.downloads_pending_provider_quality,
                        search.providerName,
                        search.qualityKey,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Rounded.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PendingSourceOptionCard(
    option: DownloadSourceOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(180),
        label = "pending_source_selected",
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = color,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(option.providerName, fontWeight = FontWeight.SemiBold)
                Text(
                    option.qualityLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun PendingChoiceComplete(
    result: BatchDownloadResult?,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(Res.string.downloads_pending_choice_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        result?.let { summary ->
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
        NuvioPrimaryButton(text = stringResource(Res.string.action_done), onClick = onDone)
    }
}

@Composable
private fun PendingSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (enabled) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
