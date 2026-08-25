package com.nuvio.app.features.anime

import co.touchlab.kermit.Logger
import com.nuvio.app.features.player.skip.SkipIntroApi
import com.nuvio.app.features.simkl.SimklPkceCrypto
import com.nuvio.app.features.simkl.SimklPlatformClock
import com.nuvio.app.features.tracking.TrackingAuthProvider
import com.nuvio.app.features.tracking.TrackingCapability
import com.nuvio.app.features.tracking.TrackingProviderDescriptor
import com.nuvio.app.features.tracking.TrackingProviderId
import com.nuvio.app.features.tracking.TrackingProviderRegistry
import com.nuvio.app.features.tracking.TrackingHistoryItem
import com.nuvio.app.features.tracking.TrackingHistoryWriter
import com.nuvio.app.features.tracking.TrackingMediaReference
import com.nuvio.app.features.tracking.TrackingMutationResult
import com.nuvio.app.features.tracking.TrackingScrobbleAction
import com.nuvio.app.features.tracking.TrackingScrobbleEvent
import com.nuvio.app.features.tracking.TrackingScrobbler
import io.ktor.http.Url
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class AnimeTrackingProvider(val displayName: String) {
    ANILIST("AniList"),
    MY_ANIME_LIST("MyAnimeList"),
}

data class AnimeTrackingAuthUiState(
    val connected: Boolean = false,
    val credentialsConfigured: Boolean = false,
    val username: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

object AniListTrackingRepository : AnimeTrackingRepository(AnimeTrackingProvider.ANILIST)
object MyAnimeListTrackingRepository : AnimeTrackingRepository(AnimeTrackingProvider.MY_ANIME_LIST)

abstract class AnimeTrackingRepository(
    val animeProvider: AnimeTrackingProvider,
) : TrackingAuthProvider, TrackingScrobbler, TrackingHistoryWriter {
    final override val descriptor = TrackingProviderDescriptor(
        id = when (animeProvider) {
            AnimeTrackingProvider.ANILIST -> TrackingProviderId.ANILIST
            AnimeTrackingProvider.MY_ANIME_LIST -> TrackingProviderId.MY_ANIME_LIST
        },
        displayName = animeProvider.displayName,
        capabilities = setOf(
            TrackingCapability.AUTHENTICATION,
            TrackingCapability.WATCHED_WRITE,
            TrackingCapability.SCROBBLE,
        ),
    )
    final override val providerId: TrackingProviderId = descriptor.id
    private val log = Logger.withTag(animeProvider.displayName)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val authorizationMutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val _isAuthenticated = MutableStateFlow(false)
    final override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    private val _uiState = MutableStateFlow(AnimeTrackingAuthUiState())
    val uiState: StateFlow<AnimeTrackingAuthUiState> = _uiState.asStateFlow()
    private var loaded = false
    private var metadata = AnimeTrackingMetadata()

    init {
        TrackingProviderRegistry.register(this)
        TrackingProviderRegistry.registerScrobbler(this)
        TrackingProviderRegistry.registerHistoryWriter(this)
    }

    final override fun ensureLoaded() {
        if (loaded) return
        loaded = true
        metadata = AnimeTrackingAuthStorage.loadMetadata(animeProvider)
            ?.let { payload -> runCatching { json.decodeFromString<AnimeTrackingMetadata>(payload) }.getOrNull() }
            ?: AnimeTrackingMetadata()
        publish()
    }

    fun onConnectRequested(): String? {
        ensureLoaded()
        if (!credentialsConfigured()) {
            publish(errorMessage = "Configure o Client ID de ${animeProvider.displayName}.")
            return null
        }
        val state = randomUrlToken(24)
        AnimeTrackingAuthStorage.saveSecret(animeProvider, STATE_KEY, state)
        metadata = metadata.copy(pendingStartedAtMs = SimklPlatformClock.nowEpochMs())
        persistMetadata()
        publish(errorMessage = null)
        return when (animeProvider) {
            AnimeTrackingProvider.ANILIST ->
                "https://anilist.co/api/v2/oauth/authorize" +
                    "?client_id=${AnimeTrackingConfig.ANILIST_CLIENT_ID.encodeURLParameter()}" +
                    "&response_type=token&state=${state.encodeURLParameter()}"
            AnimeTrackingProvider.MY_ANIME_LIST -> {
                val verifier = randomUrlToken(96)
                AnimeTrackingAuthStorage.saveSecret(animeProvider, VERIFIER_KEY, verifier)
                "https://myanimelist.net/v1/oauth2/authorize" +
                    "?response_type=code&client_id=${AnimeTrackingConfig.MAL_CLIENT_ID.encodeURLParameter()}" +
                    "&code_challenge=${verifier.encodeURLParameter()}&state=${state.encodeURLParameter()}"
            }
        }
    }

    fun onDisconnectRequested() {
        clearCredentials()
        metadata = AnimeTrackingMetadata()
        persistMetadata()
        publish()
    }

    suspend fun searchAnime(query: String): List<AnimeTrackingSearchResult> {
        val normalized = query.trim()
        if (normalized.isBlank()) return emptyList()
        return when (animeProvider) {
            AnimeTrackingProvider.ANILIST -> AnimeTrackingApi.searchAniList(normalized)
            AnimeTrackingProvider.MY_ANIME_LIST -> {
                val token = validAccessToken() ?: return emptyList()
                AnimeTrackingApi.searchMal(token, normalized)
            }
        }
    }

    suspend fun resolvedMediaId(media: TrackingMediaReference): Int? = when (animeProvider) {
        AnimeTrackingProvider.ANILIST -> resolveAniListId(media)
        AnimeTrackingProvider.MY_ANIME_LIST -> resolveMalId(media)
    }

    suspend fun loadProgress(media: TrackingMediaReference): Int? {
        val token = validAccessToken() ?: return null
        val mediaId = resolvedMediaId(media) ?: return null
        return when (animeProvider) {
            AnimeTrackingProvider.ANILIST -> AnimeTrackingApi.aniListProgress(token, mediaId)
            AnimeTrackingProvider.MY_ANIME_LIST -> AnimeTrackingApi.malProgress(token, mediaId)
        }
    }

    suspend fun saveProgress(media: TrackingMediaReference, progress: Int): Boolean {
        val token = validAccessToken() ?: return false
        val mediaId = resolvedMediaId(media) ?: return false
        return when (animeProvider) {
            AnimeTrackingProvider.ANILIST -> AnimeTrackingApi.saveAniListProgress(token, mediaId, progress.coerceAtLeast(0))
            AnimeTrackingProvider.MY_ANIME_LIST -> AnimeTrackingApi.saveMalProgress(token, mediaId, progress.coerceAtLeast(0))
        }
    }

    final override fun handleAuthCallback(url: String): Boolean {
        ensureLoaded()
        val redirect = redirectUri()
        if (!url.startsWith(redirect, ignoreCase = true)) return false
        scope.launch { completeAuthorization(url) }
        return true
    }

    final override suspend fun scrobble(
        profileId: Int,
        action: TrackingScrobbleAction,
        event: TrackingScrobbleEvent,
    ) {
        if (action != TrackingScrobbleAction.STOP || event.progressPercent < 80.0) return
        if (!AnimeTrackingSettingsRepository.preferences(animeProvider).automaticScrobble) return
        val token = validAccessToken() ?: return
        val progress = event.media.episode?.number ?: 1
        val success = when (animeProvider) {
            AnimeTrackingProvider.ANILIST -> resolveAniListId(event.media)?.let {
                AnimeTrackingApi.saveAniListProgress(token, it, progress)
            }
            AnimeTrackingProvider.MY_ANIME_LIST -> resolveMalId(event.media)?.let {
                AnimeTrackingApi.saveMalProgress(token, it, progress)
            }
        } ?: false
        if (!success) log.w { "Não foi possível atualizar o progresso." }
    }

    final override suspend fun addToHistory(
        profileId: Int,
        items: Collection<TrackingHistoryItem>,
    ): TrackingMutationResult {
        if (!AnimeTrackingSettingsRepository.preferences(animeProvider).explicitWatchedSync) {
            return TrackingMutationResult(attemptedCount = 0)
        }
        return updateHistoryProgress(
            consolidateProgressUpdates(
                items.mapNotNull { item -> item.media.episode?.number?.let { item.media to it } },
                selectProgress = { values -> values.maxOf { (_, progress) -> progress } },
            ),
        )
    }

    final override suspend fun removeFromHistory(
        profileId: Int,
        items: Collection<TrackingMediaReference>,
    ): TrackingMutationResult {
        if (!AnimeTrackingSettingsRepository.preferences(animeProvider).explicitWatchedSync) {
            return TrackingMutationResult(attemptedCount = 0)
        }
        return updateHistoryProgress(
            consolidateProgressUpdates(
                items.mapNotNull { media ->
                    media.episode?.let { episode ->
                        media to (episode.continuousProgressAfterRemoval ?: episode.number - 1).coerceAtLeast(0)
                    }
                },
                selectProgress = { values -> values.minOf { (_, progress) -> progress } },
            ),
        )
    }

    final override fun onProfileChanged() {
        loaded = false
        ensureLoaded()
    }

    final override fun clearLocalState() {
        clearCredentials()
        AnimeTrackingAuthStorage.saveMetadata(animeProvider, null)
        loaded = false
        metadata = AnimeTrackingMetadata()
        publish()
    }

    final override fun removeStoredProfile(profileId: Int) {
        AnimeTrackingAuthStorage.removeProfile(profileId)
    }

    private suspend fun completeAuthorization(callbackUrl: String) = authorizationMutex.withLock {
        publish(isLoading = true, errorMessage = null)
        val expectedState = AnimeTrackingAuthStorage.loadSecret(animeProvider, STATE_KEY)
        if (expectedState.isNullOrBlank() || authorizationExpired()) {
            clearPendingAuthorization()
            publish(isLoading = false, errorMessage = "A sessão de autorização expirou.")
            return@withLock
        }
        val callback = parseCallback(callbackUrl)
        if (callback.state != expectedState) {
            clearPendingAuthorization()
            publish(isLoading = false, errorMessage = "A resposta de autorização não é válida.")
            return@withLock
        }
        val tokenResult = when (animeProvider) {
            AnimeTrackingProvider.ANILIST -> callback.accessToken?.let {
                TokenResult(it, null, callback.expiresIn ?: DEFAULT_ANILIST_EXPIRY_SECONDS)
            }
            AnimeTrackingProvider.MY_ANIME_LIST -> {
                val verifier = AnimeTrackingAuthStorage.loadSecret(animeProvider, VERIFIER_KEY)
                if (callback.code.isNullOrBlank() || verifier.isNullOrBlank()) null else {
                    AnimeTrackingApi.exchangeMalCode(callback.code, verifier)?.let {
                        TokenResult(it.accessToken, it.refreshToken, it.expiresIn)
                    }
                }
            }
        }
        if (tokenResult == null) {
            clearPendingAuthorization()
            publish(isLoading = false, errorMessage = callback.error ?: "Não foi possível concluir a autorização.")
            return@withLock
        }
        AnimeTrackingAuthStorage.saveSecret(animeProvider, ACCESS_TOKEN_KEY, tokenResult.accessToken)
        AnimeTrackingAuthStorage.saveSecret(animeProvider, REFRESH_TOKEN_KEY, tokenResult.refreshToken)
        metadata = metadata.copy(
            tokenExpiresAtMs = SimklPlatformClock.nowEpochMs() + tokenResult.expiresIn * 1_000L,
            pendingStartedAtMs = null,
        )
        clearPendingAuthorization()
        val viewer = when (animeProvider) {
            AnimeTrackingProvider.ANILIST -> AnimeTrackingApi.aniListViewer(tokenResult.accessToken)
            AnimeTrackingProvider.MY_ANIME_LIST -> AnimeTrackingApi.malViewer(tokenResult.accessToken)
        }
        metadata = metadata.copy(username = viewer?.name, userId = viewer?.id)
        persistMetadata()
        publish(isLoading = false)
    }

    private suspend fun validAccessToken(): String? {
        ensureLoaded()
        var token = AnimeTrackingAuthStorage.loadSecret(animeProvider, ACCESS_TOKEN_KEY) ?: return null
        val expiresAt = metadata.tokenExpiresAtMs ?: return token
        if (SimklPlatformClock.nowEpochMs() < expiresAt - TOKEN_EXPIRY_SKEW_MS) return token
        if (animeProvider == AnimeTrackingProvider.ANILIST) {
            onDisconnectRequested()
            return null
        }
        val refresh = AnimeTrackingAuthStorage.loadSecret(animeProvider, REFRESH_TOKEN_KEY) ?: return null
        val refreshed = AnimeTrackingApi.refreshMalToken(refresh) ?: return null
        token = refreshed.accessToken
        AnimeTrackingAuthStorage.saveSecret(animeProvider, ACCESS_TOKEN_KEY, token)
        AnimeTrackingAuthStorage.saveSecret(animeProvider, REFRESH_TOKEN_KEY, refreshed.refreshToken ?: refresh)
        metadata = metadata.copy(tokenExpiresAtMs = SimklPlatformClock.nowEpochMs() + refreshed.expiresIn * 1_000L)
        persistMetadata()
        publish()
        return token
    }

    private suspend fun updateHistoryProgress(
        updates: Collection<Pair<TrackingMediaReference, Int>>,
    ): TrackingMutationResult {
        val token = validAccessToken() ?: return TrackingMutationResult(
            attemptedCount = updates.size,
            notFoundCount = updates.size,
        )
        var notFound = 0
        updates.forEach { (media, progress) ->
            val success = when (animeProvider) {
                AnimeTrackingProvider.ANILIST -> resolveAniListId(media)?.let { mediaId ->
                    AnimeTrackingApi.saveAniListProgress(token, mediaId, progress)
                }
                AnimeTrackingProvider.MY_ANIME_LIST -> resolveMalId(media)?.let { animeId ->
                    AnimeTrackingApi.saveMalProgress(token, animeId, progress)
                }
            } ?: false
            if (!success) notFound += 1
        }
        return TrackingMutationResult(attemptedCount = updates.size, notFoundCount = notFound)
    }

    private fun consolidateProgressUpdates(
        updates: Collection<Pair<TrackingMediaReference, Int>>,
        selectProgress: (List<Pair<TrackingMediaReference, Int>>) -> Int,
    ): List<Pair<TrackingMediaReference, Int>> = updates
        .groupBy { (media, _) -> "${media.stableKey}:${media.episode?.season ?: 0}" }
        .values
        .map { values -> values.first().first to selectProgress(values) }

    private suspend fun resolveAniListId(media: TrackingMediaReference): Int? {
        manualResolution(media)?.let { resolution -> return resolution.providerMediaId }
        directCatalogId(media, "anilist")?.let { return it }
        media.ids.anilist?.toInt()?.let { return it }
        media.ids.mal?.let { mal ->
            SkipIntroApi.resolveMalToAnilist(mal.toString())?.anilist?.let { return it }
        }
        media.ids.kitsu?.let { kitsu ->
            SkipIntroApi.resolveKitsuToAnilist(kitsu.toString())?.anilist?.let { return it }
        }
        media.ids.imdb?.let { imdb ->
            SkipIntroApi.resolveImdbToAll(imdb).firstNotNullOfOrNull { it.anilist }?.let { return it }
        }
        return null
    }

    private suspend fun resolveMalId(media: TrackingMediaReference): Int? {
        manualResolution(media)?.let { resolution -> return resolution.providerMediaId }
        directCatalogId(media, "mal")?.let { return it }
        media.ids.mal?.toInt()?.let { return it }
        media.ids.anilist?.toInt()?.let { aniList -> AnimeTrackingApi.aniListMalId(aniList)?.let { return it } }
        media.ids.kitsu?.let { kitsu ->
            SkipIntroApi.resolveKitsuToMal(kitsu.toString())?.myanimelist?.let { return it }
        }
        media.ids.imdb?.let { imdb ->
            SkipIntroApi.resolveImdbToAll(imdb).firstNotNullOfOrNull { it.myanimelist }?.let { return it }
        }
        return null
    }

    private fun directCatalogId(media: TrackingMediaReference, prefix: String): Int? =
        listOfNotNull(media.catalog?.videoId, media.catalog?.contentId)
            .firstNotNullOfOrNull { value ->
                value.takeIf { it.startsWith("$prefix:", ignoreCase = true) }
                    ?.substringAfter(':')
                    ?.substringBefore(':')
                    ?.toIntOrNull()
            }

    private fun manualResolution(media: TrackingMediaReference): ManualResolution? {
        val catalog = media.catalog ?: return null
        val mapping = AnimeTrackingSettingsRepository.mapping(
            provider = animeProvider,
            contentType = catalog.contentType,
            contentId = catalog.contentId,
            season = media.episode?.season,
            episode = media.episode?.number,
        ) ?: return null
        return ManualResolution(mapping.id(animeProvider))
    }

    private fun parseCallback(url: String): AuthCallback {
        val parsed = runCatching { Url(url) }.getOrNull() ?: return AuthCallback(error = "URL inválido")
        val fragment = parsed.fragment.split('&').mapNotNull { part ->
            val pieces = part.split('=', limit = 2)
            if (pieces.size == 2) pieces[0] to pieces[1] else null
        }.toMap()
        fun value(key: String): String? = parsed.parameters[key] ?: fragment[key]
        return AuthCallback(
            code = value("code"),
            accessToken = value("access_token"),
            expiresIn = value("expires_in")?.toLongOrNull(),
            state = value("state"),
            error = value("error_description") ?: value("error"),
        )
    }

    private fun authorizationExpired(): Boolean =
        metadata.pendingStartedAtMs?.let { SimklPlatformClock.nowEpochMs() - it > AUTHORIZATION_TTL_MS } != false

    private fun credentialsConfigured(): Boolean = when (animeProvider) {
        AnimeTrackingProvider.ANILIST -> AnimeTrackingConfig.ANILIST_CLIENT_ID.isNotBlank()
        AnimeTrackingProvider.MY_ANIME_LIST -> AnimeTrackingConfig.MAL_CLIENT_ID.isNotBlank()
    }

    private fun redirectUri(): String = when (animeProvider) {
        AnimeTrackingProvider.ANILIST -> AnimeTrackingConfig.ANILIST_REDIRECT_URI
        AnimeTrackingProvider.MY_ANIME_LIST -> AnimeTrackingConfig.MAL_REDIRECT_URI
    }

    private fun randomUrlToken(size: Int): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~"
        return SimklPkceCrypto.secureRandomBytes(size)
            .joinToString("") { byte -> alphabet[(byte.toInt() and 0xff) % alphabet.length].toString() }
    }

    private fun clearPendingAuthorization() {
        AnimeTrackingAuthStorage.saveSecret(animeProvider, STATE_KEY, null)
        AnimeTrackingAuthStorage.saveSecret(animeProvider, VERIFIER_KEY, null)
        metadata = metadata.copy(pendingStartedAtMs = null)
        persistMetadata()
    }

    private fun clearCredentials() {
        listOf(ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY, STATE_KEY, VERIFIER_KEY).forEach { key ->
            AnimeTrackingAuthStorage.saveSecret(animeProvider, key, null)
        }
    }

    private fun persistMetadata() {
        AnimeTrackingAuthStorage.saveMetadata(animeProvider, json.encodeToString(metadata))
    }

    private fun publish(isLoading: Boolean = false, errorMessage: String? = null) {
        val authenticated = AnimeTrackingAuthStorage.loadSecret(animeProvider, ACCESS_TOKEN_KEY).isNullOrBlank().not()
        _isAuthenticated.value = authenticated
        _uiState.value = AnimeTrackingAuthUiState(
            connected = authenticated,
            credentialsConfigured = credentialsConfigured(),
            username = metadata.username,
            isLoading = isLoading,
            errorMessage = errorMessage,
        )
    }

    private data class AuthCallback(
        val code: String? = null,
        val accessToken: String? = null,
        val expiresIn: Long? = null,
        val state: String? = null,
        val error: String? = null,
    )

    private data class TokenResult(val accessToken: String, val refreshToken: String?, val expiresIn: Long)
    private data class ManualResolution(val providerMediaId: Int?)

    private companion object {
        const val ACCESS_TOKEN_KEY = "access_token"
        const val REFRESH_TOKEN_KEY = "refresh_token"
        const val STATE_KEY = "oauth_state"
        const val VERIFIER_KEY = "pkce_verifier"
        const val AUTHORIZATION_TTL_MS = 10 * 60 * 1_000L
        const val TOKEN_EXPIRY_SKEW_MS = 60_000L
        const val DEFAULT_ANILIST_EXPIRY_SECONDS = 365L * 24L * 60L * 60L
    }
}

@Serializable
private data class AnimeTrackingMetadata(
    val username: String? = null,
    val userId: Int? = null,
    val tokenExpiresAtMs: Long? = null,
    val pendingStartedAtMs: Long? = null,
)
