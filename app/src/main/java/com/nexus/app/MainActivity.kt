package com.nexus.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = Color(0xFF08070B)
private val Panel = Color(0xFF15111B)
private val Purple = Color(0xFF7040A0)
private val DeepPurple = Color(0xFF301044)
private val MainText = Color(0xFFEAE5EF)
private val Muted = Color(0xFF9B8AA8)

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
    var selectedAI by remember { mutableStateOf(prefs.getString("selected_ai", "NEXUS AI") ?: "NEXUS AI") }
    var memoryText by remember { mutableStateOf("") }
    var memorySaved by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf(loadNotes(prefs)) }
    var sharing by remember { mutableStateOf(prefs.getBoolean("memory_sharing", false)) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Night,
            surface = Panel,
            primary = Purple,
            secondary = DeepPurple,
            onBackground = MainText,
            onSurface = MainText,
            onPrimary = Color.White
        )
    ) {
        Scaffold(
            containerColor = Night,
            bottomBar = {
                NavigationBar(containerColor = Panel, contentColor = MainText) {
                    listOf("Home", "Lost Mind", "Memory").forEach { item ->
                        NavigationBarItem(
                            selected = page == item,
                            onClick = { page = item },
                            icon = { Text(when (item) { "Home" -> "⌂"; "Lost Mind" -> "✧"; else -> "◈" }, fontSize = 20.sp) },
                            label = { Text(item, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = DeepPurple,
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Night)
                    .padding(padding)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(18.dp))
                Text("N E X U S", color = MainText, fontSize = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                Text("YOUR DIGITAL SPACE", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(22.dp))

                when (page) {
                    "Home" -> HomePage(
                        selectedAI = selectedAI,
                        onSelect = {
                            selectedAI = it
                            prefs.edit().putString("selected_ai", it).apply()
                        }
                    )
                    "Lost Mind" -> LostMindPage(
                        noteText = noteText,
                        onNoteChange = { noteText = it },
                        notes = notes,
                        onAdd = {
                            if (noteText.isNotBlank()) {
                                val updated = listOf(noteText.trim()) + notes
                                notes = updated
                                saveNotes(prefs, updated)
                                noteText = ""
                            }
                        },
                        onDelete = { index ->
                            notes = notes.filterIndexed { i, _ -> i != index }
                            saveNotes(prefs, notes)
                        }
                    )
                    else -> MemoryPage(
                        selectedAI = selectedAI,
                        memoryText = memoryText,
                        onMemoryChange = { memoryText = it; memorySaved = false },
                        savedText = prefs.getString("memory_$selectedAI", "") ?: "",
                        sharing = sharing,
                        onSharingChange = {
                            sharing = it
                            prefs.edit().putBoolean("memory_sharing", it).apply()
                        },
                        onSave = {
                            prefs.edit().putString("memory_$selectedAI", memoryText).apply()
                            memorySaved = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomePage(selectedAI: String, onSelect: (String) -> Unit) {
    Text("Choose your intelligence.", color = MainText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    Text("Four modes. One home.", color = Muted, fontSize = 13.sp)
    Spacer(Modifier.height(18.dp))
    val modes = listOf(
        Triple("TITAN", "Personal companion", "Your familiar AI space"),
        Triple("VOID", "Developer intelligence", "Code, systems and terminal thinking"),
        Triple("NEXUS AI", "General assistant", "Everyday questions and ideas"),
        Triple("CREATIVE AI", "Creative studio", "Music, images, stories and games")
    )
    modes.forEach { (name, subtitle, detail) ->
        Card(
            onClick = { onSelect(name) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 11.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = if (selectedAI == name) DeepPurple else Panel),
            border = if (selectedAI == name) androidx.compose.foundation.BorderStroke(1.dp, Purple) else null
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = MainText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    if (selectedAI == name) Text("ACTIVE", color = Color(0xFFC7A5E8), fontSize = 10.sp, letterSpacing = 1.sp)
                }
                Text(subtitle, color = MainText, fontSize = 13.sp)
                Text(detail, color = Muted, fontSize = 11.sp)
            }
        }
    }
    Spacer(Modifier.height(4.dp))
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("SYSTEM STATUS", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(6.dp))
            Text("Local prototype is online", color = MainText, fontSize = 14.sp)
            Text("AI chat connections will be added in a later build. Your notes and mode selection are stored on this device.", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun LostMindPage(
    noteText: String,
    onNoteChange: (String) -> Unit,
    notes: List<String>,
    onAdd: () -> Unit,
    onDelete: (Int) -> Unit
) {
    Text("LOST MIND", color = MainText, fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    Text("A safe place for unfinished thoughts.", color = Muted, fontSize = 13.sp)
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = noteText,
        onValueChange = onNoteChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Catch that thought before it disappears...", color = Muted) },
        minLines = 3,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = fieldColors()
    )
    Spacer(Modifier.height(10.dp))
    Button(onClick = onAdd, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)) {
        Text("SAVE THOUGHT")
    }
    Spacer(Modifier.height(18.dp))
    Text("YOUR THOUGHTS  ·  ${notes.size}", color = Muted, fontSize = 11.sp, letterSpacing = 1.sp)
    Spacer(Modifier.height(8.dp))
    if (notes.isEmpty()) {
        Text("Nothing here yet. The next strange idea starts here.", color = Muted, fontSize = 13.sp)
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(notes.indices.toList()) { index ->
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(notes[index], color = MainText, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        TextButton(onClick = { onDelete(index) }) { Text("DELETE", color = Muted, fontSize = 10.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryPage(
    selectedAI: String,
    memoryText: String,
    onMemoryChange: (String) -> Unit,
    savedText: String,
    sharing: Boolean,
    onSharingChange: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    Text("INDEPENDENT MEMORY", color = MainText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Text("Each AI keeps its own notes.", color = Muted, fontSize = 13.sp)
    Spacer(Modifier.height(15.dp))
    Text("CURRENT SPACE: $selectedAI", color = Color(0xFFC7A5E8), fontSize = 11.sp, letterSpacing = 1.sp)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = memoryText,
        onValueChange = onMemoryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Add something you want this AI space to remember...", color = Muted) },
        minLines = 4,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = fieldColors()
    )
    Spacer(Modifier.height(10.dp))
    Button(onClick = onSave, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)) {
        Text("SAVE TO $selectedAI")
    }
    Spacer(Modifier.height(14.dp))
    Text(if (savedText.isBlank()) "No saved memory in this space yet." else "Saved memory exists for this AI space.", color = Muted, fontSize = 12.sp)
    HorizontalDivider(color = Panel, modifier = Modifier.padding(vertical = 14.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Optional memory sharing", color = MainText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("Allow AI spaces to share selected memories later.", color = Muted, fontSize = 11.sp)
        }
        Switch(checked = sharing, onCheckedChange = onSharingChange)
    }
    Text("Sharing is off by default. This prototype stores memory locally on your phone.", color = Muted, fontSize = 11.sp)
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Purple,
    unfocusedBorderColor = Color(0xFF3A3042),
    focusedTextColor = MainText,
    unfocusedTextColor = MainText,
    cursorColor = Purple
)

private fun loadNotes(prefs: android.content.SharedPreferences): List<String> {
    val raw = prefs.getString("lost_mind_notes", "") ?: ""
    return if (raw.isBlank()) emptyList() else raw.split("\n").filter { it.isNotBlank() }
}

private fun saveNotes(prefs: android.content.SharedPreferences, notes: List<String>) {
    prefs.edit().putString("lost_mind_notes", notes.joinToString("\n")).apply()
}
