package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemoryEntity
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.GptAccent
import com.example.ui.theme.GptBackground
import com.example.ui.theme.GptBlack
import com.example.ui.theme.GptBorder
import com.example.ui.theme.GptSurface
import com.example.ui.theme.GptSurfaceElevated
import com.example.ui.theme.GptTextMuted
import com.example.ui.theme.GptTextPrimary
import com.example.ui.theme.GptTextSecondary
import com.example.ui.theme.GptWhite
import com.example.ui.viewmodel.AssistantViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    viewModel: AssistantViewModel,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val memories by viewModel.allMemories.collectAsState()
    val memoryEnabled by viewModel.preferencesRepository.memoryEnabledFlow.collectAsState(initial = true)

    var showAddDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GptBackground),
                title = { Text("Memory & Personalization", color = GptTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("memory_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GptTextPrimary)
                    }
                },
                actions = {
                    if (memories.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirm = true }, modifier = Modifier.testTag("clear_all_memory_btn")) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear All", tint = GptTextMuted)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = GptWhite,
                contentColor = GptBlack,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("add_memory_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Memory")
            }
        },
        containerColor = GptBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Memory Toggle Card
            Card(
                colors = CardDefaults.cardColors(containerColor = GptSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GptBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(GptSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = GptAccent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Active Memory Recall", color = GptTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            text = if (memoryEnabled) "SolyNext recalls your preferences across conversations." else "Memory injection paused.",
                            color = GptTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = memoryEnabled,
                        onCheckedChange = { coroutineScope.launch { viewModel.preferencesRepository.setMemoryEnabled(it) } },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GptWhite,
                            checkedTrackColor = GptAccent,
                            uncheckedThumbColor = GptTextSecondary,
                            uncheckedTrackColor = GptSurfaceElevated
                        ),
                        modifier = Modifier.testTag("memory_toggle_switch")
                    )
                }
            }

            Text(
                text = "SAVED FACTS & PREFERENCES (${memories.size})",
                color = GptTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (memories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Bookmark, contentDescription = null, tint = GptTextMuted, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No saved memories", color = GptTextSecondary, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Teach SolyNext during chat or tap + below.",
                            color = GptTextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(memories, key = { it.id }) { mem ->
                        val dateFormatted = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(mem.createdAt))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = GptSurface),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GptBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("memory_item_${mem.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(GptSurfaceElevated)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = mem.category.uppercase(),
                                                color = GptTextSecondary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = dateFormatted, color = GptTextMuted, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = mem.fact,
                                        color = GptTextPrimary,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteMemory(mem) },
                                    modifier = Modifier.testTag("delete_memory_${mem.id}")
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete memory",
                                        tint = GptTextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddMemoryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { fact, category ->
                viewModel.addManualMemory(fact, category)
                showAddDialog = false
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Memories?") },
            text = { Text("This will permanently remove saved personal facts. SolyNext will no longer recall them in conversations.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllMemories()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = GptTextSecondary)
                }
            },
            containerColor = GptSurface,
            titleContentColor = GptTextPrimary,
            textContentColor = GptTextSecondary
        )
    }
}

@Composable
fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (fact: String, category: String) -> Unit
) {
    var factText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Personal") }
    val categories = listOf("Personal", "Preference", "Project", "Work")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remember a Fact", color = GptTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Category:", color = GptTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (cat in categories) {
                        val isSel = selectedCategory == cat
                        Button(
                            onClick = { selectedCategory = cat },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) GptWhite else GptSurfaceElevated,
                                contentColor = if (isSel) GptBlack else GptTextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(cat, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = factText,
                    onValueChange = { factText = it },
                    placeholder = { Text("e.g. My favorite food is ramen", color = GptTextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GptBorder,
                        unfocusedBorderColor = GptBorder,
                        focusedTextColor = GptTextPrimary,
                        unfocusedTextColor = GptTextPrimary,
                        focusedContainerColor = GptSurfaceElevated,
                        unfocusedContainerColor = GptSurfaceElevated
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (factText.isNotBlank()) onConfirm(factText, selectedCategory.lowercase()) },
                enabled = factText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GptWhite, contentColor = GptBlack)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = GptTextSecondary)
            }
        },
        containerColor = GptSurface
    )
}
