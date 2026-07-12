package com.nuvio.app.features.cloudstream

internal actual object CloudStreamPlatformRuntime {
    actual val supportsAndroidDex: Boolean = false

    actual fun initialize(context: Any?) = Unit
    actual suspend fun provider(plugin: CloudStreamPluginItem): CloudStreamProvider? = null
    actual fun unload(pluginId: String) = Unit
    actual fun clear() = Unit
}
