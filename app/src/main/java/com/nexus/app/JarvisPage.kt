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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
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

    val motion = rememberInfiniteTransition(label = "pickelMotion")
    val floatY by motion.animateFloat(
        initialValue = -7f, targetValue = 7f,
        animationSpec = infiniteRepeatable(tween(3600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatY"
    )
    val ringRotation by motion.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18000, easing = androidx.compose.animation.core.LinearEasing)),
        label = "ringRotation"
    )
    val counterRotation by motion.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(24000, easing = androidx.compose.animation.core.LinearEasing)),
        label = "counterRotation"
    )
    val orbScale by motion.animateFloat(
        initialValue = 0.985f, targetValue = if (listening || thinking) 1.045f else 1.018f,
        animationSpec = infiniteRepeatable(tween(if (listening || thinking) 1050 else 2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
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

        FloatingPickelOrb(
            modifier = Modifier.padding(vertical = 2.dp).size(190.dp),
            floatY = floatY,
            ringRotation = ringRotation,
            counterRotation = counterRotation,
            orbScale = orbScale,
            active = listening || thinking
        )
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
                                if (message.fromUser) "MR. VOIDEX" else "PICKEL",
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


@Composable
private fun FloatingPickelOrb(
    modifier: Modifier = Modifier,
    floatY: Float,
    ringRotation: Float,
    counterRotation: Float,
    orbScale: Float,
    active: Boolean
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        // Soft ambient glow behind the orb.
        Box(
            Modifier.size(154.dp).graphicsLayer {
                translationY = floatY * 0.55f
                scaleX = orbScale * 1.18f
                scaleY = orbScale * 1.18f
                alpha = if (active) 0.42f else 0.27f
            }.background(
                Brush.radialGradient(
                    colors = listOf(Color(0xAA9B4DFF), Color(0x443F168A), Color.Transparent)
                ),
                CircleShape
            )
        )
        // Two independent, slow orbital paths create depth without abrupt movement.
        Box(
            Modifier.size(width = 178.dp, height = 74.dp)
                .graphicsLayer { rotationZ = ringRotation; translationY = floatY * 0.35f }
                .background(Color.Transparent, CircleShape)
                .then(Modifier)
        ) {
            Box(Modifier.fillMaxSize().background(Color.Transparent, CircleShape))
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                drawOval(
                    color = Color(0xB78E42FF),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6.dp.toPx())
                )
            }
        }
        androidx.compose.foundation.Canvas(
            Modifier.size(width = 164.dp, height = 94.dp).graphicsLayer {
                rotationZ = counterRotation
                translationY = -floatY * 0.25f
            }
        ) {
            drawOval(
                color = Color(0x777D35E8),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
            )
        }
        // Glass-like energy sphere.
        Box(
            Modifier.size(112.dp).graphicsLayer {
                translationY = floatY
                scaleX = orbScale
                scaleY = orbScale
            }.background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFDAA8FF), Color(0xFF8739D5), Color(0xFF28103F), Color(0xFF090512)),
                    radius = 190f
                ),
                CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(76.dp).background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xAAE8C6FF), Color(0x667C31D4), Color.Transparent)
                    ),
                    CircleShape
                )
            )
            androidx.compose.foundation.Canvas(Modifier.size(112.dp)) {
                drawCircle(
                    color = Color(0x99D9A7FF),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4.dp.toPx())
                )
                drawCircle(
                    color = Color(0x558B42E8),
                    radius = size.minDimension * 0.39f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }
        }
        // Small highlight gives the sphere a polished, dimensional surface.
        Box(
            Modifier.offset(x = (-25).dp, y = (-28).dp).size(19.dp)
                .background(Color(0x99F3DDFF), CircleShape)
        )
    }
}
