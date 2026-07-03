package com.nuvio.app.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.NuvioSurfaceCard
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.library.LibraryItem
import com.nuvio.app.features.library.LibraryRepository
import com.nuvio.app.features.library.LibraryUiState
import com.nuvio.app.features.profiles.AvatarCatalogItem
import com.nuvio.app.features.profiles.AvatarRepository
import com.nuvio.app.features.profiles.NuvioProfile
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.profiles.parseHexColor
import com.nuvio.app.features.profiles.profileAvatarImageUrl
import com.nuvio.app.features.profiles.profileBackgroundImageUrl
import com.nuvio.app.features.watched.WatchedClock
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.watched.WatchedUiState
import com.nuvio.app.features.watchprogress.CurrentDateProvider
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import com.nuvio.app.features.watchprogress.WatchProgressUiState
import kotlin.math.roundToInt
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_nav_profile
import nuvio.composeapp.generated.resources.home_hero_type_movie
import nuvio.composeapp.generated.resources.home_hero_type_series
import nuvio.composeapp.generated.resources.profile_insights_badge
import nuvio.composeapp.generated.resources.profile_insights_hours
import nuvio.composeapp.generated.resources.profile_insights_minutes
import nuvio.composeapp.generated.resources.profile_insights_resume_progress
import nuvio.composeapp.generated.resources.profile_insights_section_overview
import nuvio.composeapp.generated.resources.profile_insights_section_smart_resume
import nuvio.composeapp.generated.resources.profile_insights_section_taste
import nuvio.composeapp.generated.resources.profile_insights_smart_resume_empty_subtitle
import nuvio.composeapp.generated.resources.profile_insights_smart_resume_empty_title
import nuvio.composeapp.generated.resources.profile_insights_stat_completed
import nuvio.composeapp.generated.resources.profile_insights_stat_completed_caption
import nuvio.composeapp.generated.resources.profile_insights_stat_continue
import nuvio.composeapp.generated.resources.profile_insights_stat_continue_caption
import nuvio.composeapp.generated.resources.profile_insights_stat_library
import nuvio.composeapp.generated.resources.profile_insights_stat_library_caption
import nuvio.composeapp.generated.resources.profile_insights_stat_recent
import nuvio.composeapp.generated.resources.profile_insights_stat_recent_caption
import nuvio.composeapp.generated.resources.profile_insights_stat_time
import nuvio.composeapp.generated.resources.profile_insights_stat_time_caption
import nuvio.composeapp.generated.resources.profile_insights_stat_upcoming
import nuvio.composeapp.generated.resources.profile_insights_stat_upcoming_caption
import nuvio.composeapp.generated.resources.profile_insights_subtitle
import nuvio.composeapp.generated.resources.profile_insights_switch_profile
import nuvio.composeapp.generated.resources.profile_insights_taste_empty
import nuvio.composeapp.generated.resources.profile_insights_taste_subtitle
import nuvio.composeapp.generated.resources.profile_insights_taste_title
import nuvio.composeapp.generated.resources.profile_insights_title
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.profileInsightsContent(
    isTablet: Boolean,
    onSwitchProfile: (() -> Unit)?,
) {
    item {
        ProfileInsightsBody(
            isTablet = isTablet,
            onSwitchProfile = onSwitchProfile,
        )
    }
}

@Composable
private fun ProfileInsightsBody(
    isTablet: Boolean,
    onSwitchProfile: (() -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
    val avatars by AvatarRepository.avatars.collectAsStateWithLifecycle()
    val watchProgressState by remember {
        WatchProgressRepository.ensureLoaded()
        WatchProgressRepository.uiState
    }.collectAsStateWithLifecycle()
    val watchedState by remember {
        WatchedRepository.ensureLoaded()
        WatchedRepository.uiState
    }.collectAsStateWithLifecycle()
    val libraryState by remember {
        LibraryRepository.ensureLoaded()
        LibraryRepository.uiState
    }.collectAsStateWithLifecycle()
    val todayIsoDate = remember { CurrentDateProvider.todayIsoDate() }

    LaunchedEffect(Unit) {
        AvatarRepository.fetchAvatars()
    }

    val activeProfile = profileState.activeProfile
    val avatarItem = remember(activeProfile?.avatarId, avatars) {
        activeProfile
            ?.avatarId
            ?.let { avatarId -> avatars.firstOrNull { avatar -> avatar.id == avatarId } }
    }
    val profileNameFallback = stringResource(Res.string.compose_nav_profile)
    val profileName = activeProfile
        ?.name
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: profileNameFallback
    val stats = remember(watchProgressState, watchedState, libraryState, todayIsoDate) {
        buildProfileInsightsStats(
            watchProgressState = watchProgressState,
            watchedState = watchedState,
            libraryState = libraryState,
            todayIsoDate = todayIsoDate,
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (isTablet) 18.dp else 14.dp),
    ) {
        ProfileInsightsHero(
            profile = activeProfile,
            avatarItem = avatarItem,
            profileName = profileName,
            stats = stats,
            isTablet = isTablet,
        )
        SettingsSection(
            title = stringResource(Res.string.profile_insights_section_overview),
            isTablet = isTablet,
        ) {
            ProfileInsightsStatsGrid(
                stats = stats,
                isTablet = isTablet,
            )
        }
        SettingsSection(
            title = stringResource(Res.string.profile_insights_section_smart_resume),
            isTablet = isTablet,
        ) {
            ProfileSmartResumeCard(signal = stats.smartResume)
        }
        SettingsSection(
            title = stringResource(Res.string.profile_insights_section_taste),
            isTablet = isTablet,
        ) {
            ProfileTasteCard(stats = stats)
        }
        if (onSwitchProfile != null) {
            NuvioPrimaryButton(
                text = stringResource(Res.string.profile_insights_switch_profile),
                onClick = onSwitchProfile,
                modifier = Modifier.padding(top = tokens.spacing.controlGap),
            )
        }
    }
}

@Composable
private fun ProfileInsightsHero(
    profile: NuvioProfile?,
    avatarItem: AvatarCatalogItem?,
    profileName: String,
    stats: ProfileInsightsStats,
    isTablet: Boolean,
) {
    val tokens = MaterialTheme.nuvio
    val accent = profile?.avatarColorHex?.let(::parseHexColor) ?: tokens.colors.accent
    val avatarImageUrl = remember(profile, avatarItem) {
        profile?.let { profileAvatarImageUrl(it, avatarItem) }
    }
    val backgroundImageUrl = remember(profile) {
        profile?.let(::profileBackgroundImageUrl)
    }
    val shape = RoundedCornerShape(if (isTablet) 34.dp else 28.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF0E1727),
                        accent.copy(alpha = 0.42f),
                        tokens.colors.surface,
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape),
    ) {
        if (!backgroundImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = backgroundImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.22f),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.34f),
                            Color.Transparent,
                        ),
                        radius = 760f,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.04f),
                            Color.Black.copy(alpha = 0.56f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isTablet) 24.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfileHeroAvatar(
                    profileName = profileName,
                    avatarImageUrl = avatarImageUrl,
                    avatarColor = accent,
                    avatarBackgroundColor = avatarItem?.bgColor?.let(::parseHexColor) ?: accent,
                    isTablet = isTablet,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProfileInsightPill(
                        text = stringResource(Res.string.profile_insights_badge),
                        icon = Icons.Rounded.AutoAwesome,
                    )
                    Text(
                        text = stringResource(Res.string.profile_insights_title, profileName),
                        style = if (isTablet) {
                            MaterialTheme.typography.headlineMedium
                        } else {
                            MaterialTheme.typography.headlineSmall
                        },
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(Res.string.profile_insights_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.74f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ProfileHeroMetric(
                    value = stats.continueCount.toString(),
                    label = stringResource(Res.string.profile_insights_stat_continue),
                    modifier = Modifier.weight(1f),
                )
                ProfileHeroMetric(
                    value = stats.libraryCount.toString(),
                    label = stringResource(Res.string.profile_insights_stat_library),
                    modifier = Modifier.weight(1f),
                )
                ProfileHeroMetric(
                    value = stats.upcomingCount.toString(),
                    label = stringResource(Res.string.profile_insights_stat_upcoming),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ProfileHeroAvatar(
    profileName: String,
    avatarImageUrl: String?,
    avatarColor: Color,
    avatarBackgroundColor: Color,
    isTablet: Boolean,
) {
    val size = if (isTablet) 92.dp else 78.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (avatarImageUrl.isNullOrBlank()) {
                    avatarColor.copy(alpha = 0.18f)
                } else {
                    avatarBackgroundColor
                },
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.28f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (!avatarImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarImageUrl,
                contentDescription = profileName,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = profileName.take(1).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ProfileInsightPill(
    text: String,
    icon: ImageVector,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = Color.White,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ProfileHeroMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.70f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProfileInsightsStatsGrid(
    stats: ProfileInsightsStats,
    isTablet: Boolean,
) {
    val tiles = listOf(
        ProfileInsightTile(
            icon = Icons.Rounded.PlayArrow,
            value = stats.continueCount.toString(),
            label = stringResource(Res.string.profile_insights_stat_continue),
            caption = stringResource(Res.string.profile_insights_stat_continue_caption),
        ),
        ProfileInsightTile(
            icon = Icons.Rounded.Favorite,
            value = stats.completedCount.toString(),
            label = stringResource(Res.string.profile_insights_stat_completed),
            caption = stringResource(Res.string.profile_insights_stat_completed_caption),
        ),
        ProfileInsightTile(
            icon = Icons.Rounded.CollectionsBookmark,
            value = stats.libraryCount.toString(),
            label = stringResource(Res.string.profile_insights_stat_library),
            caption = stringResource(Res.string.profile_insights_stat_library_caption),
        ),
        ProfileInsightTile(
            icon = Icons.Rounded.AutoAwesome,
            value = profileInsightDurationLabel(stats.trackedDurationMs),
            label = stringResource(Res.string.profile_insights_stat_time),
            caption = stringResource(Res.string.profile_insights_stat_time_caption),
        ),
        ProfileInsightTile(
            icon = Icons.Rounded.Notifications,
            value = stats.recentActivityCount.toString(),
            label = stringResource(Res.string.profile_insights_stat_recent),
            caption = stringResource(Res.string.profile_insights_stat_recent_caption),
        ),
        ProfileInsightTile(
            icon = Icons.Rounded.CollectionsBookmark,
            value = stats.upcomingCount.toString(),
            label = stringResource(Res.string.profile_insights_stat_upcoming),
            caption = stringResource(Res.string.profile_insights_stat_upcoming_caption),
        ),
    )
    val columns = if (isTablet) 3 else 2

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tiles.chunked(columns).forEach { rowTiles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowTiles.forEach { tile ->
                    ProfileInsightStatCard(
                        tile = tile,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columns - rowTiles.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProfileInsightStatCard(
    tile: ProfileInsightTile,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    Surface(
        modifier = modifier.heightIn(min = 116.dp),
        color = tokens.colors.surface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, tokens.colors.borderSubtle),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = tile.value,
                    style = MaterialTheme.typography.titleLarge,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Surface(
                    modifier = Modifier.size(34.dp),
                    color = tokens.colors.accent.copy(alpha = tokens.opacity.pressed),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = tile.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = tokens.colors.accent,
                        )
                    }
                }
            }
            Text(
                text = tile.label,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = tile.caption,
                style = MaterialTheme.typography.labelSmall,
                color = tokens.colors.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProfileSmartResumeCard(signal: SmartResumeSignal?) {
    val tokens = MaterialTheme.nuvio
    NuvioSurfaceCard {
        if (signal == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfileSmartResumeArtwork(
                    imageUrl = null,
                    title = "",
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.profile_insights_smart_resume_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(Res.string.profile_insights_smart_resume_empty_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textMuted,
                    )
                }
            }
            return@NuvioSurfaceCard
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileSmartResumeArtwork(
                imageUrl = signal.imageUrl,
                title = signal.title,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = signal.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                signal.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                LinearProgressIndicator(
                    progress = { signal.progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = tokens.colors.accent,
                    trackColor = tokens.colors.borderSubtle,
                )
                Text(
                    text = stringResource(
                        Res.string.profile_insights_resume_progress,
                        (signal.progressFraction * 100f).roundToInt().coerceIn(1, 99),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun ProfileSmartResumeArtwork(
    imageUrl: String?,
    title: String,
) {
    val tokens = MaterialTheme.nuvio
    Box(
        modifier = Modifier
            .size(width = 92.dp, height = 62.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = tokens.colors.accent,
            )
        }
    }
}

@Composable
private fun ProfileTasteCard(stats: ProfileInsightsStats) {
    val tokens = MaterialTheme.nuvio
    val fallbackType = when (stats.topType) {
        "movie" -> stringResource(Res.string.home_hero_type_movie)
        "series" -> stringResource(Res.string.home_hero_type_series)
        null -> null
        else -> stats.topType.fallbackDisplayLabel()
    }
    val topSignal = stats.topGenre ?: fallbackType ?: stringResource(Res.string.profile_insights_taste_empty)

    NuvioSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                color = tokens.colors.accent.copy(alpha = tokens.opacity.pressed),
                shape = RoundedCornerShape(18.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = tokens.colors.accent,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = stringResource(Res.string.profile_insights_taste_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.colors.textMuted,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = topSignal,
                    style = MaterialTheme.typography.titleLarge,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(Res.string.profile_insights_taste_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun profileInsightDurationLabel(durationMs: Long): String {
    val minutes = (durationMs / ProfileInsightsMinuteMs).coerceAtLeast(0L)
    return if (minutes >= 60L) {
        stringResource(Res.string.profile_insights_hours, ((minutes + 30L) / 60L).toInt())
    } else {
        stringResource(Res.string.profile_insights_minutes, minutes.toInt())
    }
}

private fun buildProfileInsightsStats(
    watchProgressState: WatchProgressUiState,
    watchedState: WatchedUiState,
    libraryState: LibraryUiState,
    todayIsoDate: String,
): ProfileInsightsStats {
    val now = WatchedClock.nowEpochMs()
    val recentCutoff = now - ProfileInsightsRecentWindowMs
    val progressEntries = watchProgressState.entries
    val libraryItems = libraryState.items
    val watchedItems = watchedState.items

    return ProfileInsightsStats(
        continueCount = progressEntries.count { entry ->
            entry.isResumable && entry.progressFraction >= 0.02f
        },
        completedCount = watchedItems.size,
        libraryCount = libraryItems.size,
        trackedDurationMs = progressEntries.sumOf(WatchProgressEntry::profileTrackedDurationMs),
        recentActivityCount = watchedItems.count { it.markedAtEpochMs >= recentCutoff } +
            progressEntries.count { it.lastUpdatedEpochMs >= recentCutoff },
        upcomingCount = libraryItems.count { item ->
            item.profileReleaseIsoDate()?.let { releaseDate -> releaseDate >= todayIsoDate } == true
        },
        smartResume = progressEntries
            .asSequence()
            .filter { entry -> entry.isResumable && entry.progressFraction in 0.05f..0.92f }
            .sortedByDescending { entry -> entry.lastUpdatedEpochMs }
            .map(WatchProgressEntry::toSmartResumeSignal)
            .firstOrNull(),
        topGenre = libraryItems.profileTopGenre(),
        topType = (libraryItems.map { item -> item.type } + progressEntries.map { entry -> entry.parentMetaType })
            .mapNotNull(String::profileNormalizedType)
            .profileMostCommonValue(),
    )
}

private fun WatchProgressEntry.toSmartResumeSignal(): SmartResumeSignal =
    SmartResumeSignal(
        title = title.trim().takeIf { it.isNotBlank() } ?: parentMetaId,
        subtitle = profileEpisodeLine(),
        imageUrl = episodeThumbnail ?: background ?: poster,
        progressFraction = progressFraction,
    )

private fun WatchProgressEntry.profileEpisodeLine(): String? {
    val episodeCode = if (seasonNumber != null && episodeNumber != null) {
        "S${seasonNumber}E${episodeNumber}"
    } else {
        null
    }
    val cleanTitle = episodeTitle?.trim()?.takeIf { it.isNotBlank() }
    return when {
        episodeCode != null && cleanTitle != null -> "$episodeCode - $cleanTitle"
        episodeCode != null -> episodeCode
        cleanTitle != null -> cleanTitle
        else -> null
    }
}

private fun WatchProgressEntry.profileTrackedDurationMs(): Long {
    if (durationMs <= 0L) return lastPositionMs.coerceAtLeast(0L)
    if (isEffectivelyCompleted) return durationMs
    if (lastPositionMs > 0L) return lastPositionMs.coerceIn(0L, durationMs)
    val explicitPercent = normalizedProgressPercent ?: return 0L
    return (durationMs * (explicitPercent / 100f)).toLong().coerceIn(0L, durationMs)
}

private fun List<LibraryItem>.profileTopGenre(): String? =
    asSequence()
        .flatMap { item -> item.genres.asSequence() }
        .map { genre -> genre.trim() }
        .filter { genre -> genre.isNotBlank() }
        .groupingBy { genre -> genre }
        .eachCount()
        .maxByOrNull { (_, count) -> count }
        ?.key

private fun LibraryItem.profileReleaseIsoDate(): String? =
    releaseInfo.profileExtractIsoDate()

private fun String?.profileExtractIsoDate(): String? {
    val value = this?.trim().orEmpty()
    if (value.length < 10) return null

    for (start in 0..(value.length - 10)) {
        val candidate = value.substring(start, start + 10)
        if (candidate.isIsoDateCandidate()) return candidate
    }
    return null
}

private fun String.isIsoDateCandidate(): Boolean =
    length == 10 &&
        this[4] == '-' &&
        this[7] == '-' &&
        take(4).all(Char::isDigit) &&
        substring(5, 7).all(Char::isDigit) &&
        substring(8, 10).all(Char::isDigit)

private fun String.profileNormalizedType(): String? =
    when (trim().lowercase()) {
        "movie", "film" -> "movie"
        "series", "show", "tv", "tvshow" -> "series"
        "" -> null
        else -> trim().lowercase()
    }

private fun List<String>.profileMostCommonValue(): String? =
    filter { value -> value.isNotBlank() }
        .groupingBy { value -> value }
        .eachCount()
        .maxByOrNull { (_, count) -> count }
        ?.key

private fun String.fallbackDisplayLabel(): String {
    val clean = trim()
    if (clean.isBlank()) return clean
    return clean.replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase() else char.toString()
    }
}

private data class ProfileInsightsStats(
    val continueCount: Int,
    val completedCount: Int,
    val libraryCount: Int,
    val trackedDurationMs: Long,
    val recentActivityCount: Int,
    val upcomingCount: Int,
    val smartResume: SmartResumeSignal?,
    val topGenre: String?,
    val topType: String?,
)

private data class SmartResumeSignal(
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val progressFraction: Float,
)

private data class ProfileInsightTile(
    val icon: ImageVector,
    val value: String,
    val label: String,
    val caption: String,
)

private const val ProfileInsightsMinuteMs = 60_000L
private const val ProfileInsightsRecentWindowMs = 7L * 24L * 60L * 60L * 1000L
