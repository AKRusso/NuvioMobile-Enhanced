package com.nuvio.app.features.downloads

import kotlinx.coroutines.flow.StateFlow

internal data class DownloadExternalFolderState(
    val uri: String? = null,
    val displayName: String? = null,
    val unavailable: Boolean = false,
)

internal expect object DownloadsExternalFolderPlatform {
    val state: StateFlow<DownloadExternalFolderState>

    fun chooseFolder(onResult: (Result<String?>) -> Unit)

    fun selectedFolderUri(): String?

    fun clearFolder()

    fun markUnavailable()

    fun onProfileChanged()
}
