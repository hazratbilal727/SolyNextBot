package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiProviderType
import com.example.data.model.PersonalityStyle
import com.example.ui.components.UserAvatar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AssistantViewModel,
    onNavigateBack: () -> Unit,
    onResetAccount: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()

    val currentUserName by viewModel.preferencesRepository.userNameFlow.collectAsState(initial = "")
    val currentUserPhotoUri by viewModel.preferencesRepository.userPhotoUriFlow.collectAsState(initial = "")

    val currentProvider by viewModel.preferencesRepository.selectedProviderFlow.collectAsState(initial = AiProviderType.GEMINI)
    val currentPersonality by viewModel.preferencesRepository.personalityFlow.collectAsState(initial = PersonalityStyle.JARVIS)

    val geminiKey by viewModel.preferencesRepository.geminiKeyFlow.collectAsState(initial = "")
    val geminiModel by viewModel.preferencesRepository.geminiModelFlow.collectAsState(initial = "gemini-2.0-flash")

    val openAiKey by viewModel.preferencesRepository.openAiKeyFlow.collectAsState(initial = "")
    val openAiModel by viewModel.preferencesRepository.openAiModelFlow.collectAsState(initial = "gpt-4o-mini")

    val grokKey by viewModel.preferencesRepository.grokKeyFlow.collectAsState(initial = "")
    val grokModel by viewModel.preferencesRepository.grokModelFlow.collectAsState(initial = "grok-beta")

    val speechRate by viewModel.preferencesRepository.speechRateFlow.collectAsState(initial = 1.05f)
    val speechPitch by viewModel.preferencesRepository.speechPitchFlow.collectAsState(initial = 1.0f)
    val autoSpeak by viewModel.preferencesRepository.autoSpeakFlow.collectAsState(initial = true)

    val connectionStatus by viewModel.providerConnectionStatus.collectAsState()
    val isTestingConnection by viewModel.isTestingConnection.collectAsState()

    var tempUserName by remember(currentUserName) { mutableStateOf(currentUserName) }
    var tempKey by remember(currentProvider, geminiKey, openAiKey, grokKey) {
        mutableStateOf(
            when (currentProvider) {
                AiProviderType.GEMINI -> geminiKey
                AiProviderType.OPENAI -> openAiKey
                AiProviderType.GROK -> grokKey
            }
        )
    }
    var keyVisible by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Android zero-permission photo picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                viewModel.preferencesRepository.setUserPhotoUri(uri.toString())
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Local Account?", color = GptTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will clear your account credentials and return to the onboarding setup screen so you can re-enter your name, photo, and API key.",
                    color = GptTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        coroutineScope.launch {
                            viewModel.preferencesRepository.resetAccount()
                            onResetAccount()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson, contentColor = GptWhite)
                ) {
                    Text("Reset Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = GptTextSecondary)
                }
            },
            containerColor = GptSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GptBackground),
                title = { Text("Settings & Profile", color = GptTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GptTextPrimary)
                    }
                }
            )
        },
        containerColor = GptBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 0: Local Account Profile Card
            Card(
                colors = CardDefaults.cardColors(containerColor = GptSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GptBorder),
                modifier = Modifier.fillMaxWidth().testTag("profile_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Local Account Profile", color = GptTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("Stored privately on this device", color = GptTextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier
                                .size(64.dp)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        ) {
                            UserAvatar(
                                name = currentUserName,
                                photoUri = currentUserPhotoUri.ifBlank { null },
                                size = 64.dp
                            )
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(GptWhite)
                                    .border(1.dp, GptBackground, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Change photo",
                                    tint = GptBlack,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUserName.ifBlank { "User" },
                                color = GptTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "Active Engine: ${currentProvider.displayName}",
                                color = GptTextMuted,
                                fontSize = 12.sp
                            )

                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.testTag("change_photo_btn")
                                ) {
                                    Text("Change Photo", color = GptTextSecondary, fontSize = 11.sp)
                                }

                                if (currentUserPhotoUri.isNotBlank()) {
                                    TextButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                viewModel.preferencesRepository.setUserPhotoUri("")
                                            }
                                        },
                                        modifier = Modifier.testTag("remove_photo_btn")
                                    ) {
                                        Text("Use Letter Initial", color = GptTextMuted, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = tempUserName,
                        onValueChange = {
                            tempUserName = it
                            coroutineScope.launch { viewModel.preferencesRepository.setUserName(it) }
                        },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("user_name_input"),
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
            }

            // Section 1: AI Provider Selection & Key
            Card(
                colors = CardDefaults.cardColors(containerColor = GptSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GptBorder),
                modifier = Modifier.fillMaxWidth().testTag("provider_selection_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("AI Engine Provider", color = GptTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("Select your active AI reasoning model", color = GptTextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (type in AiProviderType.values()) {
                            val isSelected = currentProvider == type
                            Button(
                                onClick = {
                                    coroutineScope.launch { viewModel.preferencesRepository.setProvider(type) }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) GptWhite else GptSurfaceElevated,
                                    contentColor = if (isSelected) GptBlack else GptTextPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("provider_btn_${type.name}")
                            ) {
                                Text(type.displayName.split(" ").first(), fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Configuring ${currentProvider.displayName}",
                        color = GptTextPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = {
                            tempKey = it
                            coroutineScope.launch {
                                when (currentProvider) {
                                    AiProviderType.GEMINI -> viewModel.preferencesRepository.setGeminiCredentials(it, geminiModel)
                                    AiProviderType.OPENAI -> viewModel.preferencesRepository.setOpenAiCredentials(it, openAiModel)
                                    AiProviderType.GROK -> viewModel.preferencesRepository.setGrokCredentials(it, grokModel)
                                }
                            }
                        },
                        label = { Text("API Key") },
                        placeholder = {
                            Text(
                                "Enter ${currentProvider.displayName} API Key",
                                color = GptTextMuted
                            )
                        },
                        singleLine = true,
                        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { keyVisible = !keyVisible }) {
                                Icon(
                                    imageVector = if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle visibility",
                                    tint = GptTextSecondary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("api_key_input"),
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

                    if (currentProvider == AiProviderType.GEMINI) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "GEMINI MODEL VERSION",
                            color = GptTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val geminiVersions = listOf(
                            "gemini-2.0-flash" to "2.0 Flash",
                            "gemini-2.5-flash" to "2.5 Flash",
                            "gemini-2.0-flash-lite" to "2.0 Lite",
                            "gemini-1.5-flash" to "1.5 Flash",
                            "gemini-1.5-pro" to "1.5 Pro"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            geminiVersions.take(3).forEach { (modelId, label) ->
                                val isSel = geminiModel == modelId
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.preferencesRepository.setGeminiCredentials(tempKey, modelId)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSel) GptWhite else GptSurfaceElevated,
                                        contentColor = if (isSel) GptBlack else GptTextSecondary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("gemini_model_$modelId")
                                ) {
                                    Text(label, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            geminiVersions.drop(3).forEach { (modelId, label) ->
                                val isSel = geminiModel == modelId
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.preferencesRepository.setGeminiCredentials(tempKey, modelId)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSel) GptWhite else GptSurfaceElevated,
                                        contentColor = if (isSel) GptBlack else GptTextSecondary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("gemini_model_$modelId")
                                ) {
                                    Text(label, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }

                        Text(
                            text = "Supports any Gemini key. SolyNext automatically tests models and falls back seamlessly.",
                            color = GptTextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.testProviderConnection(currentProvider) },
                            enabled = !isTestingConnection,
                            colors = ButtonDefaults.buttonColors(containerColor = GptSurfaceElevated, contentColor = GptTextPrimary),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GptBorder),
                            modifier = Modifier.testTag("validate_connection_btn")
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = GptTextPrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Test Connection", fontSize = 12.sp)
                        }

                        Text(
                            text = "Active: ${
                                when (currentProvider) {
                                    AiProviderType.GEMINI -> geminiModel
                                    AiProviderType.OPENAI -> openAiModel
                                    AiProviderType.GROK -> grokModel
                                }
                            }",
                            color = GptTextMuted,
                            fontSize = 11.sp
                        )
                    }

                    connectionStatus?.let { status ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = status,
                            color = if (status.startsWith("✓")) GptAccent else AlertCrimson,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Section 2: Personality & Persona
            Card(
                colors = CardDefaults.cardColors(containerColor = GptSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GptBorder),
                modifier = Modifier.fillMaxWidth().testTag("personality_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Personality Style", color = GptTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("Select the assistant's speaking demeanor", color = GptTextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    for (style in PersonalityStyle.values()) {
                        val isSelected = currentPersonality == style
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) GptSurfaceElevated else GptSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) GptTextSecondary else GptBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(style.displayName, color = GptTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(style.promptInstruction, color = GptTextMuted, fontSize = 11.sp, maxLines = 2)
                                }
                                Button(
                                    onClick = { coroutineScope.launch { viewModel.preferencesRepository.setPersonality(style) } },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) GptWhite else GptBorder,
                                        contentColor = if (isSelected) GptBlack else GptTextPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(start = 8.dp).testTag("select_personality_${style.name}")
                                ) {
                                    Text(if (isSelected) "Active" else "Select", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Speech Synthesis
            Card(
                colors = CardDefaults.cardColors(containerColor = GptSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GptBorder),
                modifier = Modifier.fillMaxWidth().testTag("voice_settings_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Voice & Speech Synthesis", color = GptTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("Configure speech rate, pitch, and vocal responses", color = GptTextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-Speak Responses", color = GptTextPrimary, fontSize = 14.sp)
                        Switch(
                            checked = autoSpeak,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    viewModel.preferencesRepository.setVoiceSettings(speechRate, speechPitch, it)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GptWhite,
                                checkedTrackColor = GptAccent,
                                uncheckedThumbColor = GptTextSecondary,
                                uncheckedTrackColor = GptSurfaceElevated
                            ),
                            modifier = Modifier.testTag("auto_speak_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Speech Rate: ${String.format("%.2f", speechRate)}x", color = GptTextSecondary, fontSize = 12.sp)
                    Slider(
                        value = speechRate,
                        onValueChange = { rate ->
                            coroutineScope.launch {
                                viewModel.preferencesRepository.setVoiceSettings(rate, speechPitch, autoSpeak)
                            }
                        },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = GptWhite, activeTrackColor = GptWhite)
                    )

                    Text("Voice Pitch: ${String.format("%.2f", speechPitch)}x", color = GptTextSecondary, fontSize = 12.sp)
                    Slider(
                        value = speechPitch,
                        onValueChange = { pitch ->
                            coroutineScope.launch {
                                viewModel.preferencesRepository.setVoiceSettings(speechRate, pitch, autoSpeak)
                            }
                        },
                        valueRange = 0.5f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = GptWhite, activeTrackColor = GptWhite)
                    )

                    Button(
                        onClick = {
                            viewModel.speakText("All systems online. Vocal synthesizer is operating within optimal parameters.")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GptSurfaceElevated, contentColor = GptTextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GptBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End).testTag("test_voice_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Voice", fontSize = 12.sp)
                    }
                }
            }

            // Section 4: Account Actions & Reset
            Card(
                colors = CardDefaults.cardColors(containerColor = GptSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GptBorder),
                modifier = Modifier.fillMaxWidth().testTag("account_reset_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Account Management", color = GptTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("Switch user account, API key, or re-run setup wizard", color = GptTextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showResetDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AlertCrimson.copy(alpha = 0.15f),
                            contentColor = AlertCrimson
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AlertCrimson.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("reset_account_btn")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Switch Account / Re-enter API Key", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
