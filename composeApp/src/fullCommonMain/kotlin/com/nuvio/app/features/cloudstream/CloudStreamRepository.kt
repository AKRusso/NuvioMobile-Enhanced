package com.nuvio.app.features.cloudstream

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetBytesWithHeaders
import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.plugins.currentEpochMillis
import com.nuvio.app.features.profiles.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

actual object CloudStreamRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val log = Logger.withTag("CloudStreamRepo")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _uiState = MutableStateFlow(CloudStreamUiState())
    actual val uiState: StateFlow<CloudStreamUiState> = _uiState.asStateFlow()

    private var initialized = false
    private var currentProfileId = 1
    private val refreshJobs = mutableMapOf<String, Job>()

    actual fun initialize() {
        val profileId = ProfileRepository.activeProfileId.coerceAtLeast(1)
        if (initialized && currentProfileId == profileId) return
        currentProfileId = profileId
        initialized = true
        _uiState.value = restoreState(profileId)
    }

    actual fun onProfileChanged(profileId: Int) {
        refreshJobs.values.forEach { it.cancel() }
        refreshJobs.clear()
        currentProfileId = profileId.coerceAtLeast(1)
        initialized = false
        _uiState.value = CloudStreamUiState()
    }

    actual fun clearLocalState() {
        refreshJobs.values.forEach { it.cancel() }
        refreshJobs.clear()
        initialized = false
        currentProfileId = 1
        _uiState.value = CloudStreamUiState()
        CloudStreamPlatformStorage.clearPackages()
        CloudStreamPlatformStorage.clearAllState()
    }

    actual fun acceptSecurityWarning() {
        initialize()
        _uiState.update { it.copy(securityWarningAccepted = true) }
        persist()
    }

    actual suspend fun addRepository(rawUrl: String): AddCloudStreamRepositoryResult {
        initialize()
        val manifestUrl = runCatching { normalizeCloudStreamRepositoryUrl(rawUrl) }
            .getOrElse { return AddCloudStreamRepositoryResult.Error(it.message ?: "Invalid repository URL") }
        if (_uiState.value.repositories.any { it.manifest.sourceUrl == manifestUrl }) {
            return AddCloudStreamRepositoryResult.Error("CloudStream repository is already added")
        }

        return runCatching { fetchRepository(manifestUrl) }
            .fold(
                onSuccess = { (repository, plugins) ->
                    _uiState.update { current ->
                        current.copy(
                            repositories = current.repositories + CloudStreamRepositoryItem(repository),
                            plugins = (current.plugins + plugins.map(::newPluginItem))
                                .distinctBy { it.metadata.id.value }
                                .sortedBy { it.metadata.name.lowercase() },
                        )
                    }
                    persist()
                    AddCloudStreamRepositoryResult.Success(repository)
                },
                onFailure = { error ->
                    log.w(error) { "CloudStream repository install failed url=$manifestUrl" }
                    AddCloudStreamRepositoryResult.Error(error.message ?: "Could not load CloudStream repository")
                },
            )
    }

    actual fun refreshRepository(manifestUrl: String) {
        initialize()
        val normalizedUrl = runCatching { normalizeCloudStreamRepositoryUrl(manifestUrl) }.getOrNull() ?: return
        if (refreshJobs[normalizedUrl]?.isActive == true) return
        _uiState.update { current ->
            current.copy(
                repositories = current.repositories.map { item ->
                    if (item.manifest.sourceUrl == normalizedUrl) item.copy(isRefreshing = true, errorMessage = null) else item
                },
            )
        }
        lateinit var job: Job
        job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                runCatching { fetchRepository(normalizedUrl) }
                    .fold(
                        onSuccess = { (repository, plugins) -> applyRepositoryRefresh(repository, plugins) },
                        onFailure = { error ->
                            log.w(error) { "CloudStream repository refresh failed url=$normalizedUrl" }
                            _uiState.update { current ->
                                current.copy(
                                    repositories = current.repositories.map { item ->
                                        if (item.manifest.sourceUrl == normalizedUrl) {
                                            item.copy(isRefreshing = false, errorMessage = error.message ?: "Refresh failed")
                                        } else item
                                    },
                                )
                            }
                        },
                    )
                persist()
            } finally {
                if (refreshJobs[normalizedUrl] === job) refreshJobs.remove(normalizedUrl)
            }
        }
        refreshJobs[normalizedUrl] = job
        job.start()
    }

    actual fun refreshAll() {
        initialize()
        _uiState.value.repositories.forEach { refreshRepository(it.manifest.sourceUrl) }
    }

    actual fun removeRepository(manifestUrl: String) {
        initialize()
        val normalizedUrl = runCatching { normalizeCloudStreamRepositoryUrl(manifestUrl) }.getOrNull() ?: return
        val removedPlugins = _uiState.value.plugins.filter { it.metadata.repositoryManifestUrl == normalizedUrl }
        removedPlugins.forEach { CloudStreamPlatformStorage.deletePackage(it.metadata.id.storageKey) }
        _uiState.update { current ->
            current.copy(
                repositories = current.repositories.filterNot { it.manifest.sourceUrl == normalizedUrl },
                plugins = current.plugins.filterNot { it.metadata.repositoryManifestUrl == normalizedUrl },
                registryRevision = current.registryRevision + 1,
            )
        }
        persist()
    }

    actual suspend fun installPlugin(pluginId: String): CloudStreamInstallResult = installOrUpdate(pluginId)

    actual suspend fun updatePlugin(pluginId: String): CloudStreamInstallResult = installOrUpdate(pluginId)

    actual fun setPluginEnabled(pluginId: String, enabled: Boolean) {
        initialize()
        _uiState.update { current ->
            var changed = false
            val plugins = current.plugins.map { item ->
                if (item.metadata.id.value != pluginId) return@map item
                val nextEnabled = enabled && item.isInstalled && item.verified && item.compatibility.isRunnable &&
                    item.metadata.status.canInstall
                changed = item.enabled != nextEnabled
                item.copy(enabled = nextEnabled, errorMessage = null)
            }
            current.copy(
                plugins = plugins,
                registryRevision = current.registryRevision + if (changed) 1 else 0,
            )
        }
        persist()
    }

    actual fun removePlugin(pluginId: String) {
        initialize()
        val plugin = _uiState.value.plugins.firstOrNull { it.metadata.id.value == pluginId } ?: return
        CloudStreamPlatformStorage.deletePackage(plugin.metadata.id.storageKey)
        _uiState.update { current ->
            current.copy(
                plugins = current.plugins.map { item ->
                    if (item.metadata.id.value == pluginId) {
                        item.copy(
                            installedVersion = null,
                            installedAtEpochMs = 0L,
                            enabled = false,
                            verified = false,
                            errorMessage = null,
                        )
                    } else item
                },
                registryRevision = current.registryRevision + 1,
            )
        }
        persist()
    }

    actual suspend fun getMainPage(
        providerId: String,
        page: Int,
    ): Result<List<Pair<String, List<CloudStreamSearchItem>>>> = providerResult(providerId) {
        getMainPage(page.coerceAtLeast(1))
    }

    actual suspend fun search(
        query: String,
        providerId: String?,
    ): List<Result<List<CloudStreamSearchItem>>> {
        initialize()
        val activeIds = runnableProviderIds().filter { providerId == null || it == providerId }
        return activeIds.map { id -> providerResult(id) { search(query.trim()) } }
    }

    actual suspend fun load(providerId: String, data: String): Result<CloudStreamLoadItem> =
        providerResult(providerId) { load(data) }

    actual suspend fun loadLinks(providerId: String, data: String): Result<List<CloudStreamPlaybackSource>> =
        providerResult(providerId) { loadLinks(data) }

    private suspend fun installOrUpdate(pluginId: String): CloudStreamInstallResult {
        initialize()
        if (!_uiState.value.securityWarningAccepted) {
            return CloudStreamInstallResult.Error("Accept the third-party plugin security warning before installation")
        }
        val item = _uiState.value.plugins.firstOrNull { it.metadata.id.value == pluginId }
            ?: return CloudStreamInstallResult.Error("CloudStream plugin was not found")
        if (!item.metadata.status.canInstall) {
            return CloudStreamInstallResult.Error("This CloudStream plugin is marked as down")
        }
        markInstalling(pluginId, true, null)
        return runCatching {
            val bytes = httpGetBytesWithHeaders(
                item.metadata.packageUrl,
                headers = mapOf("Accept" to "application/zip, application/octet-stream"),
            )
            require(bytes.isNotEmpty()) { "Downloaded .cs3 package is empty" }
            item.metadata.fileSize?.let { expectedSize ->
                require(bytes.size.toLong() == expectedSize) { ".cs3 file size mismatch" }
            }
            CloudStreamPackageInspector.inspect(bytes)
            val expectedHash = item.metadata.fileHash
            val verified = expectedHash?.matches(bytes) == true
            if (expectedHash != null) require(verified) { ".cs3 SHA-256 hash mismatch" }
            CloudStreamPlatformStorage.savePackageAtomically(item.metadata.id.storageKey, bytes)
            val installed = item.copy(
                installedVersion = item.metadata.version,
                installedAtEpochMs = currentEpochMillis(),
                enabled = item.enabled && verified && item.compatibility.isRunnable,
                verified = verified,
                isInstalling = false,
                errorMessage = null,
            )
            _uiState.update { current ->
                current.copy(
                    plugins = current.plugins.map { if (it.metadata.id.value == pluginId) installed else it },
                    registryRevision = current.registryRevision + 1,
                )
            }
            persist()
            installed
        }.fold(
            onSuccess = { CloudStreamInstallResult.Success(it) },
            onFailure = { error ->
                log.w(error) { "CloudStream plugin installation failed id=$pluginId" }
                markInstalling(pluginId, false, error.message ?: "Plugin installation failed")
                CloudStreamInstallResult.Error(error.message ?: "Plugin installation failed")
            },
        )
    }

    private suspend fun fetchRepository(
        manifestUrl: String,
    ): Pair<CloudStreamRepositoryManifest, List<CloudStreamPluginMetadata>> {
        val payload = httpGetText(manifestUrl)
        val manifest = CloudStreamRepositoryParser.parseRepository(manifestUrl, payload)
        val lists = manifest.pluginListUrls.map { pluginListUrl ->
            val listPayload = httpGetText(pluginListUrl)
            CloudStreamRepositoryParser.parsePluginList(manifest.sourceUrl, pluginListUrl, listPayload)
        }
        return manifest to CloudStreamRepositoryParser.mergePluginLists(lists)
    }

    private fun applyRepositoryRefresh(
        repository: CloudStreamRepositoryManifest,
        plugins: List<CloudStreamPluginMetadata>,
    ) {
        _uiState.update { current ->
            val previousById = current.plugins.associateBy { it.metadata.id.value }
            val refreshed = plugins.map { metadata ->
                previousById[metadata.id.value]?.copy(
                    metadata = metadata,
                    compatibility = CloudStreamCompatibilityResolver.resolve(metadata),
                    enabled = previousById[metadata.id.value]?.enabled == true && metadata.status.canInstall,
                    errorMessage = null,
                ) ?: newPluginItem(metadata)
            }
            current.copy(
                repositories = current.repositories.map { item ->
                    if (item.manifest.sourceUrl == repository.sourceUrl) {
                        CloudStreamRepositoryItem(repository)
                    } else item
                },
                plugins = (current.plugins.filterNot { it.metadata.repositoryManifestUrl == repository.sourceUrl } + refreshed)
                    .sortedBy { it.metadata.name.lowercase() },
                registryRevision = current.registryRevision + 1,
            )
        }
    }

    private fun newPluginItem(metadata: CloudStreamPluginMetadata): CloudStreamPluginItem =
        CloudStreamPluginItem(
            metadata = metadata,
            compatibility = CloudStreamCompatibilityResolver.resolve(metadata),
        )

    private fun markInstalling(pluginId: String, installing: Boolean, error: String?) {
        _uiState.update { current ->
            current.copy(
                plugins = current.plugins.map { item ->
                    if (item.metadata.id.value == pluginId) {
                        item.copy(isInstalling = installing, errorMessage = error)
                    } else item
                },
            )
        }
    }

    private fun restoreState(profileId: Int): CloudStreamUiState {
        val payload = CloudStreamPlatformStorage.loadState(profileId) ?: return CloudStreamUiState()
        val stored = runCatching { json.decodeFromString<StoredCloudStreamState>(payload) }
            .onFailure { log.w(it) { "Could not restore CloudStream state" } }
            .getOrNull() ?: return CloudStreamUiState()
        val repositories = stored.repositories.map { repository ->
            CloudStreamRepositoryItem(
                CloudStreamRepositoryManifest(
                    sourceUrl = repository.manifestUrl,
                    name = repository.name,
                    description = repository.description,
                    iconUrl = repository.iconUrl,
                    manifestVersion = repository.manifestVersion,
                    pluginListUrls = repository.pluginListUrls,
                ),
            )
        }
        val plugins = stored.plugins.map { plugin ->
            val metadata = plugin.toMetadata()
            val packageExists = plugin.installedVersion != null &&
                CloudStreamPlatformStorage.packageExists(metadata.id.storageKey)
            val compatibility = CloudStreamCompatibilityResolver.resolve(metadata)
            CloudStreamPluginItem(
                metadata = metadata,
                compatibility = compatibility,
                installedVersion = plugin.installedVersion.takeIf { packageExists },
                installedAtEpochMs = plugin.installedAtEpochMs.takeIf { packageExists } ?: 0L,
                enabled = plugin.enabled && packageExists && plugin.verified && compatibility.isRunnable && metadata.status.canInstall,
                verified = plugin.verified && packageExists,
            )
        }
        return CloudStreamUiState(
            repositories = repositories,
            plugins = plugins,
            registryRevision = 1,
            securityWarningAccepted = stored.securityWarningAccepted,
        )
    }

    private fun persist() {
        val current = _uiState.value
        val stored = StoredCloudStreamState(
            repositories = current.repositories.map { item ->
                StoredCloudStreamRepository(
                    manifestUrl = item.manifest.sourceUrl,
                    name = item.manifest.name,
                    description = item.manifest.description,
                    iconUrl = item.manifest.iconUrl,
                    manifestVersion = item.manifest.manifestVersion,
                    pluginListUrls = item.manifest.pluginListUrls,
                )
            },
            plugins = current.plugins.map { item -> item.toStored() },
            securityWarningAccepted = current.securityWarningAccepted,
        )
        CloudStreamPlatformStorage.saveState(currentProfileId, json.encodeToString(stored))
    }

    private fun CloudStreamPluginItem.toStored(): StoredCloudStreamPlugin = StoredCloudStreamPlugin(
        repositoryManifestUrl = metadata.repositoryManifestUrl,
        packageUrl = metadata.packageUrl,
        status = metadata.status.name,
        availableVersion = metadata.version,
        installedVersion = installedVersion,
        name = metadata.name,
        internalName = metadata.internalName,
        authors = metadata.authors,
        description = metadata.description,
        fileSize = metadata.fileSize,
        projectUrl = metadata.projectUrl,
        language = metadata.language,
        rawTvTypes = metadata.rawTvTypes,
        iconUrl = metadata.iconUrl,
        apiVersion = metadata.apiVersion,
        fileHash = metadata.fileHash?.wireValue,
        enabled = enabled,
        verified = verified,
        installedAtEpochMs = installedAtEpochMs.takeIf { installedVersion != null } ?: 0L,
    )

    private fun StoredCloudStreamPlugin.toMetadata(): CloudStreamPluginMetadata = CloudStreamPluginMetadata(
        id = CloudStreamPluginId(repositoryManifestUrl, internalName),
        repositoryManifestUrl = repositoryManifestUrl,
        packageUrl = packageUrl,
        status = runCatching { CloudStreamPluginStatus.valueOf(status) }.getOrDefault(CloudStreamPluginStatus.Unknown),
        version = availableVersion,
        name = name,
        internalName = internalName,
        authors = authors,
        description = description,
        fileSize = fileSize,
        projectUrl = projectUrl,
        language = language,
        tvTypes = rawTvTypes.map(CloudStreamTvType::fromWireValue).distinct(),
        rawTvTypes = rawTvTypes,
        iconUrl = iconUrl,
        apiVersion = apiVersion,
        fileHash = CloudStreamFileHash.parse(fileHash),
    )

    private fun runnableProviderIds(): List<String> =
        _uiState.value.plugins.filter(CloudStreamPluginItem::isRunnable).map { it.metadata.id.value }

    private suspend fun <T> providerResult(
        providerId: String,
        block: suspend CloudStreamProvider.() -> T,
    ): Result<T> {
        initialize()
        if (providerId !in runnableProviderIds()) {
            return Result.failure(IllegalStateException("CloudStream provider is disabled or incompatible"))
        }
        val provider = CloudStreamProviderRegistry.find(providerId)
            ?: return Result.failure(IllegalStateException("CloudStream provider adapter was not found"))
        return runCatching { provider.block() }
            .onFailure { log.w(it) { "CloudStream provider request failed id=$providerId" } }
    }
}
