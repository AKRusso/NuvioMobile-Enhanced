package com.nuvio.app.features.downloads

internal actual object PendingDownloadSourceSearchPlatform {
    actual fun schedule(search: PendingEpisodeDownload) = Unit

    actual fun cancel(searchId: String) = Unit

    actual fun notifyManualChoice(searches: List<PendingEpisodeDownload>) = Unit
}
