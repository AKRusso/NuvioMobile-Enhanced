package com.nuvio.app.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun NuvioNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable NuvioNavigationBarScope.() -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Column(modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = tokens.borders.hairline,
            color = tokens.colors.borderDefault,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(nuvioBottomNavigationBarInsets().asPaddingValues())
                .padding(horizontal = NuvioTokens.Space.s4, vertical = nuvioBottomNavigationExtraVerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap, Alignment.CenterHorizontally),
        ) {
            NuvioNavigationBarScopeImpl(this).content()
        }
    }
}

interface NuvioNavigationBarScope {
    @Composable
    fun NavItem(
        selected: Boolean,
        onClick: () -> Unit,
        icon: ImageVector,
        contentDescription: String?,
        modifier: Modifier = Modifier,
    )

    @Composable
    fun NavItem(
        selected: Boolean,
        onClick: () -> Unit,
        icon: DrawableResource,
        contentDescription: String?,
        modifier: Modifier = Modifier,
    )

    @Composable
    fun NavItem(
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    )

    @Composable
    fun TvModeItem(
        selected: Boolean,
        onClick: () -> Unit,
        label: String,
        contentDescription: String?,
        icon: ImageVector,
        modifier: Modifier = Modifier,
    )
}

private class NuvioNavigationBarScopeImpl(
    private val rowScope: androidx.compose.foundation.layout.RowScope,
) : NuvioNavigationBarScope {

    @Composable
    override fun NavItem(
        selected: Boolean,
        onClick: () -> Unit,
        icon: ImageVector,
        contentDescription: String?,
        modifier: Modifier,
    ) {
        val tokens = MaterialTheme.nuvio
        val iconColor by animateColorAsState(
            targetValue = if (selected) tokens.colors.accent else tokens.colors.textMuted,
        )
        with(rowScope) {
            Icon(
                modifier = modifier
                    .widthIn(max = tokens.components.navItemMaxWidth)
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .clip(tokens.components.navItemShape)
                    .selectable(
                        selected = selected,
                        enabled = true,
                        role = Role.Tab,
                        onClick = onClick,
                    )
                    .padding(NuvioTokens.Space.s10)
                    .size(tokens.components.navIconSize),
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconColor,
            )
        }
    }

    @Composable
    override fun NavItem(
        selected: Boolean,
        onClick: () -> Unit,
        icon: DrawableResource,
        contentDescription: String?,
        modifier: Modifier,
    ) {
        val tokens = MaterialTheme.nuvio
        val iconColor by animateColorAsState(
            targetValue = if (selected) tokens.colors.accent else tokens.colors.textMuted,
        )
        with(rowScope) {
            Icon(
                modifier = modifier
                    .widthIn(max = tokens.components.navItemMaxWidth)
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .clip(tokens.components.navItemShape)
                    .selectable(
                        selected = selected,
                        enabled = true,
                        role = Role.Tab,
                        onClick = onClick,
                    )
                    .padding(NuvioTokens.Space.s10)
                    .size(tokens.components.navIconSize),
                painter = painterResource(icon),
                contentDescription = contentDescription,
                tint = iconColor,
            )
        }
    }

    @Composable
    override fun NavItem(
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier,
        content: @Composable () -> Unit,
    ) {
        val tokens = MaterialTheme.nuvio
        with(rowScope) {
            Box(
                modifier = modifier
                    .widthIn(max = tokens.components.navItemMaxWidth)
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .clip(tokens.components.navItemShape)
                    .selectable(
                        selected = selected,
                        enabled = true,
                        role = Role.Tab,
                        onClick = onClick,
                    )
                    .padding(NuvioTokens.Space.s10),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }

    @Composable
    override fun TvModeItem(
        selected: Boolean,
        onClick: () -> Unit,
        label: String,
        contentDescription: String?,
        icon: ImageVector,
        modifier: Modifier,
    ) {
        with(rowScope) {
            NuvioTvModeButton(
                selected = selected,
                onClick = onClick,
                label = label,
                contentDescription = contentDescription,
                icon = icon,
                modifier = modifier
                    .widthIn(min = 118.dp, max = 156.dp)
                    .weight(1.5f, fill = false),
            )
        }
    }
}

@Composable
fun NuvioTvModeButton(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    contentDescription: String?,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val transition = rememberInfiniteTransition(label = "tv-mode-button")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "tv-mode-button-progress",
    )
    val glowAlpha by transition.animateFloat(
        initialValue = if (selected) 0.52f else 0.28f,
        targetValue = if (selected) 0.86f else 0.58f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "tv-mode-button-glow",
    )
    val sweepStart = -220f + (440f * progress)
    val shape = tokens.shapes.chip
    val backgroundBrush = Brush.linearGradient(
        colorStops = arrayOf(
            0.00f to Color(0xFF5A39F4).copy(alpha = if (selected) 0.96f else 0.82f),
            0.44f to Color(0xFF2578FF).copy(alpha = if (selected) 0.94f else 0.76f),
            1.00f to Color(0xFF13D8F7).copy(alpha = if (selected) 0.78f else 0.54f),
        ),
        start = Offset.Zero,
        end = Offset(260f, 120f),
    )
    val shineBrush = Brush.linearGradient(
        colorStops = arrayOf(
            0.00f to Color.Transparent,
            0.45f to Color.White.copy(alpha = 0.00f),
            0.52f to Color.White.copy(alpha = if (selected) 0.38f else 0.24f),
            0.60f to Color.White.copy(alpha = 0.00f),
            1.00f to Color.Transparent,
        ),
        start = Offset(sweepStart, -20f),
        end = Offset(sweepStart + 180f, 90f),
    )

    Row(
        modifier = modifier
            .heightIn(min = 42.dp)
            .clip(shape)
            .background(backgroundBrush, shape)
            .background(shineBrush, shape)
            .border(1.dp, Color.White.copy(alpha = glowAlpha), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = NuvioTokens.Space.s12, vertical = NuvioTokens.Space.s10),
        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(NuvioTokens.Space.s18),
            tint = Color.White,
        )
        Text(
            text = "BETA",
            maxLines = 1,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(shape)
                .background(Color.White.copy(alpha = 0.18f), shape)
                .border(1.dp, Color.White.copy(alpha = 0.26f), shape)
                .padding(horizontal = NuvioTokens.Space.s6, vertical = 2.dp),
        )
    }
}
