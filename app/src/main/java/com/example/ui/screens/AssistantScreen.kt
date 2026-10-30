package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.AiProviderType
import com.example.data.model.AssistantState
import com.example.ui.components.MessageItemCard
import com.example.ui.components.ToolsBottomSheet
import com.example.ui.components.UserAvatar
import com.example.ui.components.VoiceModeOverlay
import com.example.ui.theme.GptAccent
import com.example.ui.theme.GptBackground
import com.example.ui.theme.GptBorder
import com.example.ui.theme.GptSidebar
import com.example.ui.theme.GptSurface
import com.example.ui.theme.GptSurfaceActive
import com.example.ui.theme.GptSurfaceElevated
import com.example.ui.theme.GptTextMuted
import com.example.ui.theme.GptTextPrimary
import com.example.ui.theme.GptTextSecondary
import com.example.ui.theme.GptWhite
import com.example.ui.viewmodel.AssistantViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val assistantState by viewModel.assistantState.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()
    val selectedProvider by viewModel.preferencesRepository.selectedProviderFlow.collectAsState(initial = AiProviderType.GEMINI)
    val userName by viewModel.preferencesRepository.userNameFlow.collectAsState(initial = "")
    val userPhotoUri by viewModel.preferencesRepository.userPhotoUriFlow.collectAsState(initial = "")

    var inputText by remember { mutableStateOf("") }
    var isVoiceModeOpen by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }
    var showToolsSheet by remember { mutableStateOf(false) }

    val toolsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    // Permission launcher for voice mode
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isVoiceModeOpen = true
            viewModel.voiceManager.startListening()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Microphone permission is required for voice mode.")
            }
        }
    }

    fun openVoiceMode() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            isVoiceModeOpen = true
            viewModel.voiceManager.startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = GptBackground,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                // Top App Bar: Minimalist ChatGPT Style
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: History drawer button
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.testTag("nav_history_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Chat Threads",
                            tint = GptTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Center: Model Switcher Pill
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(GptSurface)
                                .border(1.dp, GptBorder, RoundedCornerShape(18.dp))
                                .clickable { showModelMenu = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("model_switcher_pill")
                        ) {
                            Text(
                                text = "SolyNext",
                                color = GptTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = selectedProvider.displayName.split(" ").first(),
                                color = GptTextMuted,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Switch Model",
                                tint = GptTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showModelMenu,
                            onDismissRequest = { showModelMenu = false },
                            modifier = Modifier.background(GptSurfaceElevated)
                        ) {
                            AiProviderType.values().forEach { provider ->
                                val isCurrent = provider == selectedProvider
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = provider.displayName,
                                                color = if (isCurrent) GptAccent else GptTextPrimary,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 14.sp
                                            )
                                            if (isCurrent) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = GptAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.preferencesRepository.setProvider(provider)
                                        }
                                        showModelMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Right: New Chat button & User Avatar profile badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.createNewConversation() },
                            modifier = Modifier.testTag("new_chat_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Session",
                                tint = GptTextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onNavigateToSettings() }
                                .testTag("top_user_avatar")
                        ) {
                            UserAvatar(
                                name = userName,
                                photoUri = userPhotoUri.ifBlank { null },
                                size = 32.dp
                            )
                        }
                    }
                }

                // Chat Message Stream OR Empty Welcome Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (messages.size <= 1) {
                        // Minimalist ChatGPT Welcome State
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(GptSurfaceElevated)
                                    .border(1.dp, GptBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_solynext_logo),
                                    contentDescription = "SolyNext AI",
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "What can I help with today?",
                                color = GptTextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(28.dp))

                            // 4 Quick Prompt Cards (Clean 2x2 grid)
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    PromptCard(
                                        title = "Check Weather",
                                        subtitle = "Tokyo live telemetry",
                                        modifier = Modifier.weight(1f),
                                        onClick = { viewModel.handleUserMessage("What's the weather in Tokyo today?") }
                                    )
                                    PromptCard(
                                        title = "Web Search",
                                        subtitle = "Quantum computing news",
                                        modifier = Modifier.weight(1f),
                                        onClick = { viewModel.handleUserMessage("Search web for latest breakthroughs in quantum computing") }
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    PromptCard(
                                        title = "Schedule Alert",
                                        subtitle = "Remind me in 15 min",
                                        modifier = Modifier.weight(1f),
                                        onClick = { viewModel.handleUserMessage("Remind me in 15 minutes to take a break") }
                                    )
                                    PromptCard(
                                        title = "Save Memory",
                                        subtitle = "Teach a personal fact",
                                        modifier = Modifier.weight(1f),
                                        onClick = { viewModel.handleUserMessage("Remember that my preferred framework is Jetpack Compose") }
                                    )
                                }
                            }
                        }
                    } else {
                        // Standard Message Feed
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            items(messages, key = { it.id }) { msg ->
                                MessageItemCard(
                                    message = msg,
                                    onSpeakMessage = { text -> viewModel.speakText(text) },
                                    userName = userName,
                                    userPhotoUri = userPhotoUri.ifBlank { null }
                                )
                            }

                            // Thinking indicator
                            if (assistantState == AssistantState.THINKING) {
                                item {
                                    ThinkingDotsRow()
                                }
                            }
                        }
                    }
                }

                // ChatGPT Floating Capsule Composer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .background(GptSurface)
                            .border(1.dp, GptBorder, RoundedCornerShape(26.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Plus / Tools button
                        IconButton(
                            onClick = { showToolsSheet = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GptSurfaceElevated)
                                .testTag("composer_tools_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tools and Features",
                                tint = GptTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Text Input Field
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = "Message SolyNext...",
                                    color = GptTextMuted,
                                    fontSize = 15.sp
                                )
                            }
                            BasicTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                textStyle = TextStyle(
                                    color = GptTextPrimary,
                                    fontSize = 15.sp
                                ),
                                cursorBrush = SolidColor(GptWhite),
                                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (inputText.isNotBlank()) {
                                            val query = inputText
                                            inputText = ""
                                            viewModel.handleUserMessage(query)
                                        }
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("text_input_field")
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Send Button (when typing) OR ChatGPT Voice Mode Wave Button (when empty)
                        if (inputText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val query = inputText
                                    inputText = ""
                                    viewModel.handleUserMessage(query)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(GptWhite)
                                    .testTag("send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Send",
                                    tint = GptBackground,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            // ChatGPT Voice Mode Button
                            IconButton(
                                onClick = { openVoiceMode() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(GptSurfaceElevated)
                                    .testTag("voice_mode_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Voice Mode",
                                    tint = GptTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ChatGPT Voice Mode Immersive Overlay
            VoiceModeOverlay(
                viewModel = viewModel,
                isOpen = isVoiceModeOpen,
                onClose = { isVoiceModeOpen = false }
            )

            // Capabilities Bottom Sheet
            if (showToolsSheet) {
                ToolsBottomSheet(
                    sheetState = toolsSheetState,
                    onDismiss = { showToolsSheet = false },
                    onNavigateToTools = onNavigateToTools,
                    onNavigateToReminders = onNavigateToReminders,
                    onNavigateToMemory = onNavigateToMemory,
                    onNavigateToHistory = onNavigateToHistory,
                    onNavigateToSettings = onNavigateToSettings,
                    onPromptSelected = { prompt ->
                        viewModel.handleUserMessage(prompt)
                    }
                )
            }
        }
    }
}

@Composable
fun PromptCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(GptSurface)
            .border(1.dp, GptBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = title,
                color = GptTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = GptTextMuted,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ThinkingDotsRow() {
    val infiniteTransition = rememberInfiniteTransition(label = "ThinkingDots")
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "Dot1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse),
        label = "Dot2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse),
        label = "Dot3"
    )

    Row(
        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(GptTextSecondary.copy(alpha = alpha1))
        )
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(GptTextSecondary.copy(alpha = alpha2))
        )
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(GptTextSecondary.copy(alpha = alpha3))
        )
    }
}
