package com.nuvio.app.features.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.nuvioSafeBottomPadding
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.media_tv
import org.jetbrains.compose.resources.stringResource

@Composable
fun LiveTvScreen(
    modifier: Modifier = Modifier,
    onChannelClick: ((LiveTvChannel) -> Unit)? = null,
) {
    val channels = remember { demoLiveTvChannels() }
    val categories = remember { demoLiveTvCategories() }

    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background,
        ),
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 48.dp,
            end = 16.dp,
            bottom = nuvioSafeBottomPadding(28.dp),
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            LiveTvHeroCard()
        }

        item {
            LiveTvQuickStatsRow()
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(categories, key = { it }) { category ->
                    Surface(
                        onClick = { },
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                        ),
                    ) {
                        Text(
                            text = category,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Featured Channels",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
        }

        items(channels, key = { it.id }) { channel ->
            LiveTvChannelCard(
                channel = channel,
                onChannelClick = onChannelClick,
            )
        }
    }
}

@Composable
private fun LiveTvHeroCard() {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF121826),
                            Color(0xFF1E293B),
                            Color(0xFF0F172A),
                        ),
                    ),
                )
                .padding(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(30.dp),
                    )
                }

                Text(
                    text = stringResource(Res.string.media_tv),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Browse live channels, schedules, and on-air highlights.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiveTvMetaPill("24/7")
                    LiveTvMetaPill("HD")
                    LiveTvMetaPill("EPG")
                }
            }
        }
    }
}

@Composable
private fun LiveTvQuickStatsRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LiveTvStatCard(
            modifier = Modifier.width(110.dp),
            title = "128",
            subtitle = "Channels",
        )
        LiveTvStatCard(
            modifier = Modifier.width(110.dp),
            title = "16",
            subtitle = "Live now",
        )
        LiveTvStatCard(
            modifier = Modifier.width(110.dp),
            title = "4K",
            subtitle = "Ready",
        )
    }
}

@Composable
private fun LiveTvStatCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LiveTvMetaPill(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
        ),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LiveTvChannelCard(
    channel: LiveTvChannel,
    onChannelClick: ((LiveTvChannel) -> Unit)?,
) {
    Surface(
        onClick = { onChannelClick?.invoke(channel) },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(channel.accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = channel.shortName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(channel.liveDot),
                    )
                }

                Text(
                    text = channel.category,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Now Playing: ${channel.nowPlaying}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    LinearProgressIndicator(
                        progress = { channel.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = channel.accent,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Text(
                        text = channel.nextUp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

data class LiveTvChannel(
    val id: String,
    val name: String,
    val shortName: String,
    val category: String,
    val nowPlaying: String,
    val nextUp: String,
    val progress: Float,
    val accent: Color,
    val liveDot: Color,
)

private fun demoLiveTvChannels(): List<LiveTvChannel> =
    listOf(
        LiveTvChannel(
            id = "news-24",
            name = "News 24",
            shortName = "N24",
            category = "News",
            nowPlaying = "Morning Headlines",
            nextUp = "World Update in 12 min",
            progress = 0.62f,
            accent = Color(0xFF3B82F6),
            liveDot = Color(0xFF22C55E),
        ),
        LiveTvChannel(
            id = "sports-now",
            name = "Sports Now",
            shortName = "SP",
            category = "Sports",
            nowPlaying = "Live Match Coverage",
            nextUp = "Post-game analysis in 8 min",
            progress = 0.41f,
            accent = Color(0xFFF59E0B),
            liveDot = Color(0xFF22C55E),
        ),
        LiveTvChannel(
            id = "movie-premiere",
            name = "Movie Premiere",
            shortName = "MP",
            category = "Entertainment",
            nowPlaying = "Weekend Blockbuster",
            nextUp = "Behind the scenes in 19 min",
            progress = 0.78f,
            accent = Color(0xFFEC4899),
            liveDot = Color(0xFF22C55E),
        ),
        LiveTvChannel(
            id = "kids-zone",
            name = "Kids Zone",
            shortName = "KZ",
            category = "Kids",
            nowPlaying = "Animated Adventures",
            nextUp = "Learning Time in 25 min",
            progress = 0.23f,
            accent = Color(0xFF8B5CF6),
            liveDot = Color(0xFF22C55E),
        ),
    )

private fun demoLiveTvCategories(): List<String> =
    listOf("All", "News", "Sports", "Movies", "Kids", "Documentary")
