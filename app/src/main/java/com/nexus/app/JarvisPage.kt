package com.nexus.app

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun JarvisPage(
    modelConfigured: Boolean,
    onAsk: (String, (String?, String?) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("Good evening, Mr. VOIDEX. NEXUS is ready. How may I assist you?") }
    var status by remember { mutableStateOf("VOICE INTERFACE • STANDBY") }
    var listening by remember { mutableStateOf(false) }
    var thinking by remember { mutableStateOf(false) }
    var voiceEnabled by remember { mutableStateOf(true) }
    var speechReady by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { result ->
            speechReady = result == TextToSpeech.SUCCESS
            if (result == TextToSpeech.SUCCESS) {
                engineSafeLanguage(tts)
            }
        }
        tts = engine
        onDispose {
            try { engine.stop(); engine.shutdown() } catch (_: Exception) { }
            tts = null
        }
    }

    fun speak(text: String) {
        if (voiceEnabled && speechReady) {
            tts?.setLanguage(Locale.US)
            tts?.setPitch(0.78f)
            tts?.setSpeechRate(0.91f)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nexus_jarvis_reply")
        }
    }

    fun submitPrompt(raw: String) {
        val prompt = raw.trim()
        if (prompt.isEmpty() || thinking) return
        input = prompt
        thinking = true
        status = "NEXUS IS THINKING"
        if (!modelConfigured) {
            answer = "Your AI connection is not configured yet. Open Settings and add your secure HTTPS provider details and NEXUS AI model."
            status = "AI CONNECTION REQUIRED"
            thinking = false
            speak(answer)
            return
        }
        onAsk(prompt) { response, error ->
            thinking = false
            if (response != null) {
                answer = response
                status = "RESPONSE READY"
                speak(response)
            } else {
                answer = error ?: "I couldn't complete that request. Please try again."
                status = "CONNECTION ISSUE"
                speak(answer)
            }
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        listening = false
        val spoken = if (result.resultCode == Activity.RESULT_OK)
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        else null
        if (!spoken.isNullOrBlank()) submitPrompt(spoken)
        else status = "VOICE INPUT CANCELLED"
    }

    val pulse = rememberInfiniteTransition(label = "jarvisPulse")
    val orbScale by pulse.animateFloat(
        initialValue = 0.96f, targetValue = if (listening || thinking) 1.08f else 1.015f,
        animationSpec = infiniteRepeatable(tween(1250, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orbScale"
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ NEXUS", color = Purple) }
            Spacer(Modifier.weight(1f))
            Text("PERSONAL ASSISTANT", color = Muted, fontSize = 9.sp, letterSpacing = 1.2.sp)
        }
        Text("N E X U S", color = MainText, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 5.sp)
        Text("JARVIS MODE", color = Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Box(Modifier.padding(vertical = 8.dp).size(190.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(184.dp).scale(orbScale).background(Color(0x223F1B59), CircleShape))
            Box(Modifier.size(145.dp).background(Color(0xFF241333), CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(112.dp).background(Color(0xFF3B2052), CircleShape), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(76.dp).background(Color(0xFF9A63C7), CircleShape), contentAlignment = Alignment.Center) {
                        Text("N", color = Color.White, fontSize = 35.sp, fontWeight = FontWeight.Light)
                    }
                }
            }
        }
        Text(status, color = if (listening || thinking) Color(0xFFD7B8F2) else Muted, fontSize = 9.sp, letterSpacing = 1.6.sp)
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DeepPurple)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("ASSISTANT", color = Purple, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text(answer, color = MainText, fontSize = 14.sp, lineHeight = 21.sp)
                if (thinking) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Purple, trackColor = DeepPurple)
            }
        }
        OutlinedTextField(
            value = input, onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ask NEXUS anything...", color = Muted) },
            minLines = 2,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Purple, unfocusedBorderColor = DeepPurple,
                focusedTextColor = MainText, unfocusedTextColor = MainText,
                cursorColor = Purple
            )
        )
        Button(
            onClick = { submitPrompt(input) },
            enabled = input.isNotBlank() && !thinking,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)
        ) { Text(if (thinking) "THINKING..." else "ASK NEXUS") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            OutlinedButton(
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to NEXUS")
                    }
                    try {
                        listening = true
                        status = "LISTENING • SPEAK NOW"
                        voiceLauncher.launch(intent)
                    } catch (_: Exception) {
                        listening = false
                        status = "VOICE RECOGNITION UNAVAILABLE"
                        answer = "This device doesn't currently provide a compatible speech recognition service."
                    }
                },
                enabled = !thinking,
                modifier = Modifier.weight(1f)
            ) { Text(if (listening) "LISTENING..." else "🎙 SPEAK", color = Purple, fontSize = 11.sp) }
            OutlinedButton(
                onClick = {
                    voiceEnabled = !voiceEnabled
                    if (!voiceEnabled) tts?.stop()
                },
                modifier = Modifier.weight(1f)
            ) { Text(if (voiceEnabled) "VOICE ON" else "VOICE OFF", color = Purple, fontSize = 11.sp) }
        }
        Text(
            "Voice input uses Android's speech service. Spoken replies use your installed text-to-speech voice. Device actions are not enabled in this first JARVIS build.",
            color = Muted, fontSize = 10.sp, lineHeight = 15.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
    }
}

private fun engineSafeLanguage(engine: TextToSpeech?) {
    try { engine?.setLanguage(Locale.US) } catch (_: Exception) { }
}
