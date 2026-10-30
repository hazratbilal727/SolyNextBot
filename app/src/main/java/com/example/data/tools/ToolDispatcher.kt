package com.example.data.tools

import com.example.data.db.AppDatabase
import com.example.data.model.MemoryEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.ToolExecutionResult
import com.example.data.reminder.ReminderScheduler
import java.util.Calendar

class ToolDispatcher(
    private val weatherTool: WeatherTool,
    private val searchTool: SearchTool,
    private val appDatabase: AppDatabase,
    private val reminderScheduler: ReminderScheduler
) {
    suspend fun executeWeather(location: String): ToolExecutionResult {
        return weatherTool.getWeather(location)
    }

    suspend fun executeSearch(query: String): ToolExecutionResult {
        return searchTool.searchWeb(query)
    }

    suspend fun executeSaveMemory(fact: String, category: String = "fact"): ToolExecutionResult {
        return try {
            val memory = MemoryEntity(
                category = category,
                fact = fact.trim(),
                isApproved = true
            )
            appDatabase.memoryDao().insertMemory(memory)
            ToolExecutionResult(
                toolName = "Long-Term Memory",
                summary = "Stored in neural memory: \"${fact.trim()}\"",
                isSuccess = true
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                toolName = "Long-Term Memory",
                summary = "Failed to store memory: ${e.localizedMessage}",
                isSuccess = false
            )
        }
    }

    suspend fun executeCreateReminder(title: String, delayMinutes: Int = 15, notes: String = ""): ToolExecutionResult {
        return try {
            val due = Calendar.getInstance().apply {
                add(Calendar.MINUTE, delayMinutes.coerceAtLeast(1))
            }.timeInMillis

            val entity = ReminderEntity(
                title = title.trim(),
                notes = notes.ifBlank { "Scheduled by SolyNext Assistant" },
                dueTimestamp = due,
                isCompleted = false,
                isNotified = false
            )

            val id = appDatabase.reminderDao().insertReminder(entity)
            val scheduledEntity = entity.copy(id = id)
            reminderScheduler.schedule(scheduledEntity)

            val timeDisplay = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(due)
            ToolExecutionResult(
                toolName = "Reminder",
                summary = "Reminder set: \"$title\" in $delayMinutes minutes (at $timeDisplay).",
                detailedData = "ID: $id | Scheduled for: $timeDisplay",
                isSuccess = true
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                toolName = "Reminder",
                summary = "Failed to schedule reminder: ${e.localizedMessage}",
                isSuccess = false
            )
        }
    }

    /**
     * Checks if a user prompt directly matches an explicit tool intent.
     */
    fun detectDirectToolIntent(prompt: String): ToolIntent? {
        val lower = prompt.trim().lowercase()

        // Weather intent
        if (lower.startsWith("weather in ") || lower.startsWith("what's the weather in ") || lower.startsWith("whats the weather in ") || lower.startsWith("how is the weather in ")) {
            val city = prompt.replace(Regex("(?i)^(what's the weather in|whats the weather in|weather in|how is the weather in)\\s+"), "").trim('?', '.', ' ')
            if (city.isNotBlank()) return ToolIntent.Weather(city)
        }

        // Web search intent
        if (lower.startsWith("search ") || lower.startsWith("search web for ") || lower.startsWith("google ") || lower.startsWith("look up ")) {
            val query = prompt.replace(Regex("(?i)^(search web for|search the web for|search for|search|google|look up)\\s+"), "").trim('?', '.', ' ')
            if (query.isNotBlank()) return ToolIntent.Search(query)
        }

        // Remember intent
        if (lower.startsWith("remember that ") || lower.startsWith("remember: ") || lower.startsWith("note that ")) {
            val fact = prompt.replace(Regex("(?i)^(remember that|remember:|remember|note that)\\s+"), "").trim('?', '.', ' ')
            if (fact.isNotBlank()) return ToolIntent.Memory(fact)
        }

        // Remind intent
        if (lower.startsWith("remind me to ") || lower.startsWith("set a reminder to ") || lower.startsWith("remind me in ")) {
            val content = prompt.replace(Regex("(?i)^(remind me to|set a reminder to|remind me in|remind me)\\s+"), "").trim()
            return parseReminderPrompt(content)
        }

        return null
    }

    private fun parseReminderPrompt(content: String): ToolIntent.Reminder {
        val minRegex = Regex("(?i)in\\s+(\\d+)\\s+minute")
        val match = minRegex.find(content)
        val minutes = match?.groupValues?.get(1)?.toIntOrNull() ?: 10
        val title = content.replace(Regex("(?i)in\\s+\\d+\\s+minutes?"), "").trim()
        return ToolIntent.Reminder(title = title.ifBlank { "Scheduled Reminder" }, delayMinutes = minutes)
    }

    sealed class ToolIntent {
        data class Weather(val location: String) : ToolIntent()
        data class Search(val query: String) : ToolIntent()
        data class Memory(val fact: String) : ToolIntent()
        data class Reminder(val title: String, val delayMinutes: Int) : ToolIntent()
    }
}
