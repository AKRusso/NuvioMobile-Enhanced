package com.nuvio.app.features.downloads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal actual object DownloadsExternalFolderPlatform {
    private val _state = MutableStateFlow(DownloadExternalFolderState())
    actual val state: StateFlow<DownloadExternalFolderState> = _state.asStateFlow()

    actual fun chooseFolder(onResult: (Result<String?>) -> Unit) {
        onResult(Result.success(null))
    }

    actual fun selectedFolderUri(): String? = null

    actual fun clearFolder() = Unit

    actual fun markUnavailable() = Unit

    actual fun onProfileChanged() = Unit
}
