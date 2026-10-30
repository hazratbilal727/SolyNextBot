package com.example.data.provider

import com.example.BuildConfig
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

/**
 * Robust Google Gemini provider supporting all Gemini API key versions and models.
 * Uses "gemini-2.0-flash" as the current standard model identifier and dynamically
 * routes between v1beta and v1 endpoints to prevent 404 / unsupported method errors.
 */
class GeminiProvider(
    private val customApiKey: String,
    override val currentModel: String = DEFAULT_MODEL,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) : AiProvider {

    override val providerName: String = "Google Gemini"

    companion object {
        const val DEFAULT_MODEL = "gemini-2.0-flash"

        val KNOWN_MODELS = listOf(
            "gemini-2.0-flash",
            "gemini-2.5-flash",
            "gemini-2.0-flash-lite",
            "gemini-1.5-flash",
            "gemini-1.5-pro",
            "gemini-pro"
        )

        /**
         * Normalizes outdated model names to the current standard model identifier.
         */
        fun normalizeModel(model: String): String {
            val clean = model.trim().removePrefix("models/")
            return when {
                clean.isBlank() || clean == "gemini-1.5-flash" || clean == "gemini-pro" || clean == "gemini-1.0-pro" -> DEFAULT_MODEL
                else -> clean
            }
        }

        /**
         * Determines the candidate endpoint URLs for a model in accordance with
         * the latest Google Generative AI REST API specifications.
         * Gemini 2.0+ models use v1beta as primary, while Gemini 1.5 models use v1 as primary.
         */
        fun getGenerateEndpoints(modelName: String, apiKey: String): List<String> {
            val clean = modelName.trim().removePrefix("models/")
            return if (clean.startsWith("gemini-1.5")) {
                listOf(
                    "https://generativelanguage.googleapis.com/v1/models/$clean:generateContent?key=$apiKey",
                    "https://generativelanguage.googleapis.com/v1beta/models/$clean:generateContent?key=$apiKey"
                )
            } else {
                listOf(
                    "https://generativelanguage.googleapis.com/v1beta/models/$clean:generateContent?key=$apiKey",
                    "https://generativelanguage.googleapis.com/v1/models/$clean:generateContent?key=$apiKey"
                )
            }
        }
    }

    private fun getResolvedApiKey(): String {
        val cleanCustom = customApiKey.trim().removeSurrounding("\"").removeSurrounding("'")
        if (cleanCustom.isNotBlank()) return cleanCustom

        // Fallback to BuildConfig.GEMINI_API_KEY
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY.trim().removeSurrounding("\"").removeSurrounding("'")
        } catch (_: Exception) {
            ""
        }
        return if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
    }

    /**
     * Universal validation supporting ANY Gemini API key version.
     * Queries the Google Generative Language API models catalog endpoint across v1beta and v1.
     * If model listing is restricted, probes generateContent with standard gemini-2.0-flash.
     */
    override suspend fun validateConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        val apiKey = getResolvedApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("API key is empty. Please enter your Gemini API key."))
        }

        // Try standard v1beta and v1 models discovery endpoints
        val validationEndpoints = listOf(
            "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey",
            "https://generativelanguage.googleapis.com/v1/models?key=$apiKey"
        )

        var lastErrorMsg: String? = null

        for (endpoint in validationEndpoints) {
            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("x-goog-api-key", apiKey)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        return@withContext Result.success(true)
                    } else {
                        val parsedError = parseErrorMessage(body, response.code)
                        lastErrorMsg = parsedError
                        // If invalid key error (400 or 403), stop early and report clearly
                        if (response.code == 400 && (body.contains("API_KEY_INVALID", ignoreCase = true) || body.contains("INVALID_ARGUMENT", ignoreCase = true))) {
                            return@withContext Result.failure(IOException("Invalid Gemini API key ($parsedError)"))
                        }
                        if (response.code == 403 && (body.contains("PERMISSION_DENIED", ignoreCase = true) || body.contains("CONSUMER_SUSPENDED", ignoreCase = true))) {
                            return@withContext Result.failure(IOException("Gemini API key lacks permission or quota ($parsedError)"))
                        }
                    }
                }
            } catch (e: Exception) {
                lastErrorMsg = e.localizedMessage
            }
        }

        // Secondary fallback: test generateContent against standard 2.0 models if models catalog was restricted
        val activeModel = normalizeModel(currentModel)
        val candidateModels = listOf(
            activeModel,
            DEFAULT_MODEL,
            "gemini-2.5-flash",
            "gemini-2.0-flash-lite"
        ).distinct()

        for (model in candidateModels) {
            val endpoints = getGenerateEndpoints(model, apiKey)
            for (endpointUrl in endpoints) {
                try {
                    val pingBody = JSONObject().apply {
                        put("contents", JSONArray().apply {
                            put(JSONObject().apply {
                                put("role", "user")
                                put("parts", JSONArray().apply { put(JSONObject().apply { put("text", "Hi") }) })
                            })
                        })
                    }.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

                    val request = Request.Builder()
                        .url(endpointUrl)
                        .addHeader("x-goog-api-key", apiKey)
                        .post(pingBody)
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            return@withContext Result.success(true)
                        } else {
                            val errBody = response.body?.string().orEmpty()
                            val err = parseErrorMessage(errBody, response.code)
                            lastErrorMsg = err
                        }
                    }
                } catch (e: Exception) {
                    lastErrorMsg = e.localizedMessage
                }
            }
        }

        Result.failure(IOException(lastErrorMsg ?: "Could not validate Gemini API key. Please verify the key from Google AI Studio."))
    }

    /**
     * Discovers all available models supported by this specific API key.
     */
    suspend fun discoverAvailableModels(): Result<List<String>> = withContext(Dispatchers.IO) {
        val apiKey = getResolvedApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("API key is empty"))
        }

        val endpoints = listOf(
            "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey",
            "https://generativelanguage.googleapis.com/v1/models?key=$apiKey"
        )

        var lastError: String? = null

        for (url in endpoints) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("x-goog-api-key", apiKey)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        lastError = parseErrorMessage(body, response.code)
                        return@use
                    }

                    val json = JSONObject(body)
                    val modelsArray = json.optJSONArray("models") ?: JSONArray()
                    val resultList = mutableListOf<String>()

                    for (i in 0 until modelsArray.length()) {
                        val m = modelsArray.getJSONObject(i)
                        val rawName = m.optString("name")
                        val cleanName = rawName.removePrefix("models/")
                        val methods = m.optJSONArray("supportedGenerationMethods")
                        var canGenerate = false
                        if (methods != null) {
                            for (j in 0 until methods.length()) {
                                if (methods.getString(j) == "generateContent") {
                                    canGenerate = true
                                    break
                                }
                            }
                        }
                        if (canGenerate && cleanName.isNotBlank()) {
                            resultList.add(cleanName)
                        }
                    }

                    if (resultList.isNotEmpty()) {
                        return@withContext Result.success(resultList)
                    }
                }
            } catch (e: Exception) {
                lastError = e.localizedMessage
            }
        }

        Result.failure(IOException(lastError ?: "Failed to retrieve available Gemini models list."))
    }

    override suspend fun generateResponse(
        messages: List<MessageEntity>,
        systemPrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getResolvedApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please enter your key in Settings or Onboarding.")
            )
        }

        // Candidates to attempt in priority order, standardizing on gemini-2.0-flash first
        val activeModel = normalizeModel(currentModel)
        val candidateModels = listOf(
            activeModel,
            DEFAULT_MODEL,
            "gemini-2.5-flash",
            "gemini-2.0-flash-lite",
            "gemini-1.5-flash",
            "gemini-1.5-pro"
        ).distinct()

        var lastError: String? = null

        for (model in candidateModels) {
            val result = executeGenerateWithModel(apiKey, model, messages, systemPrompt)
            if (result.isSuccess) {
                return@withContext result
            }
            lastError = result.exceptionOrNull()?.message
            // If the error was not a 404/model not found error (e.g. invalid key or quota limit), don't keep cycling
            if (lastError?.contains("API_KEY_INVALID", ignoreCase = true) == true ||
                lastError?.contains("leaked", ignoreCase = true) == true ||
                lastError?.contains("quota", ignoreCase = true) == true
            ) {
                return@withContext result
            }
        }

        Result.failure(IOException(lastError ?: "Failed to generate response across Gemini model versions."))
    }

    private fun executeGenerateWithModel(
        apiKey: String,
        modelName: String,
        messages: List<MessageEntity>,
        systemPrompt: String
    ): Result<String> {
        try {
            val root = JSONObject()

            // System instruction (supported on Gemini 1.5 and 2.0+)
            val cleanModel = modelName.removePrefix("models/")
            val isLegacyModel = cleanModel.contains("gemini-1.0", ignoreCase = true) || cleanModel == "gemini-pro"
            if (systemPrompt.isNotBlank() && !isLegacyModel) {
                val sysInstruction = JSONObject()
                val parts = JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                }
                sysInstruction.put("parts", parts)
                root.put("system_instruction", sysInstruction)
            }

            // Message history
            val contents = JSONArray()
            val recentMessages = messages.takeLast(16)

            recentMessages.forEachIndexed { index, msg ->
                val role = if (msg.sender == "user") "user" else "model"
                val item = JSONObject()
                item.put("role", role)
                val textContent = if (index == 0 && isLegacyModel && systemPrompt.isNotBlank() && role == "user") {
                    "[System instruction: $systemPrompt]\n\n${msg.text}"
                } else {
                    msg.text
                }
                val parts = JSONArray().apply {
                    put(JSONObject().apply { put("text", textContent) })
                }
                item.put("parts", parts)
                contents.put(item)
            }

            // Fallback if empty
            if (contents.length() == 0) {
                contents.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply { put(JSONObject().apply { put("text", "Hello") }) })
                })
            }

            root.put("contents", contents)

            val genConfig = JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 1024)
            }
            root.put("generationConfig", genConfig)

            val requestBody = root.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            // Try the model's preferred endpoint first, then alternate version if 404 occurs
            val endpoints = getGenerateEndpoints(cleanModel, apiKey)
            var lastEndpointError: String? = null

            for (url in endpoints) {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("x-goog-api-key", apiKey)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val errorMsg = parseErrorMessage(responseBody, response.code)
                        lastEndpointError = errorMsg
                        // If it's a 404 / NOT_FOUND / model not found, try next endpoint in list
                        if (response.code == 404 || errorMsg.contains("not found", ignoreCase = true) || errorMsg.contains("not supported", ignoreCase = true)) {
                            return@use
                        }
                        return Result.failure(IOException("Gemini ($cleanModel) error: $errorMsg"))
                    }

                    val jsonResponse = JSONObject(responseBody)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        val contentObj = candidate.optJSONObject("content")
                        val parts = contentObj?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text")
                            return Result.success(text)
                        }
                    }
                    return Result.failure(IOException("Empty response from Gemini $cleanModel"))
                }
            }

            return Result.failure(IOException("Gemini ($cleanModel) error: ${lastEndpointError ?: "Endpoint not found"}"))
        } catch (e: Exception) {
            return Result.failure(e)
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
