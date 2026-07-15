package com.nuvio.app.features.settings

internal data class NuvioAppIconOption(
    val id: String,
) {
    companion object {
        val Default = NuvioAppIconOption("default")
        val Neon = NuvioAppIconOption("neon")
        val Gear = NuvioAppIconOption("gear")
        val Chrome = NuvioAppIconOption("chrome")
        val Aurora = NuvioAppIconOption("aurora")
        val Emerald = NuvioAppIconOption("emerald")

        val entries: List<NuvioAppIconOption> = listOf(
            Default,
            Neon,
            Gear,
            Chrome,
            Aurora,
            Emerald,
        )
    }
}

internal expect object NuvioAppIconSwitcher {
    fun apply(iconId: String): Boolean
}
