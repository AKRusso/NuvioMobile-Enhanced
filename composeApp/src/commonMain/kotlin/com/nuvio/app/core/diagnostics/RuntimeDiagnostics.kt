package com.nuvio.app.core.diagnostics

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

enum class DiagnosticArea {
    Startup,
    Tabs,
    Details,
    Streams,
    Player,
    Catalog,
    Settings,
    Other,
}

enum class DiagnosticEvent {
    StreamLoadStarted,
    StreamLoadFinished,
    StreamLoadTimedOut,
    StreamLoadCancelled,
    PluginRefreshStarted,
    PluginRefreshFinished,
    PluginRefreshFailed,
    PluginPersistStarted,
    PluginPersistFinished,
    PluginPersistFailed,
}

enum class MetadataLoadTrigger {
    Initial,
    SettingsChanged,
    Reconnected,
    Retry,
    Background,
}

enum class MetadataLoadPath {
    Network,
    CachedBase,
    CloudStream,
}

enum class MetadataLoadOutcome {
    Completed,
    Failed,
    Cancelled,
}

enum class MetadataPublicationStage {
    Base,
    Final,
    CacheOnly,
}

enum class ProfileBackgroundStatus {
    Preset,
    Loading,
    Loaded,
    Failed,
}

object RuntimeDiagnostics {
    private const val maxEvents = 20
    private val lock = SynchronizedObject()
    private val recentEvents = ArrayDeque<String>()
    private var area = DiagnosticArea.Startup
    private var previousArea = DiagnosticArea.Startup
    private var lastIssueArea: DiagnosticArea? = null
    private var network = "Unknown"
    private var metadataLoading = false
    private var nextMetadataOperation = 0L
    private val activeMetadataOperations = mutableMapOf<Long, MetadataLoadTrigger>()
    private var metadataCalls = 0L
    private var metadataStarted = 0L
    private var metadataCompleted = 0L
    private var metadataFailed = 0L
    private var metadataCancelled = 0L
    private var metadataCacheHits = 0L
    private var metadataCoalesced = 0L
    private var metadataBasePublications = 0L
    private var metadataFinalPublications = 0L
    private var metadataCacheOnlyPublications = 0L
    private var profileBackgroundStatus = ProfileBackgroundStatus.Preset
    private var profileBackgroundForegroundRetries = 0L
    private var streamGroups = 0
    private var streamLoadingGroups = 0
    private var streamProviderTasks = 0
    private var pluginRepositories = 0
    private var pluginScrapers = 0
    private var enabledPluginScrapers = 0
    private var totalPluginCodeChars = 0L
    private var largestPluginCodeChars = 0

    fun updateArea(value: DiagnosticArea) = synchronized(lock) {
        if (area != value) previousArea = area
        area = value
    }

    fun updateNetwork(value: String) = synchronized(lock) {
        network = value.takeIf { it in allowedNetworkStates } ?: "Unknown"
    }

    fun updateMetadataLoading(value: Boolean) = synchronized(lock) {
        metadataLoading = value
    }

    fun updateStreams(groups: Int, loadingGroups: Int, providerTasks: Int) = synchronized(lock) {
        streamGroups = groups.coerceAtLeast(0)
        streamLoadingGroups = loadingGroups.coerceAtLeast(0)
        streamProviderTasks = providerTasks.coerceAtLeast(0)
    }

    fun updateStreamLoading(groups: Int, loadingGroups: Int) = synchronized(lock) {
        streamGroups = groups.coerceAtLeast(0)
        streamLoadingGroups = loadingGroups.coerceAtLeast(0)
    }

    fun updatePlugins(
        repositories: Int,
        scrapers: Int,
        enabledScrapers: Int,
        totalCodeChars: Long,
        largestCodeChars: Int,
    ) = synchronized(lock) {
        pluginRepositories = repositories.coerceAtLeast(0)
        pluginScrapers = scrapers.coerceAtLeast(0)
        enabledPluginScrapers = enabledScrapers.coerceAtLeast(0)
        totalPluginCodeChars = totalCodeChars.coerceAtLeast(0L)
        largestPluginCodeChars = largestCodeChars.coerceAtLeast(0)
    }

    fun record(event: DiagnosticEvent) = synchronized(lock) {
        when (event) {
            DiagnosticEvent.StreamLoadTimedOut,
            DiagnosticEvent.StreamLoadCancelled,
            -> lastIssueArea = DiagnosticArea.Streams
            DiagnosticEvent.PluginRefreshFailed,
            DiagnosticEvent.PluginPersistFailed,
            -> lastIssueArea = DiagnosticArea.Settings
            else -> Unit
        }
        addRecentEvent(event.name)
    }

    fun startMetadataLoad(trigger: MetadataLoadTrigger, path: MetadataLoadPath): Long = synchronized(lock) {
        val operation = ++nextMetadataOperation
        metadataCalls += 1
        metadataStarted += 1
        activeMetadataOperations[operation] = trigger
        if (trigger != MetadataLoadTrigger.Background) {
            addRecentEvent("Metadata op=$operation trigger=${trigger.name} path=${path.name} status=Started")
        }
        operation
    }

    fun recordMetadataCacheHit(trigger: MetadataLoadTrigger) = synchronized(lock) {
        metadataCalls += 1
        metadataCacheHits += 1
        if (trigger != MetadataLoadTrigger.Background) {
            addRecentEvent("Metadata trigger=${trigger.name} disposition=CacheHit")
        }
    }

    fun recordMetadataCoalesced(
        trigger: MetadataLoadTrigger,
        additionalCall: Boolean = false,
    ) = synchronized(lock) {
        if (additionalCall) metadataCalls += 1
        metadataCoalesced += 1
        if (trigger != MetadataLoadTrigger.Background) {
            addRecentEvent("Metadata trigger=${trigger.name} disposition=Coalesced sameRequest=true")
        }
    }

    fun finishMetadataLoad(operation: Long, outcome: MetadataLoadOutcome, durationMs: Long) = synchronized(lock) {
        val trigger = activeMetadataOperations.remove(operation) ?: return@synchronized
        when (outcome) {
            MetadataLoadOutcome.Completed -> metadataCompleted += 1
            MetadataLoadOutcome.Failed -> {
                metadataFailed += 1
                lastIssueArea = DiagnosticArea.Details
            }
            MetadataLoadOutcome.Cancelled -> metadataCancelled += 1
        }
        if (trigger != MetadataLoadTrigger.Background) {
            addRecentEvent(
                "Metadata op=$operation outcome=${outcome.name} durationMs=${durationMs.coerceAtLeast(0L)}",
            )
        }
    }

    fun recordMetadataPublication(stage: MetadataPublicationStage) = synchronized(lock) {
        when (stage) {
            MetadataPublicationStage.Base -> metadataBasePublications += 1
            MetadataPublicationStage.Final -> metadataFinalPublications += 1
            MetadataPublicationStage.CacheOnly -> metadataCacheOnlyPublications += 1
        }
    }

    fun updateProfileBackgroundStatus(status: ProfileBackgroundStatus) = synchronized(lock) {
        profileBackgroundStatus = status
        if (status == ProfileBackgroundStatus.Failed) {
            addRecentEvent("ProfileBackground status=Failed")
        }
    }

    fun recordProfileBackgroundRetry() = synchronized(lock) {
        profileBackgroundForegroundRetries += 1
        profileBackgroundStatus = ProfileBackgroundStatus.Loading
        addRecentEvent("ProfileBackground foregroundRetry=$profileBackgroundForegroundRetries")
    }

    fun snapshotText(): String = synchronized(lock) {
        buildString {
            appendLine("Runtime context:")
            appendLine("Area: ${area.name} previous=${previousArea.name}")
            appendLine("Last issue area: ${lastIssueArea?.name ?: "none"}")
            appendLine("Network: $network")
            appendLine(
                "Metadata: loading=$metadataLoading active=${activeMetadataOperations.size} calls=$metadataCalls " +
                    "started=$metadataStarted completed=$metadataCompleted failed=$metadataFailed " +
                    "cancelled=$metadataCancelled cacheHits=$metadataCacheHits coalesced=$metadataCoalesced",
            )
            appendLine(
                "Metadata publications: base=$metadataBasePublications final=$metadataFinalPublications " +
                    "cacheOnly=$metadataCacheOnlyPublications",
            )
            appendLine(
                "Profile background: status=${profileBackgroundStatus.name} " +
                    "foregroundRetries=$profileBackgroundForegroundRetries",
            )
            appendLine("Streams: groups=$streamGroups loading=$streamLoadingGroups tasks=$streamProviderTasks")
            appendLine(
                "Plugins: repositories=$pluginRepositories scrapers=$pluginScrapers enabled=$enabledPluginScrapers " +
                    "sourceChars=$totalPluginCodeChars largestSourceChars=$largestPluginCodeChars",
            )
            append("Recent events: ")
            append(if (recentEvents.isEmpty()) "none" else recentEvents.joinToString(" | "))
        }
    }

    private fun addRecentEvent(event: String) {
        if (recentEvents.size == maxEvents) recentEvents.removeFirst()
        recentEvents.addLast(event)
    }

    internal fun resetForTests() = synchronized(lock) {
        recentEvents.clear()
        area = DiagnosticArea.Startup
        previousArea = DiagnosticArea.Startup
        lastIssueArea = null
        network = "Unknown"
        metadataLoading = false
        nextMetadataOperation = 0L
        activeMetadataOperations.clear()
        metadataCalls = 0L
        metadataStarted = 0L
        metadataCompleted = 0L
        metadataFailed = 0L
        metadataCancelled = 0L
        metadataCacheHits = 0L
        metadataCoalesced = 0L
        metadataBasePublications = 0L
        metadataFinalPublications = 0L
        metadataCacheOnlyPublications = 0L
        profileBackgroundStatus = ProfileBackgroundStatus.Preset
        profileBackgroundForegroundRetries = 0L
        streamGroups = 0
        streamLoadingGroups = 0
        streamProviderTasks = 0
        pluginRepositories = 0
        pluginScrapers = 0
        enabledPluginScrapers = 0
        totalPluginCodeChars = 0L
        largestPluginCodeChars = 0
    }
}

private val allowedNetworkStates = setOf(
    "Unknown",
    "Checking",
    "Online",
    "NoInternet",
    "ServersUnreachable",
)

internal fun localCrashReport(id: String, summary: String, details: String): LocalCrashReport {
    val contextSummary = details.lineSequence()
        .filter { line -> line.startsWith("Time:") || line.startsWith("Version:") }
        .take(2)
        .joinToString(" | ")
    return LocalCrashReport(id = id, summary = summary, details = details, contextSummary = contextSummary)
}

internal fun String.sanitizeDiagnosticText(): String =
    replace(Regex("""(?i)authorization\s*[:=]\s*bearer\s+[^\s,}]+"""), "Authorization=<redacted>")
        .replace(
            Regex("""(?i)[\"']?(access_token|refresh_token|token|api_key|apikey|client_secret|password)[\"']?\s*[:=]\s*[\"']?[^\"'&,\s}]+"""),
        ) { match -> "${match.groupValues[1]}=<redacted>" }
        .replace(Regex("""https?://[^\s)]+"""), "<redacted-url>")
        .replace(Regex("""[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}""", RegexOption.IGNORE_CASE), "<redacted-email>")
