package com.example.promptpilot.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class VoiceInputManager(private val context: Context) {
    private var tts: TextToSpeech? = null
    
    fun initializeTTS(onInitComplete: () -> Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                onInitComplete()
            } else {
                Log.e("VoiceInputManager", "TTS initialization failed")
            }
        }
    }
    
    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    
    fun stop() {
        tts?.stop()
    }
    
    fun shutdown() {
        tts?.shutdown()
    }
}