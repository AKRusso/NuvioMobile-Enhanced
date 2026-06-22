package com.nuvio.app.features.ai

internal expect object AiAssistantSettingsStorage {
    fun loadEnabled(): Boolean?
    fun saveEnabled(value: Boolean)
    fun loadProvider(): String?
    fun saveProvider(value: String)
    fun loadCerebrasApiKey(): String?
    fun saveCerebrasApiKey(value: String)
    fun loadGroqApiKey(): String?
    fun saveGroqApiKey(value: String)
    fun loadGeminiApiKey(): String?
    fun saveGeminiApiKey(value: String)
    fun loadOpenRouterApiKey(): String?
    fun saveOpenRouterApiKey(value: String)
    fun loadCerebrasModel(): String?
    fun saveCerebrasModel(value: String)
    fun loadGroqModel(): String?
    fun saveGroqModel(value: String)
    fun loadGeminiModel(): String?
    fun saveGeminiModel(value: String)
    fun loadOpenRouterModel(): String?
    fun saveOpenRouterModel(value: String)
}
