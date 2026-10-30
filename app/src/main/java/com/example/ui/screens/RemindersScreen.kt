package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReminderEntity
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: AssistantViewModel,
    onNavigateBack: () -> Unit
) {
    val reminders by viewModel.allReminders.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GptBackground),
                title = { Text("Reminders & Tasks", color = GptTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("reminders_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GptTextPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    showAddDialog = true
                },
                containerColor = GptWhite,
                contentColor = GptBlack,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("add_reminder_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Reminder")
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
            val activeReminders = reminders.filter { !it.isCompleted }
            val completedReminders = reminders.filter { it.isCompleted }

            if (reminders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Alarm, contentDescription = null, tint = GptTextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No scheduled reminders", color = GptTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Say \"Remind me in 10 minutes\" or tap + below.", color = GptTextMuted, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activeReminders.isNotEmpty()) {
                        item {
                            Text("UPCOMING (${activeReminders.size})", color = GptTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        }
                        items(activeReminders, key = { it.id }) { reminder ->
                            ReminderCard(
                                reminder = reminder,
                                onToggle = { viewModel.toggleReminderCompleted(reminder) },
                                onDelete = { viewModel.deleteReminder(reminder) }
                            )
                        }
                    }

                    if (completedReminders.isNotEmpty()) {
                        item {
                            Text("COMPLETED (${completedReminders.size})", color = GptTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                        }
                        items(completedReminders, key = { it.id }) { reminder ->
                            ReminderCard(
                                reminder = reminder,
                                onToggle = { viewModel.toggleReminderCompleted(reminder) },
                                onDelete = { viewModel.deleteReminder(reminder) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddReminderDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, minutes, notes ->
                val due = Calendar.getInstance().apply { add(Calendar.MINUTE, minutes) }.timeInMillis
                viewModel.addManualReminder(title, due, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ReminderCard(
    reminder: ReminderEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dateDisplay = SimpleDateFormat("EEE, MMM d • hh:mm a", Locale.getDefault()).format(Date(reminder.dueTimestamp))
    val isPast = reminder.dueTimestamp < System.currentTimeMillis()

    Card(
        colors = CardDefaults.cardColors(containerColor = GptSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GptBorder),
        modifier = Modifier.fillMaxWidth().testTag("reminder_item_${reminder.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = reminder.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = GptWhite,
                    checkmarkColor = GptBlack,
                    uncheckedColor = GptTextSecondary
                )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    color = if (reminder.isCompleted) GptTextMuted else GptTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (reminder.notes.isNotBlank()) {
                    Text(
                        text = reminder.notes,
                        color = GptTextSecondary,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Alarm,
                        contentDescription = null,
                        tint = if (reminder.isCompleted) GptTextMuted else if (isPast) AlertCrimson else GptAccent,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPast && !reminder.isCompleted) "Overdue: $dateDisplay" else dateDisplay,
                        color = if (reminder.isCompleted) GptTextMuted else if (isPast) AlertCrimson else GptTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = GptTextMuted)
            }
        }
    }
}

@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, delayMinutes: Int, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedMinutes by remember { mutableStateOf(15) }
    val timeOptions = listOf(5, 15, 30, 60, 120)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Reminder", color = GptTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Reminder title (e.g. Call Client)", color = GptTextMuted) },
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
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Optional notes", color = GptTextMuted) },
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
                Spacer(modifier = Modifier.height(14.dp))
                Text("Schedule in:", color = GptTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (mins in timeOptions) {
                        val isSel = selectedMinutes == mins
                        Button(
                            onClick = { selectedMinutes = mins },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) GptWhite else GptSurfaceElevated,
                                contentColor = if (isSel) GptBlack else GptTextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (mins >= 60) "${mins / 60}h" else "${mins}m", fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onConfirm(title, selectedMinutes, notes) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GptWhite, contentColor = GptBlack)
            ) {
                Text("Schedule")
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
