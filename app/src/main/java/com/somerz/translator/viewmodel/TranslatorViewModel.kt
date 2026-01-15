package com.somerz.translator.viewmodel

import android.util.Base64
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.LiveContentResponse
import com.google.firebase.ai.type.LiveGenerationConfig
import com.google.firebase.ai.type.MediaData
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Language pair for translation
 */
data class LanguagePair(
    val sourceLanguage: String,
    val targetLanguage: String,
    val sourceName: String,
    val targetName: String
)

/**
 * Available translation modes
 */
enum class TranslationMode {
    KOREAN_TO_ENGLISH,
    ENGLISH_TO_KOREAN,
    KOREAN_TO_JAPANESE,
    JAPANESE_TO_KOREAN,
    KOREAN_TO_CHINESE,
    CHINESE_TO_KOREAN
}

/**
 * Translation state
 */
sealed class TranslationState {
    data object Idle : TranslationState()
    data object Connecting : TranslationState()
    data object Active : TranslationState()
    data class Error(val message: String) : TranslationState()
}

/**
 * ViewModel for real-time translation using Gemini Live API
 */
@OptIn(PublicPreviewAPI::class)
class TranslatorViewModel : ViewModel() {

    private val generativeModel = Firebase.ai.liveModel(
        modelName = "gemini-2.0-flash-live-preview-04-09",
        generationConfig = LiveGenerationConfig(
            responseModality = ResponseModality.AUDIO,
            speechConfig = SpeechConfig(voiceName = "Kore") // Natural voice
        )
    )

    private var session: com.google.firebase.ai.type.LiveSession? = null

    // UI State
    var translationState by mutableStateOf<TranslationState>(TranslationState.Idle)
        private set

    var currentMode by mutableStateOf(TranslationMode.KOREAN_TO_ENGLISH)
        private set

    private val _transcription = MutableStateFlow("")
    val transcription: StateFlow<String> = _transcription.asStateFlow()

    private val _audioOutput = MutableStateFlow<ByteArray?>(null)
    val audioOutput: StateFlow<ByteArray?> = _audioOutput.asStateFlow()

    /**
     * Get language pair based on current mode
     */
    fun getLanguagePair(): LanguagePair {
        return when (currentMode) {
            TranslationMode.KOREAN_TO_ENGLISH -> LanguagePair("ko", "en", "Korean", "English")
            TranslationMode.ENGLISH_TO_KOREAN -> LanguagePair("en", "ko", "English", "Korean")
            TranslationMode.KOREAN_TO_JAPANESE -> LanguagePair("ko", "ja", "Korean", "Japanese")
            TranslationMode.JAPANESE_TO_KOREAN -> LanguagePair("ja", "ko", "Japanese", "Korean")
            TranslationMode.KOREAN_TO_CHINESE -> LanguagePair("ko", "zh", "Korean", "Chinese")
            TranslationMode.CHINESE_TO_KOREAN -> LanguagePair("zh", "ko", "Chinese", "Korean")
        }
    }

    /**
     * Set translation mode
     */
    fun setTranslationMode(mode: TranslationMode) {
        currentMode = mode
    }

    /**
     * Build translation system prompt
     */
    private fun buildSystemPrompt(): String {
        val pair = getLanguagePair()
        return """
            You are a real-time interpreter. Your task is to translate spoken ${pair.sourceName} to ${pair.targetName}.

            Instructions:
            1. Listen to the audio input in ${pair.sourceName}
            2. Translate it naturally and accurately to ${pair.targetName}
            3. Speak the translation clearly and naturally
            4. Maintain the tone and emotion of the original speech
            5. If you're unsure about something, make your best interpretation
            6. Do not add explanations - only speak the translation

            Important: Respond ONLY with the translated speech. No additional commentary.
        """.trimIndent()
    }

    /**
     * Start translation session
     */
    fun startTranslation() {
        viewModelScope.launch {
            try {
                translationState = TranslationState.Connecting
                _transcription.value = ""
                _audioOutput.value = null

                session = generativeModel.connect()

                // Send system prompt
                session?.send(buildSystemPrompt())

                translationState = TranslationState.Active

                // Listen for responses
                session?.receive()?.collect { response ->
                    handleResponse(response)
                }

            } catch (e: Exception) {
                translationState = TranslationState.Error(e.message ?: "Connection failed")
            }
        }
    }

    /**
     * Handle response from Gemini Live API
     */
    private fun handleResponse(response: LiveContentResponse) {
        response.data?.forEach { part ->
            // Handle audio response
            if (part is MediaData) {
                val audioBytes = Base64.decode(part.data, Base64.DEFAULT)
                _audioOutput.value = audioBytes
            }
        }

        // Handle text transcription if available
        response.text?.let { text ->
            _transcription.value = _transcription.value + text
        }
    }

    /**
     * Send audio data for translation
     */
    fun sendAudioData(audioData: ByteArray) {
        if (translationState != TranslationState.Active || session == null) return

        runBlocking {
            try {
                val base64Audio = Base64.encodeToString(audioData, Base64.NO_WRAP)
                session?.send(
                    content {
                        blob("audio/pcm;rate=16000", base64Audio.toByteArray())
                    }
                )
            } catch (e: Exception) {
                // Handle send error silently
            }
        }
    }

    /**
     * End translation session
     */
    fun endTranslation() {
        viewModelScope.launch {
            try {
                session?.close()
                session = null
                translationState = TranslationState.Idle
            } catch (e: Exception) {
                translationState = TranslationState.Error(e.message ?: "Failed to end session")
            }
        }
    }

    /**
     * Swap languages
     */
    fun swapLanguages() {
        currentMode = when (currentMode) {
            TranslationMode.KOREAN_TO_ENGLISH -> TranslationMode.ENGLISH_TO_KOREAN
            TranslationMode.ENGLISH_TO_KOREAN -> TranslationMode.KOREAN_TO_ENGLISH
            TranslationMode.KOREAN_TO_JAPANESE -> TranslationMode.JAPANESE_TO_KOREAN
            TranslationMode.JAPANESE_TO_KOREAN -> TranslationMode.KOREAN_TO_JAPANESE
            TranslationMode.KOREAN_TO_CHINESE -> TranslationMode.CHINESE_TO_KOREAN
            TranslationMode.CHINESE_TO_KOREAN -> TranslationMode.KOREAN_TO_CHINESE
        }
    }

    override fun onCleared() {
        super.onCleared()
        runBlocking {
            session?.close()
        }
    }
}
