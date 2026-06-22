package com.nuvio.app.features.ai

enum class AiProvider {
    CEREBRAS,
    GROQ,
    GEMINI,
    OPENROUTER,
}

val AiProvider.displayName: String
    get() = when (this) {
        AiProvider.CEREBRAS -> "Cerebras"
        AiProvider.GROQ -> "Groq"
        AiProvider.GEMINI -> "Gemini"
        AiProvider.OPENROUTER -> "OpenRouter Free"
    }

data class AiAssistantSettings(
    val enabled: Boolean = false,
    val provider: AiProvider = AiProvider.CEREBRAS,
    val cerebrasApiKey: String = "",
    val groqApiKey: String = "",
    val geminiApiKey: String = "",
    val openRouterApiKey: String = "",
    val cerebrasModel: String = DEFAULT_CEREBRAS_MODEL,
    val groqModel: String = DEFAULT_GROQ_MODEL,
    val geminiModel: String = DEFAULT_GEMINI_MODEL,
    val openRouterModel: String = DEFAULT_OPENROUTER_MODEL,
) {
    val activeApiKey: String
        get() = when (provider) {
            AiProvider.CEREBRAS -> cerebrasApiKey
            AiProvider.GROQ -> groqApiKey
            AiProvider.GEMINI -> geminiApiKey
            AiProvider.OPENROUTER -> openRouterApiKey
        }

    val activeModel: String
        get() = when (provider) {
            AiProvider.CEREBRAS -> cerebrasModel
            AiProvider.GROQ -> groqModel
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

internal const val DEFAULT_CEREBRAS_MODEL = "gpt-oss-120b"
internal const val DEFAULT_GROQ_MODEL = "openai/gpt-oss-120b"
internal const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"
internal const val DEFAULT_OPENROUTER_MODEL = "openrouter/free"
