package com.nuvio.app.features.details

data class MetaSeriesStats(
    val seasonCount: Int,
    val episodeCount: Int,
)

fun MetaDetails.mainSeriesStats(): MetaSeriesStats? {
    val mainEpisodes = videos
        .asSequence()
        .filter { video ->
            (video.season ?: 0) > 0 && (video.episode ?: 0) > 0
        }
        .distinctBy { video -> "${video.season}:${video.episode}" }
        .toList()

    val seasonCount = mainEpisodes
        .mapNotNull { it.season }
        .distinct()
        .size
    val episodeCount = mainEpisodes.size

    return if (seasonCount > 0 && episodeCount > 0) {
        MetaSeriesStats(seasonCount = seasonCount, episodeCount = episodeCount)
    } else {
        null
    }
}
