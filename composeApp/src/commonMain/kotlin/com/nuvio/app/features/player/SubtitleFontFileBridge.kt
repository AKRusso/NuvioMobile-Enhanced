package com.nuvio.app.features.player

internal expect object SubtitleFontFileBridge {
    fun importFont(onResult: (Result<SubtitleFontImportResult>) -> Unit)
}
