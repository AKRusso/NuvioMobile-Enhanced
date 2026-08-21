package com.nuvio.app.features.downloads

/** Platform-owned scheduling keeps source searches alive independently from the UI. */
internal expect object PendingDownloadSourceSearchPlatform {
    fun schedule(search: PendingEpisodeDownload)
    fun cancel(searchId: String)
    fun notifyManualChoice(searches: List<PendingEpisodeDownload>)
}
