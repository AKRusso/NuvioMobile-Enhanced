package com.nuvio.app.features.ai

internal expect object AiAssistantSettingsStorage {
    fun loadEnabled(): Boolean?
    fun saveEnabled(value: Boolean)
    fun loadProvider(): String?
    fun saveProvider(value: String)
    fun loadGeminiApiKey(): String?
    fun saveGeminiApiKey(value: String)
    fun loadOpenRouterApiKey(): String?
    fun saveOpenRouterApiKey(value: String)
    fun loadGeminiModel(): String?
    fun saveGeminiModel(value: String)
    fun loadOpenRouterModel(): String?
    fun saveOpenRouterModel(value: String)
}

