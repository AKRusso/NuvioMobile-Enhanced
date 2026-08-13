package com.nuvio.app.features.downloads

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager

class DownloadsForegroundService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

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
            releaseWakeLock()
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }

        acquireWakeLock()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseWakeLock()
        DownloadsLiveStatusPlatform.clearForegroundNotification(applicationContext)
        stopForegroundCompat()
        super.onDestroy()
    }

    private fun initializeDownloadRuntime() {
        val context = applicationContext
        DownloadsStorage.initialize(context)
        DownloadsExternalFolderPlatform.initialize(context)
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

    private fun acquireWakeLock() {
        val lock = wakeLock ?: run {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:downloads").also {
                it.setReferenceCounted(false)
                wakeLock = it
            }
        }
        if (!lock.isHeld) {
            lock.acquire()
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
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
