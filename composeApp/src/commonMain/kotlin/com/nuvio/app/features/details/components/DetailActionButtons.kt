package com.nuvio.app.features.details.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.AppIconResource
import com.nuvio.app.core.ui.appIconPainter
import com.nuvio.app.core.ui.nuvioKeyboardFocusIndicator
import com.nuvio.app.core.ui.rememberActionAccentStyle
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_play
import nuvio.composeapp.generated.resources.details_actions_menu_label
import org.jetbrains.compose.resources.stringResource

data class DetailSecondaryAction(
    val label: String,
    val icon: ImageVector,
    val isActive: Boolean = false,
    val onClick: () -> Unit = {},
    val onLongClick: (() -> Unit)? = null,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailActionButtons(
    modifier: Modifier = Modifier,
    playLabel: String = stringResource(Res.string.action_play),
    downloadAction: DetailSecondaryAction? = null,
    playSideAction: DetailSecondaryAction? = null,
    secondaryActions: List<DetailSecondaryAction> = emptyList(),
    actionsMenuLabel: String = stringResource(Res.string.details_actions_menu_label),
    isTablet: Boolean = false,
    onPlayClick: () -> Unit = {},
    onPlayLongClick: (() -> Unit)? = null,
    playFocusRequester: FocusRequester? = null,
) {
    val playPainter = appIconPainter(AppIconResource.PlayerPlay)
    val buttonHeight = if (isTablet) 56.dp else 52.dp
    val playShape = RoundedCornerShape(40.dp)
    val hapticFeedback = LocalHapticFeedback.current
    val playAccent = rememberActionAccentStyle()
    var actionsExpanded by remember { mutableStateOf(false) }
    val menuProgress by animateFloatAsState(
        targetValue = if (actionsExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "detail_action_menu_progress",
    )
    val expandableActions = if (downloadAction == null) {
        listOfNotNull(playSideAction) + secondaryActions
    } else {
        secondaryActions
    }
    val spacing = 12.dp

    Column(
        modifier = modifier
            .widthIn(max = if (isTablet) 520.dp else 420.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(buttonHeight)
                    .clip(playShape)
                    .background(playAccent.brush, playShape),
                shape = playShape,
                color = Color.Transparent,
                contentColor = playAccent.contentColor,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (playFocusRequester != null) {
                                Modifier.focusRequester(playFocusRequester)
                            } else {
                                Modifier
                            },
                        )
                        .nuvioKeyboardFocusIndicator(playShape)
                        .combinedClickable(
                            onClick = onPlayClick,
                            onLongClick = onPlayLongClick,
                            role = Role.Button,
                        )
                        .height(buttonHeight),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = playPainter,
                        contentDescription = null,
                        modifier = Modifier.size(if (isTablet) 20.dp else 18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = playLabel,
                        style = if (isTablet) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (downloadAction != null) {
                playSideAction?.let { action ->
                    Spacer(modifier = Modifier.width(spacing))
                    DetailCompactAction(
                        action = action,
                        size = buttonHeight,
                        hapticFeedback = hapticFeedback,
                    )
                }
            } else {
                ExpandableDetailActions(
                    actions = expandableActions,
                    expanded = actionsExpanded,
                    progress = menuProgress,
                    buttonSize = buttonHeight,
                    spacing = spacing,
                    actionsMenuLabel = actionsMenuLabel,
                    onExpandedChange = { actionsExpanded = it },
                )
            }
        }

        downloadAction?.let { action ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DetailWideAction(
                    action = action,
                    height = buttonHeight,
                    modifier = Modifier.weight(1f),
                    hapticFeedback = hapticFeedback,
                )
                ExpandableDetailActions(
                    actions = expandableActions,
                    expanded = actionsExpanded,
                    progress = menuProgress,
                    buttonSize = buttonHeight,
                    spacing = spacing,
                    actionsMenuLabel = actionsMenuLabel,
                    onExpandedChange = { actionsExpanded = it },
                )
            }
        }
    }
}

@Composable
private fun ExpandableDetailActions(
    actions: List<DetailSecondaryAction>,
    expanded: Boolean,
    progress: Float,
    buttonSize: Dp,
    spacing: Dp,
    actionsMenuLabel: String,
    onExpandedChange: (Boolean) -> Unit,
) {
    if (actions.isEmpty()) return
    val hapticFeedback = LocalHapticFeedback.current

    Spacer(modifier = Modifier.width(spacing))
    actions.forEachIndexed { index, action ->
        Box(
            modifier = Modifier
                .width(buttonSize * progress)
                .height(buttonSize)
                .graphicsLayer { clip = true },
            contentAlignment = Alignment.Center,
        ) {
            if (expanded || progress > 0.01f) {
                DetailIconAction(
                    action = action,
                    progress = progress,
                    size = buttonSize,
                    hapticFeedback = hapticFeedback,
                )
            }
        }
        if (index != actions.lastIndex) {
            Spacer(modifier = Modifier.width(spacing * progress))
        }
    }
    Spacer(modifier = Modifier.width(spacing * progress))

    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = Modifier
            .size(buttonSize)
            .clip(shape),
        shape = shape,
        color = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
        contentColor = if (expanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    ) {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .nuvioKeyboardFocusIndicator(shape)
                .clickable(role = Role.Button) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onExpandedChange(!expanded)
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = actionsMenuLabel,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { rotationZ = 90f * progress },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DetailWideAction(
    action: DetailSecondaryAction,
    height: Dp,
    hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = modifier
            .height(height)
            .clip(shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .nuvioKeyboardFocusIndicator(shape)
                .combinedClickable(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        action.onClick()
                    },
                    onLongClick = action.onLongClick,
                    role = Role.Button,
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = action.icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = action.label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DetailCompactAction(
    action: DetailSecondaryAction,
    size: Dp,
    hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback,
) {
    Surface(
        modifier = Modifier
            .size(size)
            .clip(CircleShape),
        shape = CircleShape,
        color = if (action.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (action.isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .nuvioKeyboardFocusIndicator(CircleShape)
                .combinedClickable(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        action.onClick()
                    },
                    onLongClick = action.onLongClick,
                    role = Role.Button,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = action.icon, contentDescription = action.label, modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DetailIconAction(
    action: DetailSecondaryAction,
    progress: Float,
    size: Dp,
    hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback,
) {
    Surface(
        modifier = Modifier
            .graphicsLayer {
                alpha = progress
                scaleX = 0.86f + (0.14f * progress)
                scaleY = 0.86f + (0.14f * progress)
            }
            .clip(CircleShape),
        shape = CircleShape,
        color = if (action.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (action.isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .nuvioKeyboardFocusIndicator(CircleShape)
                .combinedClickable(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        action.onClick()
                    },
                    onLongClick = action.onLongClick,
                    role = Role.Button,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = action.icon, contentDescription = action.label, modifier = Modifier.size(21.dp))
        }
    }
}
