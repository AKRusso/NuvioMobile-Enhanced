package com.nuvio.app.features.downloads

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder

class DownloadsForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        initializeDownloadRuntime()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        initializeDownloadRuntime()

        DownloadsRepository.ensureLoaded()
        val items = DownloadsRepository.uiState.value.items
        startForeground(
            DownloadsLiveStatusPlatform.foregroundNotificationId(),
            DownloadsLiveStatusPlatform.buildForegroundNotification(this, items),
        )

        if (items.none { it.status == DownloadStatus.Downloading }) {
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun initializeDownloadRuntime() {
        val context = applicationContext
        DownloadsStorage.initialize(context)
        DownloadsPlatformDownloader.initialize(context)
        DownloadsLiveStatusPlatform.initialize(context)
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, DownloadsForegroundService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DownloadsForegroundService::class.java))
        }
    }
}
