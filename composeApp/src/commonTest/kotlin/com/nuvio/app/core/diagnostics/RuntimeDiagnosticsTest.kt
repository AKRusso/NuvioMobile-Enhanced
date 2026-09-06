package com.nuvio.app.core.diagnostics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RuntimeDiagnosticsTest {
    @Test
    fun diagnosticSanitizerRemovesSecretsAndPersonalEndpoints() {
        RuntimeDiagnostics.resetForTests()
        val raw = """
            token=secret
            "api_key":"private-key"
            Authorization: Bearer bearer-secret
            https://private.example/user/42?access_token=url-secret
            person@example.com
        """.trimIndent()

        val sanitized = raw.sanitizeDiagnosticText()

        assertFalse("secret" in sanitized)
        assertFalse("private.example" in sanitized)
        assertFalse("person@example.com" in sanitized)
        assertTrue("token=<redacted>" in sanitized)
        assertTrue("<redacted-url>" in sanitized)
        assertTrue("<redacted-email>" in sanitized)
    }

    @Test
    fun crashPresentationContainsOnlyTimeAndVersionHeaders() {
        RuntimeDiagnostics.resetForTests()
        val report = localCrashReport(
            id = "123",
            summary = "Failure",
            details = """
                Nuvio Enhanced local crash report
                Time: 2026-09-05 12:00:00 +0000
                Package: com.nuvio.enhanced
                Version: 0.4.13 (117)
                Device: Example
            """.trimIndent(),
        )

        assertEquals(
            "Time: 2026-09-05 12:00:00 +0000 | Version: 0.4.13 (117)",
            report.contextSummary,
        )
    }

    @Test
    fun runtimeSnapshotUsesAggregatesWithoutIdentifiers() {
        RuntimeDiagnostics.resetForTests()
        RuntimeDiagnostics.updateArea(DiagnosticArea.Streams)
        RuntimeDiagnostics.updateNetwork("Online")
        RuntimeDiagnostics.updateStreams(groups = 4, loadingGroups = 2, providerTasks = 7)
        RuntimeDiagnostics.updatePlugins(
            repositories = 2,
            scrapers = 8,
            enabledScrapers = 6,
            totalCodeChars = 12_000,
            largestCodeChars = 4_000,
        )
        RuntimeDiagnostics.record(DiagnosticEvent.StreamLoadTimedOut)

        val snapshot = RuntimeDiagnostics.snapshotText()

        assertTrue("Area: Streams" in snapshot)
        assertTrue("groups=4 loading=2 tasks=7" in snapshot)
        assertTrue("repositories=2 scrapers=8 enabled=6" in snapshot)
        assertTrue("StreamLoadTimedOut" in snapshot)
        assertTrue("Last issue area: Streams" in snapshot)
    }

    @Test
    fun metadataOperationsAreCorrelatedWithoutContentIdentifiers() {
        RuntimeDiagnostics.resetForTests()
        val operation = RuntimeDiagnostics.startMetadataLoad(
            trigger = MetadataLoadTrigger.Initial,
            path = MetadataLoadPath.Network,
        )
        RuntimeDiagnostics.recordMetadataCoalesced(MetadataLoadTrigger.Initial)
        RuntimeDiagnostics.finishMetadataLoad(
            operation = operation,
            outcome = MetadataLoadOutcome.Completed,
            durationMs = 740,
        )
        RuntimeDiagnostics.recordMetadataCacheHit(MetadataLoadTrigger.SettingsChanged)

        val snapshot = RuntimeDiagnostics.snapshotText()

        assertTrue("active=0 calls=2 started=1 completed=1 failed=0 cancelled=0 cacheHits=1 coalesced=1" in snapshot)
        assertTrue("Metadata op=1 trigger=Initial path=Network status=Started" in snapshot)
        assertTrue("Metadata op=1 outcome=Completed durationMs=740" in snapshot)
        assertTrue("trigger=SettingsChanged disposition=CacheHit" in snapshot)
        assertFalse("title" in snapshot.lowercase())
        assertFalse("http" in snapshot.lowercase())
    }

    @Test
    fun failedMetadataOperationIdentifiesDetailsAsLastIssueArea() {
        RuntimeDiagnostics.resetForTests()
        val operation = RuntimeDiagnostics.startMetadataLoad(
            trigger = MetadataLoadTrigger.Reconnected,
            path = MetadataLoadPath.CachedBase,
        )

        RuntimeDiagnostics.finishMetadataLoad(operation, MetadataLoadOutcome.Failed, durationMs = 50)

        val snapshot = RuntimeDiagnostics.snapshotText()
        assertTrue("Last issue area: Details" in snapshot)
        assertTrue("failed=1" in snapshot)
    }

    @Test
    fun profileBackgroundDiagnosticsContainOnlyStatusAndRetryCount() {
        RuntimeDiagnostics.resetForTests()
        RuntimeDiagnostics.updateProfileBackgroundStatus(ProfileBackgroundStatus.Failed)
        RuntimeDiagnostics.recordProfileBackgroundRetry()
        RuntimeDiagnostics.updateProfileBackgroundStatus(ProfileBackgroundStatus.Loaded)

        val snapshot = RuntimeDiagnostics.snapshotText()

        assertTrue("Profile background: status=Loaded foregroundRetries=1" in snapshot)
        assertTrue("ProfileBackground status=Failed" in snapshot)
        assertTrue("ProfileBackground foregroundRetry=1" in snapshot)
        assertFalse("http" in snapshot.lowercase())
    }
}
