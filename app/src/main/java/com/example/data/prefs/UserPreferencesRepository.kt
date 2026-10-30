package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AiProviderType
import com.example.data.model.PersonalityStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "solynext_preferences")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val KEY_ACCOUNT_SETUP = booleanPreferencesKey("account_setup_completed")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_PHOTO_URI = stringPreferencesKey("user_photo_uri")
        val KEY_PROVIDER = stringPreferencesKey("provider_type")
        val KEY_GEMINI_KEY = stringPreferencesKey("gemini_api_key")
        val KEY_GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val KEY_OPENAI_KEY = stringPreferencesKey("openai_api_key")
        val KEY_OPENAI_MODEL = stringPreferencesKey("openai_model")
        val KEY_GROK_KEY = stringPreferencesKey("grok_api_key")
        val KEY_GROK_MODEL = stringPreferencesKey("grok_model")
        val KEY_PERSONALITY = stringPreferencesKey("personality_style")
        val KEY_SPEECH_RATE = floatPreferencesKey("speech_rate")
        val KEY_SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val KEY_AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val KEY_MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
    }

    val isAccountSetupFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_ACCOUNT_SETUP] ?: false }
    val userNameFlow: Flow<String> = context.dataStore.data.map { it[KEY_USER_NAME] ?: "" }
    val userPhotoUriFlow: Flow<String> = context.dataStore.data.map { it[KEY_USER_PHOTO_URI] ?: "" }

    val selectedProviderFlow: Flow<AiProviderType> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_PROVIDER] ?: AiProviderType.GEMINI.name
        try {
            AiProviderType.valueOf(name)
        } catch (_: Exception) {
            AiProviderType.GEMINI
        }
    }

    val personalityFlow: Flow<PersonalityStyle> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_PERSONALITY] ?: PersonalityStyle.JARVIS.name
        try {
            PersonalityStyle.valueOf(name)
        } catch (_: Exception) {
            PersonalityStyle.JARVIS
        }
    }

    val geminiKeyFlow: Flow<String> = context.dataStore.data.map { it[KEY_GEMINI_KEY] ?: "" }
    val geminiModelFlow: Flow<String> = context.dataStore.data.map { prefs ->
        val model = prefs[KEY_GEMINI_MODEL] ?: "gemini-2.0-flash"
        if (model == "gemini-1.5-flash" || model == "gemini-pro" || model == "gemini-1.0-pro" || model.isBlank()) {
            "gemini-2.0-flash"
        } else {
            model
        }
    }

    val openAiKeyFlow: Flow<String> = context.dataStore.data.map { it[KEY_OPENAI_KEY] ?: "" }
    val openAiModelFlow: Flow<String> = context.dataStore.data.map { it[KEY_OPENAI_MODEL] ?: "gpt-4o-mini" }

    val grokKeyFlow: Flow<String> = context.dataStore.data.map { it[KEY_GROK_KEY] ?: "" }
    val grokModelFlow: Flow<String> = context.dataStore.data.map { it[KEY_GROK_MODEL] ?: "grok-beta" }

    val speechRateFlow: Flow<Float> = context.dataStore.data.map { it[KEY_SPEECH_RATE] ?: 1.05f }
    val speechPitchFlow: Flow<Float> = context.dataStore.data.map { it[KEY_SPEECH_PITCH] ?: 1.0f }
    val autoSpeakFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_SPEAK] ?: true }
    val memoryEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_MEMORY_ENABLED] ?: true }

    suspend fun completeAccountSetup(
        name: String,
        photoUri: String,
        provider: AiProviderType,
        apiKey: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = name.trim()
            prefs[KEY_USER_PHOTO_URI] = photoUri.trim()
            prefs[KEY_PROVIDER] = provider.name
            when (provider) {
                AiProviderType.GEMINI -> {
                    prefs[KEY_GEMINI_KEY] = apiKey.trim()
                    prefs[KEY_GEMINI_MODEL] = "gemini-2.0-flash"
                }
                AiProviderType.OPENAI -> prefs[KEY_OPENAI_KEY] = apiKey.trim()
                AiProviderType.GROK -> prefs[KEY_GROK_KEY] = apiKey.trim()
            }
            prefs[KEY_ACCOUNT_SETUP] = true
        }
    }

    suspend fun setProvider(provider: AiProviderType) {
        context.dataStore.edit { it[KEY_PROVIDER] = provider.name }
    }

    suspend fun setPersonality(personality: PersonalityStyle) {
        context.dataStore.edit { it[KEY_PERSONALITY] = personality.name }
    }

    suspend fun setGeminiCredentials(apiKey: String, model: String) {
        val cleanModel = when {
            model.isBlank() || model == "gemini-1.5-flash" || model == "gemini-pro" -> "gemini-2.0-flash"
            else -> model.trim()
        }
        context.dataStore.edit {
            it[KEY_GEMINI_KEY] = apiKey.trim()
            it[KEY_GEMINI_MODEL] = cleanModel
        }
    }

    suspend fun setOpenAiCredentials(apiKey: String, model: String) {
        context.dataStore.edit {
            it[KEY_OPENAI_KEY] = apiKey.trim()
            if (model.isNotBlank()) it[KEY_OPENAI_MODEL] = model.trim()
        }
    }

    suspend fun setGrokCredentials(apiKey: String, model: String) {
        context.dataStore.edit {
            it[KEY_GROK_KEY] = apiKey.trim()
            if (model.isNotBlank()) it[KEY_GROK_MODEL] = model.trim()
        }
    }

    suspend fun setVoiceSettings(rate: Float, pitch: Float, autoSpeak: Boolean) {
        context.dataStore.edit {
            it[KEY_SPEECH_RATE] = rate
            it[KEY_SPEECH_PITCH] = pitch
            it[KEY_AUTO_SPEAK] = autoSpeak
        }
    }

    suspend fun setMemoryEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_MEMORY_ENABLED] = enabled }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[KEY_USER_NAME] = name.trim() }
    }

    suspend fun setUserPhotoUri(photoUri: String) {
        context.dataStore.edit { it[KEY_USER_PHOTO_URI] = photoUri.trim() }
    }

    suspend fun resetAccount() {
        context.dataStore.edit { it[KEY_ACCOUNT_SETUP] = false }
    }
}
