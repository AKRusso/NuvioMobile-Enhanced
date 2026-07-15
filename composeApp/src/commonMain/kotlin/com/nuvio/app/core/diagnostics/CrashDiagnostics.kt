package com.nuvio.app.core.diagnostics

import kotlinx.coroutines.flow.StateFlow

data class LocalCrashReport(
    val id: String,
    val summary: String,
    val details: String,
)

expect object CrashDiagnostics {
    val reportsSupported: Boolean
    val pendingReport: StateFlow<LocalCrashReport?>
    val lastReport: StateFlow<LocalCrashReport?>

    fun initialize(context: Any?)
    fun dismiss(reportId: String)
}
