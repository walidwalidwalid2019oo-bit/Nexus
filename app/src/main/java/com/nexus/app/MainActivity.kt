package com.nexus.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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

private var Night = Color(0xFF08070B)
private var Panel = Color(0xFF15111B)
private var Purple = Color(0xFF7040A0)
private var DeepPurple = Color(0xFF301044)
private var MainText = Color(0xFFEAE5EF)
private var Muted = Color(0xFF9B8AA8)
data class ChatMessage(val role: String, val content: String)

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
                    "Home" -> HomePage(mode, onChoose = {
                        mode=it; prefs.edit().putString("selected_ai",it).apply(); page="Chat"
                    }, onSecret = { showUnlock=true })
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
                                    val result = askProvider(endpoint, models[mode] ?: "", apiKey, mode, history)
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
                    else -> { page="Home" }
                }
            }
        }
    }
    if (showUnlock) {
        AlertDialog(onDismissRequest={showUnlock=false; unlockInput=""; unlockError=false}, containerColor=Panel,
            title={Text("VOID PROTOCOL",color=MainText,fontWeight=FontWeight.Bold)},
            text={
                Column {
                    Text("A hidden area is sealed. Enter the access phrase.",color=Muted,fontSize=12.sp)
                    OutlinedTextField(unlockInput,{unlockInput=it;unlockError=false},singleLine=true,label={Text("Secret key")},
                        visualTransformation=PasswordVisualTransformation(),colors=fieldColors())
                    if(unlockError) Text("Access denied.",color=Color(0xFFE58A8A),fontSize=11.sp)
                }
            },
            confirmButton={TextButton(onClick={
                if(unlockInput.trim().equals("void",true)){vaultUnlocked=true;page="VOID LAB";showUnlock=false;unlockInput=""}
                else unlockError=true
            }){Text("UNLOCK",color=Purple)}},
            dismissButton={TextButton(onClick={showUnlock=false;unlockInput=""}){Text("CANCEL",color=Muted)}}
        )
    }
}

@Composable
private fun HomePage(active: String, onChoose: (String) -> Unit, onSecret: () -> Unit) {
    Text("Choose your intelligence.", color = MainText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    Text("Four modes. One home.", color = Muted, fontSize = 12.sp)
    Spacer(Modifier.height(13.dp))
    listOf(
        Triple("TITAN", "Personal companion", "Your familiar AI space"),
        Triple("VOID", "Developer intelligence", "Code, systems and terminal thinking"),
        Triple("NEXUS AI", "General assistant", "Everyday questions and ideas"),
        Triple("CREATIVE AI", "Creative studio", "Music, images, stories and games")
    ).forEach { item ->
        Card(onClick = { onChoose(item.first) }, modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp),
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
    Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(15.dp)){
        Column(Modifier.padding(14.dp)){
            Text("NEXUS SYSTEM",color=Muted,fontSize=10.sp,letterSpacing=1.sp)
            Text("Your workspace is ready.",color=MainText,fontSize=14.sp)
            Text("Chats and memories are separated by mode. LOST MIND keeps your ideas together.",color=Muted,fontSize=11.sp)
        }
    }
    Spacer(Modifier.height(10.dp))
    Card(colors=CardDefaults.cardColors(containerColor=DeepPurple),shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,Purple)){
        Column(Modifier.padding(14.dp)){
            Text("◈  CLASSIFIED AREA",color=Purple,fontSize=10.sp)
            Text("VOID PROTOCOL",color=MainText,fontSize=17.sp,fontWeight=FontWeight.Bold)
            Text("Mystery tools and creative experiments.",color=Muted,fontSize=11.sp)
            Button(onClick=onSecret,colors=ButtonDefaults.buttonColors(containerColor=Purple)){Text("ENTER WITH SECRET KEY")}
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
    LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(0.68f), verticalArrangement = Arrangement.spacedBy(8.dp),
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

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Purple, unfocusedBorderColor = Color(0xFF3A3042),
    focusedTextColor = MainText, unfocusedTextColor = MainText, cursorColor = Purple
)
