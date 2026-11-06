package com.nexus.app

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Night = Color(0xFF08070B)
private val Panel = Color(0xFF15111B)
private val Purple = Color(0xFF7040A0)
private val DeepPurple = Color(0xFF301044)
private val MainText = Color(0xFFEAE5EF)
private val Muted = Color(0xFF9B8AA8)
data class ChatMessage(val role: String, val content: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NexusApp() }
    }
}

@Composable
fun NexusApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nexus_data", Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf("Home") }
    var mode by remember { mutableStateOf(prefs.getString("selected_ai", "NEXUS AI") ?: "NEXUS AI") }
    var notes by remember { mutableStateOf(loadNotes(prefs)) }
    var noteInput by remember { mutableStateOf("") }
    var memory by remember(mode) { mutableStateOf(prefs.getString("memory_" + mode, "") ?: "") }
    var memorySaved by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(prefs.getBoolean("memory_sharing", false)) }
    var chats by remember(mode) { mutableStateOf(loadMessages(prefs, mode)) }
    var chatInput by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var chatError by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf(prefs.getString("api_endpoint", "https://api.groq.com/openai/v1/chat/completions") ?: "") }
    var model by remember { mutableStateOf(prefs.getString("api_model", "openai/gpt-oss-120b") ?: "") }
    var apiKey by remember { mutableStateOf(readSecret(context)) }
    var settingsStatus by remember { mutableStateOf("") }

    MaterialTheme(colorScheme = darkColorScheme(background = Night, surface = Panel, primary = Purple,
        secondary = DeepPurple, onBackground = MainText, onSurface = MainText, onPrimary = Color.White)) {
        Scaffold(containerColor = Night, bottomBar = {
            NavigationBar(containerColor = Panel, contentColor = MainText) {
                listOf("Home", "Chat", "Lost Mind", "Memory", "Settings").forEach { tab ->
                    NavigationBarItem(selected = page == tab, onClick = { page = tab },
                        icon = { Text(when (tab) { "Home" -> "⌂"; "Chat" -> "◉"; "Lost Mind" -> "✧"; "Memory" -> "◈"; else -> "⚙" }) },
                        label = { Text(tab, fontSize = 9.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White,
                            selectedTextColor = Color.White, indicatorColor = DeepPurple,
                            unselectedIconColor = Muted, unselectedTextColor = Muted))
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().background(Night).padding(padding).padding(horizontal = 17.dp)) {
                Spacer(Modifier.height(14.dp))
                Text("N E X U S", color = MainText, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                Text("YOUR DIGITAL SPACE  /  " + mode, color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(14.dp))
                when (page) {
                    "Home" -> HomePage(mode) {
                        mode = it
                        prefs.edit().putString("selected_ai", it).apply()
                        page = "Chat"
                    }
                    "Chat" -> ChatPage(mode, chats, chatInput, { chatInput = it }, sending, chatError, {
                        val prompt = chatInput.trim()
                        if (prompt.isNotEmpty() && !sending) {
                            if (apiKey.isBlank()) chatError = "Add an API key in Settings to activate live AI chat."
                            else {
                                val history = chats + ChatMessage("user", prompt)
                                chats = history
                                saveMessages(prefs, mode, history)
                                chatInput = ""
                                chatError = ""
                                sending = true
                                Thread {
                                    val result = askProvider(endpoint, model, apiKey, mode, history)
                                    Handler(Looper.getMainLooper()).post {
                                        if (result.first != null) {
                                            chats = chats + ChatMessage("assistant", result.first!!)
                                            saveMessages(prefs, mode, chats)
                                            chatError = ""
                                        } else chatError = result.second ?: "Connection failed."
                                        sending = false
                                    }
                                }.start()
                            }
                        }
                    }, {
                        chats = emptyList()
                        saveMessages(prefs, mode, chats)
                        chatError = ""
                    })
                    "Lost Mind" -> LostMindPage(noteInput, { noteInput = it }, notes, {
                        if (noteInput.isNotBlank()) {
                            notes = listOf(noteInput.trim()) + notes
                            saveNotes(prefs, notes)
                            noteInput = ""
                        }
                    }, {
                        notes = notes.filterIndexed { i, _ -> i != it }
                        saveNotes(prefs, notes)
                    })
                    "Memory" -> MemoryPage(mode, memory, { memory = it; memorySaved = false }, sharing, {
                        sharing = it
                        prefs.edit().putBoolean("memory_sharing", it).apply()
                    }, {
                        prefs.edit().putString("memory_" + mode, memory).apply()
                        memorySaved = true
                    }, memorySaved)
                    else -> SettingsPage(endpoint, { endpoint = it }, model, { model = it }, apiKey, { apiKey = it }, settingsStatus, {
                        prefs.edit().putString("api_endpoint", endpoint.trim()).putString("api_model", model.trim()).apply()
                        settingsStatus = try {
                            writeSecret(context, apiKey.trim())
                            "Saved. Your API key is encrypted on this device."
                        } catch (_: Exception) { "Could not securely save the key on this device." }
                    })
                }
            }
        }
    }
}

@Composable
private fun HomePage(active: String, choose: (String) -> Unit) {
    Text("Choose your intelligence.", color = MainText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    Text("Four modes. One home.", color = Muted, fontSize = 12.sp)
    Spacer(Modifier.height(13.dp))
    listOf(
        Triple("TITAN", "Personal companion", "Your familiar AI space"),
        Triple("VOID", "Developer intelligence", "Code, systems and terminal thinking"),
        Triple("NEXUS AI", "General assistant", "Everyday questions and ideas"),
        Triple("CREATIVE AI", "Creative studio", "Music, images, stories and games")
    ).forEach { item ->
        Card(onClick = { choose(item.first) }, modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp),
            shape = RoundedCornerShape(17.dp),
            colors = CardDefaults.cardColors(containerColor = if (active == item.first) DeepPurple else Panel),
            border = if (active == item.first) BorderStroke(1.dp, Purple) else null) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.first, color = MainText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    if (active == item.first) Text("ACTIVE", color = Color(0xFFC7A5E8), fontSize = 9.sp)
                }
                Text(item.second, color = MainText, fontSize = 12.sp)
                Text(item.third, color = Muted, fontSize = 10.sp)
            }
        }
    }
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text("NEXUS SYSTEM", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
            Text("Your workspace is ready.", color = MainText, fontSize = 14.sp)
            Text("Chats and memories are separated by mode. LOST MIND keeps your ideas together.", color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ChatPage(
    mode: String, messages: List<ChatMessage>, input: String, onInput: (String) -> Unit,
    sending: Boolean, error: String, send: () -> Unit, clear: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(mode + " CHAT", color = MainText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Separate conversation history for this AI.", color = Muted, fontSize = 10.sp)
        }
        TextButton(onClick = clear) { Text("CLEAR", color = Muted, fontSize = 10.sp) }
    }
    LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)) {
        if (messages.isEmpty()) item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("A new conversation.", color = MainText, fontWeight = FontWeight.SemiBold)
                    Text("Set an HTTPS endpoint, model and API key in Settings to activate live chat.", color = Muted, fontSize = 11.sp)
                }
            }
        }
        items(messages) { message ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.role == "user") Arrangement.End else Arrangement.Start) {
                Card(Modifier.fillMaxWidth(0.9f), colors = CardDefaults.cardColors(
                    containerColor = if (message.role == "user") DeepPurple else Panel), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(11.dp)) {
                        Text(if (message.role == "user") "YOU" else mode, color = Color(0xFFC7A5E8), fontSize = 9.sp)
                        Spacer(Modifier.height(3.dp))
                        Text(message.content, color = MainText, fontSize = 13.sp)
                    }
                }
            }
        }
        if (sending) item { Text(mode + " is thinking...", color = Muted, fontSize = 11.sp) }
        if (error.isNotBlank()) item { Text(error, color = Color(0xFFE6A0A0), fontSize = 11.sp) }
    }
    OutlinedTextField(input, onInput, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Message " + mode + "...", color = Muted) },
        maxLines = 4, shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = fieldColors(), enabled = !sending)
    Spacer(Modifier.height(6.dp))
    Button(send, modifier = Modifier.fillMaxWidth(), enabled = !sending && input.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)) { Text(if (sending) "THINKING..." else "SEND MESSAGE") }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun LostMindPage(input: String, onInput: (String) -> Unit, notes: List<String>, add: () -> Unit, delete: (Int) -> Unit) {
    Text("LOST MIND", color = MainText, fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    Text("Random thoughts, strange concepts and unfinished ideas.", color = Muted, fontSize = 11.sp)
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(input, onInput, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Catch that thought before it disappears...", color = Muted) },
        minLines = 3, shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), colors = fieldColors())
    Spacer(Modifier.height(8.dp))
    Button(add, modifier = Modifier.fillMaxWidth(), enabled = input.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)) { Text("SAVE THOUGHT") }
    Spacer(Modifier.height(12.dp))
    Text("YOUR THOUGHTS  ·  " + notes.size, color = Muted, fontSize = 10.sp)
    if (notes.isEmpty()) Text("Nothing here yet. The next strange idea starts here.", color = Muted, fontSize = 12.sp)
    else LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        items(notes.indices.toList()) { i ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(13.dp)) {
                Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(notes[i], color = MainText, modifier = Modifier.weight(1f), fontSize = 13.sp)
                    TextButton(onClick = { delete(i) }) { Text("DELETE", color = Muted, fontSize = 9.sp) }
                }
            }
        }
    }
}

@Composable
private fun MemoryPage(mode: String, memory: String, onMemory: (String) -> Unit, sharing: Boolean,
    onSharing: (Boolean) -> Unit, save: () -> Unit, saved: Boolean) {
    Text("INDEPENDENT MEMORY", color = MainText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
    Text("Each AI keeps its own memory on this phone.", color = Muted, fontSize = 11.sp)
    Spacer(Modifier.height(13.dp))
    Text("CURRENT SPACE: " + mode, color = Color(0xFFC7A5E8), fontSize = 10.sp)
    OutlinedTextField(memory, onMemory, modifier = Modifier.fillMaxWidth(), placeholder = { Text("What should " + mode + " remember?", color = Muted) },
        minLines = 5, shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), colors = fieldColors())
    Spacer(Modifier.height(8.dp))
    Button(save, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)) { Text("SAVE TO " + mode) }
    if (saved) Text("Memory saved on this device.", color = Color(0xFFC7A5E8), fontSize = 11.sp)
    HorizontalDivider(color = Panel, modifier = Modifier.padding(vertical = 12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Optional memory sharing", color = MainText, fontSize = 13.sp)
            Text("Preference only; cross-space sharing is not active yet.", color = Muted, fontSize = 10.sp)
        }
        Switch(sharing, onSharing)
    }
}

@Composable
private fun SettingsPage(endpoint: String, onEndpoint: (String) -> Unit, model: String, onModel: (String) -> Unit,
    key: String, onKey: (String) -> Unit, status: String, save: () -> Unit) {
    Text("CONNECTION SETTINGS", color = MainText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
    Text("Connect to an OpenAI-compatible chat API.", color = Muted, fontSize = 11.sp)
    Spacer(Modifier.height(12.dp))
    Text("HTTPS API ENDPOINT", color = Muted, fontSize = 10.sp)
    OutlinedTextField(endpoint, onEndpoint, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fieldColors())
    Spacer(Modifier.height(8.dp))
    Text("MODEL ID", color = Muted, fontSize = 10.sp)
    OutlinedTextField(model, onModel, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fieldColors())
    Spacer(Modifier.height(8.dp))
    Text("API KEY", color = Muted, fontSize = 10.sp)
    OutlinedTextField(key, onKey, modifier = Modifier.fillMaxWidth(), singleLine = true,
        visualTransformation = PasswordVisualTransformation(), colors = fieldColors())
    Spacer(Modifier.height(10.dp))
    Button(save, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)) { Text("SAVE CONNECTION") }
    if (status.isNotBlank()) Text(status, color = Color(0xFFC7A5E8), fontSize = 10.sp)
    Spacer(Modifier.height(12.dp))
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(13.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text("PRIVACY & SAFETY", color = MainText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("The API key is encrypted using Android Keystore. Notes and chat history stay on this device. Messages sent to a provider leave your phone and follow that provider's policies. Use an account with a parent or guardian's help where required.", color = Muted, fontSize = 10.sp)
        }
    }
}

private fun askProvider(endpoint: String, model: String, key: String, mode: String, history: List<ChatMessage>): Pair<String?, String?> {
    var conn: HttpURLConnection? = null
    return try {
        if (!endpoint.startsWith("https://")) return Pair(null, "Use a secure HTTPS endpoint.")
        if (model.isBlank() || key.isBlank()) return Pair(null, "Complete the model and API key in Settings.")
        conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 20000; readTimeout = 60000; doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer " + key)
        }
        val prompt = when (mode) {
            "TITAN" -> "You are TITAN, a friendly, honest and thoughtful personal AI companion."
            "VOID" -> "You are VOID, a precise developer assistant focused on programming, debugging and systems."
            "CREATIVE AI" -> "You are CREATIVE AI, an imaginative helper for original music, stories and game concepts."
            else -> "You are NEXUS AI, a helpful, clear and honest general assistant."
        }
        val arr = JSONArray().put(JSONObject().put("role", "system").put("content", prompt))
        history.takeLast(24).forEach { arr.put(JSONObject().put("role", it.role).put("content", it.content)) }
        val body = JSONObject().put("model", model).put("messages", arr).toString()
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val raw = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) Pair(null, "API error " + code + ": " + raw.take(240))
        else {
            val answer = JSONObject(raw).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
            if (answer.isNullOrBlank()) Pair(null, "The provider returned an empty response.") else Pair(answer, null)
        }
    } catch (e: Exception) {
        Pair(null, "Connection error: " + (e.message ?: "unknown error").take(160))
    } finally { conn?.disconnect() }
}

private fun loadMessages(prefs: android.content.SharedPreferences, mode: String): List<ChatMessage> = try {
    val a = JSONArray(prefs.getString("chat_" + mode, "[]") ?: "[]")
    (0 until a.length()).map { i -> val o = a.getJSONObject(i); ChatMessage(o.optString("role"), o.optString("content")) }
} catch (_: Exception) { emptyList() }

private fun saveMessages(prefs: android.content.SharedPreferences, mode: String, list: List<ChatMessage>) {
    val a = JSONArray()
    list.takeLast(80).forEach { a.put(JSONObject().put("role", it.role).put("content", it.content)) }
    prefs.edit().putString("chat_" + mode, a.toString()).apply()
}

private fun loadNotes(prefs: android.content.SharedPreferences): List<String> {
    val raw = prefs.getString("lost_mind_notes", "") ?: ""
    return if (raw.isBlank()) emptyList() else raw.split("\n").filter { it.isNotBlank() }
}
private fun saveNotes(prefs: android.content.SharedPreferences, list: List<String>) {
    prefs.edit().putString("lost_mind_notes", list.joinToString("\n")).apply()
}

private fun getSecretKey(): SecretKey {
    val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (ks.getKey("nexus_api_secret", null) as? SecretKey)?.let { return it }
    val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    gen.init(KeyGenParameterSpec.Builder("nexus_api_secret", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
    return gen.generateKey()
}
private fun writeSecret(context: Context, value: String) {
    val p = context.getSharedPreferences("nexus_data", Context.MODE_PRIVATE)
    if (value.isBlank()) { p.edit().remove("encrypted_api_key").apply(); return }
    val c = Cipher.getInstance("AES/GCM/NoPadding")
    c.init(Cipher.ENCRYPT_MODE, getSecretKey())
    val packed = c.iv + c.doFinal(value.toByteArray(Charsets.UTF_8))
    p.edit().putString("encrypted_api_key", Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
}
private fun readSecret(context: Context): String = try {
    val p = context.getSharedPreferences("nexus_data", Context.MODE_PRIVATE)
    val packed = Base64.decode(p.getString("encrypted_api_key", null), Base64.NO_WRAP)
    val c = Cipher.getInstance("AES/GCM/NoPadding")
    c.init(Cipher.DECRYPT_MODE, getSecretKey(), GCMParameterSpec(128, packed.copyOfRange(0, 12)))
    String(c.doFinal(packed.copyOfRange(12, packed.size)), Charsets.UTF_8)
} catch (_: Exception) { "" }

private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Purple, unfocusedBorderColor = Color(0xFF3A3042),
    focusedTextColor = MainText, unfocusedTextColor = MainText, cursorColor = Purple
)
