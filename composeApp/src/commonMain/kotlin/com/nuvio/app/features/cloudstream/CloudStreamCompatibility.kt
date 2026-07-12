package com.nuvio.app.features.cloudstream

object CloudStreamCompatibilityResolver {
    private val crossPlatformAdapters = mapOf(
        "https://raw.githubusercontent.com/Kraptor123/cs-kraptor/master/repo.json#KickTR" to "kick-tr-v1",
    )

    fun resolve(
        metadata: CloudStreamPluginMetadata,
        supportsAndroidDex: Boolean = false,
    ): CloudStreamCompatibility {
        val adapterId = crossPlatformAdapters[metadata.id.value]
        return when {
            adapterId != null -> {
            CloudStreamCompatibility(
                runtimeKind = CloudStreamRuntimeKind.PrecompiledCrossPlatformAdapter,
                platformSupport = CloudStreamPlatformSupport.AndroidAndIos,
                adapterId = adapterId,
                reason = "This provider has a reviewed cross-platform adapter compiled into Nuvio Enhanced.",
            )
            }
            supportsAndroidDex -> {
                CloudStreamCompatibility(
                    runtimeKind = CloudStreamRuntimeKind.AndroidDex,
                    platformSupport = CloudStreamPlatformSupport.AndroidOnly,
                    reason = "Android full builds execute this standard CloudStream .cs3 package with the embedded CloudStream runtime.",
                )
            }
            else -> {
            CloudStreamCompatibility(
                runtimeKind = CloudStreamRuntimeKind.UnsupportedAndroidDex,
                platformSupport = CloudStreamPlatformSupport.Unsupported,
                reason = "This standard .cs3 package contains Android DEX code, which cannot run on iOS.",
            )
            }
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
