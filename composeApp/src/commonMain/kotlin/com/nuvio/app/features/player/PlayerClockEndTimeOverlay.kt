package com.nuvio.app.features.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.isTvLayoutProfileEnabled
import kotlinx.coroutines.delay
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.player_clock_ends_at
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToLong

@Composable
internal fun PlayerClockEndTimeOverlay(playbackSnapshot: PlayerPlaybackSnapshot, modifier: Modifier = Modifier) {
    val duration = playbackSnapshot.durationMs.takeIf { it > 0L } ?: return
    val speed = playbackSnapshot.playbackSpeed.takeIf { it > 0f } ?: 1f
    val remainingMs = ((duration - playbackSnapshot.positionMs).coerceAtLeast(0L) / speed).roundToLong()
    val useTvLayout = isTvLayoutProfileEnabled()
    val shape = RoundedCornerShape(if (useTvLayout) 18.dp else 12.dp)
    var clock by remember(remainingMs) { mutableStateOf(PlayerWallClock.snapshotForRemaining(remainingMs)) }
    LaunchedEffect(remainingMs) {
        while (true) {
            clock = PlayerWallClock.snapshotForRemaining(remainingMs)
            delay(1_000)
        }
    }
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.58f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
            .padding(
                horizontal = if (useTvLayout) 22.dp else 14.dp,
                vertical = if (useTvLayout) 18.dp else 12.dp,
            ),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(if (useTvLayout) 6.dp else 3.dp),
    ) {
        Text(
            clock.currentTime,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = if (useTvLayout) 44.sp else 18.sp,
        )
        Text(
            text = stringResource(Res.string.player_clock_ends_at, clock.endTime),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = if (useTvLayout) 26.sp else 11.sp,
        )
    }
}
