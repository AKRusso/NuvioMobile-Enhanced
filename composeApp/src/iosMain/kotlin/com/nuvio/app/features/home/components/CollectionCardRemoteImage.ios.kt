package com.nuvio.app.features.home.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import platform.CoreFoundation.CFDataCreate
import platform.CoreGraphics.CGImageRef
import platform.CoreGraphics.CGImageRelease
import platform.ImageIO.CGImageSourceCreateImageAtIndex
import platform.ImageIO.CGImageSourceCreateWithData
import platform.ImageIO.CGImageSourceGetCount
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIViewContentMode

private val animatedGifHttpClient = HttpClient(Darwin) {
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 15_000
    }
}
private val animatedGifDecodeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
private const val MaxCachedAnimatedGifs = 10
private const val MaxAnimatedGifBytes = 20 * 1024 * 1024
private const val DefaultGifFrameDelayCentiseconds = 10
private val animatedGifCache = mutableMapOf<String, AnimatedGif>()
private val animatedGifCacheOrder = mutableListOf<String>()
private val animatedGifInFlight = mutableMapOf<String, Deferred<AnimatedGif?>>()

private data class AnimatedGif(
    val images: List<UIImage>,
    val durationSeconds: Double,
)

private data class GifFrame(
    val image: UIImage,
    val delayCentiseconds: Int,
)

private data class ExpandedGifFrames(
    val images: List<UIImage>,
    val tickCentiseconds: Int,
)

private class AnimatedGifImageViewHolder {
    var imageView: UIImageView? = null
    var currentGif: AnimatedGif? = null

    fun clear() {
        imageView?.stopAnimating()
        imageView?.animationImages = null
        imageView?.image = null
        imageView = null
        currentGif = null
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun CollectionCardRemoteImage(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier,
    contentScale: ContentScale,
    animateIfPossible: Boolean,
) {
    if (!animateIfPossible) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
        return
    }

    var animatedGif by remember(imageUrl) { mutableStateOf(cachedAnimatedGif(imageUrl)) }
    var loadFailed by remember(imageUrl) { mutableStateOf(false) }

    LaunchedEffect(imageUrl) {
        loadFailed = false
        animatedGif = cachedAnimatedGif(imageUrl)
        if (animatedGif == null) {
            animatedGif = loadAnimatedGif(imageUrl)
            loadFailed = animatedGif == null
        }
    }

    val gif = animatedGif
    if (gif == null || loadFailed) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
        return
    }

    val imageViewHolder = remember(imageUrl) { AnimatedGifImageViewHolder() }
    DisposableEffect(imageUrl) {
        onDispose {
            imageViewHolder.clear()
        }
    }

    UIKitView(
        modifier = modifier,
        interactive = false,
        factory = {
            UIImageView().apply {
                clipsToBounds = true
                userInteractionEnabled = false
                imageViewHolder.imageView = this
                updateContentMode(contentScale)
                updateAnimatedGif(gif, imageViewHolder)
            }
        },
        update = { imageView ->
            imageViewHolder.imageView = imageView
            imageView.updateContentMode(contentScale)
            imageView.updateAnimatedGif(gif, imageViewHolder)
        },
    )
}

private fun UIImageView.updateContentMode(contentScale: ContentScale) {
    contentMode = when (contentScale) {
        ContentScale.Crop -> UIViewContentMode.UIViewContentModeScaleAspectFill
        ContentScale.FillBounds -> UIViewContentMode.UIViewContentModeScaleToFill
        ContentScale.Fit,
        ContentScale.Inside -> UIViewContentMode.UIViewContentModeScaleAspectFit
        else -> UIViewContentMode.UIViewContentModeScaleAspectFill
    }
}

private fun UIImageView.updateAnimatedGif(gif: AnimatedGif, holder: AnimatedGifImageViewHolder) {
    if (holder.currentGif !== gif) {
        stopAnimating()
        animationImages = gif.images
        animationDuration = gif.durationSeconds.coerceAtLeast(0.02)
        animationRepeatCount = 0
        image = gif.images.firstOrNull()
        holder.currentGif = gif
    }
    startAnimating()
}

private fun cachedAnimatedGif(imageUrl: String): AnimatedGif? {
    val gif = animatedGifCache[imageUrl] ?: return null
    animatedGifCacheOrder.remove(imageUrl)
    animatedGifCacheOrder.add(imageUrl)
    return gif
}

private fun storeAnimatedGif(imageUrl: String, gif: AnimatedGif) {
    animatedGifCache[imageUrl] = gif
    animatedGifCacheOrder.remove(imageUrl)
    animatedGifCacheOrder.add(imageUrl)

    while (animatedGifCacheOrder.size > MaxCachedAnimatedGifs) {
        val eldestKey = animatedGifCacheOrder.removeAt(0)
        animatedGifCache.remove(eldestKey)
    }
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun loadAnimatedGif(imageUrl: String): AnimatedGif? {
    cachedAnimatedGif(imageUrl)?.let { return it }

    val request = animatedGifInFlight[imageUrl] ?: animatedGifDecodeScope.async {
        runCatching {
            val bytes = animatedGifHttpClient.get(imageUrl).body<ByteArray>()
            if (bytes.isEmpty() || bytes.size > MaxAnimatedGifBytes || !bytes.hasGifHeader()) {
                return@runCatching null
            }
            decodeAnimatedGif(bytes)
        }.getOrNull()
    }.also { animatedGifInFlight[imageUrl] = it }

    val gif = try {
        request.await()
    } finally {
        if (animatedGifInFlight[imageUrl] === request) {
            animatedGifInFlight.remove(imageUrl)
        }
    }

    if (gif != null) {
        storeAnimatedGif(imageUrl, gif)
    }

    return gif
}

@OptIn(ExperimentalForeignApi::class)
private fun decodeAnimatedGif(bytes: ByteArray): AnimatedGif? {
    val data = bytes.toCFData() ?: return null
    val source = CGImageSourceCreateWithData(data, null) ?: return null
    val frameCount = CGImageSourceGetCount(source).toInt()
    if (frameCount <= 1) return null

    val frameDurations = parseGifFrameDurations(bytes)
    val frames = mutableListOf<GifFrame>()

    for (index in 0 until frameCount) {
        val imageRef: CGImageRef = CGImageSourceCreateImageAtIndex(source, index.toULong(), null) ?: continue
        try {
            frames += GifFrame(
                image = UIImage.imageWithCGImage(imageRef),
                delayCentiseconds = frameDurations.getOrNull(index)
                    ?.coerceAtLeast(2)
                    ?: DefaultGifFrameDelayCentiseconds,
            )
        } finally {
            CGImageRelease(imageRef)
        }
    }
    if (frames.size <= 1) return null

    val expanded = expandedGifFrames(frames)
    return AnimatedGif(
        images = expanded.images,
        durationSeconds = (expanded.images.size * expanded.tickCentiseconds) / 100.0,
    )
}

private fun expandedGifFrames(frames: List<GifFrame>): ExpandedGifFrames {
    val normalizedDelays = frames.map { it.delayCentiseconds.coerceAtLeast(2) }
    val tickCentiseconds = normalizedDelays.reduce(::greatestCommonDivisor)
    val expandedFrames = ArrayList<UIImage>(normalizedDelays.sumOf { it / tickCentiseconds })

    frames.forEach { frame ->
        val repeatCount = (frame.delayCentiseconds.coerceAtLeast(2) / tickCentiseconds).coerceAtLeast(1)
        repeat(repeatCount) {
            expandedFrames.add(frame.image)
        }
    }

    return ExpandedGifFrames(
        images = expandedFrames,
        tickCentiseconds = tickCentiseconds,
    )
}

private fun parseGifFrameDurations(bytes: ByteArray): List<Int> {
    if (bytes.size < 13 || !bytes.hasGifHeader()) return emptyList()

    var index = 6
    if (index + 7 > bytes.size) return emptyList()

    val logicalScreenPacked = bytes[index + 4].unsignedInt()
    index += 7

    if (logicalScreenPacked and 0x80 != 0) {
        val globalColorTableSize = 3 * (1 shl ((logicalScreenPacked and 0x07) + 1))
        index += globalColorTableSize
    }

    val frameDurations = mutableListOf<Int>()
    var pendingDelayCentiseconds: Int? = null

    while (index < bytes.size) {
        when (bytes[index].unsignedInt()) {
            0x21 -> {
                if (index + 1 >= bytes.size) break
                val extensionLabel = bytes[index + 1].unsignedInt()
                if (extensionLabel == 0xF9) {
                    if (index + 7 >= bytes.size) break
                    val delayCentiseconds = bytes.readUnsignedShort(index + 4)
                    pendingDelayCentiseconds = if (delayCentiseconds <= 1) {
                        DefaultGifFrameDelayCentiseconds
                    } else {
                        delayCentiseconds
                    }
                    index += 8
                } else {
                    index += 2
                    index = bytes.skipGifSubBlocks(index)
                }
            }

            0x2C -> {
                if (index + 9 >= bytes.size) break
                val imageDescriptorPacked = bytes[index + 9].unsignedInt()
                index += 10

                if (imageDescriptorPacked and 0x80 != 0) {
                    val localColorTableSize = 3 * (1 shl ((imageDescriptorPacked and 0x07) + 1))
                    index += localColorTableSize
                }

                if (index >= bytes.size) break
                index += 1
                index = bytes.skipGifSubBlocks(index)

                frameDurations += pendingDelayCentiseconds ?: DefaultGifFrameDelayCentiseconds
                pendingDelayCentiseconds = null
            }

            0x3B -> break
            else -> break
        }
    }

    return frameDurations
}

private fun greatestCommonDivisor(a: Int, b: Int): Int {
    var x = a
    var y = b
    while (y != 0) {
        val temp = x % y
        x = y
        y = temp
    }
    return x.coerceAtLeast(1)
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toCFData() =
    usePinned { pinned ->
        CFDataCreate(
            allocator = null,
            bytes = pinned.addressOf(0).reinterpret(),
            length = size.toLong(),
        )
    }

private fun ByteArray.hasGifHeader(): Boolean =
    size >= 6 &&
        this[0] == 'G'.code.toByte() &&
        this[1] == 'I'.code.toByte() &&
        this[2] == 'F'.code.toByte() &&
        this[3] == '8'.code.toByte() &&
        (this[4] == '7'.code.toByte() || this[4] == '9'.code.toByte()) &&
        this[5] == 'a'.code.toByte()

private fun ByteArray.skipGifSubBlocks(startIndex: Int): Int {
    var index = startIndex
    while (index < size) {
        val blockSize = this[index].unsignedInt()
        index += 1
        if (blockSize == 0) {
            return index
        }
        index += blockSize
    }
    return index
}

private fun ByteArray.readUnsignedShort(startIndex: Int): Int {
    if (startIndex + 1 >= size) return 0
    return this[startIndex].unsignedInt() or (this[startIndex + 1].unsignedInt() shl 8)
}

private fun Byte.unsignedInt(): Int = toInt() and 0xFF
