package com.nuvio.app.core.sync

import com.nuvio.app.core.time.parseZonedIsoDateTimeToEpochMs
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

internal const val PROVIDER_API_KEY_FIELD = "api_key"
internal const val PROVIDER_CLIENT_ID_FIELD = "client_id"

internal object ProviderCredentialIds {
    const val TMDB = "tmdb"
    const val MDBLIST = "mdblist"
    const val ANIMESKIP = "animeskip"
    const val INTRODB = "introdb"

    fun debrid(providerId: String): String = "debrid:$providerId"
}

internal data class ProviderCredentialValue(
    val provider: String,
    val field: String,
    val value: String,
    val updatedAtEpochMs: Long? = null,
) {
    fun credentialJson(): JsonObject = buildJsonObject {
        put(field, value.trim())
    }
}

internal data class ProviderCredentialSnapshot(
    val profileId: Int,
    val values: List<ProviderCredentialValue>,
) {
    init {
        require(values.map(ProviderCredentialValue::provider).distinct().size == values.size)
    }

    fun mergeRemote(rows: List<SupabaseProviderCredential>): ProviderCredentialSnapshot {
        val remoteByProvider = rows.associateBy { it.provider.lowercase() }
        return copy(
            values = values.map { local ->
                val remote = remoteByProvider[local.provider] ?: return@map local
                val element = remote.credentialJson[local.field] as? JsonPrimitive
                    ?: error("Invalid credential payload for ${local.provider}")
                val value = element.contentOrNull
                    ?: error("Invalid credential value for ${local.provider}")
                val remoteValue = value.trim()
                val remoteUpdatedAt = remote.updatedAt?.let(::parseZonedIsoDateTimeToEpochMs)
                when {
                    remoteValue == local.value -> local.copy(
                        updatedAtEpochMs = listOfNotNull(local.updatedAtEpochMs, remoteUpdatedAt).maxOrNull(),
                    )
                    shouldApplyRemoteCredential(local, remoteValue, remoteUpdatedAt) -> local.copy(
                        value = remoteValue,
                        updatedAtEpochMs = remoteUpdatedAt,
                    )
                    else -> local
                }
            },
        )
    }

    fun hasLocallyWonRemoteConflict(rows: List<SupabaseProviderCredential>): Boolean {
        val localByProvider = values.associateBy { it.provider }
        return rows.any { remote ->
            val local = localByProvider[remote.provider.lowercase()] ?: return@any false
            val remoteValue = (remote.credentialJson[local.field] as? JsonPrimitive)
                ?.contentOrNull
                ?.trim()
                ?: return@any false
            remoteValue != local.value && !shouldApplyRemoteCredential(
                local = local,
                remoteValue = remoteValue,
                remoteUpdatedAtEpochMs = remote.updatedAt?.let(::parseZonedIsoDateTimeToEpochMs),
            )
        }
    }
}

private fun shouldApplyRemoteCredential(
    local: ProviderCredentialValue,
    remoteValue: String,
    remoteUpdatedAtEpochMs: Long?,
): Boolean {
    val localUpdatedAt = local.updatedAtEpochMs
    if (localUpdatedAt != null) {
        return remoteUpdatedAtEpochMs != null && remoteUpdatedAtEpochMs > localUpdatedAt
    }
    // Legacy TMDB/MDBList keys are migrated as authoritative instead of being erased by an
    // undated tombstone left by an older client. Other providers retain their existing behavior.
    val supportsPersistedRevision = local.provider == ProviderCredentialIds.TMDB ||
        local.provider == ProviderCredentialIds.MDBLIST
    return !supportsPersistedRevision || remoteValue.isNotBlank() || local.value.isBlank()
}

@Serializable
internal data class SupabaseProviderCredential(
    val provider: String,
    @SerialName("credential_json") val credentialJson: JsonObject,
    @SerialName("updated_at") val updatedAt: String? = null,
)
