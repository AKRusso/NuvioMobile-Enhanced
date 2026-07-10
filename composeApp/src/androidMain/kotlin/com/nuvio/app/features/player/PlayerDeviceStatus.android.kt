package com.nuvio.app.features.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal actual fun rememberPlayerDeviceStatus(): PlayerDeviceStatus {
    val context = LocalContext.current.applicationContext
    var status by remember(context) {
        mutableStateOf(readPlayerDeviceStatus(context, readBatteryStatus(context)))
    }

    DisposableEffect(context) {
        val appContext = context
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                status = readPlayerDeviceStatus(appContext, intent)
            }
        }
        val stickyIntent = if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        if (stickyIntent != null) {
            status = readPlayerDeviceStatus(context, stickyIntent)
        }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    LaunchedEffect(context) {
        while (true) {
            status = readPlayerDeviceStatus(context, readBatteryStatus(context))
            delay(PlayerDeviceStatusRefreshMs)
        }
    }

    return status
}

private fun readPlayerDeviceStatus(context: Context, batteryStatus: Intent?): PlayerDeviceStatus =
    PlayerDeviceStatus(
        timeLabel = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(Date()),
        batteryPercent = readBatteryPercent(context, batteryStatus),
        batteryCharging = isBatteryCharging(batteryStatus),
        networkType = readNetworkType(context),
    )

private fun readBatteryStatus(context: Context): Intent? =
    context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

private fun readBatteryPercent(context: Context, batteryStatus: Intent?): Int? {
    val managerPercent = (context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager)
        ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        ?.takeIf { it in 0..100 }
    if (managerPercent != null) return managerPercent
    if (batteryStatus == null) return null

    val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level < 0 || scale <= 0) return null
    return ((level * 100f) / scale).roundToInt().coerceIn(0, 100)
}

private fun isBatteryCharging(batteryStatus: Intent?): Boolean {
    if (batteryStatus == null) return false
    return when (batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
        BatteryManager.BATTERY_STATUS_CHARGING,
        BatteryManager.BATTERY_STATUS_FULL -> true
        else -> false
    }
}

private fun readNetworkType(context: Context): PlayerDeviceNetworkType {
    val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return PlayerDeviceNetworkType.Unknown
    val network = connectivity.activeNetwork ?: return PlayerDeviceNetworkType.Offline
    val capabilities = connectivity.getNetworkCapabilities(network) ?: return PlayerDeviceNetworkType.Unknown
    return when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> PlayerDeviceNetworkType.Wifi
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> PlayerDeviceNetworkType.Cellular
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> PlayerDeviceNetworkType.Unknown
        else -> PlayerDeviceNetworkType.Offline
    }
}

private const val PlayerDeviceStatusRefreshMs = 1_000L
