package com.nuvio.app.features.onboarding

import com.nuvio.app.features.addons.httpRequestRaw
import com.nuvio.app.features.settings.NuvioEnhancedSettingsStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class EnhancedCommunityMember(
    val username: String,
    val avatarUrl: String? = null,
    val status: String = "online",
)

@Serializable
internal data class EnhancedCommunitySnapshot(
    val serverName: String = "Nuvio Enhanced",
    val serverTag: String = "ENHD",
    val memberCount: Int = 1000,
    val onlineCount: Int? = null,
    val boostCount: Int? = null,
    val iconUrl: String? = null,
    val members: List<EnhancedCommunityMember> = emptyList(),
)

internal object EnhancedCommunityRepository {
    private const val InviteUrl =
        "https://discord.com/api/v10/invites/nuvioenhanced?with_counts=true&with_expiration=true"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val mutableSnapshot = MutableStateFlow(EnhancedCommunitySnapshot())
    val snapshot = mutableSnapshot.asStateFlow()

    private var initialized = false
    private var requestStarted = false

    fun ensureLoaded() {
        if (initialized) return
        initialized = true
        NuvioEnhancedSettingsStorage.loadCommunitySnapshot()
            ?.let { payload -> runCatching { json.decodeFromString<EnhancedCommunitySnapshot>(payload) }.getOrNull() }
            ?.let { cached -> mutableSnapshot.value = cached }
    }

    suspend fun loadOnce() {
        ensureLoaded()
        if (NuvioEnhancedSettingsStorage.loadCommunitySnapshot() != null || requestStarted) return
        requestStarted = true

        val inviteResponse = runCatching {
            httpRequestRaw(
                method = "GET",
                url = InviteUrl,
                headers = mapOf("Accept" to "application/json"),
                body = "",
            )
        }.getOrNull() ?: return
        if (inviteResponse.status !in 200..299 || inviteResponse.body.isBlank()) return

        val invite = runCatching {
            json.decodeFromString<DiscordInviteResponse>(inviteResponse.body)
        }.getOrNull() ?: return
        val guildId = invite.guild?.id ?: invite.guildId
        val widgetMembers = guildId?.let { loadWidgetMembers(it) }.orEmpty()
        val profile = invite.profile
        val iconHash = profile?.iconHash ?: invite.guild?.icon
        val snapshot = EnhancedCommunitySnapshot(
            serverName = profile?.name ?: invite.guild?.name ?: "Nuvio Enhanced",
            serverTag = profile?.tag ?: "ENHD",
            memberCount = profile?.memberCount ?: invite.memberCount ?: 1000,
            onlineCount = profile?.onlineCount ?: invite.onlineCount,
            boostCount = profile?.boostCount,
            iconUrl = if (guildId != null && iconHash != null) {
                "https://cdn.discordapp.com/icons/$guildId/$iconHash.png?size=128"
            } else {
                null
            },
            members = widgetMembers,
        )
        mutableSnapshot.value = snapshot
        NuvioEnhancedSettingsStorage.saveCommunitySnapshot(json.encodeToString(snapshot))
    }

    private suspend fun loadWidgetMembers(guildId: String): List<EnhancedCommunityMember> {
        val response = runCatching {
            httpRequestRaw(
                method = "GET",
                url = "https://discord.com/api/guilds/$guildId/widget.json",
                headers = mapOf("Accept" to "application/json"),
                body = "",
            )
        }.getOrNull() ?: return emptyList()
        if (response.status !in 200..299 || response.body.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<DiscordWidgetResponse>(response.body).members
                .filter { it.username.isNotBlank() }
                .take(10)
                .map { member ->
                    EnhancedCommunityMember(
                        username = member.username,
                        avatarUrl = member.avatarUrl,
                        status = member.status,
                    )
                }
        }.getOrDefault(emptyList())
    }
}

@Serializable
private data class DiscordInviteResponse(
    @SerialName("guild_id") val guildId: String? = null,
    @SerialName("approximate_member_count") val memberCount: Int? = null,
    @SerialName("approximate_presence_count") val onlineCount: Int? = null,
    val guild: DiscordGuild? = null,
    val profile: DiscordProfile? = null,
)

@Serializable
private data class DiscordGuild(
    val id: String? = null,
    val name: String? = null,
    val icon: String? = null,
)

@Serializable
private data class DiscordProfile(
    val name: String? = null,
    val tag: String? = null,
    @SerialName("icon_hash") val iconHash: String? = null,
    @SerialName("member_count") val memberCount: Int? = null,
    @SerialName("online_count") val onlineCount: Int? = null,
    @SerialName("premium_subscription_count") val boostCount: Int? = null,
)

@Serializable
private data class DiscordWidgetResponse(
    val members: List<DiscordWidgetMember> = emptyList(),
)

@Serializable
private data class DiscordWidgetMember(
    val username: String = "",
    val status: String = "online",
    @SerialName("avatar_url") val avatarUrl: String? = null,
)
