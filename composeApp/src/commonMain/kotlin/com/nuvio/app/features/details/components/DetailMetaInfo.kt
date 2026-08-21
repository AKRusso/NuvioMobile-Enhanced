package com.nuvio.app.features.details.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.ui.nuvioHorizontalScrollBleed
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaExternalRating
import com.nuvio.app.features.details.formatRuntimeForDisplay
import com.nuvio.app.features.details.formatMetaReleaseLineForDetails
import com.nuvio.app.features.details.mainSeriesStats
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_AUDIENCE
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_IMDB
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_LETTERBOXD
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_METACRITIC
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_MAL
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_TMDB
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_TOMATOES
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_TRAKT
import coil3.compose.AsyncImage
import nuvio.composeapp.generated.resources.*
import nuvio.composeapp.generated.resources.rating_audience_score
import nuvio.composeapp.generated.resources.rating_imdb
import nuvio.composeapp.generated.resources.rating_letterboxd
import nuvio.composeapp.generated.resources.rating_metacritic
import nuvio.composeapp.generated.resources.rating_rotten_tomatoes
import nuvio.composeapp.generated.resources.rating_tmdb
import nuvio.composeapp.generated.resources.rating_trakt
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.runBlocking
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun DetailMetaInfo(
    meta: MetaDetails,
    episodeImdbRatings: Map<Pair<Int, Int>, Double> = emptyMap(),
    episodeTmdbRatings: Map<Pair<Int, Int>, Double> = emptyMap(),
    modifier: Modifier = Modifier,
    horizontalScrollPadding: Dp = 0.dp,
) {
    var showRatings by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val releaseLine = formatMetaReleaseLineForDetails(meta)
        val runtimeText = formatRuntimeForDisplay(meta.runtime)
        val mainSeasonStats = remember(meta.videos) {
            meta.mainSeriesStats()
        }
        val seriesCountText = mainSeasonStats?.let { stats ->
            if (stats.seasonCount == 1) {
                stringResource(Res.string.details_series_counts_one_season, stats.episodeCount)
            } else {
                stringResource(Res.string.details_series_counts, stats.seasonCount, stats.episodeCount)
            }
        }
        val ageBadge = meta.ageRating?.trim()?.takeIf { it.isNotBlank() }
        val hasMdbImdbRating = meta.externalRatings.any { it.source == PROVIDER_IMDB }
        val validImdbRating = meta.imdbRating
            ?.takeIf { raw -> raw.toDoubleOrNull()?.let { it > 0.0 } == true }
        val hasMetaRow = releaseLine != null ||
            runtimeText != null ||
            seriesCountText != null ||
            ageBadge != null ||
            (validImdbRating != null && !hasMdbImdbRating)
        if (hasMetaRow) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                releaseLine?.let { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                    )
                }
                runtimeText?.let { rt ->
                    Text(
                        text = rt,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                    )
                }
                seriesCountText?.let { counts ->
                    Text(
                        text = counts,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                ageBadge?.let { badge ->
                    DetailHeroMetaBadge(text = badge)
                }
                if (validImdbRating != null && !hasMdbImdbRating) {
                    val imdbTextStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ImdbRatingSourceLabel(
                            storeTextStyle = imdbTextStyle,
                            storeTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = validImdbRating,
                            style = imdbTextStyle,
                            color = ImdbYellow,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = meta.externalRatings.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            DetailRatingsRow(
                ratings = meta.externalRatings,
                horizontalScrollPadding = horizontalScrollPadding,
                onClick = { showRatings = true },
            )
        }

        if (meta.director.isNotEmpty()) {
            MetaLabelValueRow(
                label = stringResource(Res.string.details_director),
                value = meta.director.joinToString(", "),
            )
        }

        if (meta.writer.isNotEmpty()) {
            MetaLabelValueRow(
                label = stringResource(Res.string.details_writer),
                value = meta.writer.joinToString(", "),
            )
        }

        if (!meta.description.isNullOrBlank()) {
            var expanded by remember { mutableStateOf(false) }
            var canExpand by remember(meta.description) { mutableStateOf(false) }
            val overviewScrollState = rememberScrollState()
            Column(
                modifier = Modifier.animateContentSize(),
            ) {
                Text(
                    text = meta.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp,
                    onTextLayout = { result ->
                        if (!expanded) {
                            canExpand = result.hasVisualOverflow
                        }
                    },
                    modifier = if (expanded) {
                        Modifier
                            .heightIn(max = 220.dp)
                            .verticalScroll(overviewScrollState)
                    } else {
                        Modifier
                    },
                )
                if (canExpand) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (expanded) {
                            stringResource(Res.string.details_show_less)
                        } else {
                            stringResource(Res.string.details_show_more)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { expanded = !expanded },
                    )
                }
            }
        }
    }

    if (showRatings) {
        DetailRatingsSheet(
            meta = meta,
            episodeImdbRatings = episodeImdbRatings,
            episodeTmdbRatings = episodeTmdbRatings,
            onDismiss = { showRatings = false },
        )
    }
}

@Composable
private fun DetailRatingsRow(
    ratings: List<MetaExternalRating>,
    horizontalScrollPadding: Dp,
    onClick: () -> Unit,
) {
    val orderedRatings = remember(ratings) {
        val bySource = ratings.associateBy { it.source }
        ratingVisuals.mapNotNull { visuals ->
            bySource[visuals.source]?.let { rating -> visuals to rating }
        }
    }

    if (orderedRatings.isEmpty()) return

    Row(
        modifier = Modifier
            .nuvioHorizontalScrollBleed(horizontalScrollPadding)
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = horizontalScrollPadding)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        orderedRatings.forEach { (visuals, rating) ->
            val ratingTextStyle = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (visuals.source == PROVIDER_IMDB && !AppFeaturePolicy.imdbRatingLogoEnabled) {
                    ImdbRatingSourceLabel(
                        storeTextStyle = ratingTextStyle,
                        storeTextColor = visuals.valueColor,
                    )
                } else {
                    Image(
                        painter = painterResource(visuals.logo),
                        contentDescription = visuals.displayName,
                        modifier = Modifier.size(width = visuals.logoWidth, height = 16.dp),
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = visuals.format(rating.value),
                    style = ratingTextStyle,
                    color = visuals.valueColor,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DetailRatingsSheet(
    meta: MetaDetails,
    episodeImdbRatings: Map<Pair<Int, Int>, Double>,
    episodeTmdbRatings: Map<Pair<Int, Int>, Double>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val orderedRatings = remember(meta.externalRatings) {
        val bySource = meta.externalRatings.associateBy { it.source }
        ratingVisuals.mapNotNull { visuals -> bySource[visuals.source]?.let { visuals to it } }
    }
    val episodes = remember(meta.videos) {
        meta.videos.filter { (it.season ?: 0) > 0 && (it.episode ?: 0) > 0 }
            .sortedWith(compareBy({ it.season }, { it.episode }))
    }
    val validEpisodeKeys = remember(episodes) {
        episodes.map { it.season!! to it.episode!! }.toSet()
    }
    // Only show ratings for episodes that exist in the current series metadata.
    val regularImdbRatings = remember(episodeImdbRatings, validEpisodeKeys) {
        episodeImdbRatings.filterKeys(validEpisodeKeys::contains)
    }
    val regularTmdbRatings = remember(episodeTmdbRatings, validEpisodeKeys) {
        episodeTmdbRatings.filterKeys(validEpisodeKeys::contains)
    }
    val availableSources = remember(regularImdbRatings, regularTmdbRatings) {
        ratingVisuals.filter { visuals ->
            when (visuals.source) {
                PROVIDER_IMDB -> regularImdbRatings.isNotEmpty()
                PROVIDER_TMDB -> regularTmdbRatings.isNotEmpty()
                else -> false
            }
        }
    }
    var selectedSource by remember(meta.id, availableSources) {
        mutableStateOf(availableSources.firstOrNull { it.source == PROVIDER_IMDB } ?: availableSources.firstOrNull())
    }
    val selectedRating = orderedRatings.firstOrNull { it.first.source == selectedSource?.source }?.second
    val selectedEpisodeRatings = when (selectedSource?.source) {
        PROVIDER_IMDB -> regularImdbRatings
        PROVIDER_TMDB -> regularTmdbRatings
        else -> emptyMap()
    }
    val seasons = remember(episodes) { episodes.mapNotNull { it.season }.distinct().sorted() }
    val episodeNumbers = remember(episodes) { episodes.mapNotNull { it.episode }.distinct().sorted() }

    NuvioModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RatingSheetHeader(meta = meta)

            if (availableSources.isNotEmpty()) {
                Text(text = "Rating source", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    availableSources.forEach { source ->
                        val isSelected = source.source == selectedSource?.source
                        Surface(
                            modifier = Modifier.clickable { selectedSource = source },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                        ) {
                            Text(
                                text = source.displayName,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            selectedRating?.let { rating ->
                Text(
                    text = "${selectedSource?.displayName}: ${selectedSource?.format?.invoke(rating.value)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = selectedSource?.valueColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            RatingScale()

            Text(text = "Episode ratings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (selectedEpisodeRatings.isNotEmpty()) {
                EpisodeRatingsMatrix(
                    seasons = seasons,
                    episodeNumbers = episodeNumbers,
                    ratings = selectedEpisodeRatings,
                )
            } else {
                Text(
                    text = "Episode ratings are not available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RatingSheetHeader(meta: MetaDetails) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        meta.poster?.takeIf { it.isNotBlank() }?.let { poster ->
            AsyncImage(
                model = poster,
                contentDescription = null,
                modifier = Modifier
                    .size(width = 74.dp, height = 108.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = meta.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            formatMetaReleaseLineForDetails(meta)?.let {
                Text(text = it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            meta.mainSeriesStats()?.let { stats ->
                Text(
                    text = "${stats.seasonCount} seasons - ${stats.episodeCount} episodes",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun RatingScale() {
    Text(text = "Rating scale", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RatingScaleItem("Awesome (9.0+)", EpisodeRatingAwesome)
            RatingScaleItem("Good (7.5-7.9)", EpisodeRatingGood)
            RatingScaleItem("Bad (6.0-6.9)", EpisodeRatingBad)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RatingScaleItem("Great (8.0-8.9)", EpisodeRatingGreat)
            RatingScaleItem("Regular (7.0-7.4)", EpisodeRatingRegular)
            RatingScaleItem("Garbage (<6.0)", EpisodeRatingGarbage)
        }
    }
}

@Composable
private fun RatingScaleItem(label: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(14.dp), shape = RoundedCornerShape(4.dp), color = color) {}
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EpisodeRatingsMatrix(
    seasons: List<Int>,
    episodeNumbers: List<Int>,
    ratings: Map<Pair<Int, Int>, Double>,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(text = "EP", modifier = Modifier.height(38.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            episodeNumbers.forEach { episode ->
                Box(modifier = Modifier.height(40.dp), contentAlignment = Alignment.CenterEnd) {
                    Text(text = "E$episode", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            seasons.forEach { season ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(width = 52.dp, height = 38.dp), contentAlignment = Alignment.Center) {
                        Text(text = "S$season", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    episodeNumbers.forEach { episode ->
                        EpisodeRatingCell(rating = ratings[season to episode])
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeRatingCell(rating: Double?) {
    Box(modifier = Modifier.size(width = 52.dp, height = 40.dp), contentAlignment = Alignment.Center) {
        if (rating == null) {
            Text(text = "-", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Surface(shape = RoundedCornerShape(6.dp), color = episodeRatingColor(rating)) {
                Text(
                    text = formatOneDecimal(rating),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun episodeRatingColor(rating: Double): Color = when {
    rating >= 9.0 -> EpisodeRatingAwesome
    rating >= 8.0 -> EpisodeRatingGreat
    rating >= 7.5 -> EpisodeRatingGood
    rating >= 7.0 -> EpisodeRatingRegular
    rating >= 6.0 -> EpisodeRatingBad
    else -> EpisodeRatingGarbage
}

private val EpisodeRatingAwesome = Color(0xFF00CFA8)
private val EpisodeRatingGreat = Color(0xFF16D98A)
private val EpisodeRatingGood = Color(0xFFF5C518)
private val EpisodeRatingRegular = Color(0xFFFF7A00)
private val EpisodeRatingBad = Color(0xFFFF1744)
private val EpisodeRatingGarbage = Color(0xFF9C4DCC)

@Composable
private fun ImdbRatingSourceLabel(
    storeTextStyle: TextStyle,
    storeTextColor: Color,
) {
    if (AppFeaturePolicy.imdbRatingLogoEnabled) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = ImdbYellow,
        ) {
            Text(
                text = stringResource(Res.string.source_imdb),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.sp,
                ),
                color = ImdbBlack,
            )
        }
    } else {
        Text(
            text = stringResource(Res.string.source_imdb),
            style = storeTextStyle,
            color = storeTextColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun MetaLabelValueRow(
    label: String,
    value: String,
) {
    Row {
        Text(
            text = "$label:  ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DetailHeroMetaBadge(
    text: String,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Box(
        modifier = Modifier
            .border(
                border = BorderStroke(1.dp, contentColor.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val ImdbYellow = Color(0xFFF5C518)
private val ImdbBlack = Color(0xFF000000)

private data class RatingVisuals(
    val source: String,
    val displayName: String,
    val logo: DrawableResource,
    val logoWidth: androidx.compose.ui.unit.Dp,
    val valueColor: Color,
    val format: (Double) -> String,
)

private val ratingVisuals = listOf(
    RatingVisuals(
        source = PROVIDER_IMDB,
        displayName = "IMDb",
        logo = Res.drawable.rating_imdb,
        logoWidth = 30.dp,
        valueColor = Color(0xFFF5C518),
        format = ::formatOneDecimal,
    ),
    RatingVisuals(
        source = PROVIDER_TMDB,
        displayName = "TMDB",
        logo = Res.drawable.rating_tmdb,
        logoWidth = 16.dp,
        valueColor = Color(0xFF01B4E4),
        format = ::formatWhole,
    ),
    RatingVisuals(
        source = PROVIDER_TRAKT,
        displayName = "Trakt",
        logo = Res.drawable.rating_trakt,
        logoWidth = 16.dp,
        valueColor = Color(0xFFED1C24),
        format = ::formatWhole,
    ),
    RatingVisuals(
        source = PROVIDER_LETTERBOXD,
        displayName = "Letterboxd",
        logo = Res.drawable.rating_letterboxd,
        logoWidth = 16.dp,
        valueColor = Color(0xFF00E054),
        format = ::formatOneDecimal,
    ),
    RatingVisuals(
        source = PROVIDER_MAL,
        displayName = "MyAnimeList",
        logo = Res.drawable.rating_mal,
        logoWidth = 16.dp,
        valueColor = Color(0xFF2E51A2),
        format = ::formatOneDecimal,
    ),
    RatingVisuals(
        source = PROVIDER_TOMATOES,
        displayName = "Rotten Tomatoes",
        logo = Res.drawable.rating_rotten_tomatoes,
        logoWidth = 16.dp,
        valueColor = Color(0xFFFA320A),
        format = ::formatPercent,
    ),
    RatingVisuals(
        source = PROVIDER_AUDIENCE,
        displayName = runBlocking { getString(Res.string.rating_audience_score) },
        logo = Res.drawable.rating_audience_score,
        logoWidth = 16.dp,
        valueColor = Color(0xFFFA320A),
        format = ::formatPercent,
    ),
    RatingVisuals(
        source = PROVIDER_METACRITIC,
        displayName = "Metacritic",
        logo = Res.drawable.rating_metacritic,
        logoWidth = 16.dp,
        valueColor = Color(0xFFFFCC33),
        format = ::formatWhole,
    ),
)

private fun formatOneDecimal(value: Double): String {
    val rounded = (value * 10.0).roundToInt()
    val whole = rounded / 10
    val decimal = (rounded % 10).absoluteValue
    return "$whole.$decimal"
}

private fun formatWhole(value: Double): String = value.roundToInt().toString()

private fun formatPercent(value: Double): String = "${value.roundToInt()}%"
