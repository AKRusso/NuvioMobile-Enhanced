package com.nuvio.app.features.downloads

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nuvio.app.MainActivity
import com.nuvio.app.core.deeplink.buildDownloadsDeepLinkUrl

internal object PendingDownloadSourceSearchNotification {
    private const val channelId = "download_source_search"
    private const val notificationId = 77_432

    fun show(context: Context, searches: List<PendingEpisodeDownload>) {
        if (searches.isEmpty() || !canPostNotifications(context)) return
        ensureChannel(context)
        val count = searches.size
        val title = if (count == 1) {
            "A download needs a source"
        } else {
            "$count downloads need sources"
        }
        val message = if (count == 1) {
            "Choose a provider and quality to continue the pending episode."
        } else {
            "Choose providers and qualities for the pending episodes."
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = android.net.Uri.parse(buildDownloadsDeepLinkUrl())
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        NotificationManagerCompat.from(context).notify(
            notificationId,
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(com.nuvio.app.R.drawable.ic_notification_small)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build(),
        )
        DownloadsRepository.markPendingSourceSearchPrompted(searches.map(PendingEpisodeDownload::id))
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(channelId) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                channelId,
                "Download source alerts",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Alerts when pending episode downloads need a source choice."
            },
        )
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
