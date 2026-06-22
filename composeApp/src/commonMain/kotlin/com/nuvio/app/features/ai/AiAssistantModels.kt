package com.nuvio.app.features.ai

enum class AiProvider {
    GEMINI,
    OPENROUTER,
}

data class AiAssistantSettings(
    val enabled: Boolean = false,
    val provider: AiProvider = AiProvider.GEMINI,
    val geminiApiKey: String = "",
    val openRouterApiKey: String = "",
    val geminiModel: String = DEFAULT_GEMINI_MODEL,
    val openRouterModel: String = DEFAULT_OPENROUTER_MODEL,
) {
    val activeApiKey: String
        get() = when (provider) {
            AiProvider.GEMINI -> geminiApiKey
            AiProvider.OPENROUTER -> openRouterApiKey
        }

    val activeModel: String
        get() = when (provider) {
            AiProvider.GEMINI -> geminiModel
            AiProvider.OPENROUTER -> openRouterModel
        }

    val isReady: Boolean
        get() = enabled && activeApiKey.isNotBlank() && activeModel.isNotBlank()
}

data class AiChatMessage(
    val role: AiChatRole,
    val text: String,
)

enum class AiChatRole {
    USER,
    ASSISTANT,
}

internal const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"
internal const val DEFAULT_OPENROUTER_MODEL = "openrouter/free"

