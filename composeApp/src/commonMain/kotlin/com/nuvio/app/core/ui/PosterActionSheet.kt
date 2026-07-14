package com.nuvio.app.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.PosterShape
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.episodes_cd_watched
import nuvio.composeapp.generated.resources.hero_add_to_library
import nuvio.composeapp.generated.resources.hero_mark_unwatched
import nuvio.composeapp.generated.resources.hero_mark_watched
import nuvio.composeapp.generated.resources.hero_remove_from_library
import nuvio.composeapp.generated.resources.home_view_details
import org.jetbrains.compose.resources.stringResource

@Composable
fun NuvioPosterActionSheet(
    item: MetaPreview?,
    isSaved: Boolean,
    isWatched: Boolean,
    onDismiss: () -> Unit,
    onOpenDetails: () -> Unit,
    onToggleLibrary: () -> Unit,
    onToggleWatched: () -> Unit,
) {
    if (item == null) return
    val artwork = posterSheetArtwork(item)
    var description by remember(item.type, item.id) {
        mutableStateOf(
            MetaDetailsRepository.peek(item.type, item.id)
                ?.description
                ?.trim()
                ?.takeIf { it.isNotBlank() },
        )
    }

    LaunchedEffect(item.type, item.id) {
        val resolvedDescription = (MetaDetailsRepository.peek(item.type, item.id)
            ?: MetaDetailsRepository.fetch(item.type, item.id))
            ?.description
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        if (resolvedDescription != null) {
            description = resolvedDescription
        }
    }

    fun dismissAfter(action: () -> Unit) {
        action()
        onDismiss()
    }

    NuvioMediaActionOverlay(
        artworkUrl = artwork,
        contentDescription = item.name,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PosterSheetHeader(item = item, artwork = artwork)
            val sheetDescription = description
            if (sheetDescription != null) {
                Spacer(modifier = Modifier.height(14.dp))
                PosterSheetDescription(text = sheetDescription)
            }
            Spacer(modifier = Modifier.height(18.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .widthIn(max = 390.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF171717).copy(alpha = 0.96f),
                shadowElevation = 18.dp,
                tonalElevation = 6.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    PosterSheetActionRow(
                        icon = Icons.Default.Info,
                        title = stringResource(Res.string.home_view_details),
                        onClick = { dismissAfter(onOpenDetails) },
                    )
                    PosterSheetDivider()
                    PosterSheetActionRow(
                        icon = if (isSaved) Icons.Default.Check else Icons.Default.Add,
                        title = if (isSaved) {
                            stringResource(Res.string.hero_remove_from_library)
                        } else {
                            stringResource(Res.string.hero_add_to_library)
                        },
                        onClick = { dismissAfter(onToggleLibrary) },
                    )
                    PosterSheetDivider()
                    PosterSheetActionRow(
                        icon = if (isWatched) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                        title = if (isWatched) {
                            stringResource(Res.string.hero_mark_unwatched)
                        } else {
                            stringResource(Res.string.hero_mark_watched)
                        },
                        onClick = { dismissAfter(onToggleWatched) },
                    )
                }
            }
        }
    }
}

@Composable
fun NuvioWatchedBadge(
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    Box(
        modifier = modifier
            .size(NuvioTokens.Icon.md)
            .clip(tokens.shapes.avatar)
            .background(tokens.colors.accent),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = stringResource(Res.string.episodes_cd_watched),
            tint = tokens.colors.onAccent,
            modifier = Modifier.size(NuvioTokens.Icon.xs),
        )
    }
}

@Composable
fun NuvioAnimatedWatchedBadge(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        NuvioWatchedBadge()
    }
}

@Composable
fun BoxScope.NuvioPosterWatchedOverlay(
    isWatched: Boolean,
    modifier: Modifier = Modifier,
    padding: Dp = NuvioTokens.Space.s6,
) {
    NuvioAnimatedWatchedBadge(
        isVisible = isWatched,
        modifier = modifier
            .align(Alignment.TopEnd)
            .padding(padding),
    )
}

@Composable
private fun PosterSheetHeader(
    item: MetaPreview,
    artwork: String?,
) {
    val imageShape = item.posterShape
    val imageWidthFraction = posterSheetImageWidthFraction(imageShape)
    val imageMaxWidth = posterSheetImageMaxWidth(imageShape)
    val imageAspectRatio = posterSheetImageAspectRatio(imageShape)
    val subtitle = item.releaseInfo
        ?.takeIf { it.isNotBlank() }
        ?.let { formatReleaseDateForDisplay(it) }
        ?: item.type.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(imageWidthFraction)
                .widthIn(max = imageMaxWidth)
                .aspectRatio(imageAspectRatio)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E1E1E)),
            contentAlignment = Alignment.Center,
        ) {
            if (artwork != null) {
                AsyncImage(
                    model = artwork,
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.12f),
                                    Color.Black.copy(alpha = 0.50f),
                                ),
                            ),
                        ),
                )
            } else {
                Text(
                    text = item.name,
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.64f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun posterSheetArtwork(item: MetaPreview): String? =
    item.poster?.takeIf { it.isNotBlank() }
        ?: item.banner?.takeIf { it.isNotBlank() }

private fun posterSheetImageWidthFraction(shape: PosterShape): Float =
    when (shape) {
        PosterShape.Poster -> 0.58f
        PosterShape.Square -> 0.72f
        PosterShape.Landscape -> 0.94f
    }

private fun posterSheetImageMaxWidth(shape: PosterShape): Dp =
    when (shape) {
        PosterShape.Poster -> 260.dp
        PosterShape.Square -> 320.dp
        PosterShape.Landscape -> 430.dp
    }

private fun posterSheetImageAspectRatio(shape: PosterShape): Float =
    when (shape) {
        PosterShape.Poster -> 0.675f
        PosterShape.Square -> 1f
        PosterShape.Landscape -> 16f / 9f
    }

@Composable
private fun PosterSheetDescription(
    text: String,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.86f)
            .widthIn(max = 390.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.Black.copy(alpha = 0.34f),
        tonalElevation = 0.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.74f),
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PosterSheetActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(26.dp),
            tint = Color.White,
        )
    }
}

@Composable
private fun PosterSheetDivider() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.06f),
        thickness = 1.dp,
    )
}
