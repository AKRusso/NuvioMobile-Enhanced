package com.nuvio.app.features.livetv

data class LiveTvChannel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val group: String = "",
    val headers: Map<String, String> = emptyMap(),
)

data class LiveTvUiState(
    val sourceUrl: String = "",
    val channels: List<LiveTvChannel> = emptyList(),
    val favoriteUrls: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false,
    val errorMessage: String? = null,
)
