package com.nuvio.app.features.downloads

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.nuvio.app.features.streams.StreamSubtitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString
import java.io.EOFException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

private val downloadHttpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(90, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .followRedirects(true)
    .followSslRedirects(true)
    .build()

private const val downloadBufferBytes = 1024 * 1024
private const val downloadProgressMinIntervalMs = 500L
private const val downloadProgressMinBytes = 1024L * 1024L
private const val downloadMaxAttempts = 6

internal actual object DownloadsPlatformDownloader {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    actual fun start(
        request: DownloadPlatformRequest,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
        onSuccess: (localFileUri: String, totalBytes: Long?) -> Unit,
        onFailure: (message: String) -> Unit,
    ): DownloadsTaskHandle {
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.IO)
        var call: Call? = null

        scope.launch {
            val context = appContext
            if (context == null) {
                onFailure(runBlocking { getString(Res.string.downloads_error_not_initialized) })
                return@launch
            }

            val downloadsDir = File(context.filesDir, "downloads").apply { mkdirs() }
            val destination = File(downloadsDir, request.destinationFileName)
            val tempFile = File(downloadsDir, "${request.destinationFileName}.part")

            try {
                var lastFailure: Throwable? = null
                repeat(downloadMaxAttempts) { attemptIndex ->
                    ensureActive()
                    try {
                        downloadAttempt(
                            request = request,
                            destination = destination,
                            tempFile = tempFile,
                            onCall = { call = it },
                            onProgress = onProgress,
                            onSuccess = onSuccess,
                        )
                        return@launch
                    } catch (error: Throwable) {
                        if (error is CancellationException) return@launch
                        lastFailure = error
                        if (!error.isRetryableDownloadError() || attemptIndex == downloadMaxAttempts - 1) {
                            throw error
                        }
                        val delayMs = (750L * (attemptIndex + 1) * (attemptIndex + 1)).coerceAtMost(6_000L)
                        delay(delayMs)
                    }
                }
                throw lastFailure ?: IllegalStateException(
                    runBlocking { getString(Res.string.downloads_error_request_failed) },
                )
            } catch (error: Throwable) {
                if (error is CancellationException) return@launch
                onFailure(error.message ?: runBlocking { getString(Res.string.download_failed) })
            }
        }

        job.invokeOnCompletion {
            call?.cancel()
        }

        return AndroidDownloadsTaskHandle(job)
    }

    actual fun removeFile(localFileUri: String?): Boolean {
        if (localFileUri.isNullOrBlank()) return false
        val file = localFileUri.toLocalFileOrNull() ?: return false
        return runCatching { file.delete() }.getOrDefault(false)
    }

    actual fun removePartialFile(destinationFileName: String): Boolean {
        val context = appContext ?: return false
        val downloadsDir = File(context.filesDir, "downloads")
        val tempFile = File(downloadsDir, "$destinationFileName.part")
        if (!tempFile.exists()) return true
        return runCatching { tempFile.delete() }.getOrDefault(false)
    }

    actual fun resolveLocalFileUri(localFileUri: String?, destinationFileName: String): String? {
        localFileUri
            ?.toLocalFileOrNull()
            ?.takeIf { it.exists() }
            ?.let { return it.toURI().toString() }

        val context = appContext ?: return null
        val fileName = destinationFileName.trim().takeIf { it.isNotBlank() }
            ?: localFileUri
                ?.toLocalFileOrNull()
                ?.name
                ?.takeIf { it.isNotBlank() }
            ?: return null
        val downloadsDir = File(context.filesDir, "downloads")
        val localFile = File(downloadsDir, fileName)
        return localFile.takeIf { it.exists() }?.toURI()?.toString()
    }

    actual fun cacheSubtitleFiles(
        subtitles: List<StreamSubtitle>,
        companionBaseFileName: String,
    ): List<StreamSubtitle> {
        if (subtitles.isEmpty()) return emptyList()
        val context = appContext ?: return emptyList()
        val subtitlesDir = File(File(context.filesDir, "downloads"), "subtitles").apply { mkdirs() }
        val baseName = companionBaseFileName.substringBeforeLast('.')
            .sanitizeFileName()
            .ifBlank { "subtitle" }

        return subtitles.mapIndexedNotNull { index, subtitle ->
            val sourceUrl = subtitle.url.trim().takeIf { it.startsWith("http", ignoreCase = true) }
                ?: return@mapIndexedNotNull subtitle
            val extension = sourceUrl.subtitleFileExtension()
            val language = subtitle.language.ifBlank { "und" }.sanitizeFileName()
            val label = subtitle.name.orEmpty().sanitizeFileName().takeIf { it.isNotBlank() }
            val destination = File(
                subtitlesDir,
                buildString {
                    append(baseName.take(72))
                    append('_')
                    append(index + 1)
                    append('_')
                    append(language.take(16))
                    if (label != null) {
                        append('_')
                        append(label.take(32))
                    }
                    append('.')
                    append(extension)
                },
            )

            val request = Request.Builder().url(sourceUrl).apply {
                subtitle.headers.orEmpty().forEach { (key, value) ->
                    val normalizedKey = key.trim()
                    val normalizedValue = value.trim()
                    if (normalizedKey.isNotBlank() && normalizedValue.isNotBlank()) {
                        header(normalizedKey, normalizedValue)
                    }
                }
            }.build()

            runCatching {
                downloadHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching null
                    val body = response.body ?: return@runCatching null
                    destination.outputStream().use { output ->
                        body.byteStream().copyTo(output)
                    }
                }
                subtitle.copy(
                    url = destination.toURI().toString(),
                    headers = null,
                )
            }.getOrNull()
        }
    }

    actual fun openDownloadsDirectory(): Boolean {
        val context = appContext ?: return false
        val downloadsDir = File(context.filesDir, "downloads").apply { mkdirs() }
        val uri = runCatching {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                downloadsDir,
            )
        }.getOrNull() ?: return false

        val intents = listOf(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "resource/folder")
            },
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "vnd.android.document/directory")
            },
            Intent(Intent.ACTION_VIEW).apply {
                data = uri
            },
        )

        return intents.any { intent ->
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)

            runCatching {
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        }
    }
}

private suspend fun downloadAttempt(
    request: DownloadPlatformRequest,
    destination: File,
    tempFile: File,
    onCall: (Call) -> Unit,
    onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
    onSuccess: (localFileUri: String, totalBytes: Long?) -> Unit,
) {
    var resumeFromBytes = tempFile.takeIf { it.exists() }?.length()?.coerceAtLeast(0L) ?: 0L

    fun buildRequest(rangeStart: Long?): Request {
        val requestBuilder = Request.Builder().url(request.sourceUrl)
        request.sourceHeaders.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }
        if (rangeStart != null && rangeStart > 0L) {
            requestBuilder.header("Range", "bytes=$rangeStart-")
        }
        return requestBuilder.get().build()
    }

    var attemptedRangeRequest = resumeFromBytes > 0L
    var httpRequest = buildRequest(if (attemptedRangeRequest) resumeFromBytes else null)
    var activeCall = downloadHttpClient.newCall(httpRequest)
    onCall(activeCall)
    var response = activeCall.execute()

    if (attemptedRangeRequest && response.code == 416) {
        response.close()
        tempFile.delete()
        resumeFromBytes = 0L
        attemptedRangeRequest = false
        httpRequest = buildRequest(null)
        activeCall = downloadHttpClient.newCall(httpRequest)
        onCall(activeCall)
        response = activeCall.execute()
    }

    response.use { response ->
        if (!response.isSuccessful) {
            throw HttpDownloadException(response.code)
        }

        val isPartialResume = attemptedRangeRequest && response.code == 206 && resumeFromBytes > 0L
        val appendToTemp = isPartialResume
        val startingBytes = if (appendToTemp) resumeFromBytes else 0L

        if (!appendToTemp && tempFile.exists()) {
            tempFile.delete()
        }

        val body = response.body ?: error(
            runBlocking { getString(Res.string.downloads_error_empty_body) },
        )
        val totalBytes = resolveTotalBytes(
            startingBytes = startingBytes,
            isPartialResume = isPartialResume,
            contentRangeHeader = response.header("Content-Range"),
            contentLength = body.contentLength().takeIf { it > 0L },
        )
        var downloadedBytes = startingBytes
        onProgress(downloadedBytes, totalBytes)
        var lastProgressBytes = downloadedBytes
        var lastProgressAtMs = System.currentTimeMillis()

        fun publishProgress(force: Boolean = false) {
            val now = System.currentTimeMillis()
            val progressedBytes = downloadedBytes - lastProgressBytes
            if (
                force ||
                progressedBytes >= downloadProgressMinBytes ||
                now - lastProgressAtMs >= downloadProgressMinIntervalMs
            ) {
                onProgress(downloadedBytes, totalBytes)
                lastProgressBytes = downloadedBytes
                lastProgressAtMs = now
            }
        }

        body.byteStream().use { input ->
            FileOutputStream(tempFile, appendToTemp).use { output ->
                val buffer = ByteArray(downloadBufferBytes)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = input.read(buffer)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    downloadedBytes += read.toLong()
                    publishProgress()
                }
                publishProgress(force = true)
                output.flush()
            }
        }

        if (totalBytes != null && downloadedBytes < totalBytes) {
            throw EOFException("Download interrupted at $downloadedBytes / $totalBytes bytes")
        }

        if (destination.exists()) {
            destination.delete()
        }
        if (!tempFile.renameTo(destination)) {
            tempFile.copyTo(destination, overwrite = true)
            tempFile.delete()
        }

        val finalSize = destination.length()
        onSuccess(destination.toURI().toString(), totalBytes ?: finalSize)
    }
}

private class HttpDownloadException(
    val code: Int,
) : IOException(
    runBlocking { getString(Res.string.downloads_error_http_failed, code) },
)

private fun Throwable.isRetryableDownloadError(): Boolean = when (this) {
    is CancellationException -> false
    is EOFException -> true
    is java.net.SocketTimeoutException -> true
    is java.net.SocketException -> true
    is HttpDownloadException -> code == 408 || code == 429 || code in 500..599
    is IOException -> true
    else -> false
}

private class AndroidDownloadsTaskHandle(
    private val job: Job,
) : DownloadsTaskHandle {
    override fun cancel() {
        job.cancel()
    }
}

private fun String.toLocalFileOrNull(): File? {
    return runCatching {
        if (startsWith("file:")) {
            File(URI(this))
        } else {
            File(this)
        }
    }.getOrNull()
}

private fun String.subtitleFileExtension(): String {
    val path = substringBefore('?').substringBefore('#').trimEnd('/')
    return when {
        path.endsWith(".vtt", ignoreCase = true) -> "vtt"
        path.endsWith(".ass", ignoreCase = true) -> "ass"
        path.endsWith(".ssa", ignoreCase = true) -> "ssa"
        path.endsWith(".ttml", ignoreCase = true) -> "ttml"
        path.endsWith(".dfxp", ignoreCase = true) -> "dfxp"
        else -> "srt"
    }
}

private fun String.sanitizeFileName(): String =
    trim().replace(Regex("[^A-Za-z0-9._ -]"), "_")

private fun resolveTotalBytes(
    startingBytes: Long,
    isPartialResume: Boolean,
    contentRangeHeader: String?,
    contentLength: Long?,
): Long? {
    parseContentRangeTotal(contentRangeHeader)?.let { return it }
    val normalizedLength = contentLength?.takeIf { it > 0L } ?: return null
    return if (isPartialResume && startingBytes > 0L) {
        startingBytes + normalizedLength
    } else {
        normalizedLength
    }
}

private fun parseContentRangeTotal(headerValue: String?): Long? {
    val value = headerValue?.trim().orEmpty()
    if (value.isBlank()) return null
    val slashIndex = value.lastIndexOf('/')
    if (slashIndex == -1 || slashIndex == value.lastIndex) return null
    val totalPart = value.substring(slashIndex + 1).trim()
    if (totalPart == "*") return null
    return totalPart.toLongOrNull()?.takeIf { it > 0L }
}
