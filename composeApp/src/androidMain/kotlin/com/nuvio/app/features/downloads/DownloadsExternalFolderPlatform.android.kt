package com.nuvio.app.features.downloads

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.appcompat.app.AppCompatActivity
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal actual object DownloadsExternalFolderPlatform {
    private const val requestChooseFolder = 51043

    private var appContext: Context? = null
    private var activityRef: WeakReference<AppCompatActivity>? = null
    private var pendingCallback: ((Result<String?>) -> Unit)? = null

    private val _state = MutableStateFlow(DownloadExternalFolderState())
    actual val state: StateFlow<DownloadExternalFolderState> = _state.asStateFlow()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        loadCurrentProfile()
    }

    fun bindActivity(activity: AppCompatActivity) {
        activityRef = WeakReference(activity)
    }

    fun unbindActivity(activity: AppCompatActivity) {
        if (activityRef?.get() === activity) {
            activityRef = null
        }
    }

    @Suppress("DEPRECATION")
    actual fun chooseFolder(onResult: (Result<String?>) -> Unit) {
        val activity = activityRef?.get()
        if (activity == null) {
            onResult(Result.failure(IllegalStateException("Folder selection is not available right now.")))
            return
        }

        pendingCallback = onResult
        runCatching {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            }
            activity.startActivityForResult(intent, requestChooseFolder)
        }.onFailure { error ->
            pendingCallback = null
            onResult(Result.failure(error))
        }
    }

    actual fun selectedFolderUri(): String? =
        DownloadsStorage.loadExternalFolderUri()?.trim()?.takeIf { it.isNotBlank() }

    actual fun clearFolder() {
        DownloadsStorage.saveExternalFolderUri(null)
        _state.value = DownloadExternalFolderState()
    }

    actual fun markUnavailable() {
        val current = _state.value
        if (current.uri != null && !current.unavailable) {
            _state.value = current.copy(unavailable = true)
        }
    }

    actual fun onProfileChanged() {
        loadCurrentProfile()
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != requestChooseFolder) return false

        val callback = pendingCallback
        pendingCallback = null
        if (resultCode != Activity.RESULT_OK || data?.data == null) {
            callback?.invoke(Result.success(null))
            return true
        }

        val context = appContext
        val uri = data.data
        if (context == null || uri == null) {
            callback?.invoke(Result.failure(IllegalStateException("Folder selection is not available right now.")))
            return true
        }

        val result = runCatching {
            val takeFlags = data.flags and
                (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            if (takeFlags != 0) {
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            }
            val displayName = folderDisplayName(context, uri)
            DownloadsStorage.saveExternalFolderUri(uri.toString())
            _state.value = DownloadExternalFolderState(
                uri = uri.toString(),
                displayName = displayName,
                unavailable = false,
            )
            displayName
        }
        callback?.invoke(result)
        return true
    }

    private fun loadCurrentProfile() {
        val context = appContext ?: return
        val uriString = DownloadsStorage.loadExternalFolderUri()?.trim().orEmpty()
        if (uriString.isBlank()) {
            _state.value = DownloadExternalFolderState()
            return
        }

        val uri = Uri.parse(uriString)
        val displayName = folderDisplayName(context, uri)
        _state.value = DownloadExternalFolderState(
            uri = uriString,
            displayName = displayName,
            unavailable = false,
        )
    }

    private fun folderDisplayName(context: Context, uri: Uri): String? {
        val treeDocumentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
            ?: return null
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, treeDocumentId)

        return runCatching {
            context.contentResolver.query(
                documentUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val displayNameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                if (displayNameIndex >= 0) {
                    cursor.getString(displayNameIndex)?.takeIf { it.isNotBlank() }
                } else {
                    null
                }
            }
        }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: treeDocumentId
                .substringAfter(':')
                .substringAfterLast('/')
                .takeIf { it.isNotBlank() }
    }

}
