package com.nuvio.app.features.player

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.posix.SEEK_END
import platform.posix.SEEK_SET
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.fwrite

@OptIn(ExperimentalForeignApi::class)
internal actual object SubtitleFontFileBridge {
    private var importDelegate: FontDocumentPickerDelegate? = null

    actual fun importFont(onResult: (Result<SubtitleFontImportResult>) -> Unit) {
        runCatching {
            val presenter = topViewController() ?: error("Font import is not available right now.")
            val controller = UIDocumentPickerViewController(
                documentTypes = listOf(
                    "public.font",
                    "public.truetype-ttf-font",
                    "public.opentype-font",
                    "public.data",
                ),
                inMode = UIDocumentPickerMode.UIDocumentPickerModeImport,
            )
            val delegate = FontDocumentPickerDelegate(
                onResult = { result ->
                    importDelegate = null
                    onResult(result)
                },
            )
            importDelegate = delegate
            controller.delegate = delegate
            presenter.presentViewController(controller, animated = true, completion = null)
        }.onFailure { error ->
            importDelegate = null
            onResult(Result.failure(error))
        }
    }

    private fun topViewController(): UIViewController? {
        var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (controller?.presentedViewController != null) {
            controller = controller.presentedViewController
        }
        return controller
    }

    private class FontDocumentPickerDelegate(
        private val onResult: (Result<SubtitleFontImportResult>) -> Unit,
    ) : NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(
            controller: UIDocumentPickerViewController,
            didPickDocumentsAtURLs: List<*>,
        ) {
            val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
            if (url == null) {
                onResult(Result.failure(IllegalStateException("No font file selected.")))
                return
            }

            val accessed = url.startAccessingSecurityScopedResource()
            try {
                val sourcePath = url.path ?: error("Could not read font file path.")
                val displayName = url.lastPathComponent?.takeIf { it.isNotBlank() } ?: "subtitle-font.ttf"
                val bytes = readBinaryFile(sourcePath) ?: error("Could not read font file.")
                val destinationDir = NSHomeDirectory().trimEnd('/') + "/Documents/subtitle-fonts"
                NSFileManager.defaultManager.createDirectoryAtPath(
                    path = destinationDir,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null,
                )
                val extension = displayName.substringAfterLast('.', "ttf").lowercase().let { ext ->
                    if (ext in setOf("ttf", "otf")) ext else "ttf"
                }
                val destinationPath = "$destinationDir/custom-subtitle-font.$extension"
                if (!bytes.writeToFile(destinationPath)) {
                    error("Could not save font file.")
                }
                onResult(
                    Result.success(
                        SubtitleFontImportResult(
                            displayName = displayName.substringBeforeLast('.').ifBlank { displayName },
                            path = destinationPath,
                        ),
                    ),
                )
            } catch (error: Throwable) {
                onResult(Result.failure(error))
            } finally {
                if (accessed) {
                    url.stopAccessingSecurityScopedResource()
                }
            }
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            onResult(Result.failure(IllegalStateException("Font import cancelled.")))
        }
    }

    private fun readBinaryFile(path: String): ByteArray? {
        val file = fopen(path, "rb") ?: return null
        return try {
            fseek(file, 0, SEEK_END)
            val size = ftell(file).toInt()
            if (size < 0) return null
            fseek(file, 0, SEEK_SET)
            val bytes = ByteArray(size)
            val read = bytes.usePinned { pinned ->
                fread(
                    pinned.addressOf(0),
                    1.convert(),
                    size.convert(),
                    file,
                )
            }
            if (read.toLong() != size.toLong()) null else bytes
        } finally {
            fclose(file)
        }
    }

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
}
