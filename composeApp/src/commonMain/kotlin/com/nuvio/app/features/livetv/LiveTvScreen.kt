package com.nuvio.app.features.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.NuvioIconActionButton
import com.nuvio.app.core.ui.NuvioInputField
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.core.ui.NuvioSectionLabel
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.nuvio
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.live_tv_add_source
import nuvio.composeapp.generated.resources.live_tv_all
import nuvio.composeapp.generated.resources.live_tv_channel_count
import nuvio.composeapp.generated.resources.live_tv_channels
import nuvio.composeapp.generated.resources.live_tv_disconnect
import nuvio.composeapp.generated.resources.live_tv_empty_description
import nuvio.composeapp.generated.resources.live_tv_empty_title
import nuvio.composeapp.generated.resources.live_tv_load
import nuvio.composeapp.generated.resources.live_tv_refresh
import nuvio.composeapp.generated.resources.live_tv_search
import nuvio.composeapp.generated.resources.live_tv_source_hint
import nuvio.composeapp.generated.resources.live_tv_source_title
import nuvio.composeapp.generated.resources.live_tv_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun LiveTvScreen(
    modifier: Modifier = Modifier,
    onChannelClick: (LiveTvChannel) -> Unit = {},
) {
    val uiState by remember {
        LiveTvRepository.ensureLoaded()
        LiveTvRepository.uiState
    }.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var sourceUrl by rememberSaveable { mutableStateOf(uiState.sourceUrl) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedGroup by rememberSaveable { mutableStateOf("") }
    var editingSource by rememberSaveable { mutableStateOf(uiState.sourceUrl.isBlank()) }

    LaunchedEffect(uiState.sourceUrl) {
        if (sourceUrl.isBlank()) sourceUrl = uiState.sourceUrl
    }
    LaunchedEffect(Unit) {
        if (uiState.sourceUrl.isNotBlank() && uiState.channels.isEmpty() && !uiState.isLoading) {
            LiveTvRepository.load(uiState.sourceUrl)
        }
    }

    val groups = remember(uiState.channels) {
        uiState.channels.map { it.group }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val visibleChannels = remember(uiState.channels, query, selectedGroup) {
        uiState.channels.filter { channel ->
            (selectedGroup.isBlank() || channel.group == selectedGroup) &&
                (query.isBlank() || channel.name.contains(query, ignoreCase = true))
        }
    }
    val loadSource: () -> Unit = {
        scope.launch {
            if (LiveTvRepository.load(sourceUrl).isSuccess) {
                editingSource = false
                selectedGroup = ""
            }
        }
        Unit
    }

    NuvioScreen(
        modifier = modifier,
        horizontalPadding = 16.dp,
    ) {
        item {
            NuvioScreenHeader(
                title = stringResource(Res.string.live_tv_title),
                includeStatusBarPadding = false,
                actions = {
                    if (uiState.channels.isNotEmpty()) {
                        NuvioIconActionButton(
                            icon = Icons.Rounded.Refresh,
                            contentDescription = stringResource(Res.string.live_tv_refresh),
                            onClick = { scope.launch { LiveTvRepository.load(uiState.sourceUrl) } },
                        )
                        NuvioIconActionButton(
                            icon = Icons.Rounded.AddLink,
                            contentDescription = stringResource(Res.string.live_tv_add_source),
                            onClick = { editingSource = !editingSource },
                        )
                    }
                },
            )
        }

        if (editingSource || uiState.sourceUrl.isBlank()) {
            item {
                LiveTvSourceCard(
                    sourceUrl = sourceUrl,
                    isLoading = uiState.isLoading,
                    errorMessage = uiState.errorMessage,
                    hasConnectedSource = uiState.channels.isNotEmpty(),
                    onSourceUrlChange = { sourceUrl = it },
                    onLoad = loadSource,
                    onDisconnect = {
                        LiveTvRepository.disconnect()
                        sourceUrl = ""
                        editingSource = true
                    },
                )
            }
        }

        if (uiState.channels.isNotEmpty()) {
            item {
                NuvioInputField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = stringResource(Res.string.live_tv_search),
                    trailingContent = {
                        if (query.isBlank()) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = MaterialTheme.nuvio.colors.textMuted,
                            )
                        } else {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.nuvio.colors.textMuted,
                                )
                            }
                        }
                    },
                )
            }

            if (groups.isNotEmpty()) {
                item {
                    LiveTvGroupRow(
                        groups = groups,
                        selectedGroup = selectedGroup,
                        onSelected = { selectedGroup = it },
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NuvioSectionLabel(text = stringResource(Res.string.live_tv_channels))
                    Text(
                        text = stringResource(Res.string.live_tv_channel_count, visibleChannels.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.nuvio.colors.textMuted,
                    )
                }
            }

            items(
                count = visibleChannels.size,
                key = { index -> visibleChannels[index].id },
            ) { index ->
                LiveTvChannelRow(
                    channel = visibleChannels[index],
                    onClick = { onChannelClick(visibleChannels[index]) },
                )
            }
        } else if (!uiState.isLoading && !editingSource) {
            item {
                LiveTvEmptyState(onAddSource = { editingSource = true })
            }
        }
    }
}

@Composable
private fun LiveTvSourceCard(
    sourceUrl: String,
    isLoading: Boolean,
    errorMessage: String?,
    hasConnectedSource: Boolean,
    onSourceUrlChange: (String) -> Unit,
    onLoad: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = tokens.colors.surface,
        shape = tokens.shapes.card,
    ) {
        Column(
            modifier = Modifier.padding(tokens.spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
            ) {
                Box(
                    modifier = Modifier
                        .size(NuvioTokens.Space.s48)
                        .clip(tokens.shapes.compactCard)
                        .background(tokens.colors.overlaySelected),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddLink,
                        contentDescription = null,
                        tint = tokens.colors.accent,
                    )
                }
                Column {
                    Text(
                        text = stringResource(Res.string.live_tv_source_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(Res.string.live_tv_empty_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.textMuted,
                    )
                }
            }
            NuvioInputField(
                value = sourceUrl,
                onValueChange = onSourceUrlChange,
                placeholder = stringResource(Res.string.live_tv_source_hint),
            )
            errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.colors.danger,
                )
            }
            NuvioPrimaryButton(
                text = stringResource(Res.string.live_tv_load),
                enabled = sourceUrl.isNotBlank() && !isLoading,
                onClick = onLoad,
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(NuvioTokens.Icon.md)
                        .align(Alignment.CenterHorizontally),
                    color = tokens.colors.accent,
                    strokeWidth = 2.dp,
                )
            }
            if (hasConnectedSource) {
                Text(
                    text = stringResource(Res.string.live_tv_disconnect),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable(onClick = onDisconnect)
                        .padding(8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.colors.danger,
                )
            }
        }
    }
}

@Composable
private fun LiveTvGroupRow(
    groups: List<String>,
    selectedGroup: String,
    onSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
    ) {
        LiveTvGroupChip(
            label = stringResource(Res.string.live_tv_all),
            selected = selectedGroup.isBlank(),
            onClick = { onSelected("") },
        )
        groups.forEach { group ->
            LiveTvGroupChip(
                label = group,
                selected = selectedGroup == group,
                onClick = { onSelected(group) },
            )
        }
    }
}

@Composable
private fun LiveTvGroupChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Surface(
        onClick = onClick,
        color = if (selected) tokens.colors.overlaySelected else tokens.colors.surface,
        shape = tokens.shapes.chip,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(
            width = NuvioTokens.Border.thin,
            color = tokens.colors.borderSubtle,
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) tokens.colors.accent else tokens.colors.textSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun LiveTvChannelRow(
    channel: LiveTvChannel,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = tokens.colors.surface,
        shape = tokens.shapes.compactCard,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.colors.surfaceCard),
                contentAlignment = Alignment.Center,
            ) {
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Tv,
                        contentDescription = null,
                        tint = tokens.colors.textMuted,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (channel.group.isNotBlank()) {
                    Text(
                        text = channel.group,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(tokens.shapes.avatar)
                    .background(tokens.colors.overlaySelected),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = tokens.colors.accent,
                )
            }
        }
    }
}

@Composable
private fun LiveTvEmptyState(onAddSource: () -> Unit) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Tv,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = tokens.colors.textMuted,
        )
        Text(
            text = stringResource(Res.string.live_tv_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = tokens.colors.textPrimary,
        )
        Text(
            text = stringResource(Res.string.live_tv_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.colors.textMuted,
        )
        Spacer(modifier = Modifier.height(4.dp))
        NuvioPrimaryButton(
            text = stringResource(Res.string.live_tv_add_source),
            modifier = Modifier.fillMaxWidth(0.72f),
            onClick = onAddSource,
        )
    }
}
