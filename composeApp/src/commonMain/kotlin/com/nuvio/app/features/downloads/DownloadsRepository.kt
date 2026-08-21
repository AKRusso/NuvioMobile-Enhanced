package com.nuvio.app.features.downloads

import co.touchlab.kermit.Logger
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.streams.StreamItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object DownloadsRepository {
    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    private val stateLock = SynchronizedObject()
    private val activeHandles = mutableMapOf<String, DownloadsTaskHandle>()
    private var hasLoaded = false
    private var nextDownloadOrdinal = 0L
    private var lastProgressPersistAtEpochMs = 0L

    fun ensureLoaded() {
        val shouldLoad = synchronized(stateLock) {
            if (hasLoaded) false else {
                hasLoaded = true
                true
            }
        }
        if (shouldLoad) loadFromDisk()
    }

    fun onProfileChanged() {
        synchronized(stateLock) { hasLoaded = true }
        loadFromDisk()
    }

    fun removeMissingCompletedDownloads() {
        ensureLoaded()
        val remaining = _uiState.value.items.filter { item ->
            item.status != DownloadStatus.Completed ||
                DownloadsPlatformDownloader.resolveLocalFileUri(
                    localFileUri = item.localFileUri,
                    destinationFileName = item.fileName,
                ) != null
        }
        if (remaining.size == _uiState.value.items.size) return
        publish(remaining)
        persist()
    }

    fun clearLocalState() {
        val handles = synchronized(stateLock) {
            activeHandles.values.toList().also { activeHandles.clear() }
        }
        handles.forEach(DownloadsTaskHandle::cancel)
        synchronized(stateLock) {
            hasLoaded = false
            _uiState.value = DownloadsUiState()
        }
        notifyLiveStatusPlatform()
    }

    fun findPlayableDownloadByVideoId(videoId: String?, parentMetaId: String? = null): DownloadItem? {
        ensureLoaded()
        val normalizedVideoId = videoId?.trim().orEmpty()
        if (normalizedVideoId.isBlank()) return null
        val matching = _uiState.value.items.filter { item ->
            item.videoId == normalizedVideoId && item.hasPlayableLocalFile()
        }
        if (matching.isEmpty()) return null
        // Prefer the download that belongs to the same series/movie when known, so a
        // videoId collision across shows can never resolve to the wrong episode.
        if (parentMetaId != null) {
            matching.firstOrNull { it.parentMetaId.trim() == parentMetaId.trim() }?.let { return it }
        }
        return matching.first()
    }

    fun findPlayableDownload(
        parentMetaId: String,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        videoId: String? = null,
    ): DownloadItem? {
        ensureLoaded()
        val items = _uiState.value.items
        val normalizedParentMetaId = parentMetaId.trim()
        val log = Logger.withTag("Downloads")

        findPlayableDownloadByVideoId(videoId, parentMetaId = parentMetaId)?.let { matched ->
            log.d {
                "episode-index: videoId match id=${matched.id} videoId=${matched.videoId} " +
                    "S${matched.seasonNumber ?: "?"}E${matched.episodeNumber ?: "?"} parent=${matched.parentMetaId}"
            }
            return matched
        }

        val fallback = if (seasonNumber != null && episodeNumber != null) {
            items.firstOrNull { item ->
                item.parentMetaId == normalizedParentMetaId &&
                    item.seasonNumber == seasonNumber &&
                    item.episodeNumber == episodeNumber &&
                    item.hasPlayableLocalFile()
            }
        } else {
            items.firstOrNull { item ->
                item.parentMetaId == normalizedParentMetaId &&
                    item.seasonNumber == null &&
                    item.episodeNumber == null &&
                    item.hasPlayableLocalFile()
            }
        }
        if (fallback != null) {
            log.d {
                "episode-index: S/E match id=${fallback.id} " +
                    "S${fallback.seasonNumber ?: "?"}E${fallback.episodeNumber ?: "?"} " +
                    "lookup S${seasonNumber ?: "?"}E${episodeNumber ?: "?"}"
            }
        }
        return fallback
    }

    fun playableLocalFileUri(item: DownloadItem): String? {
        ensureLoaded()
        if (item.status != DownloadStatus.Completed) return null
        val resolvedUri = DownloadsPlatformDownloader.resolveLocalFileUri(
            localFileUri = item.localFileUri,
            destinationFileName = item.fileName,
        ) ?: return null

        if (resolvedUri != item.localFileUri) {
            mutateItem(item.id) { current ->
                if (current.fileName == item.fileName) {
                    current.copy(
                        localFileUri = resolvedUri,
                        updatedAtEpochMs = DownloadsClock.nowEpochMs(),
                    )
                } else {
                    current
                }
            }
        }

        return resolvedUri
    }

    fun enqueueFromStream(
        contentType: String,
        videoId: String,
        parentMetaId: String,
        parentMetaType: String,
        title: String,
        logo: String?,
        poster: String?,
        background: String?,
        seasonNumber: Int?,
        episodeNumber: Int?,
        episodeTitle: String?,
        episodeThumbnail: String?,
        episodeOverview: String? = null,
        stream: StreamItem,
        queueMode: DownloadQueueMode = DownloadQueueMode.AllAtOnce,
    ): DownloadEnqueueResult {
        ensureLoaded()
        val log = Logger.withTag("Downloads")

        val sourceUrl = stream.playableDirectUrl
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return DownloadEnqueueResult.MissingUrl

        if (!sourceUrl.isSupportedDownloadUrl()) {
            return DownloadEnqueueResult.UnsupportedFormat
        }

        val now = DownloadsClock.nowEpochMs()
        val logicalKey = buildLogicalKey(
            parentMetaId = parentMetaId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
        )

        val downloadId = nextDownloadId(now)
        val fileName = buildFileName(
            title = title,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            episodeTitle = episodeTitle,
            fallbackTitle = stream.streamLabel,
            sourceUrl = sourceUrl,
            nowEpochMs = now,
        )
        val detailsSnapshot = snapshotCurrentDetails(
            contentType = contentType,
            parentMetaId = parentMetaId,
            parentMetaType = parentMetaType,
            videoId = videoId,
        )

        val item = DownloadItem(
            id = downloadId,
            contentType = contentType,
            parentMetaId = parentMetaId,
            parentMetaType = parentMetaType,
            videoId = videoId,
            title = title,
            logo = logo,
            poster = poster,
            background = background,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            episodeTitle = episodeTitle,
            episodeThumbnail = episodeThumbnail,
            episodeOverview = episodeOverview,
            detailsSnapshot = detailsSnapshot,
            streamTitle = stream.streamLabel,
            streamSubtitle = stream.streamSubtitle,
            providerName = stream.addonName,
            providerAddonId = stream.addonId,
            externalSubtitles = stream.externalSubtitles,
            sourceUrl = sourceUrl,
            sourceHeaders = sanitizeRequestHeaders(stream.behaviorHints.proxyHeaders?.request),
            sourceResponseHeaders = sanitizeResponseHeaders(stream.behaviorHints.proxyHeaders?.response),
            localFileUri = null,
            fileName = fileName,
            status = DownloadStatus.Waiting,
            queueMode = queueMode,
            downloadedBytes = 0L,
            totalBytes = null,
            errorMessage = null,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
        )

        var replacedExisting = false
        var replacedItem: DownloadItem? = null
        var shouldStartNow = false
        val queuedItem = synchronized(stateLock) {
            val currentItems = _uiState.value.items.toMutableList()
            val existing = currentItems.firstOrNull { it.logicalContentKey == logicalKey }
            if (existing != null) {
                replacedExisting = true
                replacedItem = existing
                currentItems.removeAll { it.id == existing.id }
            }
            // Admission and publication must use the same state snapshot. Otherwise two
            // series started together can overwrite each other's progress/item list.
            shouldStartNow = queueMode == DownloadQueueMode.AllAtOnce ||
                currentItems.none { it.status == DownloadStatus.Downloading }
            val admitted = item.copy(
                status = if (shouldStartNow) DownloadStatus.Downloading else DownloadStatus.Waiting,
            )
            currentItems.add(0, admitted)
            _uiState.value = _uiState.value.copy(items = currentItems)
            admitted
        }
        notifyLiveStatusPlatform()
        replacedItem?.let { existing ->
            removeActiveHandle(existing.id)?.cancel()
            DownloadsPlatformDownloader.removeFile(playableLocalFileUri(existing) ?: existing.localFileUri)
            DownloadsPlatformDownloader.removePartialFile(existing.fileName)
            log.d {
                "enqueue: replacing existing download id=${existing.id} " +
                    "videoId=${existing.videoId} S${existing.seasonNumber ?: "?"}E${existing.episodeNumber ?: "?"}"
            }
        }
        persist()
        if (shouldStartNow) {
            startDownload(queuedItem)
        }
        log.d {
            "enqueue: id=${queuedItem.id} videoId=${queuedItem.videoId} S${queuedItem.seasonNumber ?: "?"}E${queuedItem.episodeNumber ?: "?"} " +
                "mode=$queueMode status=${queuedItem.status} " +
                (if (replacedExisting) "(replaced)" else "(new)")
        }

        return if (replacedExisting) {
            DownloadEnqueueResult.Replaced
        } else {
            DownloadEnqueueResult.Started
        }
    }

    fun queuePendingSourceSearch(
        contentType: String,
        videoId: String,
        parentMetaId: String,
        parentMetaType: String,
        title: String,
        logo: String?,
        poster: String?,
        background: String?,
        seasonNumber: Int?,
        episodeNumber: Int?,
        episodeTitle: String?,
        episodeThumbnail: String?,
        episodeOverview: String?,
        providerName: String,
        providerAddonId: String,
        providerManifestUrl: String?,
        qualityKey: String,
    ): PendingEpisodeDownload {
        ensureLoaded()
        val now = DownloadsClock.nowEpochMs()
        val logicalKey = downloadLogicalContentKey(parentMetaId, seasonNumber, episodeNumber)
        val pending = PendingEpisodeDownload(
            id = "source_${nextDownloadId(now)}",
            contentType = contentType,
            videoId = videoId,
            parentMetaId = parentMetaId,
            parentMetaType = parentMetaType,
            title = title,
            logo = logo,
            poster = poster,
            background = background,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            episodeTitle = episodeTitle,
            episodeThumbnail = episodeThumbnail,
            episodeOverview = episodeOverview,
            providerName = providerName,
            providerAddonId = providerAddonId,
            providerManifestUrl = providerManifestUrl,
            qualityKey = qualityKey,
            createdAtEpochMs = now,
            nextAttemptAtEpochMs = now + SOURCE_SEARCH_RETRY_DELAYS_MS.first(),
            expiresAtEpochMs = now + SOURCE_SEARCH_MAX_AGE_MS,
        )
        val existing = _uiState.value.pendingSourceSearches
            .filterNot { it.logicalContentKey == logicalKey }
        publish(
            items = _uiState.value.items,
            pendingSourceSearches = listOf(pending) + existing,
        )
        persist()
        PendingDownloadSourceSearchPlatform.schedule(pending)
        return pending
    }

    fun pendingSourceSearch(searchId: String): PendingEpisodeDownload? {
        ensureLoaded()
        return _uiState.value.pendingSourceSearches.firstOrNull { it.id == searchId }
    }

    fun pendingSourceSearchesForShow(parentMetaId: String): List<PendingEpisodeDownload> {
        ensureLoaded()
        return _uiState.value.pendingSourceSearches.filter { it.parentMetaId == parentMetaId }
    }

    fun updatePendingSourceSearch(search: PendingEpisodeDownload) {
        ensureLoaded()
        val updated = _uiState.value.pendingSourceSearches.map { current ->
            if (current.id == search.id) search else current
        }
        publish(items = _uiState.value.items, pendingSourceSearches = updated)
        persist()
        if (search.status == PendingSourceSearchStatus.Searching) {
            PendingDownloadSourceSearchPlatform.schedule(search)
        } else {
            PendingDownloadSourceSearchPlatform.cancel(search.id)
        }
    }

    fun removePendingSourceSearch(searchId: String) {
        ensureLoaded()
        val updated = _uiState.value.pendingSourceSearches.filterNot { it.id == searchId }
        if (updated.size == _uiState.value.pendingSourceSearches.size) return
        publish(items = _uiState.value.items, pendingSourceSearches = updated)
        persist()
        PendingDownloadSourceSearchPlatform.cancel(searchId)
    }

    fun removePendingSourceSearchForContent(logicalContentKey: String) {
        ensureLoaded()
        val searchId = _uiState.value.pendingSourceSearches
            .firstOrNull { it.logicalContentKey == logicalContentKey }
            ?.id
            ?: return
        removePendingSourceSearch(searchId)
    }

    fun markPendingSourceSearchPrompted(searchIds: Collection<String>) {
        ensureLoaded()
        val ids = searchIds.toSet()
        if (ids.isEmpty()) return
        val updated = _uiState.value.pendingSourceSearches.map { search ->
            if (search.id in ids) search.copy(manualChoicePrompted = true) else search
        }
        publish(items = _uiState.value.items, pendingSourceSearches = updated)
        persist()
    }

    fun pauseDownload(downloadId: String) {
        ensureLoaded()
        val log = Logger.withTag("Downloads")
        val item = _uiState.value.items.firstOrNull { it.id == downloadId } ?: return
        if (item.status != DownloadStatus.Downloading) return

        removeActiveHandle(downloadId)?.cancel()
        mutateItem(downloadId) { current ->
            current.copy(
                status = DownloadStatus.Paused,
                downloadSpeedBytesPerSecond = 0L,
                updatedAtEpochMs = DownloadsClock.nowEpochMs(),
                errorMessage = null,
            )
        }
        log.d {
            "pause: id=$downloadId videoId=${item.videoId} S${item.seasonNumber ?: "?"}E${item.episodeNumber ?: "?"} " +
                "bytes=${item.downloadedBytes} total=${item.totalBytes}"
        }
        promoteWaitingDownload()
    }

    fun pauseActiveDownloads() {
        ensureLoaded()
        _uiState.value.items
            .filter { it.status == DownloadStatus.Downloading }
            .map { it.id }
            .forEach(::pauseDownload)
    }

    fun resumeDownload(downloadId: String) {
        ensureLoaded()
        val log = Logger.withTag("Downloads")
        val item = _uiState.value.items.firstOrNull { it.id == downloadId } ?: return
        if (item.status != DownloadStatus.Paused && item.status != DownloadStatus.Failed) return

        // In one-at-a-time mode the resumed item must never run concurrently with an
        // already-active episode: if something else is downloading, go back to Waiting
        // and let the queue promote us when the active download finishes.
        val anyActive = _uiState.value.items.any {
            it.id != downloadId && it.status == DownloadStatus.Downloading
        }
        val resumeImmediately = item.queueMode != DownloadQueueMode.OneAtATime || !anyActive

        val reset = item.copy(
            status = if (resumeImmediately) DownloadStatus.Downloading else DownloadStatus.Waiting,
            errorMessage = null,
            localFileUri = null,
            downloadSpeedBytesPerSecond = 0L,
            updatedAtEpochMs = DownloadsClock.nowEpochMs(),
        )

        replaceItem(reset)
        persist()
        if (resumeImmediately) {
            startDownload(reset)
        }
        log.d {
            "resume: id=$downloadId videoId=${item.videoId} S${item.seasonNumber ?: "?"}E${item.episodeNumber ?: "?"} " +
                "mode=${item.queueMode} status=${reset.status} anyActive=$anyActive"
        }
    }

    fun retryDownload(downloadId: String) {
        resumeDownload(downloadId)
    }

    fun cancelDownload(downloadId: String) {
        ensureLoaded()
        val item = _uiState.value.items.firstOrNull { it.id == downloadId } ?: return

        removeActiveHandle(downloadId)?.cancel()
        DownloadsPlatformDownloader.removeFile(playableLocalFileUri(item) ?: item.localFileUri)
        DownloadsPlatformDownloader.removePartialFile(item.fileName)
        item.externalSubtitles.forEach { subtitle ->
            val subtitleUri = subtitle.url.takeIf {
                it.startsWith("file:", ignoreCase = true) ||
                    it.startsWith("content:", ignoreCase = true)
            }
                ?: return@forEach
            DownloadsPlatformDownloader.removeFile(subtitleUri)
        }

        synchronized(stateLock) {
            _uiState.update { state ->
                state.copy(items = state.items.filterNot { it.id == downloadId })
            }
        }
        notifyLiveStatusPlatform()
        persist()
        Logger.withTag("Downloads").d {
            "cancel: id=$downloadId videoId=${item.videoId} S${item.seasonNumber ?: "?"}E${item.episodeNumber ?: "?"}"
        }
        promoteWaitingDownload()
    }

    fun findOfflineMetaDetails(type: String, id: String): MetaDetails? {
        ensureLoaded()
        val normalizedType = type.trim().lowercase()
        val normalizedId = id.trim()
        if (normalizedId.isBlank()) return null

        return _uiState.value.items
            .asSequence()
            .filter { item ->
                item.parentMetaId.trim() == normalizedId ||
                    item.videoId.trim() == normalizedId ||
                    item.detailsSnapshot?.id?.trim() == normalizedId
            }
            .sortedByDescending { it.updatedAtEpochMs }
            .firstNotNullOfOrNull { item ->
                val offline = item.toOfflineMetaDetails() ?: return@firstNotNullOfOrNull null
                val typeMatches = normalizedType.isBlank() ||
                    offline.type.equals(type, ignoreCase = true) ||
                    item.parentMetaType.equals(type, ignoreCase = true) ||
                    item.contentType.equals(type, ignoreCase = true)
                if (typeMatches) offline else null
            }
    }

    private fun loadFromDisk() {
        val payload = DownloadsStorage.loadPayload().orEmpty().trim()
        if (payload.isEmpty()) {
            _uiState.value = DownloadsUiState()
            notifyLiveStatusPlatform()
            return
        }

        var shouldPersistNormalized = false
        val decoded = DownloadsCodec.decode(payload)
        val decodedItems = decoded.items
        val now = DownloadsClock.nowEpochMs()
        val normalized = decodedItems
            .map { item ->
                val statusNormalized = if (item.status == DownloadStatus.Downloading) {
                    item.copy(
                        status = DownloadStatus.Paused,
                        downloadSpeedBytesPerSecond = 0L,
                        errorMessage = null,
                    )
                } else {
                    item
                }

                val localUriNormalized = normalizeCompletedLocalFileUri(statusNormalized)
                if (localUriNormalized != item) {
                    shouldPersistNormalized = true
                }
                localUriNormalized
            }
            .filter { item ->
                item.status != DownloadStatus.Completed ||
                    DownloadsPlatformDownloader.resolveLocalFileUri(
                        localFileUri = item.localFileUri,
                        destinationFileName = item.fileName,
                    ) != null
            }
        if (normalized.size != decodedItems.size) {
            shouldPersistNormalized = true
        }
        val activePendingSearches = decoded.pendingSourceSearches.filter { search ->
            search.status == PendingSourceSearchStatus.Searching &&
                now < search.expiresAtEpochMs &&
                search.attemptCount < MAX_SOURCE_SEARCH_ATTEMPTS
        }
        if (activePendingSearches.size != decoded.pendingSourceSearches.size) {
            shouldPersistNormalized = true
        }

        synchronized(stateLock) {
            _uiState.value = DownloadsUiState(
                items = normalized,
                pendingSourceSearches = activePendingSearches,
            )
        }
        notifyLiveStatusPlatform()
        if (normalized.none { it.status == DownloadStatus.Downloading }) {
            promoteWaitingDownload()
        }
        if (shouldPersistNormalized) {
            persist()
        }
    }

    private fun startDownload(item: DownloadItem) {
        val log = Logger.withTag("Downloads")
        val request = DownloadPlatformRequest(
            sourceUrl = item.sourceUrl,
            sourceHeaders = item.sourceHeaders,
            destinationFileName = item.fileName,
        )

        val handle = DownloadsPlatformDownloader.start(
            request = request,
            onProgress = { downloadedBytes, totalBytes ->
                val nextDownloadedBytes = downloadedBytes.coerceAtLeast(0L)
                mutateItem(item.id, persist = shouldPersistProgress()) { current ->
                    if (current.status != DownloadStatus.Downloading) {
                        current
                    } else {
                        val now = DownloadsClock.nowEpochMs()
                        val byteDelta = (nextDownloadedBytes - current.downloadedBytes).coerceAtLeast(0L)
                        val elapsedMs = (now - current.updatedAtEpochMs).coerceAtLeast(0L)
                        val instantSpeed = if (byteDelta > 0L && elapsedMs > 0L) {
                            (byteDelta * 1_000L) / elapsedMs
                        } else {
                            null
                        }
                        val smoothedSpeed = when {
                            instantSpeed == null -> current.downloadSpeedBytesPerSecond
                            current.downloadSpeedBytesPerSecond > 0L ->
                                (
                                    current.downloadSpeedBytesPerSecond.toDouble() * 0.65 +
                                        instantSpeed.toDouble() * 0.35
                                    ).toLong()
                            else -> instantSpeed
                        }.coerceAtLeast(0L)

                        current.copy(
                            downloadedBytes = nextDownloadedBytes,
                            totalBytes = totalBytes?.takeIf { it > 0L },
                            downloadSpeedBytesPerSecond = smoothedSpeed,
                            updatedAtEpochMs = now,
                            errorMessage = null,
                        )
                    }
                }
                log.d {
                    "progress: id=${item.id} videoId=${item.videoId} " +
                        "S${item.seasonNumber ?: "?"}E${item.episodeNumber ?: "?"} " +
                        "$nextDownloadedBytes/${totalBytes ?: "?"} bytes"
                }
            },
            onSuccess = { localFileUri, totalBytes ->
                removeActiveHandle(item.id)
                mutateItem(item.id) { current ->
                    val cachedSubtitles = DownloadsPlatformDownloader.cacheSubtitleFiles(
                        subtitles = current.externalSubtitles,
                        companionBaseFileName = current.fileName,
                    )
                    current.copy(
                        status = DownloadStatus.Completed,
                        localFileUri = localFileUri,
                        externalSubtitles = cachedSubtitles.ifEmpty { current.externalSubtitles },
                        downloadedBytes = if (totalBytes != null && totalBytes > 0L) {
                            totalBytes
                        } else {
                            current.downloadedBytes
                        },
                        totalBytes = totalBytes?.takeIf { it > 0L } ?: current.totalBytes,
                        downloadSpeedBytesPerSecond = 0L,
                        errorMessage = null,
                        updatedAtEpochMs = DownloadsClock.nowEpochMs(),
                    )
                }
                log.d {
                    "completed: id=${item.id} videoId=${item.videoId} " +
                        "S${item.seasonNumber ?: "?"}E${item.episodeNumber ?: "?"} " +
                        "parent=${item.parentMetaId} file=${item.fileName} uri=$localFileUri"
                }
                promoteWaitingDownload()
            },
            onFailure = { message ->
                removeActiveHandle(item.id)
                mutateItem(item.id) { current ->
                    if (current.status != DownloadStatus.Downloading) {
                        current
                    } else {
                        current.copy(
                            status = DownloadStatus.Failed,
                            downloadSpeedBytesPerSecond = 0L,
                            errorMessage = message.ifBlank { "Download failed" },
                            updatedAtEpochMs = DownloadsClock.nowEpochMs(),
                        )
                    }
                }
                log.w {
                    "failed: id=${item.id} videoId=${item.videoId} " +
                        "S${item.seasonNumber ?: "?"}E${item.episodeNumber ?: "?"} error=$message"
                }
                promoteWaitingDownload()
            },
        )

        registerActiveHandle(item.id, handle)
    }

    /**
     * In one-at-a-time mode, starts the oldest queued episode download once no other
     * download is active. Newest items are stored at the front of the list, so the
     * oldest queued item is the last Waiting entry.
     */
    private fun promoteWaitingDownload() {
        val promoted = synchronized(stateLock) {
            val state = _uiState.value
            if (state.items.any { it.status == DownloadStatus.Downloading }) return@synchronized null
            val next = state.items.asReversed().firstOrNull { it.status == DownloadStatus.Waiting }
                ?: return@synchronized null
            val candidate = next.copy(
                status = DownloadStatus.Downloading,
                updatedAtEpochMs = DownloadsClock.nowEpochMs(),
                errorMessage = null,
            )
            _uiState.value = state.copy(
                items = state.items.map { item -> if (item.id == candidate.id) candidate else item },
            )
            candidate
        } ?: return
        notifyLiveStatusPlatform()
        persist()
        Logger.withTag("Downloads").d {
            "queue: promote id=${promoted.id} videoId=${promoted.videoId} " +
                "S${promoted.seasonNumber ?: "?"}E${promoted.episodeNumber ?: "?"} -> downloading"
        }
        startDownload(promoted)
    }

    private fun mutateItem(
        downloadId: String,
        persist: Boolean = true,
        transform: (DownloadItem) -> DownloadItem,
    ) {
        var changed = false
        synchronized(stateLock) {
            _uiState.update { state ->
                val items = state.items.map { item ->
                    if (item.id == downloadId) {
                        changed = true
                        transform(item)
                    } else {
                        item
                    }
                }
                if (changed) state.copy(items = items) else state
            }
        }

        if (changed) {
            notifyLiveStatusPlatform()
            if (persist) persist()
        }
    }

    private fun replaceItem(item: DownloadItem) {
        synchronized(stateLock) {
            _uiState.update { state ->
                state.copy(
                    items = state.items.map { existing ->
                        if (existing.id == item.id) item else existing
                    },
                )
            }
        }
        notifyLiveStatusPlatform()
    }

    private fun publish(
        items: List<DownloadItem>,
        pendingSourceSearches: List<PendingEpisodeDownload> = _uiState.value.pendingSourceSearches,
    ) {
        synchronized(stateLock) {
            _uiState.value = DownloadsUiState(
                items = items,
                pendingSourceSearches = pendingSourceSearches,
            )
        }
        notifyLiveStatusPlatform()
    }

    private fun notifyLiveStatusPlatform() {
        runCatching {
            DownloadsLiveStatusPlatform.onItemsChanged(_uiState.value.items)
        }
        val pendingToPrompt = _uiState.value.pendingSourceSearches.filter { search ->
            search.status == PendingSourceSearchStatus.Searching && !search.manualChoicePrompted
        }
        if (_uiState.value.items.none { it.status == DownloadStatus.Downloading } && pendingToPrompt.isNotEmpty()) {
            PendingDownloadSourceSearchPlatform.notifyManualChoice(pendingToPrompt)
        }
    }

    private fun snapshotCurrentDetails(
        contentType: String,
        parentMetaId: String,
        parentMetaType: String,
        videoId: String,
    ): DownloadDetailsSnapshot? {
        val candidates = listOf(
            parentMetaType to parentMetaId,
            contentType to parentMetaId,
            parentMetaType to videoId,
            contentType to videoId,
        )

        return candidates
            .asSequence()
            .mapNotNull { (type, id) ->
                val normalizedType = type.trim()
                val normalizedId = id.trim()
                if (normalizedType.isBlank() || normalizedId.isBlank()) {
                    null
                } else {
                    MetaDetailsRepository.peek(normalizedType, normalizedId)
                }
            }
            .firstOrNull()
            ?.toDownloadDetailsSnapshot()
    }

    private fun persist() {
        val state = synchronized(stateLock) { _uiState.value }
        DownloadsStorage.savePayload(
            DownloadsCodec.encode(
                items = state.items,
                pendingSourceSearches = state.pendingSourceSearches,
            ),
        )
    }

    private fun nextDownloadId(nowEpochMs: Long): String {
        val ordinal = synchronized(stateLock) {
            nextDownloadOrdinal += 1L
            nextDownloadOrdinal
        }
        return buildString {
            append(nowEpochMs.toString(36))
            append('_')
            append(ordinal.toString(36))
        }
    }

    private fun removeActiveHandle(downloadId: String): DownloadsTaskHandle? =
        synchronized(stateLock) { activeHandles.remove(downloadId) }

    private fun registerActiveHandle(downloadId: String, handle: DownloadsTaskHandle) {
        synchronized(stateLock) { activeHandles[downloadId] = handle }
    }

    private fun shouldPersistProgress(): Boolean = synchronized(stateLock) {
        val now = DownloadsClock.nowEpochMs()
        if (now - lastProgressPersistAtEpochMs < DOWNLOAD_PROGRESS_PERSIST_INTERVAL_MS) {
            false
        } else {
            lastProgressPersistAtEpochMs = now
            true
        }
    }

    private fun normalizeCompletedLocalFileUri(item: DownloadItem): DownloadItem {
        if (item.status != DownloadStatus.Completed) return item
        val resolvedUri = DownloadsPlatformDownloader.resolveLocalFileUri(
            localFileUri = item.localFileUri,
            destinationFileName = item.fileName,
        ) ?: return item
        return if (resolvedUri != item.localFileUri) {
            item.copy(localFileUri = resolvedUri)
        } else {
            item
        }
    }

    private fun DownloadItem.hasPlayableLocalFile(): Boolean =
        status == DownloadStatus.Completed &&
            DownloadsPlatformDownloader.resolveLocalFileUri(
                localFileUri = localFileUri,
                destinationFileName = fileName,
            ) != null
}

@Serializable
private data class StoredDownloadsPayload(
    val items: List<DownloadItem> = emptyList(),
    val pendingSourceSearches: List<PendingEpisodeDownload> = emptyList(),
)

private object DownloadsCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun decode(payload: String): StoredDownloadsPayload =
        runCatching {
            json.decodeFromString<StoredDownloadsPayload>(payload)
        }.getOrDefault(StoredDownloadsPayload())

    fun encode(
        items: Collection<DownloadItem>,
        pendingSourceSearches: Collection<PendingEpisodeDownload>,
    ): String =
        json.encodeToString(
            StoredDownloadsPayload(
                items = items.toList(),
                pendingSourceSearches = pendingSourceSearches.toList(),
            ),
        )
}

private fun sanitizeRequestHeaders(headers: Map<String, String>?): Map<String, String> =
    headers
        .orEmpty()
        .mapNotNull { (key, value) ->
            val normalizedKey = key.trim()
            val normalizedValue = value.trim()
            if (
                normalizedKey.isBlank() ||
                normalizedValue.isBlank() ||
                normalizedKey.equals("Accept-Encoding", ignoreCase = true) ||
                normalizedKey.equals("Range", ignoreCase = true)
            ) {
                null
            } else {
                normalizedKey to normalizedValue
            }
        }
        .toMap()

private fun sanitizeResponseHeaders(headers: Map<String, String>?): Map<String, String> =
    headers
        .orEmpty()
        .mapNotNull { (key, value) ->
            val normalizedKey = key.trim()
            val normalizedValue = value.trim()
            if (normalizedKey.isBlank() || normalizedValue.isBlank()) {
                null
            } else {
                normalizedKey to normalizedValue
            }
        }
        .toMap()

private fun buildLogicalKey(
    parentMetaId: String,
    seasonNumber: Int?,
    episodeNumber: Int?,
): String = downloadLogicalContentKey(parentMetaId, seasonNumber, episodeNumber)

private fun buildFileName(
    title: String,
    seasonNumber: Int?,
    episodeNumber: Int?,
    episodeTitle: String?,
    fallbackTitle: String,
    sourceUrl: String,
    nowEpochMs: Long,
): String {
    val baseTitle = if (seasonNumber != null && episodeNumber != null) {
        buildString {
            append(title)
            append(" S")
            append(seasonNumber.toString().padStart(2, '0'))
            append('E')
            append(episodeNumber.toString().padStart(2, '0'))
            if (!episodeTitle.isNullOrBlank()) {
                append(' ')
                append(episodeTitle)
            }
        }
    } else {
        title.ifBlank { fallbackTitle }
    }

    val extension = sourceUrl.fileExtensionFromUrl()
    return buildString {
        append(baseTitle.sanitizeFileName().ifBlank { "download" }.take(92))
        append('_')
        append(nowEpochMs.toString(36))
        append('.')
        append(extension)
    }
}

private fun String.sanitizeFileName(): String =
    trim().replace(Regex("[^A-Za-z0-9._ -]"), "_")

private fun String.fileExtensionFromUrl(): String {
    val withoutQuery = substringBefore('?').substringBefore('#')
    val suffix = withoutQuery.substringAfterLast('.', missingDelimiterValue = "")
        .lowercase()
        .trim()

    return if (suffix.length in 2..5 && suffix.all { it.isLetterOrDigit() }) {
        suffix
    } else {
        "mp4"
    }
}

internal fun String.isSupportedDownloadUrl(): Boolean {
    val normalized = trim().lowercase()
    if (normalized.startsWith("magnet:")) return false
    if (normalized.endsWith(".m3u8") || normalized.contains(".m3u8?")) return false
    if (normalized.endsWith(".mpd") || normalized.contains(".mpd?")) return false
    if (normalized.endsWith(".torrent") || normalized.contains(".torrent?")) return false
    return normalized.startsWith("http://") || normalized.startsWith("https://")
}

private const val SOURCE_SEARCH_MAX_AGE_MS = 24L * 60L * 60L * 1_000L
private val SOURCE_SEARCH_RETRY_DELAYS_MS = listOf(
    5L * 60L * 1_000L,
    15L * 60L * 1_000L,
    30L * 60L * 1_000L,
    60L * 60L * 1_000L,
)
private const val MAX_SOURCE_SEARCH_ATTEMPTS = 10
private const val DOWNLOAD_PROGRESS_PERSIST_INTERVAL_MS = 2_000L
