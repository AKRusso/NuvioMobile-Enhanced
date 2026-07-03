package com.nuvio.app.features.details.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaExternalRating
import com.nuvio.app.features.details.formatRuntimeForDisplay
import com.nuvio.app.features.details.formatMetaReleaseLineForDetails
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_AUDIENCE
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_IMDB
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_LETTERBOXD
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_METACRITIC
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_TMDB
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_TOMATOES
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_TRAKT
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
fun DetailMetaInfo(
    meta: MetaDetails,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val releaseLine = formatMetaReleaseLineForDetails(meta)
        val runtimeText = formatRuntimeForDisplay(meta.runtime)
        val ageBadge = meta.ageRating?.trim()?.takeIf { it.isNotBlank() }
        val seasonCountLabel = remember(meta.type, meta.videos) {
            val isSeriesLike = meta.type == "series" || meta.videos.any { it.season != null || it.episode != null }
            if (!isSeriesLike) {
                null
            } else {
                meta.videos
                    .mapNotNull { video -> video.season?.takeIf { it > 0 } }
                    .distinct()
                    .size
                    .takeIf { it > 0 }
                    ?.let { count -> runBlocking { getString(Res.string.details_total_seasons, count) } }
            }
        }
        val totalEpisodesLabel = remember(meta.type, meta.videos) {
            val isSeriesLike = meta.type == "series" || meta.videos.any { it.season != null || it.episode != null }
            if (!isSeriesLike) {
                null
            } else {
                meta.videos
                    .map { video ->
                        when {
                            video.season != null || video.episode != null -> "${video.season ?: -1}:${video.episode ?: -1}"
                            video.id.isNotBlank() -> video.id
                            else -> video.title
                        }
                    }
                    .distinct()
                    .size
                    .takeIf { it > 0 }
                    ?.let { count -> runBlocking { getString(Res.string.details_total_episodes, count) } }
            }
        }
        val hasMdbImdbRating = meta.externalRatings.any { it.source == PROVIDER_IMDB }
        val validImdbRating = meta.imdbRating
            ?.takeIf { raw -> raw.toDoubleOrNull()?.let { it > 0.0 } == true }
        val hasMetaRow = releaseLine != null ||
            seasonCountLabel != null ||
            totalEpisodesLabel != null ||
            runtimeText != null ||
            ageBadge != null ||
            (validImdbRating != null && !hasMdbImdbRating)
        val imdbSourceLabel = stringResource(Res.string.source_imdb)
        val overviewPills = buildList {
            releaseLine?.let(::add)
            seasonCountLabel?.let(::add)
            totalEpisodesLabel?.let(::add)
            runtimeText?.let(::add)
            ageBadge?.let(::add)
            if (validImdbRating != null && !hasMdbImdbRating) {
                add("$imdbSourceLabel $validImdbRating")
            }
        }
        val hasPremiumOverview = hasMetaRow ||
            meta.externalRatings.isNotEmpty() ||
            !meta.description.isNullOrBlank()
        if (hasPremiumOverview) {
            DetailPremiumOverviewCard(
                pills = overviewPills,
                ratings = meta.externalRatings,
                description = meta.description,
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

    }
}

@Composable
private fun DetailPremiumOverviewCard(
    pills: List<String>,
    ratings: List<MetaExternalRating>,
    description: String?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.055f),
                            Color.Transparent,
                        ),
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
                        .size(width = 4.dp, height = 42.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp)),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.details_premium_overview_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(Res.string.details_premium_overview_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                    )
                }
            }

            if (pills.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    pills.forEach { pill ->
                        DetailPremiumOverviewPill(text = pill)
                    }
                }
            }

            AnimatedVisibility(
                visible = ratings.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                DetailRatingsRow(ratings = ratings)
            }

            description?.trim()?.takeIf { it.isNotBlank() }?.let { cleanDescription ->
                DetailPremiumStoryBlock(description = cleanDescription)
            }
        }
    }
}

@Composable
private fun DetailPremiumOverviewPill(
    text: String,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.11f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DetailPremiumStoryBlock(
    description: String,
) {
    var expanded by remember(description) { mutableStateOf(false) }
    var canExpand by remember(description) { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.055f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.details_premium_story_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 23.sp,
                onTextLayout = { result ->
                    if (!expanded) {
                        canExpand = result.hasVisualOverflow
                    }
                },
            )
            if (canExpand) {
                Text(
                    text = if (expanded) {
                        stringResource(Res.string.details_show_less)
                    } else {
                        stringResource(Res.string.details_show_more)
                    },
                    modifier = Modifier
                        .clickable { expanded = !expanded }
                        .padding(vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun DetailRatingsRow(
    ratings: List<MetaExternalRating>,
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
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
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
        source = PROVIDER_TOMATOES,
        displayName = "Rotten Tomatoes",
        logo = Res.drawable.rating_rotten_tomatoes,
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
        source = PROVIDER_AUDIENCE,
        displayName = runBlocking { getString(Res.string.rating_audience_score) },
        logo = Res.drawable.rating_audience_score,
        logoWidth = 16.dp,
        valueColor = Color(0xFFFA320A),
        format = ::formatPercent,
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
