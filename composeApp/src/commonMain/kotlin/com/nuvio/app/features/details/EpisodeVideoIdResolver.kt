package com.nuvio.app.features.details

internal fun resolveCachedEpisodeVideoId(
    meta: MetaDetails?,
    season: Int?,
    episode: Int?,
): String? {
    if (meta == null || season == null || episode == null) return null
    return meta.videos
        .firstOrNull { video -> video.season == season && video.episode == episode }
        ?.id
        ?.takeIf(String::isNotBlank)
}

internal fun episodeListIdentity(episodes: List<MetaVideo>): List<String> =
    episodes.map { video -> "${video.season}:${video.episode}:${video.id}" }
