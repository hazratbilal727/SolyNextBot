package com.example.data.provider

import com.example.data.model.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenAiCompatibleProvider(
    override val providerName: String,
    private val baseUrl: String,
    private val apiKey: String,
    override val currentModel: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) : AiProvider {

    override suspend fun generateResponse(
        messages: List<MessageEntity>,
        systemPrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("$providerName API key is missing. Please enter your key in Settings.")
            )
        }

        try {
            val root = JSONObject()
            root.put("model", currentModel)

            val jsonMessages = JSONArray()
            if (systemPrompt.isNotBlank()) {
                val sysMsg = JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                }
                jsonMessages.put(sysMsg)
            }

            messages.takeLast(16).forEach { msg ->
                val role = if (msg.sender == "user") "user" else "assistant"
                val item = JSONObject().apply {
                    put("role", role)
                    put("content", msg.text)
                }
                jsonMessages.put(item)
            }

            if (jsonMessages.length() == 0 || (jsonMessages.length() == 1 && systemPrompt.isNotBlank())) {
                jsonMessages.put(JSONObject().apply {
                    put("role", "user")
                    put("content", "Hello")
                })
            }

            root.put("messages", jsonMessages)
            root.put("temperature", 0.7)

            val requestBody = root.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val err = parseErrorMessage(responseBody, response.code)
                    return@withContext Result.failure(IOException("$providerName API error: $err"))
                }

                val json = JSONObject(responseBody)
                val choices = json.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val message = choices.getJSONObject(0).optJSONObject("message")
                    val content = message?.optString("content")
                    if (!content.isNullOrBlank()) {
                        return@withContext Result.success(content)
                    }
                }
                Result.failure(IOException("Empty response received from $providerName"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun validateConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("API key is empty"))
        }

        try {
            val root = JSONObject().apply {
                put("model", currentModel)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "ping")
                    })
                })
                put("max_tokens", 5)
            }

            val requestBody = root.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    val err = parseErrorMessage(response.body?.string().orEmpty(), response.code)
                    Result.failure(IOException("Validation failed: $err"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(body: String, code: Int): String {
        return try {
            val json = JSONObject(body)
            val error = json.optJSONObject("error")
            val message = error?.optString("message")
            if (!message.isNullOrBlank()) message else "HTTP $code"
        } catch (_: Exception) {
            "HTTP $code: $body"
        }
    }
}
