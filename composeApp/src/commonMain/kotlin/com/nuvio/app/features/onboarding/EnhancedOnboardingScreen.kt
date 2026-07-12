package com.nuvio.app.features.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import nuvio.composeapp.generated.resources.nuvio_enhanced_concierge_desc
import nuvio.composeapp.generated.resources.nuvio_enhanced_concierge_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_live_tv_desc
import nuvio.composeapp.generated.resources.nuvio_enhanced_live_tv_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_admin_role
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_boosts_label
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_community_body
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_community_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_developer_role
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_features_body
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_features_title_accent
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_features_title_lead
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_join_discord
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_members_label
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_online_label
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
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            PageProgress(page = page)
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
                    0 -> WelcomePage(accentPhase)
                    1 -> FeaturesPage(accentPhase)
                    2 -> TeamPage()
                    else -> CommunityPage(community, onJoinDiscord)
                }
            }
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
private fun WelcomePage(phase: State<Float>) {
    PageColumn(centered = false) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            Image(
                painter = painterResource(Res.drawable.nuvio_enhanced_onboarding_logo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 38.dp, y = (-24).dp)
                    .size(210.dp)
                    .clip(RoundedCornerShape(bottomStart = 48.dp)),
            )
        }

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
            TransientAccentText(
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

@Composable
private fun FeaturesPage(phase: State<Float>) {
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
            TransientAccentText(
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
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceDark.copy(alpha = 0.88f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, BorderSoft),
        ) {
            Column {
                FeatureListItem(
                    number = "01",
                    icon = Icons.Rounded.Home,
                    tone = AccentBlue,
                    title = Res.string.nuvio_enhanced_smart_resume_title,
                    description = Res.string.nuvio_enhanced_smart_resume_desc,
                )
                FeatureDivider()
                FeatureListItem(
                    number = "02",
                    icon = Icons.Rounded.Star,
                    tone = AccentAmber,
                    title = Res.string.nuvio_enhanced_concierge_title,
                    description = Res.string.nuvio_enhanced_concierge_desc,
                )
                FeatureDivider()
                FeatureListItem(
                    number = "03",
                    icon = Icons.Rounded.Notifications,
                    tone = AccentCyan,
                    title = Res.string.nuvio_enhanced_release_digest_title,
                    description = Res.string.nuvio_enhanced_release_digest_desc,
                )
                FeatureDivider()
                FeatureListItem(
                    number = "04",
                    icon = Icons.Rounded.LiveTv,
                    tone = AccentGreen,
                    title = Res.string.nuvio_enhanced_live_tv_title,
                    description = Res.string.nuvio_enhanced_live_tv_desc,
                )
            }
        }
    }
}

@Composable
private fun FeatureListItem(
    number: String,
    icon: ImageVector,
    tone: Color,
    title: StringResource,
    description: StringResource,
) {
    Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.labelSmall,
            color = tone,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp),
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tone.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tone,
                modifier = Modifier.size(19.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(description),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FeatureDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 76.dp),
        thickness = 1.dp,
        color = BorderSoft.copy(alpha = 0.72f),
    )
}

@Composable
private fun TeamPage() {
    PageColumn(centered = false) {
        Text(
            text = stringResource(Res.string.nuvio_enhanced_onboarding_team_title),
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = stringResource(Res.string.nuvio_enhanced_onboarding_team_body),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceDark.copy(alpha = 0.88f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, BorderSoft),
        ) {
            Column {
                DeveloperProfile(
                    avatar = Res.drawable.onboarding_developer_yesnt,
                    displayName = "yesn't",
                    handle = "@yesnt10",
                    isAdmin = false,
                )
                HorizontalDivider(color = BorderSoft)
                DeveloperProfile(
                    avatar = Res.drawable.onboarding_developer_russo,
                    displayName = "Russo",
                    handle = "@AKRusso",
                    isAdmin = true,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .width(3.dp)
                    .height(42.dp)
                    .background(AccentBlue, CircleShape),
            )
            Text(
                text = stringResource(Res.string.nuvio_enhanced_onboarding_team_note),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun DeveloperProfile(
    avatar: DrawableResource,
    displayName: String,
    handle: String,
    isAdmin: Boolean,
) {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Image(
                painter = painterResource(avatar),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(AccentGreen),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = handle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isAdmin) {
                    RoleBadge(
                        text = stringResource(Res.string.nuvio_enhanced_onboarding_admin_role),
                        color = AccentPink,
                    )
                }
                RoleBadge(
                    text = stringResource(Res.string.nuvio_enhanced_onboarding_developer_role),
                    color = AccentBlue,
                )
            }
        }
    }
}

@Composable
private fun RoleBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.11f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CommunityPage(
    snapshot: EnhancedCommunitySnapshot,
    onJoinDiscord: () -> Unit,
) {
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
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceDark,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, BorderSoft),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ServerIcon(snapshot.iconUrl)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = snapshot.serverName,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = snapshot.serverTag,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(AccentGreen),
                    )
                }

                HorizontalDivider(color = BorderSoft)

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

                if (snapshot.members.isNotEmpty()) {
                    HorizontalDivider(color = BorderSoft)
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(
                            text = stringResource(Res.string.nuvio_enhanced_onboarding_online_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        snapshot.members.take(4).forEach { member ->
                            CommunityMemberRow(member)
                        }
                    }
                }

                DiscordButton(onClick = onJoinDiscord)
            }
        }
    }
}

@Composable
private fun ServerIcon(iconUrl: String?) {
    Box(
        modifier = Modifier
            .size(54.dp)
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
    Column(modifier = modifier) {
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
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(AccentGreen),
        )
    }
}

@Composable
private fun DiscordButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
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
private fun TransientAccentText(
    text: String,
    phase: State<Float>,
    style: TextStyle,
) {
    var animated by remember(text) { mutableStateOf(true) }
    LaunchedEffect(text) {
        delay(1200)
        animated = false
    }
    Box {
        if (animated) {
            Text(
                text = text,
                modifier = Modifier
                    .blur(7.dp)
                    .alpha(0.22f),
                style = style,
                color = AccentViolet,
                fontWeight = FontWeight.Black,
            )
        }
        Text(
            text = text,
            modifier = Modifier.accentGradient(phase, animated),
            style = style,
            color = Color.White,
            fontWeight = FontWeight.Black,
        )
    }
}

private fun Modifier.accentGradient(
    phase: State<Float>,
    animated: Boolean,
): Modifier = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithContent {
    drawContent()
    val progress = if (animated) phase.value else 0.22f
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
