package com.nuvio.app.features.livetv

data class LiveTvChannel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val tvgId: String? = null,
    val logoUrl: String? = null,
    val group: String = "",
    val headers: Map<String, String> = emptyMap(),
)

data class LiveTvRecentChannel(
    val streamUrl: String,
    val name: String,
    val logoUrl: String? = null,
    val group: String = "",
    val tvgId: String? = null,
)

data class LiveTvProgramme(
    val title: String,
    val startEpochMs: Long,
    val stopEpochMs: Long,
    val timeLabel: String,
)

data class LiveTvUiState(
    val sourceUrl: String = "",
    val channels: List<LiveTvChannel> = emptyList(),
    val currentProgrammes: Map<String, LiveTvProgramme> = emptyMap(),
    val recentChannel: LiveTvRecentChannel? = null,
    val favoriteUrls: Set<String> = emptySet(),
    val isEpgLoading: Boolean = false,
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false,
    val errorMessage: String? = null,
)
