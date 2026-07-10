package com.nuvio.app.features.cloudstream

object CloudStreamCompatibilityResolver {
    private val crossPlatformAdapters = mapOf(
        "https://raw.githubusercontent.com/Kraptor123/cs-kraptor/master/repo.json#KickTR" to "kick-tr-v1",
    )

    fun resolve(metadata: CloudStreamPluginMetadata): CloudStreamCompatibility {
        val adapterId = crossPlatformAdapters[metadata.id.value]
        return if (adapterId != null) {
            CloudStreamCompatibility(
                runtimeKind = CloudStreamRuntimeKind.PrecompiledCrossPlatformAdapter,
                platformSupport = CloudStreamPlatformSupport.AndroidAndIos,
                adapterId = adapterId,
                reason = "This provider has a reviewed cross-platform adapter compiled into Nuvio Enhanced.",
            )
        } else {
            CloudStreamCompatibility(
                runtimeKind = CloudStreamRuntimeKind.UnsupportedAndroidDex,
                platformSupport = CloudStreamPlatformSupport.Unsupported,
                reason = "Standard .cs3 packages contain Android DEX code. Nuvio does not execute downloaded DEX on Android and iOS cannot execute it; a reviewed cross-platform adapter is required.",
            )
        }
    }
}

fun sortCloudStreamEpisodes(episodes: List<CloudStreamEpisode>): List<CloudStreamEpisode> =
    episodes.sortedWith(
        compareBy<CloudStreamEpisode>(
            { it.season ?: Int.MAX_VALUE },
            { it.episode ?: Int.MAX_VALUE },
            { it.name.lowercase() },
        ),
    )

