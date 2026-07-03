package com.nuvio.app.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DnsOverHttpsProvider(
    val id: String,
    val label: String,
    val endpointUrl: String?,
) {
    None(
        id = "none",
        label = "None",
        endpointUrl = null,
    ),
    Google(
        id = "google",
        label = "Google",
        endpointUrl = "https://dns.google/dns-query",
    ),
    Cloudflare(
        id = "cloudflare",
        label = "Cloudflare",
        endpointUrl = "https://cloudflare-dns.com/dns-query",
    ),
    AdGuard(
        id = "adguard",
        label = "AdGuard",
        endpointUrl = "https://dns.adguard-dns.com/dns-query",
    ),
    DnsWatch(
        id = "dnswatch",
        label = "DNS.WATCH",
        endpointUrl = "https://resolver2.dns.watch/dns-query",
    ),
    Quad9(
        id = "quad9",
        label = "Quad9",
        endpointUrl = "https://dns.quad9.net/dns-query",
    ),
    DnsSb(
        id = "dnssb",
        label = "DNS.SB",
        endpointUrl = "https://doh.dns.sb/dns-query",
    ),
    CanadianShield(
        id = "canadian_shield",
        label = "Canadian Shield",
        endpointUrl = "https://protected.canadianshield.cira.ca/dns-query",
    );

    companion object {
        fun fromId(id: String?): DnsOverHttpsProvider =
            entries.firstOrNull { it.id == id } ?: None
    }
}

data class DnsOverHttpsSettings(
    val provider: DnsOverHttpsProvider = DnsOverHttpsProvider.None,
)

object DnsOverHttpsSettingsRepository {
    private val _uiState = MutableStateFlow(DnsOverHttpsSettings())
    val uiState: StateFlow<DnsOverHttpsSettings> = _uiState.asStateFlow()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        hasLoaded = true
        _uiState.value = DnsOverHttpsSettings(
            provider = DnsOverHttpsProvider.fromId(DnsOverHttpsSettingsStorage.loadProviderId()),
        )
    }

    fun snapshot(): DnsOverHttpsSettings {
        ensureLoaded()
        return _uiState.value
    }

    fun setProvider(provider: DnsOverHttpsProvider) {
        ensureLoaded()
        if (_uiState.value.provider == provider) return
        _uiState.value = DnsOverHttpsSettings(provider = provider)
        DnsOverHttpsSettingsStorage.saveProviderId(provider.id)
    }
}

expect object DnsOverHttpsSettingsStorage {
    fun loadProviderId(): String?
    fun saveProviderId(providerId: String)
}
