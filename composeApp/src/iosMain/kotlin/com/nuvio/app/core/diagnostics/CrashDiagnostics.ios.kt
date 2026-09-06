package com.nuvio.app.core.diagnostics

import com.nuvio.app.core.build.AppVersionConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.setUnhandledExceptionHook
import platform.Foundation.NSBundle
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSUserDefaults
import platform.Foundation.localeWithLocaleIdentifier
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIDevice

@OptIn(ExperimentalNativeApi::class)
actual object CrashDiagnostics {
    actual val reportsSupported: Boolean = true

    private val _pendingReport = MutableStateFlow<LocalCrashReport?>(null)
    actual val pendingReport: StateFlow<LocalCrashReport?> = _pendingReport.asStateFlow()
    private val _lastReport = MutableStateFlow<LocalCrashReport?>(null)
    actual val lastReport: StateFlow<LocalCrashReport?> = _lastReport.asStateFlow()

    private const val idKey = "nuvio_local_crash_diagnostics_id"
    private const val summaryKey = "nuvio_local_crash_diagnostics_summary"
    private const val detailsKey = "nuvio_local_crash_diagnostics_details"
    private const val lastIdKey = "nuvio_local_crash_diagnostics_last_id"
    private const val lastSummaryKey = "nuvio_local_crash_diagnostics_last_summary"
    private const val lastDetailsKey = "nuvio_local_crash_diagnostics_last_details"
    private const val maxReportLength = 24_000

    private var installed = false

    actual fun initialize(context: Any?) {
        _pendingReport.value = loadPendingReport()
        _lastReport.value = loadLastReport() ?: _pendingReport.value
        if (installed) return
        installed = true
        setUnhandledExceptionHook { throwable ->
            try {
                saveCrashReport(throwable)
            } catch (_: Throwable) {
                // Crash capture must never replace the original unhandled exception.
            }
        }
    }

    actual fun dismiss(reportId: String) {
        val defaults = NSUserDefaults.standardUserDefaults
        if (defaults.stringForKey(idKey) != reportId) return
        defaults.removeObjectForKey(idKey)
        defaults.removeObjectForKey(summaryKey)
        defaults.removeObjectForKey(detailsKey)
        defaults.synchronize()
        _pendingReport.value = null
    }

    actual fun currentReport(): String {
        val device = UIDevice.currentDevice
        val bundleId = NSBundle.mainBundle.bundleIdentifier ?: "unknown"
        return buildString {
            appendLine("Nuvio Enhanced current diagnostic report")
            appendLine("Time: ${formattedNow()}")
            appendLine("Package: $bundleId")
            appendLine("Version: ${AppVersionConfig.VERSION_NAME} (${AppVersionConfig.VERSION_CODE})")
            appendLine("iOS: ${device.systemName} ${device.systemVersion}")
            appendLine("Device model: ${device.model}")
            appendLine()
            append(RuntimeDiagnostics.snapshotText())
        }.take(maxReportLength)
    }

    private fun loadPendingReport(): LocalCrashReport? {
        val defaults = NSUserDefaults.standardUserDefaults
        val id = defaults.stringForKey(idKey)?.takeIf(String::isNotBlank) ?: return null
        val summary = defaults.stringForKey(summaryKey)?.takeIf(String::isNotBlank) ?: "Unknown crash"
        val details = defaults.stringForKey(detailsKey)?.takeIf(String::isNotBlank) ?: return null
        return localCrashReport(id = id, summary = summary, details = details)
    }

    private fun loadLastReport(): LocalCrashReport? {
        val defaults = NSUserDefaults.standardUserDefaults
        val id = defaults.stringForKey(lastIdKey)?.takeIf(String::isNotBlank) ?: return null
        val summary = defaults.stringForKey(lastSummaryKey)?.takeIf(String::isNotBlank) ?: "Unknown crash"
        val details = defaults.stringForKey(lastDetailsKey)?.takeIf(String::isNotBlank) ?: return null
        return localCrashReport(id = id, summary = summary, details = details)
    }

    private fun saveCrashReport(throwable: Throwable) {
        val timestamp = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
        val id = timestamp.toString()
        val summary = throwable.summary()
        val details = buildReport(throwable).take(maxReportLength)
        val defaults = NSUserDefaults.standardUserDefaults
        defaults.setObject(id, forKey = idKey)
        defaults.setObject(summary, forKey = summaryKey)
        defaults.setObject(details, forKey = detailsKey)
        defaults.setObject(id, forKey = lastIdKey)
        defaults.setObject(summary, forKey = lastSummaryKey)
        defaults.setObject(details, forKey = lastDetailsKey)
        defaults.synchronize()
        val report = localCrashReport(id = id, summary = summary, details = details)
        _pendingReport.value = report
        _lastReport.value = report
    }

    private fun buildReport(throwable: Throwable): String {
        val device = UIDevice.currentDevice
        val bundleId = NSBundle.mainBundle.bundleIdentifier ?: "unknown"
        val process = NSProcessInfo.processInfo
        val stackTrace = throwable.stackTraceToString().sanitizeCrashReport()
        return buildString {
            appendLine("Nuvio Enhanced local crash report")
            appendLine("Time: ${formattedNow()}")
            appendLine("Package: $bundleId")
            appendLine("Version: ${AppVersionConfig.VERSION_NAME} (${AppVersionConfig.VERSION_CODE})")
            appendLine("iOS: ${device.systemName} ${device.systemVersion}")
            appendLine("Device model: ${device.model}")
            appendLine("Process: ${process.processName}")
            appendLine("Thread: Kotlin/Native unhandled exception")
            appendLine("Exception: ${throwable::class.qualifiedName ?: throwable::class.simpleName ?: "Throwable"}")
            appendLine("Message: ${throwable.message?.sanitizeCrashReport().orEmpty()}")
            appendLine()
            appendLine(RuntimeDiagnostics.snapshotText())
            appendLine()
            appendLine(stackTrace)
        }
    }

    private fun formattedNow(): String {
        val formatter = NSDateFormatter()
        formatter.locale = NSLocale.localeWithLocaleIdentifier("en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS Z"
        return formatter.stringFromDate(NSDate())
    }

    private fun Throwable.summary(): String {
        val message = message?.sanitizeCrashReport()?.takeIf(String::isNotBlank)
        val type = this::class.qualifiedName ?: this::class.simpleName ?: "Throwable"
        return if (message == null) type else "$type: $message"
    }

    private fun String.sanitizeCrashReport(): String = sanitizeDiagnosticText()
}
