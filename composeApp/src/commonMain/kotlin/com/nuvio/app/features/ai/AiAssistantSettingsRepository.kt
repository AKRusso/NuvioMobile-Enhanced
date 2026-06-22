package com.nuvio.app.features.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AiAssistantSettingsRepository {
    private val _uiState = MutableStateFlow(AiAssistantSettings())
    val uiState: StateFlow<AiAssistantSettings> = _uiState.asStateFlow()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        hasLoaded = true
        publish(
            AiAssistantSettings(
                enabled = AiAssistantSettingsStorage.loadEnabled() ?: false,
                provider = AiAssistantSettingsStorage.loadProvider()
                    ?.let { value -> runCatching { AiProvider.valueOf(value) }.getOrNull() }
                    ?: AiProvider.GEMINI,
                geminiApiKey = AiAssistantSettingsStorage.loadGeminiApiKey()?.trim().orEmpty(),
                openRouterApiKey = AiAssistantSettingsStorage.loadOpenRouterApiKey()?.trim().orEmpty(),
                geminiModel = AiAssistantSettingsStorage.loadGeminiModel()
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?: DEFAULT_GEMINI_MODEL,
                openRouterModel = AiAssistantSettingsStorage.loadOpenRouterModel()
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?: DEFAULT_OPENROUTER_MODEL,
            ),
        )
    }

    fun setEnabled(value: Boolean) = update { copy(enabled = value) }

    fun setProvider(value: AiProvider) = update { copy(provider = value) }

    fun setGeminiApiKey(value: String) = update { copy(geminiApiKey = value.trim()) }

    fun setOpenRouterApiKey(value: String) = update { copy(openRouterApiKey = value.trim()) }

    fun setGeminiModel(value: String) = update {
        copy(geminiModel = value.trim().ifBlank { DEFAULT_GEMINI_MODEL })
    }

    fun setOpenRouterModel(value: String) = update {
        copy(openRouterModel = value.trim().ifBlank { DEFAULT_OPENROUTER_MODEL })
    }

    private fun update(transform: AiAssistantSettings.() -> AiAssistantSettings) {
        ensureLoaded()
        val next = _uiState.value.transform()
        publish(next)
        AiAssistantSettingsStorage.saveEnabled(next.enabled)
        AiAssistantSettingsStorage.saveProvider(next.provider.name)
        AiAssistantSettingsStorage.saveGeminiApiKey(next.geminiApiKey)
        AiAssistantSettingsStorage.saveOpenRouterApiKey(next.openRouterApiKey)
        AiAssistantSettingsStorage.saveGeminiModel(next.geminiModel)
        AiAssistantSettingsStorage.saveOpenRouterModel(next.openRouterModel)
    }

    private fun publish(settings: AiAssistantSettings) {
        _uiState.value = settings
    }
}

