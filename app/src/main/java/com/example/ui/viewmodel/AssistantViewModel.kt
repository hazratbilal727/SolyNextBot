package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AiProviderType
import com.example.data.model.AssistantState
import com.example.data.model.ConversationEntity
import com.example.data.model.MemoryEntity
import com.example.data.model.MessageEntity
import com.example.data.model.PersonalityStyle
import com.example.data.model.ReminderEntity
import com.example.data.prefs.UserPreferencesRepository
import com.example.data.provider.AiProviderManager
import com.example.data.reminder.ReminderScheduler
import com.example.data.tools.SearchTool
import com.example.data.tools.ToolDispatcher
import com.example.data.tools.WeatherTool
import com.example.data.voice.VoiceSpeechManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val preferencesRepository = UserPreferencesRepository(application)
    private val reminderScheduler = ReminderScheduler(application)
    private val weatherTool = WeatherTool()
    private val searchTool = SearchTool()
    val toolDispatcher = ToolDispatcher(weatherTool, searchTool, database, reminderScheduler)
    val providerManager = AiProviderManager(preferencesRepository)

    // Voice Manager
    val voiceManager = VoiceSpeechManager(
        context = application,
        onSpeechRecognized = { spokenText ->
            handleUserMessage(spokenText)
        },
        onError = { errorMsg ->
            _errorMessage.value = errorMsg
            _assistantState.value = AssistantState.ERROR
        }
    )

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val currentMessages: StateFlow<List<MessageEntity>> = _currentMessages.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _providerConnectionStatus = MutableStateFlow<String?>(null)
    val providerConnectionStatus: StateFlow<String?> = _providerConnectionStatus.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    // Database flows
    val allConversations = database.conversationDao().getAllConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMemories = database.memoryDao().getAllMemoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReminders = database.reminderDao().getAllRemindersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Observe speech/listening states to synchronize assistantState
        viewModelScope.launch {
            voiceManager.isListening.collect { listening ->
                if (listening) {
                    _assistantState.value = AssistantState.LISTENING
                } else if (_assistantState.value == AssistantState.LISTENING) {
                    _assistantState.value = AssistantState.IDLE
                }
            }
        }
        viewModelScope.launch {
            voiceManager.isSpeaking.collect { speaking ->
                if (speaking) {
                    _assistantState.value = AssistantState.SPEAKING
                } else if (_assistantState.value == AssistantState.SPEAKING) {
                    _assistantState.value = AssistantState.IDLE
                }
            }
        }
        viewModelScope.launch {
            preferencesRepository.speechRateFlow.collect { rate ->
                voiceManager.speechRate = rate
            }
        }
        viewModelScope.launch {
            preferencesRepository.speechPitchFlow.collect { pitch ->
                voiceManager.speechPitch = pitch
            }
        }

        // Initialize default or recent conversation
        viewModelScope.launch {
            val conversations = database.conversationDao().getAllConversations().first()
            if (conversations.isNotEmpty()) {
                selectConversation(conversations.first().id)
            } else {
                createNewConversation()
            }
        }
    }

    fun selectConversation(id: String) {
        _currentConversationId.value = id
        viewModelScope.launch {
            database.conversationDao().getMessagesForConversation(id).collect { messages ->
                _currentMessages.value = messages
            }
        }
    }

    fun createNewConversation(title: String = "Conversation with SolyNext") {
        viewModelScope.launch {
            val conv = ConversationEntity(
                id = UUID.randomUUID().toString(),
                title = title
            )
            database.conversationDao().insertConversation(conv)
            // Welcome greeting
            val welcome = MessageEntity(
                conversationId = conv.id,
                sender = "assistant",
                text = "Greetings. I am SolyNext, your personal AI voice assistant. All neural systems are online. How may I assist you today?"
            )
            database.conversationDao().insertMessage(welcome)
            selectConversation(conv.id)
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            database.conversationDao().deleteConversation(id)
            val remaining = database.conversationDao().getAllConversations().first()
            if (remaining.isNotEmpty()) {
                selectConversation(remaining.first().id)
            } else {
                createNewConversation()
            }
        }
    }

    fun handleUserMessage(text: String) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return

        val convId = _currentConversationId.value ?: return

        viewModelScope.launch {
            // 1. Record user message
            val userMsg = MessageEntity(
                conversationId = convId,
                sender = "user",
                text = cleanText
            )
            database.conversationDao().insertMessage(userMsg)

            _assistantState.value = AssistantState.THINKING
            _errorMessage.value = null

            // 2. Check direct tool intent
            val toolIntent = toolDispatcher.detectDirectToolIntent(cleanText)
            if (toolIntent != null) {
                handleDirectTool(convId, toolIntent)
                return@launch
            }

            // 3. Conversational AI Pipeline
            try {
                val provider = providerManager.getActiveProvider()
                val systemPrompt = buildSystemPrompt()
                val messages = database.conversationDao().getMessagesList(convId)

                val result = provider.generateResponse(messages, systemPrompt)
                result.onSuccess { responseText ->
                    // Check if AI included a tool call tag in its text
                    val processedText = inspectAndExecuteAiToolDirectives(convId, responseText)

                    val assistantMsg = MessageEntity(
                        conversationId = convId,
                        sender = "assistant",
                        text = processedText
                    )
                    database.conversationDao().insertMessage(assistantMsg)

                    // Auto speak if enabled
                    val autoSpeak = preferencesRepository.autoSpeakFlow.first()
                    if (autoSpeak) {
                        voiceManager.speak(processedText)
                    } else {
                        _assistantState.value = AssistantState.IDLE
                    }
                }.onFailure { ex ->
                    _errorMessage.value = ex.localizedMessage ?: "Unknown AI response failure"
                    _assistantState.value = AssistantState.ERROR

                    val errorMsg = MessageEntity(
                        conversationId = convId,
                        sender = "assistant",
                        text = "I encountered an error connecting to ${provider.providerName}: ${ex.localizedMessage ?: "Check your API configuration in Settings."}"
                    )
                    database.conversationDao().insertMessage(errorMsg)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
                _assistantState.value = AssistantState.ERROR
            }
        }
    }

    private suspend fun handleDirectTool(convId: String, intent: ToolDispatcher.ToolIntent) {
        val result = when (intent) {
            is ToolDispatcher.ToolIntent.Weather -> toolDispatcher.executeWeather(intent.location)
            is ToolDispatcher.ToolIntent.Search -> toolDispatcher.executeSearch(intent.query)
            is ToolDispatcher.ToolIntent.Memory -> toolDispatcher.executeSaveMemory(intent.fact)
            is ToolDispatcher.ToolIntent.Reminder -> toolDispatcher.executeCreateReminder(intent.title, intent.delayMinutes)
        }

        val toolMsg = MessageEntity(
            conversationId = convId,
            sender = "assistant",
            text = result.summary,
            toolType = result.toolName,
            isToolExecution = true
        )
        database.conversationDao().insertMessage(toolMsg)

        val autoSpeak = preferencesRepository.autoSpeakFlow.first()
        if (autoSpeak) {
            voiceManager.speak(result.summary)
        } else {
            _assistantState.value = AssistantState.IDLE
        }
    }

    private suspend fun inspectAndExecuteAiToolDirectives(convId: String, text: String): String {
        var output = text

        // Weather directive: [WEATHER: city]
        val weatherMatch = Regex("\\[WEATHER:\\s*([^\\]]+)\\]").find(text)
        if (weatherMatch != null) {
            val city = weatherMatch.groupValues[1].trim()
            val res = toolDispatcher.executeWeather(city)
            output = output.replace(weatherMatch.value, "\n${res.summary}\n")
        }

        // Search directive: [SEARCH: query]
        val searchMatch = Regex("\\[SEARCH:\\s*([^\\]]+)\\]").find(text)
        if (searchMatch != null) {
            val query = searchMatch.groupValues[1].trim()
            val res = toolDispatcher.executeSearch(query)
            output = output.replace(searchMatch.value, "\n${res.summary}\n")
        }

        // Remember directive: [REMEMBER: fact]
        val memoryMatch = Regex("\\[REMEMBER:\\s*([^\\]]+)\\]").find(text)
        if (memoryMatch != null) {
            val fact = memoryMatch.groupValues[1].trim()
            toolDispatcher.executeSaveMemory(fact)
            output = output.replace(memoryMatch.value, "")
        }

        return output.trim()
    }

    private suspend fun buildSystemPrompt(): String {
        val personality = preferencesRepository.personalityFlow.first()
        val userName = preferencesRepository.userNameFlow.first()
        val memoryEnabled = preferencesRepository.memoryEnabledFlow.first()

        val sb = StringBuilder()
        sb.append(personality.promptInstruction).append("\n")
        sb.append("The user's preferred address is: '$userName'.\n")
        val now = SimpleDateFormat("EEEE, MMMM d, yyyy - hh:mm a", Locale.getDefault()).format(Date())
        sb.append("Current time and date: $now.\n\n")

        if (memoryEnabled) {
            val approvedMemories = database.memoryDao().getApprovedMemories()
            if (approvedMemories.isNotEmpty()) {
                sb.append("--- LONG-TERM PERSONAL MEMORY ---\n")
                sb.append("You remember the following user-approved facts and preferences:\n")
                approvedMemories.forEach { mem ->
                    sb.append("• [${mem.category.uppercase()}]: ${mem.fact}\n")
                }
                sb.append("Seamlessly incorporate these remembered facts when relevant.\n\n")
            }
        }

        sb.append("--- CAPABILITIES AND TOOLS ---\n")
        sb.append("You have real integrated tools. If a user asks for weather or a web search, or asks you to remember something, you can trigger actions using tags:\n")
        sb.append("• [WEATHER: CityName] - fetch live meteorological telemetry\n")
        sb.append("• [SEARCH: Query] - execute live web lookup and citations\n")
        sb.append("• [REMEMBER: Fact] - permanently save a user fact into memory\n")
        sb.append("Keep your spoken responses natural, concise, and helpful.")

        return sb.toString()
    }

    fun toggleVoiceListening() {
        if (voiceManager.isSpeaking.value) {
            voiceManager.stopSpeaking()
            _assistantState.value = AssistantState.IDLE
            return
        }

        if (voiceManager.isListening.value) {
            voiceManager.stopListening()
            _assistantState.value = AssistantState.IDLE
        } else {
            voiceManager.startListening()
        }
    }

    fun speakText(text: String) {
        voiceManager.speak(text)
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun testProviderConnection(type: AiProviderType) {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _providerConnectionStatus.value = "Validating ${type.displayName}..."
            try {
                val provider = providerManager.createProvider(type)
                val test = provider.validateConnection()
                test.onSuccess {
                    _providerConnectionStatus.value = "✓ Connected successfully to ${type.displayName} (${provider.currentModel})"
                }.onFailure { err ->
                    _providerConnectionStatus.value = "✗ Connection failed: ${err.localizedMessage ?: "Invalid key or endpoint"}"
                }
            } catch (e: Exception) {
                _providerConnectionStatus.value = "✗ Error: ${e.localizedMessage}"
            } finally {
                _isTestingConnection.value = false
            }
        }
    }

    // Memory operations
    fun addManualMemory(fact: String, category: String = "personal") {
        viewModelScope.launch {
            toolDispatcher.executeSaveMemory(fact, category)
        }
    }

    fun deleteMemory(memory: MemoryEntity) {
        viewModelScope.launch {
            database.memoryDao().deleteMemory(memory)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            database.memoryDao().clearAllMemories()
        }
    }

    // Reminders operations
    fun addManualReminder(title: String, dueTimestamp: Long, notes: String = "") {
        viewModelScope.launch {
            val entity = ReminderEntity(
                title = title.trim(),
                notes = notes,
                dueTimestamp = dueTimestamp,
                isCompleted = false,
                isNotified = false
            )
            val id = database.reminderDao().insertReminder(entity)
            reminderScheduler.schedule(entity.copy(id = id))
        }
    }

    fun toggleReminderCompleted(reminder: ReminderEntity) {
        viewModelScope.launch {
            val updated = !reminder.isCompleted
            database.reminderDao().setCompleted(reminder.id, updated)
            if (updated) {
                reminderScheduler.cancel(reminder.id)
            }
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderScheduler.cancel(reminder.id)
            database.reminderDao().deleteReminder(reminder)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
