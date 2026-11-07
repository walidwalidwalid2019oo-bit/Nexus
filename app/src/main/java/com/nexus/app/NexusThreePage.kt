package com.nexus.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import java.util.Locale

private data class NexusModule(val number: Int, val title: String, val subtitle: String, val detail: String)

@Composable
fun NexusThreePage(onBack: () -> Unit, prefs: android.content.SharedPreferences) {
    val context = LocalContext.current
    val modules = listOf(
        NexusModule(1, "COMMAND CENTER 2.0", "A single overview of your workspace", "A central launchpad for your AI modes, projects, memory and creative tools. Existing NEXUS navigation remains available."),
        NexusModule(2, "AI COUNCIL", "Prepare multi-AI reviews", "Create a reusable briefing for TITAN, VOID, NEXUS AI and CREATIVE AI. Each model still runs through your configured provider; this screen does not pretend to run parallel agents."),
        NexusModule(3, "PROJECT STUDIO", "Keep project ideas organized", "Create and save a local project list on this device. Cloud collaboration and automatic task execution are not connected."),
        NexusModule(4, "VOICE SYSTEM", "Speak a message aloud", "Use Android's installed text-to-speech engine to read a sample or your own text. Speech recognition and live voice chat are not connected."),
        NexusModule(5, "FILE LAB", "Open and preview text files", "Choose a supported text file, read its contents locally and copy the text. PDF extraction and AI document analysis are not connected yet."),
        NexusModule(6, "MEMORY 2.0", "Review saved NEXUS memory", "See which AI modes have local memory saved and how many LOST MIND notes exist. Existing memory data stays on this device."),
        NexusModule(7, "AUTOMATION ENGINE", "Manage routine ideas", "Save and enable/disable local routine presets. Background scheduling and automatic actions are not enabled in this foundation build."),
        NexusModule(8, "NEXUS EXTENSIONS", "Toggle optional modules", "Store local extension preferences. These switches organize the future extension system; they do not download third-party plugins."),
        NexusModule(9, "CREATIVE STUDIO 2.0", "Build a creative brief", "Draft and copy an image, music or story prompt. Generation providers are not silently assumed to be connected."),
        NexusModule(10, "SECURITY & BACKUP", "Check local protection", "The existing API key is encrypted with Android Keystore. This page never displays it. Exportable backup and app-lock setup are future work.")
    )
    var selected by remember { mutableIntStateOf(0) }
    var projectDraft by remember { mutableStateOf("") }
    var projects by remember { mutableStateOf(loadNexusProjects(prefs)) }
    var councilBrief by remember { mutableStateOf("") }
    var creativeBrief by remember { mutableStateOf("") }
    var voiceText by remember { mutableStateOf("NEXUS 3.0. Your workspace is ready.") }
    var voiceStatus by remember { mutableStateOf("Android text-to-speech is available if a voice engine is installed.") }
    var chosenFile by remember { mutableStateOf("No document selected") }
    var chosenMime by remember { mutableStateOf("") }
    var filePreview by remember { mutableStateOf("") }
    var fileStatus by remember { mutableStateOf("Choose a text-based file to preview its contents.") }
    var routineEnabled by remember { mutableStateOf(prefs.getBoolean("n3_routine_daily_review", false)) }
    var extensionFlags by remember { mutableStateOf(listOf("Image tools", "Project widgets", "Voice tools", "Developer tools").associateWith { prefs.getBoolean("n3_ext_" + it, false) }) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
            chosenFile = uri.lastPathSegment?.substringAfterLast('/') ?: "Selected document"
            chosenMime = context.contentResolver.getType(uri) ?: "Unknown file type"
            val extension = chosenFile.substringAfterLast('.', "").lowercase(Locale.ROOT)
            val isText = chosenMime.startsWith("text/") || extension in setOf("txt", "md", "json", "csv", "xml", "kt", "py", "log")
            if (isText) {
                try {
                    val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                    if (content.length > 1_000_000) {
                        filePreview = content.take(1_000_000)
                        fileStatus = "Preview limited to the first 1 MB."
                    } else {
                        filePreview = content
                        fileStatus = "File opened successfully."
                    }
                } catch (_: Exception) {
                    filePreview = ""
                    fileStatus = "Could not read this file."
                }
            } else {
                filePreview = ""
                fileStatus = "This file type is selected, but text preview supports TXT, MD, JSON, CSV, XML, Kotlin, Python and LOG files."
            }
        }
    }
    val tts = remember {
        TextToSpeech(context) { status ->
            voiceStatus = if (status == TextToSpeech.SUCCESS) "Voice engine initialized." else "No compatible text-to-speech engine was found."
        }
    }
    DisposableEffect(tts) { onDispose { tts.stop(); tts.shutdown() } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ NEXUS", color = Color(0xFFB99BCB)) }
            Spacer(Modifier.weight(1f))
            Text("VERSION 3.0 / FOUNDATION", color = Purple, fontSize = 8.sp, letterSpacing = 1.sp)
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, DeepPurple)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("NEXUS 3.0", color = Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("THE COMPLETE UPGRADE", color = MainText, fontSize = 23.sp, fontWeight = FontWeight.Black)
                Text("Ten systems. One workspace. Your existing AI modes and data stay in place.", color = Muted, fontSize = 11.sp)
                Text("10 UPGRADE MODULES", color = Purple, fontSize = 9.sp, letterSpacing = 1.sp)
            }
        }
        if (selected == 0) {
            modules.forEach { module ->
                Card(onClick = { selected = module.number }, modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, DeepPurple)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(38.dp).background(DeepPurple, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                            Text(module.number.toString().padStart(2, '0'), color = Color(0xFFDCC3F0), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(module.title, color = MainText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(module.subtitle, color = Muted, fontSize = 10.sp)
                        }
                        Text("OPEN ›", color = Purple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            val module = modules[selected - 1]
            TextButton(onClick = { selected = 0 }) { Text("‹ ALL UPGRADES", color = Purple) }
            Text(module.number.toString().padStart(2, '0') + " / " + module.title, color = MainText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(module.detail, color = Muted, fontSize = 11.sp, lineHeight = 17.sp)
            when (selected) {
                1 -> {
                    UpgradeCard("WORKSPACE OVERVIEW") {
                        Text("AI MODES  •  4", color = MainText, fontSize = 13.sp)
                        Text("PROJECTS  •  ${projects.size}", color = MainText, fontSize = 13.sp)
                        Text("LOST MIND NOTES  •  ${(prefs.getString("lost_mind_notes", "") ?: "").split("\n").count { it.isNotBlank() }}", color = MainText, fontSize = 13.sp)
                        Text("MEMORY SPACES  •  ${listOf("TITAN", "VOID", "NEXUS AI", "CREATIVE AI").count { !prefs.getString("memory_" + it, "").isNullOrBlank() }}", color = MainText, fontSize = 13.sp)
                    }
                }
                2 -> UpgradeCard("COUNCIL BRIEFING") {
                    OutlinedTextField(councilBrief, { councilBrief = it }, modifier = Modifier.fillMaxWidth(), minLines = 4, label = { Text("What should the AIs review?") }, colors = fieldColors())
                    Button(onClick = { copyText(context, "NEXUS AI COUNCIL\nReview this task independently, identify risks and assumptions, then provide a concise recommendation.\n\nTASK:\n" + councilBrief) }, enabled = councilBrief.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("COPY COUNCIL BRIEF") }
                }
                3 -> UpgradeCard("LOCAL PROJECT TRACKER") {
                    OutlinedTextField(projectDraft, { projectDraft = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("New project name") }, colors = fieldColors())
                    Button(onClick = {
                        if (projectDraft.isNotBlank()) {
                            projects = (projects + projectDraft.trim()).distinct()
                            saveNexusProjects(prefs, projects)
                            projectDraft = ""
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("ADD PROJECT") }
                    projects.forEachIndexed { index, project ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(project, color = MainText, modifier = Modifier.weight(1f), fontSize = 12.sp)
                            TextButton(onClick = { projects = projects.filterIndexed { i, _ -> i != index }; saveNexusProjects(prefs, projects) }) { Text("REMOVE", color = Muted, fontSize = 9.sp) }
                        }
                    }
                }
                4 -> UpgradeCard("TEXT TO SPEECH") {
                    OutlinedTextField(voiceText, { voiceText = it }, modifier = Modifier.fillMaxWidth(), minLines = 3, label = { Text("Text to speak") }, colors = fieldColors())
                    Button(onClick = {
                        val result = tts.setLanguage(Locale.getDefault())
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) voiceStatus = "The selected device voice does not support this language."
                        else { tts.speak(voiceText, TextToSpeech.QUEUE_FLUSH, null, "nexus3_voice"); voiceStatus = "Speaking with your device's voice engine." }
                    }, modifier = Modifier.fillMaxWidth()) { Text("SPEAK") }
                    Text(voiceStatus, color = Muted, fontSize = 10.sp)
                }
                5 -> UpgradeCard("NEXUS FILE LAB") {
                    Text("OPEN AND PREVIEW TEXT FILES", color = Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = { filePicker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("OPEN FILE") }
                    Text(chosenFile, color = MainText, fontSize = 12.sp)
                    Text(chosenMime, color = Muted, fontSize = 10.sp)
                    Text(fileStatus, color = Muted, fontSize = 10.sp)
                    if (filePreview.isNotBlank()) {
                        OutlinedTextField(filePreview, {}, modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 420.dp), readOnly = true, label = { Text("FILE CONTENT") }, colors = fieldColors())
                        Button(onClick = { copyText(context, filePreview) }, modifier = Modifier.fillMaxWidth()) { Text("COPY CONTENT") }
                    }
                }
                6 -> UpgradeCard("MEMORY STATUS") {
                    listOf("TITAN", "VOID", "NEXUS AI", "CREATIVE AI").forEach { ai ->
                        val saved = prefs.getString("memory_" + ai, "").orEmpty()
                        Text(ai + "  •  " + if (saved.isBlank()) "No saved memory" else "Saved memory available", color = MainText, fontSize = 12.sp)
                    }
                    Text("LOST MIND notes: " + (prefs.getString("lost_mind_notes", "") ?: "").split("\n").count { it.isNotBlank() }, color = Muted, fontSize = 11.sp)
                }
                7 -> UpgradeCard("ROUTINE PRESET") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Daily project review", color = MainText, fontSize = 12.sp)
                            Text("A saved preference only; Android background scheduling is not active yet.", color = Muted, fontSize = 10.sp)
                        }
                        Switch(checked = routineEnabled, onCheckedChange = {
                            routineEnabled = it
                            prefs.edit().putBoolean("n3_routine_daily_review", it).apply()
                        })
                    }
                }
                8 -> UpgradeCard("OPTIONAL EXTENSIONS") {
                    extensionFlags.forEach { (name, enabled) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(name, color = MainText, modifier = Modifier.weight(1f), fontSize = 12.sp)
                            Switch(checked = enabled, onCheckedChange = { value ->
                                extensionFlags = extensionFlags + (name to value)
                                prefs.edit().putBoolean("n3_ext_" + name, value).apply()
                            })
                        }
                    }
                }
                9 -> UpgradeCard("CREATIVE BRIEF BUILDER") {
                    OutlinedTextField(creativeBrief, { creativeBrief = it }, modifier = Modifier.fillMaxWidth(), minLines = 4, label = { Text("Describe your image, song or story idea") }, colors = fieldColors())
                    Button(onClick = { copyText(context, "NEXUS CREATIVE BRIEF\n" + creativeBrief) }, enabled = creativeBrief.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("COPY CREATIVE BRIEF") }
                }
                10 -> UpgradeCard("LOCAL SECURITY CHECK") {
                    val encryptedKeyExists = !prefs.getString("encrypted_api_key", null).isNullOrBlank()
                    Text(if (encryptedKeyExists) "API KEY  •  Encrypted local value found" else "API KEY  •  No saved key on this device", color = MainText, fontSize = 12.sp)
                    Text("Android Keystore protects the existing encrypted API-key storage. This screen never reveals the key.", color = Muted, fontSize = 10.sp)
                    Text("BACKUP EXPORT  •  Not implemented", color = Muted, fontSize = 11.sp)
                    Text("APP LOCK  •  Not implemented", color = Muted, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun UpgradeCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, DeepPurple)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(title, color = Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            content()
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Purple, unfocusedBorderColor = Color(0xFF3A3042),
    focusedTextColor = MainText, unfocusedTextColor = MainText, cursorColor = Purple
)

private fun copyText(context: Context, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("NEXUS 3.0", value))
}

private fun loadNexusProjects(prefs: android.content.SharedPreferences): List<String> = try {
    val array = JSONArray(prefs.getString("nexus3_projects", "[]") ?: "[]")
    (0 until array.length()).map { array.getString(it) }
} catch (_: Exception) { emptyList() }

private fun saveNexusProjects(prefs: android.content.SharedPreferences, projects: List<String>) {
    val array = JSONArray()
    projects.forEach { array.put(it) }
    prefs.edit().putString("nexus3_projects", array.toString()).apply()
}
