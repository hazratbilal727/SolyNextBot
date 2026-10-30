package com.example.data.provider

import com.example.data.model.MessageEntity

interface AiProvider {
    val providerName: String
    val currentModel: String

    suspend fun generateResponse(
        messages: List<MessageEntity>,
        systemPrompt: String
    ): Result<String>

    suspend fun validateConnection(): Result<Boolean>
}
