package com.nuvio.app.core.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage

@Composable
fun NuvioMediaActionOverlay(
    artworkUrl: Any?,
    contentDescription: String?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dismissInteraction = remember { MutableInteractionSource() }
    val contentInteraction = remember { MutableInteractionSource() }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        entered = true
    }
    val scrimProgress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "mediaActionOverlayScrim",
    )
    val contentProgress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "mediaActionOverlayContent",
    )
    PlatformBackHandler(enabled = true, onBack = onDismissRequest)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = nuvioMediaActionOverlayDialogProperties(),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            if (artworkUrl != null) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.12f
                            scaleY = 1.12f
                            alpha = scrimProgress
                        }
                        .blur(34.dp),
                    contentScale = ContentScale.Crop,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.58f * scrimProgress)),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.18f * scrimProgress),
                                Color.Black.copy(alpha = 0.22f * scrimProgress),
                                Color.Black.copy(alpha = 0.74f * scrimProgress),
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = dismissInteraction,
                        indication = null,
                        onClick = onDismissRequest,
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = contentProgress
                            scaleX = 0.985f + (0.015f * contentProgress)
                            scaleY = 0.985f + (0.015f * contentProgress)
                            translationY = (1f - contentProgress) * 18f
                        }
                        .clickable(
                            interactionSource = contentInteraction,
                            indication = null,
                            onClick = {},
                        ),
                ) {
                    content()
                }
            }
        }
    }
}
