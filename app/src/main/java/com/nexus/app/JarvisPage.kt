package com.nexus.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private data class JarvisMessage(val fromUser: Boolean, val text: String)

@Composable
fun JarvisPage(
    modelConfigured: Boolean,
    onAsk: (String, (String?, String?) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val conversation = remember {
        mutableStateListOf(
            JarvisMessage(false, "Hello, Mr. VOIDEX. It is a pleasure to hear from you. How may I assist you today?")
        )
    }
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("VOICE INTERFACE • STANDBY") }
    var listening by remember { mutableStateOf(false) }
    var thinking by remember { mutableStateOf(false) }
    var voiceEnabled by remember { mutableStateOf(true) }
    var speechReady by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        val engine = TextToSpeech(context) { result ->
            speechReady = result == TextToSpeech.SUCCESS
        }
        tts = engine
        onDispose {
            try { engine.stop(); engine.shutdown() } catch (_: Exception) { }
            tts = null
            speechReady = false
        }
    }

    fun speak(text: String) {
        if (voiceEnabled && speechReady) {
            try {
                tts?.setLanguage(Locale.US)
                tts?.setPitch(0.80f)
                tts?.setSpeechRate(0.90f)
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), "nexus_jarvis_reply")
            } catch (_: Exception) {
                status = "TEXT-TO-SPEECH UNAVAILABLE"
            }
        }
    }

    fun submitPrompt(raw: String) {
        val question = raw.trim()
        if (question.isEmpty() || thinking) return
        input = ""
        conversation.add(JarvisMessage(true, question))
        thinking = true
        status = "PICKEL IS THINKING"

        if (!modelConfigured) {
            val message = "My AI connection is not configured yet. Please open Settings and add your provider connection and AI model."
            conversation.add(JarvisMessage(false, message))
            status = "AI CONNECTION REQUIRED"
            thinking = false
            speak(message)
            return
        }

        val history = conversation.takeLast(14).joinToString("\n") { message ->
            (if (message.fromUser) "Mr. VOIDEX: " else "PICKEL: ") + message.text
        }
        val prompt = """
            You are PICKEL, a highly capable, composed and courteous personal AI assistant created for the VOIDEX ecosystem.
            Address the user as Mr. VOIDEX. Your manner is polished, calm, intelligent and respectful.
            Answer general questions clearly. If information may be outdated or you are uncertain, say so.
            Never claim to have opened apps, sent messages, placed calls, changed settings, or performed phone actions.
            You can explain what is possible, but real device actions require a supported integration and the user's confirmation.
            Never ask the user to reveal passwords, API keys, or other authentication secrets.
            Keep replies concise enough to sound natural when spoken, unless the user asks for detail.

            Recent conversation:
            $history

            Respond to the latest message from Mr. VOIDEX.
        """.trimIndent()

        onAsk(prompt) { response, error ->
            thinking = false
            val reply = response?.takeIf { it.isNotBlank() }
                ?: "I could not complete that request. ${error ?: "Please check the AI connection in Settings and try again."}"
            conversation.add(JarvisMessage(false, reply))
            status = if (response.isNullOrBlank()) "CONNECTION ISSUE" else "RESPONSE READY"
            speak(reply)
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        listening = false
        val spoken = if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        } else null
        if (!spoken.isNullOrBlank()) {
            status = "VOICE INPUT RECEIVED"
            submitPrompt(spoken)
        } else {
            status = "VOICE INPUT CANCELLED"
        }
    }

    val pulse = rememberInfiniteTransition(label = "jarvisPulse")
    val orbScale by pulse.animateFloat(
        initialValue = 0.97f,
        targetValue = if (listening || thinking) 1.075f else 1.015f,
        animationSpec = infiniteRepeatable(
            tween(1250, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "orbScale"
    )

    LaunchedEffect(conversation.size, thinking) {
        if (conversation.isNotEmpty()) listState.animateScrollToItem(conversation.lastIndex)
    }

    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ NEXUS", color = Purple) }
            Spacer(Modifier.weight(1f))
            Text("PERSONAL ASSISTANT", color = Muted, fontSize = 9.sp, letterSpacing = 1.2.sp)
        }
        Text("P I C K E L", color = MainText, fontSize = 23.sp, fontWeight = FontWeight.Bold, letterSpacing = 5.sp)
        Text("PERSONAL AI", color = Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)

        Box(Modifier.padding(vertical = 2.dp).size(142.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(138.dp).scale(orbScale).background(Color(0x223F1B59), CircleShape))
            Box(Modifier.size(112.dp).background(Color(0xFF241333), CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(84.dp).background(Color(0xFF3B2052), CircleShape), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(58.dp).background(Color(0xFF9A63C7), CircleShape), contentAlignment = Alignment.Center) {
                        Text("N", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Light)
                    }
                }
            }
        }
        Text(
            status,
            color = if (listening || thinking) Color(0xFFD7B8F2) else Muted,
            fontSize = 9.sp,
            letterSpacing = 1.5.sp
        )

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("CONVERSATION", color = Muted, fontSize = 9.sp, letterSpacing = 1.4.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                voiceEnabled = !voiceEnabled
                if (!voiceEnabled) tts?.stop()
            }) {
                Text(if (voiceEnabled) "VOICE ON" else "VOICE OFF", color = Purple, fontSize = 9.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(conversation) { message ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        Modifier.fillMaxWidth(if (message.fromUser) 0.88f else 0.96f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (message.fromUser) DeepPurple else Panel
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = if (message.fromUser) null else BorderStroke(1.dp, DeepPurple)
                    ) {
                        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                if (message.fromUser) "MR. VOIDEX" else "NEXUS",
                                color = if (message.fromUser) Color(0xFFE4CCF6) else Purple,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(message.text, color = MainText, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }
            if (thinking) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(Modifier.size(14.dp), color = Purple, strokeWidth = 2.dp)
                        Text("PICKEL is preparing a response...", color = Muted, fontSize = 10.sp)
                    }
                }
            }
        }

        if (!modelConfigured) {
            Text(
                "Configure the AI model and provider connection in Settings to activate live answers.",
                color = Color(0xFFE2BD8C), fontSize = 9.sp, textAlign = TextAlign.Center
            )
        }

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(17.dp),
            border = BorderStroke(1.dp, DeepPurple)
        ) {
            Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("How may I assist you, Mr. VOIDEX?", color = Muted, fontSize = 11.sp) },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { submitPrompt(input) }),
                    enabled = !thinking,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Purple,
                        unfocusedBorderColor = DeepPurple,
                        focusedTextColor = MainText,
                        unfocusedTextColor = MainText,
                        cursorColor = Purple
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to PICKEL")
                            }
                            try {
                                listening = true
                                status = "LISTENING • SPEAK NOW"
                                voiceLauncher.launch(intent)
                            } catch (_: Exception) {
                                listening = false
                                status = "VOICE RECOGNITION UNAVAILABLE"
                            }
                        },
                        enabled = !thinking,
                        modifier = Modifier.weight(1f)
                    ) { Text(if (listening) "LISTENING..." else "🎙 SPEAK", color = Purple, fontSize = 10.sp) }
                    Button(
                        onClick = { submitPrompt(input) },
                        enabled = input.isNotBlank() && !thinking,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)
                    ) { Text(if (thinking) "THINKING..." else "SEND  ➤", fontSize = 10.sp) }
                }
                Text(
                    if (speechReady) "DEVICE SPEECH OUTPUT READY · ANDROID SPEECH RECOGNIZER INPUT"
                    else "INITIALIZING DEVICE VOICE · ANDROID SPEECH RECOGNIZER INPUT",
                    color = Muted, fontSize = 8.sp
                )
            }
        }
        Text(
            "Voice input uses Android's speech recognizer. Spoken replies use the installed text-to-speech engine. Phone actions are not enabled in this build.",
            color = Muted, fontSize = 9.sp, lineHeight = 13.sp, textAlign = TextAlign.Center
        )
    }
}
