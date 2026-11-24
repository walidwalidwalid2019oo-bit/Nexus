package com.nexus.app

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Replaceable speech-output contract for PICKEL.
 *
 * The UI depends on this interface rather than directly on Android TTS,
 * so a future remote voice provider can be added without redesigning the orb.
 */
interface PickelVoiceOutput {
    fun speak(text: String)
    fun stop()
    fun shutdown()
}

/** Local, offline-capable fallback using the device's installed TTS engine. */
class AndroidPickelVoiceOutput(
    context: Context,
    private val onReady: (Boolean) -> Unit
) : PickelVoiceOutput {
    private var ready = false
    private var engine: TextToSpeech? = null

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                engine?.setLanguage(Locale.US)
                engine?.setPitch(0.80f)
                engine?.setSpeechRate(0.90f)
            }
            onReady(ready)
        }
    }

    override fun speak(text: String) {
        if (!ready || text.isBlank()) return
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), "pickel_reply")
    }

    override fun stop() {
        engine?.stop()
    }

    override fun shutdown() {
        ready = false
        engine?.stop()
        engine?.shutdown()
        engine = null
    }
}
