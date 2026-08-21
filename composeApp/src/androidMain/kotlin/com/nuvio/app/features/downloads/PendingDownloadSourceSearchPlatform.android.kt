package com.nuvio.app.features.downloads

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

internal actual object PendingDownloadSourceSearchPlatform {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    actual fun schedule(search: PendingEpisodeDownload) {
        val context = appContext ?: return
        val delayMs = (search.nextAttemptAtEpochMs - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<PendingDownloadSourceSearchWorker>()
            .setInputData(workDataOf(PendingDownloadSourceSearchWorker.searchIdKey to search.id))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(search.id),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    actual fun cancel(searchId: String) {
        val context = appContext ?: return
        WorkManager.getInstance(context).cancelUniqueWork(workName(searchId))
    }

    actual fun notifyManualChoice(searches: List<PendingEpisodeDownload>) {
        val context = appContext ?: return
        PendingDownloadSourceSearchNotification.show(context, searches)
    }

    private fun workName(searchId: String): String = "pending_download_source_search_$searchId"
}
