package com.example.data.provider

import com.example.data.model.AiProviderType
import com.example.data.prefs.UserPreferencesRepository
import kotlinx.coroutines.flow.first

class AiProviderManager(
    private val preferencesRepository: UserPreferencesRepository
) {
    suspend fun getActiveProvider(): AiProvider {
        val selectedType = preferencesRepository.selectedProviderFlow.first()
        return createProvider(selectedType)
    }

    suspend fun createProvider(type: AiProviderType): AiProvider {
        return when (type) {
            AiProviderType.GEMINI -> {
                val customKey = preferencesRepository.geminiKeyFlow.first()
                val model = preferencesRepository.geminiModelFlow.first()
                GeminiProvider(customApiKey = customKey, currentModel = model)
            }
            AiProviderType.OPENAI -> {
                val apiKey = preferencesRepository.openAiKeyFlow.first()
                val model = preferencesRepository.openAiModelFlow.first()
                OpenAiCompatibleProvider(
                    providerName = "OpenAI",
                    baseUrl = "https://api.openai.com/v1",
                    apiKey = apiKey,
                    currentModel = model
                )
            }
            AiProviderType.GROK -> {
                val apiKey = preferencesRepository.grokKeyFlow.first()
                val model = preferencesRepository.grokModelFlow.first()
                OpenAiCompatibleProvider(
                    providerName = "xAI Grok",
                    baseUrl = "https://api.x.ai/v1",
                    apiKey = apiKey,
                    currentModel = model
                )
            }
        }
    }
}
