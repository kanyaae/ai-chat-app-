package com.example.aichat.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    private val gson = Gson()

    companion object {
        val API_KEY = stringPreferencesKey("api_key")
        val PROVIDER_ID = stringPreferencesKey("provider_id")
        val MODEL_NAME = stringPreferencesKey("model_name")
        val ENDPOINT_URL = stringPreferencesKey("endpoint_url")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val TEMPERATURE = stringPreferencesKey("temperature")
        val CUSTOM_PROVIDERS = stringPreferencesKey("custom_providers")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COLOR_PALETTE = stringPreferencesKey("color_palette")
        val FONT_SIZE = stringPreferencesKey("font_size")
        val MEMORY_ENABLED = stringPreferencesKey("memory_enabled")
        val LANGUAGE = stringPreferencesKey("language")
        val BACKGROUND_URI = stringPreferencesKey("background_uri")
        val CUSTOM_COLOR = stringPreferencesKey("custom_color")
        val TELEGRAM_TOKEN = stringPreferencesKey("telegram_token")
    }

    val apiKeyFlow: Flow<String> = context.dataStore.data.map { it[API_KEY] ?: "" }
    val providerIdFlow: Flow<String> = context.dataStore.data.map { it[PROVIDER_ID] ?: "openai" }
    val modelNameFlow: Flow<String> = context.dataStore.data.map { it[MODEL_NAME] ?: "gpt-4o-mini" }
    val endpointUrlFlow: Flow<String> = context.dataStore.data.map { it[ENDPOINT_URL] ?: "https://api.openai.com/v1/" }
    val systemPromptFlow: Flow<String> = context.dataStore.data.map { it[SYSTEM_PROMPT] ?: "You are a helpful assistant." }
    val temperatureFlow: Flow<Float> = context.dataStore.data.map { (it[TEMPERATURE] ?: "0.7").toFloatOrNull() ?: 0.7f }
    val themeModeFlow: Flow<String> = context.dataStore.data.map { it[THEME_MODE] ?: "system" }
    val colorPaletteFlow: Flow<String> = context.dataStore.data.map { it[COLOR_PALETTE] ?: "neon" }
    val fontSizeFlow: Flow<String> = context.dataStore.data.map { it[FONT_SIZE] ?: "medium" }
    val memoryEnabledFlow: Flow<Boolean> = context.dataStore.data.map { (it[MEMORY_ENABLED] ?: "true") == "true" }
    val languageFlow: Flow<String> = context.dataStore.data.map { it[LANGUAGE] ?: "system" }
    val backgroundUriFlow: Flow<String> = context.dataStore.data.map { it[BACKGROUND_URI] ?: "" }
    val customColorFlow: Flow<String> = context.dataStore.data.map { it[CUSTOM_COLOR] ?: "" }
    val telegramTokenFlow: Flow<String> = context.dataStore.data.map { it[TELEGRAM_TOKEN] ?: "" }

    val customProvidersFlow: Flow<List<AiProvider>> = context.dataStore.data.map { prefs ->
        val json = prefs[CUSTOM_PROVIDERS] ?: "[]"
        try {
            val type = object : TypeToken<List<AiProvider>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveProvider(providerId: String, baseUrl: String, modelName: String) {
        context.dataStore.edit {
            it[PROVIDER_ID] = providerId
            it[ENDPOINT_URL] = baseUrl
            it[MODEL_NAME] = modelName
        }
    }

    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { it[API_KEY] = apiKey }
    }

    suspend fun saveSystemPrompt(prompt: String) {
        context.dataStore.edit { it[SYSTEM_PROMPT] = prompt }
    }

    suspend fun saveTemperature(temp: Float) {
        context.dataStore.edit { it[TEMPERATURE] = temp.toString() }
    }

    suspend fun saveTelegramToken(token: String) {
        context.dataStore.edit { it[TELEGRAM_TOKEN] = token }
    }

    suspend fun saveCustomProviders(providers: List<AiProvider>) {
        context.dataStore.edit {
            it[CUSTOM_PROVIDERS] = gson.toJson(providers)
        }
    }

    suspend fun saveAppearanceSettings(themeMode: String, colorPalette: String, fontSize: String, backgroundUri: String = "", customColor: String = "") {
        context.dataStore.edit {
            it[THEME_MODE] = themeMode
            it[COLOR_PALETTE] = colorPalette
            it[FONT_SIZE] = fontSize
            it[BACKGROUND_URI] = backgroundUri
            it[CUSTOM_COLOR] = customColor
        }
    }

    suspend fun saveMemoryEnabled(enabled: Boolean) {
        context.dataStore.edit { it[MEMORY_ENABLED] = enabled.toString() }
    }

    suspend fun saveLanguage(languageCode: String) {
        context.dataStore.edit { it[LANGUAGE] = languageCode }
    }
}
