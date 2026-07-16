package com.nuvio.app.features.home.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.build.TrailerPlaybackMode
import com.nuvio.app.core.format.extractReleaseYearForDisplay
import com.nuvio.app.core.ui.rememberAnimatedAccentBrush
import com.nuvio.app.core.ui.rememberAnimatedLineBrush
import com.nuvio.app.core.ui.rememberAnimatedSoftBrush
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.details.MetaExternalRating
import com.nuvio.app.features.details.components.HeroTrailerPlayerSurface
import com.nuvio.app.features.details.formatRuntimeForDisplay
import com.nuvio.app.features.details.mainSeriesStats
import com.nuvio.app.features.details.selectHeroTrailer
import com.nuvio.app.features.details.youtubePlaybackUrl
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.stableKey
import com.nuvio.app.features.mdblist.MdbListMetadataService
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_AUDIENCE
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_IMDB
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_LETTERBOXD
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_METACRITIC
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_TMDB
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_TOMATOES
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_TRAKT
import com.nuvio.app.features.mdblist.MdbListSettingsRepository
import com.nuvio.app.features.settings.NuvioHeroArtworkSource
import com.nuvio.app.features.settings.NuvioHeroDisplayMode
import com.nuvio.app.features.tmdb.TmdbMetadataService
import com.nuvio.app.features.tmdb.TmdbSettingsRepository
import com.nuvio.app.features.trailer.TrailerPlaybackResolver
import com.nuvio.app.features.trailer.TrailerPlaybackSource
import com.nuvio.app.features.watchprogress.CurrentDateProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private const val HERO_BACKGROUND_SCALE = 1.14f
private const val HERO_SCROLL_PARALLAX = 0.3f
private const val HERO_SCROLL_DOWN_SCALE_MULTIPLIER = 0.0001f
private const val HERO_SCROLL_UP_SCALE_MULTIPLIER = 0.002f
private const val HERO_SCROLL_MAX_SCALE = 1.3f
private const val HERO_CINEMATIC_PAN_PX = 34f
private const val HERO_CINEMATIC_SCALE = 0.055f
private const val HERO_SWIPE_THRESHOLD_FRACTION = 0.16f
private const val HERO_SWIPE_VELOCITY_THRESHOLD = 300f
private const val HERO_AUTO_SCROLL_INTERVAL_MS = 5500L
private const val MOBILE_HERO_VIEWPORT_RATIO = 0.82f
private const val MOBILE_HERO_MIN_HEIGHT_DP = 360f
private const val MOBILE_HERO_MAX_HEIGHT_DP = 760f
private const val POSTER_ART_HERO_VIEWPORT_RATIO = 0.80f
private const val POSTER_ART_HERO_MIN_HEIGHT_DP = 520f
private const val POSTER_ART_HERO_MAX_HEIGHT_DP = 690f
private const val STREAMING_SHOWCASE_HERO_VIEWPORT_RATIO = 0.62f
private const val STREAMING_SHOWCASE_HERO_MIN_HEIGHT_DP = 420f
private const val STREAMING_SHOWCASE_HERO_MAX_HEIGHT_DP = 620f
private const val HERO_MDBLIST_ENRICH_TIMEOUT_MS = 5_000L
private const val HERO_TMDB_OVERVIEW_TIMEOUT_MS = 4_000L
private const val HERO_DETAIL_META_TIMEOUT_MS = 7_000L
private const val HERO_REFRESH_SCALE_X_MULTIPLIER = 0.018f
private const val HERO_REFRESH_SCALE_Y_MULTIPLIER = 0.056f
private const val HERO_REFRESH_TRANSLATION_Y_PX = 5f

internal data class HomeHeroLayout(
    val isTablet: Boolean,
    val heroHeight: Dp,
    val contentMaxWidth: Dp,
    val contentWidthFraction: Float,
    val contentHorizontalPadding: Dp,
    val contentVerticalPadding: Dp,
    val bottomFadeHeight: Dp,
    val logoWidthFraction: Float,
)

private enum class HomeHeroVisualStyle {
    Default,
    PosterArt,
    StreamingShowcase,
}

@Composable
internal fun HomeHeroSection(
    items: List<MetaPreview>,
    modifier: Modifier = Modifier,
    viewportHeight: Dp? = null,
    mobileBelowSectionHeightHint: Dp? = null,
    listState: LazyListState? = null,
    autoScrollEnabled: Boolean = true,
    motionPreviewEnabled: Boolean = false,
    heroDisplayMode: NuvioHeroDisplayMode = NuvioHeroDisplayMode.Balanced,
    heroArtworkSource: NuvioHeroArtworkSource = NuvioHeroArtworkSource.Backdrop,
    posterArtHeroEnabled: Boolean = false,
    streamingShowcaseHeroEnabled: Boolean = false,
    streamingShowcaseVideoPreviewEnabled: Boolean = true,
    streamingShowcaseVideoPreviewSoundEnabled: Boolean = true,
    compactMetadata: Boolean = true,
    showRatings: Boolean = true,
    showOverview: Boolean = true,
    metadataRefreshKey: String? = null,
    refreshPullProgress: Float = 0f,
    onItemClick: ((MetaPreview) -> Unit)? = null,
    onPlayClick: ((MetaPreview) -> Unit)? = null,
    onSaveClick: ((MetaPreview) -> Unit)? = null,
    onStreamingShowcaseVideoPreviewSoundChange: ((Boolean) -> Unit)? = null,
    isSaved: (MetaPreview) -> Boolean = { false },
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })
    val coroutineScope = rememberCoroutineScope()
    var isUserInteracting by remember { mutableStateOf(false) }
    val itemKeys = remember(items) { items.joinToString(separator = "|") { it.stableKey() } }
    val detailMetas = remember(itemKeys, metadataRefreshKey) { mutableStateMapOf<String, MetaDetails>() }
    val detailLoadCompleted = remember(itemKeys, metadataRefreshKey) { mutableStateMapOf<String, Boolean>() }
    val heroAutoScrollIntervalMs = if (streamingShowcaseHeroEnabled && streamingShowcaseVideoPreviewEnabled) {
        HERO_AUTO_SCROLL_INTERVAL_MS * 3
    } else {
        HERO_AUTO_SCROLL_INTERVAL_MS
    }

    LaunchedEffect(items.size, autoScrollEnabled, isUserInteracting, heroAutoScrollIntervalMs) {
        if (items.size <= 1 || !autoScrollEnabled) return@LaunchedEffect
        while (isActive) {
            kotlinx.coroutines.delay(heroAutoScrollIntervalMs)
            if (!autoScrollEnabled || isUserInteracting || pagerState.isScrollInProgress) continue
            val nextPage = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .homeHeroPagerGesture(
                pagerState = pagerState,
                itemCount = items.size,
                coroutineScope = coroutineScope,
                onInteractionChanged = { isUserInteracting = it },
            ),
    ) {
        val heroShape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
        val layout = homeHeroLayout(
            maxWidthDp = maxWidth.value,
            viewportHeightDp = viewportHeight?.value,
            mobileBelowSectionHeightHintDp = mobileBelowSectionHeightHint?.value,
        )
        val heroVisualStyle = when {
            streamingShowcaseHeroEnabled -> HomeHeroVisualStyle.StreamingShowcase
            posterArtHeroEnabled -> HomeHeroVisualStyle.PosterArt
            else -> HomeHeroVisualStyle.Default
        }
        val activeHeroHeight = when (heroVisualStyle) {
            HomeHeroVisualStyle.StreamingShowcase -> streamingShowcaseHeroHeight(
                maxWidthDp = maxWidth.value,
                viewportHeightDp = viewportHeight?.value,
                layout = layout,
            )
            HomeHeroVisualStyle.PosterArt -> posterArtHeroHeight(
                maxWidthDp = maxWidth.value,
                viewportHeightDp = viewportHeight?.value,
                layout = layout,
            )
            HomeHeroVisualStyle.Default -> layout.heroHeight
        }
        val density = LocalDensity.current
        val heroHeightPx = with(density) { activeHeroHeight.toPx() }
        val scrollOffsetPx by remember(listState, heroHeightPx) {
            derivedStateOf {
                when {
                    listState == null -> 0f
                    listState.firstVisibleItemIndex > 0 -> heroHeightPx
                    else -> listState.firstVisibleItemScrollOffset.toFloat()
                }
            }
        }
        val heroScrollScale = heroBackgroundScrollScale(scrollOffsetPx)
        val heroScrollTranslationY = heroBackgroundScrollTranslationY(scrollOffsetPx)
        val displayPage by remember(pagerState, items.size) {
            derivedStateOf {
                val targetPage = if (pagerState.isScrollInProgress) {
                    pagerState.targetPage
                } else {
                    pagerState.currentPage
                }
                targetPage.coerceIn(items.indices)
            }
        }
        val currentItem = items[displayPage]
        val currentItemKey = currentItem.stableKey()
        val currentDetailMeta = detailMetas[currentItemKey]
        val currentArtworkSource = when (heroVisualStyle) {
            HomeHeroVisualStyle.StreamingShowcase,
            HomeHeroVisualStyle.PosterArt -> NuvioHeroArtworkSource.Backdrop
            HomeHeroVisualStyle.Default -> heroArtworkSource
        }
        val currentArtworkUrl = currentItem.heroArtworkUrl(
            source = currentArtworkSource,
            detailMeta = currentDetailMeta,
            allowPreviewFallback = true,
        )
        val heroRefreshProgress = homeHeroRefreshEase(refreshPullProgress)
        val backgroundColor = MaterialTheme.colorScheme.background
        val mainOverlayStops = when (heroDisplayMode) {
            NuvioHeroDisplayMode.Cinematic -> arrayOf(
                0f to backgroundColor.copy(alpha = 0.01f),
                0.36f to backgroundColor.copy(alpha = 0.04f),
                0.68f to backgroundColor.copy(alpha = 0.18f),
                1f to backgroundColor.copy(alpha = 0.68f),
            )
            NuvioHeroDisplayMode.Balanced -> arrayOf(
                0f to backgroundColor.copy(alpha = 0.02f),
                0.34f to backgroundColor.copy(alpha = 0.05f),
                0.66f to backgroundColor.copy(alpha = 0.22f),
                1f to backgroundColor.copy(alpha = 0.74f),
            )
            NuvioHeroDisplayMode.InfoRich -> arrayOf(
                0f to backgroundColor.copy(alpha = 0.04f),
                0.34f to backgroundColor.copy(alpha = 0.08f),
                0.64f to backgroundColor.copy(alpha = 0.30f),
                1f to backgroundColor.copy(alpha = 0.82f),
            )
        }
        val bottomOverlayStops = when (heroDisplayMode) {
            NuvioHeroDisplayMode.Cinematic -> arrayOf(
                0f to Color.Transparent,
                0.48f to backgroundColor.copy(alpha = 0.24f),
                1f to backgroundColor.copy(alpha = 0.90f),
            )
            NuvioHeroDisplayMode.Balanced -> arrayOf(
                0f to Color.Transparent,
                0.42f to backgroundColor.copy(alpha = 0.31f),
                1f to backgroundColor.copy(alpha = 0.94f),
            )
            NuvioHeroDisplayMode.InfoRich -> arrayOf(
                0f to Color.Transparent,
                0.40f to backgroundColor.copy(alpha = 0.38f),
                1f to backgroundColor.copy(alpha = 0.97f),
            )
        }
        LaunchedEffect(itemKeys, metadataRefreshKey, displayPage) {
            val prioritizedItems = items
                .withIndex()
                .sortedBy { (index, _) -> abs(index - displayPage) }
                .map { it.value }

            suspend fun loadDetailMeta(item: MetaPreview) {
                val key = item.stableKey()
                if (detailLoadCompleted[key] == true) return
                MetaDetailsRepository.peek(type = item.type, id = item.id)?.let { cachedMeta ->
                    detailMetas[key] = cachedMeta
                }
                val meta = fetchHeroDetailMeta(item) { baseMeta ->
                    detailMetas[key] = baseMeta
                }
                if (meta != null) {
                    detailMetas[key] = meta
                    detailLoadCompleted[key] = true
                } else {
                    detailLoadCompleted.remove(key)
                }
            }

            prioritizedItems.firstOrNull()?.let { item ->
                loadDetailMeta(item)
            }

            prioritizedItems.drop(1).forEach { item ->
                launch {
                    loadDetailMeta(item)
                }
            }
        }
        val cinematicMotion = rememberInfiniteTransition(label = "heroCinematicMotion")
        val cinematicPulse by cinematicMotion.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 8200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "heroCinematicPulse",
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(activeHeroHeight),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(heroShape),
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 0.01f },
                ) {
                    Box(modifier = Modifier.fillMaxSize())
                }

                when (heroVisualStyle) {
                    HomeHeroVisualStyle.PosterArt -> PosterArtHeroPage(
                            item = currentItem,
                            detailMeta = detailMetas[currentItem.stableKey()],
                            artworkUrl = currentArtworkUrl,
                            layout = layout,
                            motionPreviewEnabled = motionPreviewEnabled,
                            cinematicPulse = cinematicPulse,
                            heroScrollScale = heroScrollScale,
                            heroScrollTranslationY = heroScrollTranslationY,
                            heroHeightPx = heroHeightPx,
                            refreshProgress = heroRefreshProgress,
                            showOverviewCue = showOverview,
                            showRatings = showRatings,
                            onItemClick = onItemClick,
                            onPlayClick = onPlayClick,
                            onSaveClick = onSaveClick,
                            isSaved = isSaved(currentItem),
                        )
                    HomeHeroVisualStyle.StreamingShowcase -> StreamingShowcaseHeroPage(
                            item = currentItem,
                            detailMeta = detailMetas[currentItem.stableKey()],
                            artworkUrl = currentArtworkUrl,
                            layout = layout,
                            motionPreviewEnabled = motionPreviewEnabled,
                            cinematicPulse = cinematicPulse,
                            heroScrollScale = heroScrollScale,
                            heroScrollTranslationY = heroScrollTranslationY,
                            heroHeightPx = heroHeightPx,
                            refreshProgress = heroRefreshProgress,
                            videoPreviewEnabled = streamingShowcaseVideoPreviewEnabled,
                            videoPreviewSoundEnabled = streamingShowcaseVideoPreviewSoundEnabled,
                            videoPreviewActive = scrollOffsetPx < heroHeightPx * 0.74f,
                            showOverview = showOverview,
                            showRatings = showRatings,
                            pagerState = pagerState,
                            itemCount = items.size,
                            currentPage = displayPage,
                            coroutineScope = coroutineScope,
                            onItemClick = onItemClick,
                            onPlayClick = onPlayClick,
                            onSaveClick = onSaveClick,
                            onVideoPreviewSoundChange = onStreamingShowcaseVideoPreviewSoundChange,
                            isSaved = isSaved(currentItem),
                        )
                    HomeHeroVisualStyle.Default -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        val motionVisibility = if (motionPreviewEnabled) 1f else 0f
                        val motionPulse = if (motionPreviewEnabled) cinematicPulse else 0.5f
                        AsyncImage(
                            model = currentArtworkUrl,
                            contentDescription = currentItem.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = 1f
                                    translationX =
                                        ((motionPulse - 0.5f) * HERO_CINEMATIC_PAN_PX * motionVisibility)
                                    val cinematicScale =
                                        1f + (HERO_CINEMATIC_SCALE * motionPulse * motionVisibility)
                                    val backgroundScale = HERO_BACKGROUND_SCALE * heroScrollScale * cinematicScale
                                    val refreshScaleX = homeHeroRefreshScaleX(heroRefreshProgress)
                                    val refreshScaleY = homeHeroRefreshScaleY(heroRefreshProgress)
                                    val verticalBleedPx =
                                        ((backgroundScale - 1f).coerceAtLeast(0f) * heroHeightPx) / 2f
                                    translationY = heroScrollTranslationY.coerceIn(-verticalBleedPx, verticalBleedPx) +
                                        homeHeroRefreshTranslationY(heroRefreshProgress)
                                    scaleX = backgroundScale * refreshScaleX
                                    scaleY = backgroundScale * refreshScaleY
                                },
                            alignment = if (layout.isTablet) Alignment.TopCenter else Alignment.Center,
                            contentScale = ContentScale.Crop,
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = mainOverlayStops,
                                    ),
                                ),
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(layout.bottomFadeHeight)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = bottomOverlayStops,
                                    ),
                                ),
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(
                                    horizontal = layout.contentHorizontalPadding,
                                    vertical = layout.contentVerticalPadding,
                                ),
                            horizontalAlignment = if (layout.isTablet) {
                                Alignment.Start
                            } else {
                                Alignment.CenterHorizontally
                            },
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(layout.contentWidthFraction)
                                    .widthIn(max = layout.contentMaxWidth),
                                contentAlignment = if (layout.isTablet) Alignment.CenterStart else Alignment.Center,
                            ) {
                                HeroContentBlock(
                                    item = currentItem,
                                    layout = layout,
                                    detailMeta = detailMetas[currentItem.stableKey()],
                                    heroDisplayMode = heroDisplayMode,
                                    compactMetadata = compactMetadata,
                                    showRatings = showRatings,
                                    showOverview = showOverview,
                                    onItemClick = onItemClick,
                                )
                            }

                            if (!layout.isTablet) {
                                Spacer(modifier = Modifier.height(14.dp))
                                HeroCtaButton(
                                    text = stringResource(Res.string.home_view_details),
                                    enabled = onItemClick != null,
                                    onClick = { onItemClick?.invoke(currentItem) },
                                )
                            }

                            if (items.size > 1) {
                                Spacer(modifier = Modifier.height(if (layout.isTablet) 14.dp else 12.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    items.forEachIndexed { index, _ ->
                                        HeroPageIndicator(
                                            activeFraction = if (index == displayPage) 1f else 0f,
                                            onClick = {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(index)
                                                }
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
    }
}

private data class HeroMetaItem(
    val text: String,
    val emphasized: Boolean = false,
)

private data class PosterHeroRatingItem(
    val label: String,
    val value: String,
    val accent: Color,
)

@Composable
fun HomeHeroReservedSpace(
    modifier: Modifier = Modifier,
    viewportHeight: Dp? = null,
    mobileBelowSectionHeightHint: Dp? = null,
    posterArtHeroEnabled: Boolean = false,
    streamingShowcaseHeroEnabled: Boolean = false,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)),
    ) {
        val layout = homeHeroLayout(
            maxWidthDp = maxWidth.value,
            viewportHeightDp = viewportHeight?.value,
            mobileBelowSectionHeightHintDp = mobileBelowSectionHeightHint?.value,
        )
        val heroHeight = when {
            streamingShowcaseHeroEnabled -> streamingShowcaseHeroHeight(
                maxWidthDp = maxWidth.value,
                viewportHeightDp = viewportHeight?.value,
                layout = layout,
            )
            posterArtHeroEnabled -> posterArtHeroHeight(
                maxWidthDp = maxWidth.value,
                viewportHeightDp = viewportHeight?.value,
                layout = layout,
            )
            else -> layout.heroHeight
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight),
        )
    }
}

private fun MetaPreview.heroArtworkUrl(
    source: NuvioHeroArtworkSource,
    detailMeta: MetaDetails?,
    allowPreviewFallback: Boolean = true,
): String? {
    val detailBackdrop = detailMeta?.background?.takeIf(String::isNotBlank)
    val detailPoster = detailMeta?.poster?.takeIf(String::isNotBlank)
    val previewBackdrop = banner?.takeIf(String::isNotBlank)
    val previewPoster = poster?.takeIf(String::isNotBlank)

    return when (source) {
        NuvioHeroArtworkSource.Backdrop ->
            if (allowPreviewFallback) {
                detailBackdrop ?: previewBackdrop ?: detailPoster ?: previewPoster
            } else {
                detailBackdrop ?: detailPoster
            }
        NuvioHeroArtworkSource.Poster ->
            if (allowPreviewFallback) {
                detailPoster ?: previewPoster ?: detailBackdrop ?: previewBackdrop
            } else {
                detailPoster ?: detailBackdrop
            }
    }
}

private fun homeHeroRefreshEase(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    return p * p * (3f - 2f * p)
}

private fun homeHeroRefreshScaleX(easedProgress: Float): Float =
    1f + (HERO_REFRESH_SCALE_X_MULTIPLIER * easedProgress.coerceIn(0f, 1f))

private fun homeHeroRefreshScaleY(easedProgress: Float): Float =
    1f + (HERO_REFRESH_SCALE_Y_MULTIPLIER * easedProgress.coerceIn(0f, 1f))

private fun homeHeroRefreshTranslationY(easedProgress: Float): Float =
    HERO_REFRESH_TRANSLATION_Y_PX * easedProgress.coerceIn(0f, 1f)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StreamingShowcaseHeroPage(
    item: MetaPreview,
    detailMeta: MetaDetails?,
    artworkUrl: String?,
    layout: HomeHeroLayout,
    motionPreviewEnabled: Boolean,
    cinematicPulse: Float,
    heroScrollScale: Float,
    heroScrollTranslationY: Float,
    heroHeightPx: Float,
    refreshProgress: Float,
    videoPreviewEnabled: Boolean,
    videoPreviewSoundEnabled: Boolean,
    videoPreviewActive: Boolean,
    showOverview: Boolean,
    showRatings: Boolean,
    pagerState: PagerState,
    itemCount: Int,
    currentPage: Int,
    coroutineScope: CoroutineScope,
    onItemClick: ((MetaPreview) -> Unit)?,
    onPlayClick: ((MetaPreview) -> Unit)?,
    onSaveClick: ((MetaPreview) -> Unit)?,
    onVideoPreviewSoundChange: ((Boolean) -> Unit)?,
    isSaved: Boolean,
) {
    val motionVisibility = if (motionPreviewEnabled) 1f else 0f
    val motionPulse = if (motionPreviewEnabled) cinematicPulse else 0.5f
    val title = detailMeta?.name?.takeIf { it.isNotBlank() } ?: item.name
    val logoUrl = detailMeta?.logo?.takeIf { it.isNotBlank() } ?: item.logo?.takeIf { it.isNotBlank() }
    var logoLoadError by remember(item.type, item.id, logoUrl) {
        mutableStateOf(false)
    }
    val displayType = detailMeta?.type?.takeIf { it.isNotBlank() } ?: item.type
    val displayGenres = detailMeta?.genres
        .orEmpty()
        .map(String::trim)
        .filter(String::isNotBlank)
        .ifEmpty {
            item.genres
                .map(String::trim)
                .filter(String::isNotBlank)
        }
    val releaseLabel = (detailMeta?.releaseInfo?.takeIf { it.isNotBlank() } ?: item.releaseInfo)
        ?.let(::heroReleaseYearLabel)
    val fallbackImdbRating = (detailMeta?.imdbRating?.takeIf { it.isNotBlank() } ?: item.imdbRating)
        ?.toDoubleOrNull()
        ?.takeIf { it > 0.0 }
    val overview = detailMeta?.description
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: item.description?.trim()?.takeIf { it.isNotBlank() }
    val typeLabel = heroTypeLabel(displayType)
    val genreLabel = displayGenres.firstOrNull()
    val metadataItems = buildList {
        typeLabel.takeIf { it.isNotBlank() }?.let { add(HeroMetaItem(text = it, emphasized = true)) }
    }.distinctBy { it.text }
    val showcaseRatings = remember(showRatings, detailMeta?.externalRatings, fallbackImdbRating, layout.isTablet) {
        if (showRatings) {
            buildStreamingShowcaseRatings(
                externalRatings = detailMeta?.externalRatings.orEmpty(),
                fallbackImdbRating = fallbackImdbRating,
                maxItems = 6,
            )
        } else {
            emptyList()
        }
    }
    val awardLabel = detailMeta?.awards?.trim()?.takeIf { it.isNotBlank() }
    val runtimeLabel = formatRuntimeForDisplay(detailMeta?.runtime)
    val ageRatingLabel = detailMeta?.ageRating?.trim()?.takeIf { it.isNotBlank() }
    val seasonCountLabel = heroSeasonCountLabel(detailMeta)
    val detailsAction = onItemClick ?: onPlayClick
    val videoPreviewSupported = AppFeaturePolicy.heroTrailerPlaybackSupported &&
        AppFeaturePolicy.trailerPlaybackMode == TrailerPlaybackMode.IN_APP
    val heroTrailerCandidate = remember(detailMeta?.trailers) {
        selectHeroTrailer(detailMeta?.trailers.orEmpty())
    }
    var heroTrailerPlaybackSource by remember(item.type, item.id, heroTrailerCandidate?.id) {
        mutableStateOf<TrailerPlaybackSource?>(null)
    }
    var heroTrailerReady by remember(item.type, item.id, heroTrailerCandidate?.id) {
        mutableStateOf(false)
    }
    var heroTrailerFinished by remember(item.type, item.id, heroTrailerCandidate?.id) {
        mutableStateOf(false)
    }
    val shouldResolveHeroTrailer = videoPreviewEnabled && videoPreviewSupported && heroTrailerCandidate != null
    LaunchedEffect(shouldResolveHeroTrailer, heroTrailerCandidate?.id, heroTrailerCandidate?.key) {
        heroTrailerPlaybackSource = null
        heroTrailerReady = false
        heroTrailerFinished = false
        if (!shouldResolveHeroTrailer || heroTrailerCandidate == null) {
            return@LaunchedEffect
        }
        val resolvedSource = runCatching {
            TrailerPlaybackResolver.resolveFromYouTubeUrl(heroTrailerCandidate.youtubePlaybackUrl())
        }.getOrNull()
        if (resolvedSource == null) {
            heroTrailerFinished = true
        } else {
            heroTrailerPlaybackSource = resolvedSource
        }
    }
    val heroTrailerSourceUrl = heroTrailerPlaybackSource
        ?.videoUrl
        ?.takeIf { it.isNotBlank() && shouldResolveHeroTrailer && videoPreviewActive && !heroTrailerFinished }
    val heroTrailerSourceAudioUrl = heroTrailerPlaybackSource
        ?.audioUrl
        ?.takeIf { heroTrailerSourceUrl != null && it.isNotBlank() }
    val trailerAlpha by animateFloatAsState(
        targetValue = if (heroTrailerSourceUrl != null && heroTrailerReady) 1f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "home_showcase_trailer_alpha",
    )
    val density = LocalDensity.current
    val heroHeightDp = with(density) { heroHeightPx.toDp() }
    val landscapeCompact = layout.isTablet && heroHeightDp <= 470.dp
    val ultraCompact = layout.isTablet && heroHeightDp <= 390.dp
    val contentWidthFraction = when {
        landscapeCompact -> 0.62f
        layout.isTablet -> 0.40f
        else -> 1f
    }
    val titleSize = when {
        landscapeCompact && title.length > 30 -> 25.sp
        landscapeCompact && title.length > 20 -> 28.sp
        landscapeCompact -> 31.sp
        layout.isTablet -> 44.sp
        title.length > 30 -> 30.sp
        title.length > 20 -> 34.sp
        else -> 39.sp
    }
    val titleLineHeight = when {
        landscapeCompact && title.length > 30 -> 26.sp
        landscapeCompact && title.length > 20 -> 29.sp
        landscapeCompact -> 32.sp
        layout.isTablet -> 46.sp
        title.length > 30 -> 31.sp
        title.length > 20 -> 35.sp
        else -> 40.sp
    }
    val logoWidthFraction = when {
        landscapeCompact -> 0.52f
        layout.isTablet -> 0.72f
        else -> 0.64f
    }
    val logoMaxWidth = when {
        landscapeCompact -> 280.dp
        layout.isTablet -> 380.dp
        else -> 300.dp
    }
    val logoHeight = when {
        ultraCompact -> 46.dp
        landscapeCompact -> 58.dp
        layout.isTablet -> 104.dp
        else -> 70.dp
    }
    val startPadding = when {
        landscapeCompact -> 44.dp
        layout.isTablet -> 58.dp
        else -> 22.dp
    }
    val endPadding = if (layout.isTablet) 22.dp else 16.dp
    val bottomPadding = when {
        ultraCompact -> 26.dp
        landscapeCompact -> 36.dp
        layout.isTablet -> 86.dp
        else -> 56.dp
    }
    val contentMaxWidth = when {
        landscapeCompact -> 720.dp
        layout.isTablet -> 520.dp
        else -> 430.dp
    }
    val contentSpacing = when {
        ultraCompact -> 6.dp
        landscapeCompact -> 8.dp
        layout.isTablet -> 18.dp
        else -> 13.dp
    }
    val compactControls = !layout.isTablet || landscapeCompact

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = onItemClick != null) {
                onItemClick?.invoke(item)
            },
    ) {
        AsyncImage(
            model = artworkUrl,
            contentDescription = title,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = ((motionPulse - 0.5f) * HERO_CINEMATIC_PAN_PX * motionVisibility)
                    val cinematicScale = 1f + (HERO_CINEMATIC_SCALE * motionPulse * motionVisibility)
                    val backgroundScale = 1.05f * heroScrollScale * cinematicScale
                    val refreshScaleX = homeHeroRefreshScaleX(refreshProgress)
                    val refreshScaleY = homeHeroRefreshScaleY(refreshProgress)
                    val verticalBleedPx = ((backgroundScale - 1f).coerceAtLeast(0f) * heroHeightPx) / 2f
                    translationY = heroScrollTranslationY.coerceIn(-verticalBleedPx, verticalBleedPx) +
                        homeHeroRefreshTranslationY(refreshProgress)
                    scaleX = backgroundScale * refreshScaleX
                    scaleY = backgroundScale * refreshScaleY
                },
            alignment = if (layout.isTablet) Alignment.CenterEnd else Alignment.Center,
            contentScale = ContentScale.Crop,
        )

        if (heroTrailerSourceUrl != null) {
            HeroTrailerPlayerSurface(
                sourceUrl = heroTrailerSourceUrl,
                sourceAudioUrl = heroTrailerSourceAudioUrl,
                playWhenReady = videoPreviewActive,
                muted = !videoPreviewSoundEnabled,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = trailerAlpha
                        scaleX = homeHeroRefreshScaleX(refreshProgress)
                        scaleY = homeHeroRefreshScaleY(refreshProgress)
                        translationY = homeHeroRefreshTranslationY(refreshProgress)
                    },
                onReady = {
                    if (!heroTrailerFinished) {
                        heroTrailerReady = true
                    }
                },
                onEnded = {
                    heroTrailerReady = false
                    heroTrailerFinished = true
                },
                onError = {
                    heroTrailerReady = false
                    heroTrailerFinished = true
                },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.88f),
                            0.26f to Color.Black.copy(alpha = 0.66f),
                            0.56f to Color.Black.copy(alpha = 0.22f),
                            1f to Color.Black.copy(alpha = 0.08f),
                        ),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.18f),
                            0.48f to Color.Transparent,
                            0.80f to Color.Black.copy(alpha = 0.58f),
                            1f to Color.Black.copy(alpha = 0.96f),
                        ),
                    ),
                ),
        )

        if (videoPreviewEnabled && videoPreviewSupported) {
            StreamingShowcaseSoundButton(
                imageVector = if (videoPreviewSoundEnabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                contentDescription = stringResource(
                    if (videoPreviewSoundEnabled) {
                        Res.string.home_hero_preview_mute
                    } else {
                        Res.string.home_hero_preview_unmute
                    },
                ),
                enabled = onVideoPreviewSoundChange != null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = if (layout.isTablet) 30.dp else 48.dp,
                        end = if (layout.isTablet) 34.dp else 22.dp,
                    ),
                onClick = {
                    onVideoPreviewSoundChange?.invoke(!videoPreviewSoundEnabled)
                },
            )
        }

        Column(
            modifier = Modifier
                .align(if (layout.isTablet) Alignment.CenterStart else Alignment.BottomStart)
                .fillMaxWidth(contentWidthFraction)
                .widthIn(max = contentMaxWidth)
                .padding(start = startPadding, end = endPadding, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
        ) {
            StreamingShowcaseMetaRail(items = metadataItems)

            if (logoUrl != null && !logoLoadError) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(logoWidthFraction)
                        .widthIn(max = logoMaxWidth)
                        .height(logoHeight)
                        .clickable(enabled = onItemClick != null) {
                            onItemClick?.invoke(item)
                        },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                scaleX = 1.035f
                                scaleY = 1.04f
                                alpha = 0.34f
                            },
                        alignment = Alignment.CenterStart,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(Color.White),
                    )
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = title,
                        modifier = Modifier.matchParentSize(),
                        alignment = Alignment.CenterStart,
                        contentScale = ContentScale.Fit,
                        onError = { logoLoadError = true },
                    )
                }
            } else {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White.copy(alpha = 0.95f),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    fontSize = titleSize,
                    lineHeight = titleLineHeight,
                    letterSpacing = 0.sp,
                    maxLines = if (layout.isTablet) 2 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (
                genreLabel != null ||
                releaseLabel != null ||
                seasonCountLabel != null ||
                runtimeLabel != null ||
                ageRatingLabel != null
            ) {
                StreamingShowcaseRuntimeAgeRow(
                    genreLabel = genreLabel,
                    releaseLabel = releaseLabel,
                    seasonCountLabel = seasonCountLabel,
                    runtimeLabel = runtimeLabel,
                    ageRatingLabel = ageRatingLabel,
                    compact = compactControls,
                )
            }

            if (showOverview) overview?.let { summary ->
                Text(
                    text = summary,
                    style = when {
                        landscapeCompact -> MaterialTheme.typography.bodyMedium
                        layout.isTablet -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.bodyMedium
                    },
                    color = Color.White.copy(alpha = 0.86f),
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = when {
                        ultraCompact -> 17.sp
                        landscapeCompact -> 18.sp
                        layout.isTablet -> 24.sp
                        else -> 20.sp
                    },
                    maxLines = when {
                        ultraCompact -> 2
                        landscapeCompact -> 3
                        layout.isTablet -> 5
                        else -> 3
                    },
                    overflow = TextOverflow.Ellipsis,
                )
            }

            StreamingShowcaseNetflixActionRow(
                detailsEnabled = detailsAction != null,
                saveEnabled = onSaveClick != null,
                isSaved = isSaved,
                compact = compactControls,
                onDetailsClick = { detailsAction?.invoke(item) },
                onSaveClick = { onSaveClick?.invoke(item) },
            )

            if (awardLabel != null || showcaseRatings.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(if (landscapeCompact) 5.dp else 8.dp)) {
                    awardLabel?.takeUnless { landscapeCompact }?.let { award ->
                        Text(
                            text = award,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.74f),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    StreamingShowcaseSignalsRow(
                        ratings = showcaseRatings,
                        compact = compactControls,
                    )
                }
            }
        }

        if (itemCount > 1) {
            StreamingShowcaseIndicators(
                itemCount = itemCount,
                currentPage = currentPage,
                pagerState = pagerState,
                coroutineScope = coroutineScope,
                compact = landscapeCompact,
                modifier = Modifier
                    .align(if (landscapeCompact) Alignment.BottomStart else Alignment.BottomCenter)
                    .padding(
                        start = if (landscapeCompact) startPadding else 0.dp,
                        bottom = if (landscapeCompact) 34.dp else if (layout.isTablet) 28.dp else 12.dp,
                    ),
            )
        }
    }
}

@Composable
private fun StreamingShowcaseMetaRail(
    items: List<HeroMetaItem>,
) {
    if (items.isEmpty()) return
    val accentColor = MaterialTheme.colorScheme.primary
    val railShape = RoundedCornerShape(999.dp)
    val emphasizedIndex = items.indexOfFirst { it.emphasized }.takeIf { it >= 0 }
    val emphasizedItem = emphasizedIndex?.let(items::get)
    val detailItems = items
        .filterIndexed { index, _ -> index != emphasizedIndex }
        .take(2)
    val detailMaxWidth = if (detailItems.size > 1) 166.dp else 228.dp

    Row(
        modifier = Modifier
            .widthIn(max = 340.dp)
            .clip(railShape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.11f),
                        Color.White.copy(alpha = 0.06f),
                        Color.Black.copy(alpha = 0.18f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.11f),
                shape = railShape,
            )
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        emphasizedItem?.let { item ->
            Text(
                text = item.text,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.34f),
                                accentColor.copy(alpha = 0.20f),
                            ),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = 0.58f),
                        shape = RoundedCornerShape(999.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    letterSpacing = 0.sp,
                ),
                color = Color.White,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        detailItems.forEachIndexed { index, item ->
            if (emphasizedItem != null || index > 0) {
                StreamingShowcaseMetaDivider()
            }
            Text(
                text = item.text,
                modifier = if (index == 0) Modifier.widthIn(max = detailMaxWidth) else Modifier,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    letterSpacing = 0.sp,
                ),
                color = Color.White.copy(alpha = if (index == detailItems.lastIndex) 0.86f else 0.78f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StreamingShowcaseMetaDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 9.dp)
            .size(3.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.42f)),
    )
}

@Composable
private fun StreamingShowcaseRuntimeAgeRow(
    genreLabel: String?,
    releaseLabel: String?,
    seasonCountLabel: String?,
    runtimeLabel: String?,
    ageRatingLabel: String?,
    compact: Boolean,
) {
    val textStyle = MaterialTheme.typography.labelLarge.copy(
        fontSize = if (compact) 13.sp else 14.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.sp,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 14.dp),
    ) {
        genreLabel?.let { genre ->
            Text(
                text = genre,
                modifier = Modifier.widthIn(max = if (compact) 150.dp else 190.dp),
                style = textStyle,
                color = Color.White.copy(alpha = 0.76f),
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (genreLabel != null && releaseLabel != null) {
            StreamingShowcaseInlineDot()
        }
        releaseLabel?.let { release ->
            Text(
                text = release,
                style = textStyle,
                color = Color.White.copy(alpha = 0.76f),
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
        seasonCountLabel?.let { count ->
            if (genreLabel != null || releaseLabel != null) {
                StreamingShowcaseInlineDot()
            }
            Text(
                text = count,
                modifier = Modifier.widthIn(max = if (compact) 94.dp else 118.dp),
                style = textStyle,
                color = Color.White.copy(alpha = 0.76f),
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        runtimeLabel?.let { signal ->
            StreamingShowcaseSignalItem(
                text = signal,
                imageVector = Icons.Rounded.AccessTime,
                textStyle = textStyle,
            )
        }
        ageRatingLabel?.let { signal ->
            StreamingShowcaseSignalItem(
                text = signal,
                imageVector = Icons.Rounded.Shield,
                textStyle = textStyle,
            )
        }
    }
}

@Composable
private fun StreamingShowcaseInlineDot() {
    Box(
        modifier = Modifier
            .size(3.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.42f)),
    )
}

@Composable
private fun heroSeasonCountLabel(detailMeta: MetaDetails?): String? {
    val seasonCount = remember(detailMeta?.videos) {
        detailMeta?.mainSeriesStats()?.seasonCount
    }?.takeIf { it > 0 } ?: return null
    return if (seasonCount == 1) {
        stringResource(Res.string.home_hero_season_count_one)
    } else {
        stringResource(Res.string.home_hero_season_count, seasonCount)
    }
}

@Composable
private fun StreamingShowcaseSignalsRow(
    ratings: List<StreamingShowcaseRatingItem>,
    compact: Boolean,
) {
    val textStyle = MaterialTheme.typography.labelLarge.copy(
        fontSize = if (compact) 13.sp else 14.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.sp,
    )
    val iconHeight = if (compact) 15.dp else 16.dp
    val itemSpacing = if (compact) 9.dp else 14.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
    ) {
        ratings.forEach { rating ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Image(
                    painter = painterResource(rating.logo),
                    contentDescription = rating.displayName,
                    modifier = Modifier.size(
                        width = rating.logoWidth,
                        height = iconHeight,
                    ),
                )
                Text(
                    text = rating.text,
                    style = textStyle,
                    color = rating.valueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

@Composable
private fun StreamingShowcaseSignalItem(
    text: String,
    imageVector: ImageVector,
    textStyle: androidx.compose.ui.text.TextStyle,
) {
    val signalColor = Color.White.copy(alpha = 0.72f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = signalColor,
        )
        Text(
            text = text,
            style = textStyle,
            color = signalColor,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

private data class StreamingShowcaseRatingVisuals(
    val source: String,
    val displayName: String,
    val logo: DrawableResource,
    val logoWidth: Dp,
    val valueColor: Color,
    val format: (Double) -> String,
)

private data class StreamingShowcaseRatingItem(
    val displayName: String,
    val logo: DrawableResource,
    val logoWidth: Dp,
    val valueColor: Color,
    val text: String,
)

private val streamingShowcaseRatingVisuals = listOf(
    StreamingShowcaseRatingVisuals(
        source = PROVIDER_IMDB,
        displayName = "IMDb",
        logo = Res.drawable.rating_imdb,
        logoWidth = 30.dp,
        valueColor = Color(0xFFF5C518),
        format = ::formatShowcaseOneDecimal,
    ),
    StreamingShowcaseRatingVisuals(
        source = PROVIDER_TOMATOES,
        displayName = "Rotten Tomatoes",
        logo = Res.drawable.rating_rotten_tomatoes,
        logoWidth = 15.dp,
        valueColor = Color(0xFFFF3B1F),
        format = ::formatShowcasePercent,
    ),
    StreamingShowcaseRatingVisuals(
        source = PROVIDER_METACRITIC,
        displayName = "Metacritic",
        logo = Res.drawable.rating_metacritic,
        logoWidth = 15.dp,
        valueColor = Color(0xFFFF3B1F),
        format = ::formatShowcaseWhole,
    ),
    StreamingShowcaseRatingVisuals(
        source = PROVIDER_TMDB,
        displayName = "TMDB",
        logo = Res.drawable.rating_tmdb,
        logoWidth = 15.dp,
        valueColor = Color(0xFF01B4E4),
        format = ::formatShowcaseWhole,
    ),
    StreamingShowcaseRatingVisuals(
        source = PROVIDER_LETTERBOXD,
        displayName = "Letterboxd",
        logo = Res.drawable.rating_letterboxd,
        logoWidth = 15.dp,
        valueColor = Color(0xFF00E054),
        format = ::formatShowcaseOneDecimal,
    ),
    StreamingShowcaseRatingVisuals(
        source = PROVIDER_AUDIENCE,
        displayName = "Audience Score",
        logo = Res.drawable.rating_audience_score,
        logoWidth = 15.dp,
        valueColor = Color(0xFFFF3B1F),
        format = ::formatShowcasePercent,
    ),
    StreamingShowcaseRatingVisuals(
        source = PROVIDER_TRAKT,
        displayName = "Trakt",
        logo = Res.drawable.rating_trakt,
        logoWidth = 15.dp,
        valueColor = Color(0xFFED1C24),
        format = ::formatShowcaseWhole,
    ),
)

private fun buildStreamingShowcaseRatings(
    externalRatings: List<MetaExternalRating>,
    fallbackImdbRating: Double?,
    maxItems: Int,
): List<StreamingShowcaseRatingItem> {
    val ratingsBySource = externalRatings
        .filter { it.value > 0.0 }
        .associateBy { it.source }
        .toMutableMap()

    if (fallbackImdbRating != null && PROVIDER_IMDB !in ratingsBySource) {
        ratingsBySource[PROVIDER_IMDB] = MetaExternalRating(
            source = PROVIDER_IMDB,
            value = fallbackImdbRating,
        )
    }

    return streamingShowcaseRatingVisuals
        .mapNotNull { visuals ->
            val rating = ratingsBySource[visuals.source] ?: return@mapNotNull null
            StreamingShowcaseRatingItem(
                displayName = visuals.displayName,
                logo = visuals.logo,
                logoWidth = visuals.logoWidth,
                valueColor = visuals.valueColor,
                text = visuals.format(rating.value),
            )
        }
        .take(maxItems)
}

private fun formatShowcaseOneDecimal(value: Double): String {
    val rounded = (value * 10.0).roundToInt()
    val whole = rounded / 10
    val decimal = (rounded % 10).absoluteValue
    return "$whole.$decimal"
}

private fun formatShowcaseWhole(value: Double): String = value.roundToInt().toString()

private fun formatShowcasePercent(value: Double): String = "${value.roundToInt()}%"

@Composable
private fun StreamingShowcaseSoundButton(
    imageVector: ImageVector,
    contentDescription: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = CircleShape
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(shape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (enabled) 0.12f else 0.07f),
                        Color.Black.copy(alpha = if (enabled) 0.24f else 0.16f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = if (enabled) 0.20f else 0.11f),
                shape = shape,
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
            tint = Color.White.copy(alpha = if (enabled) 0.88f else 0.42f),
        )
    }
}

@Composable
private fun StreamingShowcaseNetflixActionRow(
    detailsEnabled: Boolean,
    saveEnabled: Boolean,
    isSaved: Boolean,
    compact: Boolean,
    onDetailsClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(if (compact) 0.78f else 0.82f)
            .widthIn(max = if (compact) 346.dp else 420.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StreamingShowcaseNetflixButton(
            text = stringResource(Res.string.home_view_details),
            imageVector = Icons.Rounded.Info,
            primary = true,
            enabled = detailsEnabled,
            compact = compact,
            modifier = Modifier.weight(1f),
            onClick = onDetailsClick,
        )
        StreamingShowcaseNetflixButton(
            text = stringResource(Res.string.home_hero_my_list),
            imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.Add,
            primary = false,
            enabled = saveEnabled,
            compact = compact,
            modifier = Modifier.weight(1f),
            onClick = onSaveClick,
        )
    }
}

@Composable
private fun StreamingShowcaseNetflixButton(
    text: String,
    imageVector: ImageVector,
    primary: Boolean,
    enabled: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(if (compact) 7.dp else 9.dp)
    val height = if (compact) 42.dp else 48.dp
    val iconSize = if (compact) 19.dp else 22.dp
    val textStyle = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium
    val accentBrush = rememberAnimatedAccentBrush().takeIf { primary && enabled }
    val foregroundColor = if (accentBrush != null) {
        MaterialTheme.colorScheme.onPrimary
    } else if (primary) {
        Color.Black.copy(alpha = if (enabled) 0.90f else 0.50f)
    } else {
        Color.White.copy(alpha = if (enabled) 0.94f else 0.48f)
    }
    Row(
        modifier = modifier
            .height(height)
            .clip(shape)
            .then(
                when {
                    accentBrush != null -> Modifier.background(accentBrush, shape)
                    else -> Modifier.background(
                        if (primary) {
                            Color.White.copy(alpha = if (enabled) 0.96f else 0.54f)
                        } else {
                            Color(0xFF2B2D34).copy(alpha = if (enabled) 0.72f else 0.38f)
                        },
                        shape,
                    )
                },
            )
            .border(
                width = 1.dp,
                color = if (accentBrush != null) {
                    MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 0.34f else 0.16f)
                } else if (primary) {
                    Color.White.copy(alpha = 0.36f)
                } else {
                    Color.White.copy(alpha = 0.14f)
                },
                shape = shape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (compact) 12.dp else 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = foregroundColor,
        )
        Spacer(modifier = Modifier.width(if (compact) 8.dp else 10.dp))
        Text(
            text = text,
            style = textStyle,
            color = foregroundColor,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StreamingShowcaseButton(
    text: String,
    imageVector: ImageVector,
    primary: Boolean,
    compact: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(if (compact) 10.dp else 12.dp)
    val horizontalPadding = if (compact) 12.dp else 20.dp
    val verticalPadding = if (compact) 9.dp else 12.dp
    val iconSize = if (compact) 18.dp else 21.dp
    val itemSpacing = if (compact) 7.dp else 10.dp
    val accentBrush = rememberAnimatedAccentBrush().takeIf { primary && enabled }
    val contentColor = if (accentBrush != null) {
        MaterialTheme.colorScheme.onPrimary
    } else if (primary) {
        Color.Black.copy(alpha = 0.92f)
    } else {
        Color.White.copy(alpha = 0.92f)
    }
    Row(
        modifier = Modifier
            .clip(shape)
            .then(
                when {
                    accentBrush != null -> Modifier.background(accentBrush, shape)
                    else -> Modifier.background(
                        if (primary) {
                            Color.White.copy(alpha = if (enabled) 0.96f else 0.56f)
                        } else {
                            Color.White.copy(alpha = if (enabled) 0.13f else 0.07f)
                        },
                        shape,
                    )
                },
            )
            .border(
                width = 1.dp,
                color = if (accentBrush != null) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                } else if (primary) {
                    Color.Black.copy(alpha = 0.14f)
                } else {
                    Color.White.copy(alpha = 0.20f)
                },
                shape = shape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = contentColor,
        )
        Text(
            text = text,
            style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
            color = contentColor,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StreamingShowcaseIndicators(
    itemCount: Int,
    currentPage: Int,
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val visibleDotCount = itemCount.coerceAtMost(7)
    val accentColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(visibleDotCount) { index ->
            val targetPage = if (visibleDotCount == itemCount) {
                index
            } else {
                val lastIndex = visibleDotCount - 1
                when (index) {
                    0 -> 0
                    lastIndex -> itemCount - 1
                    else -> ((itemCount - 1) * (index.toFloat() / lastIndex.toFloat())).roundToInt()
                }
            }
            val active = if (visibleDotCount == itemCount) {
                index == currentPage
            } else {
                index == currentPage.coerceIn(0, visibleDotCount - 1)
            }
            Box(
                modifier = Modifier
                    .width(if (active) if (compact) 42.dp else 54.dp else if (compact) 8.dp else 10.dp)
                    .height(if (compact) 8.dp else 10.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) {
                            accentColor
                        } else {
                            Color.White.copy(alpha = 0.68f)
                        },
                    )
                    .clickable {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(targetPage)
                        }
                    },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PosterArtHeroPage(
    item: MetaPreview,
    detailMeta: MetaDetails?,
    artworkUrl: String?,
    layout: HomeHeroLayout,
    motionPreviewEnabled: Boolean,
    cinematicPulse: Float,
    heroScrollScale: Float,
    heroScrollTranslationY: Float,
    heroHeightPx: Float,
    refreshProgress: Float,
    showOverviewCue: Boolean,
    showRatings: Boolean,
    onItemClick: ((MetaPreview) -> Unit)?,
    onPlayClick: ((MetaPreview) -> Unit)?,
    onSaveClick: ((MetaPreview) -> Unit)?,
    isSaved: Boolean,
) {
    val motionVisibility = if (motionPreviewEnabled) 1f else 0f
    val motionPulse = if (motionPreviewEnabled) cinematicPulse else 0.5f
    val title = detailMeta?.name?.takeIf { it.isNotBlank() } ?: item.name
    val logoUrl = detailMeta?.logo?.takeIf { it.isNotBlank() } ?: item.logo?.takeIf { it.isNotBlank() }
    var logoLoadError by remember(item.type, item.id, logoUrl) {
        mutableStateOf(false)
    }
    val displayType = detailMeta?.type?.takeIf { it.isNotBlank() } ?: item.type
    val displayGenres = detailMeta?.genres
        .orEmpty()
        .map(String::trim)
        .filter(String::isNotBlank)
        .ifEmpty {
            item.genres
                .map(String::trim)
                .filter(String::isNotBlank)
        }
        .take(1)
    val displayRelease = detailMeta?.releaseInfo?.takeIf { it.isNotBlank() } ?: item.releaseInfo
    val runtimeLabel = formatRuntimeForDisplay(detailMeta?.runtime)
    val ageRatingLabel = detailMeta?.ageRating?.trim()?.takeIf { it.isNotBlank() }
    val seasonCountLabel = heroSeasonCountLabel(detailMeta)
    val metadataLine = buildList {
        addAll(displayGenres)
        displayRelease?.takeIf { it.isNotBlank() }?.let { add(heroReleaseYearLabel(it)) }
        seasonCountLabel?.let { add(it) }
        runtimeLabel?.takeIf { it.isNotBlank() }?.let { add(it) }
        ageRatingLabel?.let { add(it) }
    }.distinct().joinToString(separator = "  •  ")
    val displaySummary = detailMeta?.description
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: item.description?.trim()?.takeIf { it.isNotBlank() }
    val ratingItems = if (showRatings) {
        posterHeroRatings(
            item = item,
            detailMeta = detailMeta,
            maxItems = if (layout.isTablet) 5 else 4,
        )
    } else {
        emptyList()
    }
    val trailerPreviewImageUrl = posterHeroTrailerPreviewImageUrl(detailMeta)
    val motionPreviewAvailable = motionPreviewEnabled && trailerPreviewImageUrl != null
    var motionPreviewStarted by remember(item.stableKey(), trailerPreviewImageUrl, motionPreviewEnabled) {
        mutableStateOf(false)
    }
    LaunchedEffect(item.stableKey(), trailerPreviewImageUrl, motionPreviewEnabled) {
        motionPreviewStarted = false
        if (motionPreviewAvailable) {
            kotlinx.coroutines.delay(1_200L)
            motionPreviewStarted = true
        }
    }
    val motionPreviewAlpha by animateFloatAsState(
        targetValue = if (motionPreviewStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = LinearEasing),
        label = "posterHeroMotionPreviewAlpha",
    )
    val playAction = onPlayClick ?: onItemClick
    val titleSize = when {
        layout.isTablet -> 58.sp
        title.length > 28 -> 36.sp
        title.length > 18 -> 42.sp
        else -> 52.sp
    }
    val titleLineHeight = when {
        layout.isTablet -> 58.sp
        title.length > 28 -> 37.sp
        title.length > 18 -> 43.sp
        else -> 50.sp
    }
    val logoHeight = if (layout.isTablet) 132.dp else 104.dp
    val actionRailBottomPadding = if (showOverviewCue) 24.dp else 18.dp
    val actionRailHeight = if (layout.isTablet) 76.dp else 68.dp
    val contentBottomPadding = actionRailBottomPadding + actionRailHeight + if (layout.isTablet) 28.dp else 22.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = onItemClick != null) {
                onItemClick?.invoke(item)
            },
    ) {
        AsyncImage(
            model = artworkUrl,
            contentDescription = title,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = ((motionPulse - 0.5f) * HERO_CINEMATIC_PAN_PX * motionVisibility)
                    val cinematicScale = 1f + (HERO_CINEMATIC_SCALE * motionPulse * motionVisibility)
                    val backgroundScale = 1.03f * heroScrollScale * cinematicScale
                    val refreshScaleX = homeHeroRefreshScaleX(refreshProgress)
                    val refreshScaleY = homeHeroRefreshScaleY(refreshProgress)
                    val verticalBleedPx = ((backgroundScale - 1f).coerceAtLeast(0f) * heroHeightPx) / 2f
                    translationY = heroScrollTranslationY.coerceIn(-verticalBleedPx, verticalBleedPx) +
                        homeHeroRefreshTranslationY(refreshProgress)
                    scaleX = backgroundScale * refreshScaleX
                    scaleY = backgroundScale * refreshScaleY
                },
            alignment = if (layout.isTablet) Alignment.Center else Alignment.TopCenter,
            contentScale = ContentScale.Crop,
        )

        if (motionPreviewAvailable || motionPreviewAlpha > 0.01f) {
            PosterHeroMotionPreviewLayer(
                previewImageUrl = trailerPreviewImageUrl ?: artworkUrl,
                alpha = motionPreviewAlpha,
                motionPulse = motionPulse,
                refreshProgress = refreshProgress,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.20f),
                            0.28f to Color.Black.copy(alpha = 0.02f),
                            0.58f to Color.Black.copy(alpha = 0.18f),
                            0.82f to Color.Black.copy(alpha = 0.78f),
                            1f to Color.Black.copy(alpha = 0.98f),
                        ),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.44f),
                            0.32f to Color.Black.copy(alpha = 0.12f),
                            0.68f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.28f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(
                    start = if (layout.isTablet) 54.dp else 24.dp,
                    end = if (layout.isTablet) 54.dp else 24.dp,
                    bottom = contentBottomPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (logoUrl != null && !logoLoadError) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxWidth(if (layout.isTablet) 0.58f else 0.86f)
                        .height(logoHeight)
                        .clickable(enabled = onItemClick != null) {
                            onItemClick?.invoke(item)
                        },
                    alignment = Alignment.CenterStart,
                    contentScale = ContentScale.Fit,
                    onError = { logoLoadError = true },
                )
            } else {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(if (layout.isTablet) 0.62f else 0.92f),
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White.copy(alpha = 0.94f),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    fontSize = titleSize,
                    lineHeight = titleLineHeight,
                    maxLines = if (layout.isTablet) 2 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (metadataLine.isNotBlank()) {
                Text(
                    text = metadataLine,
                    style = if (layout.isTablet) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.72f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (showOverviewCue && displaySummary != null) {
                Text(
                    text = displaySummary,
                    modifier = Modifier.fillMaxWidth(if (layout.isTablet) 0.68f else 0.96f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.82f),
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = if (layout.isTablet) 22.sp else 20.sp,
                    maxLines = if (layout.isTablet) 3 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (ratingItems.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ratingItems.forEach { rating ->
                        PosterHeroRatingChip(rating = rating)
                    }
                }
            }

        }

        PosterHeroActionRail(
            playEnabled = playAction != null,
            saveEnabled = onSaveClick != null,
            isSaved = isSaved,
            onPlayClick = { playAction?.invoke(item) },
            onSaveClick = { onSaveClick?.invoke(item) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = actionRailBottomPadding),
        )
    }
}

@Composable
private fun PosterHeroMotionPreviewLayer(
    previewImageUrl: String?,
    alpha: Float,
    motionPulse: Float,
    refreshProgress: Float,
) {
    if (previewImageUrl.isNullOrBlank() || alpha <= 0.01f) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha.coerceIn(0f, 1f) },
    ) {
        AsyncImage(
            model = previewImageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val previewScale = 1.10f + (0.045f * motionPulse)
                    scaleX = previewScale * homeHeroRefreshScaleX(refreshProgress)
                    scaleY = previewScale * homeHeroRefreshScaleY(refreshProgress)
                    translationX = (0.5f - motionPulse) * 26f
                    translationY = (motionPulse - 0.5f) * 18f +
                        homeHeroRefreshTranslationY(refreshProgress)
                    this.alpha = 0.38f
                },
            alignment = Alignment.Center,
            contentScale = ContentScale.Crop,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.18f),
                            0.42f to Color.Black.copy(alpha = 0.04f),
                            0.72f to Color.Black.copy(alpha = 0.28f),
                            1f to Color.Black.copy(alpha = 0.58f),
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun PosterHeroActionRail(
    playEnabled: Boolean,
    saveEnabled: Boolean,
    isSaved: Boolean,
    onPlayClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.24f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PosterHeroRoundActionButton(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = stringResource(Res.string.action_play),
            enabled = playEnabled,
            primary = true,
            onClick = onPlayClick,
        )
        PosterHeroRoundActionButton(
            imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
            contentDescription = stringResource(
                if (isSaved) Res.string.hero_remove_from_library else Res.string.hero_add_to_library,
            ),
            enabled = saveEnabled,
            primary = false,
            onClick = onSaveClick,
        )
    }
}

@Composable
private fun PosterHeroRoundActionButton(
    imageVector: ImageVector,
    contentDescription: String?,
    enabled: Boolean,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val buttonSize = if (primary) 62.dp else 54.dp
    val iconSize = if (primary) 32.dp else 25.dp
    Box(
        modifier = Modifier
            .size(buttonSize)
            .clip(CircleShape)
            .background(
                if (primary) {
                    Color.White.copy(alpha = if (enabled) 0.95f else 0.56f)
                } else {
                    Color.White.copy(alpha = if (enabled) 0.14f else 0.07f)
                },
            )
            .border(
                width = 1.dp,
                color = if (primary) Color.Black.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.26f),
                shape = CircleShape,
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = if (primary) Color.Black.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.94f),
        )
    }
}

@Composable
private fun PosterHeroRatingChip(
    rating: PosterHeroRatingItem,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.30f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 11.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rating.label,
            style = MaterialTheme.typography.labelMedium,
            color = rating.accent,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
        Text(
            text = rating.value,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.78f),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun HeroContentBlock(
    item: MetaPreview,
    layout: HomeHeroLayout,
    detailMeta: MetaDetails?,
    heroDisplayMode: NuvioHeroDisplayMode,
    compactMetadata: Boolean,
    showRatings: Boolean,
    showOverview: Boolean,
    onItemClick: ((MetaPreview) -> Unit)?,
) {
    val logoUrl = detailMeta?.logo?.takeIf { it.isNotBlank() } ?: item.logo?.takeIf { it.isNotBlank() }
    var logoLoadError by remember(item.type, item.id, logoUrl) {
        mutableStateOf(false)
    }
    val displayType = detailMeta?.type?.takeIf { it.isNotBlank() } ?: item.type
    val detailGenres = detailMeta?.genres
        .orEmpty()
        .map(String::trim)
        .filter(String::isNotBlank)
    val fallbackGenres = item.genres
        .map(String::trim)
        .filter(String::isNotBlank)
    val maxGenres = when {
        heroDisplayMode == NuvioHeroDisplayMode.Cinematic -> 1
        compactMetadata -> 1
        else -> 2
    }
    val displayGenres = detailGenres.ifEmpty { fallbackGenres }.take(maxGenres)
    val displayRelease = detailMeta?.releaseInfo?.takeIf { it.isNotBlank() } ?: item.releaseInfo
    val seasonCountLabel = heroSeasonCountLabel(detailMeta)
    val displayImdb = if (showRatings) {
        (detailMeta?.imdbRating?.takeIf { it.isNotBlank() } ?: item.imdbRating)
            ?.takeIf { raw -> raw.toDoubleOrNull()?.let { it > 0.0 } == true }
    } else {
        null
    }
    val displaySummary = detailMeta?.description
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: item.description?.trim()?.takeIf { it.isNotBlank() }
    val displayTypeLabel = heroTypeLabel(displayType)
    val metaLimit = when (heroDisplayMode) {
        NuvioHeroDisplayMode.Cinematic -> 3
        NuvioHeroDisplayMode.Balanced -> if (compactMetadata) 4 else 5
        NuvioHeroDisplayMode.InfoRich -> 5
    }
    val heroMetaItems = buildList {
        add(HeroMetaItem(text = displayTypeLabel, emphasized = true))
        displayGenres.forEach { genre ->
            add(HeroMetaItem(text = genre))
        }
        displayRelease?.takeIf { it.isNotBlank() }?.let { info ->
            add(HeroMetaItem(text = heroReleaseYearLabel(info)))
        }
        seasonCountLabel?.let { count ->
            add(HeroMetaItem(text = count))
        }
        displayImdb?.takeIf { it.isNotBlank() }?.let { rating ->
            add(HeroMetaItem(text = "IMDb $rating"))
        }
    }.take(metaLimit)
    val logoHeight = when (heroDisplayMode) {
        NuvioHeroDisplayMode.Cinematic -> if (layout.isTablet) 96.dp else 82.dp
        NuvioHeroDisplayMode.Balanced -> if (layout.isTablet) 104.dp else 88.dp
        NuvioHeroDisplayMode.InfoRich -> if (layout.isTablet) 112.dp else 96.dp
    }
    val logoWidthBoost = when (heroDisplayMode) {
        NuvioHeroDisplayMode.Cinematic -> 0.08f
        NuvioHeroDisplayMode.Balanced -> if (compactMetadata) 0.12f else 0.16f
        NuvioHeroDisplayMode.InfoRich -> 0.18f
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (layout.isTablet) Alignment.Start else Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = if (layout.isTablet) Alignment.CenterStart else Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(
                        if (layout.isTablet) {
                            (layout.logoWidthFraction + logoWidthBoost).coerceAtMost(0.82f)
                        } else {
                            (layout.logoWidthFraction + logoWidthBoost).coerceAtMost(0.94f)
                        },
                    )
                    .height(logoHeight)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.68f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.28f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            if (logoUrl != null && !logoLoadError) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxWidth(layout.logoWidthFraction)
                        .aspectRatio(2.6f)
                        .clickable(enabled = onItemClick != null) {
                            onItemClick?.invoke(item)
                        },
                    alignment = if (layout.isTablet) Alignment.CenterStart else Alignment.Center,
                    contentScale = ContentScale.Fit,
                    onError = { logoLoadError = true },
                )
            } else {
                Text(
                    text = item.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = onItemClick != null) {
                            onItemClick?.invoke(item)
                        },
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Black,
                    textAlign = if (layout.isTablet) TextAlign.Start else TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HeroMetaGlassRail(
            items = heroMetaItems,
            layout = layout,
            heroDisplayMode = heroDisplayMode,
            compact = compactMetadata,
        )

        if (showOverview) displaySummary?.let { summary ->
            Spacer(modifier = Modifier.height(if (compactMetadata || heroDisplayMode == NuvioHeroDisplayMode.Cinematic) 9.dp else 12.dp))
            HeroSummaryCard(
                summary = summary,
                layout = layout,
                heroDisplayMode = heroDisplayMode,
                compact = compactMetadata,
            )
        }
    }
}

@Composable
private fun HeroSummaryCard(
    summary: String,
    layout: HomeHeroLayout,
    heroDisplayMode: NuvioHeroDisplayMode,
    compact: Boolean,
) {
    val summaryLabel = stringResource(Res.string.meta_section_overview_title)
    val summaryShape = RoundedCornerShape(if (compact || heroDisplayMode == NuvioHeroDisplayMode.Cinematic) 20.dp else 24.dp)
    val backgroundAlphaStart = when (heroDisplayMode) {
        NuvioHeroDisplayMode.Cinematic -> 0.34f
        NuvioHeroDisplayMode.Balanced -> 0.46f
        NuvioHeroDisplayMode.InfoRich -> 0.58f
    }
    val backgroundAlphaEnd = when (heroDisplayMode) {
        NuvioHeroDisplayMode.Cinematic -> 0.50f
        NuvioHeroDisplayMode.Balanced -> 0.62f
        NuvioHeroDisplayMode.InfoRich -> 0.76f
    }
    val summaryMaxLines = when (heroDisplayMode) {
        NuvioHeroDisplayMode.Cinematic -> if (layout.isTablet) 2 else 1
        NuvioHeroDisplayMode.Balanced -> if (layout.isTablet) 3 else 2
        NuvioHeroDisplayMode.InfoRich -> if (layout.isTablet) 4 else 2
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(summaryShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background.copy(alpha = backgroundAlphaStart),
                        MaterialTheme.colorScheme.background.copy(alpha = backgroundAlphaEnd),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.14f),
                shape = summaryShape,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = "$summaryLabel: $summary"
            },
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact || heroDisplayMode == NuvioHeroDisplayMode.Cinematic) 12.dp else 14.dp,
                vertical = if (compact || heroDisplayMode == NuvioHeroDisplayMode.Cinematic) 8.dp else 10.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 9.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(
                        when {
                            heroDisplayMode == NuvioHeroDisplayMode.Cinematic -> if (layout.isTablet) 44.dp else 34.dp
                            compact -> if (layout.isTablet) 54.dp else 38.dp
                            else -> if (layout.isTablet) 62.dp else 44.dp
                        },
                    )
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.96f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = summaryLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = summary,
                    style = if (layout.isTablet) {
                        MaterialTheme.typography.bodyLarge
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.96f),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start,
                    maxLines = summaryMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroMetaGlassRail(
    items: List<HeroMetaItem>,
    layout: HomeHeroLayout,
    heroDisplayMode: NuvioHeroDisplayMode,
    compact: Boolean,
) {
    if (items.isEmpty()) return

    val railShape = RoundedCornerShape(999.dp)
    val railWidthFraction = when (heroDisplayMode) {
        NuvioHeroDisplayMode.Cinematic -> if (layout.isTablet) 0.72f else 0.78f
        NuvioHeroDisplayMode.Balanced -> if (layout.isTablet) 0.80f else 0.84f
        NuvioHeroDisplayMode.InfoRich -> if (layout.isTablet) 0.88f else 0.92f
    }
    val railMaxWidth = when (heroDisplayMode) {
        NuvioHeroDisplayMode.Cinematic -> if (layout.isTablet) 470.dp else 350.dp
        NuvioHeroDisplayMode.Balanced -> if (layout.isTablet) 520.dp else 390.dp
        NuvioHeroDisplayMode.InfoRich -> if (layout.isTablet) 560.dp else 430.dp
    }
    val backgroundAlphaStart = when (heroDisplayMode) {
        NuvioHeroDisplayMode.Cinematic -> 0.24f
        NuvioHeroDisplayMode.Balanced -> 0.30f
        NuvioHeroDisplayMode.InfoRich -> 0.36f
    }
    val softBrush = rememberAnimatedSoftBrush()
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (layout.isTablet) Alignment.CenterStart else Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(railWidthFraction)
                .widthIn(max = railMaxWidth)
                .clip(railShape)
                .then(
                    if (softBrush != null) {
                        Modifier.background(softBrush, railShape)
                    } else {
                        Modifier.background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background.copy(alpha = backgroundAlphaStart),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.20f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                ),
                            ),
                            railShape,
                        )
                    },
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.14f),
                    shape = railShape,
                ),
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (compact || heroDisplayMode == NuvioHeroDisplayMode.Cinematic) 7.dp else 8.dp,
                        vertical = if (compact || heroDisplayMode == NuvioHeroDisplayMode.Cinematic) 5.dp else 7.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(
                    space = if (compact) 6.dp else 7.dp,
                    alignment = if (layout.isTablet) Alignment.Start else Alignment.CenterHorizontally,
                ),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                items.forEach { item ->
                    HeroMetaLabel(
                        item = item,
                        compact = compact || heroDisplayMode == NuvioHeroDisplayMode.Cinematic,
                    )
                }
            }
        }
    }
}

private suspend fun fetchHeroDetailMeta(
    item: MetaPreview,
    onBaseMeta: (MetaDetails) -> Unit = {},
): MetaDetails? =
    runCatching {
        val meta = withTimeoutOrNull(HERO_DETAIL_META_TIMEOUT_MS) {
            MetaDetailsRepository.fetch(
                type = item.type,
                id = item.id,
            )
        } ?: return@runCatching null
        onBaseMeta(meta)
        val metaWithFallbacks = meta.withHeroMetadataFallback(item)
        onBaseMeta(metaWithFallbacks)
        MdbListSettingsRepository.ensureLoaded()
        val settings = MdbListSettingsRepository.snapshot()
        withTimeoutOrNull(HERO_MDBLIST_ENRICH_TIMEOUT_MS) {
            MdbListMetadataService.enrichMeta(
                meta = metaWithFallbacks,
                fallbackItemId = item.id,
                settings = settings,
            )
        } ?: metaWithFallbacks
    }.getOrNull()

private suspend fun MetaDetails.withHeroMetadataFallback(item: MetaPreview): MetaDetails {
    val needsOverview = description.isNullOrBlank()
    val needsRating = imdbRating
        ?.takeIf { it.isNotBlank() }
        ?.toDoubleOrNull()
        ?.let { it > 0.0 } != true &&
        externalRatings.none { rating -> rating.source.equals(PROVIDER_IMDB, ignoreCase = true) && rating.value > 0.0 }
    if (!needsOverview && !needsRating) return this

    TmdbSettingsRepository.ensureLoaded()
    val tmdbSettings = TmdbSettingsRepository.snapshot()
    if (!tmdbSettings.hasApiKey) return this

    val heroSettings = tmdbSettings.copy(
        enabled = true,
        useBasicInfo = true,
    )

    return withTimeoutOrNull(HERO_TMDB_OVERVIEW_TIMEOUT_MS) {
        TmdbMetadataService.enrichMeta(
            meta = this@withHeroMetadataFallback,
            fallbackItemId = item.id,
            settings = heroSettings,
        )
    }?.takeIf { enriched ->
        (needsOverview && !enriched.description.isNullOrBlank()) ||
            (needsRating && enriched.imdbRating?.toDoubleOrNull()?.let { it > 0.0 } == true)
    } ?: this
}

@Composable
private fun HeroMetaLabel(
    item: HeroMetaItem,
    compact: Boolean,
) {
    val modifier = if (item.emphasized) {
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.88f), RoundedCornerShape(999.dp))
            .padding(horizontal = if (compact) 9.dp else 10.dp, vertical = if (compact) 3.dp else 4.dp)
    } else {
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
            .padding(horizontal = if (compact) 8.dp else 9.dp, vertical = if (compact) 3.dp else 4.dp)
    }

    Text(
        text = item.text,
        modifier = modifier,
        style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
        color = if (item.emphasized) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f)
        },
        fontWeight = if (item.emphasized) FontWeight.Black else FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun HeroCtaButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(44.dp)
    val accentBrush = rememberAnimatedAccentBrush()
    val softBrush = rememberAnimatedSoftBrush()
    Box(
        modifier = Modifier
            .clip(shape)
            .then(
                if (softBrush != null) {
                    Modifier.background(softBrush)
                } else {
                    Modifier.background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                Color.Transparent,
                            ),
                        ),
                    )
                },
            )
            .padding(2.dp),
    ) {
        Surface(
            modifier = Modifier
                .then(
                    if (accentBrush != null) {
                        Modifier.background(accentBrush, RoundedCornerShape(40.dp))
                    } else {
                        Modifier
                    },
                )
                .clickable(enabled = enabled, onClick = onClick),
            color = if (accentBrush != null) Color.Transparent else MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(40.dp),
            shadowElevation = 10.dp,
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 13.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun HeroPageIndicator(
    activeFraction: Float,
    onClick: () -> Unit,
) {
    val progress = activeFraction.coerceIn(0f, 1f)
    val lineBrush = rememberAnimatedLineBrush()
    Box(
        modifier = Modifier
            .width(8.dp + (28.dp * progress))
            .height(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.26f))
            .clickable(onClick = onClick),
    ) {
        if (progress > 0.02f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceAtLeast(0.18f))
                    .clip(CircleShape)
                    .then(
                        if (lineBrush != null) {
                            Modifier.background(lineBrush)
                        } else {
                            Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.86f))
                        },
                    ),
            )
        }
    }
}

@Composable
private fun heroTypeLabel(type: String): String =
    when {
        type.equals("movie", ignoreCase = true) || type.equals("film", ignoreCase = true) ->
            stringResource(Res.string.home_hero_type_movie)
        type.equals("series", ignoreCase = true) || type.equals("show", ignoreCase = true) ||
            type.equals("tv", ignoreCase = true) || type.equals("tvshow", ignoreCase = true) ->
            stringResource(Res.string.home_hero_type_series)
        else -> type.replaceFirstChar(Char::uppercase)
    }

private fun heroReleaseYearLabel(raw: String): String =
    extractReleaseYearForDisplay(raw)?.toString() ?: raw

private fun posterHeroTrailerPreviewImageUrl(detailMeta: MetaDetails?): String? =
    detailMeta
        ?.trailers
        .orEmpty()
        .filter { trailer ->
            trailer.key.isNotBlank() && trailer.site.equals("YouTube", ignoreCase = true)
        }
        .sortedWith(
            compareByDescending<com.nuvio.app.features.details.MetaTrailer> { trailer ->
                trailer.type.equals("Trailer", ignoreCase = true) && trailer.official
            }.thenByDescending { trailer ->
                trailer.type.equals("Trailer", ignoreCase = true)
            }.thenByDescending { trailer ->
                trailer.official
            }.thenByDescending { trailer ->
                trailer.publishedAt.orEmpty()
            },
        )
        .firstOrNull()
        ?.key
        ?.trim()
        ?.takeUnless { key -> key.startsWith("http://") || key.startsWith("https://") }
        ?.let { key -> "https://i.ytimg.com/vi/$key/hqdefault.jpg" }

private fun posterHeroRatings(
    item: MetaPreview,
    detailMeta: MetaDetails?,
    maxItems: Int,
): List<PosterHeroRatingItem> {
    val ratings = mutableListOf<PosterHeroRatingItem>()
    val externalRatings = detailMeta
        ?.externalRatings
        .orEmpty()
        .sortedWith(
            compareBy(
                { posterHeroRatingPriority(it.source) },
                { it.source },
            ),
        )
    val handledSources = mutableSetOf<String>()
    val imdbExternal = externalRatings.firstOrNull { it.source.equals("imdb", ignoreCase = true) }
    val fallbackImdb = detailMeta?.imdbRating?.takeIf { it.isNotBlank() } ?: item.imdbRating
    val imdbValue = imdbExternal?.value?.let { posterHeroRatingValue("imdb", it) }
        ?: fallbackImdb
            ?.trim()
            ?.takeIf { raw -> raw.toDoubleOrNull()?.let { it > 0.0 } == true }
            ?.let(::posterHeroImdbText)

    imdbValue?.let { value ->
        ratings += PosterHeroRatingItem(
            label = "IMDb",
            value = value,
            accent = Color(0xFFF5C518),
        )
        handledSources += "imdb"
    }

    for (rating in externalRatings) {
        if (ratings.size >= maxItems) break
        val source = rating.source.trim().lowercase()
        if (source.isBlank() || source in handledSources) continue
        val label = posterHeroRatingLabel(source)
        ratings += PosterHeroRatingItem(
            label = label,
            value = posterHeroRatingValue(source, rating.value),
            accent = posterHeroRatingAccent(source),
        )
        handledSources += source
    }

    return ratings.take(maxItems)
}

private fun posterHeroRatingPriority(source: String): Int =
    when (source.trim().lowercase()) {
        "imdb" -> 0
        "tmdb" -> 1
        "tomatoes" -> 2
        "audience" -> 3
        "metacritic" -> 4
        "trakt" -> 5
        "letterboxd" -> 6
        else -> 20
    }

private fun posterHeroRatingLabel(source: String): String =
    when (source) {
        "imdb" -> "IMDb"
        "tmdb" -> "TMDB"
        "tomatoes" -> "RT"
        "audience" -> "Audience"
        "metacritic" -> "MC"
        "trakt" -> "Trakt"
        "letterboxd" -> "Letterboxd"
        else -> source.uppercase()
    }

private fun posterHeroRatingAccent(source: String): Color =
    when (source) {
        "imdb" -> Color(0xFFF5C518)
        "tmdb" -> Color(0xFF01B4E4)
        "tomatoes", "audience" -> Color(0xFFFA320A)
        "metacritic" -> Color(0xFFFFCC33)
        "trakt" -> Color(0xFFED1C24)
        "letterboxd" -> Color(0xFF00E054)
        else -> Color.White.copy(alpha = 0.78f)
    }

private fun posterHeroRatingValue(source: String, value: Double): String =
    when (source) {
        "imdb", "letterboxd" -> "${posterHeroOneDecimal(value)}/10"
        "tmdb" -> if (value <= 10.0) "${posterHeroOneDecimal(value)}/10" else value.roundToInt().toString()
        "tomatoes", "audience" -> "${value.roundToInt()}%"
        "metacritic", "trakt" -> value.roundToInt().toString()
        else -> posterHeroOneDecimal(value)
    }

private fun posterHeroImdbText(raw: String): String =
    if (raw.contains("/")) raw else "$raw/10"

private fun posterHeroOneDecimal(value: Double): String {
    val rounded = (value * 10.0).roundToInt()
    val whole = rounded / 10
    val decimal = kotlin.math.abs(rounded % 10)
    return "$whole.$decimal"
}

internal fun homeHeroLayout(
    maxWidthDp: Float,
    viewportHeightDp: Float? = null,
    mobileBelowSectionHeightHintDp: Float? = null,
): HomeHeroLayout =
    when {
        maxWidthDp >= 1200f -> HomeHeroLayout(
            isTablet = true,
            heroHeight = (maxWidthDp * 0.42f).dp.coerceIn(360.dp, 440.dp),
            contentMaxWidth = 640.dp,
            contentWidthFraction = 0.56f,
            contentHorizontalPadding = 56.dp,
            contentVerticalPadding = 22.dp,
            bottomFadeHeight = 190.dp,
            logoWidthFraction = 0.58f,
        )
        maxWidthDp >= 840f -> HomeHeroLayout(
            isTablet = true,
            heroHeight = (maxWidthDp * 0.46f).dp.coerceIn(340.dp, 420.dp),
            contentMaxWidth = 560.dp,
            contentWidthFraction = 0.62f,
            contentHorizontalPadding = 40.dp,
            contentVerticalPadding = 20.dp,
            bottomFadeHeight = 180.dp,
            logoWidthFraction = 0.56f,
        )
        maxWidthDp >= 600f -> HomeHeroLayout(
            isTablet = true,
            heroHeight = (maxWidthDp * 0.58f).dp.coerceIn(320.dp, 380.dp),
            contentMaxWidth = 520.dp,
            contentWidthFraction = 0.72f,
            contentHorizontalPadding = 32.dp,
            contentVerticalPadding = 18.dp,
            bottomFadeHeight = 170.dp,
            logoWidthFraction = 0.54f,
        )
        else -> HomeHeroLayout(
            isTablet = false,
            heroHeight = mobileHeroHeight(
                maxWidthDp = maxWidthDp,
                viewportHeightDp = viewportHeightDp,
                mobileBelowSectionHeightHintDp = mobileBelowSectionHeightHintDp,
            ),
            contentMaxWidth = 480.dp,
            contentWidthFraction = 1f,
            contentHorizontalPadding = 24.dp,
            contentVerticalPadding = 12.dp,
            bottomFadeHeight = 260.dp,
            logoWidthFraction = 0.58f,
        )
    }

internal fun posterArtHeroHeight(
    maxWidthDp: Float,
    viewportHeightDp: Float?,
    layout: HomeHeroLayout,
): Dp {
    val viewportDrivenHeight = viewportHeightDp?.let { (it * POSTER_ART_HERO_VIEWPORT_RATIO).dp }
    val widthFallbackHeight = if (layout.isTablet) {
        (maxWidthDp * 0.56f).dp
    } else {
        (maxWidthDp * 1.52f).dp
    }
    val baseHeight = viewportDrivenHeight ?: widthFallbackHeight
    val minHeight = if (layout.isTablet) 520.dp else POSTER_ART_HERO_MIN_HEIGHT_DP.dp

    return baseHeight.coerceIn(minHeight, POSTER_ART_HERO_MAX_HEIGHT_DP.dp)
}

internal fun streamingShowcaseHeroHeight(
    maxWidthDp: Float,
    viewportHeightDp: Float?,
    layout: HomeHeroLayout,
): Dp {
    val viewportDrivenHeight = viewportHeightDp?.let { (it * STREAMING_SHOWCASE_HERO_VIEWPORT_RATIO).dp }
    val widthFallbackHeight = if (layout.isTablet) {
        (maxWidthDp * 0.54f).dp
    } else {
        (maxWidthDp * 1.22f).dp
    }
    val baseHeight = viewportDrivenHeight ?: widthFallbackHeight
    val minHeight = if (layout.isTablet) 430.dp else STREAMING_SHOWCASE_HERO_MIN_HEIGHT_DP.dp

    return baseHeight.coerceIn(minHeight, STREAMING_SHOWCASE_HERO_MAX_HEIGHT_DP.dp)
}

private fun mobileHeroHeight(
    maxWidthDp: Float,
    viewportHeightDp: Float?,
    mobileBelowSectionHeightHintDp: Float?,
): Dp {
    val viewportDrivenHeight = viewportHeightDp?.let { (it * MOBILE_HERO_VIEWPORT_RATIO).dp }
    val widthFallbackHeight = (maxWidthDp * 1.02f).dp
    val baseHeight = viewportDrivenHeight ?: widthFallbackHeight

    val cappedHeight = if (viewportHeightDp != null && mobileBelowSectionHeightHintDp != null) {
        val maxAllowedFromViewport = (viewportHeightDp - mobileBelowSectionHeightHintDp).dp
        baseHeight.coerceAtMost(maxAllowedFromViewport)
    } else {
        baseHeight
    }

    return cappedHeight.coerceIn(MOBILE_HERO_MIN_HEIGHT_DP.dp, MOBILE_HERO_MAX_HEIGHT_DP.dp)
}

private fun heroBackgroundScrollScale(scrollOffsetPx: Float): Float {
    val scaleIncrease = if (scrollOffsetPx < 0f) {
        abs(scrollOffsetPx) * HERO_SCROLL_UP_SCALE_MULTIPLIER
    } else {
        scrollOffsetPx * HERO_SCROLL_DOWN_SCALE_MULTIPLIER
    }
    return (1f + scaleIncrease).coerceAtMost(HERO_SCROLL_MAX_SCALE)
}

private fun heroBackgroundScrollTranslationY(scrollOffsetPx: Float): Float {
    return scrollOffsetPx * HERO_SCROLL_PARALLAX
}

private fun Modifier.homeHeroPagerGesture(
    pagerState: PagerState,
    itemCount: Int,
    coroutineScope: CoroutineScope,
    onInteractionChanged: (Boolean) -> Unit,
): Modifier {
    if (itemCount <= 1) return this

    return pointerInput(pagerState, itemCount) {
        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Initial)
            val widthPx = size.width.toFloat().takeIf { it > 0f } ?: return@awaitEachGesture
            val velocityTracker = VelocityTracker().apply {
                addPosition(down.uptimeMillis, down.position)
            }
            val startPage = pagerState.currentPage
            var totalDx = 0f
            var totalDy = 0f
            var dragging = false

            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                velocityTracker.addPosition(change.uptimeMillis, change.position)

                if (!change.pressed) {
                    if (dragging) {
                        val targetPage = resolveHeroTargetPage(
                            startPage = startPage,
                            itemCount = itemCount,
                            totalDx = totalDx,
                            velocityX = velocityTracker.calculateVelocity().x,
                            widthPx = widthPx,
                        )
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(targetPage)
                        }
                    }
                    onInteractionChanged(false)
                    break
                }

                val delta = change.position - change.previousPosition
                totalDx += delta.x
                totalDy += delta.y

                if (!dragging) {
                    val horizontalDrag =
                        abs(totalDx) > viewConfiguration.touchSlop && abs(totalDx) > abs(totalDy)
                    val verticalDrag =
                        abs(totalDy) > viewConfiguration.touchSlop && abs(totalDy) > abs(totalDx)

                    when {
                        verticalDrag -> {
                            onInteractionChanged(false)
                            break
                        }
                        horizontalDrag -> {
                            dragging = true
                            onInteractionChanged(true)
                        }
                        else -> continue
                    }
                }

                pagerState.dispatchRawDelta(-delta.x)
                change.consume()
            }
            onInteractionChanged(false)
        }
    }
}

private fun resolveHeroTargetPage(
    startPage: Int,
    itemCount: Int,
    totalDx: Float,
    velocityX: Float,
    widthPx: Float,
): Int {
    val thresholdPassed = abs(totalDx) > widthPx * HERO_SWIPE_THRESHOLD_FRACTION ||
        abs(velocityX) > HERO_SWIPE_VELOCITY_THRESHOLD
    if (!thresholdPassed) return startPage

    val currentPage = startPage.coerceIn(0, itemCount - 1)
    return when {
        totalDx > 0f -> if (currentPage == 0) itemCount - 1 else currentPage - 1
        totalDx < 0f -> if (currentPage == itemCount - 1) 0 else currentPage + 1
        else -> currentPage
    }
}
