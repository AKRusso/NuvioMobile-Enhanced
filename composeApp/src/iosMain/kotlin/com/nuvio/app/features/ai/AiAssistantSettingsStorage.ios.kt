package com.nuvio.app.features.ai

import com.nuvio.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

actual object AiAssistantSettingsStorage {
    actual fun loadEnabled(): Boolean? {
        val key = ProfileScopedKey.of("ai_assistant_enabled")
        val defaults = NSUserDefaults.standardUserDefaults
        return if (defaults.objectForKey(key) != null) defaults.boolForKey(key) else null
    }

    actual fun saveEnabled(value: Boolean) = saveBoolean("ai_assistant_enabled", value)
    actual fun loadProvider(): String? = loadString("ai_assistant_provider")
    actual fun saveProvider(value: String) = saveString("ai_assistant_provider", value)
    actual fun loadGeminiApiKey(): String? = loadString("ai_assistant_gemini_api_key")
    actual fun saveGeminiApiKey(value: String) = saveString("ai_assistant_gemini_api_key", value)
    actual fun loadOpenRouterApiKey(): String? = loadString("ai_assistant_openrouter_api_key")
    actual fun saveOpenRouterApiKey(value: String) = saveString("ai_assistant_openrouter_api_key", value)
    actual fun loadGeminiModel(): String? = loadString("ai_assistant_gemini_model")
    actual fun saveGeminiModel(value: String) = saveString("ai_assistant_gemini_model", value)
    actual fun loadOpenRouterModel(): String? = loadString("ai_assistant_openrouter_model")
    actual fun saveOpenRouterModel(value: String) = saveString("ai_assistant_openrouter_model", value)

    private fun loadString(key: String): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(key))

    private fun saveString(key: String, value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = ProfileScopedKey.of(key))
    }

    private fun saveBoolean(key: String, value: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(value, forKey = ProfileScopedKey.of(key))
    }
}
