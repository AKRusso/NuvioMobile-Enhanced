package com.nuvio.app.features.downloads

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nuvio.app.features.addons.AddonStorage
import com.nuvio.app.features.debrid.DebridSettingsStorage
import com.nuvio.app.features.plugins.PluginStorage
import com.nuvio.app.features.profiles.ProfileStorage

class PendingDownloadSourceSearchWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        initializeRuntime(applicationContext)
        val searchId = inputData.getString(searchIdKey) ?: return Result.failure()
        EpisodeDownloadCoordinator.retryPending(searchId)
        return Result.success()
    }

    private fun initializeRuntime(context: Context) {
        AddonStorage.initialize(context)
        PluginStorage.initialize(context)
        DebridSettingsStorage.initialize(context)
        ProfileStorage.initialize(context)
        DownloadsStorage.initialize(context)
        DownloadsExternalFolderPlatform.initialize(context)
        DownloadsPlatformDownloader.initialize(context)
        DownloadsLiveStatusPlatform.initialize(context)
    }

    companion object {
        const val searchIdKey = "pending_download_source_search_id"
    }
}
