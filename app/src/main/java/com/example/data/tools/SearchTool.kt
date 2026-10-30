package com.example.data.tools

import com.example.data.model.ToolExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class SearchTool(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    suspend fun searchWeb(query: String): ToolExecutionResult = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            return@withContext ToolExecutionResult(
                toolName = "Web Search",
                summary = "Search query was empty.",
                isSuccess = false
            )
        }

        try {
            val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
            // 1. Try DuckDuckGo Instant Answer API
            val ddgUrl = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"
            val ddgRequest = Request.Builder().url(ddgUrl).build()

            var answer = ""
            var sourceUrl = "https://duckduckgo.com/?q=$encodedQuery"

            client.newCall(ddgRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val abstractText = json.optString("AbstractText", "")
                    val abstractUrl = json.optString("AbstractURL", "")
                    val heading = json.optString("Heading", "")

                    if (abstractText.isNotBlank()) {
                        answer = if (heading.isNotBlank()) "$heading: $abstractText" else abstractText
                        if (abstractUrl.isNotBlank()) sourceUrl = abstractUrl
                    }
                }
            }

            // 2. If DuckDuckGo returned nothing, query Wikipedia API for summary
            if (answer.isBlank()) {
                val wikiUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/$encodedQuery"
                val wikiRequest = Request.Builder().url(wikiUrl).build()

                client.newCall(wikiRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string().orEmpty()
                        val json = JSONObject(body)
                        val extract = json.optString("extract", "")
                        val title = json.optString("title", cleanQuery)
                        val contentUrls = json.optJSONObject("content_urls")
                        val desktop = contentUrls?.optJSONObject("desktop")
                        val pageUrl = desktop?.optString("page", "")

                        if (extract.isNotBlank()) {
                            answer = "$title: $extract"
                            if (!pageUrl.isNullOrBlank()) sourceUrl = pageUrl
                        }
                    }
                }
            }

            if (answer.isNotBlank()) {
                ToolExecutionResult(
                    toolName = "Web Search",
                    summary = answer,
                    detailedData = "Query: $cleanQuery\nSource: $sourceUrl",
                    sourceUrl = sourceUrl,
                    isSuccess = true
                )
            } else {
                ToolExecutionResult(
                    toolName = "Web Search",
                    summary = "No immediate encyclopedic summary found for '$cleanQuery'. Live DuckDuckGo link prepared.",
                    detailedData = "Query: $cleanQuery",
                    sourceUrl = sourceUrl,
                    isSuccess = true
                )
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                toolName = "Web Search",
                summary = "Search lookup failed for '$cleanQuery': ${e.localizedMessage}",
                isSuccess = false
            )
        }
    }
}
