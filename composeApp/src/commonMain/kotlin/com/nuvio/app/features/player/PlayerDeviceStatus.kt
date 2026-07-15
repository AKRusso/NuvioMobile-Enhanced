package com.nuvio.app.features.player

import androidx.compose.runtime.Composable

internal enum class PlayerDeviceNetworkType {
    Wifi,
    Cellular,
    Offline,
    Unknown,
}

internal data class PlayerDeviceStatus(
    val timeLabel: String,
    val currentTimeMillis: Long,
    val batteryPercent: Int?,
    val batteryCharging: Boolean,
    val networkType: PlayerDeviceNetworkType,
)

@Composable
internal expect fun rememberPlayerDeviceStatus(): PlayerDeviceStatus
