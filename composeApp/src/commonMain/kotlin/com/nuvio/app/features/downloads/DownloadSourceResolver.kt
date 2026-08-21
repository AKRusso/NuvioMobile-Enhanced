package com.nuvio.app.features.downloads

import co.touchlab.kermit.Logger
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.addons.buildAddonResourceUrl
import com.nuvio.app.features.addons.enabledAddons
import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.cloudstream.CloudStreamRepository
import com.nuvio.app.features.cloudstream.parseCloudStreamRouteId
import com.nuvio.app.features.debrid.DirectDebridPlayableResult
import com.nuvio.app.features.debrid.DirectDebridPlaybackResolver
import com.nuvio.app.features.plugins.PluginRepository
import com.nuvio.app.features.streams.CloudStreamProviderGroup
import com.nuvio.app.features.streams.StreamItem
import com.nuvio.app.features.streams.StreamParser
import com.nuvio.app.features.streams.buildCloudStreamSearchRequest
import com.nuvio.app.features.streams.cloudStreamAddonId
import com.nuvio.app.features.streams.cloudStreamProviderGroupsForRequest
import com.nuvio.app.features.streams.cloudStreamSourcesToStreamItems
import com.nuvio.app.features.streams.resolveCloudStreamProviderStreams
import com.nuvio.app.features.streams.streamAddonInstanceId
import com.nuvio.app.features.streams.toStreamItem
import com.nuvio.app.features.plugins.pluginContentId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

internal data class DownloadSourceOption(
    val providerName: String,
    val providerAddonId: String,
    val providerManifestUrl: String? = null,
    val qualityKey: String,
    val qualityLabel: String,
    val stream: StreamItem,
)

internal data class EpisodeDownloadTarget(
    val videoId: String,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val title: String?,
    val thumbnail: String?,
    val overview: String?,
    val embeddedStreams: List<StreamItem> = emptyList(),
    val parentMetaId: String? = null,
    val parentMetaType: String? = null,
    val searchTitle: String? = null,
)

/** User-configurable options for multi-episode downloads (Auto Select etc.). */
internal data class EpisodeDownloadSettings(
    val autoSelect: Boolean = false,
    val preferredQuality: DownloadPreferredQuality = DownloadPreferredQuality.Best,
    val downloadMode: DownloadQueueMode = DownloadQueueMode.AllAtOnce,
)

/**
 * Persists [EpisodeDownloadSettings] across app launches so the user's Auto Select
 * preference (and the other download options) survive reopening the sheet.
 */
internal object EpisodeDownloadSettingsStorage {
    fun load(): EpisodeDownloadSettings {
        val raw = DownloadsStorage.loadEpisodeDownloadSettings() ?: return EpisodeDownloadSettings()
        val parts = raw.split('|')
        fun part(index: Int): String? = parts.getOrNull(index)?.trim()?.takeIf { it.isNotBlank() }
        return EpisodeDownloadSettings(
            autoSelect = part(0) == "1",
            preferredQuality = part(1)?.let(::preferredQualityFromKey) ?: DownloadPreferredQuality.Best,
            downloadMode = part(2)?.let(::downloadModeFromKey) ?: DownloadQueueMode.AllAtOnce,
        )
    }

    fun save(settings: EpisodeDownloadSettings) {
        DownloadsStorage.saveEpisodeDownloadSettings(
            listOf(
                if (settings.autoSelect) "1" else "0",
                preferredQualityKey(settings.preferredQuality),
                downloadModeKey(settings.downloadMode),
            ).joinToString("|"),
        )
    }

    private fun preferredQualityKey(quality: DownloadPreferredQuality): String = when (quality) {
        DownloadPreferredQuality.Best -> "best"
        DownloadPreferredQuality.Q2160 -> "2160"
        DownloadPreferredQuality.Q1080 -> "1080"
        DownloadPreferredQuality.Q720 -> "720"
        DownloadPreferredQuality.Q480 -> "480"
        DownloadPreferredQuality.Q360 -> "360"
    }

    private fun preferredQualityFromKey(key: String): DownloadPreferredQuality? = when (key) {
        "best" -> DownloadPreferredQuality.Best
        "2160" -> DownloadPreferredQuality.Q2160
        "1080" -> DownloadPreferredQuality.Q1080
        "720" -> DownloadPreferredQuality.Q720
        "480" -> DownloadPreferredQuality.Q480
        "360" -> DownloadPreferredQuality.Q360
        else -> null
    }

    private fun downloadModeKey(mode: DownloadQueueMode): String = when (mode) {
        DownloadQueueMode.AllAtOnce -> "all"
        DownloadQueueMode.OneAtATime -> "one"
    }

    private fun downloadModeFromKey(key: String): DownloadQueueMode? = when (key) {
        "all" -> DownloadQueueMode.AllAtOnce
        "one" -> DownloadQueueMode.OneAtATime
        else -> null
    }
}

/** Per-episode progress reported while the automatic multi-episode selection runs. */
internal data class EpisodeAutoSelectStatus(
    val target: EpisodeDownloadTarget,
    val state: ProviderSearchState,
    val providerName: String? = null,
    val qualityLabel: String? = null,
)

internal data class BatchDownloadResult(
    val started: Int,
    val replaced: Int,
    val awaitingSource: Int,
)

internal enum class ProviderSearchState {
    Searching,
    Found,
    NoSources,
    Failed,
}

internal data class ProviderSearchStatus(
    val providerAddonId: String,
    val providerName: String,
    val state: ProviderSearchState,
    val sourceCount: Int = 0,
    val errorMessage: String? = null,
)

internal object DownloadSourceResolver {
    suspend fun options(
        contentType: String,
        target: EpisodeDownloadTarget,
    ): List<DownloadSourceOption> = resolveOptions(contentType, target, onProgress = {})

    suspend fun findMatchingStream(
        contentType: String,
        target: EpisodeDownloadTarget,
        providerAddonId: String,
        providerName: String,
        providerManifestUrl: String?,
        qualityKey: String,
    ): StreamItem? {
        val matchingEmbedded = target.embeddedStreams.firstOrNull { stream ->
            stream.addonId == providerAddonId &&
                stream.downloadQualityKey() == qualityKey &&
                isDownloadCandidate(stream)
        }
        if (matchingEmbedded != null) return matchingEmbedded

        return fetchProviderStreams(
            contentType = contentType,
            target = target,
            providerAddonId = providerAddonId,
            providerName = providerName,
            providerManifestUrl = providerManifestUrl,
        ).firstOrNull { stream ->
            stream.downloadQualityKey() == qualityKey && isDownloadCandidate(stream)
        }
    }

    /**
     * Resolves downloadable sources using the addons/plugins already installed for the
     * current user. Reuses the same addon stream endpoints and download-candidate rules as
     * the Streams screen, reporting per-provider progress as each addon is queried.
     */
    suspend fun resolveOptions(
        contentType: String,
        target: EpisodeDownloadTarget,
        onProgress: suspend (List<ProviderSearchStatus>) -> Unit,
        onFound: suspend (List<DownloadSourceOption>) -> Unit = {},
    ): List<DownloadSourceOption> {
        val log = Logger.withTag("DownloadSources")
        val startedAt = DownloadsClock.nowEpochMs()

        val embedded = target.embeddedStreams.filter(::isDownloadCandidate)
        if (embedded.isNotEmpty()) {
            log.d { "embedded: reusing ${embedded.size} already-loaded download source(s) for id=${target.videoId}" }
            return embedded.mapNotNull(StreamItem::toDownloadOption).toDownloadOptions()
        }

        val cloudStreamRoute = parseCloudStreamRouteId(target.videoId)
        if (cloudStreamRoute != null) {
            // Mirror the Streams screen: a CloudStream route id resolves only through its
            // owning provider, so no other addon/scraper/CloudStream group is queried.
            CloudStreamRepository.initialize()
            val providerName = CloudStreamRepository.uiState.value.plugins
                .firstOrNull { it.metadata.id.value == cloudStreamRoute.providerId }
                ?.metadata
                ?.let { metadata -> metadata.name.ifBlank { metadata.internalName } }
                ?: "CloudStream"
            val providers = listOf(
                SourceProvider(
                    id = cloudStreamAddonId(cloudStreamRoute.providerId),
                    name = providerName,
                    manifestUrl = null,
                ),
            )
            log.d {
                "discovery: cloudstream route provider=$providerName id=${providers[0].id} type=$contentType id=${target.videoId}"
            }
            return resolveProvidersWithProgress(
                providers = providers,
                contentType = contentType,
                target = target,
                onProgress = onProgress,
                onFound = onFound,
            )
        }

        AddonRepository.initialize()
        // Wait (bounded) until every enabled addon manifest has finished loading so that
        // no compatible addon is silently skipped because its manifest was still pending.
        withTimeoutOrNull(MANIFEST_LOAD_WAIT_MS) {
            AddonRepository.uiState.first { state ->
                val enabled = state.addons.filter { it.enabled }
                enabled.isEmpty() || enabled.all { it.manifest != null || !it.isRefreshing }
            }
        }?.let {
            log.d { "manifests: enabled addon manifests ready for type=$contentType id=${target.videoId}" }
        }
        val installed = AddonRepository.uiState.value.addons.enabledAddons()
        val withoutManifest = installed.filter { it.manifest == null }
        if (withoutManifest.isNotEmpty()) {
            log.w {
                "discovery: ${withoutManifest.size} enabled addon(s) have no manifest and will be skipped: " +
                    withoutManifest.joinToString { it.manifestUrl }
            }
        }
        val compatibleAddons = installed.mapNotNull { addon ->
            val manifest = addon.manifest ?: return@mapNotNull null
            val supportsStreams = manifest.resources.any { resource ->
                resource.name == "stream" &&
                    resource.types.contains(contentType) &&
                    (resource.idPrefixes.isEmpty() || resource.idPrefixes.any(target.videoId::startsWith))
            }
            if (!supportsStreams) return@mapNotNull null
            SourceProvider(
                id = addon.streamAddonInstanceId(manifest.id),
                name = addon.displayTitle.ifBlank { manifest.name },
                // Reuse the addon's transport URL exactly like the Streams screen does.
                manifestUrl = manifest.transportUrl.ifBlank { addon.manifestUrl },
            )
        }
        val scrapers = if (AppFeaturePolicy.pluginsEnabled) {
            PluginRepository.getEnabledScrapersForType(contentType)
        } else {
            emptyList()
        }
        val cloudStreamRequest = target.cloudStreamSearchRequest(contentType)
        val cloudStreamProviders = if (AppFeaturePolicy.pluginsEnabled) {
            cloudStreamProviderGroupsForRequest(contentType, cloudStreamRequest)
        } else {
            emptyList()
        }
        val providers = compatibleAddons + scrapers.map { scraper ->
            SourceProvider(
                id = "plugin:${scraper.id}",
                name = scraper.name,
                manifestUrl = null,
            )
        } + cloudStreamProviders.map { group ->
            SourceProvider(
                id = group.addonId,
                name = group.addonName,
                manifestUrl = null,
            )
        }

        log.d {
            "discovery: ${installed.size} installed addon(s); ${compatibleAddons.size} support downloads; " +
                "${scrapers.size} plugin scraper(s); ${cloudStreamProviders.size} cloudstream provider(s); " +
                "type=$contentType id=${target.videoId}"
        }
        compatibleAddons.forEach { provider ->
            log.d { "  addon supports downloads: ${provider.name} (id=${provider.id})" }
        }
        if (cloudStreamRequest == null) {
            log.w { "  cloudstream: no search request could be built for id=${target.videoId}; search groups skipped" }
        }

        return resolveProvidersWithProgress(
            providers = providers,
            contentType = contentType,
            target = target,
            onProgress = onProgress,
            onFound = onFound,
        )
    }

    private suspend fun resolveProvidersWithProgress(
        providers: List<SourceProvider>,
        contentType: String,
        target: EpisodeDownloadTarget,
        onProgress: suspend (List<ProviderSearchStatus>) -> Unit,
        onFound: suspend (List<DownloadSourceOption>) -> Unit = {},
    ): List<DownloadSourceOption> {
        val log = Logger.withTag("DownloadSources")
        val startedAt = DownloadsClock.nowEpochMs()

        val statuses = providers.map { provider ->
            ProviderSearchStatus(
                providerAddonId = provider.id,
                providerName = provider.name,
                state = ProviderSearchState.Searching,
            )
        }.toMutableList()
        val statusLock = Mutex()
        suspend fun publishProgress() {
            onProgress(statusLock.withLock { statuses.toList() })
        }
        publishProgress()

        val collected = mutableListOf<DownloadSourceOption>()
        val resolved = withTimeoutOrNull(DOWNLOAD_SOURCE_TOTAL_TIMEOUT_MS) {
            // Query every compatible provider concurrently (bounded) so that a slow or
            // unresponsive addon can no longer starve the providers behind it. Each
            // provider keeps its own request timeout and publishes its status when done.
            val semaphore = Semaphore(MAX_CONCURRENT_PROVIDERS)
            providers.mapIndexed { index, provider ->
                async {
                    semaphore.withPermit {
                        val url = provider.manifestUrl?.let {
                            buildAddonResourceUrl(
                                manifestUrl = it,
                                resource = "stream",
                                type = contentType,
                                id = target.videoId,
                            )
                        }
                        log.d { "request: ${provider.name} url=${url ?: "scraper:${provider.id}"}" }
                        val requestStartedAt = DownloadsClock.nowEpochMs()
                        val result = fetchProviderStreamsResult(
                            contentType = contentType,
                            target = target,
                            providerAddonId = provider.id,
                            providerName = provider.name,
                            providerManifestUrl = provider.manifestUrl,
                        )
                        val streams = result.getOrDefault(emptyList())
                        val options = streams.mapNotNull(StreamItem::toDownloadOption)
                        val elapsedMs = DownloadsClock.nowEpochMs() - requestStartedAt
                        when {
                            result.isFailure -> {
                                val error = result.exceptionOrNull()
                                log.w { "failed: ${provider.name} error=${error?.message ?: "unknown"} (${elapsedMs}ms)" }
                            }
                            options.isNotEmpty() -> {
                                log.d { "success: ${provider.name} -> ${options.size} download source(s) (${elapsedMs}ms)" }
                            }
                            else -> {
                                log.d { "no sources: ${provider.name} returned ${streams.size} stream(s), none downloadable (${elapsedMs}ms)" }
                            }
                        }
                        if (options.isNotEmpty()) {
                            // Stream the found sources to the UI immediately so the user can
                            // start picking/downloading while the remaining providers fetch.
                            onFound(options)
                        }
                        statusLock.withLock {
                            statuses[index] = statuses[index].copy(
                                state = when {
                                    result.isFailure -> ProviderSearchState.Failed
                                    options.isNotEmpty() -> ProviderSearchState.Found
                                    else -> ProviderSearchState.NoSources
                                },
                                sourceCount = options.size,
                                errorMessage = result.exceptionOrNull()?.message,
                            )
                        }
                        publishProgress()
                        options
                    }
                }
            }.forEach { deferred ->
                collected += deferred.await()
            }
            collected.toDownloadOptions()
        }

        val finalOptions = resolved ?: collected.toDownloadOptions()
        val finalStatuses = statusLock.withLock { statuses.toList() }
        val stillSearching = finalStatuses.count { it.state == ProviderSearchState.Searching }
        if (stillSearching > 0) {
            log.w { "discovery: $stillSearching provider(s) did not respond; marking them as unavailable" }
            statusLock.withLock {
                statuses.indices.forEach { i ->
                    if (statuses[i].state == ProviderSearchState.Searching) {
                        statuses[i] = statuses[i].copy(
                            state = ProviderSearchState.Failed,
                            errorMessage = "Timed out",
                        )
                    }
                }
            }
        }
        val elapsedMs = DownloadsClock.nowEpochMs() - startedAt
        if (resolved == null) {
            log.w { "discovery: TIMED OUT after ${elapsedMs}ms -> ${finalOptions.size} download source(s) so far" }
        } else {
            log.d {
                "discovery: COMPLETE in ${elapsedMs}ms -> ${finalOptions.size} download source(s) " +
                    "from ${finalStatuses.count { it.state == ProviderSearchState.Found }} provider(s)"
            }
        }
        publishProgress()
        return finalOptions
    }

    /**
     * Resolves the best downloadable source for every target episode independently, so a
     * single episode without sources never blocks the others. Episodes are scanned with a
     * bounded number of concurrent provider queries and progress is reported per episode.
     */
    suspend fun autoSelectOptions(
        contentType: String,
        targets: List<EpisodeDownloadTarget>,
        preferredQuality: DownloadPreferredQuality,
        onProgress: suspend (List<EpisodeAutoSelectStatus>) -> Unit,
    ): List<Pair<EpisodeDownloadTarget, DownloadSourceOption?>> {
        val log = Logger.withTag("DownloadSources")
        if (targets.isEmpty()) return emptyList()

        val statuses = targets.map { target ->
            EpisodeAutoSelectStatus(target = target, state = ProviderSearchState.Searching)
        }.toMutableList()
        val statusLock = Mutex()
        suspend fun publishProgress() {
            onProgress(statusLock.withLock { statuses.toList() })
        }
        publishProgress()

        val results = MutableList(targets.size) { index -> targets[index] to (null as DownloadSourceOption?) }
        // Each episode is bounded individually by resolveOptions' own total timeout, so the
        // batch timeout only needs to cover the number of concurrent rounds this batch takes.
        // Scaling it with the episode count keeps late episodes from being cut off while still
        // guaranteeing the automatic selection always terminates.
        val rounds = (targets.size + MAX_CONCURRENT_EPISODE_SELECTIONS - 1) / MAX_CONCURRENT_EPISODE_SELECTIONS
        val batchTimeoutMs = rounds * DOWNLOAD_SOURCE_TOTAL_TIMEOUT_MS + MANIFEST_LOAD_WAIT_MS + 10_000L
        log.d { "auto-select: resolving ${targets.size} episode(s) with batch timeout ${batchTimeoutMs}ms" }
        withTimeoutOrNull(batchTimeoutMs) {
            val semaphore = Semaphore(MAX_CONCURRENT_EPISODE_SELECTIONS)
            coroutineScope {
                targets.mapIndexed { index, target ->
                    async {
                        semaphore.withPermit {
                            val chosen = resolveOptions(contentType, target, onProgress = {})
                                .pickBestForQuality(preferredQuality)
                            if (chosen != null) {
                                log.d {
                                    "auto-select: episode=${target.episodeLogLabel()} -> " +
                                        "${chosen.providerName} ${chosen.qualityKey} (${chosen.providerAddonId})"
                                }
                                statusLock.withLock {
                                    statuses[index] = EpisodeAutoSelectStatus(
                                        target = target,
                                        state = ProviderSearchState.Found,
                                        providerName = chosen.providerName,
                                        qualityLabel = chosen.qualityLabel,
                                    )
                                }
                            } else {
                                log.w { "auto-select: episode=${target.episodeLogLabel()} -> no downloadable source found" }
                                statusLock.withLock {
                                    statuses[index] = EpisodeAutoSelectStatus(
                                        target = target,
                                        state = ProviderSearchState.NoSources,
                                    )
                                }
                            }
                            publishProgress()
                            target to chosen
                        }
                    }
                }.forEachIndexed { index, deferred ->
                    results[index] = deferred.await()
                }
            }
        }

        val finalStatuses = statusLock.withLock { statuses.toList() }
        val stillSearching = finalStatuses.count { it.state == ProviderSearchState.Searching }
        if (stillSearching > 0) {
            log.w { "auto-select: $stillSearching episode(s) did not respond in time; marking them as unavailable" }
            statusLock.withLock {
                statuses.indices.forEach { index ->
                    if (statuses[index].state == ProviderSearchState.Searching) {
                        statuses[index] = EpisodeAutoSelectStatus(
                            target = targets[index],
                            state = ProviderSearchState.Failed,
                        )
                    }
                }
            }
        }
        publishProgress()
        return results
    }

    private suspend fun fetchProviderStreams(
        contentType: String,
        target: EpisodeDownloadTarget,
        providerAddonId: String,
        providerName: String,
        providerManifestUrl: String?,
    ): List<StreamItem> =
        fetchProviderStreamsResult(
            contentType = contentType,
            target = target,
            providerAddonId = providerAddonId,
            providerName = providerName,
            providerManifestUrl = providerManifestUrl,
        ).getOrDefault(emptyList())

    private suspend fun fetchProviderStreamsResult(
        contentType: String,
        target: EpisodeDownloadTarget,
        providerAddonId: String,
        providerName: String,
        providerManifestUrl: String?,
    ): Result<List<StreamItem>> {
        if (providerAddonId.startsWith("cloudstream:")) {
            return runCatching {
                CloudStreamRepository.initialize()
                val route = parseCloudStreamRouteId(target.videoId)
                if (route != null && cloudStreamAddonId(route.providerId) == providerAddonId) {
                    // Direct CloudStream route: load links for that exact episode/title.
                    val sources = withTimeoutOrNull(CLOUDSTREAM_DOWNLOAD_PROVIDER_TIMEOUT_MS) {
                        CloudStreamRepository.loadLinks(route.providerId, route.data).getOrThrow()
                    } ?: error("Timed out")
                    cloudStreamSourcesToStreamItems(
                        providerId = route.providerId,
                        providerName = providerName,
                        sources = sources,
                    )
                } else {
                    // Search-based CloudStream provider group, resolved the same way the
                    // Streams screen resolves it (title matching + loadLinks verification).
                    val plugin = CloudStreamRepository.uiState.value.plugins
                        .firstOrNull { cloudStreamAddonId(it.metadata.id.value) == providerAddonId }
                        ?: error("CloudStream provider unavailable: $providerAddonId")
                    val group = CloudStreamProviderGroup(
                        addonId = providerAddonId,
                        addonName = providerName,
                        providerId = plugin.metadata.id.value,
                    )
                    val resolved = withTimeoutOrNull(CLOUDSTREAM_DOWNLOAD_PROVIDER_TIMEOUT_MS) {
                        resolveCloudStreamProviderStreams(
                            providerGroup = group,
                            request = target.cloudStreamSearchRequest(contentType),
                        )
                    }
                    resolved?.streams.orEmpty()
                }
            }
        }
        if (providerAddonId.startsWith("plugin:")) {
            val scraperId = providerAddonId.removePrefix("plugin:")
            val scraper = PluginRepository.getEnabledScrapersForType(contentType)
                .firstOrNull { it.id == scraperId }
            if (scraper == null) {
                return Result.failure(IllegalStateException("Scraper unavailable: $scraperId"))
            }
            return runCatching {
                val results = withTimeoutOrNull(SOURCE_PROVIDER_TIMEOUT_MS) {
                    PluginRepository.executeScraper(
                        scraper = scraper,
                        tmdbId = pluginContentId(
                            videoId = target.videoId,
                            season = target.seasonNumber,
                            episode = target.episodeNumber,
                        ),
                        mediaType = contentType,
                        season = target.seasonNumber,
                        episode = target.episodeNumber,
                    ).getOrThrow()
                } ?: error("Timed out")
                results.map { result ->
                    result.toStreamItem(
                        scraper = scraper,
                        addonName = providerName,
                        addonId = providerAddonId,
                    )
                }
            }
        }

        val manifestUrl = providerManifestUrl ?: return Result.failure(IllegalStateException("Missing manifest URL"))
        val url = buildAddonResourceUrl(
            manifestUrl = manifestUrl,
            resource = "stream",
            type = contentType,
            id = target.videoId,
        )
        return runCatching {
            val payload = withTimeoutOrNull(SOURCE_PROVIDER_TIMEOUT_MS) {
                httpGetText(url)
            } ?: error("Timed out")
            StreamParser.parse(
                payload = payload,
                addonName = providerName,
                addonId = providerAddonId,
            )
        }
    }
}

private data class SourceProvider(
    val id: String,
    val name: String,
    val manifestUrl: String?,
)

private fun StreamItem.toDownloadOption(): DownloadSourceOption? =
    takeIf(::isDownloadCandidate)?.let {
        DownloadSourceOption(
            providerName = addonName,
            providerAddonId = addonId,
            providerManifestUrl = providerManifestUrl(),
            qualityKey = downloadQualityKey(),
            qualityLabel = downloadQualityLabel(),
            stream = it,
        )
    }

internal fun List<DownloadSourceOption>.toDownloadOptions(): List<DownloadSourceOption> =
    distinctBy { option ->
        listOf(option.providerAddonId, option.qualityKey, option.stream.streamLabel).joinToString("|")
    }
        .sortedWith(compareBy<DownloadSourceOption> { it.providerName.lowercase() }.thenBy { it.qualityLabel.lowercase() })

internal object EpisodeDownloadCoordinator {
    suspend fun enqueue(
        contentType: String,
        parentMetaId: String,
        parentMetaType: String,
        title: String,
        logo: String?,
        poster: String?,
        background: String?,
        targets: List<EpisodeDownloadTarget>,
        selectedOption: DownloadSourceOption,
        removeResolvedPending: Boolean = false,
        queueMode: DownloadQueueMode = DownloadQueueMode.AllAtOnce,
    ): BatchDownloadResult {
        val log = Logger.withTag("DownloadSources")
        var started = 0
        var replaced = 0
        var awaitingSource = 0

        targets.forEach { target ->
            val stream = if (target.videoId == targets.firstOrNull()?.videoId) {
                selectedOption.stream
            } else {
                try {
                    DownloadSourceResolver.findMatchingStream(
                        contentType = contentType,
                        target = target,
                        providerAddonId = selectedOption.providerAddonId,
                        providerName = selectedOption.providerName,
                        providerManifestUrl = selectedOption.providerManifestUrl,
                        qualityKey = selectedOption.qualityKey,
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    log.w(error) { "enqueue: matching stream failed for episode=${target.episodeLogLabel()}" }
                    null
                }
            }
            val resolvedStream = stream?.let { candidate ->
                try {
                    resolveForDownload(candidate, target.seasonNumber, target.episodeNumber)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    log.w(error) { "enqueue: stream resolution failed for episode=${target.episodeLogLabel()}" }
                    null
                }
            }
            if (resolvedStream == null) {
                queuePendingSearch(
                    contentType = contentType,
                    parentMetaId = parentMetaId,
                    parentMetaType = parentMetaType,
                    title = title,
                    logo = logo,
                    poster = poster,
                    background = background,
                    target = target,
                    providerName = selectedOption.providerName,
                    providerAddonId = selectedOption.providerAddonId,
                    providerManifestUrl = selectedOption.providerManifestUrl,
                    qualityKey = selectedOption.qualityKey,
                )
                awaitingSource++
                return@forEach
            }

            val enqueueResult = try {
                DownloadsRepository.enqueueFromStream(
                    contentType = contentType,
                    videoId = target.videoId,
                    parentMetaId = parentMetaId,
                    parentMetaType = parentMetaType,
                    title = title,
                    logo = logo,
                    poster = poster,
                    background = background,
                    seasonNumber = target.seasonNumber,
                    episodeNumber = target.episodeNumber,
                    episodeTitle = target.title,
                    episodeThumbnail = target.thumbnail,
                    episodeOverview = target.overview,
                    stream = resolvedStream,
                    queueMode = queueMode,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                log.w(error) { "enqueue: enqueue failed for episode=${target.episodeLogLabel()}" }
                DownloadEnqueueResult.MissingUrl
            }

            when (enqueueResult) {
                DownloadEnqueueResult.Started -> {
                    started++
                    if (removeResolvedPending) {
                        DownloadsRepository.removePendingSourceSearchForContent(
                            downloadLogicalContentKey(
                                parentMetaId = parentMetaId,
                                seasonNumber = target.seasonNumber,
                                episodeNumber = target.episodeNumber,
                            ),
                        )
                    }
                }
                DownloadEnqueueResult.Replaced -> {
                    replaced++
                    if (removeResolvedPending) {
                        DownloadsRepository.removePendingSourceSearchForContent(
                            downloadLogicalContentKey(
                                parentMetaId = parentMetaId,
                                seasonNumber = target.seasonNumber,
                                episodeNumber = target.episodeNumber,
                            ),
                        )
                    }
                }
                DownloadEnqueueResult.MissingUrl,
                DownloadEnqueueResult.UnsupportedFormat,
                -> {
                    queuePendingSearch(
                        contentType = contentType,
                        parentMetaId = parentMetaId,
                        parentMetaType = parentMetaType,
                        title = title,
                        logo = logo,
                        poster = poster,
                        background = background,
                        target = target,
                        providerName = selectedOption.providerName,
                        providerAddonId = selectedOption.providerAddonId,
                        providerManifestUrl = selectedOption.providerManifestUrl,
                        qualityKey = selectedOption.qualityKey,
                    )
                    awaitingSource++
                }
            }
        }
        return BatchDownloadResult(started = started, replaced = replaced, awaitingSource = awaitingSource)
    }

    /**
     * Enqueues a multi-episode download using automatic source selection. Each episode is
     * resolved independently (its own source + quality pick), so an episode without a
     * downloadable source only goes to the pending-source queue and never blocks the others.
     */
    suspend fun enqueueAuto(
        contentType: String,
        parentMetaId: String,
        parentMetaType: String,
        title: String,
        logo: String?,
        poster: String?,
        background: String?,
        targets: List<EpisodeDownloadTarget>,
        settings: EpisodeDownloadSettings,
        onProgress: suspend (List<EpisodeAutoSelectStatus>) -> Unit,
    ): BatchDownloadResult {
        val log = Logger.withTag("DownloadSources")
        val matches = DownloadSourceResolver.autoSelectOptions(
            contentType = contentType,
            targets = targets,
            preferredQuality = settings.preferredQuality,
            onProgress = onProgress,
        )
        var started = 0
        var replaced = 0
        var awaitingSource = 0

        matches.forEach { (target, option) ->
            if (option == null) {
                log.w {
                    "enqueue-auto: episode=${target.episodeLogLabel()} has no source; queued as pending search"
                }
                queuePendingSearch(
                    contentType = contentType,
                    parentMetaId = parentMetaId,
                    parentMetaType = parentMetaType,
                    title = title,
                    logo = logo,
                    poster = poster,
                    background = background,
                    target = target,
                    providerName = null,
                    providerAddonId = null,
                    providerManifestUrl = null,
                    qualityKey = null,
                )
                awaitingSource++
                return@forEach
            }

            val resolvedStream = try {
                resolveForDownload(
                    option.stream,
                    target.seasonNumber,
                    target.episodeNumber,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                log.w(error) { "enqueue-auto: episode=${target.episodeLogLabel()} resolution threw; queued as pending search" }
                null
            }
            if (resolvedStream == null) {
                log.w {
                    "enqueue-auto: episode=${target.episodeLogLabel()} source (${option.providerName}) could not be resolved; queued as pending search"
                }
                queuePendingSearch(
                    contentType = contentType,
                    parentMetaId = parentMetaId,
                    parentMetaType = parentMetaType,
                    title = title,
                    logo = logo,
                    poster = poster,
                    background = background,
                    target = target,
                    providerName = option.providerName,
                    providerAddonId = option.providerAddonId,
                    providerManifestUrl = option.providerManifestUrl,
                    qualityKey = option.qualityKey,
                )
                awaitingSource++
                return@forEach
            }

            val enqueueResult = try {
                DownloadsRepository.enqueueFromStream(
                    contentType = contentType,
                    videoId = target.videoId,
                    parentMetaId = parentMetaId,
                    parentMetaType = parentMetaType,
                    title = title,
                    logo = logo,
                    poster = poster,
                    background = background,
                    seasonNumber = target.seasonNumber,
                    episodeNumber = target.episodeNumber,
                    episodeTitle = target.title,
                    episodeThumbnail = target.thumbnail,
                    episodeOverview = target.overview,
                    stream = resolvedStream,
                    queueMode = settings.downloadMode,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                log.w(error) { "enqueue-auto: enqueue failed for episode=${target.episodeLogLabel()}" }
                DownloadEnqueueResult.MissingUrl
            }

            when (enqueueResult) {
                DownloadEnqueueResult.Started -> {
                    started++
                    log.d {
                        "enqueue-auto: episode=${target.episodeLogLabel()} started via ${option.providerName} ${option.qualityKey}"
                    }
                }
                DownloadEnqueueResult.Replaced -> {
                    replaced++
                    log.d {
                        "enqueue-auto: episode=${target.episodeLogLabel()} replaced existing download via ${option.providerName}"
                    }
                }
                DownloadEnqueueResult.MissingUrl,
                DownloadEnqueueResult.UnsupportedFormat,
                -> {
                    queuePendingSearch(
                        contentType = contentType,
                        parentMetaId = parentMetaId,
                        parentMetaType = parentMetaType,
                        title = title,
                        logo = logo,
                        poster = poster,
                        background = background,
                        target = target,
                        providerName = option.providerName,
                        providerAddonId = option.providerAddonId,
                        providerManifestUrl = option.providerManifestUrl,
                        qualityKey = option.qualityKey,
                    )
                    awaitingSource++
                }
            }
        }
        return BatchDownloadResult(started = started, replaced = replaced, awaitingSource = awaitingSource)
    }

    private fun queuePendingSearch(
        contentType: String,
        parentMetaId: String,
        parentMetaType: String,
        title: String,
        logo: String?,
        poster: String?,
        background: String?,
        target: EpisodeDownloadTarget,
        providerName: String?,
        providerAddonId: String?,
        providerManifestUrl: String?,
        qualityKey: String?,
    ) {
        DownloadsRepository.queuePendingSourceSearch(
            contentType = contentType,
            videoId = target.videoId,
            parentMetaId = parentMetaId,
            parentMetaType = parentMetaType,
            title = title,
            logo = logo,
            poster = poster,
            background = background,
            seasonNumber = target.seasonNumber,
            episodeNumber = target.episodeNumber,
            episodeTitle = target.title,
            episodeThumbnail = target.thumbnail,
            episodeOverview = target.overview,
            providerName = providerName ?: "Unknown provider",
            providerAddonId = providerAddonId ?: "",
            providerManifestUrl = providerManifestUrl,
            qualityKey = qualityKey ?: "source",
        )
    }

    suspend fun retryPending(searchId: String): Boolean {
        val pending = DownloadsRepository.pendingSourceSearch(searchId) ?: return false
        val now = DownloadsClock.nowEpochMs()
        if (pending.status != PendingSourceSearchStatus.Searching) return false
        if (now >= pending.expiresAtEpochMs || pending.attemptCount >= MAX_SOURCE_SEARCH_ATTEMPTS) {
            DownloadsRepository.updatePendingSourceSearch(
                pending.copy(status = PendingSourceSearchStatus.Expired),
            )
            return false
        }
        val target = EpisodeDownloadTarget(
            videoId = pending.videoId,
            parentMetaId = pending.parentMetaId,
            parentMetaType = pending.parentMetaType,
            seasonNumber = pending.seasonNumber,
            episodeNumber = pending.episodeNumber,
            title = pending.episodeTitle,
            thumbnail = pending.episodeThumbnail,
            overview = pending.episodeOverview,
        )
        val stream = DownloadSourceResolver.findMatchingStream(
            contentType = pending.contentType,
            target = target,
            providerAddonId = pending.providerAddonId,
            providerName = pending.providerName,
            providerManifestUrl = pending.providerManifestUrl,
            qualityKey = pending.qualityKey,
        )?.let { candidate -> resolveForDownload(candidate, pending.seasonNumber, pending.episodeNumber) }

        if (stream == null) {
            DownloadsRepository.updatePendingSourceSearch(
                pending.copy(
                    attemptCount = pending.attemptCount + 1,
                    nextAttemptAtEpochMs = now + sourceSearchRetryDelay(pending.attemptCount),
                ),
            )
            return false
        }

        val result = DownloadsRepository.enqueueFromStream(
            contentType = pending.contentType,
            videoId = pending.videoId,
            parentMetaId = pending.parentMetaId,
            parentMetaType = pending.parentMetaType,
            title = pending.title,
            logo = pending.logo,
            poster = pending.poster,
            background = pending.background,
            seasonNumber = pending.seasonNumber,
            episodeNumber = pending.episodeNumber,
            episodeTitle = pending.episodeTitle,
            episodeThumbnail = pending.episodeThumbnail,
            episodeOverview = pending.episodeOverview,
            stream = stream,
        )
        return if (result == DownloadEnqueueResult.Started || result == DownloadEnqueueResult.Replaced) {
            DownloadsRepository.removePendingSourceSearch(pending.id)
            true
        } else {
            DownloadsRepository.updatePendingSourceSearch(
                pending.copy(
                    attemptCount = pending.attemptCount + 1,
                    nextAttemptAtEpochMs = now + sourceSearchRetryDelay(pending.attemptCount),
                ),
            )
            false
        }
    }

    private suspend fun resolveForDownload(
        stream: StreamItem,
        seasonNumber: Int?,
        episodeNumber: Int?,
    ): StreamItem? {
        if (stream.playableDirectUrl != null) return stream
        if (!DirectDebridPlaybackResolver.shouldResolveToPlayableStream(stream)) return null
        return when (
            val resolved = DirectDebridPlaybackResolver.resolveToPlayableStream(
                stream = stream,
                season = seasonNumber,
                episode = episodeNumber,
            )
        ) {
            is DirectDebridPlayableResult.Success -> resolved.stream
            else -> null
        }
    }
}

internal fun StreamItem.downloadQualityKey(): String {
    val technical = clientResolve?.stream?.raw?.parsed?.resolution
        ?: clientResolve?.stream?.raw?.parsed?.quality
    val text = listOfNotNull(technical, name, title, description)
        .joinToString(" ")
        .lowercase()
    val resolution = Regex("(?<!\\d)(2160|1440|1080|720|576|480|360)p?(?!\\d)").find(text)
        ?.groupValues
        ?.getOrNull(1)
    return resolution?.let { "${it}p" } ?: text
        .replace(Regex("s\\d{1,2}e\\d{1,3}"), "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .take(72)
        .ifBlank { "source" }
}

internal fun StreamItem.downloadQualityLabel(): String =
    clientResolve?.stream?.raw?.parsed?.resolution
        ?: clientResolve?.stream?.raw?.parsed?.quality
        ?: streamLabel

private fun StreamItem.providerManifestUrl(): String? =
    addonId.takeIf { it.startsWith("addon:") }
        ?.substringAfter(':', missingDelimiterValue = "")
        ?.substringAfter(':', missingDelimiterValue = "")
        ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }

private fun isDownloadCandidate(stream: StreamItem): Boolean =
    stream.playableDirectUrl?.isSupportedDownloadUrl() == true ||
        DirectDebridPlaybackResolver.shouldResolveToPlayableStream(stream)

private fun List<DownloadSourceOption>.pickBestForQuality(
    preferred: DownloadPreferredQuality,
): DownloadSourceOption? {
    if (isEmpty()) return null
    val bestByQuality = maxByOrNull { it.downloadOptionQualityHeight() }
    val preferredHeight = preferred.resolution
    if (preferredHeight == null) return bestByQuality

    val exact = firstOrNull { it.downloadOptionQualityHeight() == preferredHeight }
    if (exact != null) return exact

    val closestLower = filter { it.downloadOptionQualityHeight() <= preferredHeight }
        .maxByOrNull { it.downloadOptionQualityHeight() }
    return closestLower ?: bestByQuality
}

internal fun DownloadSourceOption.downloadOptionQualityHeight(): Int {
    val key = qualityKey.lowercase()
    return Regex("(\\d{3,4})p").find(key)?.groupValues?.getOrNull(1)?.toIntOrNull()
        ?: qualityLabel.parseResolutionHeight()
}

private fun String.parseResolutionHeight(): Int {
    val key = lowercase()
    return Regex("(\\d{3,4})p").find(key)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
}

private fun EpisodeDownloadTarget.episodeLogLabel(): String {
    val season = seasonNumber
    val episode = episodeNumber
    return if (season != null && episode != null) {
        "S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}"
    } else {
        title?.trim()?.takeIf { it.isNotBlank() } ?: videoId
    }
}

private fun EpisodeDownloadTarget.cloudStreamSearchRequest(contentType: String) =
    buildCloudStreamSearchRequest(
        type = contentType,
        videoId = videoId,
        parentMetaId = parentMetaId,
        parentMetaType = parentMetaType,
        season = seasonNumber,
        episode = episodeNumber,
        searchTitle = searchTitle,
    )

private fun sourceSearchRetryDelay(attemptCount: Int): Long = when (attemptCount) {
    0 -> 5L * 60L * 1_000L
    1 -> 15L * 60L * 1_000L
    2 -> 30L * 60L * 1_000L
    else -> 60L * 60L * 1_000L
}

private const val SOURCE_PROVIDER_TIMEOUT_MS = 10_000L
private const val CLOUDSTREAM_DOWNLOAD_PROVIDER_TIMEOUT_MS = 15_000L
private const val DOWNLOAD_SOURCE_TOTAL_TIMEOUT_MS = 25_000L
private const val MANIFEST_LOAD_WAIT_MS = 8_000L
private const val MAX_CONCURRENT_PROVIDERS = 4
private const val MAX_CONCURRENT_EPISODE_SELECTIONS = 3
private const val MAX_SOURCE_SEARCH_ATTEMPTS = 10
