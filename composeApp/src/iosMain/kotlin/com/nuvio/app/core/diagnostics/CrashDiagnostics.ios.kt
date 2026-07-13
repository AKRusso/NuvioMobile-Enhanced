package com.nuvio.app.core.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual object CrashDiagnostics {
    actual val reportsSupported: Boolean = false

    private val _pendingReport = MutableStateFlow<LocalCrashReport?>(null)
    actual val pendingReport: StateFlow<LocalCrashReport?> = _pendingReport.asStateFlow()

    actual fun initialize(context: Any?) = Unit
    actual fun dismiss(reportId: String) = Unit
}
