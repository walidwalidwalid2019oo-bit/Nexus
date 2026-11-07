package com.nexus.app

import android.Manifest
import android.graphics.BitmapFactory
import android.net.Uri
import android.media.MediaPlayer
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import java.util.Locale
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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

var Night = Color(0xFF08070B)
var Panel = Color(0xFF15111B)
var Purple = Color(0xFF7040A0)
var DeepPurple = Color(0xFF301044)
var MainText = Color(0xFFEAE5EF)
var Muted = Color(0xFF9B8AA8)
data class ChatMessage(val role: String, val content: String, val imageUri: String? = null)

private fun applyTheme(name: String) {
 when(name) {
 "Midnight Blue" -> { Night=Color(0xFF070B12); Panel=Color(0xFF111A29); Purple=Color(0xFF5794D6); DeepPurple=Color(0xFF142D50); MainText=Color(0xFFE6EFFB); Muted=Color(0xFF91A5BF) }
 "Emerald" -> { Night=Color(0xFF070D0B); Panel=Color(0xFF12201A); Purple=Color(0xFF59B88B); DeepPurple=Color(0xFF123D2B); MainText=Color(0xFFE4F3EA); Muted=Color(0xFF91B5A0) }
 "Crimson" -> { Night=Color(0xFF100809); Panel=Color(0xFF241316); Purple=Color(0xFFD06A79); DeepPurple=Color(0xFF4A1824); MainText=Color(0xFFF6E8EA); Muted=Color(0xFFC0A0A5) }
 "Soft Light" -> { Night=Color(0xFFF1EDF5); Panel=Color(0xFFFFFFFF); Purple=Color(0xFF7040A0); DeepPurple=Color(0xFF5B3479); MainText=Color(0xFF21182A); Muted=Color(0xFF71677A) }
 else -> { Night=Color(0xFF08070B); Panel=Color(0xFF15111B); Purple=Color(0xFF9A63C7); DeepPurple=Color(0xFF301044); MainText=Color(0xFFEAE5EF); Muted=Color(0xFF9B8AA8) }
 }
}
private val aiModes = listOf("TITAN","VOID","NEXUS AI","CREATIVE AI")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("nexus_updates", "NEXUS updates", NotificationManager.IMPORTANCE_DEFAULT)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        setContent { NexusApp() }
    }
}

@Composable
fun NexusApp() {
    val context = LocalContext.current
    var notificationGranted by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    }
    var notificationStatus by remember { mutableStateOf("") }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationGranted = granted
        notificationStatus = if (granted) "Notifications are enabled." else "Permission not granted. You can change it in Android Settings."
    }
    val requestNotifications = {
        if (Build.VERSION.SDK_INT >= 33) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else { notificationGranted = true; notificationStatus = "Notifications are enabled on this Android version." }
    }
    val sendTestNotification = {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationGranted = false
            notificationStatus = "Grant notification permission first."
        } else {
            val notification = Notification.Builder(context, "nexus_updates")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("NEXUS is online")
                .setContentText("Your notification system is working.")
                .setAutoCancel(true)
                .build()
            context.getSystemService(NotificationManager::class.java).notify(1001, notification)
            notificationStatus = "Test notification sent."
        }
    }
    val prefs = remember { context.getSharedPreferences("nexus_data", Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf("Home") }
    var mode by remember { mutableStateOf(prefs.getString("selected_ai", "NEXUS AI") ?: "NEXUS AI") }
    var themeName by remember { mutableStateOf(prefs.getString("theme","Obsidian Purple") ?: "Obsidian Purple") }
    applyTheme(themeName)
    var models by remember { mutableStateOf(aiModes.associateWith { prefs.getString("model_"+it,"openai/gpt-oss-120b") ?: "" }) }
    var showUnlock by remember { mutableStateOf(false) }
    var unlockInput by remember { mutableStateOf("") }
    var unlockError by remember { mutableStateOf(false) }
    var vaultUnlocked by remember { mutableStateOf(false) }
    var iseeUnlocked by remember { mutableStateOf(false) }
    var ididUnlocked by remember { mutableStateOf(false) }
    var pythonFileName by remember { mutableStateOf(prefs.getString("isee_file_name", "main.py") ?: "main.py") }
    var pythonCode by remember { mutableStateOf(prefs.getString("isee_code", "# Welcome to ISEE\n# Your private Python workspace\n\nprint(\"Hello, NEXUS!\")") ?: "") }
    var creatorLog by remember { mutableStateOf(prefs.getString("idid_creator_log", "") ?: "") }
    var notes by remember { mutableStateOf(loadNotes(prefs)) }
    var noteInput by remember { mutableStateOf("") }
    var memory by remember(mode) { mutableStateOf(prefs.getString("memory_" + mode, "") ?: "") }
    var memorySaved by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(prefs.getBoolean("memory_sharing", false)) }
    var chats by remember(mode) { mutableStateOf(loadMessages(prefs, mode)) }
    var chatInput by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
            selectedPhotoUri = uri
        }
    }
    var sending by remember { mutableStateOf(false) }
    var chatError by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf(prefs.getString("api_endpoint", "https://api.groq.com/openai/v1/chat/completions") ?: "") }
    var model by remember { mutableStateOf(prefs.getString("api_model", "openai/gpt-oss-120b") ?: "") }
    var apiKey by remember { mutableStateOf(readSecret(context)) }
    var settingsStatus by remember { mutableStateOf("") }
    var darkTab by remember { mutableStateOf("MUSIC") }
    var musicUri by remember { mutableStateOf<Uri?>(null) }
    var musicName by remember { mutableStateOf("No track selected") }
    var musicPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var musicPlaying by remember { mutableStateOf(false) }
    var photoPrompt by remember { mutableStateOf("") }
    var darkStatus by remember { mutableStateOf("") }
    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                musicPlayer?.release()
                musicPlayer = MediaPlayer.create(context, uri)
                musicUri = uri
                musicName = uri.lastPathSegment?.substringAfterLast('/') ?: "Selected audio"
                musicPlaying = false
                darkStatus = if (musicPlayer != null) "Track loaded. Press PLAY to listen." else "This audio file could not be opened."
            } catch (_: Exception) {
                darkStatus = "Could not open that audio file."
            }
        }
    }
    DisposableEffect(musicPlayer) {
        val activePlayer = musicPlayer
        onDispose { try { activePlayer?.release() } catch (_: Exception) { } }
    }

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
                if (page != "Chat" && page != "DARK SIDE") {
                    Spacer(Modifier.height(14.dp))
                    Text("N E X U S", color = MainText, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                    Text("YOUR DIGITAL SPACE  /  " + mode, color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(14.dp))
                }
                when (page) {
                    "Home" -> HomePage(mode, onChoose = {
                        mode=it; prefs.edit().putString("selected_ai",it).apply(); page="Chat"
                    }, onSecret = { showUnlock=true }, onNavigate = { page=it })
                    "NEXUS 3.0" -> NexusThreePage(onBack = { page = "Home" }, prefs = prefs)
                    "PROJECT MAP" -> ProjectMapPage(onBack = { page="Home" })
                    "DARK SIDE" -> DarkSidePage(
                        tab = darkTab,
                        onTab = { darkTab = it; darkStatus = "" },
                        trackName = musicName,
                        hasTrack = musicUri != null && musicPlayer != null,
                        isPlaying = musicPlaying,
                        onPickTrack = { musicPicker.launch(arrayOf("audio/*")) },
                        onTogglePlayback = {
                            try {
                                val player = musicPlayer
                                if (player != null) {
                                    if (musicPlaying) { player.pause(); musicPlaying = false }
                                    else { player.start(); musicPlaying = true }
                                } else darkStatus = "Choose a local audio file first."
                            } catch (_: Exception) { musicPlaying = false; darkStatus = "Playback failed for this file." }
                        },
                        photoPrompt = photoPrompt,
                        onPhotoPrompt = { photoPrompt = it },
                        status = darkStatus,
                        onCopyPrompt = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("DARK SIDE image prompt", photoPrompt))
                            darkStatus = "Prompt copied. Open the image playground to use it."
                        },
                        onOpenGenerator = {
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://pollinations.ai/play"))) }
                            catch (_: Exception) { darkStatus = "Could not open the image playground." }
                        },
                        onBack = { page = "Home" }
                    )
                    "Chat" -> ChatPage(mode, chats, chatInput, { chatInput = it }, selectedPhotoUri,
                        { photoPicker.launch(arrayOf("image/*")) }, { selectedPhotoUri = null }, sending, chatError, {
                        val prompt = chatInput.trim().ifBlank { if (selectedPhotoUri != null) "Please describe this image." else "" }
                        if ((prompt.isNotEmpty() || selectedPhotoUri != null) && !sending) {
                            if (apiKey.isBlank()) chatError = "Add an API key in Settings to activate live AI chat."
                            else {
                                val photoForMessage = selectedPhotoUri
                                val history = chats + ChatMessage("user", prompt, photoForMessage?.toString())
                                chats = history
                                saveMessages(prefs, mode, history)
                                chatInput = ""
                                selectedPhotoUri = null
                                chatError = ""
                                sending = true
                                Thread {
                                    val imageDataUrl = try { photoForMessage?.let { encodeImageAsDataUrl(context, it) } }
                                        catch (e: Exception) { null }
                                    val result = if (photoForMessage != null && imageDataUrl == null)
                                        Pair(null, "Could not prepare that photo. Try a smaller image (under 8 MB).")
                                    else askProvider(endpoint, models[mode] ?: "", apiKey, mode, history, imageDataUrl)
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
                    "Settings" -> SettingsPage(endpoint, { endpoint=it }, models,
                        { ai,value -> models=models+(ai to value); prefs.edit().putString("model_"+ai,value).apply() },
                        apiKey, { apiKey=it }, settingsStatus, {
                            prefs.edit().putString("api_endpoint",endpoint.trim()).apply()
                            settingsStatus=try { writeSecret(context,apiKey.trim()); "Connection saved securely." }
                            catch (_:Exception) { "Could not securely save the key." }
                        }, themeName, { themeName=it; applyTheme(it); prefs.edit().putString("theme",it).apply() },
                        { vaultUnlocked=false }, notificationGranted, requestNotifications, sendTestNotification, notificationStatus)
                    "VOID LAB" -> if (vaultUnlocked) VoidLabPage(endpoint, models["CREATIVE AI"] ?: "", apiKey) else { page="Home" }
                    "ISEE" -> if (iseeUnlocked) IseePage(pythonFileName, { pythonFileName=it }, pythonCode, { pythonCode=it }, { prefs.edit().putString("isee_file_name",pythonFileName.trim().ifBlank { "main.py" }).putString("isee_code",pythonCode).apply() }) else { page="Home" }
                    "IDID" -> if (ididUnlocked) IdidPage(creatorLog, { creatorLog=it }, { prefs.edit().putString("idid_creator_log",creatorLog).apply() }) else { page="Home" }
                    else -> { page="Home" }
                }
            }
        }
    }
    if (showUnlock) {
        AlertDialog(onDismissRequest={showUnlock=false; unlockInput=""; unlockError=false}, containerColor=Panel,
            title={Text("CLASSIFIED VAULT ACCESS",color=MainText,fontWeight=FontWeight.Bold)},
            text={
                Column {
                    Text("Enter one of your access codes to open its workspace.",color=Muted,fontSize=12.sp)
                    OutlinedTextField(unlockInput,{unlockInput=it;unlockError=false},singleLine=true,label={Text("Secret key")},
                        visualTransformation=PasswordVisualTransformation(),colors=fieldColors())
                    if(unlockError) Text("Access denied.",color=Color(0xFFE58A8A),fontSize=11.sp)
                }
            },
            confirmButton={TextButton(onClick={
                when(unlockInput.trim().lowercase()) { "void" -> { vaultUnlocked=true; page="VOID LAB"; showUnlock=false; unlockInput="" }; "isee" -> { iseeUnlocked=true; page="ISEE"; showUnlock=false; unlockInput="" }; "idid" -> { ididUnlocked=true; page="IDID"; showUnlock=false; unlockInput="" }; else -> unlockError=true }
            }){Text("UNLOCK",color=Purple)}},
            dismissButton={TextButton(onClick={showUnlock=false;unlockInput=""}){Text("CANCEL",color=Muted)}}
        )
    }
}

@Composable
private fun HomePage(
    active: String,
    onChoose: (String) -> Unit,
    onSecret: () -> Unit,
    onNavigate: (String) -> Unit
) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, DeepPurple)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("NEXUS  /  COMMAND CENTER", color = Purple, fontSize = 9.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("Your space.\nYour systems.", color = MainText, fontSize = 27.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold)
                Text("Four intelligence modes, one connected workspace.", color = Muted, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = DeepPurple, shape = RoundedCornerShape(50)) {
                        Text("●  SYSTEM READY", color = Color(0xFFD8B9F2), modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), fontSize = 9.sp)
                    }
                    Surface(color = Night, shape = RoundedCornerShape(50)) {
                        Text("ACTIVE  /  $active", color = Muted, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), fontSize = 9.sp)
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("INTELLIGENCE", color = MainText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Choose a mode to open its chat.", color = Muted, fontSize = 10.sp)
            }
            Text("04 MODES", color = Purple, fontSize = 9.sp, letterSpacing = 1.sp)
        }
        listOf(
            Triple("TITAN", "Personal companion", "Your familiar AI space"),
            Triple("VOID", "Developer intelligence", "Code, systems and terminal thinking"),
            Triple("NEXUS AI", "General assistant", "Questions, planning and everyday ideas"),
            Triple("CREATIVE AI", "Creative workspace", "Music, stories and game concepts")
        ).forEachIndexed { index, item ->
            Card(
                onClick = { onChoose(item.first) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (active == item.first) DeepPurple else Panel),
                border = BorderStroke(1.dp, if (active == item.first) Purple else Color(0xFF29212F))
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(39.dp).background(if (active == item.first) Purple else DeepPurple, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Text(listOf("T", "V", "N", "C")[index], color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(item.first, color = MainText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(item.second, color = MainText, fontSize = 11.sp)
                        Text(item.third, color = Muted, fontSize = 10.sp)
                    }
                    Text(if (active == item.first) "ACTIVE" else "OPEN  ›", color = if (active == item.first) Color(0xFFD8B9F2) else Purple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text("WORKSPACES", color = MainText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Card(onClick = { onNavigate("Lost Mind") }, modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(15.dp)) {
                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("✧", color = Purple, fontSize = 21.sp)
                    Text("LOST MIND", color = MainText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Thoughts & experiments", color = Muted, fontSize = 9.sp)
                }
            }
            Card(onClick = { onNavigate("Memory") }, modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(15.dp)) {
                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("◈", color = Purple, fontSize = 21.sp)
                    Text("MEMORY", color = MainText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Mode-specific notes", color = Muted, fontSize = 9.sp)
                }
            }
        }
        Card(onClick = { onNavigate("DARK SIDE") }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF180D22)), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFF8B4BB8))) {
            Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(43.dp).background(Color(0xFF351548), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                    Text("♫", color = Color(0xFFE4B8FF), fontSize = 24.sp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("DARK SIDE", color = Color(0xFFEBD8F7), fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text("Music • Photos • Help • Codes", color = Color(0xFFB99BCB), fontSize = 10.sp)
                    Text("ENTER THE OTHER SIDE  ›", color = Color(0xFFC58BE8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Card(onClick = { onNavigate("NEXUS 3.0") }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF21122D)), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFF9A63C7))) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(42.dp).background(Color(0xFF4A2461), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Text("3.0", color = Color(0xFFEAD8F8), fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
                Column(Modifier.weight(1f)) {
                    Text("NEXUS 3.0", color = MainText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Ten upgrade modules • new tools and workspaces", color = Muted, fontSize = 10.sp)
                }
                Text("EXPLORE  ›", color = Purple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Card(onClick = { onNavigate("PROJECT MAP") }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, DeepPurple)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(42.dp).background(DeepPurple, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Text("⌘", color = Color(0xFFD8B9F2), fontSize = 22.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text("PROJECT MAP", color = MainText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Explore how your projects connect.", color = Muted, fontSize = 10.sp)
                }
                Text("OPEN  ›", color = Purple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = DeepPurple), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Purple)) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("CLASSIFIED WORKSPACE", color = Color(0xFFD8B9F2), fontSize = 9.sp, letterSpacing = 1.5.sp)
                Text("VOID PROTOCOL", color = MainText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Experimental tools and private creator workspaces.", color = Muted, fontSize = 10.sp)
                Button(onClick = onSecret, colors = ButtonDefaults.buttonColors(containerColor = Purple)) { Text("ENTER WITH SECRET KEY") }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DarkSidePage(
    tab: String,
    onTab: (String) -> Unit,
    trackName: String,
    hasTrack: Boolean,
    isPlaying: Boolean,
    onPickTrack: () -> Unit,
    onTogglePlayback: () -> Unit,
    photoPrompt: String,
    onPhotoPrompt: (String) -> Unit,
    status: String,
    onCopyPrompt: () -> Unit,
    onOpenGenerator: () -> Unit,
    onBack: () -> Unit
) {
    val darkBg = Color(0xFF09060D)
    val darkPanel = Color(0xFF17101E)
    val neon = Color(0xFFB56BE8)
    Column(
        Modifier.fillMaxSize().background(darkBg).verticalScroll(rememberScrollState()).padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ NEXUS", color = Color(0xFFB99BCB), fontSize = 11.sp) }
            Spacer(Modifier.weight(1f))
            Text("PRIVATE FREQUENCY", color = Color(0xFF8F719F), fontSize = 8.sp, letterSpacing = 1.5.sp)
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF21122D)), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Color(0xFF71418D))) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("NEXUS  /  AFTER HOURS", color = Color(0xFFD8A9F3), fontSize = 9.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("DARK SIDE", color = Color(0xFFF4E9FA), fontSize = 29.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text("A quieter corner for sound and imagination.", color = Color(0xFFB99BCB), fontSize = 11.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("MUSIC", "PHOTOS", "HELP", "CODELIST").forEach { item ->
                val selected = tab == item
                Surface(
                    onClick = { onTab(item) },
                    modifier = Modifier.weight(1f),
                    color = if (selected) Color(0xFF4A2461) else darkPanel,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (selected) neon else Color(0xFF30233A))
                ) {
                    Text(item, modifier = Modifier.padding(vertical = 11.dp), color = if (selected) Color.White else Color(0xFFB99BCB), fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        when (tab) {
            "MUSIC" -> {
                Text("MUSIC ROOM", color = neon, fontSize = 10.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
                Card(colors = CardDefaults.cardColors(containerColor = darkPanel), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Color(0xFF392746))) {
                    Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("YOUR LOCAL LIBRARY", color = Color(0xFFEBD8F7), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Play audio files stored on your device.", color = Color(0xFFB99BCB), fontSize = 10.sp)
                        Text(trackName, color = Color.White, fontSize = 12.sp, maxLines = 2)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onPickTrack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF60347B))) { Text("CHOOSE AUDIO", fontSize = 9.sp) }
                            OutlinedButton(onClick = onTogglePlayback, enabled = hasTrack, colors = ButtonDefaults.outlinedButtonColors(contentColor = neon)) {
                                Text(if (isPlaying) "PAUSE" else "PLAY", fontSize = 9.sp)
                            }
                        }
                    }
                }
                Text("DARK SIDE currently plays audio files you choose from your device. Online music services are not connected yet.", color = Color(0xFF8F8198), fontSize = 10.sp, lineHeight = 15.sp)
            }
            "PHOTOS" -> {
                Text("IMAGE STUDIO", color = neon, fontSize = 10.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
                Card(colors = CardDefaults.cardColors(containerColor = darkPanel), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Color(0xFF392746))) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("DESCRIBE YOUR IMAGE", color = Color(0xFFEBD8F7), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = photoPrompt, onValueChange = onPhotoPrompt, modifier = Modifier.fillMaxWidth(), minLines = 3, placeholder = { Text("A violet-lit city floating above the clouds…") }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = neon, unfocusedBorderColor = Color(0xFF493653), focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = neon))
                        Button(onClick = onCopyPrompt, enabled = photoPrompt.isNotBlank(), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF60347B))) { Text("COPY PROMPT") }
                        OutlinedButton(onClick = onOpenGenerator, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = neon)) { Text("OPEN POLLINATIONS PLAYGROUND") }
                        Text("The in-app generator still needs a secure API connection. Your secret key is not stored in this APK.", color = Color(0xFFB99BCB), fontSize = 10.sp, lineHeight = 15.sp)
                    }
                }
            }
            "HELP" -> {
                Text("DARK SIDE GUIDE", color = neon, fontSize = 10.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
                listOf(
                    "MUSIC" to "Choose an audio file on your device, then press PLAY or PAUSE.",
                    "PHOTOS" to "Write an image idea, copy the prompt, and open the Pollinations playground. Native in-app generation will be connected after secure API setup.",
                    "HELP" to "You're here. This guide explains each DARK SIDE section.",
                    "CODELIST" to "See the available DARK SIDE shortcuts and what each one opens."
                ).forEach { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = darkPanel), shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.first, color = Color(0xFFEBD8F7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(item.second, color = Color(0xFFB99BCB), fontSize = 10.sp, lineHeight = 15.sp)
                        }
                    }
                }
            }
            else -> {
                Text("AVAILABLE CODES", color = neon, fontSize = 10.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
                listOf(
                    "DARK" to "Open the DARK SIDE workspace from the NEXUS home panel.",
                    "MUSIC" to "Open the local music player section.",
                    "PHOTOS" to "Open the image prompt workspace.",
                    "HELP" to "Open the DARK SIDE guide.",
                    "CODELIST" to "Show this list of available shortcuts."
                ).forEach { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = darkPanel), shape = RoundedCornerShape(13.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.first, color = neon, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(82.dp))
                            Text(item.second, color = Color(0xFFB99BCB), fontSize = 10.sp, lineHeight = 15.sp)
                        }
                    }
                }
                Text("These are navigation shortcuts, not terminal commands.", color = Color(0xFF8F8198), fontSize = 9.sp)
            }
        }
        if (status.isNotBlank()) Text(status, color = Color(0xFFD8A9F3), fontSize = 10.sp, lineHeight = 15.sp)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ProjectMapPage(onBack: () -> Unit) {
    val projects = listOf(
        Triple("NEXUS", "Central workspace", "Multi-mode Android system connecting AI modes, memory and creator tools."),
        Triple("TITAN", "AI companion", "A personal AI concept with its own identity and conversation history."),
        Triple("VOID TERMINAL", "Developer system", "A terminal-style Python prototype for coding, tools and AI experiments."),
        Triple("DEAD SOULS", "Game project", "A 2D atmospheric horror game concept built around an eerie hotel."),
        Triple("DARK SIDE", "Music app", "A dark music-player concept with discovery and creative tools."),
        Triple("LOST MIND", "Idea archive", "A place for quick thoughts, unfinished concepts and experiments.")
    )
    var selected by remember { mutableStateOf("NEXUS") }
    val detail = projects.first { it.first == selected }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("NEXUS  /  PROJECT MAP", color = Purple, fontSize = 10.sp, letterSpacing = 1.7.sp, fontWeight = FontWeight.Bold)
                Text("The connected universe.", color = MainText, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onBack) { Text("BACK", color = Muted, fontSize = 10.sp) }
        }
        Card(colors = CardDefaults.cardColors(containerColor = DeepPurple), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Purple)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("CENTRAL HUB", color = Color(0xFFD8B9F2), fontSize = 9.sp, letterSpacing = 1.5.sp)
                Text("NEXUS", color = MainText, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("Select any node below to inspect its role in the project universe.", color = Muted, fontSize = 10.sp)
            }
        }
        Text("PROJECT NODES  /  SELECT TO EXPLORE", color = Muted, fontSize = 9.sp, letterSpacing = 1.2.sp)
        projects.forEachIndexed { index, project ->
            Card(onClick = { selected = project.first }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (selected == project.first) DeepPurple else Panel), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, if (selected == project.first) Purple else Color(0xFF29212F))) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    Box(Modifier.size(36.dp).background(if (selected == project.first) Purple else Night, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Text(if (index == 0) "⌘" else project.first.take(1), color = MainText, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(project.first, color = MainText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(project.second, color = Muted, fontSize = 10.sp)
                    }
                    Text(if (selected == project.first) "SELECTED" else "›", color = Purple, fontSize = 10.sp)
                }
            }
            if (index < projects.lastIndex) Box(Modifier.padding(start = 29.dp).width(1.dp).height(8.dp).background(DeepPurple))
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, DeepPurple)) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("NODE DETAILS", color = Purple, fontSize = 9.sp, letterSpacing = 1.3.sp)
                Text(detail.first, color = MainText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(detail.second, color = Muted, fontSize = 10.sp)
                Text(detail.third, color = MainText, fontSize = 12.sp, lineHeight = 18.sp)
                Text("Project status is descriptive and can be expanded with milestones and notes in a future update.", color = Muted, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun ChatPage(
    mode: String, messages: List<ChatMessage>, input: String, onInput: (String) -> Unit,
    selectedPhoto: Uri?, pickPhoto: () -> Unit, clearPhoto: () -> Unit,
    sending: Boolean, error: String, send: () -> Unit, clear: () -> Unit
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(mode, color = MainText, fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(if (sending) "GENERATING RESPONSE..." else "YOUR PRIVATE CONVERSATION", color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
            }
            Surface(shape = RoundedCornerShape(50), color = DeepPurple) {
                Text("●  READY", modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = Color(0xFFD8B9F2), fontSize = 9.sp)
            }
            TextButton(onClick = clear) { Text("CLEAR", color = Muted, fontSize = 9.sp) }
        }
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (messages.isEmpty()) item {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, DeepPurple)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("✦", color = Purple, fontSize = 25.sp)
                        Text("A fresh space for ideas.", color = MainText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text("Ask a question, share a photo, or type SONG to open the Creative Studio.", color = Muted, fontSize = 12.sp)
                    }
                }
            }
            items(messages) { message ->
                val isUser = message.role == "user"
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
                    Card(
                        Modifier.fillMaxWidth(if (isUser) 0.91f else 0.97f),
                        colors = CardDefaults.cardColors(containerColor = if (isUser) DeepPurple else Panel),
                        shape = RoundedCornerShape(if (isUser) 19.dp else 16.dp),
                        border = if (isUser) null else BorderStroke(1.dp, Color(0xFF30243A))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (isUser) "YOU" else mode.uppercase(), color = if (isUser) Color(0xFFE3C9F8) else Purple,
                                    fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(Modifier.weight(1f))
                                if (!isUser) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    TextButton(onClick = {
                                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("NEXUS response", message.content))
                                    }, contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp)) {
                                        Text("COPY  ⧉", color = Muted, fontSize = 9.sp)
                                    }
                                }
                            }
                            if (!message.imageUri.isNullOrBlank()) {
                                val bitmap = remember(message.imageUri) {
                                    try { context.contentResolver.openInputStream(Uri.parse(message.imageUri))?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }
                                    catch (_: Exception) { null }
                                }
                                if (bitmap != null) Image(bitmap, contentDescription = "Attached photo",
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp), contentScale = ContentScale.Crop)
                                else Text("Photo attached", color = Muted, fontSize = 10.sp)
                            }
                            if (message.content.isNotBlank()) Text(message.content, color = MainText, fontSize = 13.sp, lineHeight = 19.sp)
                        }
                    }
                }
            }
            if (sending) item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(15.dp), color = Purple, strokeWidth = 2.dp)
                    Text(mode + " is thinking...", color = Muted, fontSize = 11.sp)
                }
            }
            if (error.isNotBlank()) item {
                Text(error, color = Color(0xFFE6A0A0), fontSize = 11.sp)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, DeepPurple)) {
            Column(Modifier.fillMaxWidth().padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                if (selectedPhoto != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("PHOTO READY TO SEND", color = Purple, fontSize = 9.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = clearPhoto, contentPadding = PaddingValues(4.dp)) { Text("REMOVE", color = Muted, fontSize = 9.sp) }
                    }
                    val preview = remember(selectedPhoto) {
                        try { context.contentResolver.openInputStream(selectedPhoto)?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }
                        catch (_: Exception) { null }
                    }
                    if (preview != null) Image(preview, contentDescription = "Selected photo preview",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp), contentScale = ContentScale.Crop)
                }
                OutlinedTextField(input, onInput, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Message " + mode + "...  (type SONG for music)", color = Muted) },
                    maxLines = 5, shape = RoundedCornerShape(15.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = fieldColors(), enabled = !sending)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = pickPhoto, enabled = !sending, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("＋ PHOTO", color = Purple, fontSize = 10.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Button(send, enabled = !sending && (input.isNotBlank() || selectedPhoto != null),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepPurple),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp)) {
                        Text(if (sending) "WAIT..." else "SEND  ➤", fontSize = 11.sp)
                    }
                }
            }
        }
    }
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
private fun SettingsPage(endpoint:String,onEndpoint:(String)->Unit,models:Map<String,String>,onModel:(String,String)->Unit,
 key:String,onKey:(String)->Unit,status:String,save:()->Unit,themeName:String,onTheme:(String)->Unit,lockVault:()->Unit,
 notificationsGranted:Boolean,requestNotifications:()->Unit,sendTestNotification:()->Unit,notificationStatus:String){
 var themeMenu by remember{mutableStateOf(false)}
 var modeMenu by remember{mutableStateOf(false)}
 var editMode by remember{mutableStateOf("NEXUS AI")}
 var localModel by remember(editMode,models){mutableStateOf(models[editMode]?: "")}
 LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=18.dp)){
  item{Text("SETTINGS",color=MainText,fontSize=21.sp,fontWeight=FontWeight.Bold);Text("Organized controls for your workspace.",color=Muted,fontSize=11.sp)}
  item{SettingsCard("APPEARANCE"){
   Text("Choose a color theme.",color=Muted,fontSize=11.sp)
   Box{OutlinedButton(onClick={themeMenu=true}){Text(themeName+"  ▾",color=MainText)}
    DropdownMenu(expanded=themeMenu,onDismissRequest={themeMenu=false}){
     listOf("Obsidian Purple","Midnight Blue","Emerald","Crimson","Soft Light").forEach{n->DropdownMenuItem(text={Text(n)},onClick={onTheme(n);themeMenu=false})}
    }}
  }}
  item{SettingsCard("AI MODELS"){
   Text("Select an AI, then edit its model ID here. Model controls stay in Settings.",color=Muted,fontSize=11.sp)
   Box{OutlinedButton(onClick={modeMenu=true}){Text(editMode+"  ▾",color=MainText)}
    DropdownMenu(expanded=modeMenu,onDismissRequest={modeMenu=false}){
     aiModes.forEach{n->DropdownMenuItem(text={Text(n)},onClick={editMode=n;localModel=models[n]?: "";modeMenu=false})}
    }}
   OutlinedTextField(localModel,{localModel=it},modifier=Modifier.fillMaxWidth(),label={Text("Model ID")},singleLine=true,colors=fieldColors())
   Button(onClick={onModel(editMode,localModel.trim())},colors=ButtonDefaults.buttonColors(containerColor=DeepPurple)){Text("SAVE "+editMode+" MODEL")}
  }}
  item{SettingsCard("AI CONNECTION"){
   Text("One HTTPS endpoint and key can serve all four models.",color=Muted,fontSize=11.sp)
   OutlinedTextField(endpoint,onEndpoint,modifier=Modifier.fillMaxWidth(),label={Text("HTTPS API endpoint")},singleLine=true,colors=fieldColors())
   OutlinedTextField(key,onKey,modifier=Modifier.fillMaxWidth(),label={Text("API key")},singleLine=true,visualTransformation=PasswordVisualTransformation(),colors=fieldColors())
   Button(onClick=save,colors=ButtonDefaults.buttonColors(containerColor=DeepPurple)){Text("SAVE CONNECTION")}
   if(status.isNotBlank())Text(status,color=Purple,fontSize=10.sp)
  }}
  item{SettingsCard("PERMISSIONS & NOTIFICATIONS"){
   Text("NEXUS only asks for permissions when a feature needs them. Notifications are used for app updates and alerts.",color=Muted,fontSize=11.sp)
   Text(if(notificationsGranted) "Notification permission: GRANTED" else "Notification permission: NOT GRANTED",color=if(notificationsGranted) Purple else Muted,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    OutlinedButton(onClick=requestNotifications){Text(if(notificationsGranted) "CHECK PERMISSION" else "ALLOW NOTIFICATIONS",color=Purple)}
    Button(onClick=sendTestNotification,enabled=notificationsGranted,colors=ButtonDefaults.buttonColors(containerColor=DeepPurple)){Text("TEST")}
   }
   if(notificationStatus.isNotBlank()) Text(notificationStatus,color=Muted,fontSize=10.sp)
   Text("Camera, microphone and location are not requested because the current app features do not need them.",color=Muted,fontSize=10.sp)
  }}
  item{SettingsCard("PRIVACY & SECURITY"){
   Text("API keys are encrypted with Android Keystore. Chats, memories and notes are stored locally. Messages sent to an AI provider leave your phone and follow that provider's policies. Never share your API key.",color=Muted,fontSize=11.sp)
   OutlinedButton(onClick=lockVault){Text("LOCK VOID PROTOCOL",color=Purple)}
  }}
  item{SettingsCard("ABOUT NEXUS"){Text("NEXUS · Early access build",color=MainText,fontSize=12.sp);Text("External AI services may have usage limits or costs.",color=Muted,fontSize=10.sp)}}
 }
}
@Composable
private fun SettingsCard(title:String,content:@Composable ColumnScope.()->Unit){
 Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(15.dp),border=BorderStroke(1.dp,DeepPurple)){
  Column(Modifier.fillMaxWidth().padding(13.dp)){Text(title,color=Purple,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp);Spacer(Modifier.height(8.dp));content()}
 }
}
@Composable
private fun VoidLabPage(endpoint: String, modelId: String, apiKey: String){
 var storyNo by remember{mutableIntStateOf(0)}
 var story by remember{mutableStateOf(mysteryStory(0))}
 var script by remember{mutableStateOf("The signal came from a room that does not exist.")}
 var pitch by remember{mutableFloatStateOf(0.85f)}
 var speed by remember{mutableFloatStateOf(0.9f)}
 var status by remember{mutableStateOf("On-device speech. Your text is not uploaded.")}
 val context=LocalContext.current
 var engine by remember{mutableStateOf<TextToSpeech?>(null)}
 var ready by remember{mutableStateOf(false)}
 DisposableEffect(Unit){
  val t=TextToSpeech(context){result->ready=result==TextToSpeech.SUCCESS;if(result==TextToSpeech.SUCCESS)engine?.language=Locale.US}
  engine=t
  onDispose{t.stop();t.shutdown();engine=null;ready=false}
 }
 LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=20.dp)){
  item{Text("VOID PROTOCOL",color=Purple,fontSize=11.sp,letterSpacing=2.sp);Text("THE HIDDEN LAB",color=MainText,fontSize=21.sp,fontWeight=FontWeight.Bold);Text("Experimental tools and creative utilities.",color=Muted,fontSize=11.sp)}
  item{SettingsCard("01 / MYSTERY STORY ENGINE"){
   Text("An original atmospheric mystery, generated offline.",color=Muted,fontSize=11.sp)
   Text(story,color=MainText,fontSize=12.sp,lineHeight=19.sp)
   Button(onClick={storyNo++;story=mysteryStory(storyNo)},colors=ButtonDefaults.buttonColors(containerColor=DeepPurple)){Text("GENERATE ANOTHER STORY")}
  }}
  item{SettingsCard("02 / VOICE MAKER"){
   Text("Turn your script into speech using your phone's built-in voice.",color=Muted,fontSize=11.sp)
   OutlinedTextField(script,{script=it},modifier=Modifier.fillMaxWidth(),minLines=3,label={Text("Your script")},colors=fieldColors())
   Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
    OutlinedButton(onClick={pitch=0.85f}){Text("Calm",fontSize=10.sp)}
    OutlinedButton(onClick={pitch=0.7f}){Text("Deep",fontSize=10.sp)}
    OutlinedButton(onClick={pitch=1.15f}){Text("Bright",fontSize=10.sp)}
   }
   Text("Pitch",color=Muted,fontSize=10.sp);Slider(pitch,{pitch=it},valueRange=0.5f..1.5f)
   Text("Speed",color=Muted,fontSize=10.sp);Slider(speed,{speed=it},valueRange=0.6f..1.3f)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    Button(onClick={if(ready&&script.isNotBlank()){engine?.setPitch(pitch);engine?.setSpeechRate(speed);engine?.speak(script,TextToSpeech.QUEUE_FLUSH,null,"nexus_voice");status="Speaking with installed device voice."}else status="Speech engine loading or script empty."},enabled=ready&&script.isNotBlank(),colors=ButtonDefaults.buttonColors(containerColor=DeepPurple)){Text("▶ SPEAK")}
    OutlinedButton(onClick={engine?.stop();status="Playback stopped."}){Text("STOP",color=Purple)}
   }
   Text(status,color=Muted,fontSize=10.sp);Text("Audio file export is not included yet.",color=Muted,fontSize=9.sp)
  }}
  item{var idea by remember{mutableStateOf("A radio station receives weather reports from tomorrow.")};SettingsCard("03 / IDEA GLITCH"){
   Text(idea,color=MainText,fontSize=14.sp,fontWeight=FontWeight.Medium)
   Button(onClick={idea=ideaPrompt()},colors=ButtonDefaults.buttonColors(containerColor=DeepPurple)){Text("ROLL A NEW IDEA")}
  }}
  item{
   var topic by remember{mutableStateOf("A digital world that slowly forgets its creator")}
   var vibe by remember{mutableStateOf("Dark dreamy indie pop")}
   var lyrics by remember{mutableStateOf("")}
   var working by remember{mutableStateOf(false)}
   var songStatus by remember{mutableStateOf("Use your CREATIVE AI model from Settings to write original lyrics.")}
   SettingsCard("04 / SONG MAKER"){
    Text("Build an original song idea and lyrics. Choose a theme and sound.",color=Muted,fontSize=11.sp)
    OutlinedTextField(topic,{topic=it},modifier=Modifier.fillMaxWidth(),label={Text("Song theme")},minLines=2,colors=fieldColors())
    OutlinedTextField(vibe,{vibe=it},modifier=Modifier.fillMaxWidth(),label={Text("Sound / mood")},singleLine=true,colors=fieldColors())
    Button(onClick={
     if(topic.isBlank()) songStatus="Add a song theme first."
     else if(apiKey.isBlank() || modelId.isBlank()) songStatus="Add an API key and CREATIVE AI model in Settings to generate lyrics."
     else{
      working=true;songStatus="CREATIVE AI is writing..."
      val prompt="Write completely original song lyrics based on this theme: "+topic+". Sound and mood: "+vibe+". Create a title, Verse 1, Pre-Chorus, Chorus, Verse 2, Bridge and Final Chorus. Make it vivid and singable. Do not imitate any existing artist or song. Return only the song draft."
      Thread{
       val result=askProvider(endpoint,modelId,apiKey,"CREATIVE AI",listOf(ChatMessage("user",prompt)))
       Handler(Looper.getMainLooper()).post{lyrics=result.first ?: "";songStatus=result.second ?: "Song draft created.";working=false}
      }.start()
     }
    },modifier=Modifier.fillMaxWidth(),enabled=!working,colors=ButtonDefaults.buttonColors(containerColor=DeepPurple)){Text(if(working)"WRITING..." else "CREATE SONG")}
    Text(songStatus,color=Muted,fontSize=10.sp)
    if(lyrics.isNotBlank()){
     Card(colors=CardDefaults.cardColors(containerColor=Night),shape=RoundedCornerShape(12.dp)){
      Column(Modifier.padding(12.dp)){
       Text(lyrics,color=MainText,fontSize=12.sp,lineHeight=18.sp)
       TextButton(onClick={
        val clip=android.content.ClipData.newPlainText("NEXUS song draft",lyrics)
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip)
        songStatus="Lyrics copied to clipboard."
       }){Text("COPY LYRICS",color=Purple)}
      }
     }
    }
    Text("Creates lyrics, not a finished audio recording.",color=Muted,fontSize=9.sp)
   }
  }
 }
}
private fun mysteryStory(n:Int):String{
 val places=listOf("an abandoned observatory","a hotel with thirteen floors","a quiet train station","a library beneath the city","a house beside the salt flats")
 val objects=listOf("a brass key","a cassette marked 04:17","a photograph with one face missing","a clock that runs backwards","a notebook filled with tomorrow's dates")
 val clues=listOf("the lights blinked in a pattern","a voice whispered from a disconnected speaker","every window reflected a different room","the elevator stopped between floors","a message appeared on dusty glass")
 val endings=listOf("The final page contained a drawing of the reader, standing exactly where they were.","At sunrise, the building was gone. The key remained warm in their pocket.","The signal stopped. Then the same message arrived from inside their own phone.","Behind the locked door was a second copy of the room, still waiting for someone to enter.")
 val a=Math.floorMod(n*7+2,places.size);val b=Math.floorMod(n*11+1,objects.size);val c=Math.floorMod(n*13+3,clues.size);val d=Math.floorMod(n*17+1,endings.size)
 return "At 04:17, a stranger arrived at "+places[a]+". On the desk sat "+objects[b]+". As soon as it was touched, "+clues[c]+". A label read: 'You have been here before.\\n\\n'"+endings[d]
}
private fun ideaPrompt():String{
 val ideas=listOf("A city where every shadow points toward a missing street.","A music player that remembers songs nobody has written.","A game map that changes when you stop looking at it.","A museum displaying inventions from timelines that never happened.","A quiet planet where the stars turn off one by one.")
 return ideas[(System.nanoTime().toInt() and Int.MAX_VALUE)%ideas.size]
}

private fun askProvider(endpoint: String, model: String, key: String, mode: String, history: List<ChatMessage>, imageDataUrl: String? = null): Pair<String?, String?> {
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
        val recentHistory = history.takeLast(24)
        recentHistory.forEachIndexed { index, message ->
            val isCurrentPhotoMessage = imageDataUrl != null && index == recentHistory.lastIndex
            val content: Any = if (isCurrentPhotoMessage) JSONArray()
                .put(JSONObject().put("type", "text").put("text", message.content))
                .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", imageDataUrl)))
                else message.content
            arr.put(JSONObject().put("role", message.role).put("content", content))
        }
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

private fun encodeImageAsDataUrl(context: Context, uri: Uri): String {
    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: throw IllegalStateException("Unable to open selected photo")
    if (bytes.size > 8 * 1024 * 1024) throw IllegalArgumentException("Please choose a photo smaller than 8 MB.")
    return "data:" + mime + ";base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
}

private fun loadMessages(prefs: android.content.SharedPreferences, mode: String): List<ChatMessage> = try {
    val a = JSONArray(prefs.getString("chat_" + mode, "[]") ?: "[]")
    (0 until a.length()).map { i -> val o = a.getJSONObject(i); ChatMessage(o.optString("role"), o.optString("content"), o.optString("imageUri").takeIf { it.isNotBlank() }) }
} catch (_: Exception) { emptyList() }

private fun saveMessages(prefs: android.content.SharedPreferences, mode: String, list: List<ChatMessage>) {
    val a = JSONArray()
    list.takeLast(80).forEach { message -> val item = JSONObject().put("role", message.role).put("content", message.content); if (!message.imageUri.isNullOrBlank()) item.put("imageUri", message.imageUri); a.put(item) }
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


@Composable
private fun IseePage(
    fileName: String, onFileName: (String) -> Unit, code: String, onCode: (String) -> Unit, save: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("ISEE  /  PYTHON LAB", color=Purple, fontSize=11.sp, letterSpacing=2.sp, fontWeight=FontWeight.Bold)
        Text("Your private coding workspace.", color=MainText, fontSize=20.sp, fontWeight=FontWeight.Bold)
        Text("Write and save Python snippets on this device. Code execution is not included in this first version.", color=Muted, fontSize=11.sp)
        SettingsCard("WORKSPACE FILE") {
            OutlinedTextField(fileName, onFileName, modifier=Modifier.fillMaxWidth(), singleLine=true,
                label={Text("File name")}, colors=fieldColors())
            OutlinedTextField(code, onCode, modifier=Modifier.fillMaxWidth().heightIn(min=280.dp),
                label={Text("Python source")}, minLines=14, colors=fieldColors(),
                textStyle=MaterialTheme.typography.bodySmall.copy(fontFamily=androidx.compose.ui.text.font.FontFamily.Monospace))
            Button(onClick=save, modifier=Modifier.fillMaxWidth(), colors=ButtonDefaults.buttonColors(containerColor=DeepPurple)) {
                Text("SAVE PYTHON FILE")
            }
            Text("Saved locally inside NEXUS. This editor does not upload your code.", color=Muted, fontSize=10.sp)
        }
        SettingsCard("QUICK START") {
            Text("Edit the starter code, then save it. Use ISEE to keep drafts, experiments and small Python projects organized.", color=Muted, fontSize=11.sp)
        }
    }
}

@Composable
private fun IdidPage(log: String, onLog: (String) -> Unit, save: () -> Unit) {
    val projects = listOf(
        Triple("TITAN", "Personal AI companion concept and Android app experiments.", "PROJECT HISTORY"),
        Triple("VOID TERMINAL", "Terminal-style Python workspace prototype, planned for AI chat and memory.", "PROTOTYPE"),
        Triple("NEXUS", "Multi-mode Android workspace bringing AI spaces, LOST MIND and classified tools together.", "IN DEVELOPMENT"),
        Triple("DEAD SOULS", "2D horror game concept about a father searching an eerie hotel for his children's souls.", "GAME CONCEPT"),
        Triple("DARK SIDE", "Dark-themed music app concept with local playback and creative tools.", "APP CONCEPT"),
        Triple("LOST MIND", "A space for quick thoughts, unusual ideas, experiments and unfinished projects.", "NEXUS WORKSPACE")
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("IDID  /  CREATOR ARCHIVE", color=Purple, fontSize=11.sp, letterSpacing=2.sp, fontWeight=FontWeight.Bold)
        Text("THE STORY SO FAR", color=MainText, fontSize=21.sp, fontWeight=FontWeight.Bold)
        Text("A growing record of ideas, experiments and projects. Status labels describe known stages, not claims that every project is finished.", color=Muted, fontSize=11.sp)
        SettingsCard("THE CREATOR'S TIMELINE") {
            listOf(
                "THE BEGINNING" to "Ideas, experiments and the first AI companion plans.",
                "BUILDING SYSTEMS" to "Python, Android prototypes, terminal interfaces and creative tools.",
                "EXPANDING THE UNIVERSE" to "Game concepts, music ideas and the NEXUS multi-mode workspace.",
                "CURRENT CHAPTER" to "Developing NEXUS and organizing the projects in one place."
            ).forEachIndexed { index, item ->
                Row(verticalAlignment=Alignment.Top, horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                    Text("STEP " + (index + 1), color=Purple, fontSize=12.sp, fontWeight=FontWeight.Bold)
                    Column(Modifier.weight(1f)) {
                        Text(item.first, color=MainText, fontSize=12.sp, fontWeight=FontWeight.SemiBold)
                        Text(item.second, color=Muted, fontSize=10.sp)
                    }
                }
            }
        }
        projects.forEach { project ->
            Card(colors=CardDefaults.cardColors(containerColor=Panel), shape=RoundedCornerShape(14.dp),
                border=BorderStroke(1.dp,DeepPurple)) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(11.dp)) {
                    Box(Modifier.size(38.dp).background(DeepPurple,RoundedCornerShape(11.dp)), contentAlignment=Alignment.Center) {
                        Text(project.first.take(1), color=Color(0xFFD7B8F2), fontWeight=FontWeight.Bold, fontSize=17.sp)
                    }
                    Column(Modifier.weight(1f), verticalArrangement=Arrangement.spacedBy(3.dp)) {
                        Text(project.first, color=MainText, fontWeight=FontWeight.Bold, fontSize=13.sp)
                        Text(project.second, color=Muted, fontSize=10.sp)
                        Text(project.third, color=Purple, fontSize=9.sp, letterSpacing=1.sp)
                    }
                }
            }
        }
        SettingsCard("CREATOR'S LOG") {
            Text("Add your own memories, dates, milestones or details to the archive.", color=Muted, fontSize=10.sp)
            OutlinedTextField(log,onLog,modifier=Modifier.fillMaxWidth(),minLines=4,
                placeholder={Text("Write a moment you want this archive to remember...",color=Muted)},colors=fieldColors())
            Button(onClick=save,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=DeepPurple)) {
                Text("SAVE TO THE ARCHIVE")
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Purple, unfocusedBorderColor = Color(0xFF3A3042),
    focusedTextColor = MainText, unfocusedTextColor = MainText, cursorColor = Purple
)