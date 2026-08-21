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
                webSearchEnabled = AiAssistantSettingsStorage.loadWebSearchEnabled() ?: true,
                provider = AiAssistantSettingsStorage.loadProvider()
                    ?.let { value -> runCatching { AiProvider.valueOf(value) }.getOrNull() }
                    ?: AiProvider.CEREBRAS,
                tavilyApiKey = AiAssistantSettingsStorage.loadTavilyApiKey()?.trim().orEmpty(),
                cerebrasApiKey = AiAssistantSettingsStorage.loadCerebrasApiKey()?.trim().orEmpty(),
                groqApiKey = AiAssistantSettingsStorage.loadGroqApiKey()?.trim().orEmpty(),
                geminiApiKey = AiAssistantSettingsStorage.loadGeminiApiKey()?.trim().orEmpty(),
                openRouterApiKey = AiAssistantSettingsStorage.loadOpenRouterApiKey()?.trim().orEmpty(),
                cerebrasModel = AiAssistantSettingsStorage.loadCerebrasModel()
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?: DEFAULT_CEREBRAS_MODEL,
                groqModel = AiAssistantSettingsStorage.loadGroqModel()
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?: DEFAULT_GROQ_MODEL,
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

    fun onProfileChanged() {
        hasLoaded = false
        ensureLoaded()
    }

    fun setEnabled(value: Boolean) = update { copy(enabled = value) }

    fun setWebSearchEnabled(value: Boolean) = update { copy(webSearchEnabled = value) }

    fun setProvider(value: AiProvider) = update { copy(provider = value) }

    fun setTavilyApiKey(value: String) = update { copy(tavilyApiKey = value.trim()) }

    fun setCerebrasApiKey(value: String) = update { copy(cerebrasApiKey = value.trim()) }

    fun setGroqApiKey(value: String) = update { copy(groqApiKey = value.trim()) }

    fun setGeminiApiKey(value: String) = update { copy(geminiApiKey = value.trim()) }

    fun setOpenRouterApiKey(value: String) = update { copy(openRouterApiKey = value.trim()) }

    fun setCerebrasModel(value: String) = update {
        copy(cerebrasModel = value.trim().ifBlank { DEFAULT_CEREBRAS_MODEL })
    }

    fun setGroqModel(value: String) = update {
        copy(groqModel = value.trim().ifBlank { DEFAULT_GROQ_MODEL })
    }

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
        AiAssistantSettingsStorage.saveWebSearchEnabled(next.webSearchEnabled)
        AiAssistantSettingsStorage.saveProvider(next.provider.name)
        AiAssistantSettingsStorage.saveTavilyApiKey(next.tavilyApiKey)
        AiAssistantSettingsStorage.saveCerebrasApiKey(next.cerebrasApiKey)
        AiAssistantSettingsStorage.saveGroqApiKey(next.groqApiKey)
        AiAssistantSettingsStorage.saveGeminiApiKey(next.geminiApiKey)
        AiAssistantSettingsStorage.saveOpenRouterApiKey(next.openRouterApiKey)
        AiAssistantSettingsStorage.saveCerebrasModel(next.cerebrasModel)
        AiAssistantSettingsStorage.saveGroqModel(next.groqModel)
        AiAssistantSettingsStorage.saveGeminiModel(next.geminiModel)
        AiAssistantSettingsStorage.saveOpenRouterModel(next.openRouterModel)
    }

    private fun publish(settings: AiAssistantSettings) {
        _uiState.value = settings
    }
}
