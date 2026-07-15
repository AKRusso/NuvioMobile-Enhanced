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

    actual fun initialize(context: Any?) {
        val appContext = (context as? Context)?.applicationContext ?: return
        preferences = appContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        _pendingReport.value = loadPendingReport()
        _lastReport.value = loadLastReport() ?: _pendingReport.value
        if (installed) return
        installed = true
        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            saveCrashReport(appContext, thread, throwable)
            previousHandler?.uncaughtException(thread, throwable) ?: run {
                Process.killProcess(Process.myPid())
                exitProcess(10)
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

    private fun loadPendingReport(): LocalCrashReport? {
        val prefs = preferences ?: return null
        val id = prefs.getString(idKey, null)?.takeIf(String::isNotBlank) ?: return null
        val summary = prefs.getString(summaryKey, null)?.takeIf(String::isNotBlank) ?: "Unknown crash"
        val details = prefs.getString(detailsKey, null)?.takeIf(String::isNotBlank) ?: return null
        return LocalCrashReport(id = id, summary = summary, details = details)
    }

    private fun loadLastReport(): LocalCrashReport? {
        val prefs = preferences ?: return null
        val id = prefs.getString(lastIdKey, null)?.takeIf(String::isNotBlank) ?: return null
        val summary = prefs.getString(lastSummaryKey, null)?.takeIf(String::isNotBlank) ?: "Unknown crash"
        val details = prefs.getString(lastDetailsKey, null)?.takeIf(String::isNotBlank) ?: return null
        return LocalCrashReport(id = id, summary = summary, details = details)
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
        val report = LocalCrashReport(id = id, summary = summary, details = details)
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
            appendLine("Thread: ${thread.name}")
            appendLine("Exception: ${throwable.javaClass.name}")
            appendLine("Message: ${throwable.message?.sanitizeCrashReport().orEmpty()}")
            appendLine()
            appendLine(stackTrace)
        }
        return rawReport.take(maxReportLength)
    }

    private fun Throwable.summary(): String {
        val message = message?.sanitizeCrashReport()?.takeIf(String::isNotBlank)
        return if (message == null) javaClass.name else "${javaClass.name}: $message"
    }

    private fun String.sanitizeCrashReport(): String =
        replace(Regex("""(?i)(access_token|refresh_token|token|api_key|apikey|client_secret|password)=([^&\s]+)""")) {
            "${it.groupValues[1]}=<redacted>"
        }
            .replace(Regex("""https?://[^\s)]+""")) { match ->
                val value = match.value
                val base = value.substringBefore("?").substringBefore("#")
                if (base == value) value else "$base?<redacted>"
            }
}
