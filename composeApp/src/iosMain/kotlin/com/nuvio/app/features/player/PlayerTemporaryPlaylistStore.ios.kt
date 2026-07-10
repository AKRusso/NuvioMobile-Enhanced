package com.nuvio.app.features.player

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.usePinned
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

internal actual fun writeTemporaryHlsPlaylist(playlistText: String): String? {
    val directory = NSTemporaryDirectory().trimEnd('/')
    val filename = "nuvio_player_quality_${NSUUID().UUIDString}.m3u8"
    val path = "$directory/$filename"
    val success = playlistText.encodeToByteArray().writeToFile(path)
    return if (success) "file://$path" else null
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.writeToFile(path: String): Boolean =
    usePinned { pinned ->
        val file = fopen(path, "wb") ?: return false
        try {
            val written = fwrite(
                pinned.addressOf(0),
                1.convert(),
                size.convert(),
                file,
            )
            written.toLong() == size.toLong()
        } finally {
            fclose(file)
        }
    }
