package com.nuvio.app.features.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.home.HomeReleaseRadarItem
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.buildHomeReleaseRadarItems
import com.nuvio.app.features.home.homeRadarDetailsRequestKey
import com.nuvio.app.features.home.libraryItemKeyForHomeRadar
import com.nuvio.app.features.home.components.HomeReleaseRadarSection
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.settings.NuvioEnhancedSettingsRepository
import com.nuvio.app.features.settings.NuvioEnhancedSettingsUiState
import com.nuvio.app.features.settings.filteredByNuvioEnhancedReleaseRadar
import com.nuvio.app.features.watchprogress.CurrentDateProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal data class LibraryEnhancedContent(
    val settings: NuvioEnhancedSettingsUiState,
    val releaseRadarItems: List<HomeReleaseRadarItem>,
    val healthReport: LibraryHealthReport,
)

@Composable
internal fun rememberLibraryEnhancedContent(items: List<LibraryItem>): LibraryEnhancedContent {
    val settings by remember {
        NuvioEnhancedSettingsRepository.ensureLoaded()
        NuvioEnhancedSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val profileId = ProfileRepository.activeProfileId
    val detailsRequestKey = remember(items) { items.homeRadarDetailsRequestKey() }
    var resolvedDetails by remember(profileId, detailsRequestKey) {
        mutableStateOf<Map<String, MetaDetails>>(emptyMap())
    }

    LaunchedEffect(profileId, detailsRequestKey) {
        resolvedDetails = if (detailsRequestKey.isBlank()) {
            emptyMap()
        } else {
            resolveLibraryRadarDetails(items)
        }
    }

    val todayIsoDate = CurrentDateProvider.todayIsoDate()
    val releaseRadarItems = remember(todayIsoDate, items, resolvedDetails, settings) {
        buildHomeReleaseRadarItems(
            todayIsoDate = todayIsoDate,
            continueWatchingItems = emptyList(),
            libraryItems = items,
            catalogSections = emptyList(),
            resolvedLibraryDetails = resolvedDetails,
        ).filteredByNuvioEnhancedReleaseRadar(settings)
    }
    val healthReport = remember(items, releaseRadarItems, todayIsoDate) {
        buildLibraryHealthReport(items, releaseRadarItems, todayIsoDate)
    }

    return LibraryEnhancedContent(settings, releaseRadarItems, healthReport)
}

internal fun LazyListScope.libraryEnhancedSections(
    content: LibraryEnhancedContent,
    onPosterClick: ((LibraryItem) -> Unit)?,
) {
    if (content.settings.enhancedHomeFeaturesEnabled && content.settings.libraryHealthEnabled) {
        item(key = "library_health") {
            LibraryHealthCard(
                report = content.healthReport,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
    }
    if (content.releaseRadarItems.isNotEmpty()) {
        item(key = "library_release_radar") {
            HomeReleaseRadarSection(
                items = content.releaseRadarItems,
                modifier = Modifier.padding(bottom = 12.dp),
                sectionPadding = 16.dp,
                showDigest = content.settings.releaseRadarDigestEnabled,
                onPosterClick = onPosterClick?.let { posterClick ->
                    { preview: MetaPreview -> posterClick(preview.toLibraryItem(savedAtEpochMs = 0L)) }
                },
            )
        }
    }
}

private suspend fun resolveLibraryRadarDetails(items: List<LibraryItem>): Map<String, MetaDetails> =
    coroutineScope {
        val resolved = mutableListOf<Pair<String, MetaDetails>>()
        items
            .filter(LibraryItem::isSeriesLike)
            .take(24)
            .chunked(4)
            .forEach { chunk ->
                resolved += chunk.map { item ->
                    async {
                        val details = runCatching {
                            MetaDetailsRepository.fetch(item.type, item.id)
                        }.getOrNull() ?: return@async null
                        libraryItemKeyForHomeRadar(item) to details
                    }
                }.awaitAll().filterNotNull()
            }
        resolved.toMap()
    }

private fun LibraryItem.isSeriesLike(): Boolean =
    type.equals("series", ignoreCase = true) ||
        type.equals("tv", ignoreCase = true) ||
        type.equals("show", ignoreCase = true) ||
        type.equals("tvshow", ignoreCase = true)

internal data class LibraryHealthReport(
    val score: Int,
    val missingArtwork: Int,
    val thinMetadata: Int,
    val upcomingSignals: Int,
    val summary: String,
)

private fun buildLibraryHealthReport(
    items: List<LibraryItem>,
    releaseRadarItems: List<HomeReleaseRadarItem>,
    todayIsoDate: String,
): LibraryHealthReport {
    if (items.isEmpty()) {
        return LibraryHealthReport(
            score = 100,
            missingArtwork = 0,
            thinMetadata = 0,
            upcomingSignals = 0,
            summary = "No saved titles to scan yet.",
        )
    }

    val missingArtwork = items.count { item -> item.poster.isNullOrBlank() && item.banner.isNullOrBlank() }
    val thinMetadata = items.count { item ->
        item.description.isNullOrBlank() || item.genres.isEmpty() || item.imdbRating.isNullOrBlank()
    }
    val futureLibraryDates = items.count { item ->
        item.releaseInfo
            ?.substringBefore('T')
            ?.takeIf { date -> ISO_DATE_REGEX.matches(date) }
            ?.let { date -> date >= todayIsoDate } == true
    }
    val score = (100 - (((missingArtwork + thinMetadata).toFloat() / (items.size * 2f)) * 100f).toInt())
        .coerceIn(0, 100)
    val summary = when {
        missingArtwork == 0 && thinMetadata == 0 && releaseRadarItems.isNotEmpty() ->
            "Your library looks polished and has active upcoming-release coverage."
        missingArtwork == 0 && thinMetadata == 0 ->
            "Your saved titles look polished. Radar will light up when upcoming releases appear."
        else ->
            "Found ${missingArtwork + thinMetadata} quality signals across ${items.size} saved titles."
    }

    return LibraryHealthReport(
        score = score,
        missingArtwork = missingArtwork,
        thinMetadata = thinMetadata,
        upcomingSignals = (releaseRadarItems.size + futureLibraryDates).coerceAtLeast(releaseRadarItems.size),
        summary = summary,
    )
}

@Composable
private fun LibraryHealthCard(
    report: LibraryHealthReport,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = colors.surface.copy(alpha = 0.84f),
        border = BorderStroke(1.dp, colors.onSurface.copy(alpha = 0.10f)),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(colors.primary.copy(alpha = 0.18f), Color.Transparent),
                    ),
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = colors.primary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Library Health",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = report.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "${report.score}%",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.primary,
                    fontWeight = FontWeight.Black,
                )
            }
            LinearProgressIndicator(
                progress = { report.score / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = colors.primary,
                trackColor = colors.onSurface.copy(alpha = 0.08f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LibraryHealthMetric(report.missingArtwork.toString(), "artwork gaps", Modifier.weight(1f))
                LibraryHealthMetric(report.thinMetadata.toString(), "thin metadata", Modifier.weight(1f))
                LibraryHealthMetric(report.upcomingSignals.toString(), "radar signals", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LibraryHealthMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.onSurface.copy(alpha = 0.055f))
            .border(1.dp, colors.onSurface.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val ISO_DATE_REGEX = Regex("\\d{4}-\\d{2}-\\d{2}")
