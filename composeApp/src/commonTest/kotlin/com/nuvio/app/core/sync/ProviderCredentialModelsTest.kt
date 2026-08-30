package com.nuvio.app.core.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ProviderCredentialModelsTest {
    @Test
    fun `remote values replace only supported local providers`() {
        val local = ProviderCredentialSnapshot(
            profileId = 2,
            values = listOf(
                ProviderCredentialValue("debrid:torbox", "api_key", "local-torbox"),
                ProviderCredentialValue("animeskip", "client_id", "local-anime"),
            ),
        )
        val remote = listOf(
            SupabaseProviderCredential(
                provider = "debrid:torbox",
                credentialJson = buildJsonObject { put("api_key", "remote-torbox") },
            ),
            SupabaseProviderCredential(
                provider = "unsupported",
                credentialJson = buildJsonObject { put("api_key", "ignored") },
            ),
        )

        val merged = local.mergeRemote(remote)

        assertEquals("remote-torbox", merged.values[0].value)
        assertEquals("local-anime", merged.values[1].value)
    }

    @Test
    fun `undated remote blank cannot erase a migrated local key`() {
        val local = ProviderCredentialSnapshot(
            profileId = 1,
            values = listOf(ProviderCredentialValue("mdblist", "api_key", "local")),
        )
        val remote = listOf(
            SupabaseProviderCredential(
                provider = "mdblist",
                credentialJson = buildJsonObject { put("api_key", "") },
            ),
        )

        assertEquals("local", local.mergeRemote(remote).values.single().value)
    }

    @Test
    fun `stale remote blank cannot erase a newer local key`() {
        val local = ProviderCredentialSnapshot(
            profileId = 1,
            values = listOf(
                ProviderCredentialValue("tmdb", "api_key", "local", updatedAtEpochMs = 2_000L),
            ),
        )
        val remote = listOf(
            SupabaseProviderCredential(
                provider = "tmdb",
                credentialJson = buildJsonObject { put("api_key", "") },
                updatedAt = "1970-01-01T00:00:01Z",
            ),
        )

        assertEquals("local", local.mergeRemote(remote).values.single().value)
        assertEquals(true, local.hasLocallyWonRemoteConflict(remote))
    }

    @Test
    fun `newer explicit remote deletion clears local key`() {
        val local = ProviderCredentialSnapshot(
            profileId = 1,
            values = listOf(
                ProviderCredentialValue("mdblist", "api_key", "local", updatedAtEpochMs = 1_000L),
            ),
        )
        val remote = listOf(
            SupabaseProviderCredential(
                provider = "mdblist",
                credentialJson = buildJsonObject { put("api_key", "") },
                updatedAt = "1970-01-01T00:00:02Z",
            ),
        )

        val merged = local.mergeRemote(remote)

        assertEquals("", merged.values.single().value)
        assertEquals(2_000L, merged.values.single().updatedAtEpochMs)
    }

    @Test
    fun `equal remote deletion does not clear local key`() {
        val local = ProviderCredentialSnapshot(
            profileId = 1,
            values = listOf(
                ProviderCredentialValue("tmdb", "api_key", "local", updatedAtEpochMs = 2_000L),
            ),
        )
        val remote = listOf(
            SupabaseProviderCredential(
                provider = "tmdb",
                credentialJson = buildJsonObject { put("api_key", "") },
                updatedAt = "1970-01-01T00:00:02Z",
            ),
        )

        assertEquals("local", local.mergeRemote(remote).values.single().value)
    }

    @Test
    fun `newer remote nonblank key syncs across devices`() {
        val local = ProviderCredentialSnapshot(
            profileId = 1,
            values = listOf(
                ProviderCredentialValue("mdblist", "api_key", "old", updatedAtEpochMs = 1_000L),
            ),
        )
        val remote = listOf(
            SupabaseProviderCredential(
                provider = "mdblist",
                credentialJson = buildJsonObject { put("api_key", "new") },
                updatedAt = "1970-01-01T00:00:02Z",
            ),
        )

        assertEquals("new", local.mergeRemote(remote).values.single().value)
    }

    @Test
    fun `persisted revision still protects key after process restart`() {
        val persistedValue = "local"
        val persistedRevision = 5_000L
        val restartedSnapshot = ProviderCredentialSnapshot(
            profileId = 1,
            values = listOf(
                ProviderCredentialValue("tmdb", "api_key", persistedValue, persistedRevision),
            ),
        )
        val staleRemote = listOf(
            SupabaseProviderCredential(
                provider = "tmdb",
                credentialJson = buildJsonObject { put("api_key", "") },
                updatedAt = "1970-01-01T00:00:04Z",
            ),
        )

        assertEquals("local", restartedSnapshot.mergeRemote(staleRemote).values.single().value)
    }
}
