package com.nuvio.app.features.player

import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal actual object PlayerWallClock {
    actual fun snapshotForRemaining(remainingMs: Long): PlayerWallClockSnapshot {
        val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
        val now = LocalTime.now()
        return PlayerWallClockSnapshot(
            currentTime = now.format(formatter),
            endTime = now.plus(Duration.ofMillis(remainingMs.coerceAtLeast(0L))).format(formatter),
        )
    }
}
