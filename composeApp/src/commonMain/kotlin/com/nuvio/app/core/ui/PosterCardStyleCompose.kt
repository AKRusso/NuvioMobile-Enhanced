package com.nuvio.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kotlin.math.roundToInt

@Composable
internal fun rememberPosterCardStyleUiState(): PosterCardStyleUiState {
    PosterCardStyleRepository.ensureLoaded()
    val uiState by PosterCardStyleRepository.uiState.collectAsState()
    val tvLayout = LocalTvLayoutProfile.current
    return remember(uiState, tvLayout) {
        if (!tvLayout.enabled) {
            uiState
        } else {
            val scale = 2.3f
            uiState.copy(
                widthDp = (uiState.widthDp * scale).roundToInt(),
                heightDp = (uiState.heightDp * scale).roundToInt(),
                cornerRadiusDp = (uiState.cornerRadiusDp * scale).roundToInt(),
            )
        }
    }
}
