package com.nuvio.app.features.settings

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.nuvio_enhanced_supporter
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val BadgeHeightRatio = 132f / 344f
private const val BadgeVerticalOffsetRatio = -12f / 344f
private const val BadgeGradientWidthMultiplier = 2.4f
private const val BadgeSweepHalfDurationMs = 2_750

@Composable
internal fun MemberBrandWordmark(
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val fullLabel = stringResource(Res.string.nuvio_enhanced_supporter)
    var badgeSize by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "enhancedSupporterGradient")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = BadgeSweepHalfDurationMs,
                easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f),
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "enhancedSupporterGradientProgress",
    )
    val brush = remember(badgeSize, progress) {
        enhancedSupporterBrush(badgeSize, progress)
    }

    Row(
        modifier = modifier
            .height(height)
            .semantics(mergeDescendants = true) { contentDescription = fullLabel },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppBrandWordmark(modifier = Modifier.height(height))
        Spacer(modifier = Modifier.width(height * 0.14f))
        Text(
            text = "Enhanced Supporter",
            style = TextStyle(
                brush = brush,
                fontSize = (height.value * BadgeHeightRatio).sp,
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier
                .offset(y = height * BadgeVerticalOffsetRatio)
                .onSizeChanged { badgeSize = it },
        )
    }
}

private fun enhancedSupporterBrush(size: IntSize, progress: Float): Brush {
    val textWidth = size.width.toFloat().coerceAtLeast(1f)
    val textHeight = size.height.toFloat().coerceAtLeast(1f)
    val gradientWidth = textWidth * BadgeGradientWidthMultiplier
    val gradientLeft = -(gradientWidth - textWidth) * progress.coerceIn(0f, 1f)
    val angleRadians = 100f * (PI.toFloat() / 180f)
    val directionX = sin(angleRadians)
    val directionY = -cos(angleRadians)
    val lineLength = abs(gradientWidth * directionX) + abs(textHeight * directionY)
    val center = Offset(gradientLeft + gradientWidth / 2f, textHeight / 2f)
    val halfVector = Offset(directionX * lineLength / 2f, directionY * lineLength / 2f)

    return Brush.linearGradient(
        colorStops = arrayOf(
            0f to Color(0xFF91A8FF),
            0.52f to Color(0xFFF08BD8),
            0.78f to Color(0xFFFF9B8E),
            1f to Color(0xFF91A8FF),
        ),
        start = center - halfVector,
        end = center + halfVector,
    )
}
