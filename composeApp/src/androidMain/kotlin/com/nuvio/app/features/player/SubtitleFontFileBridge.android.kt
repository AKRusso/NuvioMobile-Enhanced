package com.nuvio.app.features.player

import android.app.Activity
import android.content.Intent
import android.provider.OpenableColumns
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.lang.ref.WeakReference

internal actual object SubtitleFontFileBridge {
    private const val requestImportFont = 51061

    private var activityRef: WeakReference<AppCompatActivity>? = null
    private var pendingImportCallback: ((Result<SubtitleFontImportResult>) -> Unit)? = null

    fun bindActivity(activity: AppCompatActivity) {
        activityRef = WeakReference(activity)
    }

    fun unbindActivity(activity: AppCompatActivity) {
        if (activityRef?.get() === activity) {
            activityRef = null
        }
    }

    actual fun importFont(onResult: (Result<SubtitleFontImportResult>) -> Unit) {
        val activity = activityRef?.get()
        if (activity == null) {
            onResult(Result.failure(IllegalStateException("Font import is not available right now.")))
            return
        }

        pendingImportCallback = onResult
        runCatching {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(
                    Intent.EXTRA_MIME_TYPES,
                    arrayOf(
                        "font/ttf",
                        "font/otf",
                        "application/font-sfnt",
                        "application/vnd.ms-opentype",
                        "application/octet-stream",
                    ),
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivityForResult(intent, requestImportFont)
        }.onFailure { error ->
            clearPendingImport()
            onResult(Result.failure(error))
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != requestImportFont) return false
        val callback = pendingImportCallback
        clearPendingImport()
        if (callback != null) {
            callback(handleImportResult(resultCode, data))
        }
        return true
    }

    private fun handleImportResult(
        resultCode: Int,
        data: Intent?,
    ): Result<SubtitleFontImportResult> = runCatching {
        if (resultCode != Activity.RESULT_OK) error("Font import cancelled.")
        val uri = data?.data ?: error("No font file selected.")
        val activity = activityRef?.get() ?: error("Font import is not available right now.")
        val resolver = activity.contentResolver
        val displayName = resolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }?.takeIf { it.isNotBlank() } ?: "subtitle-font.ttf"
        val extension = displayName.substringAfterLast('.', "ttf").lowercase().let { ext ->
            if (ext in setOf("ttf", "otf")) ext else "ttf"
        }
        val destinationDir = File(activity.filesDir, "subtitle-fonts").apply { mkdirs() }
        val destination = File(destinationDir, "custom-subtitle-font.$extension")
        resolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not read font file.")
        SubtitleFontImportResult(
            displayName = displayName.substringBeforeLast('.').ifBlank { displayName },
            path = destination.absolutePath,
        )
    }

    private fun clearPendingImport() {
        pendingImportCallback = null
    }
}
