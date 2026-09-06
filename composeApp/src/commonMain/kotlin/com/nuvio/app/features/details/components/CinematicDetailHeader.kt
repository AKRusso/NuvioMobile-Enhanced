package com.nuvio.app.features.details.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.heroStretchHeight
import com.nuvio.app.core.ui.heroStretchZoom
import com.nuvio.app.core.ui.nuvioKeyboardFocusIndicator
import com.nuvio.app.features.details.MetaCompany
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.formatMetaReleaseLineForDetails
import com.nuvio.app.features.details.formatRuntimeForDisplay
import com.nuvio.app.features.details.mainSeriesStats
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_IMDB
import com.nuvio.app.features.settings.CinematicHeaderContentMode
import com.nuvio.app.features.tmdb.TmdbWatchProviderAvailability
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_play
import nuvio.composeapp.generated.resources.compose_action_pause
import nuvio.composeapp.generated.resources.details_total_episodes
import nuvio.composeapp.generated.resources.details_total_seasons
import nuvio.composeapp.generated.resources.home_hero_preview_mute
import nuvio.composeapp.generated.resources.home_hero_preview_unmute
import nuvio.composeapp.generated.resources.nuvio_enhanced_cinematic_header_content_hidden
import nuvio.composeapp.generated.resources.nuvio_enhanced_cinematic_header_content_productions
import nuvio.composeapp.generated.resources.nuvio_enhanced_cinematic_header_content_where_to_watch
import nuvio.composeapp.generated.resources.details_where_to_watch_attribution
import nuvio.composeapp.generated.resources.details_where_to_watch_tmdb_required
import nuvio.composeapp.generated.resources.nuvio_enhanced_cinematic_header_content_title
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class DetailHeaderMetadataData(
    val ageRating: String?,
    val rawImdbRating: String?,
)

internal fun detailHeaderMetadataData(meta: MetaDetails): DetailHeaderMetadataData {
    val hasExternalImdbRating = meta.externalRatings.any {
        it.source.equals(PROVIDER_IMDB, ignoreCase = true)
    }
    val rawImdbRating = meta.imdbRating
        ?.trim()
        ?.takeIf { value -> value.toDoubleOrNull()?.let { it > 0.0 } == true }
        ?.takeUnless { hasExternalImdbRating }

    return DetailHeaderMetadataData(
        ageRating = meta.ageRating?.trim()?.takeIf { it.isNotBlank() },
        rawImdbRating = rawImdbRating,
    )
}

internal data class CinematicDetailHeaderData(
    val isSeriesLike: Boolean,
    val brands: List<MetaCompany>,
    val brandKind: String,
    val releaseLine: String?,
    val runtime: String?,
    val seasonCount: Int?,
    val episodeCount: Int?,
    val ageRating: String?,
    val imdbRating: String?,
)

internal fun cinematicTrailerPlayWhenReady(
    manualOverride: Boolean?,
    autoPlay: Boolean,
    playbackAllowed: Boolean,
): Boolean = (manualOverride ?: autoPlay) && playbackAllowed

internal fun detailContrastRatio(first: Color, second: Color): Float {
    val lighter = maxOf(first.luminance(), second.luminance())
    val darker = minOf(first.luminance(), second.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

internal fun readableDetailContentColor(
    backgroundColor: Color,
    lightColor: Color = Color(0xFFF5F7F8),
    darkColor: Color = Color(0xFF111111),
): Color = if (
    detailContrastRatio(lightColor, backgroundColor) >= detailContrastRatio(darkColor, backgroundColor)
) {
    lightColor
} else {
    darkColor
}

internal fun shouldAdaptBrandLogoColor(
    logoColor: Color,
    backgroundColor: Color,
    hasOpaqueBackground: Boolean = false,
): Boolean = !hasOpaqueBackground && detailContrastRatio(logoColor, backgroundColor) < 3f

internal fun shouldShowCinematicHeaderSettings(contentMode: CinematicHeaderContentMode): Boolean =
    contentMode != CinematicHeaderContentMode.Hidden

internal data class CinematicLogoLayoutMetrics(
    val itemWidthDp: Float,
    val itemHeightDp: Float,
    val spacingDp: Float,
    val scrollable: Boolean,
)

internal fun cinematicLogoLayoutMetrics(
    count: Int,
    availableWidthDp: Float,
    isTablet: Boolean,
    squareItems: Boolean,
): CinematicLogoLayoutMetrics {
    val safeCount = count.coerceAtLeast(1)
    val spacing = if (isTablet) 10f else 8f
    if (squareItems) {
        val size = when {
            isTablet -> 46f
            safeCount > 6 -> 36f
            else -> 40f
        }
        return CinematicLogoLayoutMetrics(size, size, spacing, safeCount > 4)
    }

    val regularWidth = if (isTablet) 142f else 120f
    val regularHeight = if (isTablet) 46f else 40f
    if (safeCount <= 2) {
        return CinematicLogoLayoutMetrics(
            itemWidthDp = regularWidth,
            itemHeightDp = regularHeight,
            spacingDp = spacing,
            scrollable = regularWidth * safeCount + spacing * (safeCount - 1) > availableWidthDp,
        )
    }

    val fittedWidth = (availableWidthDp - spacing * (safeCount - 1)) / safeCount
    val scrollable = safeCount > 4
    return CinematicLogoLayoutMetrics(
        itemWidthDp = if (scrollable) {
            if (isTablet) 104f else 72f
        } else {
            minOf(regularWidth, fittedWidth).coerceAtLeast(if (isTablet) 64f else 48f)
        },
        itemHeightDp = if (isTablet) 42f else 36f,
        spacingDp = spacing,
        scrollable = scrollable,
    )
}

internal data class CinematicLogoAnalysis(
    val sourceX: Int,
    val sourceY: Int,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val averageVisibleColor: Color,
    val opaqueCoverage: Float,
) {
    val hasOpaqueBackground: Boolean
        get() = opaqueCoverage >= 0.82f
}

internal fun analyzeCinematicLogoPixels(
    width: Int,
    height: Int,
    pixelAt: (Int, Int) -> Color,
): CinematicLogoAnalysis? {
    if (width <= 0 || height <= 0) return null
    var minX = width
    var minY = height
    var maxX = -1
    var maxY = -1
    var visiblePixels = 0
    var totalWeight = 0f
    var red = 0f
    var green = 0f
    var blue = 0f

    for (y in 0 until height) {
        for (x in 0 until width) {
            val color = pixelAt(x, y)
            if (color.alpha < 0.08f) continue
            minX = minOf(minX, x)
            minY = minOf(minY, y)
            maxX = maxOf(maxX, x)
            maxY = maxOf(maxY, y)
            visiblePixels += 1
            totalWeight += color.alpha
            red += color.red * color.alpha
            green += color.green * color.alpha
            blue += color.blue * color.alpha
        }
    }
    if (visiblePixels == 0 || totalWeight <= 0f) return null

    val horizontalPadding = maxOf(1, width / 100)
    val verticalPadding = maxOf(1, height / 100)
    val paddedMinX = (minX - horizontalPadding).coerceAtLeast(0)
    val paddedMinY = (minY - verticalPadding).coerceAtLeast(0)
    val paddedMaxX = (maxX + horizontalPadding).coerceAtMost(width - 1)
    val paddedMaxY = (maxY + verticalPadding).coerceAtMost(height - 1)
    return CinematicLogoAnalysis(
        sourceX = paddedMinX,
        sourceY = paddedMinY,
        sourceWidth = paddedMaxX - paddedMinX + 1,
        sourceHeight = paddedMaxY - paddedMinY + 1,
        averageVisibleColor = Color(red / totalWeight, green / totalWeight, blue / totalWeight),
        opaqueCoverage = visiblePixels.toFloat() / (width * height).toFloat(),
    )
}

internal fun cinematicDetailHeaderData(meta: MetaDetails): CinematicDetailHeaderData {
    val isSeriesLike = meta.type.lowercase() in setOf("series", "show", "tv", "tvshow") ||
        meta.videos.any { it.season != null || it.episode != null }
    val preferredBrands = if (isSeriesLike) {
        meta.networks.ifEmpty { meta.productionCompanies }
    } else {
        meta.productionCompanies.ifEmpty { meta.networks }
    }
    val brands = preferredBrands
        .filter { it.name.isNotBlank() }
        .distinctBy { it.name.trim().lowercase() }
        .sortedByDescending { !it.logo.isNullOrBlank() }
    val stats = meta.mainSeriesStats().takeIf { isSeriesLike }
    val semanticMetadata = detailHeaderMetadataData(meta)

    return CinematicDetailHeaderData(
        isSeriesLike = isSeriesLike,
        brands = brands,
        brandKind = if (isSeriesLike && meta.networks.isNotEmpty()) "network" else "company",
        releaseLine = formatMetaReleaseLineForDetails(meta),
        runtime = meta.runtime?.trim()?.takeIf { it.isNotBlank() }.takeUnless { isSeriesLike },
        seasonCount = if (isSeriesLike) {
            meta.videos.mapNotNull { it.season?.takeIf { season -> season > 0 } }.distinct().size.takeIf { it > 0 }
        } else {
            null
        },
        episodeCount = stats?.episodeCount?.takeIf { it > 0 },
        ageRating = semanticMetadata.ageRating,
        imdbRating = semanticMetadata.rawImdbRating,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CinematicDetailHeader(
    meta: MetaDetails,
    isTablet: Boolean,
    contentMaxWidth: Dp,
    scrollOffsetProvider: () -> Float,
    stretchPx: () -> Float,
    onHeightChanged: (Int) -> Unit,
    onBackdropLoaded: (Painter, ImageBitmap?) -> Unit,
    onCompanyClick: ((MetaCompany, String) -> Unit)?,
    trailerSourceUrl: String? = null,
    trailerSourceAudioUrl: String? = null,
    trailerAutoPlay: Boolean = false,
    trailerPlaybackAllowed: Boolean = true,
    trailerMuted: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.background,
    effectiveBackgroundColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    topChromeColor: Color = Color.Black,
    contentMode: CinematicHeaderContentMode = CinematicHeaderContentMode.Productions,
    watchProviderAvailability: TmdbWatchProviderAvailability? = null,
    tmdbAvailable: Boolean = false,
    onContentModeChange: (CinematicHeaderContentMode) -> Unit = {},
    onTrailerMuteToggle: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val data = remember(meta) { cinematicDetailHeaderData(meta) }
    val seasonCountLabel = data.seasonCount?.let {
        stringResource(Res.string.details_total_seasons, it)
    }
    val episodeCountLabel = data.episodeCount?.let {
        stringResource(Res.string.details_total_episodes, it)
    }
    val runtimeLabel = formatRuntimeForDisplay(data.runtime)
    val metadata = buildList {
        data.releaseLine?.let(::add)
        if (data.isSeriesLike) {
            seasonCountLabel?.let(::add)
            episodeCountLabel?.let(::add)
        } else {
            runtimeLabel?.let(::add)
        }
    }
    var trailerReady by remember(trailerSourceUrl) { mutableStateOf(false) }
    var trailerFailed by remember(trailerSourceUrl) { mutableStateOf(false) }
    var manualPlaybackOverride by remember(trailerSourceUrl) { mutableStateOf<Boolean?>(null) }
    var playbackSnapshot by remember(trailerSourceUrl) { mutableStateOf(HeroTrailerPlaybackSnapshot()) }
    var seekRequestId by remember(trailerSourceUrl) { mutableIntStateOf(0) }
    var seekRequest by remember(trailerSourceUrl) { mutableStateOf<HeroTrailerSeekRequest?>(null) }
    var scrubProgress by remember(trailerSourceUrl) { mutableStateOf<Float?>(null) }
    var centerControlVisible by remember(trailerSourceUrl) { mutableStateOf(true) }
    var controlInteractionId by remember(trailerSourceUrl) { mutableIntStateOf(0) }
    val wantsPlayback = manualPlaybackOverride ?: trailerAutoPlay
    val playWhenReady = cinematicTrailerPlayWhenReady(
        manualOverride = manualPlaybackOverride,
        autoPlay = trailerAutoPlay,
        playbackAllowed = trailerPlaybackAllowed,
    )
    val centerControlAlpha by animateFloatAsState(
        targetValue = if (centerControlVisible) 1f else 0f,
        animationSpec = tween(durationMillis = if (centerControlVisible) 140 else 180),
        label = "cinematic_trailer_center_control_alpha",
    )
    val displayedProgress = scrubProgress ?: if (playbackSnapshot.durationMs > 0L) {
        (playbackSnapshot.positionMs.toFloat() / playbackSnapshot.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val playLabel = stringResource(Res.string.action_play)
    val pauseLabel = stringResource(Res.string.compose_action_pause)
    val muteLabel = stringResource(Res.string.home_hero_preview_mute)
    val unmuteLabel = stringResource(Res.string.home_hero_preview_unmute)
    val contentSettingsLabel = stringResource(Res.string.nuvio_enhanced_cinematic_header_content_title)
    val productionModeLabel = stringResource(Res.string.nuvio_enhanced_cinematic_header_content_productions)
    val whereToWatchModeLabel = stringResource(Res.string.nuvio_enhanced_cinematic_header_content_where_to_watch)
    val hiddenModeLabel = stringResource(Res.string.nuvio_enhanced_cinematic_header_content_hidden)
    val uriHandler = LocalUriHandler.current
    var contentMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(trailerSourceUrl) {
        trailerReady = false
        trailerFailed = false
        centerControlVisible = true
    }
    LaunchedEffect(centerControlVisible, trailerReady, controlInteractionId) {
        if (centerControlVisible && trailerReady) {
            delay(1_500L)
            centerControlVisible = false
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .onSizeChanged { onHeightChanged(it.height) },
    ) {
        val imageHeight = if (isTablet) {
            (maxWidth * 0.48f).coerceIn(320.dp, 560.dp)
        } else {
            (maxWidth * 0.5625f).coerceIn(210.dp, 430.dp)
        }
        val horizontalPadding = if (isTablet) 32.dp else 18.dp
        val topChromeHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp

        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topChromeHeight)
                    .background(topChromeColor),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heroStretchHeight(imageHeight, stretchPx)
                    .graphicsLayer { clip = true },
            ) {
                val imageUrl = meta.background ?: meta.poster
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = meta.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .heroStretchZoom(stretchPx)
                            .graphicsLayer {
                                translationY = scrollOffsetProvider() * 0.22f
                                scaleX = 1.03f
                                scaleY = 1.03f
                            },
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                        onSuccess = { state ->
                            onBackdropLoaded(
                                state.painter,
                                loadedBackdropImageBitmap(state.result),
                            )
                        },
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                    )
                }
                if (trailerSourceUrl != null && !trailerFailed) {
                    HeroTrailerPlayerSurface(
                        sourceUrl = trailerSourceUrl,
                        sourceAudioUrl = trailerSourceAudioUrl,
                        playWhenReady = playWhenReady,
                        muted = trailerMuted,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = if (trailerReady) 1f else 0f },
                        onReady = { trailerReady = true },
                        onEnded = {
                            manualPlaybackOverride = false
                            centerControlVisible = true
                        },
                        onError = {
                            trailerReady = false
                            trailerFailed = true
                        },
                        seekRequest = seekRequest,
                        onPlaybackStateChanged = { playbackSnapshot = it },
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)),
                            ),
                        ),
                )
                if (trailerSourceUrl != null && !trailerFailed) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                enabled = trailerReady,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                centerControlVisible = true
                                controlInteractionId += 1
                            },
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(if (isTablet) 46.dp else 42.dp)
                            .nuvioKeyboardFocusIndicator(CircleShape, enabled = trailerReady)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.58f))
                            .clickable(enabled = trailerReady) {
                                centerControlVisible = true
                                controlInteractionId += 1
                                onTrailerMuteToggle()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (trailerMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = if (trailerMuted) unmuteLabel else muteLabel,
                            tint = Color.White,
                            modifier = Modifier.size(if (isTablet) 24.dp else 22.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(if (isTablet) 68.dp else 58.dp)
                            .graphicsLayer { alpha = centerControlAlpha }
                            .nuvioKeyboardFocusIndicator(CircleShape, enabled = trailerReady && centerControlVisible)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = if (trailerReady) 0.62f else 0.38f))
                            .clickable(enabled = trailerReady && centerControlVisible) {
                                centerControlVisible = true
                                controlInteractionId += 1
                                if (wantsPlayback) {
                                    manualPlaybackOverride = false
                                } else {
                                    if (
                                        playbackSnapshot.durationMs > 0L &&
                                        playbackSnapshot.positionMs >= playbackSnapshot.durationMs - 500L
                                    ) {
                                        seekRequestId += 1
                                        seekRequest = HeroTrailerSeekRequest(seekRequestId, 0L)
                                    }
                                    manualPlaybackOverride = true
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (wantsPlayback) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (wantsPlayback) pauseLabel else playLabel,
                            tint = Color.White,
                            modifier = Modifier.size(if (isTablet) 40.dp else 34.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Color.White.copy(alpha = 0.32f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(displayedProgress)
                                .height(2.dp)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                    Slider(
                        value = displayedProgress,
                        onValueChange = { value ->
                            if (playbackSnapshot.durationMs > 0L) {
                                scrubProgress = value
                                centerControlVisible = true
                                controlInteractionId += 1
                            }
                        },
                        onValueChangeFinished = {
                            scrubProgress?.let { progress ->
                                seekRequestId += 1
                                seekRequest = HeroTrailerSeekRequest(
                                    id = seekRequestId,
                                    positionMs = (playbackSnapshot.durationMs * progress).toLong(),
                                )
                                scrubProgress = null
                            }
                        },
                        enabled = trailerReady && playbackSnapshot.durationMs > 0L,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(28.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Transparent,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent,
                            disabledThumbColor = Color.Transparent,
                            disabledActiveTrackColor = Color.Transparent,
                            disabledInactiveTrackColor = Color.Transparent,
                        ),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = if (isTablet) 22.dp else 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = contentMaxWidth),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 42.dp),
                        verticalArrangement = Arrangement.spacedBy(if (isTablet) 12.dp else 9.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        when (contentMode) {
                            CinematicHeaderContentMode.Productions -> if (data.brands.isNotEmpty()) {
                                CinematicLogoRow(
                                    items = data.brands.map { brand ->
                                        CinematicLogoItem(
                                            name = brand.name,
                                            logo = brand.logo,
                                            kind = CinematicLogoKind.Wordmark,
                                            onClick = onCompanyClick
                                                ?.takeIf { brand.tmdbId != null }
                                                ?.let { callback -> { callback(brand, data.brandKind) } },
                                        )
                                    },
                                    isTablet = isTablet,
                                    effectiveBackgroundColor = effectiveBackgroundColor,
                                )
                            }
                            CinematicHeaderContentMode.WhereToWatch -> {
                                val providers = watchProviderAvailability?.providers.orEmpty()
                                if (providers.isNotEmpty()) {
                                    CinematicLogoRow(
                                        items = providers.map { provider ->
                                            CinematicLogoItem(
                                                name = provider.name,
                                                logo = provider.logo,
                                                kind = CinematicLogoKind.ProviderIcon,
                                            )
                                        },
                                        isTablet = isTablet,
                                        effectiveBackgroundColor = effectiveBackgroundColor,
                                    )
                                } else if (!tmdbAvailable) {
                                    Text(
                                        text = stringResource(Res.string.details_where_to_watch_tmdb_required),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = contentColor.copy(alpha = 0.72f),
                                    )
                                }
                                watchProviderAvailability?.attributionUrl?.let { url ->
                                    Text(
                                        text = stringResource(Res.string.details_where_to_watch_attribution),
                                        modifier = Modifier.clickable { runCatching { uriHandler.openUri(url) } },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = contentColor.copy(alpha = 0.68f),
                                        maxLines = 1,
                                    )
                                }
                            }
                            CinematicHeaderContentMode.Hidden -> Unit
                        }

                        Text(
                            text = meta.name,
                            style = if (isTablet) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineLarge,
                            color = contentColor,
                            fontWeight = FontWeight.Bold,
                        )

                        if (metadata.isNotEmpty() || data.ageRating != null || data.imdbRating != null) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                metadata.forEach { value ->
                                    Text(
                                        text = value,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = contentColor.copy(alpha = 0.78f),
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                    )
                                }
                                data.ageRating?.let { ageRating ->
                                    DetailHeroMetaBadge(
                                        text = ageRating,
                                        contentColor = contentColor.copy(alpha = 0.86f),
                                    )
                                }
                                data.imdbRating?.let { rating ->
                                    DetailImdbRating(
                                        rating = rating,
                                        fallbackColor = contentColor.copy(alpha = 0.78f),
                                    )
                                }
                            }
                        }
                    }

                    if (shouldShowCinematicHeaderSettings(contentMode)) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(38.dp)
                                .nuvioKeyboardFocusIndicator(CircleShape)
                                .clip(CircleShape)
                                .clickable { contentMenuExpanded = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = contentSettingsLabel,
                                tint = contentColor.copy(alpha = 0.82f),
                                modifier = Modifier.size(20.dp),
                            )
                            DropdownMenu(
                                expanded = contentMenuExpanded,
                                onDismissRequest = { contentMenuExpanded = false },
                            ) {
                                listOf(
                                    CinematicHeaderContentMode.Productions to productionModeLabel,
                                    CinematicHeaderContentMode.WhereToWatch to whereToWatchModeLabel,
                                    CinematicHeaderContentMode.Hidden to hiddenModeLabel,
                                ).forEach { (mode, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                        },
                                        leadingIcon = if (mode == contentMode) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                        onClick = {
                                            onContentModeChange(mode)
                                            contentMenuExpanded = false
                                        },
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

private enum class CinematicLogoKind {
    Wordmark,
    ProviderIcon,
}

private data class CinematicLogoItem(
    val name: String,
    val logo: String?,
    val kind: CinematicLogoKind,
    val onClick: (() -> Unit)? = null,
)

@Composable
private fun CinematicLogoRow(
    items: List<CinematicLogoItem>,
    isTablet: Boolean,
    effectiveBackgroundColor: Color,
) {
    if (items.isEmpty()) return
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val squareItems = items.all { it.kind == CinematicLogoKind.ProviderIcon }
        val metrics = cinematicLogoLayoutMetrics(
            count = items.size,
            availableWidthDp = maxWidth.value,
            isTablet = isTablet,
            squareItems = squareItems,
        )
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (metrics.scrollable) Modifier.horizontalScroll(scrollState) else Modifier),
            horizontalArrangement = Arrangement.spacedBy(metrics.spacingDp.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                CinematicLogoChip(
                    item = item,
                    effectiveBackgroundColor = effectiveBackgroundColor,
                    modifier = Modifier.size(
                        width = metrics.itemWidthDp.dp,
                        height = metrics.itemHeightDp.dp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CinematicLogoChip(
    item: CinematicLogoItem,
    effectiveBackgroundColor: Color,
    modifier: Modifier,
) {
    var logoLoadError by remember(item.logo) { mutableStateOf(false) }
    var logoBitmap by remember(item.logo) { mutableStateOf<ImageBitmap?>(null) }
    var logoAnalysis by remember(item.logo) { mutableStateOf<CinematicLogoAnalysis?>(null) }
    var logoAnalysisUnavailable by remember(item.logo) { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    val fallbackColor = readableDetailContentColor(effectiveBackgroundColor)
    val adaptLogoColor = item.kind == CinematicLogoKind.Wordmark && (
        logoAnalysisUnavailable || logoAnalysis?.let { analysis ->
            shouldAdaptBrandLogoColor(
                logoColor = analysis.averageVisibleColor,
                backgroundColor = effectiveBackgroundColor,
                hasOpaqueBackground = analysis.hasOpaqueBackground,
            )
        } == true
    )

    LaunchedEffect(item.logo, logoBitmap) {
        logoAnalysis = null
        val bitmap = logoBitmap ?: return@LaunchedEffect
        logoAnalysis = withContext(Dispatchers.Default) {
            runCatching {
                val pixels = bitmap.toPixelMap()
                analyzeCinematicLogoPixels(bitmap.width, bitmap.height) { x, y -> pixels[x, y] }
            }.getOrNull()
        }
        logoAnalysisUnavailable = logoAnalysis == null
    }

    Box(
        modifier = modifier
            .clip(shape)
            .nuvioKeyboardFocusIndicator(shape, enabled = item.onClick != null)
            .then(if (item.onClick != null) Modifier.clickable(onClick = item.onClick) else Modifier)
            .padding(
                horizontal = if (item.kind == CinematicLogoKind.ProviderIcon) 2.dp else 6.dp,
                vertical = if (item.kind == CinematicLogoKind.ProviderIcon) 2.dp else 4.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!item.logo.isNullOrBlank() && !logoLoadError) {
            val bitmap = logoBitmap
            val analysis = logoAnalysis
            if (bitmap != null && analysis != null) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = item.name },
                ) {
                    val source = if (item.kind == CinematicLogoKind.ProviderIcon) {
                        CinematicLogoAnalysis(
                            sourceX = 0,
                            sourceY = 0,
                            sourceWidth = bitmap.width,
                            sourceHeight = bitmap.height,
                            averageVisibleColor = analysis.averageVisibleColor,
                            opaqueCoverage = analysis.opaqueCoverage,
                        )
                    } else {
                        analysis
                    }
                    drawFittedCinematicLogo(
                        image = bitmap,
                        source = source,
                        colorFilter = if (adaptLogoColor) ColorFilter.tint(fallbackColor) else null,
                    )
                }
            } else {
                AsyncImage(
                    model = item.logo,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    colorFilter = if (adaptLogoColor) ColorFilter.tint(fallbackColor) else null,
                    onSuccess = { state ->
                        val loadedBitmap = loadedBackdropImageBitmap(state.result)
                        if (loadedBitmap == null) {
                            logoAnalysisUnavailable = true
                        } else {
                            logoBitmap = loadedBitmap
                        }
                    },
                    onError = { logoLoadError = true },
                )
            }
        } else {
            Text(
                text = item.name.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = fallbackColor,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun DrawScope.drawFittedCinematicLogo(
    image: ImageBitmap,
    source: CinematicLogoAnalysis,
    colorFilter: ColorFilter?,
) {
    val sourceWidth = source.sourceWidth.coerceAtLeast(1)
    val sourceHeight = source.sourceHeight.coerceAtLeast(1)
    val sourceAspect = sourceWidth.toFloat() / sourceHeight.toFloat()
    val destinationAspect = size.width / size.height.coerceAtLeast(1f)
    val destinationWidth: Float
    val destinationHeight: Float
    if (sourceAspect >= destinationAspect) {
        destinationWidth = size.width
        destinationHeight = destinationWidth / sourceAspect
    } else {
        destinationHeight = size.height
        destinationWidth = destinationHeight * sourceAspect
    }
    drawImage(
        image = image,
        srcOffset = IntOffset(source.sourceX, source.sourceY),
        srcSize = IntSize(sourceWidth, sourceHeight),
        dstOffset = IntOffset(
            x = ((size.width - destinationWidth) / 2f).toInt(),
            y = ((size.height - destinationHeight) / 2f).toInt(),
        ),
        dstSize = IntSize(destinationWidth.toInt().coerceAtLeast(1), destinationHeight.toInt().coerceAtLeast(1)),
        colorFilter = colorFilter,
    )
}
