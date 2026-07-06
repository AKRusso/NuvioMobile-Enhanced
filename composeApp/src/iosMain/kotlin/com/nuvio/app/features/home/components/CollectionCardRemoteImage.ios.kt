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
private const val DefaultGifFrameDelaySeconds = 0.1
private val animatedGifCache = mutableMapOf<String, AnimatedGif>()
private val animatedGifCacheOrder = mutableListOf<String>()
private val animatedGifInFlight = mutableMapOf<String, Deferred<AnimatedGif?>>()

private data class AnimatedGif(
    val images: List<UIImage>,
    val durationSeconds: Double,
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
    val shouldLoadAnimatedGif = remember(imageUrl, animateIfPossible) {
        animateIfPossible || imageUrl.looksLikeGifUrl()
    }

    if (!shouldLoadAnimatedGif) {
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
        animationDuration = gif.durationSeconds.coerceAtLeast(DefaultGifFrameDelaySeconds)
        animationRepeatCount = 0
        image = gif.images.firstOrNull()
        holder.currentGif = gif
    }
    startAnimating()
}

private fun String.looksLikeGifUrl(): Boolean =
    substringBefore('?')
        .substringBefore('#')
        .trim()
        .endsWith(".gif", ignoreCase = true)

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

    val images = mutableListOf<UIImage>()
    for (index in 0 until frameCount) {
        val imageRef: CGImageRef = CGImageSourceCreateImageAtIndex(source, index.toULong(), null) ?: continue
        try {
            images += UIImage.imageWithCGImage(imageRef)
        } finally {
            CGImageRelease(imageRef)
        }
    }
    if (images.size <= 1) return null

    return AnimatedGif(
        images = images,
        durationSeconds = images.size * DefaultGifFrameDelaySeconds,
    )
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
