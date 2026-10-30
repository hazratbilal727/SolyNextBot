package com.example.data.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceSpeechManager(
    private val context: Context,
    private val onSpeechRecognized: (String) -> Unit,
    private val onError: (String) -> Unit
) : RecognitionListener, TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _audioDecibels = MutableStateFlow(0f)
    val audioDecibels: StateFlow<Float> = _audioDecibels.asStateFlow()

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    var speechRate: Float = 1.05f
        set(value) {
            field = value
            textToSpeech?.setSpeechRate(value)
        }

    var speechPitch: Float = 1.0f
        set(value) {
            field = value
            textToSpeech?.setPitch(value)
        }

    init {
        initSpeechRecognizer()
        textToSpeech = TextToSpeech(context, this)
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@VoiceSpeechManager)
            }
        }
    }

    fun startListening() {
        stopSpeaking()
        if (speechRecognizer == null) {
            initSpeechRecognizer()
        }

        if (speechRecognizer == null) {
            onError("Speech recognition service is not available on this device.")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            _liveTranscript.value = ""
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            _isListening.value = false
            onError("Unable to activate microphone: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _isListening.value = false
        _audioDecibels.value = 0f
    }

    fun cancelListening() {
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
        _isListening.value = false
        _audioDecibels.value = 0f
        _liveTranscript.value = ""
    }

    fun speak(text: String, utteranceId: String = "solynext_response") {
        if (!_isTtsReady.value) return
        stopListening()
        stopSpeaking()

        val cleanText = sanitizeForSpeech(text)
        if (cleanText.isBlank()) return

        textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {}
        _isSpeaking.value = false
    }

    private fun sanitizeForSpeech(text: String): String {
        return text
            .replace(Regex("\\[(WEATHER|SEARCH|REMIND|REMEMBER):[^\\]]+\\]"), "")
            .replace(Regex("`{1,3}[^`]*`{1,3}"), "")
            .replace(Regex("[*#_>~]"), "")
            .replace(Regex("https?://\\S+"), "web link")
            .trim()
    }

    // --- RecognitionListener Callbacks ---

    override fun onReadyForSpeech(params: Bundle?) {
        _isListening.value = true
    }

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {
        // Normalize dB range roughly from -2 to 10
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        _audioDecibels.value = normalized
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _isListening.value = false
        _audioDecibels.value = 0f
    }

    override fun onError(error: Int) {
        _isListening.value = false
        _audioDecibels.value = 0f
        val msg = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Please speak clearly."
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network interruption during speech recognition."
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timed out."
            else -> "Speech recognition error code: $error"
        }
        if (error != SpeechRecognizer.ERROR_NO_MATCH) {
            onError(msg)
        }
    }

    override fun onResults(results: Bundle?) {
        _isListening.value = false
        _audioDecibels.value = 0f
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val spoken = matches?.firstOrNull()?.trim().orEmpty()
        if (spoken.isNotBlank()) {
            _liveTranscript.value = spoken
            onSpeechRecognized(spoken)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partial = matches?.firstOrNull()?.trim().orEmpty()
        if (partial.isNotBlank()) {
            _liveTranscript.value = partial
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    // --- TextToSpeech.OnInitListener ---

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.getDefault()
            textToSpeech?.setSpeechRate(speechRate)
            textToSpeech?.setPitch(speechPitch)
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                }
            })
            _isTtsReady.value = true
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Exception) {}
    }
}
