package com.nuvio.app.features.ai

import android.content.Context
import android.content.SharedPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

actual object AiAssistantSettingsStorage {
    private const val preferencesName = "nuvio_ai_assistant_settings"
    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadEnabled(): Boolean? = loadBoolean("enabled")
    actual fun saveEnabled(value: Boolean) = saveBoolean("enabled", value)
    actual fun loadProvider(): String? = loadString("provider")
    actual fun saveProvider(value: String) = saveString("provider", value)
    actual fun loadCerebrasApiKey(): String? = loadString("cerebras_api_key")
    actual fun saveCerebrasApiKey(value: String) = saveString("cerebras_api_key", value)
    actual fun loadGroqApiKey(): String? = loadString("groq_api_key")
    actual fun saveGroqApiKey(value: String) = saveString("groq_api_key", value)
    actual fun loadGeminiApiKey(): String? = loadString("gemini_api_key")
    actual fun saveGeminiApiKey(value: String) = saveString("gemini_api_key", value)
    actual fun loadOpenRouterApiKey(): String? = loadString("openrouter_api_key")
    actual fun saveOpenRouterApiKey(value: String) = saveString("openrouter_api_key", value)
    actual fun loadCerebrasModel(): String? = loadString("cerebras_model")
    actual fun saveCerebrasModel(value: String) = saveString("cerebras_model", value)
    actual fun loadGroqModel(): String? = loadString("groq_model")
    actual fun saveGroqModel(value: String) = saveString("groq_model", value)
    actual fun loadGeminiModel(): String? = loadString("gemini_model")
    actual fun saveGeminiModel(value: String) = saveString("gemini_model", value)
    actual fun loadOpenRouterModel(): String? = loadString("openrouter_model")
    actual fun saveOpenRouterModel(value: String) = saveString("openrouter_model", value)

    private fun loadString(key: String): String? =
        preferences?.getString(ProfileScopedKey.of(key), null)

    private fun saveString(key: String, value: String) {
        preferences?.edit()?.putString(ProfileScopedKey.of(key), value)?.apply()
    }

    private fun loadBoolean(key: String): Boolean? = preferences?.let { prefs ->
        val scopedKey = ProfileScopedKey.of(key)
        if (prefs.contains(scopedKey)) prefs.getBoolean(scopedKey, false) else null
    }

    private fun saveBoolean(key: String, value: Boolean) {
        preferences?.edit()?.putBoolean(ProfileScopedKey.of(key), value)?.apply()
    }
}
