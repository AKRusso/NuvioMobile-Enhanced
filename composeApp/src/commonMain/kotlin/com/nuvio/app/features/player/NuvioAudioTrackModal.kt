package com.nuvio.app.features.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.nuvio
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_player_audio_tracks
import nuvio.composeapp.generated.resources.compose_player_no_audio_tracks_available
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NuvioAudioTrackModal(
    visible: Boolean,
    audioTracks: List<AudioTrack>,
    selectedIndex: Int,
    onTrackSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlayerOverlayScaffold(
        visible = visible,
        onDismiss = onDismiss,
        modifier = modifier,
        contentPadding = PaddingValues(start = 52.dp, end = 52.dp, top = 36.dp, bottom = 76.dp),
    ) {
        BoxWithConstraints {
            val railMaxHeight = (maxHeight - 72.dp).coerceAtLeast(120.dp)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .width(320.dp)
                    .padding(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.compose_player_audio_tracks),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Column(
                    modifier = Modifier
                        .heightIn(max = railMaxHeight)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (audioTracks.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.compose_player_no_audio_tracks_available),
                            color = MaterialTheme.nuvio.colors.textMuted,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 20.dp),
                        )
                    } else {
                        audioTracks.forEach { track ->
                            NuvioAudioTrackRow(
                                track = track,
                                selected = track.index == selectedIndex,
                                onClick = { onTrackSelected(track.index) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NuvioAudioTrackRow(
    track: AudioTrack,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) tokens.colors.accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(
            text = localizedTrackDisplayName(track.label, track.language, track.index),
            color = if (selected) tokens.colors.onAccent else Color.White,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        track.language?.takeIf { it.isNotBlank() }?.let { language ->
            Text(
                text = language,
                color = if (selected) tokens.colors.onAccent.copy(alpha = 0.78f) else tokens.colors.textMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
