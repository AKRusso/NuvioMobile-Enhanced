package com.nuvio.app.features.cloudstream

import kotlinx.coroutines.flow.StateFlow

data class CloudStreamRepositoryItem(
    val manifest: CloudStreamRepositoryManifest,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

data class CloudStreamPluginItem(
    val metadata: CloudStreamPluginMetadata,
    val compatibility: CloudStreamCompatibility,
    val installedVersion: Int? = null,
    val installedAtEpochMs: Long = 0L,
    val enabled: Boolean = false,
    val verified: Boolean = false,
    val isInstalling: Boolean = false,
    val errorMessage: String? = null,
) {
    val isInstalled: Boolean
        get() = installedVersion != null

    val hasUpdate: Boolean
        get() = installedVersion != null && metadata.version > installedVersion

    val isRunnable: Boolean
        get() = isInstalled && enabled && verified && compatibility.isRunnable
}

data class CloudStreamUiState(
    val repositories: List<CloudStreamRepositoryItem> = emptyList(),
    val plugins: List<CloudStreamPluginItem> = emptyList(),
    val registryRevision: Long = 0L,
    val securityWarningAccepted: Boolean = false,
)

sealed interface AddCloudStreamRepositoryResult {
    data class Success(val repository: CloudStreamRepositoryManifest) : AddCloudStreamRepositoryResult
    data class Error(val message: String) : AddCloudStreamRepositoryResult
}

sealed interface CloudStreamInstallResult {
    data class Success(val plugin: CloudStreamPluginItem) : CloudStreamInstallResult
    data class Error(val message: String) : CloudStreamInstallResult
}

expect object CloudStreamRepository {
    val uiState: StateFlow<CloudStreamUiState>

    fun initialize()
    fun onProfileChanged(profileId: Int)
    fun clearLocalState()
    fun acceptSecurityWarning()

    suspend fun addRepository(rawUrl: String): AddCloudStreamRepositoryResult
    fun refreshRepository(manifestUrl: String)
    fun refreshAll()
    fun removeRepository(manifestUrl: String)

    suspend fun installPlugin(pluginId: String): CloudStreamInstallResult
    suspend fun updatePlugin(pluginId: String): CloudStreamInstallResult
    fun setPluginEnabled(pluginId: String, enabled: Boolean)
    fun removePlugin(pluginId: String)

    suspend fun getMainPage(providerId: String, page: Int = 1): Result<List<Pair<String, List<CloudStreamSearchItem>>>>
    suspend fun search(query: String, providerId: String? = null): List<Result<List<CloudStreamSearchItem>>>
    suspend fun load(providerId: String, data: String): Result<CloudStreamLoadItem>
    suspend fun loadLinks(providerId: String, data: String): Result<List<CloudStreamPlaybackSource>>
}

internal expect object CloudStreamPlatformStorage {
    fun initialize(context: Any?)
    fun loadState(profileId: Int): String?
    fun saveState(profileId: Int, payload: String)
    fun savePackageAtomically(storageKey: String, bytes: ByteArray)
    fun packageExists(storageKey: String): Boolean
    fun deletePackage(storageKey: String)
    fun clearPackages()
    fun clearAllState()
}
