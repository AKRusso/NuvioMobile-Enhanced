package com.nuvio.app.features.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAddCheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.app.core.i18n.localizedSeasonEpisodeCode
import com.nuvio.app.core.ui.NuvioMediaActionOverlay
import com.nuvio.app.features.details.MetaVideo
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.episode_mark_previous_seasons_watched
import nuvio.composeapp.generated.resources.episode_mark_previous_unwatched
import nuvio.composeapp.generated.resources.episode_mark_previous_watched
import nuvio.composeapp.generated.resources.episode_mark_season_unwatched
import nuvio.composeapp.generated.resources.episode_mark_season_watched
import nuvio.composeapp.generated.resources.episode_mark_unwatched
import nuvio.composeapp.generated.resources.episode_mark_watched
import nuvio.composeapp.generated.resources.play_manually
import nuvio.composeapp.generated.resources.streams_download_file
import org.jetbrains.compose.resources.stringResource

@Composable
fun EpisodeWatchedActionSheet(
    episode: MetaVideo,
    seasonLabel: String,
    isEpisodeWatched: Boolean,
    canMarkPreviousEpisodes: Boolean,
    arePreviousEpisodesWatched: Boolean,
    isSeasonWatched: Boolean,
    onDismiss: () -> Unit,
    onToggleWatched: () -> Unit,
    onTogglePreviousWatched: () -> Unit,
    onToggleSeasonWatched: () -> Unit,
    onDownload: (() -> Unit)? = null,
    showPlayManually: Boolean = false,
    onPlayManually: (() -> Unit)? = null,
) {
    val artwork = episodeActionSheetArtwork(episode)

    fun dismissAfter(action: () -> Unit) {
        action()
        onDismiss()
    }

    NuvioMediaActionOverlay(
        artworkUrl = artwork,
        contentDescription = episode.title,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EpisodeActionSheetHeader(episode = episode, artwork = artwork)
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .widthIn(max = 390.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF171717).copy(alpha = 0.96f),
                shadowElevation = 18.dp,
                tonalElevation = 6.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (onDownload != null) {
                        EpisodeSheetActionRow(
                            icon = Icons.Default.Download,
                            title = stringResource(Res.string.streams_download_file),
                            prominent = true,
                            onClick = { dismissAfter(onDownload) },
                        )
                        EpisodeSheetDivider()
                    }
                    EpisodeSheetActionRow(
                        icon = Icons.Default.CheckCircle,
                        title = if (isEpisodeWatched) {
                            stringResource(Res.string.episode_mark_unwatched)
                        } else {
                            stringResource(Res.string.episode_mark_watched)
                        },
                        onClick = { dismissAfter(onToggleWatched) },
                    )
                    if (canMarkPreviousEpisodes) {
                        EpisodeSheetDivider()
                        EpisodeSheetActionRow(
                            icon = Icons.Default.DoneAll,
                            title = if (arePreviousEpisodesWatched) {
                                stringResource(Res.string.episode_mark_previous_unwatched)
                            } else {
                                stringResource(Res.string.episode_mark_previous_watched)
                            },
                            onClick = { dismissAfter(onTogglePreviousWatched) },
                        )
                    }
                    EpisodeSheetDivider()
                    EpisodeSheetActionRow(
                        icon = Icons.Default.PlaylistAddCheckCircle,
                        title = if (isSeasonWatched) {
                            stringResource(Res.string.episode_mark_season_unwatched, seasonLabel)
                        } else {
                            stringResource(Res.string.episode_mark_season_watched, seasonLabel)
                        },
                        onClick = { dismissAfter(onToggleSeasonWatched) },
                    )
                    if (showPlayManually && onPlayManually != null) {
                        EpisodeSheetDivider()
                        EpisodeSheetActionRow(
                            icon = Icons.Default.PlayArrow,
                            title = stringResource(Res.string.play_manually),
                            onClick = { dismissAfter(onPlayManually) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SeasonWatchedActionSheet(
    seasonLabel: String,
    isSeasonWatched: Boolean,
    canMarkPreviousSeasons: Boolean,
    onDismiss: () -> Unit,
    onToggleSeasonWatched: () -> Unit,
    onMarkPreviousSeasonsWatched: () -> Unit,
) {
    NuvioMediaActionOverlay(
        artworkUrl = null,
        contentDescription = seasonLabel,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 430.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = seasonLabel,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(22.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 390.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF171717).copy(alpha = 0.96f),
                shadowElevation = 18.dp,
                tonalElevation = 6.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    EpisodeSheetActionRow(
                        icon = Icons.Default.PlaylistAddCheckCircle,
                        title = if (isSeasonWatched) {
                            stringResource(Res.string.episode_mark_season_unwatched, seasonLabel)
                        } else {
                            stringResource(Res.string.episode_mark_season_watched, seasonLabel)
                        },
                        onClick = {
                            onToggleSeasonWatched()
                            onDismiss()
                        },
                    )
                    if (canMarkPreviousSeasons) {
                        EpisodeSheetDivider()
                        EpisodeSheetActionRow(
                            icon = Icons.Default.DoneAll,
                            title = stringResource(Res.string.episode_mark_previous_seasons_watched),
                            onClick = {
                                onMarkPreviousSeasonsWatched()
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeActionSheetHeader(
    episode: MetaVideo,
    artwork: String?,
) {
    val episodeCode = localizedSeasonEpisodeCode(
        seasonNumber = episode.season,
        episodeNumber = episode.episode,
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 430.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E1E1E)),
            contentAlignment = Alignment.Center,
        ) {
            if (artwork != null) {
                AsyncImage(
                    model = artwork,
                    contentDescription = episode.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.08f),
                                ),
                            ),
                        ),
                )
            } else {
                Text(
                    text = episode.title,
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = episode.title,
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .widthIn(max = 390.dp),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (episodeCode != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = episodeCode,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.58f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun episodeActionSheetArtwork(episode: MetaVideo): String? =
    episode.thumbnail?.takeIf { it.isNotBlank() }
        ?: episode.seasonPoster?.takeIf { it.isNotBlank() }

@Composable
private fun EpisodeSheetActionRow(
    icon: ImageVector,
    title: String,
    prominent: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (prominent) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent)
            .height(68.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = if (prominent) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.94f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (prominent) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.94f),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun EpisodeSheetDivider() {
    HorizontalDivider(
        color = Color.White.copy(alpha = 0.06f),
    )
}
