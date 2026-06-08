package com.example.aichat.data

data class AiProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKeyUrl: String,
    val models: List<String>,
    val isCustom: Boolean = false
)

object BuiltInProviders {
    val providers = listOf(
        AiProvider(
            id = "openai",
            name = "OpenAI",
            baseUrl = "https://api.openai.com/v1/",
            apiKeyUrl = "https://platform.openai.com/api-keys",
            models = listOf("gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-4", "gpt-3.5-turbo", "o1-preview", "o1-mini")
        ),
        AiProvider(
            id = "anthropic",
            name = "Anthropic (Claude)",
            baseUrl = "https://api.anthropic.com/v1/",
            apiKeyUrl = "https://console.anthropic.com/settings/keys",
            models = listOf("claude-sonnet-4-20250514", "claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022", "claude-3-opus-20240229")
        ),
        AiProvider(
            id = "google",
            name = "Google Gemini",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai/",
            apiKeyUrl = "https://aistudio.google.com/apikey",
            models = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash", "gemini-1.5-pro", "gemini-1.5-flash")
        ),
        AiProvider(
            id = "openrouter",
            name = "OpenRouter",
            baseUrl = "https://openrouter.ai/api/v1/",
            apiKeyUrl = "https://openrouter.ai/keys",
            models = listOf("openai/gpt-4o", "anthropic/claude-sonnet-4", "google/gemini-2.5-flash", "meta-llama/llama-3.1-405b", "deepseek/deepseek-r1")
        ),
        AiProvider(
            id = "groq",
            name = "Groq",
            baseUrl = "https://api.groq.com/openai/v1/",
            apiKeyUrl = "https://console.groq.com/keys",
            models = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b-32768", "gemma2-9b-it")
        ),
        AiProvider(
            id = "deepseek",
            name = "DeepSeek",
            baseUrl = "https://api.deepseek.com/v1/",
            apiKeyUrl = "https://platform.deepseek.com/api_keys",
            models = listOf("deepseek-chat", "deepseek-reasoner")
        ),
        AiProvider(
            id = "mistral",
            name = "Mistral AI",
            baseUrl = "https://api.mistral.ai/v1/",
            apiKeyUrl = "https://console.mistral.ai/api-keys/",
            models = listOf("mistral-large-latest", "mistral-medium-latest", "mistral-small-latest", "open-mixtral-8x22b")
        ),
        AiProvider(
            id = "ollama",
            name = "Ollama (Local)",
            baseUrl = "http://localhost:11434/v1/",
            apiKeyUrl = "https://ollama.com/download",
            models = listOf("llama3.1", "llama3", "mistral", "codellama", "phi3", "gemma2")
        )
    )

    fun findById(id: String): AiProvider? = providers.find { it.id == id }
}
