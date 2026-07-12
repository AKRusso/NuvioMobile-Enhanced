package com.nuvio.app.features.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.AppIconResource
import com.nuvio.app.core.ui.PlatformBackHandler
import com.nuvio.app.core.ui.appIconPainter
import kotlinx.coroutines.delay
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_back
import nuvio.composeapp.generated.resources.action_next
import nuvio.composeapp.generated.resources.nuvio_enhanced_concierge_desc
import nuvio.composeapp.generated.resources.nuvio_enhanced_concierge_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_live_tv_desc
import nuvio.composeapp.generated.resources.nuvio_enhanced_live_tv_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_community_body
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_community_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_developer_role
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_features_body
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_features_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_join_discord
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_members
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_start
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_team_body
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_team_note
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_team_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_welcome_body
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_welcome_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_release_digest_desc
import nuvio.composeapp.generated.resources.nuvio_enhanced_release_digest_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_smart_resume_desc
import nuvio.composeapp.generated.resources.nuvio_enhanced_smart_resume_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_logo
import nuvio.composeapp.generated.resources.onboarding_developer_russo
import nuvio.composeapp.generated.resources.onboarding_developer_yesnt
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val OnboardingPageCount = 4

private val OnboardingBackground = Color(0xFF05050A)
private val OnboardingSurface = Color(0xFF11111A)
private val OnboardingSurfaceRaised = Color(0xFF181724)
private val OnboardingText = Color(0xFFF8F7FF)
private val OnboardingTextMuted = Color(0xFFB7B3C7)
private val OnboardingPurple = Color(0xFF8B5CF6)
private val OnboardingBlue = Color(0xFF3B82F6)
private val OnboardingViolet = Color(0xFFC026D3)
private val OnboardingPink = Color(0xFFEC4899)
private val DiscordBlue = Color(0xFF5865F2)

private val EnhancedGradient = listOf(
    OnboardingPurple,
    OnboardingBlue,
    OnboardingViolet,
    OnboardingPink,
    OnboardingPurple,
)

@Composable
internal fun EnhancedOnboardingScreen(
    onJoinDiscord: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by remember { mutableIntStateOf(0) }
    val motion = rememberInfiniteTransition(label = "enhanced_onboarding_motion")
    val phase = motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "enhanced_onboarding_phase",
    )

    PlatformBackHandler(enabled = true) {
        if (page > 0) page--
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OnboardingBackground),
    ) {
        LivingBackdrop(phase = phase)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            OnboardingProgress(
                currentPage = page,
                phase = phase,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            AnimatedContent(
                targetState = page,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn(tween(320, easing = FastOutSlowInEasing)) +
                        scaleIn(spring(), initialScale = 0.985f))
                        .togetherWith(
                            fadeOut(tween(180)) +
                                scaleOut(tween(220), targetScale = 1.01f),
                        )
                },
                label = "enhanced_onboarding_page",
            ) { currentPage ->
                when (currentPage) {
                    0 -> WelcomePage(phase = phase)
                    1 -> FeaturesPage(phase = phase)
                    2 -> TeamPage(phase = phase)
                    else -> CommunityPage(phase = phase, onJoinDiscord = onJoinDiscord)
                }
            }

            OnboardingNavigation(
                page = page,
                phase = phase,
                onBack = { page-- },
                onNext = {
                    if (page == OnboardingPageCount - 1) onComplete() else page++
                },
            )
        }
    }
}

@Composable
private fun LivingBackdrop(phase: State<Float>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val angle = phase.value * (PI * 2.0)
        val orbitX = cos(angle).toFloat()
        val orbitY = sin(angle).toFloat()

        drawRect(color = OnboardingBackground)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(OnboardingBlue.copy(alpha = 0.24f), Color.Transparent),
                center = Offset(
                    x = size.width * (0.84f + orbitX * 0.08f),
                    y = size.height * (0.12f + orbitY * 0.06f),
                ),
                radius = size.maxDimension * 0.58f,
            ),
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(OnboardingViolet.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(
                    x = size.width * (0.10f - orbitY * 0.06f),
                    y = size.height * (0.56f + orbitX * 0.08f),
                ),
                radius = size.maxDimension * 0.62f,
            ),
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(OnboardingPurple.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(
                    x = size.width * (0.78f - orbitX * 0.07f),
                    y = size.height * (0.92f - orbitY * 0.04f),
                ),
                radius = size.maxDimension * 0.50f,
            ),
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, OnboardingBackground.copy(alpha = 0.42f)),
            ),
        )
    }
}

@Composable
private fun WelcomePage(phase: State<Float>) {
    val stage = rememberEntranceStage(3)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AnimatedVisibility(
            visible = stage >= 1,
            modifier = Modifier.align(Alignment.TopEnd),
            enter = fadeIn(tween(520)) + scaleIn(tween(620), initialScale = 0.88f),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = 52.dp, y = (-34).dp)
                    .size(238.dp)
                    .clip(RoundedCornerShape(bottomStart = 72.dp))
                    .animatedGlow(phase = phase, glowAlpha = 0.20f),
            ) {
                Image(
                    painter = painterResource(Res.drawable.nuvio_enhanced_onboarding_logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 190.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AnimatedVisibility(
                visible = stage >= 2,
                enter = fadeIn(tween(480)) + slideInVertically(tween(520)) { it / 7 },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrandTitle(phase = phase)
                    Text(
                        text = stringResource(Res.string.nuvio_enhanced_onboarding_welcome_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = OnboardingText,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            AnimatedVisibility(
                visible = stage >= 3,
                enter = fadeIn(tween(560)) + slideInVertically(tween(560)) { it / 8 },
            ) {
                Surface(
                    color = OnboardingSurface.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, OnboardingPurple.copy(alpha = 0.28f)),
                ) {
                    Text(
                        text = stringResource(Res.string.nuvio_enhanced_onboarding_welcome_body),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnboardingTextMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandTitle(phase: State<Float>) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Nuvio",
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
            color = OnboardingText,
            fontWeight = FontWeight.Black,
        )
        GradientText(
            text = "Enhanced",
            phase = phase,
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
            fontWeight = FontWeight.Black,
            glow = true,
        )
    }
}

@Composable
private fun FeaturesPage(phase: State<Float>) {
    val stage = rememberEntranceStage(6)
    OnboardingPageContainer {
        OnboardingHeading(
            visible = stage >= 1,
            title = stringResource(Res.string.nuvio_enhanced_onboarding_features_title),
            body = stringResource(Res.string.nuvio_enhanced_onboarding_features_body),
            phase = phase,
            settleTitle = true,
        )
        FeatureRow(
            visible = stage >= 2,
            phase = phase,
            icon = Icons.Rounded.Home,
            title = Res.string.nuvio_enhanced_smart_resume_title,
            description = Res.string.nuvio_enhanced_smart_resume_desc,
        )
        FeatureRow(
            visible = stage >= 3,
            phase = phase,
            icon = Icons.Rounded.Star,
            title = Res.string.nuvio_enhanced_concierge_title,
            description = Res.string.nuvio_enhanced_concierge_desc,
        )
        FeatureRow(
            visible = stage >= 4,
            phase = phase,
            icon = Icons.Rounded.Notifications,
            title = Res.string.nuvio_enhanced_release_digest_title,
            description = Res.string.nuvio_enhanced_release_digest_desc,
        )
        FeatureRow(
            visible = stage >= 5,
            phase = phase,
            icon = Icons.Rounded.LiveTv,
            title = Res.string.nuvio_enhanced_live_tv_title,
            description = Res.string.nuvio_enhanced_live_tv_desc,
        )
    }
}

@Composable
private fun TeamPage(phase: State<Float>) {
    val stage = rememberEntranceStage(5)
    OnboardingPageContainer {
        OnboardingHeading(
            visible = stage >= 1,
            title = stringResource(Res.string.nuvio_enhanced_onboarding_team_title),
            body = stringResource(Res.string.nuvio_enhanced_onboarding_team_body),
            phase = phase,
            centered = true,
        )
        DeveloperCard(
            visible = stage >= 2,
            phase = phase,
            avatar = Res.drawable.onboarding_developer_yesnt,
            displayName = "yesn't",
            handle = "@yesnt10",
            accent = OnboardingViolet,
        )
        DeveloperConnector(visible = stage >= 3, phase = phase)
        DeveloperCard(
            visible = stage >= 3,
            phase = phase,
            avatar = Res.drawable.onboarding_developer_russo,
            displayName = "Russo",
            handle = "@AKRusso",
            accent = OnboardingBlue,
        )
        AnimatedVisibility(
            visible = stage >= 4,
            enter = gentleEntrance(),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = OnboardingSurface.copy(alpha = 0.82f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Brush.linearGradient(EnhancedGradient)),
            ) {
                Text(
                    text = stringResource(Res.string.nuvio_enhanced_onboarding_team_note),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnboardingTextMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun CommunityPage(
    phase: State<Float>,
    onJoinDiscord: () -> Unit,
) {
    val stage = rememberEntranceStage(4)
    OnboardingPageContainer(centered = true) {
        AnimatedVisibility(
            visible = stage >= 1,
            enter = fadeIn(tween(460)) + scaleIn(tween(560), initialScale = 0.84f),
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .animatedGradientBackground(phase),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.size(86.dp),
                    color = Color(0xFF171823),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = appIconPainter(AppIconResource.DiscordMark),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(52.dp),
                        )
                    }
                }
            }
        }

        OnboardingHeading(
            visible = stage >= 2,
            title = stringResource(Res.string.nuvio_enhanced_onboarding_community_title),
            body = stringResource(Res.string.nuvio_enhanced_onboarding_community_body),
            phase = phase,
            centered = true,
        )

        AnimatedVisibility(
            visible = stage >= 3,
            enter = gentleEntrance(),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF171823).copy(alpha = 0.94f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, DiscordBlue.copy(alpha = 0.62f)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box {
                            Image(
                                painter = painterResource(Res.drawable.onboarding_developer_yesnt),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape),
                            )
                            Image(
                                painter = painterResource(Res.drawable.onboarding_developer_russo),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .padding(start = 26.dp)
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color(0xFF171823), CircleShape),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Nuvio Enhanced",
                                style = MaterialTheme.typography.titleMedium,
                                color = OnboardingText,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "discord.gg/nuvioenhanced",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB5BAFF),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF23A55A)),
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DiscordBlue.copy(alpha = 0.13f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF23A55A)),
                        )
                        Text(
                            text = stringResource(Res.string.nuvio_enhanced_onboarding_members),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnboardingText,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    OnboardingActionButton(
                        text = stringResource(Res.string.nuvio_enhanced_onboarding_join_discord),
                        phase = phase,
                        icon = null,
                        onClick = onJoinDiscord,
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContainer(
    centered: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 20.dp, bottom = 18.dp),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun OnboardingHeading(
    visible: Boolean,
    title: String,
    body: String,
    phase: State<Float>,
    centered: Boolean = false,
    settleTitle: Boolean = false,
) {
    var titleAnimated by remember { mutableStateOf(true) }
    LaunchedEffect(settleTitle) {
        if (settleTitle) {
            delay(2100)
            titleAnimated = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = gentleEntrance(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GradientText(
                text = title,
                phase = phase,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                animated = !settleTitle || titleAnimated,
                glow = titleAnimated,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = OnboardingTextMuted,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            )
        }
    }
}

@Composable
private fun FeatureRow(
    visible: Boolean,
    phase: State<Float>,
    icon: ImageVector,
    title: StringResource,
    description: StringResource,
) {
    AnimatedVisibility(
        visible = visible,
        enter = gentleEntrance(),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            OnboardingPurple.copy(alpha = 0.48f),
                            OnboardingBlue.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                    shape = RoundedCornerShape(8.dp),
                ),
            color = OnboardingSurface.copy(alpha = 0.88f),
            shape = RoundedCornerShape(8.dp),
        ) {
            Row(
                modifier = Modifier.padding(13.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .animatedGradientBackground(phase),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    GradientText(
                        text = stringResource(title),
                        phase = phase,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        glow = true,
                    )
                    Text(
                        text = stringResource(description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnboardingTextMuted,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeveloperCard(
    visible: Boolean,
    phase: State<Float>,
    avatar: DrawableResource,
    displayName: String,
    handle: String,
    accent: Color,
) {
    AnimatedVisibility(
        visible = visible,
        enter = gentleEntrance(),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(accent.copy(alpha = 0.78f), OnboardingPurple.copy(alpha = 0.18f)),
                    ),
                    shape = RoundedCornerShape(8.dp),
                ),
            color = OnboardingSurfaceRaised.copy(alpha = 0.94f),
            shape = RoundedCornerShape(8.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .animatedGradientBackground(phase)
                            .padding(3.dp),
                    ) {
                        Image(
                            painter = painterResource(avatar),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(17.dp)
                            .clip(CircleShape)
                            .background(OnboardingSurfaceRaised)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF23A55A)),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = OnboardingText,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = handle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnboardingTextMuted,
                    )
                    Surface(
                        color = accent.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
                    ) {
                        Text(
                            text = stringResource(Res.string.nuvio_enhanced_onboarding_developer_role),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeveloperConnector(
    visible: Boolean,
    phase: State<Float>,
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(300))) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .padding(horizontal = 54.dp)
                .animatedGradientBackground(phase),
        )
    }
}

@Composable
private fun OnboardingNavigation(
    page: Int,
    phase: State<Float>,
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
            SecondaryOnboardingButton(
                text = stringResource(Res.string.action_back),
                icon = Icons.Rounded.ArrowBack,
                modifier = Modifier.weight(1f),
                onClick = onBack,
            )
        }
        OnboardingActionButton(
            text = stringResource(
                if (page == OnboardingPageCount - 1) {
                    Res.string.nuvio_enhanced_onboarding_start
                } else {
                    Res.string.action_next
                },
            ),
            phase = phase,
            icon = Icons.Rounded.ArrowForward,
            modifier = Modifier.weight(if (page > 0) 1f else 2f),
            onClick = onNext,
        )
    }
}

@Composable
private fun OnboardingActionButton(
    text: String,
    phase: State<Float>,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .animatedGradientBackground(phase)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        if (icon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SecondaryOnboardingButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(OnboardingSurface.copy(alpha = 0.86f))
            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OnboardingTextMuted,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = OnboardingText,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun GradientText(
    text: String,
    phase: State<Float>,
    style: TextStyle,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Bold,
    textAlign: TextAlign = TextAlign.Start,
    animated: Boolean = true,
    glow: Boolean = false,
) {
    Box(modifier = modifier) {
        if (glow) {
            Text(
                text = text,
                modifier = Modifier
                    .blur(9.dp)
                    .alpha(0.52f),
                style = style,
                color = OnboardingViolet,
                fontWeight = fontWeight,
                textAlign = textAlign,
            )
        }
        Text(
            text = text,
            modifier = Modifier.animatedGradientText(
                phase = phase,
                animated = animated,
            ),
            style = style,
            color = Color.White,
            fontWeight = fontWeight,
            textAlign = textAlign,
        )
    }
}

private fun Modifier.animatedGradientText(
    phase: State<Float>,
    animated: Boolean,
): Modifier = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithContent {
    drawContent()
    val progress = if (animated) phase.value else 0.18f
    val angle = progress * (PI * 2.0)
    val orbitX = cos(angle).toFloat()
    val orbitY = sin(angle).toFloat()
    drawRect(
        brush = Brush.linearGradient(
            colors = EnhancedGradient,
            start = Offset(
                x = -size.width * 0.45f + orbitX * size.width * 0.28f,
                y = -size.height + orbitY * size.height * 0.45f,
            ),
            end = Offset(
                x = size.width * 1.45f + orbitX * size.width * 0.28f,
                y = size.height * 2f + orbitY * size.height * 0.45f,
            ),
        ),
        blendMode = BlendMode.SrcIn,
    )
}

@Composable
private fun OnboardingProgress(
    currentPage: Int,
    phase: State<Float>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        repeat(OnboardingPageCount) { index ->
            Box(
                modifier = Modifier
                    .height(5.dp)
                    .width(if (index == currentPage) 28.dp else 7.dp)
                    .clip(CircleShape)
                    .then(
                        if (index == currentPage) {
                            Modifier.animatedGradientBackground(phase)
                        } else {
                            Modifier.background(Color.White.copy(alpha = 0.22f))
                        },
                    ),
            )
        }
    }
}

private fun Modifier.animatedGradientBackground(phase: State<Float>): Modifier = drawBehind {
    val angle = phase.value * (PI * 2.0)
    val orbitX = cos(angle).toFloat()
    val orbitY = sin(angle).toFloat()
    val travel = size.maxDimension.coerceAtLeast(1f)
    drawRect(
        brush = Brush.linearGradient(
            colors = EnhancedGradient,
            start = Offset(
                x = -travel * 0.35f + orbitX * travel * 0.25f,
                y = -travel * 0.25f + orbitY * travel * 0.20f,
            ),
            end = Offset(
                x = size.width + travel * 0.35f + orbitX * travel * 0.25f,
                y = size.height + travel * 0.25f + orbitY * travel * 0.20f,
            ),
        ),
    )
}

private fun Modifier.animatedGlow(
    phase: State<Float>,
    glowAlpha: Float,
): Modifier = drawBehind {
    val angle = phase.value * (PI * 2.0)
    val orbitX = cos(angle).toFloat()
    val orbitY = sin(angle).toFloat()
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                OnboardingBlue.copy(alpha = glowAlpha),
                OnboardingViolet.copy(alpha = glowAlpha * 0.62f),
                Color.Transparent,
            ),
            center = Offset(
                x = size.width * (0.5f + orbitX * 0.08f),
                y = size.height * (0.5f + orbitY * 0.08f),
            ),
            radius = size.maxDimension * 0.72f,
        ),
        radius = size.maxDimension * 0.72f,
    )
}

private fun gentleEntrance() =
    fadeIn(tween(420, easing = FastOutSlowInEasing)) +
        slideInVertically(tween(480, easing = FastOutSlowInEasing)) { it / 8 }

@Composable
private fun rememberEntranceStage(stageCount: Int): Int {
    var stage by remember { mutableIntStateOf(0) }
    LaunchedEffect(stageCount) {
        repeat(stageCount) { index ->
            delay(if (index == 0) 60 else 72)
            stage = index + 1
        }
    }
    return stage
}
