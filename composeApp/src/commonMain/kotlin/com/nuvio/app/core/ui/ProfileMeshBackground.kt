package com.nuvio.app.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp

@Composable
fun ProfileMeshBackground(
    profileColor: Color,
    modifier: Modifier = Modifier,
    secondaryColor: Color? = null,
    tertiaryColor: Color? = null,
) {
    val animatedProfileColor by animateColorAsState(
        targetValue = profileColor,
        animationSpec = tween(durationMillis = 520),
        label = "profileMeshBackgroundColor",
    )
    val baseColor = Color.Black
    val primaryMeshColor = lerp(baseColor, animatedProfileColor, 0.72f)
    val secondaryMeshColor = secondaryColor
        ?: lerp(animatedProfileColor, MaterialTheme.colorScheme.secondary, 0.32f)
    val tertiaryMeshColor = tertiaryColor
        ?: lerp(animatedProfileColor, MaterialTheme.colorScheme.tertiary, 0.28f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val maxDimension = maxOf(size.width, size.height)
                val sweep = Path().apply {
                    moveTo(size.width * -0.16f, size.height * 0.58f)
                    cubicTo(
                        size.width * 0.12f,
                        size.height * 0.05f,
                        size.width * 0.42f,
                        size.height * 0.07f,
                        size.width * 0.55f,
                        size.height * 0.48f,
                    )
                    cubicTo(
                        size.width * 0.68f,
                        size.height * 0.91f,
                        size.width * 0.91f,
                        size.height * 0.9f,
                        size.width * 1.14f,
                        size.height * 0.58f,
                    )
                }
                val upperSweep = Path().apply {
                    moveTo(size.width * -0.12f, size.height * 0.18f)
                    cubicTo(
                        size.width * 0.18f,
                        size.height * 0.34f,
                        size.width * 0.31f,
                        size.height * 0.7f,
                        size.width * 0.56f,
                        size.height * 0.69f,
                    )
                    cubicTo(
                        size.width * 0.79f,
                        size.height * 0.68f,
                        size.width * 0.83f,
                        size.height * 0.37f,
                        size.width * 1.12f,
                        size.height * 0.22f,
                    )
                }

                drawRect(color = baseColor, size = size)
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to primaryMeshColor.copy(alpha = 0.38f),
                            0.4f to primaryMeshColor.copy(alpha = 0.15f),
                            0.74f to primaryMeshColor.copy(alpha = 0.04f),
                            1f to Color.Transparent,
                        ),
                        center = Offset(size.width * 0.08f, size.height * 0.02f),
                        radius = maxDimension * 0.64f,
                    ),
                    size = size,
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to animatedProfileColor.copy(alpha = 0.18f),
                            0.44f to animatedProfileColor.copy(alpha = 0.07f),
                            0.78f to animatedProfileColor.copy(alpha = 0.02f),
                            1f to Color.Transparent,
                        ),
                        center = Offset(size.width * 0.72f, size.height * 0.62f),
                        radius = maxDimension * 0.5f,
                    ),
                    size = size,
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to secondaryMeshColor.copy(alpha = 0.14f),
                            0.5f to secondaryMeshColor.copy(alpha = 0.05f),
                            1f to Color.Transparent,
                        ),
                        center = Offset(size.width * 0.96f, size.height * 0.04f),
                        radius = maxDimension * 0.4f,
                    ),
                    size = size,
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to tertiaryMeshColor.copy(alpha = 0.1f),
                            0.52f to tertiaryMeshColor.copy(alpha = 0.03f),
                            1f to Color.Transparent,
                        ),
                        center = Offset(size.width * 0.18f, size.height * 0.88f),
                        radius = maxDimension * 0.34f,
                    ),
                    size = size,
                )
                val ribbonBrush = Brush.linearGradient(
                    colors = listOf(
                        primaryMeshColor.copy(alpha = 0.08f),
                        secondaryMeshColor.copy(alpha = 0.34f),
                        animatedProfileColor.copy(alpha = 0.12f),
                        tertiaryMeshColor.copy(alpha = 0.28f),
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                )
                drawPath(
                    path = sweep,
                    brush = ribbonBrush,
                    style = Stroke(width = maxDimension * 0.13f, cap = StrokeCap.Round),
                    alpha = 0.22f,
                )
                drawPath(
                    path = sweep,
                    brush = ribbonBrush,
                    style = Stroke(width = maxDimension * 0.035f, cap = StrokeCap.Round),
                    alpha = 0.72f,
                )
                drawPath(
                    path = sweep,
                    color = secondaryMeshColor.copy(alpha = 0.32f),
                    style = Stroke(width = maxDimension * 0.006f, cap = StrokeCap.Round),
                )
                drawPath(
                    path = upperSweep,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.Transparent,
                            tertiaryMeshColor.copy(alpha = 0.32f),
                            secondaryMeshColor.copy(alpha = 0.18f),
                            Color.Transparent,
                        ),
                    ),
                    style = Stroke(width = maxDimension * 0.028f, cap = StrokeCap.Round),
                )
            },
    )
}
