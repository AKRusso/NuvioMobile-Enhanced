package com.nuvio.app.features.player

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle

internal actual object PlayerWallClock {
    actual fun snapshotForRemaining(remainingMs: Long): PlayerWallClockSnapshot {
        val formatter = NSDateFormatter().apply {
            dateStyle = NSDateFormatterNoStyle
            timeStyle = NSDateFormatterShortStyle
        }
        val now = NSDate()
        return PlayerWallClockSnapshot(
            currentTime = formatter.stringFromDate(now),
            endTime = formatter.stringFromDate(
                NSDate(
                    timeIntervalSinceReferenceDate = now.timeIntervalSinceReferenceDate +
                        remainingMs.coerceAtLeast(0L).toDouble() / 1000.0,
                ),
            ),
        )
    }
}
