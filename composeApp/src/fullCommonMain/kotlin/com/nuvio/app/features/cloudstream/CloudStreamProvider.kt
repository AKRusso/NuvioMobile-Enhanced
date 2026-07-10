package com.nuvio.app.features.cloudstream

internal interface CloudStreamProvider {
    val id: String
    suspend fun getMainPage(page: Int): List<Pair<String, List<CloudStreamSearchItem>>>
    suspend fun search(query: String): List<CloudStreamSearchItem>
    suspend fun load(data: String): CloudStreamLoadItem
    suspend fun loadLinks(data: String): List<CloudStreamPlaybackSource>
}

internal object CloudStreamProviderRegistry {
    private val providers: List<CloudStreamProvider> = listOf(
        KickTrCloudStreamProvider,
    )

    fun find(providerId: String): CloudStreamProvider? =
        providers.firstOrNull { it.id == providerId }
}

