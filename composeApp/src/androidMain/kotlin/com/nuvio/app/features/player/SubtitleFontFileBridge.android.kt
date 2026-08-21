package com.nuvio.app.features.player

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.provider.OpenableColumns
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.lang.ref.WeakReference
import java.util.UUID

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

    // Activity results are routed through MainActivity to preserve the shared bridge contract.
    @Suppress("DEPRECATION")
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
        val fontsRoot = File(activity.filesDir, "subtitle-fonts").apply { mkdirs() }
        check(fontsRoot.isDirectory) { "Could not create the font storage directory." }
        // libmpv/libass receive only the selected file's parent directory. Keeping each
        // import isolated prevents an older font with the same embedded family winning.
        val destinationDir = File(fontsRoot, UUID.randomUUID().toString()).apply { mkdirs() }
        check(destinationDir.isDirectory) { "Could not create the font storage directory." }
        val temporary = File(destinationDir, ".font-import.$extension")
        val destination = File(destinationDir, "custom-subtitle-font.$extension")
        try {
            resolver.openInputStream(uri)?.use { input ->
                temporary.outputStream().use { output -> input.copyTo(output) }
            } ?: error("Could not read font file.")
            check(temporary.length() > 0L) { "The selected font file is empty." }
            Typeface.createFromFile(temporary)
            check(temporary.renameTo(destination)) { "Could not save the selected font file." }
        } finally {
            temporary.delete()
            if (!destination.exists()) destinationDir.delete()
        }
        val embeddedFamily = subtitleFontFamilyName(destination.absolutePath)
        SubtitleFontImportResult(
            displayName = embeddedFamily ?: displayName.substringBeforeLast('.').ifBlank { displayName },
            path = destination.absolutePath,
        )
    }

    private fun clearPendingImport() {
        pendingImportCallback = null
    }
}
