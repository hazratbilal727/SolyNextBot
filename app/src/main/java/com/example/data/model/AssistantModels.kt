package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class AssistantState(val label: String) {
    IDLE("SolyNext Ready"),
    LISTENING("Listening to voice..."),
    THINKING("Neural processing..."),
    SPEAKING("Vocalizing response..."),
    ERROR("System notification")
}

enum class AiProviderType(val displayName: String, val defaultModel: String, val endpointNote: String) {
    GEMINI("Google Gemini", "gemini-2.0-flash", "Universal Google Gemini API (2.5, 2.0, 1.5, Pro & Flash)"),
    OPENAI("OpenAI GPT", "gpt-4o-mini", "Official OpenAI API v1/chat/completions"),
    GROK("xAI Grok", "grok-beta", "Official xAI API v1/chat/completions")
}

enum class PersonalityStyle(val displayName: String, val promptInstruction: String) {
    JARVIS(
        "Jarvis (Sophisticated & Witty)",
        "You are SolyNext, an advanced artificial intelligence inspired by J.A.R.V.I.S. You speak with refined intellect, subtle polite wit, absolute competence, and executive loyalty. Keep answers natural, elegant, and concise. Address the user respectfully."
    ),
    PROFESSIONAL(
        "Executive (Direct & Analytical)",
        "You are SolyNext, a high-efficiency executive AI assistant. Deliver factual, logically structured, crisp answers. Avoid unnecessary fluff and focus on actionable insights."
    ),
    CYBERPUNK(
        "Netrunner (Futuristic Tech)",
        "You are SolyNext, a next-gen cybernetic AI console. You have a sleek futuristic sci-fi attitude, highly knowledgeable in tech, computing, and digital systems."
    ),
    CASUAL(
        "Companion (Warm & Friendly)",
        "You are SolyNext, a warm, conversational, friendly AI assistant. You speak casually, naturally, and warmly like a knowledgeable best friend."
    )
}

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId")]
)
data class MessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val sender: String, // "user", "assistant", "system", "tool"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolType: String? = null,
    val isToolExecution: Boolean = false
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val category: String, // "personal", "preference", "project", "instruction"
    val fact: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isApproved: Boolean = true
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val notes: String = "",
    val dueTimestamp: Long,
    val isCompleted: Boolean = false,
    val isNotified: Boolean = false
)

data class ToolExecutionResult(
    val toolName: String,
    val summary: String,
    val detailedData: String? = null,
    val sourceUrl: String? = null,
    val isSuccess: Boolean = true
)
