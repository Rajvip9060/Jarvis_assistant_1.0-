package com.jervis.jarvis_assistant.logic

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import com.jervis.jarvis_assistant.data.api.ChatRequest
import com.jervis.jarvis_assistant.data.api.Message
import com.jervis.jarvis_assistant.data.api.OpenRouterApi
import kotlinx.coroutines.*
import java.util.Locale

class VoiceAssistantManager(
    private val context: Context,
    private val api: OpenRouterApi,
    private val apiKey: String,
    private val localHandler: LocalCommandHandler,
    private val onUIUpdate: (String) -> Unit
) : RecognitionListener {

    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private var tts: TextToSpeech? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        speechRecognizer.setRecognitionListener(this)
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                configureJarvisVoice()
            }
        }
    }

    private fun configureJarvisVoice() {
        tts?.apply {
            language = Locale.UK
            setPitch(0.7f) // Deep Voice
            setSpeechRate(0.9f) // Steady pace
        }
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.UK)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        speechRecognizer.startListening(intent)
        onUIUpdate("Listening, Sir...")
    }

    override fun onResults(results: Bundle?) {
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0) ?: ""
        onUIUpdate("You: $text")
        processCommand(text)
    }

    private fun processCommand(input: String) {
        val cmd = input.lowercase()
        
        when {
            cmd.contains("torch") || cmd.contains("flashlight") -> {
                val state = !cmd.contains("off")
                localHandler.toggleFlashlight(state)
                speak("Right away, Sir. Flashlight ${if (state) "on" else "off"}.")
            }
            cmd.contains("wifi") -> {
                localHandler.openWifiSettings()
                speak("Accessing WiFi settings for you, Sir.")
            }
            else -> {
                // If no local command, send to OpenRouter AI
                askJarvisAI(input)
            }
        }
    }

    private fun askJarvisAI(query: String) {
        if (apiKey.isEmpty()) {
            speak("Sir, please update your API key in settings.")
            return
        }

        scope.launch {
            try {
                onUIUpdate("Jarvis is thinking...")
                val response = withContext(Dispatchers.IO) {
                    api.getChatCompletion(
                        token = "Bearer $apiKey",
                        request = ChatRequest(
                            model = "mistralai/mistral-7b-instruct",
                            messages = listOf(
                                Message("system", "You are JARVIS, a sophisticated British AI. Address user as Sir. Be brief."),
                                Message("user", query)
                            )
                        )
                    )
                }
                val reply = response.choices[0].message.content
                onUIUpdate("Jarvis: $reply")
                speak(reply)
            } catch (e: Exception) {
                onUIUpdate("Connection error, Sir.")
                speak("I am having trouble connecting to the network, Sir.")
            }
        }
    }

    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    // Unused Listeners
    override fun onReadyForSpeech(p0: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(p0: Float) {}
    override fun onBufferReceived(p0: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onError(p0: Int) { onUIUpdate("System standby, Sir.") }
    override fun onPartialResults(p0: Bundle?) {}
    override fun onEvent(p0: Int, p1: Bundle?) {}
}