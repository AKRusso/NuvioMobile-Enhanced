package com.nuvio.app.features.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.AppIconResource
import com.nuvio.app.core.ui.PlatformBackHandler
import com.nuvio.app.core.ui.appIconPainter
import kotlinx.coroutines.delay
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_back
import nuvio.composeapp.generated.resources.action_next
import nuvio.composeapp.generated.resources.action_play
import nuvio.composeapp.generated.resources.nuvio_enhanced_concierge_desc
import nuvio.composeapp.generated.resources.nuvio_enhanced_concierge_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_live_tv_desc
import nuvio.composeapp.generated.resources.nuvio_enhanced_live_tv_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_boosts_label
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_community_body
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_community_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_features_body
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_features_title_accent
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_features_title_lead
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_join_discord
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_members_label
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_more_members
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_online_label
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_sample_description
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_sample_genre
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_start
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_welcome_body
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_welcome_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_release_digest_desc
import nuvio.composeapp.generated.resources.nuvio_enhanced_release_digest_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_smart_resume_desc
import nuvio.composeapp.generated.resources.nuvio_enhanced_smart_resume_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_logo
import nuvio.composeapp.generated.resources.onboarding_sample_movie
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val OnboardingPageCount = 3

private val BackgroundTop = Color(0xFF0B1220)
private val BackgroundBottom = Color(0xFF07090D)
private val SurfaceDark = Color(0xFF11151D)
private val SurfaceRaised = Color(0xFF171C25)
private val BorderSoft = Color(0xFF2A303C)
private val TextPrimary = Color(0xFFF5F7FA)
private val TextSecondary = Color(0xFFA8B0BE)
private val AccentBlue = Color(0xFF73A7FF)
private val AccentCyan = Color(0xFF4FD1C5)
private val AccentAmber = Color(0xFFF2B766)
private val AccentGreen = Color(0xFF68C78A)
private val AccentViolet = Color(0xFFA78BFA)
private val AccentPink = Color(0xFFF472B6)
private val DiscordBlue = Color(0xFF5865F2)

private val AccentGradient = listOf(
    Color(0xFF60A5FA),
    Color(0xFF8B5CF6),
    Color(0xFFD946EF),
    Color(0xFF60A5FA),
)

@Composable
internal fun EnhancedOnboardingScreen(
    onJoinDiscord: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by remember { mutableIntStateOf(0) }
    var introLogoDocked by remember { mutableStateOf(false) }
    var introContentVisible by remember { mutableStateOf(false) }
    val community by remember {
        EnhancedCommunityRepository.ensureLoaded()
        EnhancedCommunityRepository.snapshot
    }.collectAsStateWithLifecycle()
    val motion = rememberInfiniteTransition(label = "onboarding_accent")
    val accentPhase = motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "onboarding_accent_phase",
    )

    LaunchedEffect(Unit) {
        EnhancedCommunityRepository.loadOnce()
    }

    LaunchedEffect(Unit) {
        delay(500)
        introLogoDocked = true
        delay(600)
        introContentVisible = true
    }

    PlatformBackHandler(enabled = true) {
        if (page > 0) page--
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BackgroundTop, BackgroundBottom, BackgroundBottom),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .align(Alignment.Center)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            AnimatedVisibility(
                visible = page > 0 || introContentVisible,
                enter = fadeIn(tween(260)),
            ) {
                PageProgress(page = page)
            }
            AnimatedContent(
                targetState = page,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                        slideInVertically(tween(300, easing = FastOutSlowInEasing)) { it / 20 })
                        .togetherWith(
                            fadeOut(tween(150)) + slideOutVertically(tween(180)) { -it / 24 },
                        )
                },
                label = "onboarding_page",
            ) { currentPage ->
                when (currentPage) {
                    0 -> WelcomePage(
                        phase = accentPhase,
                        logoDocked = introLogoDocked,
                        contentVisible = introContentVisible,
                    )
                    1 -> FeaturesPage(accentPhase)
                    else -> CommunityPage(community, accentPhase, onJoinDiscord)
                }
            }
            AnimatedVisibility(
                visible = page > 0 || introContentVisible,
                enter = fadeIn(tween(260)),
            ) {
                PageNavigation(
                    page = page,
                    onBack = { page-- },
                    onNext = {
                        if (page == OnboardingPageCount - 1) onComplete() else page++
                    },
                )
            }
        }
    }
}

@Composable
private fun PageProgress(page: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "0${page + 1}",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(OnboardingPageCount) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(
                            if (index <= page) TextPrimary else TextPrimary.copy(alpha = 0.14f),
                            CircleShape,
                        ),
                )
            }
        }
        Text(
            text = "0$OnboardingPageCount",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )
    }
}

@Composable
private fun WelcomePage(
    phase: State<Float>,
    logoDocked: Boolean,
    contentVisible: Boolean,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        val expandedLogoSize = 196.dp
        val logoSize by animateDpAsState(
            targetValue = if (logoDocked) 82.dp else expandedLogoSize,
            animationSpec = tween(620, easing = FastOutSlowInEasing),
            label = "welcome_logo_size",
        )
        val logoX by animateDpAsState(
            targetValue = if (logoDocked) 0.dp else (maxWidth - expandedLogoSize) / 2,
            animationSpec = tween(620, easing = FastOutSlowInEasing),
            label = "welcome_logo_x",
        )
        val logoY by animateDpAsState(
            targetValue = if (logoDocked) 10.dp else (maxHeight - expandedLogoSize) / 2,
            animationSpec = tween(620, easing = FastOutSlowInEasing),
            label = "welcome_logo_y",
        )

        Box(modifier = Modifier.fillMaxSize()) {
            WelcomeArtwork(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(220.dp),
            )
            Image(
                painter = painterResource(Res.drawable.nuvio_enhanced_onboarding_logo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = logoX, y = logoY)
                    .size(logoSize)
                    .clip(LogoCutoutShape),
            )

            AnimatedVisibility(
                visible = contentVisible,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 112.dp),
                enter = fadeIn(tween(420)) + slideInVertically(tween(440)) { it / 10 },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Nuvio",
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 35.sp),
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                        )
                        AnimatedAccentText(
                            text = "Enhanced",
                            phase = phase,
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 35.sp),
                        )
                    }
                    Text(
                        text = stringResource(Res.string.nuvio_enhanced_onboarding_welcome_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(3.dp)
                            .background(AccentBlue, CircleShape),
                    )
                    Text(
                        text = stringResource(Res.string.nuvio_enhanced_onboarding_welcome_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeArtwork(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)),
    ) {
        SampleArtwork(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.44f),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            BackgroundBottom,
                            BackgroundBottom.copy(alpha = 0.12f),
                            BackgroundBottom.copy(alpha = 0.58f),
                        ),
                    ),
                ),
        )
    }
}

private val LogoCutoutShape = GenericShape { size, _ ->
    moveTo(size.width * 0.20f, size.height * 0.06f)
    quadraticBezierTo(size.width * 0.10f, size.height * 0.16f, size.width * 0.10f, size.height * 0.34f)
    lineTo(size.width * 0.10f, size.height * 0.66f)
    quadraticBezierTo(size.width * 0.10f, size.height * 0.84f, size.width * 0.20f, size.height * 0.94f)
    lineTo(size.width * 0.80f, size.height * 0.66f)
    quadraticBezierTo(size.width * 0.94f, size.height * 0.58f, size.width * 0.94f, size.height * 0.50f)
    quadraticBezierTo(size.width * 0.94f, size.height * 0.42f, size.width * 0.80f, size.height * 0.34f)
    close()
}

@Composable
private fun FeaturesPage(phase: State<Float>) {
    var selectedFeature by remember { mutableIntStateOf(0) }
    val features = remember {
        listOf(
            FeaturePreview(Icons.Rounded.Home, AccentBlue, Res.string.nuvio_enhanced_smart_resume_title, Res.string.nuvio_enhanced_smart_resume_desc),
            FeaturePreview(Icons.Rounded.Star, AccentAmber, Res.string.nuvio_enhanced_concierge_title, Res.string.nuvio_enhanced_concierge_desc),
            FeaturePreview(Icons.Rounded.Notifications, AccentCyan, Res.string.nuvio_enhanced_release_digest_title, Res.string.nuvio_enhanced_release_digest_desc),
            FeaturePreview(Icons.Rounded.LiveTv, AccentGreen, Res.string.nuvio_enhanced_live_tv_title, Res.string.nuvio_enhanced_live_tv_desc),
        )
    }

    PageColumn(centered = false) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = stringResource(Res.string.nuvio_enhanced_onboarding_features_title_lead),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Black,
            )
            AnimatedAccentText(
                text = stringResource(Res.string.nuvio_enhanced_onboarding_features_title_accent),
                phase = phase,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        Text(
            text = stringResource(Res.string.nuvio_enhanced_onboarding_features_body),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            features.forEachIndexed { index, feature ->
                FeatureSelector(
                    feature = feature,
                    selected = selectedFeature == index,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedFeature = index },
                )
            }
        }
        AnimatedContent(
            targetState = selectedFeature,
            transitionSpec = { fadeIn(tween(220)).togetherWith(fadeOut(tween(140))) },
            label = "feature_preview",
        ) { index ->
            FeaturePreviewPanel(feature = features[index], index = index)
        }
    }
}

private data class FeaturePreview(
    val icon: ImageVector,
    val tone: Color,
    val title: StringResource,
    val description: StringResource,
)

@Composable
private fun FeatureSelector(
    feature: FeaturePreview,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) feature.tone.copy(alpha = 0.16f) else SurfaceDark)
            .border(1.dp, if (selected) feature.tone.copy(alpha = 0.72f) else BorderSoft, RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = feature.icon,
            contentDescription = stringResource(feature.title),
            tint = if (selected) feature.tone else TextSecondary,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
private fun FeaturePreviewPanel(feature: FeaturePreview, index: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(feature.tone.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(feature.icon, contentDescription = null, tint = feature.tone, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(feature.title), style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(feature.description),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        FeatureVisual(index = index, tone = feature.tone)
    }
}

@Composable
private fun FeatureVisual(index: Int, tone: Color) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
        color = BackgroundBottom.copy(alpha = 0.86f),
        shape = RoundedCornerShape(8.dp),
    ) {
        when (index) {
            0 -> ResumeVisual(tone)
            1 -> ConciergeVisual(tone)
            2 -> RadarVisual(tone)
            else -> LiveTvVisual(tone)
        }
    }
}

@Composable
private fun ResumeVisual(tone: Color) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        SampleArtwork(Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, BackgroundBottom.copy(alpha = 0.50f), BackgroundBottom.copy(alpha = 0.98f)),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = "The Last Signal",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = stringResource(Res.string.nuvio_enhanced_onboarding_sample_genre),
                style = MaterialTheme.typography.labelMedium,
                color = tone,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                SampleMetadataChip("2026")
                SampleMetadataChip("1h 48m")
                SampleMetadataChip("8.4")
            }
            Text(
                text = stringResource(Res.string.nuvio_enhanced_onboarding_sample_description),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text("S1 • E4", style = MaterialTheme.typography.labelMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Box(Modifier.weight(1f).height(5.dp).background(BorderSoft, CircleShape)) {
                    Box(Modifier.fillMaxWidth(0.72f).height(5.dp).background(tone, CircleShape))
                }
                Text("72%", style = MaterialTheme.typography.labelSmall, color = tone, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(tone)
                        .padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = BackgroundBottom, modifier = Modifier.size(15.dp))
                    Text(
                        text = stringResource(Res.string.action_play),
                        style = MaterialTheme.typography.labelSmall,
                        color = BackgroundBottom,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SampleMetadataChip(value: String) {
    Text(
        text = value,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.42f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        color = TextPrimary,
    )
}

@Composable
private fun ConciergeVisual(tone: Color) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        SampleArtwork(
            modifier = Modifier
                .width(104.dp)
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text("The Last Signal", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Black)
            Text(stringResource(Res.string.nuvio_enhanced_onboarding_sample_genre), style = MaterialTheme.typography.labelSmall, color = tone)
            Text(
                text = stringResource(Res.string.nuvio_enhanced_onboarding_sample_description),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "92%",
                style = MaterialTheme.typography.titleLarge,
                color = tone,
                fontWeight = FontWeight.Black,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(Icons.Rounded.Home, Icons.Rounded.Notifications, Icons.Rounded.LiveTv).forEach { icon ->
                    Box(
                        modifier = Modifier.size(31.dp).clip(RoundedCornerShape(7.dp)).background(tone.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RadarVisual(tone: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Notifications, contentDescription = null, tint = tone, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("The Last Signal", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("03", style = MaterialTheme.typography.labelLarge, color = tone, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            listOf("12", "18", "24").forEachIndexed { index, day ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(136.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    if (index == 1) tone.copy(alpha = 0.48f) else SurfaceRaised,
                                    SurfaceDark,
                                ),
                            ),
                        )
                        .border(1.dp, if (index == 1) tone.copy(alpha = 0.72f) else BorderSoft, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(day, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Black)
                    SampleArtwork(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(6.dp)),
                    )
                    Text("20:${index}0", style = MaterialTheme.typography.labelSmall, color = if (index == 1) tone else TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun LiveTvVisual(tone: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(3) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (index == 0) tone.copy(alpha = 0.12f) else SurfaceRaised.copy(alpha = 0.68f))
                    .border(1.dp, if (index == 0) tone.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (index == 0) tone.copy(alpha = 0.2f) else BorderSoft.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center,
                ) {
                    SampleArtwork(Modifier.fillMaxSize())
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = listOf("The Last Signal", "Nuvio One", "Cinema+")[index],
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Box(Modifier.fillMaxWidth().height(4.dp).background(BorderSoft, CircleShape)) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.35f + index * 0.18f)
                                .height(4.dp)
                                .background(if (index == 0) tone else TextSecondary.copy(alpha = 0.45f), CircleShape),
                        )
                    }
                }
                Text("${20 + index}:00", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun SampleArtwork(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(Res.drawable.onboarding_sample_movie),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

@Composable
private fun CommunityPage(
    snapshot: EnhancedCommunitySnapshot,
    phase: State<Float>,
    onJoinDiscord: () -> Unit,
) {
    val visibleMembers = remember(snapshot.members) {
        snapshot.members
            .sortedWith(compareBy<EnhancedCommunityMember>({ statusPriority(it.status) }, { it.username.lowercase() }))
            .take(10)
    }
    PageColumn(centered = false) {
        Text(
            text = stringResource(Res.string.nuvio_enhanced_onboarding_community_title),
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = stringResource(Res.string.nuvio_enhanced_onboarding_community_body),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
        )

        CommunityHero(snapshot)

        CommunityChannels()

        if (visibleMembers.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.nuvio_enhanced_onboarding_online_label),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark.copy(alpha = 0.72f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BorderSoft),
            ) {
                Column {
                    visibleMembers.forEachIndexed { index, member ->
                        CommunityMemberRow(member)
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 54.dp),
                            color = BorderSoft.copy(alpha = 0.72f),
                        )
                    }
                    Text(
                        text = stringResource(Res.string.nuvio_enhanced_onboarding_more_members),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = DiscordBlue,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        DiscordButton(phase = phase, onClick = onJoinDiscord)
    }
}

@Composable
private fun CommunityHero(snapshot: EnhancedCommunitySnapshot) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DiscordBlue.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, DiscordBlue.copy(alpha = 0.36f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(DiscordBlue.copy(alpha = 0.18f), Color.Transparent, AccentViolet.copy(alpha = 0.10f)),
                    ),
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                ServerIcon(snapshot.iconUrl)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = snapshot.serverName,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = snapshot.serverTag,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                    if (snapshot.members.isNotEmpty()) {
                        Spacer(Modifier.height(3.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy((-7).dp)) {
                            snapshot.members.take(4).forEach { member ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceRaised)
                                        .border(1.dp, SurfaceDark, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (member.avatarUrl != null) {
                                        AsyncImage(
                                            model = member.avatarUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    } else {
                                        Text(
                                            text = member.username.take(1).uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextPrimary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Icon(
                    painter = appIconPainter(AppIconResource.DiscordMark),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Row(modifier = Modifier.fillMaxWidth()) {
                CommunityStat(
                    value = snapshot.memberCount.toString(),
                    label = stringResource(Res.string.nuvio_enhanced_onboarding_members_label),
                    modifier = Modifier.weight(1f),
                )
                CommunityStat(
                    value = snapshot.onlineCount?.toString() ?: "--",
                    label = stringResource(Res.string.nuvio_enhanced_onboarding_online_label),
                    modifier = Modifier.weight(1f),
                )
                if (snapshot.boostCount != null) {
                    CommunityStat(
                        value = snapshot.boostCount.toString(),
                        label = stringResource(Res.string.nuvio_enhanced_onboarding_boosts_label),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunityChannels() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceDark.copy(alpha = 0.72f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, BorderSoft),
    ) {
        Column {
            listOf(
                "# general" to AccentBlue,
                "# support" to AccentGreen,
                "# suggestions" to AccentViolet,
            ).forEachIndexed { index, (name, color) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 13.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(color.copy(alpha = 0.13f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("#", color = color, fontWeight = FontWeight.Black)
                    }
                    Text(
                        text = name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Icon(
                        imageVector = Icons.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
                if (index < 2) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 53.dp),
                        color = BorderSoft.copy(alpha = 0.72f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerIcon(iconUrl: String?) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceRaised),
        contentAlignment = Alignment.Center,
    ) {
        if (iconUrl != null) {
            AsyncImage(
                model = iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = appIconPainter(AppIconResource.DiscordMark),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun CommunityStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}

@Composable
private fun CommunityMemberRow(member: EnhancedCommunityMember) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(SurfaceRaised),
            contentAlignment = Alignment.Center,
        ) {
            if (member.avatarUrl != null) {
                AsyncImage(
                    model = member.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = member.username.take(1).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            text = member.username,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(statusColor(member.status)),
        )
    }
}

private fun statusPriority(status: String): Int = when (status.lowercase()) {
    "online" -> 0
    "idle" -> 1
    "dnd" -> 2
    else -> 3
}

private fun statusColor(status: String): Color = when (status.lowercase()) {
    "online" -> AccentGreen
    "idle" -> AccentAmber
    "dnd" -> AccentPink
    else -> TextSecondary
}

@Composable
private fun DiscordButton(
    phase: State<Float>,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .rotatingRainbowBorder(phase)
            .padding(3.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(DiscordBlue)
                .clickable(role = Role.Button, onClick = onClick),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = appIconPainter(AppIconResource.DiscordMark),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(21.dp),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = stringResource(Res.string.nuvio_enhanced_onboarding_join_discord),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun Modifier.rotatingRainbowBorder(phase: State<Float>): Modifier = drawBehind {
    val hueShift = phase.value * 360f
    val colors = (0..6).map { index ->
        Color.hsv(
            hue = (hueShift + index * 60f) % 360f,
            saturation = 0.78f,
            value = 1f,
        )
    }
    val cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
    drawRoundRect(
        brush = Brush.sweepGradient(colors.map { it.copy(alpha = 0.24f) }),
        cornerRadius = cornerRadius,
        style = Stroke(width = 8.dp.toPx()),
    )
    drawRoundRect(
        brush = Brush.sweepGradient(colors),
        cornerRadius = cornerRadius,
        style = Stroke(width = 2.dp.toPx()),
    )
}

@Composable
private fun PageColumn(
    centered: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 18.dp, bottom = 18.dp),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
private fun PageNavigation(
    page: Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (page > 0) {
            NavigationButton(
                text = stringResource(Res.string.action_back),
                icon = Icons.Rounded.ArrowBack,
                primary = false,
                modifier = Modifier.weight(1f),
                onClick = onBack,
            )
        }
        NavigationButton(
            text = stringResource(
                if (page == OnboardingPageCount - 1) {
                    Res.string.nuvio_enhanced_onboarding_start
                } else {
                    Res.string.action_next
                },
            ),
            icon = Icons.Rounded.ArrowForward,
            primary = true,
            modifier = Modifier.weight(if (page > 0) 1f else 2f),
            onClick = onNext,
        )
    }
}

@Composable
private fun NavigationButton(
    text: String,
    icon: ImageVector,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val background = if (primary) TextPrimary else SurfaceDark
    val contentColor = if (primary) BackgroundBottom else TextPrimary
    Row(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .then(
                if (primary) Modifier else Modifier.border(1.dp, BorderSoft, RoundedCornerShape(8.dp)),
            )
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!primary) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            fontWeight = FontWeight.Bold,
        )
        if (primary) {
            Spacer(Modifier.width(8.dp))
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun AnimatedAccentText(
    text: String,
    phase: State<Float>,
    style: TextStyle,
) {
    Box {
        Text(
            text = text,
            modifier = Modifier
                .blur(12.dp)
                .alpha(0.42f)
                .accentGradient(phase),
            style = style,
            color = Color.White,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = text,
            modifier = Modifier
                .blur(5.dp)
                .alpha(0.48f)
                .accentGradient(phase),
            style = style,
            color = Color.White,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = text,
            modifier = Modifier.accentGradient(phase),
            style = style,
            color = Color.White,
            fontWeight = FontWeight.Black,
        )
    }
}

private fun Modifier.accentGradient(
    phase: State<Float>,
): Modifier = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithContent {
    drawContent()
    val progress = phase.value
    val angle = progress * (PI * 2.0)
    val offsetX = cos(angle).toFloat() * size.width * 0.24f
    val offsetY = sin(angle).toFloat() * size.height * 0.30f
    drawRect(
        brush = Brush.linearGradient(
            colors = AccentGradient,
            start = Offset(-size.width * 0.35f + offsetX, -size.height + offsetY),
            end = Offset(size.width * 1.35f + offsetX, size.height * 2f + offsetY),
        ),
        blendMode = BlendMode.SrcIn,
    )
}
