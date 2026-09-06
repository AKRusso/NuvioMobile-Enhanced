package com.nuvio.app.core.diagnostics

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Process
import android.util.Log
import com.nuvio.app.core.build.AppVersionConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.system.exitProcess

actual object CrashDiagnostics {
    actual val reportsSupported: Boolean = true

    private const val preferencesName = "nuvio_local_crash_diagnostics"
    private const val idKey = "id"
    private const val summaryKey = "summary"
    private const val detailsKey = "details"
    private const val lastIdKey = "last_id"
    private const val lastSummaryKey = "last_summary"
    private const val lastDetailsKey = "last_details"
    private const val maxReportLength = 24_000

    private val _pendingReport = MutableStateFlow<LocalCrashReport?>(null)
    actual val pendingReport: StateFlow<LocalCrashReport?> = _pendingReport.asStateFlow()
    private val _lastReport = MutableStateFlow<LocalCrashReport?>(null)
    actual val lastReport: StateFlow<LocalCrashReport?> = _lastReport.asStateFlow()

    private var preferences: SharedPreferences? = null
    private var installed = false
    private var previousHandler: Thread.UncaughtExceptionHandler? = null
    private var appContext: Context? = null
    private var emergencyMemory: ByteArray? = ByteArray(64 * 1024)

    actual fun initialize(context: Any?) {
        val appContext = (context as? Context)?.applicationContext ?: return
        this.appContext = appContext
        preferences = appContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        _pendingReport.value = loadPendingReport()
        _lastReport.value = loadLastReport() ?: _pendingReport.value
        if (installed) return
        installed = true
        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (throwable is OutOfMemoryError) emergencyMemory = null
            try {
                saveCrashReport(appContext, thread, throwable)
            } catch (captureFailure: Throwable) {
                Log.e("CrashDiagnostics", "Failed to save local crash report", captureFailure)
            } finally {
                previousHandler?.uncaughtException(thread, throwable) ?: run {
                    Process.killProcess(Process.myPid())
                    exitProcess(10)
                }
            }
        }
    }

    actual fun dismiss(reportId: String) {
        val prefs = preferences ?: return
        if (prefs.getString(idKey, null) != reportId) return
        prefs.edit()
            .remove(idKey)
            .remove(summaryKey)
            .remove(detailsKey)
            .apply()
        _pendingReport.value = null
    }

    actual fun currentReport(): String {
        val context = appContext
        val timestamp = System.currentTimeMillis()
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date(timestamp))
        return buildString {
            appendLine("Nuvio Enhanced current diagnostic report")
            appendLine("Time: $time")
            appendLine("Package: ${context?.packageName ?: "unknown"}")
            appendLine("Version: ${AppVersionConfig.VERSION_NAME} (${AppVersionConfig.VERSION_CODE})")
            appendLine("Android: ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine(heapSummary())
            appendLine()
            append(RuntimeDiagnostics.snapshotText())
        }.take(maxReportLength)
    }

    private fun loadPendingReport(): LocalCrashReport? {
        val prefs = preferences ?: return null
        val id = prefs.getString(idKey, null)?.takeIf(String::isNotBlank) ?: return null
        val summary = prefs.getString(summaryKey, null)?.takeIf(String::isNotBlank) ?: "Unknown crash"
        val details = prefs.getString(detailsKey, null)?.takeIf(String::isNotBlank) ?: return null
        return localCrashReport(id = id, summary = summary, details = details)
    }

    private fun loadLastReport(): LocalCrashReport? {
        val prefs = preferences ?: return null
        val id = prefs.getString(lastIdKey, null)?.takeIf(String::isNotBlank) ?: return null
        val summary = prefs.getString(lastSummaryKey, null)?.takeIf(String::isNotBlank) ?: "Unknown crash"
        val details = prefs.getString(lastDetailsKey, null)?.takeIf(String::isNotBlank) ?: return null
        return localCrashReport(id = id, summary = summary, details = details)
    }

    private fun saveCrashReport(context: Context, thread: Thread, throwable: Throwable) {
        val prefs = preferences ?: context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val timestamp = System.currentTimeMillis()
        val id = timestamp.toString()
        val summary = throwable.summary()
        val details = buildReport(context, thread, throwable, timestamp)
        prefs.edit()
            .putString(idKey, id)
            .putString(summaryKey, summary)
            .putString(detailsKey, details)
            .putString(lastIdKey, id)
            .putString(lastSummaryKey, summary)
            .putString(lastDetailsKey, details)
            .commit()
        val report = localCrashReport(id = id, summary = summary, details = details)
        _pendingReport.value = report
        _lastReport.value = report
    }

    private fun buildReport(
        context: Context,
        thread: Thread,
        throwable: Throwable,
        timestamp: Long,
    ): String {
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date(timestamp))
        val stackTrace = Log.getStackTraceString(throwable).sanitizeCrashReport()
        val rawReport = buildString {
            appendLine("Nuvio Enhanced local crash report")
            appendLine("Time: $time")
            appendLine("Package: ${context.packageName}")
            appendLine("Version: ${AppVersionConfig.VERSION_NAME} (${AppVersionConfig.VERSION_CODE})")
            appendLine("Android: ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine(heapSummary())
            appendLine("Thread: ${thread.name}")
            appendLine("Exception: ${throwable.javaClass.name}")
            appendLine("Message: ${throwable.message?.sanitizeCrashReport().orEmpty()}")
            appendLine()
            appendLine(RuntimeDiagnostics.snapshotText())
            appendLine()
            appendLine(stackTrace)
        }
        return rawReport.take(maxReportLength)
    }

    private fun Throwable.summary(): String {
        val message = message?.sanitizeCrashReport()?.takeIf(String::isNotBlank)
        return if (message == null) javaClass.name else "${javaClass.name}: $message"
    }

    private fun String.sanitizeCrashReport(): String = sanitizeDiagnosticText()

    private fun heapSummary(): String {
        val runtime = Runtime.getRuntime()
        val total = runtime.totalMemory()
        val free = runtime.freeMemory()
        val used = (total - free).coerceAtLeast(0L)
        return "Heap bytes: used=$used free=$free total=$total max=${runtime.maxMemory()}"
    }
}
