package com.nuvio.app.features.player

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Extracts low-resolution frames for timeline scrubbing without touching the active player.
 * Some adaptive streams do not expose frames through the platform retriever, so callers must
 * treat a null result as an unavailable preview rather than as a playback error.
 */
internal class AndroidSeekPreviewFrameExtractor(
    private val sourceUrl: String,
    private val requestHeaders: Map<String, String>,
) {
    private val previewCache = LruCache<Long, PlayerSeekPreview>(18)

    fun extract(positionMs: Long): PlayerSeekPreview? {
        val normalizedPositionMs = positionMs.coerceAtLeast(0L).let { (it / 1_000L) * 1_000L }
        synchronized(previewCache) {
            previewCache.get(normalizedPositionMs)?.let { return it }
        }

        val preview = runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                if (requestHeaders.isEmpty()) {
                    retriever.setDataSource(sourceUrl)
                } else {
                    retriever.setDataSource(sourceUrl, requestHeaders)
                }
                val sourceFrame = retriever.getFrameAtTime(
                    normalizedPositionMs * 1_000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                ) ?: return@runCatching null
                val scaledFrame = sourceFrame.scaleForSeekPreview()
                if (scaledFrame !== sourceFrame) {
                    sourceFrame.recycle()
                }
                PlayerSeekPreview(
                    positionMs = normalizedPositionMs,
                    image = scaledFrame.asImageBitmap(),
                )
            } finally {
                retriever.release()
            }
        }.getOrNull()

        if (preview != null) {
            synchronized(previewCache) {
                previewCache.put(normalizedPositionMs, preview)
            }
        }
        return preview
    }
}

private fun Bitmap.scaleForSeekPreview(): Bitmap {
    val maxWidth = 320
    val maxHeight = 180
    if (width <= maxWidth && height <= maxHeight) return this

    val scale = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
    return Bitmap.createScaledBitmap(
        this,
        (width * scale).toInt().coerceAtLeast(1),
        (height * scale).toInt().coerceAtLeast(1),
        true,
    )
}
